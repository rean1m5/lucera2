package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class L2JailZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(this,FLAG_JAIL, true);
			character.setInsideZone(this,FLAG_NOCHAT, true);
			character.setInsideZone(this,FLAG_NOSUMMON, true);
			if (Config.JAIL_IS_PVP)
			{
				character.setInsideZone(this,FLAG_PVP, true);
				character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
			}
		}
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(this,FLAG_JAIL, false);
			character.setInsideZone(this,FLAG_NOCHAT, false);
			character.setInsideZone(this,FLAG_NOSUMMON, false);
			if (Config.JAIL_IS_PVP)
			{
				character.setInsideZone(this,FLAG_PVP, false);
				character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
			}
			if (((L2PcInstance) character).isInJail())
				character.teleToLocation(-114356, -249645, -2984, false);
		}
		super.onExit(character);
	}
}