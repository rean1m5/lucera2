package ru.catssoftware.loginserver.clientpackets;

import java.security.GeneralSecurityException;
import java.util.Map;

import javax.crypto.Cipher;

import javolution.util.FastMap;

import ru.catssoftware.Config;
import ru.catssoftware.loginserver.L2LoginClient;
import ru.catssoftware.loginserver.L2LoginClient.LoginClientState;
import ru.catssoftware.loginserver.manager.BanManager;
import ru.catssoftware.loginserver.manager.LoginManager;
import ru.catssoftware.loginserver.manager.LoginManager.AuthLoginResult;
import ru.catssoftware.loginserver.model.Account;
import ru.catssoftware.loginserver.model.GameServerInfo;
import ru.catssoftware.loginserver.network.serverpackets.LoginFailReason;
import ru.catssoftware.loginserver.network.serverpackets.LoginOk;
import ru.catssoftware.loginserver.network.serverpackets.RequestCardId;
import ru.catssoftware.loginserver.network.serverpackets.ServerList;
import ru.catssoftware.loginserver.services.exception.AccountBannedException;
import ru.catssoftware.loginserver.services.exception.AccountWrongPasswordException;
import ru.catssoftware.tools.random.Rnd;


/**
 * Format: x
 * 0 (a leading null)
 * x: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
	private byte[]				_raw	= new byte[128];

	private String				_user, _password;
	private int					_ncotp;
	private static 				Map<String,Integer> _invalidTryes = new FastMap<String, Integer>();
	public String getPassword()
	{
		return _password;
	}

	public String getUser()
	{
		return _user;
	}

	public int getOneTimePassword()
	{
		return _ncotp;
	}

	@Override
	public boolean readImpl()
	{
		if (getAvaliableBytes() >= 128)
		{
			readB(_raw);
			return true;
		}
		else
			return false;
	}
	public static boolean addTry(L2LoginClient cl) {
		synchronized(_invalidTryes) {
			Integer nTry = _invalidTryes.get(cl.getIp());
			if(nTry==null)
				nTry = 0;
			if(nTry>=Config.LOGIN_TRY_BEFORE_BAN) {
				_invalidTryes.remove(cl.getIp());
				BanManager.getInstance().addBanForAddress(cl.getInetAddress(),Config.LOGIN_BLOCK_AFTER_BAN * 1000 );
				return false;
			}
			_invalidTryes.put(cl.getIp(), nTry+1);
			return true;
		}
	}
	public static void clearTry(L2LoginClient cl) {
		synchronized(_invalidTryes) {
			_invalidTryes.remove(cl.getIp());
		}
	}
	@Override
	public void run()
	{
		
		byte[] decrypted = null;
		try
		{
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
		}
		catch (GeneralSecurityException e)
		{
			e.printStackTrace();
			return;
		}

		_user = new String(decrypted, 0x5E, 14).trim();
		_user = _user.toLowerCase();
		_password = new String(decrypted, 0x6C, 16).trim();
		_ncotp = decrypted[0x7c];
		_ncotp |= decrypted[0x7d] << 8;
		_ncotp |= decrypted[0x7e] << 16;
		_ncotp |= decrypted[0x7f] << 24;

		LoginManager lc = LoginManager.getInstance();
		L2LoginClient client = getClient();
		try
		{
			AuthLoginResult result = lc.tryAuthLogin(_user, _password, getClient());
			switch (result)
			{
				case AUTH_SUCCESS:
					RequestAuthLogin.clearTry(client);
					client.setAccount(_user);
					
					client.setSessionKey(lc.assignSessionKeyToClient(_user, client));
					Account acc  = client._accInfo;
					
					if(Config.CARD_ENABLED && !client.getIp().equals(acc.getLastIp()) ) {
						if(Config.DEBUG)
							System.out.println("Client last IP mismatch: await "+acc.getLastIp()+", got "+client.getIp());
						client.setState(LoginClientState.AUTHED_CARD);
					
					} else {
						client.setState(LoginClientState.AUTHED_LOGIN);
						if (Config.SHOW_LICENCE)
							client.sendPacket(new LoginOk(getClient().getSessionKey()));
						else {
							getClient().sendPacket(new ServerList(getClient(),true));
							try { Thread.sleep(20); } catch(InterruptedException e) {
								return;
							}
							getClient().sendPacket(new ServerList(getClient(),false));
						}
						return;
					} 
					break;
				case ALREADY_ON_LS:
					L2LoginClient oldClient;
					if ((oldClient = lc.getAuthedClient(_user)) != null)
					{
						lc.removeAuthedLoginClient(_user);
						// kick the other client
						oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
						lc.removeAuthedLoginClient(_user);
					}
					// kick also current client
					client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
					return;
				case ALREADY_ON_GS:
					GameServerInfo gsi;
					if ((gsi = lc.getAccountOnGameServer(_user)) != null)
					{
						client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);

						// kick from there
						if (gsi.isAuthed()) 
							gsi.getGameServerThread().kickPlayer(_user);
						gsi.getGameServerThread().removeAcc(_user);
							
					}
					return;
				case INVALID_PASSWORD:
					if(!RequestAuthLogin.addTry(client)) {
						client.close(LoginFailReason.REASON_ACCESS_FAILED);
						return;
					}
					client.setSessionKey(lc.assignSessionKeyToClient(client.getAccount(), client));
					if(!Config.CARD_ENABLED) 
						if(Config.BRUT_PROTECTION_ENABLED) {
							client.setSessionKey(lc.assignSessionKeyToClient(client.getAccount(), client));
							if (Config.SHOW_LICENCE) {
								client.sendPacket(new LoginOk(getClient().getSessionKey()));
							}
							else
								getClient().sendPacket(new ServerList(getClient(),true));
						} else {
							client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
							return;
						}
					break;
				default:
					client.close(LoginFailReason.REASON_SYSTEM_ERROR);
					return;
			}
			if(Config.CARD_ENABLED) {
				client._CardNo = (byte)(1+Rnd.get(16));
				client.sendPacket(new RequestCardId());
			}
		}
		catch (AccountBannedException e)
		{
			client.close(LoginFailReason.REASON_ACCOUNT_BANNED);
		}
		catch (AccountWrongPasswordException e)
		{
			client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
		} 
	}
}