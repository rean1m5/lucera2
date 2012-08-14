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
package ru.catssoftware.gameserver.model;

/**
 *
 */
public class ItemRequest
{
	int	_objectId;
	int	_itemId;
	int	_count;
	int	_price;

	public ItemRequest(int objectId, int count, int price)
	{
		_objectId = objectId;
		_count = count;
		_price = price;
	}

	public ItemRequest(int objectId, int itemId, int count, int price)
	{
		_objectId = objectId;
		_itemId = itemId;
		_count = count;
		_price = price;
	}

	public int getObjectId()
	{
		return _objectId;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public void setCount(int count)
	{
		_count = count;
	}

	public int getCount()
	{
		return _count;
	}

	public int getPrice()
	{
		return _price;
	}
}
