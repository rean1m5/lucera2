package ru.catssoftware.gameserver.jdklogextensions;

import java.io.IOException;
import java.util.logging.FileHandler;

public final class GmAccessLogHandler extends FileHandler
{
	public GmAccessLogHandler() throws IOException, SecurityException
	{
		super();
	}
}
