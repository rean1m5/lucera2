/* This program is free software; you can redistribute it and/or modify
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
package ru.catssoftware.loginserver.thread;

import java.net.Socket;
import java.util.List;

import ru.catssoftware.Config;
import javolution.util.FastList;

/**
 * @author  KenM
 */
public class GameServerListener extends FloodProtectedListener
{
	private static List<GameServerThread>	_gameServers	= new FastList<GameServerThread>();

	public GameServerListener()
	{
		super(Config.LOGIN_HOSTNAME, Config.LOGIN_PORT);
	}

	@Override
	public void addClient(Socket s)
	{
		GameServerThread gst = new GameServerThread(s);
		_gameServers.add(gst);
	}

	public GameServerThread getGameServer(int id) {
		for(GameServerThread gst : _gameServers) {
			if(gst.getGameServerInfo().getId()== id) {
				return gst;
			}
		}
		return null;
	}
	public void removeGameServer(GameServerThread gst)
	{
		_gameServers.remove(gst);
	}
}