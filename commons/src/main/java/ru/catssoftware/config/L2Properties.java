/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.log4j.Logger;


/**
 * @author Noctarius
 */
public final class L2Properties extends Properties
{
	private static final long	serialVersionUID	= -4599023842346938325L;
	public static String CONFIG_DIR=null;
	private static final Logger _log = Logger.getLogger(L2Properties.class);

	private boolean _warn = true;

	public L2Properties()
	{
	}

	public L2Properties setLog(boolean warn)
	{
		_warn = warn;

		return this;
	}

	public L2Properties(String name) throws IOException
	{
		File f;
		if(CONFIG_DIR!=null) {
			f = new File(CONFIG_DIR+name);
			if(!f.exists())
				f = new File(name);
		} else
			f = new File(name);
		load(new FileInputStream(f));
	}

	public L2Properties(File file) throws IOException
	{
		load(new FileInputStream(file));
	}

	public L2Properties(InputStream inStream) throws IOException
	{
		load(inStream);
	}

	public L2Properties(Reader reader) throws IOException
	{
		load(reader);
	}

	public void load(String name) throws IOException
	{
		load(new FileInputStream(name));
	}

	public void load(File file) throws IOException
	{
		load(new FileInputStream(file));
	}

	@Override
	public void load(InputStream inStream) throws IOException
	{
		try
		{
			super.load(inStream);
			for (Object key : keySet()) // Преобразование в UTF-8
				setProperty(key.toString(), new String( super.getProperty(key.toString()).getBytes("iso8859-1"), "utf-8"));
		}
		finally
		{
			inStream.close();
		}
	}

	@Override
	public void load(Reader reader) throws IOException
	{
		try
		{
			super.load(reader);
		}
		finally
		{
			reader.close();
		}
	}

	@Override
	public String getProperty(String key)
	{
		String property = super.getProperty(key);

		if (property == null)
		{
			if (_warn)
				_log.warn("L2Properties: Missing property for key - " + key);

			return null;
		}

		return property.trim();
	}

	@Override
	public String getProperty(String key, String defaultValue)
	{
		String property = super.getProperty(key, defaultValue);

		if (property == null)
		{
			if (_warn)
				_log.warn("L2Properties: Missing defaultValue for key - " + key);

			return null;
		}

		return property.trim();
	}
}