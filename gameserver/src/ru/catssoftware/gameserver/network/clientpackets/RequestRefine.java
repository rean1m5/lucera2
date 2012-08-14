package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.xml.AugmentationData;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExVariationResult;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;

public final class RequestRefine extends L2GameClientPacket
{
	private static final String	_C__D0_2C_REQUESTREFINE	= "[C] D0:2C RequestRefine";
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
		activeChar._bbsMultisell = 0;
		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		L2ItemInstance gemstoneItem = activeChar.getInventory().getItemByObjectId(_gemstoneItemObjId);

		if (targetItem == null || refinerItem == null || gemstoneItem == null || targetItem.getOwnerId() != activeChar.getObjectId()
				|| refinerItem.getOwnerId() != activeChar.getObjectId() || gemstoneItem.getOwnerId() != activeChar.getObjectId() || activeChar.getLevel() < 46) // must be lvl 46
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}

		// unequip item
		if (targetItem.isEquipped())
			activeChar.disarmWeapons();

		if (tryAugmentItem(activeChar, targetItem, refinerItem, gemstoneItem))
		{
			int stat12 = 0x0000FFFF & targetItem.getAugmentation().getAugmentationId();
			int stat34 = targetItem.getAugmentation().getAugmentationId() >> 16;
			activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
			activeChar.sendPacket(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED);
		}
		else
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
		}
	}

	private boolean tryAugmentItem(L2PcInstance player, L2ItemInstance targetItem, L2ItemInstance refinerItem, L2ItemInstance gemstoneItem)
	{
		if (targetItem.isAugmented() || targetItem.isWear())
			return false;

		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return false;
		}
		if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN);
			return false;
		}
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return false;
		}
		if (player.isParalyzed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return false;
		}
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_TRADING);
			return false;
		}
		if (player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return false;
		}
		if (player.getInventory().getItemByObjectId(refinerItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong LifeStone-id.", Config.DEFAULT_PUNISH);
			return false;
		}
		if (player.getInventory().getItemByObjectId(targetItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Weapon-id.", Config.DEFAULT_PUNISH);
			return false;
		}
		if (player.getInventory().getItemByObjectId(gemstoneItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Gemstone-id.", Config.DEFAULT_PUNISH);
			return false;
		}

		int itemGrade = targetItem.getItem().getItemGrade();
		int lifeStoneId = refinerItem.getItemId();
		int gemstoneItemId = gemstoneItem.getItemId();

		// is the refiner Item a life stone?
		if (lifeStoneId < 8723 || lifeStoneId > 8762)
			return false;

		// must be a weapon or ACCESSORY, must be > d grade
		if (itemGrade < L2Item.CRYSTAL_C || targetItem.getItem().getType2() != L2Item.TYPE2_WEAPON || !targetItem.isDestroyable() || targetItem.isShadowItem())
		{
			return false;
		}

		int modifyGemstoneCount = _gemstoneCount;
		int lifeStoneGrade = 0;//normal grade
		int lifeStoneLevel;

		lifeStoneLevel = getLifeStoneLevel(lifeStoneId);
		lifeStoneGrade = getLifeStoneGrade(lifeStoneId);

		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				if (player.getLevel() < 46 || gemstoneItemId != 2130)
					return false;
				modifyGemstoneCount = 20;
				break;
			case L2Item.CRYSTAL_B:
				if (player.getLevel() < 52 || gemstoneItemId != 2130  || lifeStoneLevel < 3)
					return false;
				modifyGemstoneCount = 30;
				break;
			case L2Item.CRYSTAL_A:
				if (player.getLevel() < 61 || gemstoneItemId != 2131 || lifeStoneLevel < 6)
					return false;
				modifyGemstoneCount = 20;
				break;
			case L2Item.CRYSTAL_S:
				if (player.getLevel() < 76 || gemstoneItemId != 2131 || lifeStoneLevel < 10)
					return false;
				modifyGemstoneCount = 25;
				break;
		}

		// check if the lifestone is appropriate for this player
		switch (lifeStoneLevel)
		{
			case 1:
				if (player.getLevel() < 46)
					return false;
				break;
			case 2:
				if (player.getLevel() < 49)
					return false;
				break;
			case 3:
				if (player.getLevel() < 52)
					return false;
				break;
			case 4:
				if (player.getLevel() < 55)
					return false;
				break;
			case 5:
				if (player.getLevel() < 58)
					return false;
				break;
			case 6:
				if (player.getLevel() < 61)
					return false;
				break;
			case 7:
				if (player.getLevel() < 64)
					return false;
				break;
			case 8:
				if (player.getLevel() < 67)
					return false;
				break;
			case 9:
				if (player.getLevel() < 70)
					return false;
				break;
			case 10:
				if (player.getLevel() < 76)
					return false;
				break;
		}

		// consume the life stone
		if (!player.destroyItem("RequestRefine", refinerItem, 1, null, false))
			return false;

		// consume the gemstones
		if (!player.destroyItem("RequestRefine", gemstoneItem, modifyGemstoneCount, null, false))
			return false;

		// generate augmentation
		targetItem.setAugmentation(AugmentationData.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade));

		// finish and send the inventory update packet
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);

		return true;
	}

	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
		if (itemId < 10)
			return 0; // normal grade
		if (itemId < 20)
			return 1; // mid grade
		if (itemId < 30)
			return 2; // high grade
		return 3; // top grade
	}

	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		return itemId;
	}

	@Override
	public String getType()
	{
		return _C__D0_2C_REQUESTREFINE;
	}
}
