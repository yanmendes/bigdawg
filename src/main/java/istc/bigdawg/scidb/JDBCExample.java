package istc.bigdawg.scidb;

import org.scidb.jdbc.IResultSetWrapper;
import org.scidb.jdbc.IStatementWrapper;

import com.google.common.primitives.UnsignedLong;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

class JDBCExample {
	public static void main(String[] args) throws IOException {
		try {
			Class.forName("org.scidb.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Driver is not in the CLASSPATH -> " + e);
		}

		try {
			Connection conn = DriverManager.getConnection("jdbc:scidb://localhost/");
			conn.setAutoCommit(false);
			Statement st = conn.createStatement();
			boolean result = st.execute("load region from '/home/adam/data/tpch/tpch1G/csv/region.sci'");
			System.out.println("result of the load statement: "+result);
//			ResultSet resLoad = st.getResultSet();
//			ResultSet rsRegion = st.executeQuery("select * from region");
//			while(!rsRegion.isAfterLast()) {
//				System.out.println(rsRegion.getString(2));
//				rsRegion.next();
//			}
			conn.commit();
			// create array A<a:string>[x=0:2,3,0, y=0:2,3,0];
			// select * into A from
			// array(A, '[["a","b","c"]["d","e","f"]["123","456","789"]]');
			ResultSet res = st.executeQuery("select * from " + " array(<a:string>[x=0:2,3,0, y=0:2,3,0],"
					+ " '[[\"a\",\"b\",\"c\"][\"d\",\"e\",\"f\"][\"123\",\"456\",\"789\"]]')");
			ResultSetMetaData meta = res.getMetaData();

			System.out.println("Source array name: " + meta.getTableName(0));
			System.out.println(meta.getColumnCount() + " columns:");

			IResultSetWrapper resWrapper = res.unwrap(IResultSetWrapper.class);
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				System.out.println(meta.getColumnName(i) + " - " + meta.getColumnTypeName(i) + " - is attribute:"
						+ resWrapper.isColumnAttribute(i) + "  - is column dimension: " + resWrapper.isColumnDimension(i));
			}
			
//			System.out.println("=====");
//
//			System.out.println("x y a");
//			System.out.println("-----");
			while (!res.isAfterLast()) {
				System.out.println(res.getString("logical_plan"));//.getLong("x") + " " + res.getLong("y") + " " + res.getString("a"));
				res.next();
			}
			res.close();
			
			ResultSet res2 = st.executeQuery("select * from test_waveform_flat");
			res2.close();
			st.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getMessage().contains("does not exist"));
			System.out.println(e.getMessage());
			//System.out.println(e);
		}
		System.exit(0);
	}
}
