package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.communitybbs.CommunityBoard;

public class RequestBBSwrite extends L2GameClientPacket
{
	private static final String	_C__22_REQUESTBBSWRITE	= "[C] 22 RequestBBSwrite";
	private String				_url, _arg1, _arg2, _arg3, _arg4, _arg5;

	@Override
	protected void readImpl()
	{
		_url = readS();
		_arg1 = readS();
		_arg2 = readS();
		_arg3 = readS();
		_arg4 = readS();
		_arg5 = readS();
	}

	@Override
	protected void runImpl()
	{
//		CommunityBoard.getInstance().write(getClient(), _url, _arg1, _arg2, _arg3, _arg4, _arg5);
	}

	@Override
	public String getType()
	{
		return _C__22_REQUESTBBSWRITE;
	}
}
