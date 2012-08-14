package ru.catssoftware.gameserver.instancemanager.games;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import javolution.util.FastList;
import org.apache.log4j.Logger;


import ru.catssoftware.Message;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.tools.random.Rnd;

public class fishingChampionship
{
	private static fishingChampionship		_instance;
	protected static Logger					_log				= Logger.getLogger(fishingChampionship.class.getName());
	private long							_enddate			= 0;
	private FastList<String>				_playersName		= new FastList<String>(); 
	private FastList<String>				_fishLength			= new FastList<String>();
	private FastList<String>				_winPlayersName		= new FastList<String>(); 
	private FastList<String>				_winFishLength		= new FastList<String>();
	private FastList<Fisher>				_tmpPlayer			= new FastList<Fisher>(); 
	private FastList<Fisher>				_winPlayer			= new FastList<Fisher>();
	private float							_minFishLength		= 0;
	private boolean							_needRefresh		= true;

	public static fishingChampionship getInstance()
	{
		if (_instance == null)
			_instance = new fishingChampionship();
		return _instance;
	}

	private fishingChampionship()
	{
		_log.info("Fishing Championship Manager : started");
		restoreData();
		refreshWinResult();
		setNewMin();
		if (_enddate<=System.currentTimeMillis())
		{
			_enddate = System.currentTimeMillis();
			new finishChamp().run();
		}
		else
			ThreadPoolManager.getInstance().scheduleGeneral(new finishChamp(), _enddate - System.currentTimeMillis());
	}

	private void setEndOfChamp()
	{
		Calendar finishtime = Calendar.getInstance();
		finishtime.setTimeInMillis(_enddate);
		finishtime.set(Calendar.MINUTE, 0);
		finishtime.set(Calendar.SECOND, 0);
		finishtime.add(Calendar.DAY_OF_MONTH, 6);
		finishtime.set(Calendar.DAY_OF_WEEK, 3);
		finishtime.set(Calendar.HOUR_OF_DAY, 19);
		_enddate = finishtime.getTimeInMillis();
	}

	private void restoreData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT finish_date FROM fishing_championship_date");
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				_enddate=rs.getLong("finish_date");
			}
			rs.close();
			statement.close();
			statement = con.prepareStatement("SELECT PlayerName,fishLength,rewarded FROM fishing_championship");
			rs = statement.executeQuery();
			while (rs.next())
			{
				int rewarded=rs.getInt("rewarded");
				if (rewarded==0) // Текущий участник
				{
					Fisher fisher = new Fisher();
					fisher._name = rs.getString("PlayerName");
					fisher._length = rs.getFloat("fishLength");
					_tmpPlayer.add(fisher);
				}
				if (rewarded>0) // Победитель прошлой недели
				{
					Fisher fisher = new Fisher();
					fisher._name = rs.getString("PlayerName");
					fisher._length = rs.getFloat("fishLength");
					fisher._rewarded = rewarded;
					_winPlayer.add(fisher);
				}
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Exception: can't get fishing championship info: " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	public synchronized void newFish(L2PcInstance pl)
	{
		float p1 = Rnd.get(60, 90);
		float len = (Rnd.get(0, 99)/100)+p1;
		if (_tmpPlayer.size()<5)
		{
			for(int x=0 ; x<_tmpPlayer.size();x++)
			{
				if(_tmpPlayer.get(x)._name.equalsIgnoreCase(pl.getName()))
				{
					if (_tmpPlayer.get(x)._length<len)
					{
						_tmpPlayer.get(x)._length=len;
						pl.sendMessage(Message.getMessage(pl, Message.MessageId.MSG_FISHCHAMP_BETTER_RESULT));
						setNewMin();
					}
					return;
				}
			}
			Fisher newFisher = new Fisher();
			newFisher._name=pl.getName();
			newFisher._length=len;
			_tmpPlayer.add(newFisher);
			pl.sendMessage(Message.getMessage(pl, Message.MessageId.MSG_FISHCHAMP_WINER_LIST));
			setNewMin();
		}
		else if (_minFishLength<len)
		{
			for(int x=0 ; x<_tmpPlayer.size();x++)
			{
				if(_tmpPlayer.get(x)._name.equalsIgnoreCase(pl.getName()))
				{
					if (_tmpPlayer.get(x)._length<len)
					{
						_tmpPlayer.get(x)._length=len;
						pl.sendMessage(Message.getMessage(pl, Message.MessageId.MSG_FISHCHAMP_BETTER_RESULT));
						setNewMin();
					}
					return;
				}
			}
			Fisher minFisher=null;
			float minLen=99999;
			for(int x=0 ; x<_tmpPlayer.size();x++)
			{
				if(_tmpPlayer.get(x)._length<minLen)
				{
					minFisher = _tmpPlayer.get(x);
					minLen = minFisher._length; 
				}
			}
			_tmpPlayer.remove(minFisher);
			Fisher newFisher = new Fisher();
			newFisher._name=pl.getName();
			newFisher._length=len;
			_tmpPlayer.add(newFisher);
			pl.sendMessage(Message.getMessage(pl, Message.MessageId.MSG_FISHCHAMP_WINER_LIST));
			setNewMin();
		}
	}

	private void setNewMin()
	{
		float minLen=99999;
		for(int x=0 ; x<_tmpPlayer.size();x++)
		{
			if(_tmpPlayer.get(x)._length<minLen)
			{
				minLen = _tmpPlayer.get(x)._length; 
			}
		}
		_minFishLength=minLen;
	}

	public long getTimeRemaining()
	{
		return (_enddate-System.currentTimeMillis())/60000;
	}

	public String getWinnerName(int par)
	{
		if (_winPlayersName.size()>=par)
			return _winPlayersName.get(par-1);
		return "Нет";
	}

	public String getCurrentName(int par)
	{
		if (_playersName.size()>=par)
			return _playersName.get(par-1);
		return "Нет";
	}

	public String getFishLength(int par)
	{
		if (_winFishLength.size()>=par)
			return _winFishLength.get(par-1);
		return "0";
	}

	public String getCurrentFishLength(int par)
	{
		if (_fishLength.size()>=par)
			return _fishLength.get(par-1);
		return "0";
	}

	public void getReward(L2PcInstance pl)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(pl.getObjectId());
		String str;
		str = "<html><head><title>Королевский турнир по ловле рыбы</title></head>";
		str += "Примите наши поздравления!<br>";
		str += "Вот Ваш приз! Вы настоящий рыбак!<br>";
		str += "Удачи на следующей неделе";
		str += "</body></html>";
		html.setHtml(str);
		pl.sendPacket(html);
		for (Fisher fisher : _winPlayer)
		{
			if (fisher._name.equalsIgnoreCase(pl.getName()))
			{
				if (fisher._rewarded!=2)
				{
					int rewardCnt=0;
					for (int x=0 ; x < _winPlayersName.size() ;x++)
					{
						if (_winPlayersName.get(x).equalsIgnoreCase(pl.getName()))
						{
							switch (x)
							{
								case 0:
									rewardCnt=800000;
									break;
								case 1:
									rewardCnt=500000;
									break;
								case 2:
									rewardCnt=300000;
									break;
								case 3:
									rewardCnt=200000;
									break;
								case 4:
									rewardCnt=100000;
									break;
							}
						}
					}
					fisher._rewarded=2;
					if (rewardCnt>0)
					{
						L2ItemInstance item = pl.getInventory().addItem("reward", 57, rewardCnt, pl, pl);
						SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						smsg.addItemName(item);
						smsg.addNumber(rewardCnt);
						pl.sendPacket(smsg);
						pl.sendPacket(new ItemList(pl, false));
					}
				}
			}
		}
	}

	public void showMidResult(L2PcInstance pl)
	{
		if (_needRefresh==true)
		{
			refreshResult();
			ThreadPoolManager.getInstance().scheduleGeneral(new needRefresh(), 60000);
		}
		NpcHtmlMessage html = new NpcHtmlMessage(pl.getObjectId());
		String str;
		str = "<html><head><title>Королевский турнир по ловле рыбы</title></head>";
		str += "Сейчас проходят соревнования по рыбной ловле! Этот турнир организуется и спонсируется Гильдией Рыболовов. Наша цель - привлечь к рыбалке внимание как можно большего количества людей. При поимке большого улова рыбак получит ценный приз!<br><br>";
		str += "По завершении турнира победители смогут получить свою награду в Гильдии Рыболовов. Забрать приз можно только <font color=\"LEVEL\"> в течение недели после окончания турнира</font>!<br>";
		str += "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>Место</td><td width=110 align=center>Рыбак</td><td width=80 align=center>Длина</td></tr></table><table width=280>";
		for(int x=1;x<=5;x++)
		{
			str += "<tr><td width=70 align=center>"+x+" Место:</td>";
			str += "<td width=110 align=center>"+getCurrentName(x)+"</td>";
			str += "<td width=80 align=center>"+getCurrentFishLength(x)+"</td></tr>";
		}
		str += "<td width=80 align=center>0</td></tr></table><br>";
		str += "Список призов<br><table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>Место</td><td width=110 align=center>Приз</td><td width=80 align=center>Количество</td></tr></table><table width=280>";
		str += "<tr><td width=70 align=center>1 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>800000</td></tr><tr><td width=70 align=center>2 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>500000</td></tr><tr><td width=70 align=center>3 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>300000</td></tr>";
		str += "<tr><td width=70 align=center>4 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>200000</td></tr><tr><td width=70 align=center>5 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>100000</td></tr></table></body></html>";
		html.setHtml(str);
		pl.sendPacket(html);
	}

	public void shutdown()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM fishing_championship_date");
			statement.execute();
			statement.close();
			statement = con.prepareStatement("INSERT INTO fishing_championship_date (finish_date) VALUES (?)");
			statement.setLong(1, _enddate);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM fishing_championship");
			statement.execute();
			statement.close();

			for(Fisher fisher : _winPlayer)
			{
				statement = con.prepareStatement("INSERT INTO fishing_championship(PlayerName,fishLength,rewarded) VALUES (?,?,?)");
				statement.setString(1, fisher._name);
				statement.setFloat(2, fisher._length);
				statement.setInt(3, fisher._rewarded);
				statement.execute();
				statement.close();
			}
			for(Fisher fisher : _tmpPlayer)
			{
				statement = con.prepareStatement("INSERT INTO fishing_championship(PlayerName,fishLength,rewarded) VALUES (?,?,?)");
				statement.setString(1, fisher._name);
				statement.setFloat(2, fisher._length);
				statement.setInt(3, 0);
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.error("Exception: can't update player vitality: " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	private synchronized void refreshResult()
	{
		_needRefresh=false;
		_playersName.clear();
		_fishLength.clear();
		Fisher fisher1=null;
		Fisher fisher2=null;
		for (int x=0 ; x<=_tmpPlayer.size()-1;x++)
		{
			for (int y=0 ; y<=_tmpPlayer.size()-2;y++)
			{
				fisher1=_tmpPlayer.get(y);
				fisher2=_tmpPlayer.get(y+1);
				if (fisher1._length<fisher2._length)
				{
					_tmpPlayer.set(y,fisher2);
					_tmpPlayer.set(y+1,fisher1);
				}
			}
		}
		for (int x=0 ; x<=_tmpPlayer.size()-1;x++)
		{
			_playersName.add(_tmpPlayer.get(x)._name);
			_fishLength.add(""+_tmpPlayer.get(x)._length);
		}
	}

	private void refreshWinResult()
	{
		_winPlayersName.clear();
		_winFishLength.clear();
		Fisher fisher1=null;
		Fisher fisher2=null;
		for (int x=0 ; x<=_winPlayer.size()-1;x++)
		{
			for (int y=0 ; y<=_winPlayer.size()-2;y++)
			{
				fisher1=_winPlayer.get(y);
				fisher2=_winPlayer.get(y+1);
				if (fisher1._length<fisher2._length)
				{
					_winPlayer.set(y,fisher2);
					_winPlayer.set(y+1,fisher1);
				}
			}
		}
		for (int x=0 ; x<=_winPlayer.size()-1;x++)
		{
			_winPlayersName.add(_winPlayer.get(x)._name);
			_winFishLength.add(""+_winPlayer.get(x)._length);
		}
	}

	private class finishChamp implements Runnable
	{
		public void run()
		{
			_winPlayer.clear();
			for(Fisher fisher : _tmpPlayer)
			{
				fisher._rewarded=1;
				_winPlayer.add(fisher);
			}
			_tmpPlayer.clear();
			refreshWinResult();
			setEndOfChamp();
			shutdown();
			_log.info("Fishing Championship Manager : start new event period.");
			ThreadPoolManager.getInstance().scheduleGeneral(new finishChamp(), _enddate - System.currentTimeMillis());
		}
	}

	private class needRefresh implements Runnable
	{
		public void run()
		{
			_needRefresh=true;
		}
	}

	private class Fisher
	{
		float 	_length=0;
		String	_name;
		int 	_rewarded=0;
	}
}