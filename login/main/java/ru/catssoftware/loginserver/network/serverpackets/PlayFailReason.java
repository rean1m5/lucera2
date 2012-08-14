package ru.catssoftware.loginserver.network.serverpackets;

public enum PlayFailReason
{
	REASON_SYSTEM_ERROR(0x01), REASON_USER_OR_PASS_WRONG(0x02), REASON3(0x03), REASON4(0x04), REASON_TOO_MANY_PLAYERS(0x0f);

	private final int	_code;

	PlayFailReason(int code)
	{
		_code = code;
	}

	public final int getCode()
	{
		return _code;
	}
}