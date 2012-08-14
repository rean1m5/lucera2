package ru.catssoftware.gameserver.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javolution.util.FastList;
import org.apache.log4j.Logger;


public class L2SiegeStatus
{
	private Map<Integer, PlayerInfo>					_clansInfoList			= new HashMap<Integer, PlayerInfo>();
	private static final Logger							_log					= Logger.getLogger(L2SiegeStatus.class.getName());
	private static final L2SiegeStatus					_instance				= new L2SiegeStatus();	

	public static final L2SiegeStatus getInstance()
	{
		return _instance;
	}

	private L2SiegeStatus()
	{
		// need add
		_log.info("SiegeStatus: loading...");
	}

	public void shutdown()
	{
		// need add
	}

	public void addStatus(int clanId,int playerID,boolean killer)
	{
		synchronized(_clansInfoList)
		{
			PlayerInfo player=_clansInfoList.get(playerID);
			if (player != null)
			{
				if (killer==true)
					player._kill += 1;
				else
					player._death += 1;
			}
			else
			{
				player = new PlayerInfo();
				player._playerId=playerID;
				player._clanID=clanId;
				if (killer==true)
					player._kill += 1;
				else
					player._death += 1;

				_clansInfoList.put(playerID, player);
			}
		}
	}

	public void addStatus(int clanId,int playerID)
	{
		synchronized(_clansInfoList)
		{
			PlayerInfo player=_clansInfoList.get(playerID);
			if (player == null)
			{
				player = new PlayerInfo();
				player._playerId=playerID;
				player._clanID=clanId;
				player._kill = 0;
				player._death = 0;
				_clansInfoList.put(playerID, player);
			}
		}
	}

	public List<PlayerInfo> getMembers(int clanID)
	{
		List<PlayerInfo> result = new FastList<PlayerInfo>();
		synchronized(_clansInfoList)
		{
			for (PlayerInfo temp : _clansInfoList.values())
			{
				if (temp != null)
				{
					if (temp._clanID==clanID)
						result.add(temp);
				}
			}
		}
		return result;
	}

	public void clearClanStatus(int clanID)
	{
		for (PlayerInfo temp : getMembers(clanID))
		{
			synchronized(_clansInfoList)
			{
				_clansInfoList.remove(temp._playerId);
			}
		}
	}

	public class PlayerInfo
	{
		public int _clanID;
		public int _playerId;
		public int _kill;
		public int _death;
	}
}