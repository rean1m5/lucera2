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
package ru.catssoftware.gameserver.model.zone.form;

import org.apache.log4j.Logger;

import org.w3c.dom.Node;

public class Tupel
{
	protected static Logger	_log	= Logger.getLogger(Tupel.class.getName());

	public int				x		= 0;
	public int				y		= 0;

	public static Tupel parseTupel(Node n, int zoneId)
	{
		if (n.getAttributes().getNamedItem("x") == null || n.getAttributes().getNamedItem("y") == null)
		{
			_log.error("x or y value missing in zone "+zoneId);
			return null;
		}

		try
		{
			Tupel t = new Tupel();
			t.x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
			t.y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
			return t;
		}
		catch (NumberFormatException nfe)
		{
			_log.error("x or y value not a number in zone " + zoneId);
		}
		return null;
	}
}