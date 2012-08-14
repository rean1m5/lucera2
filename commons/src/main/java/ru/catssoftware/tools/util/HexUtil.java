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
package ru.catssoftware.tools.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Properties;
import java.util.Random;

import ru.catssoftware.config.L2Properties;


/**
 * This class provide tools functions for hex manipulations
 */
public class HexUtil
{
	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Random rnd = new Random();
		for (int i = 0; i < size; i++)
			array[i] = (byte) rnd.nextInt(256);

		return array;
	}

	/**
	 * @param string
	 * @return
	 */
	public static byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}

	public static String hexToString(byte[] hex)
	{
		if (hex == null)
			return "null";
		return new BigInteger(hex).toString(16);
	}

	/**
	 * Save hexadecimal ID of the server in the properties file.
	 * @param string (String) : hexadecimal ID of the server to store
	 * @param fileName (String) : name of the properties file
	 */
	public static void saveHexid(String string, String fileName)
	{
		try
		{
			Properties hexSetting = new L2Properties();
			File file = new File(fileName);
			//Create a new empty file only if it doesn't exist
			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("HexID", string);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Save hexadecimal ID of the server in the properties file.
	 * @param hexId (String) : hexadecimal ID of the server to store
	 * @param fileName (String) : name of the properties file
	 */
	public static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			Properties hexSetting = new L2Properties();
			File file = new File(fileName);
			//Create a new empty file only if it doesn't exist
			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String printData(byte[] data, int len)
	{
		StringBuffer result = new StringBuffer();

		int counter = 0;

		for (int i = 0; i < len; i++)
		{
			if (counter % 16 == 0)
				result.append(fillHex(i, 4) + ": ");

			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if (counter == 16)
			{
				result.append("   ");

				int charpoint = i - 15;
				for (int a = 0; a < 16; a++)
				{
					int t1 = data[charpoint++];
					if (t1 > 0x1f && t1 < 0x80)
						result.append((char) t1);
					else
						result.append('.');
				}

				result.append("\n");
				counter = 0;
			}
		}

		int rest = data.length % 16;
		if (rest > 0)
		{
			for (int i = 0; i < 17 - rest; i++)
				result.append("   ");

			int charpoint = data.length - rest;
			for (int a = 0; a < rest; a++)
			{
				int t1 = data[charpoint++];
				if (t1 > 0x1f && t1 < 0x80)
					result.append((char) t1);
				else
					result.append('.');
			}

			result.append("\n");
		}

		return result.toString();
	}

	public static String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);

		for (int i = number.length(); i < digits; i++)
			number = "0" + number;

		return number;
	}

	/**
	 * @param raw
	 * @return
	 */
	public static String printData(byte[] raw)
	{
		return printData(raw, raw.length);
	}
}