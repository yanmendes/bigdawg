package istc.bigdawg.islands.SciDB.operators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import istc.bigdawg.islands.OperatorVisitor;
import istc.bigdawg.islands.SciDB.SciDBArray;
import istc.bigdawg.islands.operators.Merge;
import istc.bigdawg.islands.operators.Operator;
import istc.bigdawg.schema.DataObjectAttribute;

public class SciDBIslandMerge extends SciDBIslandOperator implements Merge {

//	public enum MergeType {Intersect, Union};
	
	private boolean isUnionAll = true; 
	
	
	
//	public SciDBIslandMerge(Map<String, String> parameters, List<String> output, List<SciDBIslandOperator> childs, SQLTableExpression supplement) throws Exception  {
//		super(parameters, output, childs, supplement);
//
//		isBlocking = true;
//		blockerCount++;
//		this.blockerID = blockerCount;
//
//		// Union ALL is set by the parent Aggregate or Unique
//		
//		if (children.get(0) instanceof SciDBIslandJoin) {
//			outSchema = new LinkedHashMap<>();
//			for(int i = 0; i < output.size(); ++i) {
//				String expr = output.get(i);
//				SQLOutItem out = new SQLOutItem(expr, childs.get(0).outSchema, supplement); // TODO CHECK THIS TODO
//				SQLAttribute attr = out.getAttribute();
//				String attrName = attr.getName();
//				outSchema.put(attrName, attr);
//			}
//		} else {
//			outSchema = new LinkedHashMap<>(childs.get(0).outSchema);
//		}
//		
//	}
	
	// for AFL
	public SciDBIslandMerge(Map<String, String> parameters, SciDBArray output, List<Operator> childs) throws Exception  {
		super(parameters, output, childs);

		isBlocking = true;
		blockerCount++;
		this.blockerID = blockerCount;

		outSchema = new LinkedHashMap<String, DataObjectAttribute>(((SciDBIslandOperator)childs.get(0)).outSchema);
	}
	
	public SciDBIslandMerge(SciDBIslandOperator o, boolean addChild) throws Exception {
		super(o, addChild);
		SciDBIslandMerge s = (SciDBIslandMerge) o;
		
		this.isUnionAll = s.isUnionAll;
		this.blockerID = s.blockerID;
	}
	
	
	public String toString() {
		if (isUnionAll) return "Union operator with UNION ALL option";
		else return "Union operator";
	}
	
	
	@Override
	public void accept(OperatorVisitor operatorVisitor) throws Exception {
		operatorVisitor.visit(this);
	}
	
	@Override
	public String getTreeRepresentation(boolean isRoot) throws Exception{
		
		StringBuilder sb = new StringBuilder();
		sb.append('{').append("union");
		for (Operator o : children) {
			sb.append(o.getTreeRepresentation(false));
		}
		sb.append('}');
		
		
		return sb.toString();
	}
	
	@Override
	public Map<String, Set<String>> getObjectToExpressionMappingForSignature() throws Exception{

		Map<String, Set<String>> out = children.get(0).getObjectToExpressionMappingForSignature();
		
		for (int i = 1 ; i < children.size() ; i++) {
			Map<String, Set<String>> temp = children.get(i).getObjectToExpressionMappingForSignature();
			for (String s : temp.keySet()) {
				if (out.containsKey(s)) 
					out.get(s).addAll(temp.get(s));
				else 
					out.put(s, temp.get(s));
			}
		}
		return out;
	}

	public boolean setUnionAll(boolean isUnionAll) {
		return this.isUnionAll = isUnionAll;
	}

	public boolean isUnionAll() {
		return this.isUnionAll;
	}
	
};