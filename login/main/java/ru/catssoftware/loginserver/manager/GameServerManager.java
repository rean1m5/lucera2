/*
 * $Header: GameServerListener.java, 14-Jul-2005 03:26:20 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 14-Jul-2005 03:26:20 $
 * $Revision: 1 $
 * $Log: GameServerListener.java,v $
 * Revision 1  14-Jul-2005 03:26:20  luisantonioa
 * Added copyright notice
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
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastMap;


import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.loginserver.model.GameServerInfo;
import ru.catssoftware.tools.random.Rnd;

/**
 * Manager servers
 * Servers come from server.xml file and database.
 * For each server in database, an instance of Gameserver is launch. The manager controls each gameserver threads.
 */
public class GameServerManager
{
	private static final Logger 			_log			= Logger.getLogger(GameServerManager.class);
	private static GameServerManager		__instance		= null;

	// Game Server from database
	private Map<Integer, GameServerInfo>	_gameServers	= new FastMap<Integer, GameServerInfo>().setShared(true);
	private Map<Integer,String>				_serverNames  = new FastMap<Integer, String>();
	// RSA Config
	private static final int				KEYS_SIZE		= 10;
	private KeyPair[]						_keyPairs;


	/**
	 * Return singleton
	 * exit the program if we didn't succeed to load the instance
	 * @return  GameServerManager
	 */
	public static GameServerManager getInstance()
	{
		if (__instance == null)
		{
			try
			{
				__instance = new GameServerManager();
			}
			catch (NoSuchAlgorithmException e)
			{
				_log.fatal("FATAL: Failed loading GameServerManager. Reason: " + e.getMessage(), e);
				System.exit(1);
			}
			catch (InvalidAlgorithmParameterException e)
			{
				_log.fatal("FATAL: Failed loading GameServerManager. Reason: " + e.getMessage(), e);
				System.exit(1);
			}
		}
		return __instance;
	}


	private void loadRSAKeys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4);
		keyGen.initialize(spec);

		_keyPairs = new KeyPair[KEYS_SIZE];
		for (int i = 0; i < KEYS_SIZE; i++)
			_keyPairs[i] = keyGen.genKeyPair();

		_log.info("Cached " + _keyPairs.length + " RSA keys for Game Server communication.");
	}
	/**
	 * Initialize keypairs
	 * Initialize servers list from xml and db
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 */
	private GameServerManager() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		load();
		loadRSAKeys();
	}


	/**
	 * Load RSA keys
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 */

	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return _gameServers;
	}

	public GameServerInfo getRegisteredGameServerById(int id)
	{
		return _gameServers.get(id);
	}

	public boolean hasRegisteredGameServerOnId(int id)
	{
		return _gameServers.containsKey(id);
	}

	public boolean registerWithFirstAvailableId(GameServerInfo gsi)
	{
		// avoid two servers registering with the same "free" id
		return false;
	}

	public boolean register(int id, GameServerInfo gsi)
	{
		// avoid two servers registering with the same id
		synchronized (_gameServers)
		{
			if (!_gameServers.containsKey(id))
			{
				_gameServers.put(id, gsi);
				gsi.setId(id);
				return true;
			}
		}
		return false;
	}
	
	public Collection<GameServerInfo> getRegisterdServers() {
		return _gameServers.values();
	}
	public void registerServerOnDB(GameServerInfo gsi)
	{
		this.registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getIp());
	}

	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		try {
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			GameServerInfo gs = new GameServerInfo(id, hexId);
			gs.store(con);
			gs.setIp(externalHost);
			_gameServers.put(gs.getId(), gs);
		} catch(SQLException e) {
			_log.error("GameServerManager: Unable to store gameserver",e );
		}
	}

	public String getServerNameById(int id)
	{
		return _serverNames.get(id);
	}

	/**
	 * Load Gameserver from DAO
	 * For each gameserver, instantiate a GameServer, (a container that hold a thread)
	 */
	private void load()
	{
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			try {
				Document doc = factory.newDocumentBuilder().parse(new File("./config/servername.xml"));
				Node names =  doc.getFirstChild();
				if(names!=null)
					for(Node n = names.getFirstChild(); n!=null; n =n.getNextSibling()) {
						if(n.getNodeName().equals("server")) {
							NamedNodeMap attr = n.getAttributes();
							_serverNames.put(Integer.parseInt(attr.getNamedItem("id").getNodeValue()), attr.getNamedItem("name").getNodeValue());
						}
					}
				_log.info("GameServerManager: Loaded "+_serverNames.size()+" server name(s)");
			} catch(Exception e) {
				_log.warn("GameServerManager: Unable to load  servernames.xml",e);
			}
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("select * from gameservers");
			ResultSet rs = stm.executeQuery();
			while(rs.next()) {
				GameServerInfo gs = new GameServerInfo(rs);
				_gameServers.put(gs.getId(), gs);
			}
			rs.close();
			stm.close();
			con.close();
		} catch(SQLException e) {
			_log.error("GameServerManager: Unable to load gameserver lsit",e);
			System.exit(1);
		}
		_log.info("GameServerManager: Loaded " + _gameServers.size()+" server(s)");
	}

	/**
	*
	* @param id - the server id
	*/
	public void deleteServer(int id)
	{
		try {
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm =  con.prepareStatement("delete from gameservers where server_id=?");
			stm.setInt(1, id);
			stm.execute();
			stm.close();
			con.close();
			_gameServers.remove(id);
		} catch(SQLException e) {
			_log.error("GameServerManager: Unable to delete gameserver.",e);
		}
		
		
	}

	public void deleteAllServer()
	{
		for(Integer serverId: _gameServers.keySet())
			deleteServer(serverId);
			
	}

	public KeyPair getKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}
	
}