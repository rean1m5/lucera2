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
package ru.catssoftware.tools.security;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:18 $
 */
public class Crypt
{
	private final byte[]	_key			= new byte[16];
	private boolean			_isEnabled;
	public static String	_comKey			= "D91GEGW9EG1QEQ51GDVQ";

	public void setKey(byte[] key)
	{
		System.arraycopy(key, 0, _key, 0, key.length);
		_isEnabled = true;
	}

	public void decrypt(ByteBuffer buf)
	{
		if (!_isEnabled)
			return;

		final int sz = buf.remaining();
		int temp = 0;
		for (int i = 0; i < sz; i++)
		{
			int temp2 = buf.get(i);
			buf.put(i, (byte) (temp2 ^ _key[i & 15] ^ temp));
			temp = temp2;
		}

		int old = _key[8] & 0xff;
		old |= _key[9] << 8 & 0xff00;
		old |= _key[10] << 0x10 & 0xff0000;
		old |= _key[11] << 0x18 & 0xff000000;

		old += sz;

		_key[8] = (byte) (old & 0xff);
		_key[9] = (byte) (old >> 0x08 & 0xff);
		_key[10] = (byte) (old >> 0x10 & 0xff);
		_key[11] = (byte) (old >> 0x18 & 0xff);
	}

	public void encrypt(ByteBuffer buf)
	{
		if (!_isEnabled)
			return;

		int temp = 0;
		final int sz = buf.remaining();
		for (int i = 0; i < sz; i++)
		{
			int temp2 = buf.get(i);
			temp = temp2 ^ _key[i & 15] ^ temp;
			buf.put(i, (byte) temp);
		}

		int old = _key[8] & 0xff;
		old |= _key[9] << 8 & 0xff00;
		old |= _key[10] << 0x10 & 0xff0000;
		old |= _key[11] << 0x18 & 0xff000000;

		old += sz;

		_key[8] = (byte) (old & 0xff);
		_key[9] = (byte) (old >> 0x08 & 0xff);
		_key[10] = (byte) (old >> 0x10 & 0xff);
		_key[11] = (byte) (old >> 0x18 & 0xff);
	}

	public static String createMD5(String raw)
	{
		String output = null;
		try
		{
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			md.update(raw.getBytes(), 0, raw.length());
			output = new BigInteger(1, md.digest()).toString(16);
		}
		catch (NoSuchAlgorithmException e)
		{
		}
		return output;
	}

	public static String convertToHex(byte[] data)
	{
		StringBuilder buf = new StringBuilder();
		for (byte aData : data) {
			int halfbyte = (aData >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = aData & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String toSHa1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		MessageDigest md;
		md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}
}