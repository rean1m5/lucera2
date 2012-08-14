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
package ru.catssoftware.gsregistering;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;

import org.apache.log4j.xml.DOMConfigurator;

import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.loginserver.manager.GameServerManager;
import ru.catssoftware.loginserver.model.GameServerInfo;
import ru.catssoftware.tools.util.HexUtil;


public class GameServerRegister
{
	private String						_choice;
	private static GameServerManager	gsServerManager;
	private boolean						_choiceOk;

	public static void main(String[] args) throws Throwable
	{
		// o Load config
		// -------------
		DOMConfigurator.configure("./config/log4j.xml");
		Config.load();
		L2DatabaseFactory.getInstance();
		gsServerManager = GameServerManager.getInstance();

		GameServerRegister gsRegister = new GameServerRegister();

		gsRegister.displayMenu();
	}

	/**
	* Display menu and return true or false if we should continue
	* @return true or false if user want to stop
	* @throws IOException
	* @throws
	* @throws IOException
	*/
	private void displayMenu() throws IOException
	{
		//L2EMU_EDIT
		System.out.println("Welcome to L2CatsSoftware GameServer Registering");
		//L2EMU_EDIT
		System.out.println("Enter The id of the server you want to register");
		System.out.println("Type 'help' to get a list of ids.");
		System.out.println("Type 'clean' to unregister all currently registered gameservers on this LoginServer.");
		LineNumberReader _in = new LineNumberReader(new InputStreamReader(System.in));

		// o ask id of server until id is a valid one
		// ------------------------------------------
		while (!_choiceOk)
		{
			System.out.println("Your choice:");
			_choice = _in.readLine();

			// o ask the list of server
			// -----------------------
			if (_choice.equalsIgnoreCase("help"))
			{
				displayServer();
			}
			// o clean all servers
			// -------------------
			else if (_choice.equalsIgnoreCase("clean"))
			{
				System.out.print("This is going to UNREGISTER ALL servers from this LoginServer. Are you sure? (y/n) ");
				_choice = _in.readLine();
				if (_choice.equals("y"))
				{
					GameServerRegister.cleanRegisteredGameServersFromDB();
					gsServerManager.getRegisteredGameServers().clear();
				}
				else
				{
					System.out.println("ABORTED");
				}
			}
			else
			{
				// o register server
				// ----------------
				registerServer();
			}
		}
	}

	/**
	 * register server.
	 * Check that id is not to high, > 0 and free.
	 * Create gameserver in database referential and hexid file.
	 */
	private void registerServer()
	{
		try
		{
			int id = new Integer(_choice).intValue();
			String name = gsServerManager.getServerNameById(id);
			if (name == null)
			{
				System.out.println("No name for id: " + id);
				return;
			}
			if (id < 0)
			{
				System.out.println("ID must be positive number");
			}
			else
			{
				if (!gsServerManager.hasRegisteredGameServerOnId(id))
				{
					byte[] hex = HexUtil.generateHex(16);

					gsServerManager.registerServerOnDB(hex, id, "");
					HexUtil.saveHexid(id, new BigInteger(hex).toString(16), "hexid(server " + id + ").txt");
					System.out.println("Server Registered hexid saved to 'hexid(server " + id + ").txt'");
					//L2EMU_EDIT
					System.out.println("Put this file in the config/network folder of your gameserver and rename it to 'hexid.txt'");
					//L2EMU_EDIT
					_choiceOk = true;
				}
				else
				{
					System.out.println("This id is not free");
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("Please, type a number or 'help'");
		}
	}

	/**
	 *
	 */
	private void displayServer()
	{
		for (GameServerInfo gs : gsServerManager.getRegisterdServers())
		{
			System.out.println("Server: id:" + gs.getId() + " - " + gsServerManager.getServerNameById(gs.getId()));
		}
		System.out.println("You can also see servername.xml");
	}

	public static void cleanRegisteredGameServersFromDB()
	{
		GameServerManager.getInstance().deleteAllServer();
	}
}