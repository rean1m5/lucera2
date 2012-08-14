package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.AbnormalEffect;

public class MysteryPotion implements IItemHandler
{
	private static final int[]	ITEM_IDS				= { 5234 };

	private static final int	MYSTERY_POTION_SKILL	= 2103;
	private static final int	EFFECT_DURATION			= 1200000;	// 20 mins
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;

		// Use a summon skill effect for fun ;)
		MagicSkillUse MSU = new MagicSkillUse(playable, playable, 2103, 1, 0, 0, false);
		activeChar.sendPacket(MSU);
		activeChar.broadcastPacket(MSU);

		activeChar.startAbnormalEffect(AbnormalEffect.BIG_HEAD);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

		SystemMessage sm = new SystemMessage(SystemMessageId.USE_S1);
		sm.addSkillName(MYSTERY_POTION_SKILL);
		activeChar.sendPacket(sm);

		MysteryPotionStop mp = new MysteryPotionStop(playable);
		ThreadPoolManager.getInstance().scheduleEffect(mp, EFFECT_DURATION);
	}

	public class MysteryPotionStop implements Runnable
	{
		private L2PlayableInstance	_playable;

		public MysteryPotionStop(L2PlayableInstance playable)
		{
			_playable = playable;
		}

		public void run()
		{
			try
			{
				if (!(_playable instanceof L2PcInstance))
					return;

				_playable.stopAbnormalEffect(AbnormalEffect.BIG_HEAD);
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