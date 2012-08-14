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
package ru.catssoftware.tools.network;

import java.util.StringTokenizer;

/**
 * @author G1ta0
 */
public class SubNet
{
	private final int	_net;
	private final int	_mask;

	public SubNet(int net, int mask)
	{
		_net = net;
		_mask = mask;
	}

	public SubNet(String net, int mask)
	{
		_net = ipToInt(net);
		_mask = mask;
	}

	public SubNet(String net, String mask)
	{
		_net = ipToInt(net);
		_mask = ipToInt(mask);
	}

	public SubNet(int net, String mask)
	{
		_net = net;
		_mask = ipToInt(mask);
	}

	public SubNet(String netMask)
	{
		StringTokenizer st = new StringTokenizer(netMask.trim(), "/");
		_net = ipToInt(st.nextToken());
		if (st.hasMoreTokens())
			_mask = ipToInt(st.nextToken());
		else
			_mask = 0xFFFFFFFF;
	}

	public int getNet()
	{
		return _net;
	}

	public int getMask()
	{
		return _mask;
	}

	public boolean isInSubnet(int ip)
	{

		return ((ip & _mask) == _net);
	}

	public boolean isInSubnet(String ip)
	{
		int _ip = ipToInt(ip);
		return isInSubnet(_ip);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SubNet)
			return (((SubNet) obj).getNet() == _net && ((SubNet) obj).getMask() == _mask);

		return false;
	}

	private int ipToInt(String ip)
	{
		int _ip = 0;

		StringTokenizer st = new StringTokenizer(ip.trim(), ".");

		int _dots = st.countTokens();

		if (_dots == 1)
		{
			_ip = 0xFFFFFFFF;
			try
			{
				int _bitmask = Integer.parseInt(st.nextToken());

				if (_bitmask > 0)
				{
					if (_bitmask < 32)
						_ip = (_ip << (32 - _bitmask));
				}
				else
					_ip = 0;
			}
			catch (NumberFormatException e)
			{
			}
		}
		else
		{
			for (int i = 0; i < _dots; i++)
			{
				try
				{
					_ip += (Integer.parseInt(st.nextToken()) << (24 - i * 8));
				}
				catch (NumberFormatException e)
				{
				}
			}
		}

		return _ip;
	}
}