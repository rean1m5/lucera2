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

import ru.catssoftware.gameserver.model.L2Character;

/**
 * @author -Wooden-
 */
public class ExFishingHpRegen extends L2GameServerPacket
{
	private static final String	_S__FE_28_EXFISHINGHPREGEN	= "[S] FE:28 ExFishingHPRegen [dddcccdc]";
	private L2Character			_activeChar;
	private int					_time, _fishHp, _hpMode, _anim, _goodUse, _penalty, _hpBarColor;

	public ExFishingHpRegen(L2Character character, int time, int fishHp, int HPmode, int GoodUse, int anim, int penalty, int hpBarColor)
	{
		_activeChar = character;
		_time = time;
		_fishHp = fishHp;
		_hpMode = HPmode;
		_goodUse = GoodUse;
		_anim = anim;
		_penalty = penalty;
		_hpBarColor = hpBarColor;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x16);

		writeD(_activeChar.getObjectId());
		writeD(_time);
		writeD(_fishHp);
		writeC(_hpMode); // 0 = HP stop, 1 = HP raise
		writeC(_goodUse); // 0 = none, 1 = success, 2 = failed
		writeC(_anim); // Anim: 0 = none, 1 = reeling, 2 = pumping
		writeD(_penalty); // Penalty
		writeC(_hpBarColor); // 0 = normal hp bar, 1 = purple hp bar
	}

	@Override
	public String getType()
	{
		return _S__FE_28_EXFISHINGHPREGEN;
	}
}