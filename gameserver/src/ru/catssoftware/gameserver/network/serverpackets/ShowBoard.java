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

import java.io.UnsupportedEncodingException;
import java.util.List;

import ru.catssoftware.gameserver.cache.HTMParser;

public class ShowBoard extends L2GameServerPacket
{
	private static final String	_S__6E_SHOWBOARD	= "[S] 6e ShowBoard";

	private String				_htmlCode;
	private String				_id;
	private List<String>		_arg;

	public ShowBoard(String htmlCode, String id)
	{
		_id = id;
		_htmlCode = htmlCode;
		
	}

	public ShowBoard(List<String> arg)
	{
		_id = "1002";
		_htmlCode = null;
		_arg = arg;
	}

	private byte[] get1002()
	{
		int len = _id.getBytes().length * 2 + 2;
		for (String arg : _arg)
		{
			len += (arg.getBytes().length + 4) * 2;
		}
		byte data[] = new byte[len];
		int i = 0;
		for (int j = 0; j < _id.getBytes().length; j++, i += 2)
		{
			data[i] = _id.getBytes()[j];
			data[i + 1] = 0;
		}
		data[i++] = 8;
		data[i++] = 0;
		for (String arg : _arg)
		{
			for (int j = 0; j < arg.getBytes().length; j++, i += 2)
			{
				data[i] = arg.getBytes()[j];
				data[i + 1] = 0;
			}
			data[i++] = 0x20;
			data[i++] = 0x0;
			data[i++] = 0x8;
			data[i++] = 0x0;
		}
		return data;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6E);
		writeC(0x01); //c4 1 to show community 00 to hide
		writeS("bypass _bbshome"); // top
		writeS("bypass _bbsgetfav"); // favorite
		writeS("bypass _bbsloc"); // region
		writeS("bypass _bbsclan"); // clan
		writeS("bypass _bbsmemo"); // memo
		writeS("bypass _bbsmail"); // mail
		writeS("bypass _bbsfriends"); // friends
		writeS("bypass  bbs_add_fav"); // add fav.
		if(_htmlCode!=null)
			_htmlCode = HTMParser.parseHTM(_htmlCode, getClient().getActiveChar());
//		if(_id.equalsIgnoreCase("101"))
//			getClient().getActiveChar().cleanBypasses(true); 
//		_htmlCode = getClient().getActiveChar().encodeBypasses(_htmlCode, true);
		
		if (!_id.equals("1002"))
		{
			// getBytes is a very costly operation, and should only be called once
			byte[] htmlBytes = new byte[0];
			if (_htmlCode != null) {
				htmlBytes = _htmlCode.getBytes();
			}

			byte[] idBytes = _id.getBytes();
			byte data[] = new byte[2 + 2 + 2 + idBytes.length * 2 + 2 * ((_htmlCode != null) ? htmlBytes.length : 0)];
			int i = 0;
			for (int j = 0; j < idBytes.length; j++, i += 2)
			{
				data[i] = idBytes[j];
				data[i + 1] = 0;
			}
			data[i++] = 8;
			data[i++] = 0;

			byte[] html = new byte[0];
			if (_htmlCode != null)
			{
				try
				{
					html = _htmlCode.getBytes("UTF-16LE");
				}
				catch (UnsupportedEncodingException e)
				{
					html = new byte[_htmlCode.length() * 2];
					for (int j = 0; j < htmlBytes.length; i += 2, j++)
					{
						data[i] = htmlBytes[j];
						data[i + 1] = 0;
					}
				}
			}
			System.arraycopy(html, 0, data, i, html.length);
			i += html.length;

			data[i++] = 0;
			data[i] = 0;
			writeB(data);
		}
		else
		{
			writeB(get1002());
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__6E_SHOWBOARD;
	}
}