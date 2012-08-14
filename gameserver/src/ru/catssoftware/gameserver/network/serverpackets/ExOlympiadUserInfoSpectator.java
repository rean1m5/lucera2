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
// TODO: Переделать/Убрать
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 *
 * @author godson
 */
public class ExOlympiadUserInfoSpectator extends L2GameServerPacket
{
	// chcdSddddd
	private static final String	_S__FE_29_OLYMPIADUSERINFOSPECTATOR	= "[S] FE:29 OlympiadUserInfoSpectator";
	private int					_side;
	private L2PcInstance		_player;

	/**
	 * @param _player
	 * @param _side (1 = right, 2 = left)
	 */
	public ExOlympiadUserInfoSpectator(L2PcInstance player)
	{
		this(player, player.getOlympiadSide());
	}

	public ExOlympiadUserInfoSpectator(L2PcInstance player, int side)
	{
		_player = player;
		_side = side;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x7a);
		writeC(_side);
		writeD(_player.getObjectId());
		writeS(_player.getName());
		writeD(_player.getClassId().getId());
		writeD((int) _player.getStatus().getCurrentHp());
		writeD(_player.getMaxHp());
		writeD((int) _player.getStatus().getCurrentCp());
		writeD(_player.getMaxCp());
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_29_OLYMPIADUSERINFOSPECTATOR;
	}
}