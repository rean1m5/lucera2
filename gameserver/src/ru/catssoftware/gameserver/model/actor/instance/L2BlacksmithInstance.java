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

import ru.catssoftware.gameserver.model.L2Multisell;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author zabbix
 * Lets drink to code!
 */
public class L2BlacksmithInstance extends L2FolkInstance
{

	public L2BlacksmithInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("multisell"))
		{
			int listId = Integer.parseInt(command.substring(9).trim());
			L2Multisell.getInstance().separateAndSend(listId, player, false, getCastle().getTaxRate());
		}

		super.onBypassFeedback(player, command);
	}

	@Override
	protected String getHtmlFolder() {
		return "blacksmith";
	}

}
