package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class SocialAction extends L2GameServerPacket
{
	private static final String	_S__3D_SOCIALACTION	= "[S] 2D SocialAction";
	private int					_charObjId,	_actionId;

	public SocialAction(int playerId, int actionId)
	{
		_charObjId = playerId;
		_actionId = actionId;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x2D);
		writeD(_charObjId);
		writeD(_actionId);
	}

	@Override
	public String getType()
	{
		return _S__3D_SOCIALACTION;
	}
}
