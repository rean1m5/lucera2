package ru.catssoftware.gameserver.network.clientpackets;

public class RequestPCCafeCouponUse extends L2GameClientPacket
{
	private static final String	_C__D0_20_REQUESTPCCAFECOUPONUSE	= "[C] D0:20 RequestPCCafeCouponUse";
	private String				_str;

	@Override
	protected void readImpl()
	{
		_str = readS();
	}

	@Override
	protected void runImpl()
	{
		_log.debug("C5: RequestPCCafeCouponUse: S: " + _str);
	}

	@Override
	public String getType()
	{
		return _C__D0_20_REQUESTPCCAFECOUPONUSE;
	}
}