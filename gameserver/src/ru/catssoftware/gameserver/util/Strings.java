package ru.catssoftware.gameserver.util;


import ru.catssoftware.gameserver.datatables.ClassTreeTable;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;


public class Strings
{

	private static final char hex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String bytesToString(byte[] b)
	{
		String ret = "";
		for(byte element : b)
		{
			ret += String.valueOf(hex[(element & 0xF0) >> 4]);
			ret += String.valueOf(hex[element & 0x0F]);
		}
		return ret;
	}

	public static String addSlashes(String s)
	{
		if(s == null)
			return "";

		s = s.replace("\\", "\\\\");
		s = s.replace("\"", "\\\"");
		s = s.replace("@", "\\@");
		s = s.replace("'", "\\'");
		return s;
	}

	public static String stripSlashes(String s)
	{
		if(s == null)
			return "";
		s = s.replace("\\'", "'");
		s = s.replace("\\\\", "\\");
		return s;
	}

	public static Integer parseInt(Object x)
	{
		if(x == null)
			return 0;

		if(x instanceof Integer)
			return (Integer) x;

		if(x instanceof Double)
			return ((Double) x).intValue();

		if(x instanceof Boolean)
			return (Boolean) x ? -1 : 0;

		Integer res = 0;
		try
		{
			res = Integer.parseInt("" + x);
		}
		catch(Exception e)
		{}
		return res;
	}

	public static Double parseFloat(Object x)
	{
		if(x instanceof Double)
			return (Double) x;

		if(x instanceof Integer)
			return 0.0 + (Integer) x;

		if(x == null)
			return 0.0;

		Double res = 0.0;
		try
		{
			res = Double.parseDouble("" + x);
		}
		catch(Exception e)
		{}
		return res;
	}

	public static Boolean parseBoolean(Object x)
	{
		if(x instanceof Integer)
			return (Integer) x != 0;

		if(x == null)
			return false;

		if(x instanceof Boolean)
			return (Boolean) x;

		if(x instanceof Double)
			return Math.abs((Double) x) < 0.00001;

		return !("" + x).equals("");
	}

	public static String replace(String str, String regex, int flags, String replace)
	{
		return Pattern.compile(regex, flags).matcher(str).replaceAll(replace);
	}

	public static boolean matches(String str, String regex, int flags)
	{
		return Pattern.compile(regex, flags).matcher(str).matches();
	}

	public static String bbParse(String s)
	{
		if(s == null)
			return null;

		s = s.replace("\r", "");


		s = s.replaceAll("(\\s|\"|\'|\\(|^|\n)\\*(.*?)\\*(\\s|\"|\'|\\)|\\?|\\.|!|:|;|,|$|\n)", "$1<font color=\"LEVEL\">$2</font>$3");

		s = replace(s, "^!(.*?)$", Pattern.MULTILINE, "<font color=\"LEVEL\">$1</font>\n\n");

		s = s.replaceAll("%%\\s*\n", "<br1>");

		s = s.replaceAll("\n\n\n+", "<br><br>");

		s = s.replaceAll("\n\n+", "<br>");

		s = replace(s, "\\[([^\\]\\|]*?)\\|([^\\]]*?)\\]", Pattern.DOTALL, "<a action=\"bypass -h $1\">$2</a>");

		s = s.replaceAll(" @", "\" msg=\"");


		return s;
	}

	public static String utf2win(String utfString)
	{
		String winString;
		try
		{
			winString = new String(utfString.getBytes("Cp1251"));
		}
		catch(UnsupportedEncodingException uee)
		{
			winString = utfString;
		}
		return winString;
	}

	public static String joinStrings(String glueStr, String strings[], int startIdx, int maxCount)
	{
		String result = "";
		if(startIdx < 0)
		{
			startIdx += strings.length;
			if(startIdx < 0)
				return result;
		}
		for(; startIdx < strings.length && maxCount != 0; maxCount--)
		{
			if(!result.isEmpty() && glueStr != null && !glueStr.isEmpty())
				result = (new StringBuilder()).append(result).append(glueStr).toString();
			result = (new StringBuilder()).append(result).append(strings[startIdx++]).toString();
		}

		return result;
	}

	public static String joinStrings(String glueStr, String strings[], int startIdx)
	{
		return joinStrings(glueStr, strings, startIdx, -1);
	}

	public static String joinStrings(String glueStr, String strings[])
	{
		return joinStrings(glueStr, strings, 0);
	}

	public static String getPriceName(int[] price)
	{
		return getPriceName(price[0], price[1]);
	}

	public static String getPriceName(int itemId, int count)
	{
		return count + " " + ItemTable.getInstance().getItemName(itemId);
	}

	public static String getClassName(L2PcInstance player)
	{
		return ClassTreeTable.getInstance().getParentClasses(player.getClassId().getId())[0].split("_")[1];
	}

	public static String getButton(String string, String bypass, boolean big)
	{
		String size = !big ? "width=100 height=25 back=\"L2UI_CH3.bigbutton_down\" fore=\"L2UI_CH3.bigbutton\"" : "width=157 height=25 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"";
		return "<button value=\"" + string + "\" action=\"bypass -h " + bypass + "\" " + size + " />";
	}

	public static String buttonCBUI94(String name, String bypass, int width, int height)
	{
		return "<button value=\""+ name + "\" action=\"bypass -h " + bypass + "\" width="+width+" height=" + height + " back=\"sek.cbui94\" fore=\"sek.cbui92\">";
	}

	private static String[][] timeType = {
			{ "секунда", "минута", "час" },
			{ "секунды", "минуты", "часа" },
			{ "секунд", "минут", "часов" },
	};
	/**
	 * Получаем окончание времени (минутЫ, минутА, минут).
	 * Тип - (0 - секунды, 1 - минуты, 2 - часы).
	 * @param time
	 * @param type
	 * @return
	 */
	public static String timeSuffix(int time, int type)
	{
		int num = time;
		if(num > 100) num %= 100;
		if(num > 20) num %= 10;

		if (type < 0)
			type = 0;
		else if (type > 2)
			type = 2;

		switch(num)
		{
			case 1:
				num = 0;
				break;
			case 2:
			case 3:
			case 4:
				num = 1;
				break;
			default:
				num = 2;
				break;
		}

		String strTime = Integer.toString(time);
		try
		{
			strTime += " " + timeType[num][type];
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return strTime;
	}

	public static String substring(String str, int maxSize)
	{
		return str.length() > maxSize ? str.substring(0, maxSize) + ".." : str;
	}
}