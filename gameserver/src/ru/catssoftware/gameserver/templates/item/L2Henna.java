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
package ru.catssoftware.gameserver.templates.item;

import ru.catssoftware.util.StatsSet;

public final class L2Henna
{
	private final int		_symbolId;
	private final int		_itemId;
	private final int		_price;
	private final int		_amount;
	private final int		_statINT;
	private final int		_statSTR;
	private final int		_statCON;
	private final int		_statMEM;
	private final int		_statDEX;
	private final int		_statWIT;
	private final String	_name;

	public L2Henna(StatsSet set)
	{
		_symbolId = set.getInteger("symbol_id");
		_itemId = set.getInteger("dye_id");
		_price = set.getInteger("price");
		_amount = set.getInteger("dye_amount");
		_statINT = set.getInteger("stat_INT");
		_statSTR = set.getInteger("stat_STR");
		_statCON = set.getInteger("stat_CON");
		_statMEM = set.getInteger("stat_MEM");
		_statDEX = set.getInteger("stat_DEX");
		_statWIT = set.getInteger("stat_WIT");

		String name = "";

		if (_statINT > 0)
			name += "INT +" + _statINT;
		if (_statSTR > 0)
			name += "STR +" + _statSTR;
		if (_statCON > 0)
			name += "CON +" + _statCON;
		if (_statMEM > 0)
			name += "MEN +" + _statMEM;
		if (_statDEX > 0)
			name += "DEX +" + _statDEX;
		if (_statWIT > 0)
			name += "WIT +" + _statWIT;

		if (_statINT < 0)
			name += ", INT " + _statINT;
		if (_statSTR < 0)
			name += ", STR " + _statSTR;
		if (_statCON < 0)
			name += ", CON " + _statCON;
		if (_statMEM < 0)
			name += ", MEN " + _statMEM;
		if (_statDEX < 0)
			name += ", DEX " + _statDEX;
		if (_statWIT < 0)
			name += ", WIT " + _statWIT;

		_name = name;
	}

	public int getSymbolId()
	{
		return _symbolId;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getPrice()
	{
		return _price;
	}

	public int getAmount()
	{
		return _amount;
	}

	public int getStatINT()
	{
		return _statINT;
	}

	public int getStatSTR()
	{
		return _statSTR;
	}

	public int getStatCON()
	{
		return _statCON;
	}

	public int getStatMEM()
	{
		return _statMEM;
	}

	public int getStatDEX()
	{
		return _statDEX;
	}

	public int getStatWIT()
	{
		return _statWIT;
	}

	public String getName()
	{
		return _name;
	}
}