package ru.catssoftware.gameserver.model.restriction;

public class RestrictionBindClassException extends Exception
{
	private static final long	serialVersionUID	= -2193188657782054883L;

	public RestrictionBindClassException()
	{
		super();
	}

	public RestrictionBindClassException(String message)
	{
		super(message);
	}
}