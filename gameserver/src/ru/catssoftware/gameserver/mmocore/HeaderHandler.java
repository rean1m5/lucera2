package ru.catssoftware.gameserver.mmocore;

@SuppressWarnings("unchecked")
public abstract class HeaderHandler<T extends MMOClient, H extends HeaderHandler<T, H>>
{
	private final H _subHeaderHandler;

	public HeaderHandler(H subHeaderHandler)
	{
		_subHeaderHandler = subHeaderHandler;
	}

	/**
	 * @return the subHeaderHandler
	 */
	public final H getSubHeaderHandler()
	{
		return _subHeaderHandler;
	}

	public final boolean isChildHeaderHandler()
	{
		return this.getSubHeaderHandler() == null;
	}
}
