package ru.catssoftware.gameserver.network.clientpackets;



import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.model.L2EnchantSkillLearn;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExEnchantSkillInfo;

public final class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private static final String	_C__D0_06_REQUESTEXENCHANTSKILLINFO	= "[C] D0:06 RequestExEnchantSkillInfo";

	private int					 _skillId, _skillLvl;

	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null || _skillId == 0)
			return;

		if (activeChar.getLevel() < 76)
			return;

		L2FolkInstance trainer = activeChar.getLastFolkNPC();

		if ((trainer == null || !activeChar.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !activeChar.isGM())
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);

		if (skill == null || skill.getId() != _skillId)
		{
			activeChar.sendMessage("Данный скил не найден на сервере");
			return;
		}

		if (trainer != null && !trainer.getTemplate().canTeach(activeChar.getClassId()) && !activeChar.isGM())
			return; // cheater

		this.showEnchantInfo(activeChar, skill);
	}

	public void showEnchantInfo(L2PcInstance activeChar, L2Skill skill)
	{
		boolean canteach = false;

		L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(activeChar);

		for(L2EnchantSkillLearn s : skills)
		{
			if(s.getId() == _skillId && s.getLevel() == _skillLvl)
			{
				canteach = true;
				break;
			}
		}

		if(!canteach)
			return;

		int requiredSp = SkillTreeTable.getInstance().getEnchantSkillSpCost(activeChar, skill);
		int requiredExp = SkillTreeTable.getInstance().getEnchantSkillExpCost(activeChar, skill);
		byte rate = SkillTreeTable.getInstance().getEnchantSkillRate(activeChar, skill);
		ExEnchantSkillInfo asi = new ExEnchantSkillInfo(skill.getId(), skill.getLevel(), requiredSp, requiredExp, rate);

		if(Config.ES_SP_BOOK_NEEDED && (skill.getLevel() == 101 || skill.getLevel() == 141)) // only first lvl requires book
		{
			int spbId = 6622;
			asi.addRequirement(4, spbId, 1, 0);
		}
		sendPacket(asi);
	}

	@Override
	public String getType()
	{
		return _C__D0_06_REQUESTEXENCHANTSKILLINFO;
	}
}