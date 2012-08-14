package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExPutCommissionResultForVariationMake;
import ru.catssoftware.gameserver.templates.item.L2Item;

/**
 * Format:(ch) dddd
 * @author  -Wooden-
 */

public final class RequestConfirmGemStone extends L2GameClientPacket
{
	private static final String	_C__D0_2B_REQUESTCONFIRMGEMSTONE	= "[C] D0:2B RequestConfirmGemStone";
	private int					_targetItemObjId, _refinerItemObjId, _gemstoneItemObjId, _gemstoneCount;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
			return;

		L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		if (refinerItem == null)
			return;

		L2ItemInstance gemstoneItem = activeChar.getInventory().getItemByObjectId(_gemstoneItemObjId);
		if (gemstoneItem == null || gemstoneItem.getCount()<_gemstoneCount) {
			activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
			return;
		}

		int gemstoneItemId = gemstoneItem.getItem().getItemId();
		if (gemstoneItemId != 2130 && gemstoneItemId != 2131 && gemstoneItemId != 2132)
		{
			activeChar.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		int itemGrade = targetItem.getItem().getItemGrade();
		if (targetItem.getItem().getType2()==L2Item.TYPE2_WEAPON)
		{
			switch (itemGrade)
			{
				case L2Item.CRYSTAL_C:
					if (_gemstoneCount != 20 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
				case L2Item.CRYSTAL_B:
					if (_gemstoneCount != 30 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
				case L2Item.CRYSTAL_A:
					if (_gemstoneCount != 20 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
				case L2Item.CRYSTAL_S:
					if (_gemstoneCount != 25 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
			}
		}
		else
		{
			switch (itemGrade)
			{
				case L2Item.CRYSTAL_C:
					if (_gemstoneCount != 200 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
				case L2Item.CRYSTAL_B:
					if (_gemstoneCount != 300 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
				case L2Item.CRYSTAL_A:
					if (_gemstoneCount != 200 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
				case L2Item.CRYSTAL_S:
					if (_gemstoneCount != 250 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
						return;
					}
					break;
			}
		}
		activeChar.sendPacket(new ExPutCommissionResultForVariationMake(_gemstoneItemObjId, _gemstoneCount,gemstoneItemId));
		activeChar.sendPacket(SystemMessageId.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN);
	}

	@Override
	public String getType()
	{
		return _C__D0_2B_REQUESTCONFIRMGEMSTONE;
	}
}