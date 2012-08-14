package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExVariationCancelResult;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;

public final class RequestRefineCancel extends L2GameClientPacket
{
	private static final String	_C__D0_2E_REQUESTREFINECANCEL	= "[C] D0:2E RequestRefineCancel";
	private int					_targetItemObjId;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		// cannot remove augmentation from a not augmented item
		if (!targetItem.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		// get the price
		int price = 0;
		switch (targetItem.getItem().getItemGrade())
		{
		case L2Item.CRYSTAL_C:
			if (targetItem.getCrystalCount() < 1720)
				price = 95000;
			else if (targetItem.getCrystalCount() < 2452)
				price = 150000;
			else
				price = 210000;
			break;
		case L2Item.CRYSTAL_B:
			if (targetItem.getCrystalCount() < 1746)
				price = 240000;
			else
				price = 270000;
			break;
		case L2Item.CRYSTAL_A:
			if (targetItem.getCrystalCount() < 2160)
				price = 330000;
			else if (targetItem.getCrystalCount() < 2824)
				price = 390000;
			else
				price = 420000;
			break;
		case L2Item.CRYSTAL_S:
			price = 480000;
			break;
		default:
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		if (!activeChar.reduceAdena("RequestRefineCancel", price, null, true))
			return;
		if (targetItem.isEquipped())
			activeChar.disarmWeapons();

		targetItem.removeAugmentation();
		activeChar.sendPacket(new ExVariationCancelResult(1));
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);
		SystemMessage sm = new SystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addString(targetItem.getItemName());
		activeChar.sendPacket(sm);
	}

	@Override
	public String getType()
	{
		return _C__D0_2E_REQUESTREFINECANCEL;
	}
}