package ru.catssoftware.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


public class L2FortEnvoyInstance extends L2NpcInstance
{
	public L2FortEnvoyInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showMessageWindow(player);
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		String filename;
		if (!player.isClanLeader() || player.getClan() == null || getFort().getFortId() != player.getClan().getHasFort())
			filename = "data/html/fortress/envoy-noclan.htm";
		else if (getFort().getFortState() == 0)
			filename = "data/html/fortress/envoy.htm";
		else
			filename = "data/html/fortress/envoy-no.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%castleName%", String.valueOf(CastleManager.getInstance().getCastleById(getFort().getCastleIdFromEnvoy(getNpcId())).getName()));
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		String par = "";

		if (st.countTokens() >= 1)
			par = st.nextToken();

		if (actualCommand.equalsIgnoreCase("select"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(par);
			}
			catch (IndexOutOfBoundsException ioobe){}
			catch (NumberFormatException nfe){}
			int castleId = 0;
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (val == 2)
			{
				castleId = getFort().getCastleIdFromEnvoy(getNpcId());
				if (CastleManager.getInstance().getCastleById(castleId).getOwnerId() < 1)
				{
					html.setHtml("<html><body>Заключить союз в настоящее время невозможно.<br> Замок " +CastleManager.getInstance().getCastleById(castleId).getName()+ " в данный момент не имеет Лорда.</body></html>");
					player.sendPacket(html);
					return;
			}
			}
			getFort().setFortState(val, castleId);
			html.setFile("data/html/fortress/envoy-ok.htm");
			html.replace("%castleName%", String.valueOf(CastleManager.getInstance().getCastleById(getFort().getCastleIdFromEnvoy(getNpcId())).getName()));
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}
}