/**
 * 
 */
package istc.bigdawg.migration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import istc.bigdawg.LoggerSetup;
import istc.bigdawg.exceptions.MigrationException;
import istc.bigdawg.migration.direct.FromPostgresToSciDB;
import istc.bigdawg.postgresql.PostgreSQLConnectionInfo;
import istc.bigdawg.postgresql.PostgreSQLConnectionInfoTest;
import istc.bigdawg.postgresql.PostgreSQLHandler;
import istc.bigdawg.scidb.SciDBConnectionInfo;
import istc.bigdawg.scidb.SciDBConnectionInfoTest;
import istc.bigdawg.scidb.SciDBHandler;
import istc.bigdawg.utils.Utils;

/**
 * Test the migration from PostgreSQL to SciDB.
 * 
 * @author Adam Dziedzic
 * 
 *         Feb 18, 2016 10:26:20 AM
 */
public class FromPostgresToSciDBTest {

	private FromPostgresToSciDB migrator = new FromPostgresToSciDB();
	private PostgreSQLConnectionInfo conFrom = new PostgreSQLConnectionInfoTest();
	private String fromTable = "region_test_from_13241";
	private SciDBConnectionInfo conTo = new SciDBConnectionInfoTest();
	// private SciDBConnectionInfo conTo = new
	// SciDBConnectionInfo("localhost","1239", "scidb", "mypassw",
	// "/opt/scidb/14.12/bin/");
	// private String toArray =
	// "bigdawg_region_test_from_13241_FromPostgresToSciDBTest";
	private String toArray = "region_test_from_13241";
	private long numberOfRowsPostgres = 0;

	@Before
	/**
	 * Prepare the test data in a table in PostgreSQL.
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void loadDataToPostgres() throws SQLException, IOException {
		LoggerSetup.setLogging();
		PostgreSQLHandler handler = new PostgreSQLHandler(conFrom);
		handler.dropTableIfExists(fromTable);
		handler.createTable("CREATE TABLE " + fromTable
				+ " (r_regionkey BIGINT NOT NULL,r_name CHAR(25) NOT NULL,r_comment VARCHAR(152) NOT NULL)");
		Connection con = PostgreSQLHandler.getConnection(conFrom);
		con.setAutoCommit(false);
		CopyManager cpTo = new CopyManager((BaseConnection) con);
		InputStream input = FromPostgresToSciDBTest.class.getClassLoader()
				.getResourceAsStream("region.csv");
		// FileInputStream input = new FileInputStream(new
		// File("./region.csv"));
		// CHECK IF THE INPUT STREAM CONTAINS THE REQUIRED DATA
		// int size = 384;
		// byte[] buffer = new byte[size];
		// input.read(buffer, 0, size);
		// String in = new String(buffer, StandardCharsets.UTF_8);
		// System.out.println(in);
		numberOfRowsPostgres = cpTo.copyIn(
				"Copy " + fromTable
						+ " from STDIN with (format csv, delimiter '|')",
				input);
		con.commit();
		con.close();
		assertEquals(5, numberOfRowsPostgres);
	}

	@Test
	/**
	 * If the test fails, first check if the target array is already in the
	 * SciDB database.
	 * 
	 * @throws SQLException
	 * @throws MigrationException
	 */
	public void testFromPostgresToSciDBPreparedArray()
			throws SQLException, MigrationException {
		// prepare the target array
		SciDBHandler.dropArrayIfExists(conTo, toArray);
		SciDBHandler handler = new SciDBHandler(conTo);
		handler.executeStatement("create array " + toArray
				+ " <r_regionkey:int64,r_name:string,r_comment:string> [i=0:*,1000000,0]");
		handler.commit();
		handler.close();
		/*
		 * test of the main method
		 */
		migrator.migrate(conFrom, fromTable, conTo, toArray);
		checkNumberOfElements(conTo, toArray);
		// clean: remove the target array
		SciDBHandler.dropArrayIfExists(conTo, toArray);
	}

	/**
	 * Check if the number of loaded elements is correct. This is an internal
	 * method.
	 * 
	 * @param conTo
	 * @param toArray
	 * @throws SQLException
	 */
	private void checkNumberOfElements(SciDBConnectionInfo conTo,
			String toArray) throws SQLException {
		long numberOfCellsSciDB = Utils.getNumberOfCellsSciDB(conTo, toArray);
		assertEquals(numberOfRowsPostgres, numberOfCellsSciDB);
	}

	@Test
	/**
	 * Check if a target array is created when only the target array name is
	 * given.
	 * 
	 * @throws MigrationException
	 * @throws SQLException
	 */
	public void testFromPostgresToSciDBNoTargetArray()
			throws MigrationException, SQLException {
		// make sure that the target array does not exist
		SciDBHandler.dropArrayIfExists(conTo, toArray);
		migrator.migrate(conFrom, fromTable, conTo, toArray);
		checkNumberOfElements(conTo, toArray);
		// drop the created array
		SciDBHandler.dropArrayIfExists(conTo, toArray);
	}

	@Test
	/**
	 * The data should be loaded to the multi-dimensional target array and after
	 * the process the intermediate flat array should be removed.
	 * 
	 * @throws MigrationException
	 * @throws SQLException
	 */
	public void testFromPostgresToSciDBMultiDimensionalTargetArray()
			throws MigrationException, SQLException {
		// prepare the target array
		SciDBHandler.dropArrayIfExists(conTo, toArray);
		SciDBHandler handler = new SciDBHandler(conTo);
		handler.executeStatement("create array " + toArray
				+ " <r_name:string,r_comment:string> [r_regionkey=0:*,1000000,0]");
		handler.commit();
		handler.close();
		/*
		 * test of the main method
		 */
		migrator.migrate(conFrom, fromTable, conTo, toArray);
		checkNumberOfElements(conTo, toArray);
		// clean: remove the target array
		SciDBHandler.dropArrayIfExists(conTo, toArray);
	}

	@After
	/**
	 * Remove the test table for PostgreSQL.
	 * 
	 * @throws SQLException
	 */
	public void removePostgreSQLTestTable() throws SQLException {
		PostgreSQLHandler handler = new PostgreSQLHandler(conFrom);
		handler.dropTableIfExists(fromTable);
	}

}
