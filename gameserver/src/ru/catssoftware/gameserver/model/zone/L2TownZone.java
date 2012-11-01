package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.TownManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.serverpackets.AgitDecoInfo;

public class L2TownZone extends L2DefaultZone
{
	@Override
	protected void register()
	{
		TownManager.getInstance().registerTown(this);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		boolean peace = true;
/*		if(character instanceof L2MonsterInstance) {
			L2MonsterInstance mob = (L2MonsterInstance)character;
			/*if(mob.getSpawn()!=null && !ZoneManager.getInstance().isInsideZone(ZoneType.Town, mob.getSpawn().getLocx(), mob.getSpawn().getLocy())) {
				mob.stopMove();
				
				mob.abortAttack();
				mob.teleToLocation(mob.getSpawn().getLocx(), mob.getSpawn().getLocy(), mob.getSpawn().getLocz());
				return;
			} 
		} */
		if (character.isPlayer())
		{
			if (((L2PcInstance) character).getSiegeState() != 0 && Config.ZONE_TOWN == 1)
			{
				character.setInsideZone(this,FLAG_PVP, true);
				peace = false;
			}
		}

		if (Config.ZONE_TOWN == 2)
		{
			peace = false;
			character.setInsideZone(this,FLAG_PVP, true);
		}
		if (peace)
			character.setInsideZone(this,FLAG_PEACE, true);

		// Players must always see deco, not only inside clan hall.
		if (character.isPlayer())
		{
			ClanHall[] townHalls = ClanHallManager.getInstance().getTownClanHalls(getTownId());
			if (townHalls!=null)
				for (ClanHall ch : townHalls)
					if (ch.getOwnerId()>0)
						character.getPlayer().sendPacket(new AgitDecoInfo(ch));
		}
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (character.isInsideZone(FLAG_PVP))
			character.setInsideZone(this,FLAG_PVP, false);

		if (character.isInsideZone(FLAG_PEACE))
			character.setInsideZone(this,FLAG_PEACE, false);
		super.onExit(character);
	}
}