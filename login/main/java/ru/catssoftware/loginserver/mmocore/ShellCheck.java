package ru.catssoftware.loginserver.mmocore;

public class ShellCheck
{
	public static boolean			_scriptOk					= false;
	public static boolean			_startOk					= false;
	private static String			_scriptKey					= "QWVW8GDSWEG81GWE681G";
	private static String			_startKey					= "ASD6S5DF0S56DF10SD6F";

	public static void checkScript(String bootKey)
	{
		if (scriptLoad(bootKey))
			_scriptOk = true;
	}

	public static void checkStart(String bootKey)
	{
		if (startLoad(bootKey))
			_startOk = true;
	}

	public static boolean scriptLoad(String bootKey)
	{
		boolean _result = false;

		if (bootKey.contains(_scriptKey))
		{
			_result = true;
		}
		return _result;
	}

	public static boolean startLoad(String bootKey)
	{
		boolean _result = false;

		if (bootKey.contains(_startKey))
		{
			_result = true;
		}
		return _result;
	}
}