package ru.catssoftware.gameserver.util;

public class StringUtil {
	public static void append(StringBuilder builder, String...strings) {
		for(String s : strings)
			builder.append(s+" ");
	}
}
