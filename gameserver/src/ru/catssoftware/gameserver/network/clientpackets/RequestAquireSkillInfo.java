package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillSpellbookTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.model.L2PledgeSkillLearn;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2SkillLearn;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.AcquireSkillInfo;

public class RequestAquireSkillInfo extends L2GameClientPacket
{
	private static final String	_C__6B_REQUESTAQUIRESKILLINFO	= "[C] 6B RequestAquireSkillInfo";
	private int					_id, _level, _skillType;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
	}

	@Override
	protected void runImpl()
	{
		boolean canteach = false;

		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);
		if (skill == null)
			return;

		L2FolkInstance trainer = activeChar.getLastFolkNPC();
		if ((trainer == null || !activeChar.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !activeChar.isGM())
			return;

		if (_skillType == 0)
		{
			if (trainer != null && !trainer.getTemplate().canTeach(activeChar.getSkillLearningClassId()))
				return; // cheater

			L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(activeChar, activeChar.getSkillLearningClassId());

			for (L2SkillLearn s : skills)
			{
				if (s.getId() == _id && s.getLevel() == _level)
				{
					canteach = true;
					break;
				}
			}

			if (!canteach)
				return; // cheater :)

			int requiredSp = SkillTreeTable.getInstance().getSkillCost(activeChar, skill);
			AcquireSkillInfo asi = new AcquireSkillInfo(skill.getId(), skill.getLevel(), requiredSp, 0);

			if (Config.SP_BOOK_NEEDED)
			{
				int spbId = -1;
				if (skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION)
					spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill.getId(), _level);
				else
					spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill.getId(), _level);

				if (skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION || skill.getLevel() == 1 && spbId > -1)
					asi.addRequirement(99, spbId, 1, 50);
			}

			sendPacket(asi);
		}
		else if (_skillType == 2)
		{
			int requiredRep = 0;
			int itemId = 0;
			L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(activeChar);

			for (L2PledgeSkillLearn s : skills)
			{
				if (s.getId() == _id && s.getLevel() == _level)
				{
					canteach = true;
					requiredRep = s.getRepCost();
					itemId = s.getItemId();
					break;
				}
			}

			if (!canteach)
				return; // cheater :)

			AcquireSkillInfo asi = new AcquireSkillInfo(skill.getId(), skill.getLevel(), requiredRep, 2);

			if (Config.LIFE_CRYSTAL_NEEDED)
				asi.addRequirement(1, itemId, 1, 0);
			sendPacket(asi);
		}
		else
		// Common Skills
		{
			int costid = 0;
			int costcount = 0;
			int spcost = 0;

			L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSkills(activeChar);

			for (L2SkillLearn s : skillsc)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

				if (sk == null || sk != skill)
					continue;

				canteach = true;
				costid = s.getIdCost();
				costcount = s.getCostCount();
				spcost = s.getSpCost();
			}

			AcquireSkillInfo asi = new AcquireSkillInfo(skill.getId(), skill.getLevel(), spcost, 1);
			asi.addRequirement(4, costid, costcount, 0);
			sendPacket(asi);
		}
	}

	@Override
	public String getType()
	{
		return _C__6B_REQUESTAQUIRESKILLINFO;
	}
}
