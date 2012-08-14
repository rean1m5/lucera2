package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2SepulchersOpenBoxInstance extends L2SepulcherNpcInstance {

	public L2SepulchersOpenBoxInstance(int objectID, L2NpcTemplate template) {
		super(objectID, template);
	}

	
	@Override
	public void onSpawn() {
		setBusy(false);
	}
	@Override
	protected void doAction(L2PcInstance player) {
			if(isBusy())
				return;
			setBusy(true);
			setIsInvul(false);
			reduceCurrentHp(getMaxHp()+1, player);
			_mausoleum.nextStage();
	}
}
