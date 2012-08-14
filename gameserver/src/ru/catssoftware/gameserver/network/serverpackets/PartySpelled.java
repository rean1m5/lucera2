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
package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;

public final class PartySpelled extends EffectInfoPacket
{
	private static final String _S__EE_PartySpelled = "[S] EE PartySpelled";

	public PartySpelled(EffectInfoPacketList list)
	{
		super(list);
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xEE);
		writeD(getPlayable() instanceof L2SummonInstance ? 2 : getPlayable() instanceof L2PetInstance ? 1 : 0);
		writeD(getPlayable().getObjectId());
		writeD(size());
		writeEffectInfos();
	}

	@Override
	public String getType()
	{
		return _S__EE_PartySpelled;
	}
}