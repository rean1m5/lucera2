package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class L2MothertreeZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer())
		{
			L2PcInstance player = (L2PcInstance) character;

			if (player.getRace() != Race.Elf)
				return;
			if (player.isInParty())
			{
				for (L2PcInstance member : player.getParty().getPartyMembers())
				{
					if (member.getRace() != Race.Elf)
						return;
				}
			}
			player.setInsideZone(this,FLAG_MOTHERTREE, true);
			player.sendPacket(SystemMessageId.ENTER_SHADOW_MOTHER_TREE);
		}
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (character.isPlayer() && character.isInsideZone(L2Zone.FLAG_MOTHERTREE))
		{
			character.setInsideZone(this,FLAG_MOTHERTREE, false);
			character.sendPacket(SystemMessageId.EXIT_SHADOW_MOTHER_TREE);
		}
		super.onExit(character);
	}
}