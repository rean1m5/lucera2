package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExPutEnchantTargetItemResult;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.tools.random.Rnd;

public class RequestEnchantItem extends L2GameClientPacket
{
	private static final String	_C__58_REQUESTENCHANTITEM	= "[C] 58 RequestEnchantItem";
	private static final int[] CRYSTAL_SCROLLS ={
			731, 732, 949, 950, 953, 954, 957, 958, 961, 962
	};

	private static final int[] NORMAL_WEAPON_SCROLLS ={
			729, 947, 951, 955, 959
	};

	private static final int[] BLESSED_WEAPON_SCROLLS ={
			6569, 6571, 6573, 6575, 6577
	};

	private static final int[] CRYSTAL_WEAPON_SCROLLS ={
			731, 949, 953, 957, 961
	};

	private static final int[] NORMAL_ARMOR_SCROLLS ={
			730, 948, 952, 956, 960
	};

	private static final int[] BLESSED_ARMOR_SCROLLS ={
			6570, 6572, 6574, 6576, 6578
	};

	private static final int[] CRYSTAL_ARMOR_SCROLLS ={
			732, 950, 954, 958, 962
	};

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

		if (activeChar == null || _objectId == 0)
			return;
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		L2ItemInstance scroll = activeChar.getActiveEnchantItem();
		if (activeChar.isOnline() == 0)
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}
		if(activeChar.isOlympiadStart()) {
			activeChar.setActiveEnchantItem(null);
			ActionFailed();
			return;
			
		}
		if (activeChar.isDead())
		{
			activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
			activeChar.setActiveEnchantItem(null);
			return;
		}		
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_ENCHANT && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			activeChar.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			return;
		}
		if(Config.ENCHAT_TIME > 0 && !FloodProtector.tryPerformAction(activeChar, Protected.ENCHANT)) {
			activeChar.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			activeChar.setActiveEnchantItem(null);
			return;
		}
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.cancelActiveTrade();
			activeChar.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			return;
		}
		if (activeChar.isProcessingTransaction())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			activeChar.setActiveEnchantItem(null);
			return;
		}
		if (activeChar.getPrivateStoreType() != 0 || activeChar.getTrading())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			return;
		}
		if (item == null || scroll == null || activeChar.getInventory().getItemByObjectId(scroll.getObjectId())!=scroll)
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}
		if ((item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY) && (item.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL))
			return;
		if (item.isWear())
		{
			Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался точить свадебный подарок", IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		SystemMessage sm;
		if (item.getItem().getItemType() == L2WeaponType.ROD || item.isShadowItem() || (!Config.ENCHANT_HERO_WEAPONS && item.isHeroItem())
				|| (item.getItemId() >= 7816 && item.getItemId() <= 7831) || item.getItem().isCommonItem())
		{
			activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
			activeChar.setActiveEnchantItem(null);
			return;
		}
		try {
		int itemType2 = item.getItem().getType2();
		boolean enchantItem = false;
		boolean enchantBreak = true;
		int crystalId = 0;

		/** pretty code ;D */
		switch (item.getItem().getCrystalType())
		{
			case L2Item.CRYSTAL_A:
				crystalId = 1461;
				switch (scroll.getItemId())
				{
					case 729:
					case 731:
					case 6569:
						if (itemType2 == L2Item.TYPE2_WEAPON)
							enchantItem = true;
						break;
					case 730:
					case 732:
					case 6570:
						if ((itemType2 == L2Item.TYPE2_SHIELD_ARMOR) || (itemType2 == L2Item.TYPE2_ACCESSORY))
							enchantItem = true;
						break;
				}
				break;
			case L2Item.CRYSTAL_B:
				crystalId = 1460;
				switch (scroll.getItemId())
				{
					case 947:
					case 949:
					case 6571:
						if (itemType2 == L2Item.TYPE2_WEAPON)
							enchantItem = true;
						break;
					case 948:
					case 950:
					case 6572:
						if ((itemType2 == L2Item.TYPE2_SHIELD_ARMOR) || (itemType2 == L2Item.TYPE2_ACCESSORY))
							enchantItem = true;
						break;
				}
				break;
			case L2Item.CRYSTAL_C:
				crystalId = 1459;
				switch (scroll.getItemId())
				{
					case 951:
					case 953:
					case 6573:
						if (itemType2 == L2Item.TYPE2_WEAPON)
							enchantItem = true;
						break;
					case 952:
					case 954:
					case 6574:
						if ((itemType2 == L2Item.TYPE2_SHIELD_ARMOR) || (itemType2 == L2Item.TYPE2_ACCESSORY))
							enchantItem = true;
						break;
				}
				break;
			case L2Item.CRYSTAL_D:
				crystalId = 1458;
				switch (scroll.getItemId())
				{
					case 955:
					case 957:
					case 6575:
						if (itemType2 == L2Item.TYPE2_WEAPON)
							enchantItem = true;
						break;
					case 956:
					case 958:
					case 6576:
						if ((itemType2 == L2Item.TYPE2_SHIELD_ARMOR) || (itemType2 == L2Item.TYPE2_ACCESSORY))
							enchantItem = true;
						break;
				}
				break;
			case L2Item.CRYSTAL_S:
				crystalId = 1462;
				switch (scroll.getItemId())
				{
					case 959:
					case 961:
					case 6577:
						if (itemType2 == L2Item.TYPE2_WEAPON)
							enchantItem = true;
						break;
					case 960:
					case 962:
					case 6578:
						if ((itemType2 == L2Item.TYPE2_SHIELD_ARMOR) || (itemType2 == L2Item.TYPE2_ACCESSORY))
							enchantItem = true;
						break;
				}
				break;
		}

		if (!enchantItem)
		{
			activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if(scroll.getItemId() >= 6569 && scroll.getItemId() <= 6578)
		{
			enchantBreak = false;
		}
		else
		{
			for(int crystalscroll : CRYSTAL_SCROLLS)
				if(scroll.getItemId() == crystalscroll)
				{
					enchantBreak = false;
					break;
				}
		}

		int chance = 0;
		int maxEnchantLevel = 0;

		if(item.getItem().getType2() == L2Item.TYPE2_WEAPON)
		{
			for(int normalweaponscroll : NORMAL_WEAPON_SCROLLS)
			{
				if(scroll.getItemId() == normalweaponscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.NORMAL_WEAPON_ENCHANT_LEVEL.size())
					{
						chance = Config.NORMAL_WEAPON_ENCHANT_LEVEL.get(Config.NORMAL_WEAPON_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.NORMAL_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_WEAPON<=0?65535:Config.ENCHANT_MAX_WEAPON;
				}
			}
			for(int blessedweaponscroll : BLESSED_WEAPON_SCROLLS)
			{
				if(scroll.getItemId() == blessedweaponscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.BLESS_WEAPON_ENCHANT_LEVEL.size())
					{
						chance = Config.BLESS_WEAPON_ENCHANT_LEVEL.get(Config.BLESS_WEAPON_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.BLESS_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_WEAPON<=0?65535:Config.ENCHANT_MAX_WEAPON;
				}
			}
			for(int crystalweaponscroll : CRYSTAL_WEAPON_SCROLLS)
			{
				if(scroll.getItemId() == crystalweaponscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.CRYTAL_WEAPON_ENCHANT_LEVEL.size())
					{
						chance = Config.CRYTAL_WEAPON_ENCHANT_LEVEL.get(Config.CRYTAL_WEAPON_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.CRYTAL_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_WEAPON<=0?65535:Config.ENCHANT_MAX_WEAPON;
				}
			}
		}
		else if(item.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR)
		{
			for(int normalarmorscroll : NORMAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == normalarmorscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.NORMAL_ARMOR_ENCHANT_LEVEL.size())
					{
						chance = Config.NORMAL_ARMOR_ENCHANT_LEVEL.get(Config.NORMAL_ARMOR_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.NORMAL_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_ARMOR<=0?65535:Config.ENCHANT_MAX_ARMOR;
				}
			}
			for(int blessedarmorscroll : BLESSED_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == blessedarmorscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.BLESS_ARMOR_ENCHANT_LEVEL.size())
					{
						chance = Config.BLESS_ARMOR_ENCHANT_LEVEL.get(Config.BLESS_ARMOR_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.BLESS_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_ARMOR<=0?65535:Config.ENCHANT_MAX_ARMOR;
				}
			}
			for(int crystalarmorscroll : CRYSTAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == crystalarmorscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size())
					{
						chance = Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.get(Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_ARMOR<=0?65535:Config.ENCHANT_MAX_ARMOR;
				}
			}
		}
		else if(item.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
		{
			for(int normaljewelscroll : NORMAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == normaljewelscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size())
					{
						chance = Config.NORMAL_JEWELRY_ENCHANT_LEVEL.get(Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.NORMAL_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_JEWELRY<=0?65535:Config.ENCHANT_MAX_JEWELRY;
				}
			}
			for(int blessedjewelscroll : BLESSED_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == blessedjewelscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.BLESS_JEWELRY_ENCHANT_LEVEL.size())
					{
						chance = Config.BLESS_JEWELRY_ENCHANT_LEVEL.get(Config.BLESS_JEWELRY_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.BLESS_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_JEWELRY<=0?65535:Config.ENCHANT_MAX_JEWELRY;
				}
			}
			for(int crystaljewelscroll : CRYSTAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == crystaljewelscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size())
					{
						chance = Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.get(Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_MAX_JEWELRY<=0?65535:Config.ENCHANT_MAX_JEWELRY;
				}
			}
		}

		if (item.getEnchantLevel()+1 >= maxEnchantLevel)
		{
			activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
			activeChar.setActiveEnchantItem(null);
			return;
		}

		scroll = activeChar.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, activeChar, item);
		if (scroll == null)
		{
			activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался точить вещь, не имея точки в руках",
					Config.DEFAULT_PUNISH);
			activeChar.setActiveEnchantItem(null);
			return;
		}
		activeChar.getInventory().updateInventory(scroll);

		if (item.getEnchantLevel() < Config.ENCHANT_SAFE_MAX
				|| (item.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && item.getEnchantLevel() < Config.ENCHANT_SAFE_MAX_FULL))
			chance = 100;

		else if (activeChar.getRace() == Race.Dwarf && Config.ENCHANT_DWARF_SYSTEM)
		{
			int _charlevel = activeChar.getLevel();
			int _itemlevel = item.getEnchantLevel();
			if (_charlevel >= 20 && _itemlevel <= Config.ENCHANT_DWARF_1_ENCHANTLEVEL)
				chance = chance + Config.ENCHANT_DWARF_1_CHANCE;
			else if (_charlevel >= 40 && _itemlevel <= Config.ENCHANT_DWARF_2_ENCHANTLEVEL)
				chance = chance + Config.ENCHANT_DWARF_2_CHANCE;
			else if (_charlevel >= 76 && _itemlevel <= Config.ENCHANT_DWARF_3_ENCHANTLEVEL)
				chance = chance + Config.ENCHANT_DWARF_3_CHANCE;
		}
		if (item.isWeapon() && item.getWeaponItem().isMagic())
		{
			chance*=Config.ENCHANT_MAGIC_WEAPON_CHANCE;
			maxEnchantLevel = Config.ENCHANT_SAFE_MAX_MAGIC_WEAPON;
		}

		switch (item.getLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
				switch (item.getLocation())
				{
				case VOID:
				case PET:
				case WAREHOUSE:
				case CLANWH:
				case LEASE:
				case FREIGHT:
				case NPC:
					chance = 0;
					activeChar.setActiveEnchantItem(null);
					Util.handleIllegalPlayerAction(activeChar, "Warning!! Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
					return;
				}
				if (item.getOwnerId() != activeChar.getObjectId())
				{
					activeChar.setActiveEnchantItem(null);
					activeChar.sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
					return;
				}
				break;
			default:
				chance = 0;
				activeChar.setActiveEnchantItem(null);
				Util.handleIllegalPlayerAction(activeChar, "Warning!! Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
				return;
		}

		Integer iChance = (Integer)GameExtensionManager.getInstance().handleAction(item, Action.ITEM_ENCHANTCALCCHANCE,scroll,chance);
		if(iChance!=null)
			chance = iChance;
		boolean failed = false;
		if (Rnd.get(100) < chance)
		{
			GameExtensionManager.getInstance().handleAction(item,Action.ITEM_ENCHANTSUCCESS,activeChar);
			synchronized (item)
			{
				if (item.getOwnerId() != activeChar.getObjectId()) // has just lost the item
				{
					activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
					activeChar.setActiveEnchantItem(null);
					activeChar.sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
					return;
				}
				if(item.getEnchantLevel() == 0)
				{
					sm = new SystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED);
					sm.addItemName(item.getItemId());
					activeChar.sendPacket(sm);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item.getItemId());
					activeChar.sendPacket(sm);
				}
				item.setEnchantLevel(item.getEnchantLevel() + 1);
				item.setLastChange(L2ItemInstance.MODIFIED);
			}
		}
		else
		{
			
			failed = true;
			if (enchantBreak)
			{
				if(GameExtensionManager.getInstance().handleAction(item, ObjectExtension.Action.ITEM_ENCHANTFAIL)!=null) {
					activeChar.getInventory().updateInventory(item);
					activeChar.sendPacket(new UserInfo(activeChar));
					activeChar.broadcastUserInfo(true);
					activeChar.setActiveEnchantItem(null);
					return;
					
				}

				if (item.isEquipped())
				{
					if (item.getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
						sm.addNumber(item.getEnchantLevel());
						sm.addItemName(item);
						activeChar.sendPacket(sm);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_DISARMED);
						sm.addItemName(item);
						activeChar.sendPacket(sm);
					}

					L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
					InventoryUpdate iu = new InventoryUpdate();
					for (L2ItemInstance element : unequiped)
						iu.addItem(element);
					activeChar.sendPacket(iu);
				}

				int count = item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2;
				if (count < 1)
					count = 1;

				L2ItemInstance destroyItem = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
				if (destroyItem == null)
				{
					if (item.getLocation() != null)
						activeChar.getWarehouse().destroyItem("Enchant", item, activeChar, null);

					activeChar.setActiveEnchantItem(null);
					activeChar.sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
					return;
				}
				if(item.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_S2_EVAPORATED);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item.getItemId());
					activeChar.sendPacket(sm);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_EVAPORATED);
					sm.addItemName(item.getItemId());
					activeChar.sendPacket(sm);
				}
				L2World.getInstance().removeObject(destroyItem);

				L2ItemInstance crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyItem);
				sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(crystals);
				sm.addNumber(count);
				activeChar.sendPacket(sm);
				activeChar.getInventory().updateInventory(crystals);
				activeChar.sendPacket(new ExPutEnchantTargetItemResult(1, crystalId, count));
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.BLESSED_ENCHANT_FAILED);
				activeChar.sendPacket(sm);
				if (Config.ALT_FAILED_ENC_LEVEL)
					item.setEnchantLevel(Config.ENCHANT_SAFE_MAX);
				else
					item.setEnchantLevel(0);
				item.setLastChange(L2ItemInstance.MODIFIED);
				activeChar.sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
			}
		}
		sm = null;
		if (!failed)
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(0, 0 ,0));
		activeChar.getInventory().updateInventory(item);
		activeChar.sendPacket(new UserInfo(activeChar));
		activeChar.broadcastUserInfo(true);
		activeChar.setActiveEnchantItem(null);
		} finally {
			activeChar.getInventory().updateDatabase();
		}
	}

	@Override
	public String getType()
	{
		return _C__58_REQUESTENCHANTITEM;
	}
}
