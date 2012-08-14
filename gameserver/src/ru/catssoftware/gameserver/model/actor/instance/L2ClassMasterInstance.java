package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.handler.VoicedCommandHandler;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public final class L2ClassMasterInstance extends L2FolkInstance
{
	public L2ClassMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		if (getObjectId() != player.getTargetId())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				return;
			}
			if(VoicedCommandHandler.getInstance()._classMasterHandler!=null)
				VoicedCommandHandler.getInstance()._classMasterHandler.useVoicedCommand("classmaster", player, "");
			else {
				NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
				msg.setFile("data/html/defualt/npc-no.htm");
				player.sendPacket(msg);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	

}
