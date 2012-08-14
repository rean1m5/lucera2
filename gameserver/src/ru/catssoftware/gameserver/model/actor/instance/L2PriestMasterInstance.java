package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.model.base.ClassType;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2PriestMasterInstance extends L2VillageMasterInstance {

	public L2PriestMasterInstance(int objectId, L2NpcTemplate template) {
		super(objectId, template);
	}
	@Override
	protected ClassType getVillageMasterTeachType() {
		return ClassType.Priest;
	}

}
