package ru.catssoftware.gameserver.network.serverpackets;

public class AskJoinAlly extends L2GameServerPacket
{
	private static final String	_S__BB_ASKJOINALLY	= "[S] bb AskJoinAlly [ds]";

	private String				_requestorName;
	private int					_requestorObjId;

	public AskJoinAlly(int requestorObjId, String requestorName)
	{
		_requestorName = requestorName;
		_requestorObjId = requestorObjId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xa8);
		writeD(_requestorObjId);
		writeS(_requestorName);
	}

	@Override
	public String getType()
	{
		return _S__BB_ASKJOINALLY;
	}
}
