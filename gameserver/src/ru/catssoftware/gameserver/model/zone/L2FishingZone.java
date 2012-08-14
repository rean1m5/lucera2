package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;

public class L2FishingZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(this,FLAG_FISHING, true);

		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(this,FLAG_FISHING, false);

		super.onExit(character);
	}
}