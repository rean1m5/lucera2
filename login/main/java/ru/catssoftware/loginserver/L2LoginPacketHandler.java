package ru.catssoftware.loginserver;

import java.nio.ByteBuffer;
import java.util.Map;

import javolution.util.FastMap;

import ru.catssoftware.Config;
import ru.catssoftware.loginserver.L2LoginClient.LoginClientState;
import ru.catssoftware.loginserver.clientpackets.AuthGameGuard;
import ru.catssoftware.loginserver.clientpackets.RequestAuthLogin;
import ru.catssoftware.loginserver.clientpackets.RequestServerList;
import ru.catssoftware.loginserver.clientpackets.RequestServerLogin;
import ru.catssoftware.loginserver.clientpackets.SendCardCode;
import ru.catssoftware.loginserver.mmocore.IPacketHandler;
import ru.catssoftware.loginserver.mmocore.ReceivablePacket;
import ru.catssoftware.loginserver.network.serverpackets.LoginFailReason;



/**
 * Handler for packets received by Login Server
 *
 * @author  KenM
 */
public final class L2LoginPacketHandler implements IPacketHandler<L2LoginClient>
{
	/**
	 * @see com.l2jserver.mmocore.network.IPacketHandler#handlePacket(java.nio.ByteBuffer, com.l2jserver.mmocore.interfaces.MMOClient)
	 */
	private static L2LoginPacketHandler _instance;
	private Map<L2LoginClient, Long> _connections = new FastMap<L2LoginClient, Long>();
	private class ConnectionChecker extends Thread {
		@Override
		public void run() {
			for(;;) {
				long curTime = System.currentTimeMillis();
				synchronized (_connections) {
					for(L2LoginClient cl : _connections.keySet()) {
						if(!cl.checkOK && _connections.get(cl)+Config.INACTIVE_TIMEOUT * 1000 > curTime) {
							cl.close(LoginFailReason.REASON_IGNORE);
							_connections.remove(cl);
						}
					}
				}
				try { Thread.sleep(2000); } catch(Exception e) {}
			}
		}
	}
	public L2LoginPacketHandler() {
		super();
		_instance = this;
		new ConnectionChecker().start();
	}
	
	public static L2LoginPacketHandler getInstance() {
		return _instance;
	}
	public void addClient(L2LoginClient cl) {
		synchronized (_connections) {
			_connections.put(cl, System.currentTimeMillis());
		}
	}
	public ReceivablePacket<L2LoginClient> handlePacket(ByteBuffer buf, L2LoginClient client)
	{
		int opcode = buf.get() & 0xFF;
		ReceivablePacket<L2LoginClient> packet = null;
		LoginClientState state = client.getState();
		switch (state)
		{
			case CONNECTED:
				if (opcode == 0x07)
				{
					packet = new AuthGameGuard();
				}
				else
				{
					this.debugOpcode(opcode, state);
					client.close(LoginFailReason.REASON_ACCESS_FAILED);
				}
				break;
			case AUTHED_GG:
				if (opcode == 0x00)
				{
					packet = new RequestAuthLogin();
				}
				else if (opcode == 0x06)
				{
					packet = new SendCardCode();
				}
				
				else
				{
					if(client.getAccount()!=null)
						this.debugOpcode(opcode, state);
					client.close(LoginFailReason.REASON_ACCESS_FAILED);
				}
				break;
			case AUTHED_CARD:
				if (opcode == 0x06)
				{
					packet = new SendCardCode();
				}
				break;
			case AUTHED_LOGIN:
				if (opcode == 0x06)
				{
					client.setState(LoginClientState.AUTHED_CARD);
					packet = new RequestServerList();
				}
				
				else if (opcode == 0x05)
				{
					packet = new RequestServerList();
				}
				else if (opcode == 0x02)
				{
					packet = new RequestServerLogin();
				}
				else
				{
					this.debugOpcode(opcode, state);
				}
				break;
		}
		if(packet!=null && !client.checkOK) {
			client.checkOK = true;
			synchronized (_connections) {
				_connections.remove(client);
			}
			ClientManager.getInstance().addClient(client);
		}
		return packet;  
	}

	private void debugOpcode(int opcode, LoginClientState state)
	{
		System.out.println("Unknown Opcode: " + opcode + " for state: " + state.name());
	}
}
