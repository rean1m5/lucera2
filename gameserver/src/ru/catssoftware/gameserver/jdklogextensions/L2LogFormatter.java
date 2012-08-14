package ru.catssoftware.gameserver.jdklogextensions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import javolution.text.TextBuilder;

public abstract class L2LogFormatter extends Formatter
{
	private static final SimpleDateFormat DATE_SYS_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final SimpleDateFormat DATE_LOG_FORMAT = new SimpleDateFormat("dd MMM HH:mm:ss,SSS");
	
	@Override
	public final String format(LogRecord record)
	{
		TextBuilder tb = TextBuilder.newInstance();
		
		format0(record, tb);
		
		appendNewline(tb);
		
		try
		{
			return tb.toString();
		}
		finally
		{
			TextBuilder.recycle(tb);
		}
	}
	
	protected abstract void format0(LogRecord record, TextBuilder tb);
	
	protected final void appendDateLog(LogRecord record, TextBuilder tb)
	{
		tb.append("[").append(DATE_LOG_FORMAT.format(new Date(record.getMillis()))).append("] ");
	}

	protected final void appendDateSys(LogRecord record, TextBuilder tb)
	{
		tb.append("[").append(DATE_SYS_FORMAT.format(new Date(record.getMillis()))).append("] ");
	}
	
	protected final void appendMessage(LogRecord record, TextBuilder tb)
	{
		tb.append(record.getMessage());
	}
	
	protected final void appendParameters(LogRecord record, TextBuilder tb, String separator, boolean before)
	{
		if (record.getParameters() != null)
		{
			for (Object parameter : record.getParameters())
			{
				if (parameter == null)
					continue;
				
				if (before)
				{
					tb.append(separator);
					tb.append(parameter);
				}
				else
				{
					tb.append(parameter);
					tb.append(separator);
				}
			}
		}
	}
	
	protected final void appendNewline(TextBuilder tb)
	{
		tb.append("\r\n");
	}
}
