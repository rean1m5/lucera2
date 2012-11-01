package ru.catssoftware.gameserver.network;

import org.apache.log4j.Logger;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.instancemanager.DuelManager;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Duel;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * @author NB4L1
 */
public final class Disconnection
{
	private static final Logger _log = Logger.getLogger("Disconnection");
	public static L2GameClient getClient(L2GameClient client, L2PcInstance activeChar)
	{
		if (client != null)
			return client;

		if (activeChar != null)
			return activeChar.getClient();

		return null;
	}
	
	public static L2PcInstance getActiveChar(L2GameClient client, L2PcInstance activeChar)
	{
		if (activeChar != null)
			return activeChar;

		if (client != null)
			return client.getActiveChar();

		return null;
	}

	private final L2GameClient _client;
	private final L2PcInstance _activeChar;

	public Disconnection(L2GameClient client)
	{
		this(client, null);
		
	}

	public Disconnection(L2PcInstance activeChar)
	{
		this(null, activeChar);
	}

	public Disconnection(L2GameClient client, L2PcInstance activeChar)
	{
		_client = getClient(client, activeChar);
		_activeChar = getActiveChar(client, activeChar);

		if(_activeChar!=null)
			store();

		if (_client != null)
			_client.setActiveChar(null);
		
		if (_activeChar != null)
			_activeChar.setClient(null);
	}

	public Disconnection store()
	{
		try
		{
				if (_activeChar != null)
				{ 
					if(_activeChar.isMoving())
						_activeChar.stopMove();
					if (_activeChar.getInstanceId()!=0)
					{
						Instance instanceObj = InstanceManager.getInstance().getInstance(_activeChar.getInstanceId());
						if (instanceObj != null)
							_activeChar.getPosition().setXYZ(instanceObj.getTpLoc(_activeChar).getX(),instanceObj.getTpLoc(_activeChar).getY(),instanceObj.getTpLoc(_activeChar).getZ());
					}

					QuestState stq = _activeChar.getQuestState("HellboundTown");
					if (stq != null && stq.getInt("cond") == 1)
						stq.getQuest().onDisconnect(stq,_activeChar);
					
					if(_activeChar.inObserverMode())
						_activeChar.leaveObserverMode();
					if(_activeChar.getGameEvent()!=null )
						_activeChar.getGameEvent().onLogout(_activeChar);
					
					if (RainbowSpringSiege.getInstance().isPlayerInArena(_activeChar))
						RainbowSpringSiege.getInstance().removeFromArena(_activeChar);
					if (_activeChar.getDuelState()!=Duel.DUELSTATE_NODUEL)
					{
						Duel duel = DuelManager.getInstance().getDuel(_activeChar.getDuelId());
						if (!duel.isPartyDuel())
						{
							duel.doSurrender(_activeChar);
							duel.restorePlayerConditions(_activeChar);
						}
					}
					if(_activeChar.getPartner()!=null) {
						_activeChar.getPartner().onPartnerDisconnect();
					}
					_activeChar.store(true);
				}
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
		return this;
	}

	public Disconnection deleteMe()
	{
		try
		{
			if (_activeChar != null) {
				_activeChar.deleteMe();
			}
			
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}

		return this;
	}

	public Disconnection close(boolean toLoginScreen)
	{
		if (_client != null)
			_client.close(toLoginScreen);

		return this;
	}

	public void defaultSequence(boolean toLoginScreen)
	{
		deleteMe();
		close(toLoginScreen);
	}

	public void onDisconnection()
	{
		if (_activeChar != null)
		{
			ThreadPoolManager.getInstance().schedule(new Runnable()
			{
				@Override
				public void run()
				{
					deleteMe();
				}
			}, _activeChar.canLogout() ? 0 : AttackStanceTaskManager.COMBAT_TIME);
		}
	}
}