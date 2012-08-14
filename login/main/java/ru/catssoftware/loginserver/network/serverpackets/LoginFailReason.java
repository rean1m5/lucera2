package ru.catssoftware.loginserver.network.serverpackets;

public enum LoginFailReason
{
	REASON_SYSTEM_ERROR(0x01),
	REASON_PASS_WRONG(0x02),
	REASON_USER_OR_PASS_WRONG(0x03),
	REASON_ACCESS_FAILED(0x04),
	REASON_ACCOUNT_IN_USE(0x07),
	REASON_ACCOUNT_BANNED(0x09),
	REASON_SERVER_OVERLOADED   (0x0f),
	REASON_DUAL_BOX            (0x23),
	REASON_IGNORE (0x17),
	REASON_INVALID_SECURITY_CARD_NO   (0x1f);

	private final int	_code;

	LoginFailReason(int code)
	{
		_code = code;
	}

	public final int getCode()
	{
		return _code;
	}
}