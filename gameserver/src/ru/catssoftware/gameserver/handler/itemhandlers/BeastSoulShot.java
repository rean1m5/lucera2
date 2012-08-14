package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2BabyPetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExAutoSoulShot;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.util.Broadcast;

/**
 * Beast SoulShot Handler
 *
 * @author Tempy
 */
public class BeastSoulShot implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static final int[]	ITEM_IDS	= { 6645 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean animation)
	{
		if (playable == null)
			return;

		L2PcInstance activeOwner = null;
		if (playable instanceof L2Summon)
		{
			activeOwner = ((L2Summon) playable).getOwner();
			activeOwner.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return;
		}
		else if (playable instanceof L2PcInstance)
			activeOwner = (L2PcInstance) playable;

		if (activeOwner == null)
			return;

		L2Summon activePet = activeOwner.getPet();

		if (activePet == null)
		{
			activeOwner.sendPacket(SystemMessageId.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return;
		}

		if (activePet.isDead())
		{
			activeOwner.sendPacket(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET);
			return;
		}

		int itemId = 6645;
		int shotConsumption = 1;
		L2ItemInstance weaponInst = null;
		L2Weapon weaponItem = null;

		if ((activePet instanceof L2PetInstance) && !(activePet instanceof L2BabyPetInstance))
		{
			weaponInst = activePet.getActiveWeaponInstance();
			weaponItem = activePet.getActiveWeaponItem();

			if (weaponInst == null)
			{
				activeOwner.sendPacket(SystemMessageId.CANNOT_USE_SOULSHOTS);
				return;
			}

			if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
				return; // SoulShots are already active.

			int shotCount = item.getCount();
			shotConsumption = weaponItem.getSoulShotCount();

			if (shotConsumption == 0)
			{
				activeOwner.sendPacket(SystemMessageId.CANNOT_USE_SOULSHOTS);
				return;
			}

			if (!(shotCount > shotConsumption))
			{
				// Not enough Soulshots to use.
				activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS_FOR_PET);
				return;
			}

			weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT,false);
		}
		else
		{
			if (activePet.getChargedSoulShot() != L2ItemInstance.CHARGED_NONE)
				return;

			activePet.setChargedSoulShot(L2ItemInstance.CHARGED_SOULSHOT);
		}

		// If the player doesn't have enough beast soulshot remaining, remove any auto soulshot task.
		if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false))
		{
			if (activeOwner.getAutoSoulShot().containsKey(itemId))
			{
				activeOwner.removeAutoSoulShot(itemId);
				activeOwner.sendPacket(new ExAutoSoulShot(itemId, 0));

				SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
				sm.addString(item.getItem().getName());
				activeOwner.sendPacket(sm);
				return;
			}

			activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS);
			return;
		}
		if (animation)
		{
			// Pet uses the power of spirit.
			activeOwner.sendPacket(SystemMessageId.PET_USE_THE_POWER_OF_SPIRIT);

			Broadcast.toSelfAndKnownPlayersInRadius(activeOwner, new MagicSkillUse(activePet, activePet, 2033, 1, 0, 0, false), 360000);
		}
	}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		useItem(playable,item,true);
	}
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}