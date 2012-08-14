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

import java.util.StringTokenizer;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.instancemanager.games.fishingChampionship;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2SkillLearn;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.AcquireSkillList;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


public class L2FishermanInstance extends L2MerchantInstance
{
	/**
	 * @param objectId
	 * @param template
	 */
	public L2FishermanInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val,L2PcInstance talker)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/fisherman/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("FishSkillList"))
		{
			player.setSkillLearningClassId(player.getClassId());
			showSkillList(player);
		}
		if (command.startsWith("fishingChampionship"))
		{
			showChampScreen(player);
		}
		if (command.startsWith("fishingReward"))
		{
			fishingChampionship.getInstance().getReward(player);
		}
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String cmd = st.nextToken();

		if (cmd.equalsIgnoreCase("Buy"))
		{
			if (st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if (cmd.equalsIgnoreCase("Sell"))
			showSellWindow(player);
		else
			super.onBypassFeedback(player, command);
	}

	public void showSkillList(L2PcInstance player)
	{
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Fishing);

		int counts = 0;

		for (L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

			if (sk == null)
				continue;

			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 1);
		}

		if (counts == 0)
		{
			SystemMessage sm;
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player);
			if (minlevel > 0)
			{
				// No more skills to learn, come back when you level.
				sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_REACHED_S1);
				sm.addNumber(minlevel);
			}
			else
				sm = new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);

			player.sendPacket(sm);
		}
		else
			player.sendPacket(asl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	public void showChampScreen(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String str;
		str = "<html><head><title>Королевский турнир по ловле рыбы</title></head>";
		str += "Член Гильдии Рыболовов:<br><br>";
		str += "Здравствуйте! У меня есть список победителей турнира по рыбной ловле прошлой недели!<br>";
		str += "Ваше имя есть в списке? Если да, то я вручу Вам приз!<br>";
		str += "Помните, что Вы можете забрать его только<font color=\"LEVEL\"> в течение этой недели</font>.<br>";
		str += "Не расстраивайтесь, если не удалось выиграть! Повезет в следующий раз!<br>";
		str += "Это сообщение будет обновлено через "+fishingChampionship.getInstance().getTimeRemaining()+" мин!<br>";
		str += "<center><a action=\"bypass -h npc_%objectId%_fishingReward\">Получить приз</a><br></center>";
		str += "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>Место</td><td width=110 align=center>Рыбак</td><td width=80 align=center>Длина</td></tr></table><table width=280>";
		for(int x=1;x<=5;x++)
		{
			str += "<tr><td width=70 align=center>"+x+" Место:</td>";			
			str += "<td width=110 align=center>"+fishingChampionship.getInstance().getWinnerName(x)+"</td>";
			str += "<td width=80 align=center>"+fishingChampionship.getInstance().getFishLength(x)+"</td></tr>";			
		}
		str += "<td width=80 align=center>0</td></tr></table><br>";
		str += "Список призов<br><table width=280 border=0 bgcolor=\"000000\"><tr><td width=70 align=center>Место</td><td width=110 align=center>Приз</td><td width=80 align=center>Количество</td></tr></table><table width=280>";
		str += "<tr><td width=70 align=center>1 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>800000</td></tr><tr><td width=70 align=center>2 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>500000</td></tr><tr><td width=70 align=center>3 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>300000</td></tr>";
		str += "<tr><td width=70 align=center>4 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>200000</td></tr><tr><td width=70 align=center>5 Место:</td><td width=110 align=center>аден</td><td width=80 align=center>100000</td></tr></table></body></html>";
		html.setHtml(str);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);		
	}
}