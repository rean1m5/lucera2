//TODO: Remove?

package ru.catssoftware.gameserver.network.serverpackets;

public class TradeOtherDone extends L2GameServerPacket
{
	private static final String _S__82_SENDTRADEOTHERDONE = "[S] 82 SendTradeOtherDone";
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x82);
	}
	
	@Override
	public String getType()
	{
		return _S__82_SENDTRADEOTHERDONE;
	}
}