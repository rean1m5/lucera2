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

import javolution.text.TextBuilder;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.CharTemplateTable;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.L2Clan.SubPledge;
import ru.catssoftware.gameserver.model.base.*;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.gameserver.util.PcAction;

import java.util.Set;


/**
 * This class ...
 *
 * @version $Revision: 1.4.2.3.2.8 $ $Date: 2005/03/29 23:15:15 $
 */
public class L2VillageMasterInstance extends L2FolkInstance
{
	/**
	 * @param template
	 */
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		String[] commandStr = command.split(" ");
		String actualCommand = commandStr[0]; // Get actual command

		String cmdParams = "";
		String cmdParams2 = "";

		if (commandStr.length >= 2)
			cmdParams = commandStr[1];
		if (commandStr.length >= 3)
			cmdParams2 = commandStr[2];

		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
				return;

			ClanTable.getInstance().createClan(player, command.substring(actualCommand.length()).trim());
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.isEmpty())
				return;

			createSubPledge(player, cmdParams, null, -1, 5);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.isEmpty())
				return;

			createSubPledge(player, cmdParams, cmdParams2, 100, 6);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.isEmpty())
				return;

			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("rename_royal1") || actualCommand.equalsIgnoreCase("rename_royal2")
				|| actualCommand.equalsIgnoreCase("rename_knights1") || actualCommand.equalsIgnoreCase("rename_knights2")
				|| actualCommand.equalsIgnoreCase("rename_knights3") || actualCommand.equalsIgnoreCase("rename_knights4"))
		{
			if (cmdParams.isEmpty())
				return;
			renameSubPledge(player, cmdParams, actualCommand);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.isEmpty())
				return;

			createSubPledge(player, cmdParams, cmdParams2, 1001, 7);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
				return;

			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
				return;
			}
			player.getClan().createAlly(player, command.substring(actualCommand.length()).trim());
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
				return;
			}
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
			dissolveClan(player, player.getClanId());
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
				return;

			changeClanLeader(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
			recoverClan(player, player.getClanId());
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			player.getClan().levelUpClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
			showPledgeSkillList(player);
		else if (command.contains("Link villagemaster/SubClass.htm"))
		{
				String html = "";
				html = HtmCache.getInstance().getHtm("data/html/villagemaster/SubClass.htm",player);
				NpcHtmlMessage subMsg = new NpcHtmlMessage(getObjectId());
				subMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(getObjectId())));
				player.sendPacket(subMsg);
		}
		else if (command.startsWith("Subclass"))
		{
			int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());

			if(Olympiad.getInstance().isRegistered(player) || Olympiad.getInstance().isRegisteredInComp(player)) {
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_YOU_IN_OLYMPIAD));
				return;
			}
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
				return;
			}
			if (player.getPet() != null)
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
				return;
			}

			TextBuilder content = new TextBuilder("<html><body>");
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			Set<PlayerClass> subsAvailable;

			int paramOne = 0;
			int paramTwo = 0;

			try
			{
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
					endIndex = command.length();

				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
				if (command.length() > endIndex)
					paramTwo = Integer.parseInt(command.substring(endIndex).trim());
			}
			catch (Exception NumberFormatException)
			{
			}

			switch (cmdChoice)
			{
				case 1: // Add Subclass - Initial
					// Avoid giving player an option to add a new sub class, if they have three already.
					if (player.getTotalSubClasses() == Config.MAX_SUBCLASS)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_MAX_SUB_CLASS));
						return;
					}
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					subsAvailable = getAvailableSubClasses(player);
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						content.append("Добавить суб-класс:<br>Какой суб-класс желаете добавить?<br>");
						for (PlayerClass subClass : subsAvailable)
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 4 " + subClass.ordinal() + "\" msg=\"1268;" + CharTemplateTable.getClassNameById(subClass.ordinal()) + "\">" + CharTemplateTable.getClassNameById(subClass.ordinal()) + "</a><br>");
					}
					else
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_SUB_CLASS));
						return;
					}
					break;
				case 2:
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					content.append("Сменить суб-класс:<br>");
					final int baseClassId = player.getBaseClass();
					if (player.getSubClasses().isEmpty())
						content.append("Вы должны добавить хотя бы 1 суб-класс для его смены.<br>" + "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 1\">Добавить суб-класс.</a>");
					else
					{
						content.append("Какой суб-класс желаете выбрать?<br>");
						if (baseClassId == player.getActiveClass())
							content.append(CharTemplateTable.getClassNameById(baseClassId) + "&nbsp;<font color=\"LEVEL\">(Основной класс)</font><br>");
						else
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 0\">" + CharTemplateTable.getClassNameById(baseClassId) + "</a>&nbsp;" + "<font color=\"LEVEL\">(Основной класс)</font><br>");

						for (SubClass subClass : player.getSubClasses().values())
						{
							int subClassId = subClass.getClassId();
							if (subClassId == player.getActiveClass())
								content.append(CharTemplateTable.getClassNameById(subClassId) + "<br>");
							else
								content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClassId) + "</a><br>");
						}
					}
					break;
				case 3:
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					content.append("Сменить суб-класс:<br>Какой из суб-класс вы хотели бы сменить?<br>");
					int classIndex = 1;
					for (SubClass subClass : player.getSubClasses().values())
					{
						content.append("Суб-класс " + classIndex++ + "<br1>");
						content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 6 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClass.getClassId()) + "</a><br>");
					}
					content.append("<br>Ваш новый суб-класс получит 40 уровень и 2 профессию.");
					break;
				case 4:
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					boolean allowAddition = true;
					if (!FloodProtector.tryPerformAction(player, Protected.SUBCLASS))
					{
						TextBuilder text = new TextBuilder();
						text.append("<html><head><body><font color=\"LEVEL\">Сообщение сервера:</font><br>Подождите несколько секунд.</body></html>");
						html.setHtml(text.toString());
						player.sendPacket(html);
						return;
					}
					if (player.getLevel() < 75)
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
						allowAddition = false;
					}
					if (player.getGameEvent()!=null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
						return;
					}
					if (allowAddition)
					{
						if (!player.getSubClasses().isEmpty())
						{
							for (SubClass subClass : player.getSubClasses().values())
							{
								if (subClass.getLevel() < 75)
								{
									html.setFile("data/html/villagemaster/SubClass_Fail.htm");
									allowAddition = false;
									break;
								}
							}
						}
					}

					if (Config.SUBCLASS_WITH_CUSTOM_ITEM && Config.SUBCLASS_WITH_CUSTOM_ITEM_COUNT>=0)
						allowAddition = PcAction.removeItem(player,
								Config.SUBCLASS_WITH_CUSTOM_ITEM_ID,
								Config.SUBCLASS_WITH_CUSTOM_ITEM_COUNT,
								"master subclass");

					if(!allowAddition && !(allowAddition = Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS))
					{
						QuestState qs = player.getQuestState("235_MimirsElixir");
						allowAddition = qs != null && qs.isCompleted();
					}

					if (allowAddition)
					{
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
						{
							player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CANT_ADD_SUB));
							return;
						}
						player.setActiveClass(player.getTotalSubClasses());
						content.append("Суб-класс добавлен:<br>Поздравляю! Вы добавили новый суб-класс. <br> Откройте окно персонажа (ALT + T) для убеждения.");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS);
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
							html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					}

					break;
				case 5:
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					if (player.getGameEvent()!=null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
						return;
					}
					if (!FloodProtector.tryPerformAction(player, Protected.SUBCLASS))
					{
						TextBuilder text = new TextBuilder();
						text.append("<html><head><body><font color=\"LEVEL\">Сообщение сервера:</font><br>Подождите несколько секунд.</body></html>");
						html.setHtml(text.toString());
						player.sendPacket(html);
						return;
					}
					player.stopAllEffects();
					player.clearCharges();
					player.setActiveClass(paramOne);
					content.append("Суб-класс изменен:<br>Ваш текущий суб-класс <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getActiveClass()) + "</font>.");
					player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED);
					if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
						player.checkAllowedSkills();
					break;
				case 6:
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					content.append("Выберите суб-класс для изменения. Если нужного класса сдесь нет, " + "то найдите соответствующего мастера.<br>" + "<font color=\"LEVEL\">Внимание!</font> Вся информация этого класса будет удалена.<br><br>");

					subsAvailable = getAvailableSubClasses(player);
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						for (PlayerClass subClass : subsAvailable)
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 7 " + paramOne + " " + subClass.ordinal() + "\">" + CharTemplateTable.getClassNameById(subClass.ordinal()) + "</a><br>");
					}
					else
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_SUB_CLASS));
						return;
					}
					break;
				case 7:
					if (player.getPet() != null)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNSUMMON_YOUR_PET));
						return;
					}
					if (!FloodProtector.tryPerformAction(player, Protected.SUBCLASS))
					{
						TextBuilder text = new TextBuilder();
						text.append("<html><head><body><font color=\"LEVEL\">Сообщение сервера:</font><br>Подождите несколько секунд.</body></html>");
						html.setHtml(text.toString());
						player.sendPacket(html);
						return;
					}
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.stopAllEffects(); // all effects from old subclass stopped!
						player.clearCharges();
						player.setActiveClass(paramOne);
						content.append("Смена суб-класса:<br>Ваш класс изменен на <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(paramTwo) + "</font>.");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS);
						player.sendPacket(ActionFailed.STATIC_PACKET);
						if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
							player.checkAllowedSkills();
					}
					else
					{
						player.setActiveClass(0);
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CANT_ADD_SUB));
						return;
					}
					break;
			}
			content.append("</body></html>");
			if (content.length() > 26)
				html.setHtml(content.toString());
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	protected String getHtmlFolder() {
		return "villagemaster";
	}
	

	public void dissolveClan(L2PcInstance player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY);
			return;
		}
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR);
			return;
		}
		if (clan.getHasCastle() != 0 || clan.getHasHideout() != 0 || clan.getHasFort() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE);
			return;
		}
		for (Castle castle : CastleManager.getInstance().getCastles().values())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getCastleId()))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
				return;
			}
		}
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getFortId()))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
				return;
			}
		}
		if (SiegeManager.getInstance().checkIfInZone(player))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
			return;
		}
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.DISSOLUTION_IN_PROGRESS);
			return;
		}
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L); //24*60*60*1000 = 86400000
		clan.updateClanInDB();
		ClanTable.getInstance().scheduleRemoveClan(clan.getClanId());
		player.deathPenalty(false, false);
	}

	public void recoverClan(L2PcInstance player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		L2Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}

	public void changeClanLeader(L2PcInstance player, String target)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (player.getName().equalsIgnoreCase(target))
			return;
		if (player.isFlying())
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_UNRIDE_PET));
			return;
		}
		L2Clan clan = player.getClan();
		if(SiegeManager.getInstance().getSiege(clan)!=null && SiegeManager.getInstance().getSiege(clan).getIsInProgress()) {
			SystemMessage sm = new SystemMessage(SystemMessageId.S1).addString("Can't do it while siege in progress");
			player.sendPacket(sm);
			sm = null;
			return;
		}
		L2ClanMember member = clan.getClanMember(target);
		if (member == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
			sm.addString(target);
			player.sendPacket(sm);
			sm = null;
			return;
		}
		if (!member.isOnline())
		{
			player.sendPacket(SystemMessageId.INVITED_USER_NOT_ONLINE);
			return;
		}
		if(member.getSubPledgeType()==L2Clan.SUBUNIT_ACADEMY) {
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CANT_GIVE_TO_ACADEM));
			return;
		}
		clan.setNewLeader(member);
	}

	public void createSubPledge(L2PcInstance player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		L2Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY);
			else
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			return;
		}
		if (!Config.CLAN_ALLY_NAME_PATTERN.matcher(clanName).matches())
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY && clan.getClanMember(leaderName) == null)
			return;
		int leaderId = pledgeType != L2Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		if (leaderId != 0 && clan.getLeaderSubPledge(leaderId) != 0)
		{
			player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
			return;
		}
		for (L2Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
					sm.addString(clanName);
					player.sendPacket(sm);
				}
				else
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
				return;
			}
		}
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getSubPledgeType() != 0)
			{
				if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				return;
			}
		}
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
			return;

		SystemMessage sm;
		if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = new SystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = new SystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = new SystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
			sm = new SystemMessage(SystemMessageId.CLAN_CREATED);

		player.sendPacket(sm);
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			if (leaderSubPledge.getPlayerInstance() == null)
				return;
			leaderSubPledge.getPlayerInstance().setPledgeClass(L2ClanMember.getCurrentPledgeClass(leaderSubPledge.getPlayerInstance()));
			leaderSubPledge.getPlayerInstance().sendPacket(new UserInfo(leaderSubPledge.getPlayerInstance()));
			try
			{
				clan.getClanMember(leaderName).updateSubPledgeType();
				for (L2Skill skill : leaderSubPledge.getPlayerInstance().getAllSkills())
					leaderSubPledge.getPlayerInstance().removeSkill(skill, false);
				clan.getClanMember(leaderName).getPlayerInstance().setActiveClass(0);
			}
			catch (Throwable t)
			{
			}

			for (L2ClanMember member : clan.getMembers())
			{
				if (member == null || member.getPlayerInstance() == null || member.getPlayerInstance().isOnline() == 0)
					continue;
				SubPledge[] subPledge = clan.getAllSubPledges();
				for (SubPledge element : subPledge)
					member.getPlayerInstance().sendPacket(new PledgeReceiveSubPledgeCreated(element, clan));
			}
		}
	}

	public void renameSubPledge(L2PcInstance player, String newName, String command)
	{
		if (player == null || player.getClan() == null || !player.isClanLeader())
		{
			if (player != null)
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		L2Clan clan = player.getClan();
		SubPledge[] subPledge = clan.getAllSubPledges();
		for (SubPledge element : subPledge)
		{
			switch (element.getId())
			{
				case 100: // 1st Royal Guard
					if (command.equalsIgnoreCase("rename_royal1"))
					{
						changeSubPledge(clan, element, newName);
						return;
					}
					break;
				case 200: // 2nd Royal Guard
					if (command.equalsIgnoreCase("rename_royal2"))
					{
						changeSubPledge(clan, element, newName);
						return;
					}
					break;
				case 1001: // 1st Order of Knights
					if (command.equalsIgnoreCase("rename_knights1"))
					{
						changeSubPledge(clan, element, newName);
						return;
					}
					break;
				case 1002: // 2nd Order of Knights
					if (command.equalsIgnoreCase("rename_knights2"))
					{
						changeSubPledge(clan, element, newName);
						return;
					}
					break;
				case 2001: // 3rd Order of Knights
					if (command.equalsIgnoreCase("rename_knights3"))
					{
						changeSubPledge(clan, element, newName);
						return;
					}
					break;
				case 2002: // 4th Order of Knights
					if (command.equalsIgnoreCase("rename_knights4"))
					{
						changeSubPledge(clan, element, newName);
						return;
					}
					break;
			}
		}
		player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_ERROR_CONTACT_GM));
	}

	public void changeSubPledge(L2Clan clan, SubPledge element, String newName)
	{
		if (newName.length() > 16 || newName.length() < 3)
		{
			clan.getLeader().getPlayerInstance().sendPacket(SystemMessageId.CLAN_NAME_TOO_LONG);
			return;
		}
		String oldName = element.getName();
		element.setName(newName);
		clan.updateSubPledgeInDB(element.getId());
		for (L2ClanMember member : clan.getMembers())
		{
			if (member == null || member.getPlayerInstance() == null || member.getPlayerInstance().isOnline() == 0)
				continue;
			if(member.getSubPledgeType()==L2Clan.SUBUNIT_ACADEMY) 
				continue;
			SubPledge[] subPledge = clan.getAllSubPledges();
			for (SubPledge sp : subPledge)
				member.getPlayerInstance().sendPacket(new PledgeReceiveSubPledgeCreated(sp, clan));
			if (member.getPlayerInstance() != null)
				member.getPlayerInstance().sendMessage("Clan sub unit " + oldName + "'s name has been changed into " + newName + ".");
		}
	}

	public void assignSubPledgeLeader(L2PcInstance player, String clanName, String leaderName)
	{
		L2Clan clan = player.getClan();
		if (clan == null)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}

		if (player.getName().equals(leaderName))
		{
			player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}

		SubPledge subPledge = player.getClan().getSubPledge(clanName);

		if (null == subPledge)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}

		if (subPledge.getId() == L2Clan.SUBUNIT_ACADEMY)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}

		L2PcInstance newLeader = L2World.getInstance().getPlayer(leaderName);
		if (newLeader == null || newLeader.getClan() == null || newLeader.getClan() != clan)
		{
			player.sendMessage(String.format(Message.getMessage(player, Message.MessageId.MSG_NOT_FOUND_IN_CLAN), leaderName));
			return;
		}

		if (clan.getClanMember(leaderName) == null || (clan.getClanMember(leaderName).getSubPledgeType() != 0))
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}

		try
		{
			L2ClanMember oldLeader = clan.getClanMember(subPledge.getLeaderId());
			String oldLeaderName = oldLeader == null ? "" : oldLeader.getName();
			clan.getClanMember(oldLeaderName).setSubPledgeType(0);
			clan.getClanMember(oldLeaderName).updateSubPledgeType();
			clan.getClanMember(oldLeaderName).getPlayerInstance().setPledgeClass(L2ClanMember.getCurrentPledgeClass(clan.getClanMember(oldLeaderName).getPlayerInstance()));
			clan.getClanMember(oldLeaderName).getPlayerInstance().setActiveClass(0);
		}
		catch (Throwable t)
		{
		}

		int leaderId = clan.getClanMember(leaderName).getObjectId();

		subPledge.setLeaderId(leaderId);
		clan.updateSubPledgeInDB(subPledge.getId());
		L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		leaderSubPledge.getPlayerInstance().setPledgeClass(L2ClanMember.getCurrentPledgeClass(leaderSubPledge.getPlayerInstance()));
		leaderSubPledge.getPlayerInstance().sendPacket(new UserInfo(leaderSubPledge.getPlayerInstance()));
		clan.broadcastClanStatus();
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2);
		sm.addString(leaderName);
		sm.addString(clanName);
		clan.broadcastToOnlineMembers(sm);
		sm = null;
	}

	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		int baseClassId = player.getBaseClass();
		if ((baseClassId >= 88 && baseClassId <= 118) || (baseClassId >= 131 && baseClassId <= 134) || baseClassId == 136)
			baseClassId = ClassId.values()[baseClassId].getParent().getId();

		PlayerClass baseClass = PlayerClass.values()[baseClassId];

		final Race npcRace = getVillageMasterRace();
		final ClassType npcTeachType = getVillageMasterTeachType();

		Set<PlayerClass> availSubs = baseClass.getAvailableSubclasses(player);

		if (availSubs != null && !availSubs.isEmpty())
		{
			for (PlayerClass availSub : availSubs)
			{
				for (SubClass subClass : player.getSubClasses().values())
				{
					int subClassId = subClass.getClassId();
					if ((subClassId >= 88 && subClassId <= 118) || (subClassId >= 131 && subClassId <= 134) || subClassId == 136)
						subClassId = ClassId.values()[subClassId].getParent().getId();

					if (availSub.ordinal() == subClassId || availSub.ordinal() == baseClassId)
						availSubs.remove(availSub);
				}

				if (npcRace == Race.Human || npcRace == Race.Elf)
				{
					if (!availSub.isOfType(npcTeachType))
						availSubs.remove(availSub);
					else if (!availSub.isOfRace(Race.Human) && !availSub.isOfRace(Race.Elf))
						availSubs.remove(availSub);
				}
				else
				{
					if (!availSub.isOfRace(npcRace))
						availSubs.remove(availSub);
				}
			}
		}
		return availSubs;
	}

	public void showPledgeSkillList(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		if (player.getClan() == null || !player.isClanLeader())
		{
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("<br><br>Вы не в праве изучать клан скиллы.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Clan);
		int counts = 0;

		for (L2PledgeSkillLearn s : skills)
		{
			int cost = s.getRepCost();
			counts++;

			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if (counts == 0)
		{
			if (player.getClan().getLevel() < 8)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_REACHED_S1);
				if (player.getClan().getLevel() < 5)
					sm.addNumber(5);
				else
					sm.addNumber(player.getClan().getLevel() + 1);
				player.sendPacket(sm);
				player.sendPacket(new AcquireSkillDone());
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
				player.sendPacket(sm);
			}
		}
		else
			player.sendPacket(asl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private final Race getVillageMasterRace()
	{
		return getTemplate().getNpcRace();
	}

	protected ClassType getVillageMasterTeachType()
	{
		return ClassType.Fighter;
	}
}
