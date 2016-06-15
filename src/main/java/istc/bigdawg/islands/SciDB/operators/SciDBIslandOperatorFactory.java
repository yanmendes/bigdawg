package istc.bigdawg.islands.SciDB.operators;

import java.util.List;
import java.util.Map;

import istc.bigdawg.islands.SciDB.AFLQueryPlan;
import istc.bigdawg.islands.SciDB.SciDBArray;
import istc.bigdawg.islands.operators.Operator;

public class SciDBIslandOperatorFactory {

//	public static Operator get(String opType, Map<String, String> parameters, List<String> output,  List<String> sortKeys, List<Operator> children, SQLQueryPlan plan, SQLTableExpression supplement) throws Exception {
//	
//		switch (opType) {
//			case "Unique":
//			case "Aggregate":
//				if (children.get(0) instanceof Merge) {
//					((Merge)children.get(0)).setUnionAll(false);
//					return children.get(0);
//				}
//			case "HashAggregate":
//			case "GroupAggregate":
//				if(supplement != null && supplement.hasDistinct()) {
//					return new Distinct(parameters, output, children.get(0), supplement);
//				}
//				return new Aggregate(parameters, output, children.get(0), supplement);
//			case "CTE Scan":
//				return new CommonSQLTableExpressionScan(parameters, output, null, plan, supplement);
//			case "Hash Join":
//			case "Nested Loop":
//			case "Merge Join":
//				return new Join(parameters, output, children.get(0), children.get(1), supplement);
//			case "Index Scan":
//			case "Index Only Scan":
//			case "Subquery Scan":
//			case "Seq Scan":
//				return new SeqScan(parameters, output, null, supplement);
//			case "Sort":
//				return new Sort(parameters, output, sortKeys, children.get(0), supplement);					
//			case "WindowAgg":
//				return new WindowAggregate(parameters, output, children.get(0), supplement);
//			case "Limit":
//				return new Limit(parameters, output, children.get(0), supplement);
//			case "Append":
//				return new Merge(parameters, output, children, supplement);
//			default: // skip it, only designed for 1:1 io like hash and materialize
////				System.out.println("---> opType from OperatorFactory: "+opType);
//				return children.get(0);
//		}
//		
//	}
//	
	public static Operator get(String opType, Map<String, String> parameters, SciDBArray output,  List<String> sortKeys, List<Operator> children, AFLQueryPlan plan) throws Exception {
		
		switch (opType) {
			case "Aggregate":
//			case "HashAggregate":
//			case "GroupAggregate":
//				if(supplement.hasDistinct()) {
//					return new Distinct(parameters, output, children.get(0), supplement);
//				}
				return new SciDBIslandAggregate(parameters, output, children.get(0));
//			case "CTE Scan":
//				return new CommonSQLTableExpressionScan(parameters, output, null, plan, supplement);
			case "Cross Join":
				return new SciDBIslandJoin(parameters, output, children.get(0), children.get(1));
			case "Seq Scan":
				if (children.isEmpty())
					return new SciDBIslandSeqScan(parameters, output, null);
				else 
					return new SciDBIslandSeqScan(parameters, output, children.get(0));
			case "Sort":
				return new SciDBIslandSort(parameters, output, sortKeys, children.get(0));					
			case "WindowAgg":
				return new SciDBIslandWindowAggregate(parameters, output, children.get(0));
				
			default: // skip it, only designed for 1:1 io like hash and materialize
				System.out.println("Factory default trigger: "+opType);
				return children.get(0);
		}
		
	}
	
}
