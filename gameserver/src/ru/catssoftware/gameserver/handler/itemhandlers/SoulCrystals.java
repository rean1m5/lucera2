package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;

public class SoulCrystals implements IItemHandler
{
	private static final int[]	ITEM_IDS	=
	{
			4629,
			4630,
			4631,
			4632,
			4633,
			4634,
			4635,
			4636,
			4637,
			4638,
			4639,
			5577,
			5580,
			5908,
			9570,
			4640,
			4641,
			4642,
			4643,
			4644,
			4645,
			4646,
			4647,
			4648,
			4649,
			4650,
			5578,
			5581,
			5911,
			9572,
			4651,
			4652,
			4653,
			4654,
			4655,
			4656,
			4657,
			4658,
			4659,
			4660,
			4661,
			5579,
			5582,
			5914
	};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par)
	{
	}


	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Object target = activeChar.getTarget();
		if (!(target instanceof L2MonsterInstance))
		{
			// Send a System Message to the caster
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// u can use soul crystal only when target hp goes below <50%
		if (((L2MonsterInstance) target).getStatus().getCurrentHp() > ((L2MonsterInstance) target).getMaxHp() / 2.0)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int crystalId = item.getItemId();

		// Soul Crystal Casting section
		L2Skill skill = SkillTable.getInstance().getInfo(2096, 1);
		activeChar.useMagic(skill, false, true);
		CrystalFinalizer cf = new CrystalFinalizer(activeChar, target, crystalId);
		ThreadPoolManager.getInstance().scheduleEffect(cf, skill.getHitTime());

	}

	static class CrystalFinalizer implements Runnable
	{
		private L2PcInstance	_activeChar;
		private L2Attackable	_target;
		private int				_crystalId;

		CrystalFinalizer(L2PcInstance activeChar, L2Object target, int crystalId)
		{
			_activeChar = activeChar;
			_target = (L2Attackable) target;
			_crystalId = crystalId;
		}

		public void run()
		{
			if (_activeChar.isDead() || _target.isDead())
				return;
			_activeChar.enableAllSkills();
			try
			{
				_target.addAbsorber(_activeChar, _crystalId);
				_activeChar.setTarget(_target);
			}
			catch (Exception e)
			{
				_log.error(e.getMessage(), e);
			}
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}