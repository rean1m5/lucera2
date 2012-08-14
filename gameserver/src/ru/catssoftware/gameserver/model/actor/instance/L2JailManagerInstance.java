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

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.PlaySound;
import ru.catssoftware.gameserver.services.WindowService;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Util;

/**
 *
 * @author Rayan
 */

public class L2JailManagerInstance extends L2NpcInstance
{
	public L2JailManagerInstance(int objectId, L2NpcTemplate template)
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

		return "data/html/mods/jail/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("start_mission") && Config.ALLOW_JAILMANAGER)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/mods/jail/mission.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%playername%", player.getName());
			html.replace("%amount%", String.valueOf(Config.REQUIRED_JAIL_POINTS));
			html.replace("%npcname%", String.valueOf(getName()));
			player.sendPacket(html);

			if (player.isDead() || player.isFakeDeath())
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
				return;
			}
		}
		else if (command.equalsIgnoreCase("check_points") && player.isInJailMission())
		{
			NpcHtmlMessage html2 = new NpcHtmlMessage(getObjectId());
			html2.setFile("data/html/mods/jail/points.htm");
			html2.replace("%objectId%", String.valueOf(getObjectId()));
			html2.replace("%points%", String.valueOf(player.getJailPoints()));
			html2.replace("%rest%", String.valueOf(Config.REQUIRED_JAIL_POINTS - player.getJailPoints()));
			html2.replace("%npcname%", String.valueOf(getName()));
			player.sendPacket(html2);

			if (player.isDead() || player.isFakeDeath())
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
				return;
			}
		}
		else if (command.equalsIgnoreCase("get_mission"))
		{
			if (player.isInJailMission())
			{
				WindowService.sendWindow(player, "data/html/mods/jail/", "mission_already.htm");
				return;
			}
			player.setIsInJailMission(true);
			PlaySound ps = new PlaySound(0, "ItemSound2.race_start", 0, player.getObjectId(), player.getX(), player.getY(), player.getZ());
			player.sendPacket(ps);
			WindowService.sendWindow(player, "data/html/mods/jail/", "started.htm");

			if (player.isDead() || player.isFakeDeath())
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
				return;
			}
		}
		else if (command.equalsIgnoreCase("finish_mission"))
		{
			if (!player.isInJailMission())
			{
				WindowService.sendWindow(player, "data/html/mods/jail/", "notstarted.htm");
				return;
			}
			if (player.getJailPoints() < Config.REQUIRED_JAIL_POINTS)
			{
				WindowService.sendWindow(player, "data/html/mods/jail/", "notcompleted.htm");
				return;
			}
			if (player.isDead() || player.isFakeDeath())
			{
				player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
				return;
			}
			PlaySound ps = new PlaySound(0, "ItemSound.quest_finish", 0, player.getObjectId(), player.getX(), player.getY(), player.getZ());
			player.sendPacket(ps);
			WindowService.sendWindow(player, "data/html/mods/jail/", "completed.htm");

			try
			{
				Thread.sleep(Util.convertSecondsToMiliseconds(5));
			}
			catch (Throwable t)
			{
				_log.error("Error, L2JailManagerInstance, reason: " + t.getMessage());
			}
			player.setInJail(false, 0);
			player.resetJailPoints();
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_JAIL_POINTS_RESET));
			player.setIsInJailMission(false);
		}
		super.onBypassFeedback(player, command);
	}
}
