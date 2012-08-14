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
package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

/**
 * @author Kerberos
 */

public class L2TownPetInstance extends L2NpcInstance
{
	private int _spawnX, _spawnY, _spawnZ;

	public L2TownPetInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The color to display in the select window is White
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2ArtefactInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void firstSpawn()
	{
		super.firstSpawn();
		_spawnX = getX();
		_spawnY = getY();
		_spawnZ = getZ();
		if (Config.ALLOW_PET_WALKERS)
			ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RandomWalkTask(), 2000, 4000,false);
	}

	public class RandomWalkTask implements Runnable
	{
		public void run()
		{
			if (!isInActiveRegion())
				return; // but rather the AI should be turned off completely

			int randomX = _spawnX + Rnd.get(-1, 1) * 50;
			int randomY = _spawnY + Rnd.get(-1, 1) * 50;

			if (randomX != getX() || randomY != getY())
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(randomX, randomY, _spawnZ, 0));
		}
	}
}