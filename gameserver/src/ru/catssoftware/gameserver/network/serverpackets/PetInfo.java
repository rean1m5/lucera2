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

import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

/**
 * This class ...
 *
 * @version $Revision: 1.6.2.5.2.12 $ $Date: 2005/03/31 09:19:16 $
 */
public class PetInfo extends L2GameServerPacket
{
	private static final String	_S__CA_PETINFO	= "[S] b1 PetInfo";
	private L2Summon			_summon;
	private int					_x, _y, _z, _heading;
	private boolean				_isSummoned;
	private int					_val;
	private int					_mAtkSpd, _pAtkSpd;
	private int					_runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private int					_maxHp, _maxMp;
	private int					_maxFed, _curFed;
	private float				_multiplier;

	/**
	 * rev 478  dddddddddddddddddddffffdddcccccSSdddddddddddddddddddddddddddhc
	 * @param _characters
	 */
	public PetInfo(L2Summon summon, int val)
	{
		summon.updateEffectIcons();

		_summon = summon;
		_isSummoned = _summon.isShowSummonAnimation();
		_x = _summon.getX();
		_y = _summon.getY();
		_z = _summon.getZ();
		_heading = _summon.getHeading();
		_mAtkSpd = _summon.getMAtkSpd();
		_pAtkSpd = _summon.getPAtkSpd();
		_multiplier = _summon.getStat().getMovementSpeedMultiplier();
		_runSpd = _summon.getPetSpeed();
		_walkSpd =  _summon.isMountable() ? 45 : 30;
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
		_maxHp = _summon.getMaxHp();
		_maxMp = _summon.getMaxMp();
		_val = val;
		if (_summon instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) _summon;
			_curFed = pet.getCurrentFed(); // how fed it is
			_maxFed = pet.getMaxFed(); //max fed it can be
		}
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0xb1);
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().getIdTemplate() + 1000000);
		writeD(0x00);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0x00);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);
		writeD(_swimWalkSpd);
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);

		writeF(_multiplier); // movement multiplier
		writeF(1); // attack speed multiplier
		writeF(_summon.getTemplate().getCollisionRadius());
		writeF(_summon.getTemplate().getCollisionHeight());
		writeD(_summon.getWeapon()); // right hand weapon
		writeD(_summon.getArmor()); // body armor
		writeD(0x00); // left hand weapon
		writeC(_summon.getOwner() != null ? 1 : 0);    // when pet is dead and player exit game, pet doesn't show master name
		writeC(1); // running=1 (it is always 1, walking mode is calculated from multiplier)
		writeC(_summon.isInCombat() ? 1 : 0); // attacking 1=true
		writeC(_summon.isAlikeDead() ? 1 : 0); // dead 1=true
		writeC(_isSummoned ? 2 : _val); //  0=teleported  1=default   2=summoned
		writeS(_summon.getName()); // summon name
		writeS(_summon.getTitle()); // owner name
		writeD(0x01);
		writeD(_summon.getOwner() != null ? _summon.getOwner().getPvpFlag() : 0); //0 = white,2= purpleblink, if its greater then karma = purple 
		writeD(_summon.getOwner() != null ? _summon.getOwner().getKarma() : 0); // karma
		writeD(_curFed); // how fed it is
		writeD(_maxFed); //max fed it can be
		writeD((int) _summon.getStatus().getCurrentHp());//current hp
		writeD(_maxHp);// max hp
		writeD((int) _summon.getStatus().getCurrentMp());//current mp
		writeD(_maxMp);//max mp
		writeD(_summon.getStat().getSp()); //sp
		writeD(_summon.getLevel());// lvl
		writeQ(_summon.getStat().getExp());
		if (_summon.getExpForThisLevel() > _summon.getStat().getExp())
			writeQ(_summon.getStat().getExp());// 0%  absolute value
		else
			writeQ(_summon.getExpForThisLevel());// 0%  absolute value
		writeQ(_summon.getExpForNextLevel());// 100% absoulte value
		writeD(_summon instanceof L2PetInstance ? _summon.getInventory().getTotalWeight() : 0);//weight
		writeD(_summon.getMaxLoad());//max weight it can carry
		writeD(_summon.getPAtk(null));//patk
		writeD(_summon.getPDef(null));//pdef
		writeD(_summon.getMAtk(null, null));//matk
		writeD(_summon.getMDef(null, null));//mdef
		writeD(_summon.getAccuracy());//accuracy
		writeD(_summon.getEvasionRate(null));//evasion
		writeD(_summon.getCriticalHit(null, null));//critical
		writeD((int) _summon.getStat().getMoveSpeed());//speed
		writeD(_summon.getPAtkSpd());//atkspeeds
		writeD(_summon.getMAtkSpd());//casting speed

		writeD(_summon.getAbnormalEffect());//c2  abnormal visual effect... bleed=1; poison=2; poison & bleed=3; flame=4;

		writeH(_summon.isMountable() ? 1 : 0);//c2    ride button

		writeC(0); // c2

		// Following all added in C4.
		writeH(0); // ??
		writeC(_summon.getOwner() != null ? _summon.getOwner().getTeam() : 0); // team aura (1 = blue, 2 = red)
		writeD(_summon.getSoulShotsPerHit()); // How many soulshots this servitor uses per hit
		writeD(_summon.getSpiritShotsPerHit()); // How many spiritshots this servitor uses per hit
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__CA_PETINFO;
	}
}