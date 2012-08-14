package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;
import ru.catssoftware.gameserver.templates.item.L2Item;

public final class RequestConfirmCancelItem extends L2GameClientPacket
{
	private static final String	_C__D0_2D_REQUESTCONFIRMCANCELITEM	= "[C] D0:2D RequestConfirmCancelItem";

	private int					_itemId;

	@Override
	protected void readImpl()
	{
		_itemId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_itemId);

		if (item == null)
			return;

		if (!item.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}

		int price = 0;
		switch (item.getItem().getItemGrade())
		{
			case L2Item.CRYSTAL_C:
				if (item.getCrystalCount() < 1720)
					price = 95000;
				else if (item.getCrystalCount() < 2452)
					price = 150000;
				else
					price = 210000;
				break;
			case L2Item.CRYSTAL_B:
				if (item.getCrystalCount() < 1746)
					price = 240000;
				else
					price = 270000;
				break;
			case L2Item.CRYSTAL_A:
				if (item.getCrystalCount() < 2160)
					price = 330000;
				else if (item.getCrystalCount() < 2824)
					price = 390000;
				else
					price = 420000;
				break;
			case L2Item.CRYSTAL_S:
				price = 480000;
				break;
			default:
				return;
		}
		activeChar.sendPacket(new ExPutItemResultForVariationCancel(_itemId, price));
	}

	@Override
	public String getType()
	{
		return _C__D0_2D_REQUESTCONFIRMCANCELITEM;
	}
}
