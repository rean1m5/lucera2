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


import javolution.text.TextBuilder;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.actor.knownlist.ObjectKnownList;
import ru.catssoftware.gameserver.model.actor.poly.ObjectPoly;
import ru.catssoftware.gameserver.model.actor.position.ObjectPosition;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.lang.L2Entity;




public abstract class L2Object extends L2Entity
{
	public static final L2Object[] EMPTY_ARRAY = new L2Object[0];
	private String				_name;
	private ObjectPoly			_poly;
	private ObjectPosition		_position;
	private int					_instanceId	= 0;
	private int					_lastInstance = 0;
	private boolean				_inWorld;

	protected L2Object(int objectId)
	{
		super(objectId);
		_name = "";
	}

	public void onAction(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onActionShift(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onForcedAttack(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onSpawn()
	{
	}

	public void firstSpawn()
	{
		onSpawn();
	}

	public final int getX()
	{
		return getPosition().getX();
	}

	public final int getY()
	{
		return getPosition().getY();
	}

	public final int getZ()
	{
		return getPosition().getZ();
	}

	public void decayMe()
	{
		L2WorldRegion reg = getPosition().getWorldRegion();

		synchronized (this)
		{
			getPosition().clearWorldRegion();
		}
		_inWorld = false;
		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);
	}

	public boolean isInWorld() {
		return _inWorld;
	}

	private void spawnMe(boolean firstspawn)
	{
		synchronized (this)
		{
			getPosition().updateWorldRegion();
		}

		L2World.getInstance().storeObject(this);

		getPosition().getWorldRegion().addVisibleObject(this);

		L2World.getInstance().addVisibleObject(this, null);
		_inWorld = true;
		if (firstspawn)
			firstSpawn();
		else
			onSpawn();
	}

	public final void spawnMe()
	{
		spawnMe(false);
	}

	public final void spawnMe(int x, int y, int z, boolean firstspawn)
	{
		synchronized (this)
		{
			getPosition().setWorldPosition(x, y, z);
		}

		spawnMe(firstspawn);
	}

	public final void spawnMe(int x, int y, int z)
	{
		spawnMe(x, y, z, false);
	}

	public void toggleVisible()
	{
		if (isVisible())
			decayMe();
		else
			spawnMe();
	}

	public boolean isAttackable()
	{
		return false;
	}

	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	public final boolean isVisible()
	{
		return getPosition().getWorldRegion() != null;
	}

	public ObjectKnownList getKnownList()
	{
		return ObjectKnownList.getInstance();
	}

	public final String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = (name == null ? "" : name.intern());
	}


	public final ObjectPoly getPoly()
	{
		if (_poly == null)
			_poly = new ObjectPoly();
		return _poly;
	}

	public final ObjectPosition getPosition()
	{
		if (_position == null)
			_position = new ObjectPosition(this);
		return _position;
	}

	public L2WorldRegion getWorldRegion()
	{
		return getPosition().getWorldRegion();
	}

	public int getInstanceId()
	{
		return _instanceId;
	}

	public void setInstanceId(int instanceId)
	{
		if (_instanceId == instanceId)
			return;

		if (this instanceof L2PcInstance)
		{

			if (_instanceId > 0)
			{
				Instance inst = InstanceManager.getInstance().getInstance(_instanceId);
				if (inst != null)
					inst.removePlayer(getObjectId());
			}
			if (instanceId > 0)
			{
				Instance inst = InstanceManager.getInstance().getInstance(instanceId);
				if (inst != null)
					inst.addPlayer(getObjectId());
			}
			if (((L2PcInstance)this).getPet() != null)
				((L2PcInstance)this).getPet().setInstanceId(instanceId);
		}

		_instanceId = instanceId;
		if (instanceId>0)
			_lastInstance=instanceId;

		if (isVisible())
		{
			if (this instanceof L2PcInstance){}
			else
			{
				decayMe();
				spawnMe();
			}
		}
	}
	public int getLastInstanceId()
	{
		return _lastInstance;
	}
	@Override
	public String toString()
	{
		TextBuilder tb = TextBuilder.newInstance();
		tb.append("(");
		tb.append(getClass().getSimpleName());
		tb.append(") ");
		tb.append(getObjectId());
		tb.append(" - ");
		tb.append(getName());

		try
		{
			return tb.toString();
		}
		finally
		{
			TextBuilder.recycle(tb);
		}
	}

	public L2PcInstance getActingPlayer()
	{
		return null;
	}

	public final static L2PcInstance getActingPlayer(L2Object obj)
	{
		return (obj == null ? null : obj.getActingPlayer());
	}

	public L2Summon getActingSummon()
	{
		return null;
	}

	public final static L2Summon getActingSummon(L2Object obj)
	{
		return (obj == null ? null : obj.getActingSummon());
	}

	public boolean isInFunEvent()
	{
		L2PcInstance player = getActingPlayer();

		return (player != null && player.isInFunEvent());
	}

	public Location getLoc()
	{
		return new Location(getX(), getY(), getZ(), 0);
	}

	public Integer getPrimaryKey()
	{
		return getObjectId();
	}

	public int getHeading() {
		return 0;
	}

	public void reset()
	{
	}

	public int getColHeight() {
		return 50;
	}
	public int  getColRadius() {
		return 50;
	}
	public L2GameServerPacket getInfoPacket() {
		return null;
	}

	public L2PcInstance getPlayer()
	{
		return null;
	}

	public boolean isPlayer()
	{
		return false;
	}

	public L2Character getCharacter()
	{
		return null;
	}

	public boolean isCharacter()
	{
		return false;
	}

	public L2NpcInstance getNpc()
	{
		return null;
	}

	public boolean isNpc()
	{
		return false;
	}

	public L2GuardInstance getGuard()
	{
		return null;
	}

	public boolean isGuard()
	{
		return false;
	}

	public L2MonsterInstance getMonster()
	{
		return null;
	}

	public boolean isMonster()
	{
		return false;
	}

	public L2RaidBossInstance getBoss()
	{
		return null;
	}

	public boolean isBoss()
	{
		return false;
	}

	public L2GrandBossInstance getGrandBoss()
	{
		return null;
	}

	public boolean isGrandBoss()
	{
		return false;
	}

	public L2DoorInstance getDoor()
	{
		return null;
	}

	public boolean isDoor()
	{
		return false;
	}


	public L2Summon getSummon()
	{
		return null;
	}

	public boolean isSummon()
	{
		return false;
	}

}