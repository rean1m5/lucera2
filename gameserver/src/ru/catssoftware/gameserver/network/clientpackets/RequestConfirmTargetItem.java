package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExPutItemResultForVariationMake;
import ru.catssoftware.gameserver.templates.item.L2Item;

public final class RequestConfirmTargetItem extends L2GameClientPacket
{
	private static final String	_C__D0_29_REQUESTCONFIRMTARGETITEM	= "[C] D0:29 RequestConfirmTargetItem";
	private int					_itemObjId;

	@Override
	protected void readImpl()
	{
		_itemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_itemObjId);

		if (activeChar == null)
			return;
		if (item == null)
			return;

		int itemGrade = item.getItem().getItemGrade();
		int itemType = item.getItem().getType1();

		if (item == null)
			return;
		if (activeChar.getLevel() < 46)
		{
			activeChar.sendMessage("Вы должны иметь 46 уровень для улучшения предметов.");
			return;
		}
		if (item.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN);
			return;
		}
		else if (itemGrade < L2Item.CRYSTAL_C || itemType != L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE || !item.isDestroyable() || item.isShadowItem() || item.getItem().isCommonItem())
		{
			activeChar.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}
		if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return;
		}
		if (activeChar.isDead())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return;
		}
		if (activeChar.isParalyzed())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return;
		}
		if (activeChar.isFishing())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return;
		}
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN);
			return;
		}
		activeChar.sendPacket(new ExPutItemResultForVariationMake(_itemObjId));
		activeChar.sendPacket(SystemMessageId.SELECT_THE_CATALYST_FOR_AUGMENTATION);
	}

	@Override
	public String getType()
	{
		return _C__D0_29_REQUESTCONFIRMTARGETITEM;
	}
}