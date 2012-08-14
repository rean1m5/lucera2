package ru.catssoftware.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.tools.random.Rnd;


public class AutoChatHandler implements SpawnListener
{
	protected static Logger							_log				= Logger.getLogger(AutoChatHandler.class.getName());
	private static AutoChatHandler					_instance;
	private static final int						DEFAULT_CHAT_RANGE	= 1500;

	protected final FastMap<Integer, AutoChatInstance>	_registeredChats;

	private AutoChatHandler()
	{
		_registeredChats = new FastMap<Integer, AutoChatInstance>();
		restoreChatData();
		L2Spawn.addSpawnListener(this);
		_log.info("AutoChatHandler: Loaded " + size() + " handlers in total.");
	}

	private void restoreChatData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);

			statement = con.prepareStatement("SELECT * FROM auto_chat ORDER BY groupId ASC");
			rs = statement.executeQuery();

			while (rs.next())
			{
				int groupId = rs.getInt("groupId");
				int npcId = rs.getInt("npcId");
				long chatDelay = rs.getLong("chatDelay") * 1000;
				int chatRange = rs.getInt("chatRange");
				boolean chatRandom = rs.getBoolean("chatRandom");


				statement2 = con.prepareStatement("SELECT * FROM auto_chat_text WHERE groupId=?");
				statement2.setInt(1, groupId);
				rs2 = statement2.executeQuery();

				ArrayList<String> chatTexts = new ArrayList<String>();

				while (rs2.next())
					chatTexts.add(rs2.getString("chatText"));

				if (!chatTexts.isEmpty())
					registerGlobalChat(npcId, chatTexts.toArray(new String[chatTexts.size()]), chatDelay, chatRange, chatRandom);
				else
					_log.warn("AutoChatHandler: Chat group " + groupId + " is empty.");

				statement2.close();
			}

			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("AutoSpawnHandler: Could not restore chat data: " + e);
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

	public void reload()
	{
		// unregister all registered spawns
		for (AutoChatInstance aci : _registeredChats.values())
		{
			if (aci != null)
			{
				// clear timer
				if (aci._chatTask != null)
					aci._chatTask.cancel(true);
				removeChat(aci);
			}
		}
		_registeredChats.clear();
		restoreChatData();
	}

	public static AutoChatHandler getInstance()
	{
		if (_instance == null)
			_instance = new AutoChatHandler();
		return _instance;
	}

	public int size()
	{
		return _registeredChats.size();
	}

	public AutoChatInstance registerGlobalChat(int npcId, String[] chatTexts, long chatDelay, int chatRange, boolean chatRandom)
	{
		return registerChat(npcId, null, chatTexts, chatDelay, chatRange, chatRandom);
	}

	public AutoChatInstance registerChat(L2NpcInstance npcInst, String[] chatTexts, long chatDelay)
	{
		return registerChat(npcInst.getNpcId(), npcInst, chatTexts, chatDelay, DEFAULT_CHAT_RANGE, false);
	}

	public AutoChatInstance registerChat(L2NpcInstance npcInst, String[] chatTexts, long chatDelay, int chatRange, boolean chatRandom)
	{
		return registerChat(npcInst.getNpcId(), npcInst, chatTexts, chatDelay, chatRange, chatRandom);
	}

	private final AutoChatInstance registerChat(int npcId, L2NpcInstance npcInst, String[] chatTexts, long chatDelay, int chatRange, boolean chatRandom)
	{
		AutoChatInstance chatInst = null;

		if (chatDelay < 0)
			chatDelay = 30000;
		if (chatRange < 0)
			chatRange = DEFAULT_CHAT_RANGE;

		if (_registeredChats.containsKey(npcId))
			chatInst = _registeredChats.get(npcId);
		else
			chatInst = new AutoChatInstance(npcId, chatTexts, chatDelay, chatRange, chatRandom, (npcInst == null));

		if (npcInst != null)
			chatInst.addChatDefinition(npcInst);

		_registeredChats.put(npcId, chatInst);

		return chatInst;
	}

	public boolean removeChat(int npcId)
	{
		AutoChatInstance chatInst = _registeredChats.get(npcId);

		return removeChat(chatInst);
	}

	public boolean removeChat(AutoChatInstance chatInst)
	{
		if (chatInst == null)
			return false;

		_registeredChats.remove(chatInst.getNPCId());
		chatInst.setActive(false);
		return true;
	}

	public AutoChatInstance getAutoChatInstance(int id, boolean byObjectId)
	{
		if (!byObjectId)
			return _registeredChats.get(id);

		for (AutoChatInstance chatInst : _registeredChats.values())
		{
			if (chatInst.getChatDefinition(id) != null)
				return chatInst;
		}
		return null;
	}

	public void setAutoChatActive(boolean isActive)
	{
		for (AutoChatInstance chatInst : _registeredChats.values())
			chatInst.setActive(isActive);
	}

	public void setAutoChatActive(int npcId, boolean isActive)
	{
		for (AutoChatInstance chatInst : _registeredChats.values())
		{
			if (chatInst.getNPCId() == npcId)
				chatInst.setActive(isActive);
		}
	}

	public void npcSpawned(L2NpcInstance npc)
	{
		synchronized (_registeredChats)
		{
			if (npc == null)
				return;

			int npcId = npc.getNpcId();

			if (_registeredChats.containsKey(npcId))
			{
				AutoChatInstance chatInst = _registeredChats.get(npcId);

				if (chatInst != null && chatInst.isGlobal())
					chatInst.addChatDefinition(npc);
			}
		}
	}

	public class AutoChatInstance
	{
		protected int									_npcId;
		private long									_defaultDelay		= 30000;
		private int										_defaultRange		= DEFAULT_CHAT_RANGE;
		private String[]								_defaultTexts;
		private boolean									_defaultRandom;

		private boolean									_globalChat			= false;
		private boolean									_isActive;

		private FastMap<Integer, AutoChatDefinition>	_chatDefinitions	= new FastMap<Integer, AutoChatDefinition>();
		protected ScheduledFuture<?>					_chatTask;

		protected AutoChatInstance(int npcId, String[] chatTexts, long chatDelay, int chatRange, boolean chatRandom, boolean isGlobal)
		{
			_defaultTexts = chatTexts;
			_npcId = npcId;
			_defaultDelay = (chatDelay <= 0 ? 30000 : chatDelay);
			_defaultRange = chatRange;
			_defaultRandom = chatRandom;
			_globalChat = isGlobal;
			setActive(true);
		}

		protected AutoChatDefinition getChatDefinition(int objectId)
		{
			return _chatDefinitions.get(objectId);
		}

		protected AutoChatDefinition[] getChatDefinitions()
		{
			return _chatDefinitions.values().toArray(new AutoChatDefinition[_chatDefinitions.values().size()]);
		}

		public int addChatDefinition(L2NpcInstance npcInst)
		{
			return addChatDefinition(npcInst, null, 0);
		}

		public int addChatDefinition(L2NpcInstance npcInst, String[] chatTexts, long chatDelay)
		{
			int objectId = npcInst.getObjectId();
			AutoChatDefinition chatDef = new AutoChatDefinition(this, npcInst, chatTexts, chatDelay);

			_chatDefinitions.put(objectId, chatDef);
			return objectId;
		}

		public boolean removeChatDefinition(int objectId)
		{
			if (!_chatDefinitions.containsKey(objectId))
				return false;

			AutoChatDefinition chatDefinition = _chatDefinitions.get(objectId);
			chatDefinition.setActive(false);
			_chatDefinitions.remove(objectId);
			return true;
		}

		public boolean isActive()
		{
			return _isActive;
		}

		public boolean isGlobal()
		{
			return _globalChat;
		}

		public boolean isDefaultRandom()
		{
			return _defaultRandom;
		}

		public boolean isRandomChat(int objectId)
		{
			if (!_chatDefinitions.containsKey(objectId))
				return false;
			return _chatDefinitions.get(objectId).isRandomChat();
		}

		public int getNPCId()
		{
			return _npcId;
		}

		public int getDefinitionCount()
		{
			return _chatDefinitions.size();
		}

		public L2NpcInstance[] getNPCInstanceList()
		{
			FastList<L2NpcInstance> npcInsts = new FastList<L2NpcInstance>();

			for (AutoChatDefinition chatDefinition : _chatDefinitions.values())
				npcInsts.add(chatDefinition._npcInstance);
			return npcInsts.toArray(new L2NpcInstance[npcInsts.size()]);
		}

		public long getDefaultDelay()
		{
			return _defaultDelay;
		}

		public String[] getDefaultTexts()
		{
			return _defaultTexts;
		}

		public int getDefaultRange()
		{
			return _defaultRange;
		}

		public void setDefaultChatDelay(long delayValue)
		{
			_defaultDelay = delayValue;
		}

		public void setDefaultChatTexts(String[] textsValue)
		{
			_defaultTexts = textsValue;
		}

		public void setDefaultRange(int rangeValue)
		{
			_defaultRange = rangeValue;
		}

		public void setDefaultRandom(boolean randValue)
		{
			_defaultRandom = randValue;
		}

		public void setChatDelay(int objectId, long delayValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if (chatDef != null)
				chatDef.setChatDelay(delayValue);
		}

		public void setChatTexts(int objectId, String[] textsValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if (chatDef != null)
				chatDef.setChatTexts(textsValue);
		}

		public void setRandomChat(int objectId, boolean randValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if (chatDef != null)
				chatDef.setRandomChat(randValue);
		}

		public void setChatRange(int objectId, int rangeValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if (chatDef != null)
				chatDef.setChatRange(rangeValue);
		}

		public void setActive(boolean activeValue)
		{
			if (_isActive == activeValue)
				return;

			_isActive = activeValue;

			if (!isGlobal())
			{
				for (AutoChatDefinition chatDefinition : _chatDefinitions.values())
					chatDefinition.setActive(activeValue);
				return;
			}

			if (isActive())
			{
				AutoChatRunner acr = new AutoChatRunner(_npcId, -1);
				_chatTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(acr, _defaultDelay, _defaultDelay);
			}
			else
				_chatTask.cancel(false);
		}

		private class AutoChatDefinition
		{
			protected int				_chatIndex	= 0;
			protected L2NpcInstance		_npcInstance;

			protected AutoChatInstance	_chatInstance;

			private long				_chatDelay	= 30000;
			private int					_chatRange	= DEFAULT_CHAT_RANGE;
			private String[]			_chatTexts	= null;
			private boolean				_isActiveDefinition;
			private boolean				_randomChat;

			protected AutoChatDefinition(AutoChatInstance chatInst, L2NpcInstance npcInst, String[] chatTexts, long chatDelay)
			{
				_npcInstance = npcInst;

				_chatInstance = chatInst;
				_randomChat = chatInst.isDefaultRandom();

				_chatDelay = (chatDelay <= 0 ? 30000 : chatDelay);
				_chatRange = chatInst.getDefaultRange();
				_chatTexts = chatTexts;

				if (!chatInst.isGlobal())
					setActive(true);
			}

			protected AutoChatDefinition(AutoChatInstance chatInst, L2NpcInstance npcInst)
			{
				this(chatInst, npcInst, null, -1);
			}

			protected String[] getChatTexts()
			{
				if (_chatTexts != null)
					return _chatTexts;
				return _chatInstance.getDefaultTexts();
			}

			private long getChatDelay()
			{
				if (_chatDelay > 0)
					return _chatDelay;
				return _chatInstance.getDefaultDelay();
			}

			private boolean isActive()
			{
				return _isActiveDefinition;
			}

			boolean isRandomChat()
			{
				return _randomChat;
			}

			void setRandomChat(boolean randValue)
			{
				_randomChat = randValue;
			}

			void setChatDelay(long delayValue)
			{
				_chatDelay = delayValue;
			}

			void setChatRange(int rangeValue)
			{
				_chatRange = rangeValue;
			}

			void setChatTexts(String[] textsValue)
			{
				_chatTexts = textsValue;
			}

			void setActive(boolean activeValue)
			{
				if (isActive() == activeValue)
					return;

				if (activeValue)
				{
					AutoChatRunner acr = new AutoChatRunner(_npcId, _npcInstance.getObjectId());
					_chatTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(acr, getChatDelay(), getChatDelay());
				}
				else
					_chatTask.cancel(false);

				_isActiveDefinition = activeValue;
			}
		}

		private class AutoChatRunner implements Runnable
		{
			private int	_runnerNpcId;
			private int	_objectId;

			protected AutoChatRunner(int pNpcId, int pObjectId)
			{
				_runnerNpcId = pNpcId;
				_objectId = pObjectId;
			}

			public synchronized void run()
			{
				AutoChatInstance chatInst = _registeredChats.get(_runnerNpcId);
				AutoChatDefinition[] chatDefinitions;

				if (chatInst.isGlobal())
				{
					chatDefinitions = chatInst.getChatDefinitions();
				}
				else
				{
					AutoChatDefinition chatDef = chatInst.getChatDefinition(_objectId);

					if (chatDef == null)
					{
						_log.warn("AutoChatHandler: Auto chat definition is NULL for NPC ID " + _npcId + ".");
						return;
					}

					chatDefinitions = new AutoChatDefinition[]
					{ chatDef };
				}

				for (AutoChatDefinition chatDef : chatDefinitions)
				{
					try
					{
						L2NpcInstance chatNpc = chatDef._npcInstance;
						FastList<L2PcInstance> nearbyPlayers = new FastList<L2PcInstance>();

						for (L2Character player : chatNpc.getKnownList().getKnownCharactersInRadius(chatDef._chatRange))
							if (player instanceof L2PcInstance && !((L2PcInstance) player).isGM())
								nearbyPlayers.add((L2PcInstance) player);

						int maxIndex = chatDef.getChatTexts().length;
						int lastIndex = Rnd.nextInt(maxIndex);

						String creatureName = chatNpc.getName();
						String text;

						if (!chatDef.isRandomChat())
						{
							lastIndex = chatDef._chatIndex;
							if (lastIndex == maxIndex)
								lastIndex = 0;
							chatDef._chatIndex = lastIndex + 1;
						}

						text = chatDef.getChatTexts()[lastIndex];

						if (text == null)
							return;

						if (!nearbyPlayers.isEmpty())
						{
							final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
							int losingCabal = SevenSigns.CABAL_NULL;

							if (winningCabal == SevenSigns.CABAL_DAWN)
								losingCabal = SevenSigns.CABAL_DUSK;
							else if (winningCabal == SevenSigns.CABAL_DUSK)
								losingCabal = SevenSigns.CABAL_DAWN;

							if (text.indexOf("%player_") > -1)
							{
								ArrayList<Integer> karmaPlayers = new ArrayList<Integer>();
								ArrayList<Integer> winningCabals = new ArrayList<Integer>();
								ArrayList<Integer> losingCabals = new ArrayList<Integer>();

								for (int i = 0; i < nearbyPlayers.size(); i++)
								{
									L2PcInstance nearbyPlayer = nearbyPlayers.get(i);

									// Get all nearby players with karma
									if (nearbyPlayer.getKarma() > 0)
										karmaPlayers.add(i);

									// Get all nearby Seven Signs winners and loosers
									if (SevenSigns.getInstance().getPlayerCabal(nearbyPlayer) == winningCabal)
										winningCabals.add(i);
									else if (SevenSigns.getInstance().getPlayerCabal(nearbyPlayer) == losingCabal)
										losingCabals.add(i);
								}

								if (text.indexOf("%player_random%") > -1)
								{
									int randomPlayerIndex = Rnd.nextInt(nearbyPlayers.size());
									L2PcInstance randomPlayer = nearbyPlayers.get(randomPlayerIndex);
									text = text.replaceAll("%player_random%", randomPlayer.getName());
								}
								else if (text.indexOf("%player_killer%") > -1 && !karmaPlayers.isEmpty())
								{
									int randomPlayerIndex = karmaPlayers.get(Rnd.nextInt(karmaPlayers.size()));
									L2PcInstance randomPlayer = nearbyPlayers.get(randomPlayerIndex);
									text = text.replaceAll("%player_killer%", randomPlayer.getName());
								}
								else if (text.indexOf("%player_cabal_winner%") > -1 && !winningCabals.isEmpty())
								{
									int randomPlayerIndex = winningCabals.get(Rnd.nextInt(winningCabals.size()));
									L2PcInstance randomPlayer = nearbyPlayers.get(randomPlayerIndex);
									text = text.replaceAll("%player_cabal_winner%", randomPlayer.getName());
								}
								 else if (text.indexOf("%player_cabal_loser%") > -1 && !losingCabals.isEmpty())
								{
									int randomPlayerIndex = losingCabals.get(Rnd.nextInt(losingCabals.size()));
									L2PcInstance randomPlayer = nearbyPlayers.get(randomPlayerIndex);
									text = text.replaceAll("%player_cabal_loser%", randomPlayer.getName());
								}
							}
						}

						if (text == null)
							return;

						if (text.contains("%player_"))
							return;

						chatNpc.broadcastPacket(new CreatureSay(chatNpc.getObjectId(), SystemChatChannelId.Chat_Normal, creatureName, text));
					}
					catch (Exception e)
					{
						_log.error(e.getMessage(), e);
						return;
					}
				}
			}
		}
	}
}