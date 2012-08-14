package ru.catssoftware.gameserver.datatables;



public class RecordTable
{
	private static RecordTable	_instance;

	public static RecordTable getInstance()
	{
		if (_instance == null)
			_instance = new RecordTable();
		return _instance;
	}

	private RecordTable()
	{
	}

	public int getMaxPlayer()
	{
		try {
			return  ServerData.getInstance().getData().getInteger("Records.MaxPlayers");
		} catch(IllegalArgumentException e) {
			return 0;
		}
	}

	public String getDateMaxPlayer()
	{
		try {
			return  ServerData.getInstance().getData().getString("Records.RecordDate");
		} catch(IllegalArgumentException e) {
			return "не известна";
		}
	}
}