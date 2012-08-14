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

import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;



public class L2NpcBufferInstance extends L2NpcInstance
{

	private Quest _quest = null;
	public L2NpcBufferInstance (int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	@Override
	public void onAction(L2PcInstance player) {
		if(_quest==null) {
			_quest = QuestManager.getInstance().getQuest("50000_Buffer");
			if( _quest == null) { 
				super.onAction(player);
				return;
			}
			_quest.addFirstTalkId(getNpcId());
			_quest.addStartNpc(getNpcId());
			_quest.addTalkId(getNpcId());
		}
		super.onAction(player);
	}
	
	
}