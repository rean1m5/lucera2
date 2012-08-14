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

import ru.catssoftware.gameserver.network.serverpackets.ExQuestInfo;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision: $ $Date: $
 * @author  LBaldi
 */
public class L2AdventurerInstance extends L2FolkInstance
{
	public L2AdventurerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("questlist"))
			player.sendPacket(new ExQuestInfo());
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	protected String getHtmlFolder() {
		return "adventurer_guildsman";
	}
}