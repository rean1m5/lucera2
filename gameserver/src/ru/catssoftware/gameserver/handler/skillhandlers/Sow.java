package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Manor;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.PlaySound;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

public class Sow implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.SOW };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance activePlayer = (L2PcInstance) activeChar;

		for (L2Character element : targets)
		{
			if (!(element instanceof L2MonsterInstance))
				continue;

			L2MonsterInstance target = (L2MonsterInstance) element;

			if (target.isSeeded())
				continue;

			if (target.isDead())
				continue;

			if (target.getSeeder() != activeChar)
				continue;

			int seedId = target.getSeedType();
			if (seedId == 0)
				continue;

			L2ItemInstance item = activePlayer.getInventory().getItemByItemId(seedId);
			if (item == null)
				return;

			// Consuming used seed
			activePlayer.destroyItem("Consume", item.getObjectId(), 1, null, false);

			SystemMessage sm;
			if (calcSuccess(activePlayer, target, seedId))
			{
				activePlayer.sendPacket(new PlaySound(0, "Itemsound.quest_itemget"));
				target.setSeeded();
				sm = new SystemMessage(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
			}
			else
				sm = new SystemMessage(SystemMessageId.THE_SEED_WAS_NOT_SOWN);
			if (activePlayer.getParty() == null)
				activePlayer.sendPacket(sm);
			else
				activePlayer.getParty().broadcastToPartyMembers(sm);
		}
	}

	private boolean calcSuccess(L2PcInstance activeChar, L2MonsterInstance target, int seedId)
	{
		int basicSuccess = (L2Manor.getInstance().isAlternative(seedId) ? 20 : 90);
		int minlevelSeed = 0;
		int maxlevelSeed = 0;
		minlevelSeed = L2Manor.getInstance().getSeedMinLevel(seedId);
		maxlevelSeed = L2Manor.getInstance().getSeedMaxLevel(seedId);

		int levelPlayer = activeChar.getLevel(); // Attacker Level
		int levelTarget = target.getLevel(); // target Level

		// seed level
		if (levelTarget < minlevelSeed)
			basicSuccess -= 5 * (minlevelSeed - levelTarget);
		if (levelTarget > maxlevelSeed)
			basicSuccess -= 5 * (levelTarget - maxlevelSeed);

		// 5% decrease in chance if player level
		// is more than +/- 5 levels to _target's_ level
		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
			diff = -diff;
		if (diff > 5)
			basicSuccess -= 5 * (diff - 5);

		// chance can't be less than 1%
		if (basicSuccess < 1)
			basicSuccess = 1;

		int rate = Rnd.nextInt(10);

		return (rate < basicSuccess);
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}