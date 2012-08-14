package ru.catssoftware.gameserver.model.actor.instance;

import java.util.Calendar;
import java.util.StringTokenizer;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.TeleportLocationTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.L2TeleportLocation;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.restriction.AvailableRestriction;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2TeleporterInstance extends L2FolkInstance
{
	private final static Logger	_log						= Logger.getLogger(L2TeleporterInstance.class.getName());
	private static final int	COND_ALL_FALSE				= 0;
	private static final int	COND_BUSY_BECAUSE_OF_SIEGE	= 1;
	private static final int	COND_OWNER					= 2;
	private static final int	COND_REGULAR				= 3;

	public L2TeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (ObjectRestrictions.getInstance().checkRestriction(player, AvailableRestriction.PlayerTeleport))
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
			return;
		}

		int condition = validateCondition(player);

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("goto"))
		{
			if(player.isImmobilized()) {
				player.actionFail();
				return;
			}
			int npcId = getTemplate().getNpcId();
			String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
			int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
			boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
			int compWinner = SevenSigns.getInstance().getCabalHighestScore();

			switch (npcId)
			{
			case 31103: //
			case 31104: //
			case 31105: //
			case 31106: // Exit Necropolises
			case 31107: //
			case 31108: //
			case 31109: //
			case 31110: //
			case 31120: //
			case 31121: //
			case 31122: // Exit Catacombs
			case 31123: //
			case 31124: //
			case 31125: //
				player.setIsIn7sDungeon(false);
				break;
			case 31095: //
			case 31096: //
			case 31097: //
			case 31098: // Enter Necropolises
			case 31099: //
			case 31100: //
			case 31101: //
			case 31102: //
			{
				boolean canPort = true;
				if (isSealValidationPeriod)
				{
					if (Config.ALT_STRICT_SEVENSIGNS)
					{
						if (compWinner == SevenSigns.CABAL_DAWN && playerCabal != SevenSigns.CABAL_DAWN)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
							canPort = false;
						}
						else if (compWinner == SevenSigns.CABAL_DUSK && playerCabal != SevenSigns.CABAL_DUSK)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
							canPort = false;
						}
						else if (compWinner == SevenSigns.CABAL_NULL && playerCabal != SevenSigns.CABAL_NULL)
							canPort = true;
						else if (playerCabal == SevenSigns.CABAL_NULL)
							canPort = false;
					}
				}
				else
				{
					if (Config.ALT_STRICT_SEVENSIGNS)
					{
						if (playerCabal == SevenSigns.CABAL_NULL)
							canPort = false;
					}
				}
				if (!canPort)
				{
					NpcHtmlMessage htmlNecro = new NpcHtmlMessage(getObjectId());
					filename += "necro_no.htm";
					htmlNecro.setFile(filename);
					player.sendPacket(htmlNecro);
					return;
				}
				player.setIsIn7sDungeon(true);
				break;
			}
			case 31114: //
			case 31115: //
			case 31116: // Enter Catacombs
			case 31117: //
			case 31118: //
			case 31119: //
			{
				boolean canPort = true;
				if (isSealValidationPeriod)
				{
					if (Config.ALT_STRICT_SEVENSIGNS)
					{
						if (compWinner == SevenSigns.CABAL_DAWN && playerCabal != SevenSigns.CABAL_DAWN)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
							canPort = false;
						}
						else if (compWinner == SevenSigns.CABAL_DUSK && playerCabal != SevenSigns.CABAL_DUSK)
						{
							player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
							canPort = false;
						}
						else if (compWinner == SevenSigns.CABAL_NULL && playerCabal != SevenSigns.CABAL_NULL)
							canPort = true;
						else if (playerCabal == SevenSigns.CABAL_NULL)
							canPort = false;
					}
				}
				else
				{
					if (Config.ALT_STRICT_SEVENSIGNS)
					{
						if (playerCabal == SevenSigns.CABAL_NULL)
							canPort = false;
					}
				}
				if (!canPort)
				{
					NpcHtmlMessage htmlCata = new NpcHtmlMessage(getObjectId());
					filename += "cata_no.htm";
					htmlCata.setFile(filename);
					player.sendPacket(htmlCata);
					return;
				}
				player.setIsIn7sDungeon(true);
				break;
			}
			case 35092: //
			case 35134: //
			case 35176: //
			case 35218: //
			case 35261: //
			case 35308: //
			case 35352: //
			case 35497: //
			case 35544: //
			case 35093: //
			case 35135: //
			case 35177: //
			case 35219: //
			case 35262: //
			case 35309: // Ticket Siege Teleport
			case 35353: //
			case 35498: //
			case 35545: //
			case 35094: //
			case 35136: //
			case 35178: //
			case 35220: //
			case 35263: //
			case 35310: //
			case 35354: //
			case 35499: //
			case 35546: //
			case 35264: //
			case 35265: //
			case 35500: //
			case 35501: //
			{
				if (CastleManager.getInstance().getCastle(this) != null && player.getClan() != null)
					if (getCastle().getOwnerId() == player.getClanId())
					{
						if (st.countTokens() <= 0)
							return;

						int val = Integer.parseInt(st.nextToken());
						L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
						if (list != null)
						{
							player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
						}
						else
						{
							_log.warn("No teleport destination with id:" + val);
						}
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				break;
			}
			}

			if (st.countTokens() <= 0)
				return;

			int whereTo = Integer.parseInt(st.nextToken());
			if (condition == COND_REGULAR)
			{
				if (player != null)
					doTeleport(player, whereTo);
				return;
			}
			else if (condition == COND_OWNER)
			{
				int minPrivilegeLevel = 0; // NOTE: Replace 0 with highest level when privilege level is implemented
				if (st.countTokens() >= 1)
					minPrivilegeLevel = Integer.parseInt(st.nextToken());
				if (10 >= minPrivilegeLevel) // NOTE: Replace 10 with privilege level of player
					doTeleport(player, whereTo);
				else
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ACCESSABLE));
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("ctele"))
		{
			if (st.countTokens() <= 0)
				return;

			int whereTo = Integer.parseInt(st.nextToken());
			if (condition == COND_REGULAR)
			{
				doCustomTeleport(player, whereTo);
				return;
			}
			else if (condition == COND_OWNER)
			{
				int minPrivilegeLevel = 0; //Replace 0 with highest level when privilege level is implemented
				if (st.countTokens() >= 1)
					minPrivilegeLevel = Integer.parseInt(st.nextToken());
				if (10 >= minPrivilegeLevel) //Replace 10 with privilege level of player
					doTeleport(player, whereTo);
				else
					player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
				return;
			}
			super.onBypassFeedback(player, command);
		}
		else if (command.startsWith("Chat"))
		{
			Calendar cal = Calendar.getInstance();
			int val = 0;

			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{}
			catch (NumberFormatException nfe)
			{}

			if (val == 1 && player.getLevel() < 41)
			{
				showNewbieHtml(player);
				return;
			}
			else if (val == 1 && cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
			{
				showhalfPriceHtml(player);
				return;
			}
			showChatWindow(player, val);
		}
		super.onBypassFeedback(player, command);
	}

	@Override
	protected String getHtmlFolder() {
		return "teleporter";
	}



	private void showNewbieHtml(L2PcInstance player)
	{
		if (player == null)
			return;

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		String filename = "data/html/teleporter/free/" + getNpcId() + ".htm";
		if(HtmCache.getInstance().getHtm(filename, player)==null)
			filename = "data/html/teleporter/"+getNpcId() + "-1.htm";

		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	private void showhalfPriceHtml(L2PcInstance player)
	{
		if (player == null)
			return;

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		String filename = "data/html/teleporter/" + getNpcId() + "-1.htm";

		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename = "data/html/teleporter/castleteleporter-no.htm";

		int condition = validateCondition(player);
		if (condition == COND_REGULAR)
		{
			super.showChatWindow(player);
			return;
		}
		else if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER) // Clan owns castle
				filename = getHtmlPath(getNpcId(), 0,player); // Owner message window
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	private void doTeleport(L2PcInstance player, int val)
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			if (SiegeManager.getInstance().checkIfInZone(list.getLocX(), list.getLocY(), list.getLocZ()))
			{
				player.sendPacket(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE);
				return;
			}
			if (TownManager.getInstance().townHasCastleInSiege(list.getLocX(), list.getLocY(), list.getLocZ()))
			{
				player.sendPacket(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE);
				return;
			}
			if (player.isCombatFlagEquipped()) 
			{ 
				player.sendPacket(SystemMessageId.NOT_WORKING_PLEASE_TRY_AGAIN_LATER); 
				return; 
			} 			
			if (list.isForNoble() && !player.isNoble())
			{
				String filename = "data/html/teleporter/nobleteleporter-no.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				return;
			}
			if (player.isAlikeDead())
				return;

			int price = list.getPrice();
			if (player.getLevel() < 41 && !this.getTemplate().isCustom())
				price = 0;

			if (!list.isForNoble() && price != 0)
			{
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
					price /= 2;
			}
			
			if (!list.isForNoble() && (Config.ALT_GAME_FREE_TELEPORT || player.reduceAdena("Teleport", price, this, true)))
			{
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
				if (Config.PLAYER_SPAWN_PROTECTION > 0 && !isInsidePeaceZone(player))
					player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_SPAWN_PROTECTION), Config.PLAYER_SPAWN_PROTECTION));
			}
			else if (list.isForNoble() && (Config.ALT_GAME_FREE_TELEPORT || player.destroyItemByItemId("Noble Teleport", 13722, list.getPrice(), this, true)))
			{
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
				if (Config.PLAYER_SPAWN_PROTECTION > 0 && !isInsidePeaceZone(player))
					player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_SPAWN_PROTECTION), Config.PLAYER_SPAWN_PROTECTION));
			}
		}
		else
		{
			if (Config.LOAD_CUSTOM_TELEPORTS)
				doCustomTeleport(player, val);
			else
				_log.warn("No teleport destination with id:" + val);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private int validateCondition(L2PcInstance player)
	{
		if (CastleManager.getInstance().getCastle(this) == null)
			return COND_REGULAR;
//		else if (getCastle().getSiege().getIsInProgress())
//			return COND_BUSY_BECAUSE_OF_SIEGE;
		else if (player.getClan() != null)
		{
			if (getCastle().getOwnerId() == player.getClanId())
				return COND_OWNER;
		}
		return COND_ALL_FALSE;
	}

	private void doCustomTeleport(L2PcInstance player, int val)
	{
		if (player == null)
			return;

		if (!Config.LOAD_CUSTOM_TELEPORTS)
			return;

		L2TeleportLocation clist = TeleportLocationTable.getInstance().getCustomTemplate(val);
		if (clist != null)
		{
			if (!Config.ALLOW_TELE_IN_SIEGE_TOWN && SiegeManager.getInstance().checkIfInZone(clist.getLocX(), clist.getLocY(), clist.getLocZ()) && !player.isGM())
			{
				player.sendPacket(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE);
				return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0 && !player.isGM()) //karma
			{
				player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
				return;
			}
			else if (player.isAlikeDead())
				return;

			else if (Config.ALT_GAME_FREE_TELEPORT || player.reduceAdena("Teleport", clist.getPrice(), this, true))
			{
				player.teleToLocation(clist.getLocX(), clist.getLocY(), clist.getLocZ());
				if (Config.PLAYER_SPAWN_PROTECTION > 0 && !isInsidePeaceZone(player))
					player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_SPAWN_PROTECTION), Config.PLAYER_SPAWN_PROTECTION));
			}
		}
		else
			_log.info("No Custom Teleport Destination For ID:" + val);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
