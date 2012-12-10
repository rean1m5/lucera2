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
package ru.catssoftware.loginserver.network.gameserverpackets;

import ru.catssoftware.loginserver.clientpackets.ClientBasePacket;

import java.util.logging.Logger;


/**
 * Format: cccddb
 * c desired ID
 * c accept alternative ID
 * c reserve Host
 * s ExternalHostName
 * s InetranlHostName
 * d max players
 * d hexid size
 * b hexid
 *
 * @author -Wooden-
 */
public abstract class GameServerAuthAbstract extends ClientBasePacket
{
	protected static Logger _log = Logger.getLogger(GameServerAuthAbstract.class.getName());
	protected byte[] _hexId;
	protected int _desiredId;
	protected boolean _hostReserved;
	protected boolean _acceptAlternativeId;
	protected int _maxPlayers;
	protected int _port;
	protected String _gsNetConfig1;
	protected String _gsNetConfig2;
	protected String _key = null;

	/**
	 * @param decrypt
	 */
	public GameServerAuthAbstract(byte[] decrypt)
	{
		super(decrypt);
	}

	/**
	 * @return
	 */
	public byte[] getHexID()
	{
		return _hexId;
	}

	public boolean getHostReserved()
	{
		return _hostReserved;
	}

	public int getDesiredID()
	{
		return _desiredId;
	}

	public boolean acceptAlternateID()
	{
		return _acceptAlternativeId;
	}

	/**
	 * @return Returns the max players.
	 */
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}

	/**
	 * @return Returns the gameserver netconfig string.
	 */
	public String getNetConfig()
	{
		String _netConfig = "";

		//	network configuration string formed on server
		if (_gsNetConfig1.contains(";") || _gsNetConfig1.contains(","))
		{
			_netConfig = _gsNetConfig1;
		}
		else
		// make network config string
		{
			if (_gsNetConfig2.length() > 0) // internal hostname and default internal networks
			{
				_netConfig = _gsNetConfig2 + "," + "10.0.0.0/8,192.168.0.0/16" + ";";
			}
			if (_gsNetConfig1.length() > 0) // external hostname and all avaible addresses by default
			{
				_netConfig += _gsNetConfig1 + "," + "0.0.0.0/0" + ";";
			}
		}

		return _netConfig;
	}

	/**
	 * @return Returns the port.
	 */
	public int getPort()
	{
		return _port;
	}

	public boolean isPW()
	{
		return _key != null;
	}
}
