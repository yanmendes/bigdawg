package istc.bigdawg.monitoring;

import istc.bigdawg.executor.plan.QueryExecutionPlan;
import istc.bigdawg.postgresql.PostgreSQLInstance;
import istc.bigdawg.query.QueryClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static istc.bigdawg.postgresql.PostgreSQLHandler.getColumnNames;
import static istc.bigdawg.postgresql.PostgreSQLHandler.getRows;

/**
 * Created by chenp on 11/17/2015.
 */
public class MonitoringTask implements Runnable {
    private static final String RETRIEVE = "SELECT query FROM monitoring WHERE lastRan=(SELECT min(lastRan) FROM monitoring) AND island='%s' ORDER BY RAND() LIMIT 1";
    private static final double MAX_LOAD = 0.7;

    private final String island;
    private final int cores;

    /**
     * Runs in background on each machine. In lean mode, no benchmarks are run except through this.
     *
     * This is currently made with the assumption that each island resides on one machine. To adapt this, would need to add a
     * machine field to the db and choose queries based on that field..
     * @param island
     */
    public MonitoringTask (String island) {
        this.island = island;

        int cores = 1;

        try {
            Process p = Runtime.getRuntime().exec("nproc");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = reader.readLine())!= null) {
                sb.append(line + "\n");
            }
            String result = sb.toString().replaceAll("[^0-9]", "");
            cores = Integer.parseInt(result);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.cores = cores;
    }

    /**
     * Checks whether we are below MAX_CPU. If so, runs a query.
     */
    @Override
    public void run() {
        while(true) {
            if (this.can_add()) {
                try {
                    final String query = this.getQuery();
                    final String island = this.island;
                    if (query != null) {
                        Thread t1 = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    QueryExecutionPlan qep = QueryExecutionPlan.stringToQEP(query);
                                    ArrayList<QueryExecutionPlan> qeps = new ArrayList<>();
                                    qeps.add(qep);
                                    Monitor.runBenchmarks(qeps);
                                } catch (Exception e) {}
                            }
                        });
                        t1.start();
                    }
                } catch (SQLException e) {}
            } else {
                Random r = new Random();
                int duration = r.nextInt(100);
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Checks whether the load average is below some set threshold
     * @return true if it is currently under the threshold. false otherwise.
     */
    private boolean can_add() {
        try {
            Process p = Runtime.getRuntime().exec("uptime | awk '{print $9}'");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = reader.readLine())!= null) {
                sb.append(line + "\n");
            }
            String result = sb.toString().replaceAll("[^0-9.]", "");
            double load = Double.parseDouble(result);
            if (load/this.cores > MAX_LOAD) {
                return false;
            }
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Finds the Least Recently Updated query for the island
     * @return query
     * @throws SQLException
     */
    private String getQuery() throws SQLException {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = PostgreSQLInstance.getConnection();
            st = con.createStatement();
            rs = st.executeQuery(String.format(RETRIEVE, this.island));
            ResultSetMetaData rsmd = rs.getMetaData();
            List<String> colNames = getColumnNames(rsmd);
            List<List<String>> rows = getRows(rs);

            int queryCol = colNames.indexOf("query");
            if (rows.size() > 0){
                List<String> query = rows.get(0);
                if (query.size() > queryCol){
                    return query.get(queryCol);
                }
            }
            return null;
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(QueryClient.class.getName());
            ex.printStackTrace();
            lgr.log(Level.ERROR, ex.getMessage() + "; query: " + RETRIEVE, ex);
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                Logger lgr = Logger.getLogger(QueryClient.class.getName());
                ex.printStackTrace();
                lgr.log(Level.INFO, ex.getMessage() + "; query: " + RETRIEVE, ex);
                throw ex;
            }
        }
    }
}
