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

import java.io.IOException;
import java.util.List;

import javolution.util.FastList;

import ru.catssoftware.Config;
import ru.catssoftware.loginserver.ClientManager;
import ru.catssoftware.loginserver.L2LoginClient;
import ru.catssoftware.loginserver.L2LoginServer;
import ru.catssoftware.loginserver.model.SessionKey;
import ru.catssoftware.loginserver.network.loginserverpackets.ClientConnected;


public final class PlayOk extends L2LoginServerPacket
{
	private int	_playOk1, _playOk2;

	private static List<String> _ip = new FastList<String>();
	public PlayOk(SessionKey sessionKey)
	{
		_playOk1 = sessionKey.playOkID1;
		_playOk2 = sessionKey.playOkID2;
	}

	/**
	 * @see com.l2jserver.mmocore.network.SendablePacket#write()
	 */
	@Override
	protected void write(L2LoginClient client)
	{
		
		if(Config.DDOS_PROTECTION_ENABLED) {
			ClientManager.getInstance().removeClient(client);
			if(Config.ON_SUCCESS_LOGIN_COMMAND.length()!=0) 
				    if(!_ip.contains(client.getIp())) try {
				    	Runtime.getRuntime().exec(Config.ON_SUCCESS_LOGIN_COMMAND.replace("%ip%", client.getIp()));
				    	_ip.add(client.getIp());
				    } catch(IOException e) {}
		}
		if(L2LoginServer.getInstance().getGameServerListener().getGameServer(client.getLastServerId()).getGameServerInfo().isDDoSEnabled())
			L2LoginServer.getInstance().getGameServerListener().getGameServer(client.getLastServerId()).sendPacket(new ClientConnected(client));
		writeC(0x07);
		writeD(_playOk1);
		writeD(_playOk2);
	}
	
}