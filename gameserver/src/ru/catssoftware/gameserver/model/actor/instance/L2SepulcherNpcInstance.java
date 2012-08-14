package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.FourSepulchersManager.FourSepulchersMausoleum;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;



/**
 * @author sandman
 */

public class L2SepulcherNpcInstance extends L2NpcInstance
{
	protected FourSepulchersMausoleum		_mausoleum;
	
	public L2SepulcherNpcInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	public void setMausoleum(FourSepulchersMausoleum mausoleum) {
		_mausoleum = mausoleum;
	}
	@Override
	public void onAction(L2PcInstance player) {
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
				player.sendPacket(my);

				// Send a Server->Client packet StatusUpdate of the
				// L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			else
			{
				// Send a Server->Client packet MyTargetSelected to the
				// L2PcInstance player
				MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
				player.sendPacket(my);
			}

			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the player is attackable (without a forced attack) and
			// isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
				else
				{
					// Send a Server->Client packet ActionFailed (target is out
					// of attack range) to the L2PcInstance player
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}

			if (!isAutoAttackable(player))
			{
				// Calculate the distance between the L2PcInstance and the
				// L2NpcInstance
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Send a Server->Client packet SocialAction to the all
					// L2PcInstance on the _knownPlayer of the L2NpcInstance
					// to display a social action of the L2NpcInstance on their
					// client
					SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
					broadcastPacket(sa);

					doAction(player);
				}
			}
			// Send a Server->Client ActionFailed to the L2PcInstance in order
			// to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	protected void doAction(L2PcInstance player) {
		super.onAction(player);
	}
	
	@Override
	protected String getHtmlFolder() {
		return "SepulcherNpc";
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (isBusy())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName());
			html.replace("%playername%", player.getName());
			player.sendPacket(html);
		}
		else if (command.startsWith("open_gate")) {
			L2ItemInstance hallsKey = player.getInventory().getItemByItemId(7260);
			if (hallsKey == null)
				showChatWindow(player, "Gatekeeper-no.htm");
			else {
				player.destroyItem("FourSepuchers", hallsKey, this, true);
				_mausoleum.nextRoom();
			}
			
		} else super.onBypassFeedback(player, command);
	}	
	
}