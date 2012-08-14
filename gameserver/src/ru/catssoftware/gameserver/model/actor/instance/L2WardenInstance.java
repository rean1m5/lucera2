package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2WardenInstance extends L2NpcInstance
{
	protected static final int	COND_ALL_FALSE				= 0;
	protected static final int	COND_BUSY_BECAUSE_OF_SIEGE	= 1;
	protected static final int	COND_OWNER					= 2;	
	public L2WardenInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		if (this != player.getTarget())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showMessageWindow(player,0);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	@Override	
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Quest"))
		{
			String quest = "";
			try
			{
				quest = command.substring(5).trim();
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}

			if (quest.length() == 0)
				showQuestWindow(player);
			else
				showQuestWindow(player, quest);
		}		
	}	
	public void showMessageWindow(L2PcInstance player,int val)
	{
		String filename = "data/html/fortress/warden-nocondition.htm";
		if (validateCondition(player)==COND_OWNER)
		{
			if (val==0)
				filename = "data/html/fortress/warden.htm";
			else
				filename = "data/html/fortress/warden-no.htm";
		}
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
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
		if (getFort() != null && getFort().getFortId()>0)
		{
			if (player.getClan() != null)
			{
				if (getFort().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if (getFort().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
			}			
		}
		return COND_ALL_FALSE;
	}	
}