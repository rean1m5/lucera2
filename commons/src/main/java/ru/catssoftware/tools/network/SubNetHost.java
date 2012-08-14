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

import java.util.List;

import javolution.util.FastList;

/**
 * @author G1ta0
 */
public class SubNetHost
{
	private String			_hostname;

	private String			_hostip;

	private List<SubNet>	_subnets;

	public SubNetHost(String hostName)
	{
		_hostname = hostName;
		_subnets = new FastList<SubNet>();
	}

	public String getHostname()
	{
		return _hostname;
	}

	public void setHostname(String hostName)
	{
		_hostname = hostName;
	}

	public String getIp()
	{
		return _hostip;
	}

	public void setIp(String ip)
	{
		_hostip = ip;
	}

	public void addSubNet(SubNet net)
	{
		if (net != null)
			_subnets.add(net);
	}

	public void addSubNet(String net, String mask)
	{
		SubNet _net = new SubNet(net, mask);
		addSubNet(_net);
	}

	public boolean isInSubnet(String ip)
	{
		boolean _rightHost = false;
		for (SubNet net : _subnets)
			if (net.isInSubnet(ip))
			{
				_rightHost = true;
				break;
			}
		return _rightHost;
	}
}