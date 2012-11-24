package ru.catssoftware.gameserver.network;

import javolution.text.TextBuilder;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.LoginServerThread;
import ru.catssoftware.gameserver.LoginServerThread.SessionKey;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.CharNameTable;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.handler.KeyProtection;
import ru.catssoftware.gameserver.mmocore.MMOClient;
import ru.catssoftware.gameserver.mmocore.MMOConnection;
import ru.catssoftware.gameserver.mmocore.ReceivablePacket;
import ru.catssoftware.gameserver.model.CharSelectInfoPackage;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.LeaveWorld;
import ru.catssoftware.gameserver.network.serverpackets.ServerClose;
import ru.catssoftware.gameserver.threadmanager.FIFOPacketRunnableQueue;
import ru.catssoftware.gameserver.util.DatabaseUtils;
import ru.catssoftware.protection.CatsGuard;
import ru.catssoftware.protection.LameStub;
import ru.catssoftware.tools.security.BlowFishKeygen;
import ru.catssoftware.tools.security.GameCrypt;
import ru.catssoftware.util.StatsSet;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>>
{
	private static final Logger	_log	= Logger.getLogger(L2GameClient.class.getName());

	public static enum GameClientState
	{
		CONNECTED,
		AUTHED,
		IN_GAME
	}
	public static interface IExReader {
		public int read(ByteBuffer buf);
		public void checkChar(L2PcInstance cha);
	}

	public GameClientState				_state						= GameClientState.CONNECTED;
	private String						_hostAddress				= getSocket().getInetAddress().getHostAddress();
	private int							_unknownPackets				= 0;
	private String						_hwid						= null;
	private String						_accountName				= null;
	private SessionKey					_sessionId;
	private L2PcInstance				_activeChar;
	private int[]						_charSlotMapping;
	private GameCrypt					_crypt;
	private boolean						_disconnected;
	private boolean						_protocol;
	private long 						_protocolVer;
	public  IExReader					_reader;
	private StatsSet					_accountData 			   = new StatsSet();

	private List<Integer> acceptAction = new ArrayList<Integer>();
	
	private static final String	LOAD_ACC_DATA			        = "SELECT valueName,valueData from account_data where account_name=?";
	private static final String	STORE_ACC_DATA				    = "REPLACE account_data (account_name, valueName,valueData) VALUES(?,?,?)";
	public static final String PLAYER_LANG = "lang";
	private int _bufferError;
	
	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		super(con);
	}

	private GameCrypt getCrypt()
	{
		if (_crypt == null)
			_crypt = new GameCrypt();
		return _crypt;
	}

	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
		getCrypt().setKey(key);
		return key;
	}

	public GameClientState getState()
	{
		return _state;
	}

	public void setState(GameClientState pState)
	{
		_state = pState;
		
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		getCrypt().decrypt(buf.array(), buf.position(), size);
		return true;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		getCrypt().encrypt(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
		return true;
	}

	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}

	private Integer[] charsOnAccount;

	public boolean haveCharOnAccount(int charId)
	{
		if (charsOnAccount == null)
				setCharOnAccount();

		for(int i: charsOnAccount)
			if (i == charId)
				return true;

		return false;
	}

	public void setCharOnAccount()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		int count = CharNameTable.getInstance().accountCharNumber(getAccountName());
		charsOnAccount = new Integer[count];
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			statement = con.prepareStatement("SELECT `charId` FROM characters WHERE account_name=?");
			statement.setString(1, getAccountName());
			rset = statement.executeQuery();

			while (rset.next())
				charsOnAccount[--count] = rset.getInt("charId");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.close(con,  statement, rset);
		}
	}

	private boolean checkHwId() {
		if(getActiveChar()!=null && getHWid()!=null && getHWid().length()!=0) try {
			String hw = getActiveChar().getAccountData().getString("hwbind"); 
			if(hw.length()>0) {
				if(!isSameHWID(hw, getHWid())) {
					new Disconnection(this).deleteMe().defaultSequence(false);
					return false;
				}
				
			}
		} catch(IllegalArgumentException e) {
			
		}
		return true;
	} 
	public void setActiveChar(L2PcInstance pActiveChar)
	{
		_activeChar = pActiveChar;
		if (_activeChar != null)
		{
			if(_reader!=null && _activeChar!=null) 
				_reader.checkChar(_activeChar);
			if(!checkHwId())
				return;
		}
		L2World.getInstance().storeObject(_activeChar);
	}

	public void setAccountName(String pAccountName)
	{
		_accountName = pAccountName;
		_accountData.clear();
		try {
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement(LOAD_ACC_DATA);
			if(getAccountName()!=null && getAccountName().length()>0) {
				stm.setString(1,getAccountName());
				ResultSet rs = stm.executeQuery();
				while(rs.next()) 
					_accountData.set(rs.getString(1), rs.getString(2));
				rs.close();
				stm.close();
			}
			con.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		if(_reader==null)
			CatsGuard.getInstance().initSession(this);
		if(GameExtensionManager.getInstance().handleAction(this, Action.ACC_SETNAME)!=null) 
			close(ServerClose.STATIC_PACKET);
	}

	public void storeData() {
		try {
			if(_accountName==null)
				return;
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(STORE_ACC_DATA);
			statement.setString(1,_accountName);
			for(String s : _accountData.getSet().keySet()) {
				statement.setString(2,s);
				statement.setString(3,_accountData.getString(s));
				statement.execute();
			}
			statement.close();
			con.close();
		} catch(SQLException e) {
			_log.error("Error while saving account data",e);
		}
		
	}
	public StatsSet getAccountData() {
		return _accountData;
	}
	
	public String getAccountName()
	{
		return _accountName;
	}

	public void setSessionId(SessionKey sk)
	{
		_sessionId = sk;
	}

	public SessionKey getSessionId()
	{
		return _sessionId;
	}

	/**
	 * Паблик метод. Служит для удаления персонажа
	 * 
	 * Возвращает byte:
	 *             - 1: Ошибка, чар не найден в указаном слоте
	 *               0: Чар может быть удален, т.к. не находится в клане
	 *               1: Чар не может быть удален, т.к. находится в клане
	 *               2: Чар не может быть удален, т.к. является клан лидером
	 */
	public byte markToDeleteChar(int charslot)
	{
		int objid = getObjectIdForSlot(charslot);
		
		if (objid < 0)
			return -1;

		byte result = -1;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT clanId from characters WHERE charId=?");
			statement.setInt(1, objid);
			ResultSet rs = statement.executeQuery();
			byte answer = -1;
			if (rs.next())
			{
				int clanId = rs.getInt(1);
				if (clanId != 0)
				{
					L2Clan clan = ClanTable.getInstance().getClan(clanId);
					
					if (clan == null)
						answer = 0; // jeezes!
					else if (clan.getLeaderId() == objid)
						answer = 2;
					else 
						answer = 1;
				}
				else
					answer = 0;

				if (answer == 0)
				{
					if (Config.DELETE_DAYS < 1)
						deleteCharByObjId(objid);
					else
					{
						statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?");
						statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS * 86400000L);
						statement.setInt(2, objid);
						statement.execute();
						statement.close();
					}
				}
			}
			result = answer;
		}
		catch (Exception e)
		{
			_log.error("Error updating delete time of character.", e);
			result = -1;
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
		return result;
	}

	public void markRestoredChar(int charslot) throws Exception
	{
		int objid = getObjectIdForSlot(charslot);
		if (objid < 0)
			return;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error restoring character.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}

	public static void deleteCharByObjId(int objid)
	{
		if (objid < 0)
			return;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;

			statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_skills WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_raid_points WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_recommends WHERE charId=? OR target_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM seven_signs WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM characters WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error deleting character.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}

	public L2PcInstance loadCharFromDisk(int charslot)
	{
		L2PcInstance character = L2PcInstance.load(getObjectIdForSlot(charslot));

		if (character != null)
		{
			character.setRunning();
			character.standUp();
			character.refreshOverloaded();
			character.refreshExpertisePenalty();
			character.setOnlineStatus(true);
		}
		else
			_log.fatal("could not restore in slot: " + charslot);

		return character;
	}

	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping = new int[chars.length];

		int i = 0;
		for (CharSelectInfoPackage element : chars)
			_charSlotMapping[i++] = element.getObjectId();
	}

	private int getObjectIdForSlot(int charslot)
	{
		if (_charSlotMapping == null || charslot < 0 || charslot >= _charSlotMapping.length)
		{
			_log.warn(toString() + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}
		return _charSlotMapping[charslot];
	}

	@Override
	public String toString()
	{
		TextBuilder tb = TextBuilder.newInstance();

		tb.append("[State: ").append(getState());

		String host = getHostAddress();
		if (host != null)
			tb.append(" | IP: ").append(String.format("%-15s", host));

		String account = getAccountName();
		if (account != null)
			tb.append(" | Account: ").append(String.format("%-15s", account));

		L2PcInstance player = getActiveChar();
		if (player != null)
			tb.append(" | Character: ").append(String.format("%-15s", player.getName()));

		tb.append("]");

		final String toString = tb.toString();

		TextBuilder.recycle(tb);

		return toString;
	}

	public boolean isProtocolOk()
	{
		return _protocol;
	}

	public void setProtocolOk(boolean b)
	{
		_protocol = b;
		
	}

	public void setProtocolVer(long ver)
	{
		_protocolVer = ver;
	}

	public long getProtocolVer()
	{
		return _protocolVer;
	}

	public String getHostAddress()
	{
		String host;
		try
		{
			if (_hostAddress == null || _hostAddress.isEmpty())
			{
				if (getSocket() != null && getSocket().getInetAddress() != null)
					host =  getSocket().getInetAddress().getHostAddress();
				else
					host = "";
			}
			else
				host = _hostAddress;
		}
		catch (Exception e)
		{
			host = "";
			if (Config.DEBUG)
				_log.info("Could't get client host address. Return empty. Error: " + e);
		}
		return host;
	}

	public void setHostAddress(String hostAddress)
	{
		_hostAddress = hostAddress;
	}

	public boolean isDisconnected()
	{
		return _disconnected;
	}

	public void setDisconnected()
	{
		_disconnected = true;
	}

	void execute(ReceivablePacket<L2GameClient> rp)
	{
		getPacketQueue().execute(rp);
	}

	private FIFOPacketRunnableQueue<ReceivablePacket<L2GameClient>> _packetQueue;

	private FIFOPacketRunnableQueue<ReceivablePacket<L2GameClient>> getPacketQueue()
	{
		if (_packetQueue == null)
			_packetQueue = new FIFOPacketRunnableQueue<ReceivablePacket<L2GameClient>>() {};

		return _packetQueue;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		if(gsp==null)
			return;
		try
		{
			if (isDisconnected())
				return;
			if (!gsp.canBroadcast(getActiveChar()))
				return;
			gsp.runImpl(this, getActiveChar());
			if(getState() == GameClientState.IN_GAME && gsp.isImportant())
				getActiveChar().addPacket(gsp.getClass().getSimpleName());
			getConnection().sendPacket(gsp);
			if(Config.SEND_PACKET_LOG)
				System.out.println("-> "+gsp.getClass().getSimpleName());
		}
		catch(Exception e)
		{
			_log.error("Error sending "+gsp.getClass().getSimpleName(),e);
		}
	}

	synchronized void close(boolean toLoginScreen)
	{
		getConnection().close(toLoginScreen ? ServerClose.STATIC_PACKET : LeaveWorld.STATIC_PACKET);
		setDisconnected();
	}

	public synchronized void close(L2GameServerPacket gsp)
	{
		getConnection().close(gsp);
		setDisconnected();
	}

	public synchronized void closeNow()
	{
		new Disconnection(this).defaultSequence(false);
	}

	@Override
	protected synchronized void onDisconnection()
	{
		if(_activeChar!=null)
			GameExtensionManager.getInstance().handleAction(_activeChar, Action.CHAR_LEAVEWORLD);
		storeData();
		ThreadPoolManager.getInstance().executeGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				LoginServerThread.getInstance().sendLogout(getAccountName());
			}
		});

		new Disconnection(this).onDisconnection();

		if(_reader!=null)
			CatsGuard.getInstance().doneSession(this);
//		Restrictions.onDisconnect(this);
	
		setDisconnected();
	}

	@Override
	protected synchronized void onForcedDisconnection()
	{
		if (Config.DEBUG || _log.isDebugEnabled())
			_log.info("Client " + toString() + " disconnected abnormally.");
	}

	/**
	 * Метод служит для повышения числа неизвестных пакетов
	 * Возвращает число неизвестных пакетов
	 * Действителен для 1 сессии
	 **/
	public int incUnknownPackets()
	{
		if (Config.DEBUG || _log.isDebugEnabled())
			_log.info("Server received unknown packets from IP: " + getHostAddress() + " [Account: " + getAccountName() + "]. UPC: " + _unknownPackets + ".");

		return _unknownPackets++;
	}

	/**
	 * Метод служит для заполнения поля _hwid
	 * _hwid - уникальный ID компьютера игрока
	 **/
	public void setHWID(String hwid)
	{
		_hwid = hwid;
	
	}

	/**
	 * Метод получения HWID адреса клиента
	 * Возвращает поле _hwid
	 **/
	public String getHWid()
	{
		return _hwid;
	}
	private int _nWindowCount;
	public int getnWindowCount() {
		return _nWindowCount;
	}
	public void setWindowCount(int _nCount) {
		_nWindowCount = _nCount;
	}

	public static boolean isSameHWID(String hwid1, String hwid2) {
		if(CatsGuard.getInstance().isEnabled())
			return hwid1.equals(hwid2);
		if(LameStub.ISLAME)
			return com.lameguard.HWID.equals(hwid1, hwid2, com.lameguard.HWID.HWID_HDD | com.lameguard.HWID.HWID_CPU | com.lameguard.HWID.HWID_BIOS);
		return true;
	}
	public int getBufferErrors() {
		return _bufferError;
	}
	public void clearBufferErrors() {
		_bufferError = 0;
	}
	public void incBufferErrors() {
		_bufferError++;
	}

	public boolean checkKeyProtection()
	{
		return checkKeyProtection(true);
	}

	public boolean checkKeyProtection(boolean send)
	{
		if (!KeyProtection.isActive())
			return true;

		if (!acceptAction.contains(_activeChar.getObjectId()))
		{
			if (!send)
				return false;
			else if (!KeyProtection.getInstance().access(_activeChar, ""))
				return false;
		}

		return true;
	}

	public void setKeyProtection(boolean active)
	{
		if (active)
			acceptAction.add(_activeChar.getObjectId());
		else
			acceptAction.remove((Integer)_activeChar.getObjectId());
	}

	protected static final Logger _packetLog = Logger.getLogger("PacketLogger");

	@Override
	public void logInfo(String str)
	{
		if (_activeChar == null)
			return;

		//if (developer.packetLoggerList.contains(_activeChar.getObjectId()))
			_packetLog.info(_activeChar.getName() + " (" + _activeChar.getObjectId() + "): " + str);
	}
}
