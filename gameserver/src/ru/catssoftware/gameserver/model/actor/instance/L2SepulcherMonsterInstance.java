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

import ru.catssoftware.gameserver.instancemanager.FourSepulchersManager.FourSepulchersMausoleum;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


/**
 *
 * @author  sandman
 */
public class L2SepulcherMonsterInstance extends L2MonsterInstance
{
	protected FourSepulchersMausoleum		_mausoleum;
	
	public L2SepulcherMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setShowSummonAnimation(true);
	}

	private int _stage;
	public void setStage(int stage) {
		_stage = stage;
	}
	public int getStage() {
		return _stage;
	}
	
	public void setMausoleum(FourSepulchersMausoleum mausoleum) {
		_mausoleum = mausoleum;
	}

	@Override
	public void onSpawn()
	{
		setShowSummonAnimation(false);
		super.onSpawn();		
	}
	@Override 
	public boolean doDie(L2Character killer) {
		if(!super.doDie(killer))
			return false;
		if(_mausoleum!=null)
			_mausoleum.onKill(this,killer);
		
		return true;
	}
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return true;
	}
	
}
