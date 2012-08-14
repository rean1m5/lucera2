package ru.catssoftware.gameserver.network.serverpackets;

public class AllyCrest extends L2GameServerPacket
{
	private static final String	_S__AF_ALLYCREST	= "[S] ae AllyCrest [ddb]";

	private int					_crestId, _crestSize;
	private byte[]				_data;

	public AllyCrest(int crestId, byte[] data)
	{
		_crestId = crestId;
		_data = data;
		_crestSize = _data.length;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xae);
		writeD(_crestId);
		writeD(_crestSize);
		writeB(_data);
		_data = null;
	}

	@Override
	public String getType()
	{
		return _S__AF_ALLYCREST;
	}
}
