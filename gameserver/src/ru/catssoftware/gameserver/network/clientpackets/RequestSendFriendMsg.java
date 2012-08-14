package ru.catssoftware.gameserver.network.clientpackets;


import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.L2FriendSay;

public class RequestSendFriendMsg extends L2GameClientPacket
{
	private static final String	_C__CC_REQUESTSENDMSG	= "[C] CC RequestSendMsg";
	private static Logger			_logChat				= Logger.getLogger("chat");
	private String				_message;
	private String				_reciever;

	@Override
	protected void readImpl()
	{
		_message = readS();
		_reciever = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2PcInstance targetPlayer = L2World.getInstance().getPlayer(_reciever);

		if (targetPlayer == null)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}

		if (Config.LOG_CHAT)
		{
			_logChat.info("PRIV_MSG" + "[" + activeChar.getName() + " to " + _reciever + "]" + _message);
		}

		L2FriendSay frm = new L2FriendSay(activeChar.getName(), _reciever, _message);
		targetPlayer.sendPacket(frm);
	}

	@Override
	public String getType()
	{
		return _C__CC_REQUESTSENDMSG;
	}
}
