package ru.catssoftware.gameserver.network.serverpackets;

public class AskJoinPledge extends L2GameServerPacket
{
	private static final String	_S__2C_ASKJOINPLEDGE	= "[S] 2c AskJoinPledge [ds]";

	private int					_requestorObjId;
	private String				_pledgeName;

	public AskJoinPledge(int requestorObjId, String pledgeName)
	{
		_requestorObjId = requestorObjId;
		_pledgeName = pledgeName;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x32);
		writeD(_requestorObjId);
		writeS(_pledgeName);
	}

	@Override
	public String getType()
	{
		return _S__2C_ASKJOINPLEDGE;
	}
}
