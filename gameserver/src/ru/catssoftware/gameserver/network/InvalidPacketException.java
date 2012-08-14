package ru.catssoftware.gameserver.network;

public class InvalidPacketException extends Exception
{
	private static final long serialVersionUID = -8023992556276431695L;

	public InvalidPacketException()
	{
		super();
	}

	public InvalidPacketException(String message)
	{
		super(message);
	}

	public InvalidPacketException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public InvalidPacketException(Throwable cause)
	{
		super(cause);
	}
}