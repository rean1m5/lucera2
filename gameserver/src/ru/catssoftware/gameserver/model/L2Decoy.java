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

package ru.catssoftware.gameserver.model;

import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.knownlist.DecoyKnownList;
import ru.catssoftware.gameserver.network.serverpackets.CharInfo;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.item.L2Weapon;

public  class L2Decoy extends L2Character
{
	private L2PcInstance	_owner;

	public L2Decoy(L2PcInstance owner)
	{
		super(IdFactory.getInstance().getNextId(), null);
		getKnownList();
		_owner = owner;
		getPosition().setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
		setHeading(owner.getHeading());
	}

	private boolean _isSit;
	public void sitDown() {
		_isSit = true;
	}
	public void standUp() {
		_isSit = false;
	}
	public boolean isSitting() {
		return _isSit;
	}
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		broadcastFullInfo();
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		player.setTarget(this);
		MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
		player.sendPacket(my);
	}

	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	@Override
	public DecoyKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new DecoyKnownList(this);

		return (DecoyKnownList) _knownList;
	}

	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}


	@Override
	public int getLevel()
	{
		return _owner.getLevel();
	}

	public void deleteMe(L2PcInstance owner)
	{
		decayMe();
		getKnownList().removeAllKnownObjects();
		
	}


	public final L2PcInstance getOwner()
	{
		return _owner;
	}

	@Override
	public L2PcInstance getActingPlayer()
	{
		return _owner;
	}

	@Override
	public void broadcastFullInfoImpl()
	{
		broadcastPacket(new CharInfo(this));
	}
}