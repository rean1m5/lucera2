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
package ru.catssoftware.gameserver.model.itemcontainer;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ItemInstance.ItemLocation;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public final class ClanWarehouse extends Warehouse
{
	private L2Clan	_clan;

	public ClanWarehouse(L2Clan clan)
	{
		_clan = clan;
	}

	@Override
	public int getOwnerId()
	{
		return _clan.getClanId();
	}

	@Override
	public L2PcInstance getOwner()
	{
		return _clan.getLeader().getPlayerInstance();
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}

	public String getLocationId()
	{
		return "0";
	}

	public int getLocationId(boolean dummy)
	{
		return 0;
	}

	public void setLocationId(L2PcInstance dummy)
	{
	}

	@Override
	public boolean validateCapacity(int slots)
	{
		return (_items.size() + slots <= Config.WAREHOUSE_SLOTS_CLAN);
	}
}
