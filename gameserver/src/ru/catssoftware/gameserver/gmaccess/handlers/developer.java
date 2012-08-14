package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegionRestart;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.network.SystemMessageId;

/**
 * @author m095
 * @version 1.0
 */
public class developer extends gmHandler
{
	private final String[]	commands =
	{
		"msg",
		"region_check"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		
		final String cmd = params[0];
		if (cmd.equals("msg"))
		{
			try
			{
				int msgId = Integer.parseInt(params[1]);
				admin.sendPacket(SystemMessageId.getSystemMessageId(msgId));
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //msg [message_id]");
			}
			return;
		}
		else if (cmd.equals("region_check"))
		{
			L2MapRegion region = MapRegionManager.getInstance().getRegion(admin);

			if (region != null)
			{
				L2MapRegionRestart restart = MapRegionManager.getInstance().getRestartLocation(region.getRestartId(admin.getRace()));

				admin.sendMessage("Actual region: " + region.getId());
				admin.sendMessage("Respawn position will be: " + restart.getName() + " (" + restart.getLocName() + ")");

				if (restart.getBannedRace() != null)
				{
					L2MapRegionRestart redirect = MapRegionManager.getInstance().getRestartLocation(restart.getRedirectId());
					admin.sendMessage("Banned race: " + restart.getBannedRace().name());
					admin.sendMessage("Redirect To: " + redirect.getName() + " (" + redirect.getLocName() + ")");
				}

				Location loc;
				loc = MapRegionManager.getInstance().getTeleToLocation(admin, TeleportWhereType.Castle);
				admin.sendMessage("TeleToLocation (Castle): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

				loc = MapRegionManager.getInstance().getTeleToLocation(admin, TeleportWhereType.ClanHall);
				admin.sendMessage("TeleToLocation (ClanHall): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

				loc = MapRegionManager.getInstance().getTeleToLocation(admin, TeleportWhereType.SiegeFlag);
				admin.sendMessage("TeleToLocation (SiegeFlag): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

				loc = MapRegionManager.getInstance().getTeleToLocation(admin, TeleportWhereType.Town);
				admin.sendMessage("TeleToLocation (Town): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

				String nearestTown = TownManager.getInstance().getClosestTownName(admin);
				Announcements.getInstance().announceToAll(admin.getName() + " has tried spawn-announce near " + nearestTown + "!");
			}
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}