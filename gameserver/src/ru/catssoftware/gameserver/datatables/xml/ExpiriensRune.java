package ru.catssoftware.gameserver.datatables.xml;

public class ExpiriensRune {
	private static ExpiriensRune _instance;
	public static ExpiriensRune getInstance() {
		if(_instance==null)
			_instance = new ExpiriensRune();
		return _instance;
	}
	
}
