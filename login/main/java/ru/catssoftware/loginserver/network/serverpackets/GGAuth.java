/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.catssoftware.loginserver.network.serverpackets;

import org.apache.log4j.Logger;


import ru.catssoftware.loginserver.L2LoginClient;


/**
 * Fromat: d
 * d: response
 */
public final class GGAuth extends L2LoginServerPacket
{
	private static final Logger _log					= Logger.getLogger(GGAuth.class.getName());
	public static final int		SKIP_GG_AUTH_REQUEST	= 0x0b;

	private int					_response;

	public GGAuth(int response)
	{
		_response = response;
		if (_log.isDebugEnabled())
			_log.warn("Reason Hex: " + (Integer.toHexString(response)));
	}

	/**
	 * @see com.l2jserver.mmocore.network.SendablePacket#write()
	 */
	@Override
	protected void write(L2LoginClient client)
	{
		writeC(0x0b);
		writeD(_response);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
	}
}