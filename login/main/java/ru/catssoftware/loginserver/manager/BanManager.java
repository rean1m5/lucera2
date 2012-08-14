/*
 * $HeadURL: $
 *
 * $Author: $
 * $Date: $
 * $Revision: $
 *
 *
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
package ru.catssoftware.loginserver.manager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;



/**
 * This class manage ban list
 *
 * Ban list is stored in a file BAN_LIST before the startup. The BanManager load this file on startup and store for each ban
 * the ip and the expiration time (the date when this ban is finished). If the expiration is 0, the ban is eternal.
 *
 */
public class BanManager
{
	private static BanManager		_instance	= null;
	private static final Logger 	_log		= Logger.getLogger(BanManager.class);
	/** Banned ips */
	private Map<String, Long>	_bannedIps	= new FastMap<String, Long>().setShared(true);

	public static String			BAN_LIST	= "config/banned_ip.cfg";
	private static final String		ENCODING	= "UTF-8";

	/**
	 * return singleton for banmanager
	 * @return BanManager instance
	 */
	public static BanManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new BanManager();
			return _instance;
		}
		else
			return _instance;
	}

	private BanManager()
	{
		load();
	}

	/**
	 * Load banned list
	 */
	public void load()
	{
		try
		{
			_bannedIps.clear();
			// try to read banned list
			File file = new File(BAN_LIST);
			List<?> lines = FileUtils.readLines(file, ENCODING);

			for (int i = 0; i < lines.size(); i++)
			{
				String line = (String) lines.get(i);
				line = line.trim();
				if (line.length() > 0 && !line.startsWith("#"))
					addBannedIP(line);
			}
			_log.info("BanManager: Loaded " + getNbOfBannedIp() + " banned ip/subnet(s).");
		}
		catch (IOException e)
		{
			_log.warn("error while reading banned file:" + e);
		}
	}

	/**
	 * Store a ban ip in memory.
	 * Read a line, ignore comment and split it to get the ip and the expiration
	 * If no expiration was found, this is a eternal ban
	 * @param line
	 */
	private void addBannedIP(String line)
	{
		String[] parts;
		// split comments if any
		parts = line.split("#");

		// discard comments in the line, if any
		line = parts[0];

		parts = line.split(" ");

		String address = parts[0];

		long duration = 0;

		if (parts.length > 1)
		{
			try
			{
				duration = Long.parseLong(parts[1]);
			}
			catch (NumberFormatException e)
			{
				_log.warn("Skipped: Incorrect ban duration (" + parts[1] + ") on Line: " + line);
				return;
			}
		}

		{
			try
			{
				addBanForAddress(address, duration);
			}
			catch (UnknownHostException e)
			{
				_log.warn("Skipped: Invalid address (" + parts[0] + ") on Line: " + line);
			}
		}
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 *
	 * @param address The Address to be banned.
	 * @param expiration Timestamp in miliseconds when this ban expires
	 * @throws UnknownHostException if the address is invalid.
	 */
	public void addBanForAddress(String address, long expiration) throws UnknownHostException
	{
		_bannedIps.put(address,  expiration);
	}

	/**
	 * Adds the network to the ban list of the login server, with the given duration.
	 *
	 * @param net The Network to be banned.
	 * @param duration is miliseconds
	 */

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 *
	 * @param address The Address to be banned.
	 * @param duration is miliseconds
	 */
	public void addBanForAddress(InetAddress address, long duration)
	{
		_bannedIps.put(address.getHostAddress(), System.currentTimeMillis() + duration);
	}

	/**
	 * Check if an Ip is banned
	 *
	 * @param address
	 * @return true if ip is banned or false otherwise
	 */
	public boolean isBannedAddress(InetAddress address)
	{

		if(_bannedIps.containsKey(address.getHostAddress()) && _bannedIps.get(address.getHostAddress()) > System.currentTimeMillis())
				return true;
		if(_bannedIps.containsKey(address.getHostAddress()))
				_bannedIps.remove(address.getHostAddress());
		return false;
	}

	/**
	 * get all banned ips
	 * @return a map of banned ip
	 */

	/**
	 * Remove the specified address from the ban list
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip
	 */
	public boolean removeBanForAddress(InetAddress address)
	{
		return _bannedIps.remove(address.getHostAddress())!=null;
	}

	/**
	 * Remove the specified address from the ban list
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip or the address was invalid.
	 */
	public boolean removeBanForAddress(String address)
	{
		try
		{
			return this.removeBanForAddress(InetAddress.getByName(address));
		}
		catch (UnknownHostException e)
		{
			return false;
		}
	}

	/**
	 * @return number of ip banned
	 */
	public int getNbOfBannedIp()
	{
		return _bannedIps.size();
	}
}