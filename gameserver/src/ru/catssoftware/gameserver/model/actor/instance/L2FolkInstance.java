/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model.actor.instance;

import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2EnchantSkillLearn;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2SkillLearn;
import ru.catssoftware.gameserver.model.actor.status.FolkStatus;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.AcquireSkillList;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.ExEnchantSkillList;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.effects.EffectBuff;
import ru.catssoftware.gameserver.skills.effects.EffectDebuff;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

import javolution.text.TextBuilder;


public class L2FolkInstance extends L2NpcInstance
{
	private List<ClassId>	_classesToTeach;

	public L2FolkInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_classesToTeach = template.getTeachInfo();
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		player.setLastFolkNPC(this);
		super.onAction(player);
	}

	@Override
	public FolkStatus getStatus()
	{
		if (_status == null)
			_status = new FolkStatus(this);
		return (FolkStatus) _status;
	}

	@Override
	public void addEffect(L2Effect newEffect)
	{
		if (newEffect instanceof EffectDebuff || newEffect instanceof EffectBuff)
			super.addEffect(newEffect);
		else if (newEffect != null)
			newEffect.stopEffectTask();
	}

	/**
	 * this displays SkillList to the player.
	 * @param player
	 */
	public void showSkillList(L2PcInstance player, ClassId classId)
	{
		if (_log.isDebugEnabled() || Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		int npcId = getTemplate().getNpcId();
		if(GameExtensionManager.getInstance().handleAction(player, Action.PC_LEARN_SKILL, classId,this)!=null)
			return;
		if (_classesToTeach == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you. My class list is empty.<br>Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:"
					+ npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);

			return;
		}

		if (!getTemplate().canTeach(classId))
		{
			showNoTeachHtml(player);
			return;
		}

		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Usual);
		int counts = 0;

		for (L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

			if (sk == null || !sk.getCanLearn(player.getClassId()) || !sk.canTeachBy(npcId))
				continue;
			
			if (sk.getSkillType() == L2SkillType.NOTDONE)
			{
				switch (Config.SEND_NOTDONE_SKILLS)
				{
				case 3:
					break;
				case 2:
					if (player.isGM())
						break;
				default:
					continue;
				}
			}

			int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;

			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if (counts == 0)
		{
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);

			if (minlevel > 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_REACHED_S1);
				sm.addNumber(minlevel);
				player.sendPacket(sm);
			}
			else
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
		else
			player.sendPacket(asl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * this displays EnchantSkillList to the player.
	 * @param player
	 */
	public void showEnchantSkillList(L2PcInstance player)
	{
		int npcId = getTemplate().getNpcId();

		if (_classesToTeach == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you. My class list is empty.<br>Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:"
					+ npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);

			return;
		}

		if (!getTemplate().canTeach(player.getClassId()))
		{
			showNoTeachHtml(player);
			return;
		}

		if (player.getClassId().level() < 3) // requires to have 3rd class quest completed
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>Enchant A Skill:<br>");
			sb.append("Only characters who have changed their occupation three times are allowed to enchant a skill.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}

		int playerLevel = player.getLevel();

		if (playerLevel >= 76)
		{
			L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
			ExEnchantSkillList esl = new ExEnchantSkillList();
			int counts = 0;
			for(L2EnchantSkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if(sk == null)
				{
					continue;
				}
				counts++;
				esl.addSkill(s.getId(), s.getLevel(), s.getSpCost(), s.getExp());
			}

			if (counts == 0)
				player.sendPacket(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT);
			else
				player.sendPacket(esl);
		}
		else
			player.sendPacket(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (player.inObserverMode())
			return;

		if (command.startsWith("SkillList"))
		{
			if (Config.ALT_GAME_SKILL_LEARN)
			{
				String id = command.substring(9).trim();

				if (id.length() != 0)
				{
					player.setSkillLearningClassId(ClassId.values()[Integer.parseInt(id)]);
					showSkillList(player, ClassId.values()[Integer.parseInt(id)]);
				}
				else
				{
					boolean own_class = false;

					if (_classesToTeach != null)
					{
						for (ClassId cid : _classesToTeach)
						{
							if (cid.equalsOrChildOf(player.getClassId()))
							{
								own_class = true;
								break;
							}
						}
					}

					String text = "<html><body><center>Skill learning:</center><br>";

					if (!own_class)
					{
						String charType = player.getClassId().isMage() ? "fighter" : "mage";
						text += "Skills of your class are the easiest to learn.<br>\n" + "Skills of another class of your race are a little harder.<br>"
								+ "Skills for classes of another race are extremely difficult.<br>" + "But the hardest of all to learn are the " + charType
								+ " skills!<br>";
					}

					// make a list of classes
					if (_classesToTeach != null)
					{
						int count = 0;
						ClassId classCheck = player.getClassId();

						while ((count == 0) && (classCheck != null))
						{
							for (ClassId cid : _classesToTeach)
							{
								if (cid.level() != classCheck.level())
									continue;

								if (SkillTreeTable.getInstance().getAvailableSkills(player, cid).length == 0)
									continue;

								text += "<a action=\"bypass -h npc_%objectId%_SkillList " + cid.getId() + "\">Learn " + cid + "'s class Skills</a><br>\n";
								count++;
							}
							classCheck = classCheck.getParent();
						}
						classCheck = null;
					}
					else
						text += "No Skills.<br>\n";

					text += "</body></html>";

					insertObjectIdAndShowChatWindow(player, text);
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			else
			{
				player.setSkillLearningClassId(player.getClassId());
				showSkillList(player, player.getClassId());
			}
		}
		else if (command.startsWith("EnchantSkillList"))
			showEnchantSkillList(player);
		else
			super.onBypassFeedback(player, command);
	}

	private void showNoTeachHtml(L2PcInstance player)
	{
		int npcId = getNpcId();
		String html = "";

		if (this instanceof L2WarehouseInstance)
			html = HtmCache.getInstance().getHtm("data/html/warehouse/" + npcId + "-noteach.htm",player);
		else if (this instanceof L2TrainerInstance)
			html = HtmCache.getInstance().getHtm("data/html/trainer/" + npcId + "-noteach.htm",player);
		if (html == null)
		{
			//_log.warn("Npc "+npcId+" missing noTeach html!");
			NpcHtmlMessage htm = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
			sb.append("</body></html>");
			htm.setHtml(sb.toString());
			player.sendPacket(htm);
		}
		else 
		{
			NpcHtmlMessage noTeachMsg = new NpcHtmlMessage(getObjectId());
			noTeachMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(getObjectId())));
			player.sendPacket(noTeachMsg);
		}
	}
}
