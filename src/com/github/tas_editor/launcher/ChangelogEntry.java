package com.github.tas_editor.launcher;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ChangelogEntry {
	
	public Type type;
	public String text;
	
	public ChangelogEntry(String initString) {
		if(initString.startsWith("-")) initString = initString.replaceFirst("\\-", "");
		try {
			type = Type.valueOf(initString.split(":")[0].trim().toUpperCase());
			text = Arrays.stream(initString.split(":")).skip(1).collect(Collectors.joining(":")).trim();
		} catch(IllegalArgumentException e) { //doesn't match the format
			System.out.println("didn't find type for "+initString.split(":")[0].trim());
			type = null;
			text = initString;
		}
	}
	
	public String toString() {
		if(type == null) return text;
		return type+": "+text;
	}
	
	public static int compare(ChangelogEntry first, ChangelogEntry second) {
		if(first.type == null) return -1;
		if(second.type == null) return 1;
		return first.type.compareTo(second.type);
	}
	
	public enum Type {
		NEW, CHANGE, FIX, REMOVED, INTERNAL; //also directly defines the sorting order
	}

}
