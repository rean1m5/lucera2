package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author  Kerberos
 */

public class L2CastleMagicianInstance extends L2FolkInstance
{
	protected static final int	COND_ALL_FALSE				= 0;
	protected static final int	COND_BUSY_BECAUSE_OF_SIEGE	= 1;
	protected static final int	COND_OWNER					= 2;

	/**
	* @param template
	*/
	public L2CastleMagicianInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/castlemagician/magician-no.htm";

		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castlemagician/magician-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER) // Clan owns castle
			{
				if (val == 0)
					filename = "data/html/castlemagician/magician.htm";
				else
					filename = "data/html/castlemagician/magician-" + val + ".htm";
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", String.valueOf(getName() + " " + getTitle()));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equals("clan_gate"))
		{
			Castle castle = getCastle();
			if (!castle.isGateOpen())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/castlemagician/magician-nogate.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html); html = null;
				return;
			}
			player.teleToLocation(castle.getGateX(), castle.getGateY(), castle.getGateZ());
			player.stopMove(new L2CharPosition(castle.getGateX(), castle.getGateY(), castle.getGateZ(), player.getHeading()));
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			showChatWindow(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}

	protected int validateCondition(L2PcInstance player)
	{
		if (player.isGM())
			return COND_OWNER;
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}
}