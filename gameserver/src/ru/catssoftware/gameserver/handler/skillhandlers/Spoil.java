package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Spoil implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.SPOIL };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar.isPlayer()))
			return;

		for (L2Character element : targets)
		{
			if (!(element instanceof L2MonsterInstance))
				continue;

			L2MonsterInstance target = (L2MonsterInstance) element;

			if (target.isSpoil())
			{
				activeChar.sendPacket(SystemMessageId.ALREADY_SPOILED);
				continue;
			}

			// SPOIL SYSTEM by Lbaldi
			boolean spoil = false;
			if (!target.isDead())
			{
				spoil = Formulas.calcMagicSuccess(activeChar, target, skill);

				if (spoil)
				{
					target.setSpoil(true);
					target.setIsSpoiledBy(activeChar.getObjectId());
					activeChar.sendPacket(SystemMessageId.SPOIL_SUCCESS);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}				
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
			}
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}