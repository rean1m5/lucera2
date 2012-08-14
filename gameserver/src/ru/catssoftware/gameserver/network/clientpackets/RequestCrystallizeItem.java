package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.PcInventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;

public class RequestCrystallizeItem extends L2GameClientPacket
{
	private static final String	_C__72_REQUESTDCRYSTALLIZEITEM	= "[C] 72 RequestCrystallizeItem";
	private int					_objectId, _count;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_CREATEITEM && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			activeChar.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}

		if (_count <= 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался кристализировать предмет, кол-во кристалов < 0!", IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		if (activeChar.getPrivateStoreType() != 0 || activeChar.isInCrystallize())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}

		int skillLevel = activeChar.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
		if (skillLevel <= 0)
		{
			activeChar.sendPacket(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW);
			ActionFailed();
			return;
		}

		PcInventory inventory = activeChar.getInventory();
		if (inventory != null)
		{
			L2ItemInstance item = inventory.getItemByObjectId(_objectId);

			if (item == null || item.isWear())
			{
				ActionFailed();
				return;
			}
			if ((item).isHeroItem())
				return;
			if (_count > item.getCount())
				_count = activeChar.getInventory().getItemByObjectId(_objectId).getCount();
		}

		L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);
		if (itemToRemove == null || itemToRemove.isWear() || itemToRemove.isShadowItem())
			return;

		if (!itemToRemove.getItem().isCrystallizable() || (itemToRemove.getItem().getCrystalCount() <= 0) || (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_NONE))
			return;

		boolean canCrystallize = true;
		switch (itemToRemove.getItem().getCrystalType())
		{
			case L2Item.CRYSTAL_C:
			{
				if (skillLevel <= 1)
					canCrystallize = false;
				break;
			}
			case L2Item.CRYSTAL_B:
			{
				if (skillLevel <= 2)
					canCrystallize = false;
				break;
			}
			case L2Item.CRYSTAL_A:
			{
				if (skillLevel <= 3)
					canCrystallize = false;
				break;
			}
			case L2Item.CRYSTAL_S:
			{
				if (skillLevel <= 4)
					canCrystallize = false;
				break;
			}
		}

		if (!canCrystallize)
		{
			activeChar.sendPacket(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW);
			ActionFailed();
			return;
		}

		activeChar.setInCrystallize(true);

		//unequip if needed
		if (itemToRemove.isEquipped())
		{
			L2ItemInstance[] unequiped = inventory.unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
				iu.addModifiedItem(element);
			activeChar.sendPacket(iu);
			SystemMessage sm;
			if (itemToRemove.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(itemToRemove.getEnchantLevel());
				sm.addItemName(itemToRemove);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(itemToRemove);
			}
			activeChar.sendPacket(sm);
		}

		// remove from inventory
		L2ItemInstance removedItem = inventory.destroyItem("Crystalize", _objectId, _count, activeChar, null);
		if (removedItem == null)
		{
			activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			ActionFailed();
			return;
		}
		

		// add crystals
		int crystalId = itemToRemove.getItem().getCrystalItemId();
		int crystalAmount = itemToRemove.getCrystalCount();
		L2ItemInstance createditem = inventory.addItem("Crystalize", crystalId, crystalAmount, activeChar, activeChar);
		activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CRYSTALLIZED).addItemName(removedItem));
		
		SystemMessage sm;
		sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(createditem);
		sm.addNumber(crystalAmount);
		activeChar.sendPacket(sm);
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		iu.addNewItem(createditem);
		activeChar.sendPacket(iu);

		L2World.getInstance().removeObject(removedItem);
		activeChar.broadcastUserInfo();
		activeChar.setInCrystallize(false);
	}

	@Override
	public String getType()
	{
		return _C__72_REQUESTDCRYSTALLIZEITEM;
	}
}