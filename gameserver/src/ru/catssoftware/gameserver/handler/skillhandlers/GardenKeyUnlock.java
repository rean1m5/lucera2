package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class GardenKeyUnlock implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.GARDEN_KEY_UNLOCK };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2Object[] targetList = skill.getTargetList(activeChar);

		if (targetList == null)
			return;

		L2DoorInstance door = (L2DoorInstance) targetList[0];
		switch (skill.getId())
		{
			// Gate Key: Kamael
			case 9703:
				if (door.getDoorId() == 16200002)
					door.openMe();
				break;
			// Gate Key: Archives
			case 9704:
				if (door.getDoorId() == 16200005)
					door.openMe();
				break;
			// Gate Key: Observation	
			case 9705:
				if (door.getDoorId() == 16200009)
					door.openMe();
				break;
			// Gate Key: Specula
			case 9706:
				if (door.getDoorId() == 16200003)
					door.openMe();
				break;
			// Gate Key: Harkilgamed
			case 9707:
				if (door.getDoorId() == 16200007)
					door.openMe();
				break;
			// Gate Key: Rodenpikula
			case 9708:
				if (door.getDoorId() == 16200008)
					door.openMe();
				break;
			// Gate Key: Arvitaire
			case 9709:
				if (door.getDoorId() == 16200010)
					door.openMe();
				break;
			// Gate Key: Katenar
			case 9710:
				if (door.getDoorId() == 16200006)
					door.openMe();
				break;
			// Gate Key: Prediction
			case 9711:
				if (door.getDoorId() == 16200011)
					door.openMe();
				break;
			// Gate Key: Massive Cavern
			case 9712:
				if (door.getDoorId() == 16200012)
					door.openMe();
				break;
		}
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(skill.getId() + 7360);
		activeChar.sendPacket(sm);
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
