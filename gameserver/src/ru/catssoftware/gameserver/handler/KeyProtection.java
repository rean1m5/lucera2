package ru.catssoftware.gameserver.handler;

public class KeyProtection
{
	private static IKeyProtection _instance;

	public static IKeyProtection getInstance()
	{
		return _instance;
	}

	public static void setKeyProtection(IKeyProtection keyProtection)
	{
		_instance = keyProtection;
	}

	public static boolean isActive()
	{
		return _instance != null;
	}
}
