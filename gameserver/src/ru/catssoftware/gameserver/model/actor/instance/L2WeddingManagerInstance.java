package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.instancemanager.CoupleManager;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.entity.Couple;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2WeddingManagerInstance extends L2NpcInstance
{
	public L2WeddingManagerInstance(int objectId, L2NpcTemplate template)
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
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		String filename = "data/html/mods/wedding/start.htm";
		String replace = "";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		html.replace("%price%", Config.WEDDING_PRICE);
		player.sendPacket(html);
	}

	@Override
	public synchronized void onBypassFeedback(final L2PcInstance player, String command)
	{
		String filename = "data/html/mods/wedding/start.htm";
		String replace = "";

		if(player.getPartnerId() == 0)
		{
			filename = "data/html/mods/wedding/nopartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}

		L2Object obj = L2World.getInstance().findObject(player.getPartnerId());
		final L2PcInstance ptarget = obj instanceof L2PcInstance ? (L2PcInstance) obj : null;
		if(ptarget == null || ptarget.isOnline() == 0)
		{
			filename = "data/html/mods/wedding/notfound.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}

		if(player.isMaried())
		{
			filename = "data/html/mods/wedding/already.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if (player.isMaryAccepted())
		{
			filename = "data/html/mods/wedding/waitforpartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if (command.startsWith("AcceptWedding"))
		{
			player.setMaryAccepted(true);
			Couple couple = CoupleManager.getInstance().getCouple(player.getCoupleId());
			couple.marry();
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOW_YOU_MARIED));
			player.setMaried(true);
			player.setMaryRequest(false);
			ptarget.sendMessage(Message.getMessage(ptarget, Message.MessageId.MSG_NOW_YOU_MARIED));
			ptarget.setMaried(true);
			ptarget.setMaryRequest(false); 
			if(Config.WEDDING_GIVE_CUPID_BOW)
			{
				player.addItem("Cupids Bow", 9140, 1, player, true, true);
				ptarget.addItem("Cupids Bow", 9140, 1, ptarget, true, true);
				player.sendSkillList();
				ptarget.sendSkillList();
			}

			MagicSkillUse MSU = new MagicSkillUse(player, player, 2230, 1, 1, 0, false);
			player.broadcastPacket(MSU);
			MSU = new MagicSkillUse(ptarget, ptarget, 2230, 1, 1, 0, false);
			ptarget.broadcastPacket(MSU);

			L2Skill skill = SkillTable.getInstance().getInfo(2025,1);
			if (skill != null)
			{
				MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0, false);
				player.sendPacket(MSU);
				player.broadcastPacket(MSU);
				player.useMagic(skill, false, false);

				MSU = new MagicSkillUse(ptarget, ptarget, 2025, 1, 1, 0, false);
				ptarget.sendPacket(MSU);
				ptarget.broadcastPacket(MSU);
				ptarget.useMagic(skill, false, false);
			}
			Announcements.getInstance().announceToAll("Поздравляем вас, "+player.getName()+" и "+ptarget.getName()+" с вашей свадьбой!");

			MSU = null;
			filename = "data/html/mods/wedding/accepted.htm";
			replace = ptarget.getName();
			sendHtmlMessage(ptarget, filename, replace);
			if (Config.WEDDING_HONEYMOON_PORT)
			{
				ThreadPoolManager.getInstance().schedule(new Runnable() {
					@Override
					public void run()
					{
						player.teleToLocation(Config.WEDDING_PORT_X, Config.WEDDING_PORT_Y, Config.WEDDING_PORT_Z);
						ptarget.teleToLocation(Config.WEDDING_PORT_X, Config.WEDDING_PORT_Y, Config.WEDDING_PORT_Z);
					}
				}, 10000);
			}
			return;
		}
		else if (command.startsWith("DeclineWedding"))
		{
			player.setMaryRequest(false);
			ptarget.setMaryRequest(false);
			player.setMaryAccepted(false);
			ptarget.setMaryAccepted(false);
			ptarget.sendMessage(Message.getMessage(ptarget, Message.MessageId.MSG_PARTNER_DECLINE));
			replace = ptarget.getName();
			filename = "data/html/mods/wedding/declined.htm";
			sendHtmlMessage(ptarget, filename, replace);
			return;
		}
		else if (player.isMary())
		{
			if(Config.WEDDING_FORMALWEAR && !player.isWearingFormalWear())
			{
				filename = "data/html/mods/wedding/noformal.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			filename = "data/html/mods/wedding/ask.htm";
			player.setMaryRequest(false);
			ptarget.setMaryRequest(false);
			replace = ptarget.getName();
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if (command.startsWith("AskWedding"))
		{
			if(Config.WEDDING_FORMALWEAR && !player.isWearingFormalWear())
			{
				filename = "data/html/mods/wedding/noformal.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			else if(player.getAdena() < Config.WEDDING_PRICE)
			{
				filename = "data/html/mods/wedding/adena.htm";
				replace = String.valueOf(Config.WEDDING_PRICE);
				sendHtmlMessage(player, filename, replace);
				return;
			}
			else
			{
				player.setMaryAccepted(true);
				ptarget.setMaryRequest(true);
				replace = ptarget.getName();
				filename = "data/html/mods/wedding/requested.htm";
				player.getInventory().reduceAdena("Wedding", Config.WEDDING_PRICE, player, player.getLastFolkNPC());
				sendHtmlMessage(player, filename, replace);
				return;
			}
		}
		sendHtmlMessage(player, filename, replace);
	}

	private void sendHtmlMessage(L2PcInstance player, String filename, String replace)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
