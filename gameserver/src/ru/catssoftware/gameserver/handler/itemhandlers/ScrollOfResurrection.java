package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class ScrollOfResurrection implements IItemHandler
{
	// all the items ids that this handler knows
	private static final int[]	ITEM_IDS	= { 737, 3936, 3959, 6387, 9157};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return;
		}
		if (activeChar.isMovementDisabled())
			return;

		int itemId = item.getItemId();
		boolean humanScroll = (itemId == 3936 || itemId == 3959 || itemId == 737 || itemId == 9157);
		boolean petScroll = (itemId == 6387 || itemId == 737);

		// SoR Animation section
		L2Object object = activeChar.getTarget();
		if (object != null && object instanceof L2Character)
		{
			L2Character target = (L2Character) object;

			if (target.isDead())
			{
				L2PcInstance targetPlayer = null;

				if (target instanceof L2PcInstance)
					targetPlayer = (L2PcInstance) target;

				L2PetInstance targetPet = null;

				if (target instanceof L2PetInstance)
					targetPet = (L2PetInstance) target;

				if (targetPlayer != null || targetPet != null)
				{
					boolean condGood = true;

					//check target is not in a active siege zone
					Siege siege = null;
					FortSiege fsiege = null;
					if (targetPlayer != null)
					{
						siege = SiegeManager.getInstance().getSiege(targetPlayer);
						fsiege = FortSiegeManager.getInstance().getSiege(targetPlayer);
					}
					else
					{
						siege = SiegeManager.getInstance().getSiege(targetPet);
						fsiege = FortSiegeManager.getInstance().getSiege(targetPet);
					}

					if ((siege != null && siege.getIsInProgress())||(fsiege != null && fsiege.getIsInProgress()))
					{
						condGood = false;
						activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
					}

					siege = null;

					if (targetPet != null)
					{
						if (targetPet.getOwner().isPetReviveRequested())
						{
							activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
							condGood = false;
						}
						else if (!petScroll && targetPet.getOwner() != activeChar)
						{
							condGood = false;
							activeChar.sendPacket(SystemMessageId.INCORRECT_ITEM);
						}
					}
					else
					{
						if (targetPlayer != null && targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
						{
							condGood = false;
							activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_CANNOT_USE),"SOR"));
						}
						else if (targetPlayer != null && targetPlayer.isReviveRequested())
						{
							activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
							condGood = false;
						}
						else if (!humanScroll)
						{
							condGood = false;
							activeChar.sendPacket(SystemMessageId.INCORRECT_ITEM);
						}
					}

					if (condGood && !activeChar.isMuted())
					{
						if(!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
							return;

						int skillId = 0;
						int skillLevel = 1;

						switch (itemId)
						{
						case 737:
							skillId = 2014;
							break; // Scroll of Resurrection
						case 3936:
							skillId = 2049;
							break; // Blessed Scroll of Resurrection
						case 3959:
							skillId = 2062;
							break; // L2Day - Blessed Scroll of Resurrection
						case 6387:
							skillId = 2179;
							break; // Blessed Scroll of Resurrection: For Pets
						case 9157:
							skillId = 2321;
							break; // Blessed Scroll of Resurrection (Event)
						}

						if (skillId != 0)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
							activeChar.useMagic(skill, true, true);
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
							sm.addItemName(item);
							activeChar.sendPacket(sm);
						}
					}
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
		}
		else
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}