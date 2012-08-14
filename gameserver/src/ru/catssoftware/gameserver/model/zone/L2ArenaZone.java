package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.model.L2Character;
// import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class L2ArenaZone extends L2DefaultZone
{
	@Override
	protected void onEnter(L2Character character)
	{
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		super.onExit(character);
/*		if(character instanceof L2PcInstance) {
			L2PcInstance pc = (L2PcInstance)character;
			if(pc.getGameEvent()!=null && pc.getGameEvent().isRunning() && !pc.isTeleporting()) {
				pc.sendMessage("Вы покинули зону эвента, и будете удалены с эвента");
				pc.getGameEvent().remove(pc);
			}
		} */
	}
}