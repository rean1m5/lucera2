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

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;

public class CreatureSay extends L2GameServerPacket
{
	private static final String	_S__4A_CREATURESAY	= "[S] 4A CreatureSay [ddss]";
	private int					_objectId;
	private SystemChatChannelId	_channel;
	private String				_charName;
	private String				_text;

	/**
	 * @param _characters
	 */
	public CreatureSay(int objectId, SystemChatChannelId channel, String charName, String text)
	{
		_objectId = objectId;
		_channel = channel;
		_charName = charName;
		_text = text;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4a);

		writeD(_objectId);
		writeD(_channel.getId());
		writeS(_charName);
		writeS(_text);
		L2PcInstance _pci = getClient().getActiveChar();
		if(_pci != null)
			_pci.broadcastSnoop(_channel.getId(), _charName, _text);
		
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__4A_CREATURESAY;
	}
}