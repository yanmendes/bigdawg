package istc.bigdawg;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import istc.bigdawg.catalog.CatalogInstance;
import istc.bigdawg.migration.MigratorTask;
import istc.bigdawg.monitoring.MonitoringTask;
import istc.bigdawg.postgresql.PostgreSQLHandler;
import istc.bigdawg.properties.BigDawgConfigProperties;
import istc.bigdawg.scidb.SciDBHandler;

/**
 * Main class.
 */
public class Main {

    public static String BASE_URI;
    private static Logger logger;
    private static MigratorTask migratorTask;
    private static MonitoringTask relationalTask;
    private static HttpServer server;
    private static volatile boolean shutdown;

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
     * application.
     *
     * @param ipAddress The IP address on which the Grizzly server should wait for requests.
     * @return Grizzly HTTP Server.
     * @throws IOException
     */
    public static HttpServer startServer(String ipAddress) throws IOException {
        // exposing the Jersey application at BASE_URI
        if (ipAddress != null) {
            BASE_URI = BigDawgConfigProperties.INSTANCE.getBaseURI(ipAddress);
        } else {
            BASE_URI = BigDawgConfigProperties.INSTANCE.getBaseURI();
        }
        if (logger == null) {
            // Logger
            LoggerSetup.setLogging();
            logger = Logger.getLogger(Main.class);
        }

        logger.info("base uri: " + BASE_URI);

        // create a resource config that scans for JAX-RS resources and
        // providers in istc.bigdawg package
        final ResourceConfig rc = new ResourceConfig().packages("istc.bigdawg");

        // create and start a new instance of grizzly http server
        System.out.println("Starting HTTP server on: " + BASE_URI);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void checkDatabaseConnections() {
        // Todo: configure these checks based on catalog.engines
        int postgreSQL1Engine = 0;
        try {
            new PostgreSQLHandler(postgreSQL1Engine);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = "QueryClient could not register PostgreSQL handler!";
            System.err.println(msg);
            System.exit(1);
        }
//		new AccumuloHandler();
        try {
            new SciDBHandler();
        } catch (SQLException e) {
            e.printStackTrace();
            //log.error(e.getMessage() + " " + StackTrace.getFullStackTrace(e));
        }
    }

    /**
     * Main method. Starts the Catalog, Monitor, Migrator, and HTTP server.
     * <p>
     * Requires Catalog database
     *
     * @param args ip address on which the grizzly server should
     *             wait for requests. If empty, then use the configured base uri.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Logger
        LoggerSetup.setLogging();
        logger = Logger.getLogger(Main.class);
        logger.info("Starting application ...");

        // Catalog
        logger.info("Connecting to catalog");
        CatalogInstance.INSTANCE.getCatalog();

        logger.info("Checking registered database connections");
        checkDatabaseConnections();
        boolean isConsole = System.console() != null;

        synchronized (Main.class) {
            // Monitor
            relationalTask = new MonitoringTask();
            relationalTask.run();

            // Migrator
            migratorTask = new MigratorTask();

            // Assign the IP address for the HTTP server to listen on
            String ipAddress = null;
            if (args.length > 0) {
                ipAddress = args[0];
                logger.debug("args 0: " + args[0]);
            } else {
                logger.debug("args length: " + args.length);
            }

            // ZooKeeperUtils.registerNodeInZooKeeper();

            // S-Store migration task
            // SStoreMigrationTask sstoreMigration = new SStoreMigrationTask();

            // HTTP server
            server = startServer(ipAddress);
            System.out.println(String.format(
                    "Jersey app started with WADL available at %sapplication.wadl\n" +
                            (isConsole ? "Hit enter to stop it..." : ""),
                    BASE_URI));
        }

        if (isConsole) {
            System.in.read();
            Shutdown();
        } else {
            // Try to catch the shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        Thread.sleep(100);
                        Shutdown();
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            });

            // Infinite loop until killed.
            shutdown = false;
            while (!shutdown) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static synchronized void Shutdown() {
        // Shutdown
        shutdown = true;
        CatalogInstance.INSTANCE.closeCatalog();
        migratorTask.close();
        relationalTask.shutdown();
        server.shutdownNow();
        System.exit(0);
    }
}
