package ru.catssoftware.gameserver.network.clientpackets;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.network.serverpackets.KeyPacket;
import ru.catssoftware.gameserver.network.serverpackets.SendStatus;
import ru.catssoftware.protection.LameStub;

public class ProtocolVersion extends L2GameClientPacket
{
	private static final String	_C__00_PROTOCOLVERSION	= "[C] 00 ProtocolVersion";
	private long				_version;
    private byte[] _data;
    private byte[] _check;

	@Override
	protected void readImpl()
	{
		if(LameStub.ISLAME && _buf.remaining()>0x100) {
			_version = readD();
            _data = new byte[0x100];
            _check = new byte[4];
            readB(_data);
            readB(_check);
		} else
			_version = readH();
	}

	@Override
	protected void runImpl()
	{
		KeyPacket kp = null;
		if(LameStub.ISLAME) {
	        if (_version == -2L)
	        {
	            getClient().closeNow();
	        }
	        else if (_version == -3L)
	        {
	            getClient().close(new SendStatus());
	            return;
	        }
	        else if (_version != 746)
	        {
	            getClient().close(new KeyPacket(null));
	        }
	        else
	        {
	            try
	            {
	            	
	                if (com.lameguard.LameGuard.getInstance().checkData(_data, _check))
	                {
	                    	com.lameguard.session.ClientSession cs = com.lameguard.LameGuard.getInstance().checkClient(getClient().getConnection().getSocket().getInetAddress().getHostAddress(), _data);
	                    	if(cs!=null ) {
	                    		_data = new byte[0x400 + 1];
	                    		byte[] key = getClient().enableCrypt();
	                    		getClient().setHWID(cs.getHWID());
	                    		getClient().setWindowCount(cs.getInstances());
	                    		_data = com.lameguard.LameGuard.getInstance().assembleAnswer(cs, key);
	                    		getClient().setProtocolOk(true);
	                    		getClient().setProtocolVer(_version);
	                    		sendPacket(new KeyPacket(_data));
	                    		return;
	                    	}
	                }
	                else
	                {
	                    getClient().close(new KeyPacket(null));
	                    return;
	                }
	            }
	            catch (Exception e)
	            {
	                
	            }
	            
	            getClient().closeNow();
	        }
			
		} 
		else if (_version == 65534 || _version == -2) // Получение пинга
		{
			if (Config.DEBUG)
				_log.info("Ping received from: " + getClient().getHostAddress());
			getClient().closeNow();
		}
		else if (_version == 65533 || _version == -3) // ПОлучения запроса от L2Top
		{
			_log.info("Remote status request from: " + getClient().getHostAddress());
			getClient().close(new SendStatus());
			return;
		}
		else if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION) // Проверка версии протокола клиента
		{
			_log.info("Wrong Protocol Version " + _version);
			kp = new KeyPacket(getClient().enableCrypt(), 0);
			getClient().sendPacket(kp);
			getClient().setProtocolOk(false);
		}
		else // Все гуд, пускаем клиента
		{
			getClient().setProtocolOk(true);
			getClient().setProtocolVer(_version);
			kp = new KeyPacket(getClient().enableCrypt(), 1);
			sendPacket(kp);
			
		}
	}

	@Override
	public String getType()
	{
		return _C__00_PROTOCOLVERSION;
	}
}