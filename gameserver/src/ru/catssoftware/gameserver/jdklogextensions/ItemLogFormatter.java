package ru.catssoftware.gameserver.jdklogextensions;

import java.util.logging.LogRecord;
import javolution.text.TextBuilder;

public final class ItemLogFormatter extends L2LogFormatter
{
	@Override
	protected void format0(LogRecord record, TextBuilder tb)
	{
		appendDateLog(record, tb);
		appendMessage(record, tb);
		appendParameters(record, tb, ", ", true);
	}
}
