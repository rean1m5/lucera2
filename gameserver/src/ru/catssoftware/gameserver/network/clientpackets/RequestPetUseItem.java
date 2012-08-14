package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.PetDataTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.handler.ItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.PetItemList;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2ArmorType;
import ru.catssoftware.gameserver.templates.item.L2Item;


public class RequestPetUseItem extends L2GameClientPacket
{
	private static final String	_C__8A_REQUESTPETUSEITEM	= "[C] 8a RequestPetUseItem";

	private int					_objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		activeChar._bbsMultisell = 0;
		L2PetInstance pet = (L2PetInstance) activeChar.getPet();

		if (pet == null)
			return;

		L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);

		if (item == null)
			return;

		if (item.isWear())
			return;

		if (activeChar.isAlikeDead() || pet.isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			activeChar.sendPacket(sm);
			return;
		}

		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(pet, pet, true))
				return;
		}

		if (item.getItem().getBodyPart() == L2Item.SLOT_NECK)
		{
			if (item.getItem().getItemType() == L2ArmorType.PET)
			{
				useItem(pet, item, activeChar);
				return;
			}
		}

		//check if the item matches the pet
		if ((PetDataTable.isWolf(pet.getNpcId()) && item.getItem().isForWolf())
				|| (PetDataTable.isHatchling(pet.getNpcId()) && item.getItem().isForHatchling())
				|| (PetDataTable.isBaby(pet.getNpcId()) && item.getItem().isForBabyPet())
				|| (PetDataTable.isStrider(pet.getNpcId()) && item.getItem().isForStrider())
				|| (PetDataTable.isEvolvedWolf(pet.getNpcId()) && item.getItem().isForEvolvedWolf())
				|| (PetDataTable.isImprovedBaby(pet.getNpcId()) && item.getItem().isForBabyPet()))
		{
			useItem(pet, item, activeChar);
			return;
		}

		IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());

		if (handler != null)
			useItem(pet, item, activeChar);
		else
			activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
	}

	private synchronized void useItem(L2PetInstance pet, L2ItemInstance item, L2PcInstance activeChar)
	{
		if (item.isEquipable())
		{
			if (item.isEquipped())
			{
				pet.getInventory().unEquipItemInSlot(item.getLocationSlot());
				switch (item.getItem().getBodyPart())
				{
					case L2Item.SLOT_R_HAND:
						pet.setWeapon(0);
						break;
					case L2Item.SLOT_CHEST:
						pet.setArmor(0);
						break;
					case L2Item.SLOT_NECK:
						pet.setJewel(0);
						break;
				}
			}
			else
			{
				pet.getInventory().equipItem(item);
				switch (item.getItem().getBodyPart())
				{
					case L2Item.SLOT_R_HAND:
						pet.setWeapon(item.getItemId());
						break;
					case L2Item.SLOT_CHEST:
						pet.setArmor(item.getItemId());
						break;
					case L2Item.SLOT_NECK:
						pet.setJewel(item.getItemId());
						break;
				}
			}
			PetItemList pil = new PetItemList(pet);
			activeChar.sendPacket(pil);
			pet.broadcastFullInfo();
		}
		else
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());

			if (handler == null)
				_log.warn("no itemhandler registered for itemId:" + item.getItemId());
			else
			{
				handler.useItem(pet, item);
				pet.broadcastFullInfo();
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__8A_REQUESTPETUSEITEM;
	}
}