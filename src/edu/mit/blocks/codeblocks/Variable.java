package edu.mit.blocks.codeblocks;

import java.util.ArrayList;
import java.util.List;

public class Variable implements Comparable<Variable> {
	public enum VariableType {NUMBER, BOOLEAN, STRING, LIST}
	public enum VariableScope {GLOBAL, LOCAL}
	
	private String name;
	private VariableType type;
	private VariableScope scope;
	private boolean isList;
	
	public Variable(String name, VariableType type, VariableScope scope, boolean isList) {
		this.name = name;
		this.type = type;
		this.scope = scope;
		this.isList = isList;
	}
	
	public Variable(String name, String type, String scope) {
		this.name = name;
		
		if (type.startsWith("number"))
			this.type = VariableType.NUMBER;
		else if (type.startsWith("boolean"))
			this.type = VariableType.BOOLEAN;
		else if (type.startsWith("string"))
			this.type = VariableType.STRING;
		else assert false;
		
		this.isList = type.endsWith("list");
		
		if (scope.equals("global"))
			this.scope = VariableScope.GLOBAL;
		else if (scope.equals("local"))
			this.scope = VariableScope.LOCAL;
		else assert false;
	}
	
	public String getName() {
		return name;
	}
	
	public VariableType getType() {
		return type;
	}
	
	public VariableScope getScope() {
		return scope;
	}
	
	public boolean isList() {
		return isList;
	}
	
	public int compareTo(Variable v) {
		return name.compareTo(v.name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Variable))
			return false;
		Variable v = (Variable)o;
		return name.equals(v.name) && type.equals(v.type) && scope.equals(v.scope) && (isList == v.isList);
	}
	
	@Override
	public int hashCode() {
	    return name.hashCode() + type.hashCode() * (scope.ordinal() + 1) +
	           (isList ? 52933 : 99102);
	}
	
	public String getScopeString() {
		switch(scope) {
		case GLOBAL:
		    return "global";
		case LOCAL:
		    return "local";
		default:
		    return "<unknown scope>";
		}
	}
	
	public String getTypeString() {
		String typeName = null;
		switch(type) {
		case NUMBER:
			typeName = "number";
			break;
		case BOOLEAN:
			typeName = "boolean";
			break;
		case STRING:
			typeName = "string";
			break;
		default:
			typeName = "<unknown type>";
		}
		if (isList)
			typeName += "-list";
		return typeName;
	}

	@Override
	public String toString() {
		String scopeName = getScopeString();
		String typeName = getTypeString();
		return scopeName + "-" + typeName + (isList ? "-" : "-var-") + name;
	}
	
	public List<String> toList() {
		ArrayList<String> strs = new ArrayList<String>();
		strs.add(name);
		strs.add(getTypeString());
		strs.add(getScopeString());
		return strs;
	}
	
	public static Variable fromList(List<?> l) {
		if (l.size() != 3)
			return null;
		String n = getString(l, 0);
		String t = getString(l, 1);
		String s = getString(l, 2);
		if (n == null || t == null || s == null)
			return null;
		return new Variable(n, t, s);
	}
	
	private static String getString(List<?> l, int i) {
		Object o = l.get(i);
		if (!(o instanceof String))
			return null;
		return (String)o;
	}
}
