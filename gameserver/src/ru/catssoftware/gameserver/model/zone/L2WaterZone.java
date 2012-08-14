package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class L2WaterZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance && ((L2PcInstance) character).isInBoat())
			return;

		character.setInsideZone(this,FLAG_WATER, true);

		if (character instanceof L2PcInstance)
		{
			((L2PcInstance) character).broadcastUserInfo();
		}
		else if (character instanceof L2NpcInstance)
			character.broadcastFullInfo();

		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(this,FLAG_WATER, false);
		if (character instanceof L2PcInstance)
			((L2PcInstance) character).broadcastUserInfo();
		else if (character instanceof L2NpcInstance)
			character.broadcastFullInfo();

		super.onExit(character);
	}
}