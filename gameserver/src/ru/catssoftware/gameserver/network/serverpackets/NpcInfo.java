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
package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeFlagInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.9 $ $Date: 2005/04/11 10:05:54 $
 */
public class NpcInfo extends L2GameServerPacket
{
	//   ddddddddddddddddddffffdddcccccSSddd dddddc
	//   ddddddddddddddddddffffdddcccccSSddd dddddccffd

	private static final String	_S__22_NPCINFO	= "[S] 16 NpcInfo";
	private L2Character			_activeChar;
	private int					_x, _y, _z, _heading;
	private int					_idTemplate;
	private boolean				_isSummoned;
	private int					_mAtkSpd, _pAtkSpd;
	private int					_runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	@SuppressWarnings("unused")
	private int					_rhand, _lhand, _chest;
	private int					_collisionHeight, _collisionRadius;
	private String				_name			= "";
	private String				_title			= "";
	@SuppressWarnings("unused")
	private int					_form			= 0;

	/**
	 * @param _activeCharracters
	 */
	public NpcInfo(L2NpcInstance cha)
	{
		_activeChar = cha;
		_idTemplate = cha.getTemplate().getIdTemplate();
		_rhand = cha.getRightHandItem();
		_lhand = cha.getLeftHandItem();
		_isSummoned = cha.isShowSummonAnimation();
		_collisionHeight = cha.getCollisionHeight();
		_collisionRadius = cha.getCollisionRadius();
		_name = cha.getName();

		if (cha.isChampion())
			_title = (Config.CHAMPION_TITLE);
		else 
			_title = cha.getTitle();

		if (Config.SHOW_NPC_LVL && _activeChar instanceof L2MonsterInstance)
		{
			String t = "Lv " + cha.getLevel() + (cha.getAggroRange() > 0 ? "*" : "");
			if (_title != null && !_title.isEmpty())
				t += " " + _title;

			_title = t;
		}
		if (_activeChar instanceof L2SiegeFlagInstance)
			_title = cha.getTitle();

		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_runSpd = _activeChar.getTemplate().getBaseRunSpd();
		_walkSpd = _activeChar.getTemplate().getBaseWalkSpd();
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}

	public NpcInfo(L2Summon cha)
	{
		_activeChar = cha;
		_idTemplate = cha.getTemplate().getIdTemplate();
		_rhand = cha.getWeapon();
		_lhand = 0;
		_chest = cha.getArmor();
		_collisionHeight = _activeChar.getTemplate().getCollisionHeight();
		_collisionRadius = _activeChar.getTemplate().getCollisionRadius();

		_name = cha.getName();
		_title = cha.getOwner() != null ? (cha.getOwner().isOnline() == 0 ? "" : cha.getOwner().getName()) : ""; // when owner online, summon will show in title owner name
		int npcId = cha.getTemplate().getNpcId();
		if (npcId == 16041 || npcId == 16042)
		{
			if (cha.getLevel() > 84)
				_form = 3;
			else if (cha.getLevel() > 79)
				_form = 2;
			else if (cha.getLevel() > 74) 
				_form = 1;
		}
		else if (npcId == 16025 || npcId == 16037)
		{
			if (cha.getLevel() > 69)
				_form = 3;
			else if (cha.getLevel() > 64) 
				_form = 2;
			else if (cha.getLevel() > 59)
				_form = 1;
		}

		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_runSpd = cha.getPetSpeed();
		_walkSpd = cha.isMountable() ? 45 : 30;
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}


	@Override
	protected void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		if(_activeChar instanceof L2Summon)
			if(((L2Summon) _activeChar).getOwner() != null && ((L2Summon) _activeChar).getOwner().getAppearance().isInvisible())
				return;
		writeC(0x16);
		writeD(_activeChar.getObjectId());
		writeD(_idTemplate + 1000000); // npctype id
		writeD(_activeChar.isAutoAttackable(activeChar) ? 1 : 0);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0x00);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd/*0x32*/); // swimspeed
		writeD(_swimWalkSpd/*0x32*/); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(1.1/*_activeChar.getProperMultiplier()*/);
		//writeF(1/*_activeChar.getAttackSpeedMultiplier()*/);
		writeF(_pAtkSpd / 277.478340719);
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(_rhand); // right hand weapon
		writeD(0);
		writeD(_lhand); // left hand weapon
		writeC(1); // name above char 1=true ... ??
		writeC(_activeChar.isRunning() ? 1 : 0);
		writeC(_activeChar.isInCombat() ? 1 : 0);
		writeC(_activeChar.isAlikeDead() ? 1 : 0);
		writeC(_isSummoned ? 2 : 0); // invisible ?? 0=false  1=true   2=summoned (only works if model has a summon animation)
		writeS(_name);
		writeS(_title);
		writeD(0);
		writeD(0);
		writeD(0000); // hmm karma ??

		writeD(_activeChar.getAbnormalEffect()); // C2
		writeD(0000); // C2
		writeD(0000); // C2
		writeD(0000); // C2
		writeD(0000); // C2
		writeC(0000); // C2

		writeC(_activeChar.getTeam()); // C3  team circle 1-blue, 2-red
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(0x00); // C4
		writeD(0x00); // C6
	}

	@Override
	public boolean canBroadcast(L2PcInstance activeChar)
	{
		if (_activeChar instanceof L2Summon && ((L2Summon) _activeChar).getOwner() == activeChar)
			return false;

		if (activeChar==null || !activeChar.canSee(_activeChar, false))
			return false;

		return true;
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__22_NPCINFO;
	}
}