package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;

public class L2StadiumZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer() && ((L2PcInstance) character).getOlympiadGameId() == -1 && !((L2PcInstance) character).isGM())
		{
			character.teleToLocation(TeleportWhereType.Town);
			return;
		}
		character.setInsideZone(this,FLAG_STADIUM, true);
		character.setInsideZone(this,FLAG_PVP, true);
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(this,FLAG_STADIUM, false);
		character.setInsideZone(this,FLAG_PVP, false);
		super.onExit(character);
	}
}