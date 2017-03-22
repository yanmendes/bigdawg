package istc.bigdawg.islands;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;

import istc.bigdawg.accumulo.AccumuloConnectionInfo;
import istc.bigdawg.accumulo.AccumuloExecutionEngine;
import istc.bigdawg.catalog.Catalog;
import istc.bigdawg.catalog.CatalogViewer;
import istc.bigdawg.exceptions.BigDawgCatalogException;
import istc.bigdawg.exceptions.BigDawgException;
import istc.bigdawg.exceptions.IslandException;
import istc.bigdawg.exceptions.UnsupportedIslandException;
import istc.bigdawg.executor.QueryResult;
import istc.bigdawg.islands.Myria.MyriaQueryParser;
import istc.bigdawg.islands.SStore.SStoreQueryParser;
import istc.bigdawg.islands.SciDB.AFLPlanParser;
import istc.bigdawg.islands.SciDB.AFLQueryPlan;
import istc.bigdawg.islands.SciDB.ArrayIsland;
import istc.bigdawg.islands.SciDB.operators.SciDBIslandJoin;
import istc.bigdawg.islands.operators.Join;
import istc.bigdawg.islands.operators.Join.JoinType;
import istc.bigdawg.islands.operators.Operator;
import istc.bigdawg.islands.relational.RelationalIsland;
import istc.bigdawg.islands.relational.SQLPlanParser;
import istc.bigdawg.islands.relational.SQLQueryPlan;
import istc.bigdawg.islands.relational.operators.SQLIslandJoin;
import istc.bigdawg.islands.text.AccumuloJSONQueryParser;
import istc.bigdawg.islands.text.TextIsland;
import istc.bigdawg.islands.text.operators.TextScan;
import istc.bigdawg.myria.MyriaHandler;
import istc.bigdawg.postgresql.PostgreSQLConnectionInfo;
import istc.bigdawg.postgresql.PostgreSQLHandler;
import istc.bigdawg.properties.BigDawgConfigProperties;
import istc.bigdawg.query.ConnectionInfo;
import istc.bigdawg.query.DBHandler;
import istc.bigdawg.scidb.SciDBConnectionInfo;
import istc.bigdawg.scidb.SciDBHandler;
import istc.bigdawg.shims.ArrayToSciDBShim;
import istc.bigdawg.shims.RelationalToPostgresShim;
import istc.bigdawg.shims.RelationalToSciDBShim;
import istc.bigdawg.shims.Shim;
import istc.bigdawg.shims.TextToAccumuloShim;
import istc.bigdawg.signature.builder.ArraySignatureBuilder;
import istc.bigdawg.signature.builder.RelationalSignatureBuilder;
import istc.bigdawg.sstore.SStoreSQLConnectionInfo;
import istc.bigdawg.sstore.SStoreSQLHandler;
import net.sf.jsqlparser.JSQLParserException;

/**
 * @author Jack
 * 
 * This class should contain only static objects and functions. 
 * It is intended to contain all functions that do different things when the island context is different.
 * Island writers should go through each of these functions to add supports for their islands
 * 
 * NOTE: all relational island queries are assumed to be base in PostgreSQL. 
 *       This assumption needs to change as new engines join Relational Island 
 *
 */
public class IslandAndCastResolver {
	
	public static enum Engine {
		PostgreSQL, SciDB, SStore, Accumulo, Myria
	};
	
	public enum Scope {
		RELATIONAL, ARRAY, KEYVALUE, TEXT, GRAPH, DOCUMENT, STREAM, CAST, MYRIA 
	}

	public static final int  psqlSchemaHandlerDBID = BigDawgConfigProperties.INSTANCE.getPostgresSchemaServerDBID();
	public static final int  scidbSchemaHandlerDBID = BigDawgConfigProperties.INSTANCE.getSciDBSchemaServerDBID();
	public static final int  sstoreDBID = BigDawgConfigProperties.INSTANCE.getSStoreDBID();
	public static final int  accumuloSchemaHandlerDBID = BigDawgConfigProperties.INSTANCE.getAccumuloSchemaServerDBID(); 
	
	private static final Pattern predicatePattern = Pattern.compile("(?<=\\()([^\\(^\\)]+)(?=\\))");
	
	public static Pattern QueryParsingPattern	= Pattern.compile("(?i)(bdrel\\(|bdarray\\(|bdkv\\(|bdtext\\(|bdgraph\\(|bddoc\\(|bdstream\\(|bdmyria\\(|bdcast\\(|\\(|\\))");
	public static Pattern ScopeStartPattern		= Pattern.compile("^((bdrel\\()|(bdarray\\()|(bdkv\\()|(bdtext\\()|(bdgraph\\()|(bddoc\\()|(bdstream\\()|(bdmyria\\()|(bdcast\\())");
	public static Pattern ScopeEndPattern		= Pattern.compile("\\) *;? *$");
	public static Pattern CastScopePattern		= Pattern.compile("(?i)(relational|array|keyvalue|text|graph|document|stream|myria)\\) *;? *$");
	public static Pattern CastSchemaPattern		= Pattern.compile("(?<=([_a-z0-9, ]+')).*(?=(' *, *(relational|array|keyvalue|text|graph|document|stream|myria)))");
	public static Pattern CastNamePattern		= Pattern.compile("(?<=(, ))([_@0-9a-zA-Z]+)(?=, *')");
	
	
//	public static List<String> sqlEngineTokenList = new ArrayList<>();
//	
//	static {
//		sqlEngineTokenList.add("postgres");
//		sqlEngineTokenList.add("sstore");
//	}
	/**
	 * For CrossIslandQueryPlan
	 * Determines whether an island is implemented
	 * @param scope
	 * @return
	 */
	public static boolean isOperatorBasedIsland(Scope scope) throws UnsupportedIslandException {
		switch (scope) {
		case ARRAY:
		case RELATIONAL:
		case TEXT:
			return true;
		case DOCUMENT:
		case GRAPH:
		case KEYVALUE:
		case STREAM:
		case MYRIA:
			return false;
		default:
			throw new UnsupportedIslandException(scope, "isOperatorBasedIsland");
		}
	}
	
	/**
	 * Used in CatalogViewer
	 * @param engineString
	 * @return
	 * @throws BigDawgException
	 */
	public static IslandAndCastResolver.Engine getEngineEnum(String engineString) throws BigDawgException {
		if (engineString.startsWith(IslandAndCastResolver.Engine.PostgreSQL.name()))
			return IslandAndCastResolver.Engine.PostgreSQL;
		else if (engineString.startsWith(IslandAndCastResolver.Engine.SciDB.name()))
			return IslandAndCastResolver.Engine.SciDB;
		else if (engineString.startsWith(IslandAndCastResolver.Engine.SStore.name()))
			return IslandAndCastResolver.Engine.SStore;
		else if (engineString.startsWith(IslandAndCastResolver.Engine.Accumulo.name()))
			return IslandAndCastResolver.Engine.Accumulo;
		else {
			throw new BigDawgException("Unsupported engine: "+ engineString);
		}
	}
	
	/**
	 * Used in CatalogViewer
	 * @param cc
	 * @param e
	 * @param dbid
	 * @return
	 * @throws SQLException
	 * @throws BigDawgCatalogException
	 */
	public static ConnectionInfo getQConnectionInfo(Catalog cc, IslandAndCastResolver.Engine e, int dbid) throws SQLException, BigDawgCatalogException {
		
		ConnectionInfo extraction = null;
		ResultSet rs2 = null;
		
		try {
			switch (e) {
			case PostgreSQL:
				rs2 = cc.execRet("select dbid, eid, host, port, db.name as dbname, userid, password "
						+ "from catalog.databases db "
						+ "join catalog.engines e on db.engine_id = e.eid "
						+ "where dbid = "+dbid);
				if (rs2.next())
					extraction = new PostgreSQLConnectionInfo(rs2.getString("host"), rs2.getString("port"),rs2.getString("dbname"), rs2.getString("userid"), rs2.getString("password"));
				break;
			case SciDB:
				rs2 = cc.execRet("select dbid, db.engine_id, host, port, bin_path, userid, password "
						+ "from catalog.databases db "
						+ "join catalog.engines e on db.engine_id = e.eid "
						+ "join catalog.scidbbinpaths sp on db.engine_id = sp.eid where dbid = "+dbid);
				if (rs2.next())
					extraction = new SciDBConnectionInfo(rs2.getString("host"), rs2.getString("port"), rs2.getString("userid"), rs2.getString("password"), rs2.getString("bin_path"));
					break;
			case SStore:
				rs2 = cc.execRet("select dbid, eid, host, port, db.name as dbname, userid, password "
						+ "from catalog.databases db "
						+ "join catalog.engines e on db.engine_id = e.eid "
						+ "where dbid = "+dbid);
				if (rs2.next())
					extraction = new SStoreSQLConnectionInfo(rs2.getString("host"), rs2.getString("port"),rs2.getString("dbname"), rs2.getString("userid"), rs2.getString("password"));
				break;
			case Accumulo:
				rs2 = cc.execRet("select dbid, eid, host, port, db.name as dbname, userid, password "
						+ "from catalog.databases db "
						+ "join catalog.engines e on db.engine_id = e.eid "
						+ "where dbid = "+dbid);
				if (rs2.next())
					extraction = new AccumuloConnectionInfo(rs2.getString("host"), rs2.getString("port"),rs2.getString("dbname"), rs2.getString("userid"), rs2.getString("password"));
				break;
			default:
				throw new BigDawgCatalogException("This is not supposed to happen");
			}
			
			if (extraction == null) {
				rs2.close();
				throw new BigDawgCatalogException("Connection Info Cannot Be Formulated: "+dbid);
			}
				
		} catch (SQLException ex) {
			cc.rollback();
			throw ex;
		} finally {
			if (rs2 != null) {
				rs2.close();
			}
		}
		
		return extraction;
	}
	
	/**
	 * For Executor.
	 * @param scope
	 * @param remainderDBID 
	 * @return Instance of a query generator
	 * @throws BigDawgException 
	 * @throws SQLException 
	 */
	public static Shim getShim(Scope scope, int dbid) throws BigDawgException, SQLException {
		
		IslandAndCastResolver.Engine e = CatalogViewer.getEngineOfDB(dbid);
		
		switch (scope) {
		case ARRAY:
			switch (e) {
			case SciDB:
				return new ArrayToSciDBShim();
			default:
				break;
			}
			throw new BigDawgException("getQueryGenerator fails for scope: "+scope.name()+" and dbid: "+dbid);
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			switch (e) {
			case PostgreSQL:
				return new RelationalToPostgresShim();
			case SciDB:
				return new RelationalToSciDBShim();
			default:
				break;
			}
			throw new BigDawgException("getQueryGenerator fails for scope: "+scope.name()+" and dbid: "+dbid);
		case STREAM:
			break;
		case TEXT:
			return new TextToAccumuloShim();
		case MYRIA:
			throw new BigDawgException("MYRIA island does not support the concept of generator; getQueryGenerator");
		default:
			break;
		}
		throw new BigDawgException("getQueryGenerator fails for scope: "+scope.name()+" and dbid: "+dbid);
	}
	
	/**
	 * For Catalog
	 * @param scope
	 * @return A string in the form of " AND scope_name = 'RELATIONAL' ", where RELATIONAL is replaced by the island reference in the Catalog 
	 * @throws UnsupportedIslandException
	 */
	public static String getCatalogIslandSelectionPredicate(Scope scope) throws UnsupportedIslandException {
		switch (scope) {
		case ARRAY:
			return "ARRAY";
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			return "RELATIONAL";
		case STREAM:
			return "STREAM";
		case TEXT:
			return "TEXT";
		case MYRIA:
			return "MYRIA";
		default:
			break;
		}
		throw new UnsupportedIslandException(scope, "getCatalogIslandSelectionPredicate");
	}
	
	/**
	 * For query planning, CrossIslandQueryNode
	 * This function is used to create temporary tables in schema engines, where the temporary tables represent intermediate results from other islands
	 * @param sourceScope
	 * @param children
	 * @param transitionSchemas
	 * @return
	 * @throws SQLException
	 * @throws BigDawgException 
	 * @throws AccumuloSecurityException 
	 * @throws AccumuloException 
	 * @throws TableExistsException 
	 * @throws TableNotFoundException 
	 */
	@Deprecated
	public static DBHandler createTableForPlanning(Scope sourceScope, Set<String> children, Map<String, String> transitionSchemas) throws SQLException, BigDawgException, AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {

		DBHandler dbSchemaHandler = null;
		ConnectionInfo connectionInfo = null;
		Set<String> createdTables = new HashSet<>();
		switch (sourceScope) {
		case ARRAY:
			createdTables = new HashSet<>();
			connectionInfo = CatalogViewer.getConnectionInfo(scidbSchemaHandlerDBID);
			
			for (String key : transitionSchemas.keySet()) 
				if (children.contains(key)) {
					try {
						createdTables.add(key);
						dbSchemaHandler = new SciDBHandler(connectionInfo);
						((SciDBHandler)dbSchemaHandler).executeStatementAQL(transitionSchemas.get(key));
//						((SciDBHandler)dbSchemaHandler).commit();
//						((SciDBHandler)dbSchemaHandler).close();
					} catch (Exception e) {
						for (String s : createdTables) {
							SciDBHandler.dropArrayIfExists(connectionInfo, s);
						}
						throw e;
					}
				}
			if (dbSchemaHandler == null)
				dbSchemaHandler = new SciDBHandler(connectionInfo);
			break;
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			dbSchemaHandler = new PostgreSQLHandler((PostgreSQLConnectionInfo)CatalogViewer.getConnectionInfo(psqlSchemaHandlerDBID));
			createdTables = new HashSet<>();
			for (String key : transitionSchemas.keySet()) {
				if (children.contains(key)) {
					try {
						createdTables.add(key);
						((PostgreSQLHandler)dbSchemaHandler).executeStatementPostgreSQL(transitionSchemas.get(key));
					} catch (Exception e) {
						for (String s : createdTables) {
							((PostgreSQLHandler)dbSchemaHandler).dropDataSetIfExists(s);
						}
						throw e;
					}
				}
			}
				
			break;
		case STREAM:
			throw new BigDawgException("STREAM island does not support data immigration; createTableForPlanning");
		case TEXT:
			createdTables = new HashSet<>();
			dbSchemaHandler = new AccumuloExecutionEngine(CatalogViewer.getConnectionInfo(accumuloSchemaHandlerDBID));
			for (String key : transitionSchemas.keySet()) 
				if (children.contains(key)) {
					try {
						createdTables.add(key);
						((AccumuloExecutionEngine)dbSchemaHandler).createTable(key);
					} catch (Exception e) {
						for (String s : createdTables) {
							((AccumuloExecutionEngine)dbSchemaHandler).dropDataSetIfExists(s);
						} 
						throw e;
					}
				}
			break;
//			throw new BigDawgException("TEXT island does not support data immigration; createTableForPlanning");
		case MYRIA:
			throw new BigDawgException("MYRIA island does not support data immigration; createTableForPlanning");
		default:
			break;
		}
		if (dbSchemaHandler == null) throw new UnsupportedIslandException(sourceScope, "createTableForPlanning");
		return dbSchemaHandler;
	}
	
	/**
	 * For query planning, CrossIslandQueryNode
	 * This function is responsible for removing temporary tables created in the name of intermediate results from other islands. 
	 * @param sourceScope
	 * @param dbSchemaHandler
	 * @param children
	 * @param transitionSchemas
	 * @throws SQLException
	 * @throws BigDawgException 
	 * @throws AccumuloSecurityException 
	 * @throws AccumuloException 
	 * @throws TableNotFoundException 
	 */
	@Deprecated
	public static void removeTemporaryTableCreatedForPlanning(Scope sourceScope, DBHandler dbSchemaHandler, Set<String> children, Map<String, String> transitionSchemas) throws SQLException, BigDawgException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
		
		switch (sourceScope) {
		case ARRAY:
			ConnectionInfo sciDBconnectionInfo = CatalogViewer.getConnectionInfo(scidbSchemaHandlerDBID);
			for (String key : transitionSchemas.keySet()) 
				if (children.contains(key)) {
//					dbSchemaHandler = new SciDBHandler(scidbSchemaHandlerDBID); // because now the code closes the connection forcefully each time
					SciDBHandler.dropArrayIfExists(sciDBconnectionInfo, key);
//					((SciDBHandler)dbSchemaHandler).dropDataSetIfExists(key);
//					//((SciDBHandler)dbSchemaHandler).executeStatementAFL("remove("+key+")");
//					//((SciDBHandler)dbSchemaHandler).commit();
				}
			return;
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			for (String key : transitionSchemas.keySet()) 
				if (children.contains(key)) 
					((PostgreSQLHandler)dbSchemaHandler).dropDataSetIfExists(key);
			return;
		case STREAM:
			throw new BigDawgException("STREAM island does not support data immigration; removeTemporaryTableCreatedForPlanning");
		case TEXT:
			dbSchemaHandler = new AccumuloExecutionEngine(CatalogViewer.getConnectionInfo(accumuloSchemaHandlerDBID));
			for (String key : transitionSchemas.keySet()) 
				if (children.contains(key)) {
					((AccumuloExecutionEngine)dbSchemaHandler).dropDataSetIfExists(key);
				}
			return;
		case MYRIA:
			throw new BigDawgException("MYRIA island does not support data immigration; removeTemporaryTableCreatedForPlanning");
		default:
			break;
		}
		
		throw new UnsupportedIslandException(sourceScope, "removeTemporaryTableCreatedForPlanning");
	}
	
	/**
	 * For query planning, CrossIslandQueryNode
	 * @param scope
	 * @return
	 * @throws BigDawgException 
	 */
	@Deprecated
	public static Integer getSchemaEngineDBID(Scope scope) throws BigDawgException {
		
		switch (scope) {
		case ARRAY:
			return scidbSchemaHandlerDBID;
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			return psqlSchemaHandlerDBID;
		case STREAM:
			throw new BigDawgException("STREAM island does not have a default SchemaEngine, getSchemaEngineDBID");
		case TEXT:
			return accumuloSchemaHandlerDBID;
		case MYRIA:
			throw new BigDawgException("MYRIA island does not have a default SchemaEngine, getSchemaEngineDBID");
		default:
			break;
		}
		
		throw new UnsupportedIslandException(scope, "getSchemaEngineDBID");
	}
	
	/**
	 * For query planning, CrossIslandQueryNode 
	 * Construct a join operator using limited information; used in creating permutations 
	 * @param scope
	 * @param o1, left operator
	 * @param o2, right operator
	 * @param jt, type of join
	 * @param joinPred, join predicate, could be null
	 * @param isFilter, whether it originally present as a joinFilter and not joinPredicate (distinction unclear)
	 * @return
	 * @throws JSQLParserException 
	 * @throws BigDawgException 
	 * @throws Exception
	 */
	@Deprecated
	public static Join constructJoin (Scope scope, Operator o1, Operator o2, JoinType jt, List<String> joinPred, boolean isFilter) throws JSQLParserException, BigDawgException {
		
		switch (scope) {
		case ARRAY:
			if (joinPred == null) return new SciDBIslandJoin().construct(o1, o2, jt, null, isFilter);
			return new SciDBIslandJoin().construct(o1, o2, jt, String.join(", ", joinPred.stream().map(s -> s.replaceAll("[<>=]+", ", ")).collect(Collectors.toSet())), isFilter);
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			if (joinPred == null) return new SQLIslandJoin(o1, o2, jt, null, isFilter);
			return new SQLIslandJoin(o1, o2, jt, String.join(" AND ", joinPred), isFilter);
		case STREAM:
			throw new BigDawgException("STREAM island does not support the concept of Join; constructJoin");
		case TEXT:
			throw new BigDawgException("TEXT island does not support the concept of Join; constructJoin");
		case MYRIA:
			throw new BigDawgException("MYRIA island does not support the concept of Join; constructJoin");
		default:
			break;
		}
		
		throw new UnsupportedIslandException(scope, "constructJoin");
	}
	
	/**
	 * For query planning, CrossIslandQueryNode
	 * Used in signature creation. Sig2 means all tables involved in a query.
	 * @param scope
	 * @param dbSchemaHandler
	 * @param queryString
	 * @param objs
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public static Operator generateOperatorTreesAndAddDataSetObjectsSignature(Scope scope, DBHandler dbSchemaHandler, String queryString, List<String> objs) throws Exception {
		
		Operator root = null;
		
		switch (scope) {
		case ARRAY:
			AFLQueryPlan arrayQueryPlan = AFLPlanParser.extractDirect((SciDBHandler)dbSchemaHandler, queryString);
			root = arrayQueryPlan.getRootNode();
			objs.addAll(ArraySignatureBuilder.sig2(queryString));
			break;
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			SQLQueryPlan relQueryPlan = SQLPlanParser.extractDirectFromPostgreSQL((PostgreSQLHandler)dbSchemaHandler, queryString);
			root = relQueryPlan.getRootNode();
			objs.addAll(RelationalSignatureBuilder.sig2(queryString));
			break;
		case STREAM:
			throw new BigDawgException("STREAM island does not support signature; generateOperatorTreesAndAddDataSetObjectsSignature");
		case TEXT:
			root = (new AccumuloJSONQueryParser()).parse(queryString);
			if (!(root instanceof TextScan))
				throw new BigDawgException("TEXT island does not support operator "+root.getClass().getName()+"; generateOperatorTreesAndAddDataSetObjectsSignature");
			AccumuloExecutionEngine.addExecutionTree(root.getSubTreeToken(), root);
			objs.add(((TextScan)root).getSourceTableName());
			break;
		case MYRIA:
			throw new BigDawgException("MYRIA island does not support signature; generateOperatorTreesAndAddDataSetObjectsSignature");
		default:
			break;
		}
		
		if (root == null) throw new UnsupportedIslandException(scope, "generateOperatorTrees");
		return root;
	} 
	
	/**
	 * For query planning, CrossIslandQueryNode
	 * @return
	 * @throws BigDawgException 
	 */
	@Deprecated
	public static Set<String> splitPredicates(Scope scope, String predicates) throws IslandException {
		
		Set<String> results = new HashSet<>();

		String joinDelim = null;

		switch (scope) {
		case ARRAY:
			joinDelim = "[,=]";
			break;
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			joinDelim = "[=<>]+";
			break;
		case STREAM:
			throw new IslandException("STREAM island does not participate in splitPredicates function; splitPredicates");
		case TEXT:
			throw new IslandException("TEXT island does not participate in splitPredicates function; splitPredicates");
		case MYRIA:
			throw new IslandException("MYRIA island does not participate in splitPredicates function; splitPredicates");
		default:
			break;
		}
		
		if (joinDelim == null) throw new UnsupportedIslandException(scope, "splitPredicates");

		Matcher m = predicatePattern.matcher(predicates);
		while (m.find()){
			String current = m.group().replace(" ", "");
			String[] filters = current.split(joinDelim);
			Arrays.sort(filters);
			String result = String.join(joinDelim, filters);
			results.add(result);
		}
		return results;
	}
	
	/**
	 * For monitoring, signature
	 * Sig3 consist of constants and other literals used in the query.
	 * @param scope
	 * @param query
	 * @return
	 * @throws IOException
	 * @throws BigDawgException 
	 */
	@Deprecated
	public static List<String> getLiteralsAndConstantsSignature(Scope scope, String query) throws IOException, BigDawgException {
		
		switch (scope) {
		case ARRAY:
			return ArraySignatureBuilder.sig3(query);
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			return RelationalSignatureBuilder.sig3(query);
		case STREAM:
			throw new BigDawgException("STREAM island does not support signature; getLiteralsAndConstantsSignature");
		case TEXT:
			List<String> ret = new ArrayList<>();
			ret.add(query);
			return ret;
		case MYRIA:
			throw new BigDawgException("MYRIA island does not support signature; getLiteralsAndConstantsSignature");
		default:
			break;
		}

		throw new UnsupportedIslandException(scope, "getDataSetObjectsSignature");
	}
	
	/**
	 * For monitoring, signature
	 * @param scope
	 * @param query
	 * @return
	 * @throws UnsupportedIslandException
	 */
	@Deprecated
	public static String getIslandStyleQuery(Scope scope, String query) throws UnsupportedIslandException {
		
		switch (scope) {
		case ARRAY:
			return String.format("bdarray(%s);", query);
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			return String.format("bdrel(%s);", query);
		case STREAM:
			return String.format("bdstream(%s);", query);
		case TEXT:
			return String.format("bdtext(%s);", query);
		case MYRIA:
			return String.format("bdmyria(%s);", query);
		default:
			break;
		}
			
		throw new UnsupportedIslandException(scope, "getIslandStyleQuery");
	}
	
	/**
	 * For CrossIslandCastNode
	 * @param scope
	 * @param name
	 * @param schemaCreationQuery
	 * @return
	 * @throws BigDawgException 
	 */
	@Deprecated
	public static String getCreationQueryForCast(Scope scope, String name, String schemaCreationQuery) throws BigDawgException {
		
		switch (scope) {
		case ARRAY:
			return String.format("CREATE ARRAY %s %s", name, schemaCreationQuery);
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			return String.format("CREATE TABLE %s %s", name, schemaCreationQuery);
		case STREAM:
			throw new BigDawgException("STREAM Island does not allow you to create new tables; getCreationQueryForCast");
		case TEXT:
			AccumuloExecutionEngine.addExecutionTree(AccumuloExecutionEngine.AccumuloCreateTableCommandPrefix+name, null);
			return name;
//			throw new BigDawgException("TEXT Island does not allow you to create new tables; getCreationQueryForCast");
		case MYRIA:
			throw new BigDawgException("MYRIA Island does not allow you to create new tables; getCreationQueryForCast");	
		default:
			break;
		}
		
		throw new UnsupportedIslandException(scope, "getCreationQuery");
	}
	
	public static Island getIsland(Scope scope) throws IslandException {
		switch (scope) {
		case ARRAY:
			return new ArrayIsland();
		case CAST:
			break;
		case DOCUMENT:
			break;
		case GRAPH:
			break;
		case KEYVALUE:
			break;
		case RELATIONAL:
			return new RelationalIsland();
		case STREAM:
			break;
		case TEXT:
			return new TextIsland(); 
//			AccumuloExecutionEngine.addExecutionTree(AccumuloExecutionEngine.AccumuloCreateTableCommandPrefix+name, null);
//			return name;
		case MYRIA:
			break;	
		default:
			break;
		}
		
		throw new UnsupportedIslandException(scope, "getCreationQuery");
	}
	
	
	//// island specific
	
	/**
	 * For Relational Island only.
	 * @param e
	 * @param tableName
	 * @return
	 * @throws SQLException
	 * @throws BigDawgCatalogException 
	 * @throws BigDawgException
	 */
	@Deprecated
	public static String getRelationalIslandCreateTableString(String tableName) throws SQLException, BigDawgCatalogException  {
		Connection con = PostgreSQLHandler.getConnection((PostgreSQLConnectionInfo)CatalogViewer.getConnectionInfo(psqlSchemaHandlerDBID));
		return PostgreSQLHandler.getCreateTable(con, tableName).replaceAll("\\scharacter[\\(]", " char(");
	}
	
	//// Operator free options
	
	public static QueryResult runOperatorFreeIslandQuery(CrossIslandNonOperatorNode node) throws Exception {
		
		switch (node.sourceScope) {
		
		case DOCUMENT:
		case GRAPH:
		case KEYVALUE:
			throw new UnsupportedIslandException(node.sourceScope, "runOperatorFreeIslandQuery");
		case STREAM:
			List<String> ssparsed = (new SStoreQueryParser()).parse(node.getQueryString());
			return (new SStoreSQLHandler(sstoreDBID)).executePreparedStatement((new SStoreSQLHandler(sstoreDBID)).getConnection(), ssparsed);
		case MYRIA:
			String input = node.getQueryString();
			List<String> mparsed = (new MyriaQueryParser()).parse(input);
			
			System.out.printf("MYRIA Island query: -->%s<--; parsed form: -->%s<--\n", input, mparsed);
			
			return MyriaHandler.executeMyriaQuery(mparsed);
		case RELATIONAL:
		case ARRAY:
		case TEXT:
		default:
			throw new IslandException("Unapplicable island for runOperatorFreeIslandQuery: "+node.sourceScope.name());
		}
		
	}
	
	
	public static Scope convertFunctionScope (String prefix) throws UnsupportedIslandException {
		switch (prefix) {
		case "bdrel(":
		case "bdrel":
			return Scope.RELATIONAL;
		case "bdarray(":
		case "bdarray":
			return Scope.ARRAY;
		case "bdkv(":
		case "bdkv":
			return Scope.KEYVALUE;
		case "bdtext(":
		case "bdtext":
			return Scope.TEXT;
		case "bdgraph(":
		case "bdgraph":
			return Scope.GRAPH;
		case "bddoc(":
		case "bddoc":
			return Scope.DOCUMENT;
		case "bdstream(":
		case "bdstream":
			return Scope.STREAM;
		case "bdcast(":
		case "bdcast":
			return Scope.CAST;
		case "bdmyria(":
		case "bdmyria":
			return Scope.MYRIA;
		default:
			throw new UnsupportedIslandException(prefix);
		}
		
	}
	
	public static Scope convertDestinationScope (String prefix) throws UnsupportedIslandException {
		switch (prefix.toLowerCase()) {
		case "relational":
			return Scope.RELATIONAL;
		case "array":
			return Scope.ARRAY;
		case "keyvalue":
			return Scope.KEYVALUE;
		case "text":
			return Scope.TEXT;
		case "graph":
			return Scope.GRAPH;
		case "document":
			return Scope.DOCUMENT;
		case "stream":
			return Scope.STREAM;
		case "cast":
			return Scope.CAST;
		case "myria":
			return Scope.MYRIA;
		default:
			throw new UnsupportedIslandException(prefix);
		}
		
	}
}