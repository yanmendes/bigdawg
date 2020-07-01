package istc.bigdawg.properties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public enum BigDawgConfigProperties {
	INSTANCE;

	public static final String PROJECT_NAME = "BigDawg";

	private String grizzlyIpAddress;
	private String grizzlyPort;

	private int postgresSchemaServerDBID;
	private String postgreSQLURL;
	private String postgreSQLUser;
	private String postgreSQLPassword;
	private boolean postgreSQLDropDataSet;

	private String postgreSQLTestHost;
	private String postgreSQLTestPort;
	private String postgreSQLTestDatabase;
	private String postgreSQLTestUser;
	private String postgreSQLTestPassword;

	private String mySQLTestHost;
	private String mySQLTestPort;
	private String mySQLTestDatabase;
	private String mySQLTestUser;
	private String mySQLTestPassword;
	private boolean mySQLDropDataSet;
	private boolean verticaDropDataSet;

	private int accumuloSchemaServerDBID;
	private String accumuloIstanceType;
	private String accumuloIstanceName;
	private String accumuloUser;
	private String accumuloPasswordToken;
	private boolean accumuloDropDataSet;

	private int scidbSchemaServerDBID;
	private String scidbHostname;
	private String scidbPort;
	private String scidbUser;
	private String scidbPassword;
	private String scidbBinPath;
	private boolean scidbDropDataSet;

	private String scidbTestHostname;
	private String scidbTestPort;
	private String scidbTestUser;
	private String scidbTestPassword;
	private String scidbTestBinPath;

	private int sstoreDBID;
	private String sStoreURL;
	private int seaflowDBID;
	private String accumuloShellScript;

	private int tpccDBID;

	private String myriaHost;
	private String myriaPort;
	private String myriaDownloadPort;
	private String myriaContentType;

	private String networkMessagePort;
	private int networkRequestTimeout;
	private int networkDataPort;
	private int networkRetryConnection;

	private String migratorTempDir;
	private String cmigratorDir;

	private String zooKeepers;

	BigDawgConfigProperties() throws AssertionError {
		Properties prop = new Properties();
		String propFileName = "bigdawg-config.properties";
		InputStream inputStream = BigDawgConfigProperties.class.getClassLoader()
				.getResourceAsStream(propFileName);
		if (inputStream != null) {
			try {
				prop.load(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
				throw new AssertionError(e);
			}
		} else {
			FileNotFoundException e = new FileNotFoundException(
					"property file '" + propFileName
							+ "' not found in the classpath");
			e.printStackTrace();
			throw new AssertionError(e);
		}
		this.grizzlyIpAddress = prop.getProperty("main.grizzly.ipaddress");
		this.grizzlyPort = prop.getProperty("main.grizzly.port");

		this.postgresSchemaServerDBID = Integer
				.parseInt(prop.getProperty("main.postgresql.dbid.schema"));
		this.postgreSQLURL = prop.getProperty("main.postgresql.url");
		this.postgreSQLUser = prop.getProperty("main.postgresql.user");
		this.postgreSQLPassword = prop.getProperty("main.postgresql.password");
		this.postgreSQLDropDataSet = Boolean.valueOf(prop.getProperty("main.postgresql.dropdataset"));

		this.postgreSQLTestHost = prop.getProperty("main.postgresql.test.host");
		this.postgreSQLTestPort = prop.getProperty("main.postgresql.test.port");
		this.postgreSQLTestDatabase = prop
				.getProperty("main.postgresql.test.database");
		this.postgreSQLTestUser = prop.getProperty("main.postgresql.test.user");
		this.postgreSQLTestPassword = prop
				.getProperty("main.postgresql.test.password");

		this.mySQLTestHost = prop.getProperty("main.mysql.test.host");
		this.mySQLTestPort = prop.getProperty("main.mysql.test.port");
		this.mySQLTestDatabase = prop
				.getProperty("main.mysql.test.database");
		this.mySQLTestUser = prop.getProperty("main.mysql.test.user");
		this.mySQLTestPassword = prop
				.getProperty("main.mysql.test.password");
		this.mySQLDropDataSet = Boolean.valueOf(prop.getProperty("main.mysql.dropdataset"));
		this.verticaDropDataSet = Boolean.valueOf(prop.getProperty("main.vertica.dropdataset"));

		this.accumuloSchemaServerDBID = Integer
				.parseInt(prop.getProperty("main.accumulo.dbid.schema"));
		this.accumuloIstanceType = prop
				.getProperty("main.accumulo.instanceType");
		this.accumuloIstanceName = prop
				.getProperty("main.accumulo.instanceName");
		this.accumuloUser = prop.getProperty("main.accumulo.user");
		this.accumuloPasswordToken = prop
				.getProperty("main.accumulo.passwordToken");
		this.accumuloShellScript = prop
				.getProperty("main.accumulo.shell.script");
		this.accumuloDropDataSet = Boolean.valueOf(prop.getProperty("main.accumulo.dropdataset"));

		this.sstoreDBID =Integer.parseInt(prop.getProperty("main.sstore.dbid"));
		this.sStoreURL = prop.getProperty("main.sstore.alerturl");
		// this.seaflowDBID =
		// Integer.parseInt(prop.getProperty("main.seaflow.dbid"));
		// this.postgreSQLOceanDBURL =
		// prop.getProperty("main.postgresql.oceandb.url");
		// this.postgreSQLOceanDBUser =
		// prop.getProperty("main.postgresql.oceandb.user");
		// this.postgreSQLOceanDBPassword =
		// prop.getProperty("main.postgresql.oceandb.password");

		this.myriaHost = prop.getProperty("main.myria.host");
		this.myriaPort = prop.getProperty("main.myria.port");
		this.myriaDownloadPort = prop.getProperty("main.myria.downloadport");
		this.myriaContentType = prop.getProperty("main.myria.content.type");

		this.scidbSchemaServerDBID = Integer.parseInt(prop.getProperty("main.scidb.dbid.schema"));
		// this.tpccDBID = Integer.parseInt(prop.getProperty("main.tpcc.dbid"));
		this.scidbHostname = prop.getProperty("main.scidb.hostname");
		this.scidbPort = prop.getProperty("main.scidb.port");
		this.scidbPassword = prop.getProperty("main.scidb.password");
		this.scidbUser = prop.getProperty("main.scidb.user");
		this.scidbBinPath = prop.getProperty("main.scidb.bin_path");
		this.scidbDropDataSet = Boolean.valueOf(prop.getProperty("main.scidb.dropdataset"));

		this.scidbTestHostname = prop.getProperty("main.scidb.test.hostname");
		this.scidbTestPort = prop.getProperty("main.scidb.test.port");
		this.scidbTestPassword = prop.getProperty("main.scidb.test.password");
		this.scidbTestUser = prop.getProperty("main.scidb.test.user");
		this.scidbTestBinPath = prop.getProperty("main.scidb.test.bin_path");

		this.networkMessagePort = prop.getProperty("main.network.message.port");
		this.networkDataPort = Integer.valueOf(prop.getProperty("main.network.data.port"));
		this.networkRequestTimeout = Integer.valueOf(prop.getProperty("main.network.request.timeout"));
		this.networkRetryConnection = Integer.valueOf(prop.getProperty("main.network.retry.connection"));

		this.migratorTempDir = prop.getProperty("main.migrator.temp.dir");
		this.cmigratorDir = prop.getProperty("main.cmigrator.dir");

		this.zooKeepers = prop.getProperty("main.zooKeepers");
	}

	/**
	 * @return the accumuloIstanceType
	 */
	public String getAccumuloIstanceType() {
		return accumuloIstanceType;
	}

	/**
	 * @return the accumuloIstanceType
	 */
	public String getAccumuloIstanceName() {
		return accumuloIstanceName;
	}

	/**
	 * @return the accumuloZooKeepers
	 */
	public String getZooKeepers() {
		return zooKeepers;
	}

	/**
	 * @return the accumuloUser
	 */
	public String getAccumuloUser() {
		return accumuloUser;
	}

	/**
	 * @return the accumuloPasswordToken
	 */
	public String getAccumuloPasswordToken() {
		return accumuloPasswordToken;
	}

	/**
	 * @return the scidbHost
	 */
	public String getScidbHostname() {
		return scidbHostname;
	}

	/**
	 * @return the scidbUser
	 */
	public String getScidbUser() {
		return scidbUser;
	}

	/**
	 * @return the scidbPassword
	 */
	public String getScidbPassword() {
		return scidbPassword;
	}

	/**
	 * @return the postgreSQLUser
	 */
	public String getPostgreSQLUser() {
		return postgreSQLUser;
	}

	/**
	 * @return the postgreSQLPassword
	 */
	public String getPostgreSQLPassword() {
		return postgreSQLPassword;
	}

	public String getPostgreSQLURL() {
		return postgreSQLURL;
	}

	public int getPostgresSchemaServerDBID() {
		return postgresSchemaServerDBID;
	}

	public int getSciDBSchemaServerDBID() {
		return scidbSchemaServerDBID;
	}

	/**
	 * @return the grizzlyIpAddress
	 */
	public String getGrizzlyIpAddress() {
		return grizzlyIpAddress;
	}

	/**
	 * @return the grizzlyPort
	 */
	public String getGrizzlyPort() {
		return grizzlyPort;
	}

	public String getBaseURI() {
		String baseURI = "http://"
				+ BigDawgConfigProperties.INSTANCE.getGrizzlyIpAddress() + ":"
				+ BigDawgConfigProperties.INSTANCE.getGrizzlyPort()
				+ "/bigdawg/";
		return baseURI;
	}

	public String getBaseURI(String ipAddress) {
		String baseURI = "http://" + ipAddress + ":"
				+ BigDawgConfigProperties.INSTANCE.getGrizzlyPort()
				+ "/bigdawg/";
		return baseURI;
	}

	public String getsStoreURL() {
		return sStoreURL;
	}

	/**
	 * @return the accumuloShellScript
	 */
	public String getAccumuloShellScript() {
		return accumuloShellScript;
	}

	/**
	 * @return the myriaHost
	 */
	public String getMyriaHost() {
		return myriaHost;
	}

	public String getMyriaDownloadPort() {
		return myriaDownloadPort;
	}

	/**
	 * @return the myriaPort
	 */
	public String getMyriaPort() {
		return myriaPort;
	}

	/**
	 * @return the myriaContentType
	 */
	public String getMyriaContentType() {
		return myriaContentType;
	}

	/**
	 * 
	 * @return SciDB bin path
	 */
	public String getScidbBinPath() {
		return scidbBinPath;
	}

	/**
	 * @return the scidbPort
	 */
	public String getScidbPort() {
		return scidbPort;
	}

	/**
	 * @return the postgreSQLTestHost
	 */
	public String getPostgreSQLTestHost() {
		return postgreSQLTestHost;
	}

	/**
	 * @return the postgreSQLTestPort
	 */
	public String getPostgreSQLTestPort() {
		return postgreSQLTestPort;
	}

	/**
	 * @return the postgreSQLTestDatabase
	 */
	public String getPostgreSQLTestDatabase() {
		return postgreSQLTestDatabase;
	}

	/**
	 * @return the postgreSQLTestUser
	 */
	public String getPostgreSQLTestUser() {
		return postgreSQLTestUser;
	}

	/**
	 * @return the postgreSQLTestPassword
	 */
	public String getPostgreSQLTestPassword() {
		return postgreSQLTestPassword;
	}

	/**
	 * @return the mySQLTestHost
	 */
	public String getMySQLTestHost() { return mySQLTestHost; }

	/**
	 * @return the mySQLTestPort
	 */
	public String getMySQLTestPort() {
		return mySQLTestPort;
	}

	/**
	 * @return the mySQLTestDatabase
	 */
	public String getMySQLTestDatabase() {
		return mySQLTestDatabase;
	}

	/**
	 * @return the mySQLTestUser
	 */
	public String getMySQLTestUser() {
		return mySQLTestUser;
	}

	/**
	 * @return the mySQLTestPassword
	 */
	public String getMySQLTestPassword() {
		return mySQLTestPassword;
	}

	/**
	 * @return the scidbTestHostname
	 */
	public String getScidbTestHostname() {
		return scidbTestHostname;
	}

	/**
	 * @return the scidbTestPort
	 */
	public String getScidbTestPort() {
		return scidbTestPort;
	}

	/**
	 * @return the scidbTestUser
	 */
	public String getScidbTestUser() {
		return scidbTestUser;
	}

	/**
	 * @return the scidbTestPassword
	 */
	public String getScidbTestPassword() {
		return scidbTestPassword;
	}

	/**
	 * @return the scidbTestBinPath
	 */
	public String getScidbTestBinPath() {
		return scidbTestBinPath;
	}

	/**
	 * @return DBID of the only sstore associated with this instance.
	 */
	public Integer getSStoreDBID() {
		return sstoreDBID;
	}

	public Integer getSeaflowDBID() {
		return seaflowDBID;
	}

	public Integer getTpccDBID() {
		return tpccDBID;
	}

	/**
	 * @return the networkMessagePort
	 */
	public String getNetworkMessagePort() {
		return networkMessagePort;
	}

	/**
	 * @return the timeout in ms: how long should we wait for reply (this if for
	 *         heart beat message to check if a remote machine is up and
	 *         running)
	 */
	public int getNetworkRequestTimeout() {
		return networkRequestTimeout;
	}

	/**
	 * @return get the temporary dir where the pipes for migration can be
	 *         created
	 */
	public String getMigratorTempDir() {
		return migratorTempDir;
	}

	public String getCmigratorDir() {
		return cmigratorDir;
	}

	/**
	 * @return the networkDataPort Get the port number through which a big data
	 *         will be sent/received.
	 */
	public int getNetworkDataPort() {
		return networkDataPort;
	}

	/**
	 * @return the networkRetryConnection
	 */
	public int getNetworkRetryConnection() {
		return networkRetryConnection;
	}

	public int getAccumuloSchemaServerDBID() {
		return accumuloSchemaServerDBID;
	}

	public boolean isPostgreSQLDropDataSet() {
		return postgreSQLDropDataSet;
	}

	public boolean isMySQLDropDataSet() {
		return mySQLDropDataSet;
	}

	public boolean isAccumuloDropDataSet() {
		return accumuloDropDataSet;
	}

	public boolean isScidbDropDataSet() {
		return scidbDropDataSet;
	}

	public boolean isVerticaDropDataSet() {
		return verticaDropDataSet;
	}
}
