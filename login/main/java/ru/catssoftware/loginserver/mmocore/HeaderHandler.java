package ru.catssoftware.loginserver.mmocore;

abstract class HeaderHandler<T extends MMOConnection<T>, H extends HeaderHandler<T, H>>
{
	private final H _subHeaderHandler;

	HeaderHandler(H subHeaderHandler)
	{
		_subHeaderHandler = subHeaderHandler;
	}

	final H getSubHeaderHandler()
	{
		return _subHeaderHandler;
	}

	final boolean isChildHeaderHandler()
	{
		return getSubHeaderHandler() == null;
	}

	private final HeaderInfo<T> _headerInfoReturn = new HeaderInfo<T>();

	protected final HeaderInfo<T> getHeaderInfoReturn()
	{
		return _headerInfoReturn;
	}
}