package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.handler.ItemHandler;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.ShowCalculator;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Armor;
import ru.catssoftware.gameserver.templates.item.L2ArmorType;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.lang.L2System;
import ru.catssoftware.lang.RunnableImpl;

public final class UseItem extends L2GameClientPacket
{
	private static final String	_C__14_USEITEM	= "[C] 14 UseItem";

	private int					_objectId;

	/** Weapon Equip Task */
	public class WeaponEquipTask extends RunnableImpl
	{
		L2ItemInstance	item;
		L2PcInstance	activeChar;

		public WeaponEquipTask(L2ItemInstance it, L2PcInstance character)
		{
			item = it;
			activeChar = character;
		}

		@Override
		public void runImpl()
		{
			//If character is still engaged in strike we should not change weapon
			if (activeChar.isAttackingNow())
				return;
			// Equip or unEquip
			activeChar.useEquippableItem(item, false);
		}
	}

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

		if (activeChar.isOutOfControl())
			return;
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessageId.NOT_USE_ITEMS_IN_PRIVATE_STORE);
			ActionFailed();
			return;
		}

		if (activeChar.getActiveTradeList() != null)
			activeChar.cancelActiveTrade();

		activeChar._inWorld = true;
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;
		if(activeChar._lastUseItem == item.getItemId()) {
			if(!FloodProtector.tryPerformAction(activeChar, Protected.USEITEM)) {
				ActionFailed();
				return;
			}
		}
		
		activeChar._lastUseItem = item.getItemId();
		
		if(GameExtensionManager.getInstance().handleAction(item, Action.ITEM_USE)!=null)
			return;
		
		if(activeChar.getGameEvent()!=null && activeChar.getGameEvent().isRunning() && !activeChar.getGameEvent().canUseItem(activeChar, item))
			return;

		if (item.isWear())
		{
			// No unequipping wear-items
			return;
		}

		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
			return;
		}

		int itemId = item.getItemId();
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT
				&& activeChar.getKarma() > 0
				&& (itemId == 736 || itemId == 1538 || itemId == 1829 || itemId == 1830 || itemId == 3958 || itemId == 5858 || itemId == 5859 || itemId == 6663
						|| itemId == 6664 || (itemId >= 7117 && itemId <= 7135) || (itemId >= 7554 && itemId <= 7559) || itemId == 7618 || itemId == 7619))
			return;

		// Items that cannot be used
		if (itemId == 57)
			return;

		if (activeChar.isFishing() && (itemId < 6535 || itemId > 6540))
		{
			// You cannot do anything else while fishing
			getClient().getActiveChar().sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			return;
		}

		// Char cannot use item when dead
		if (activeChar.isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			getClient().getActiveChar().sendPacket(sm);
			sm = null;
			return;
		}

		if (Config.CHECK_ENCHANT_LEVEL_EQUIP)
		{
			if(!activeChar.isGM())
			{
				if(Config.ENCHANT_MAX_WEAPON >0 && item.getItem().getType2() == L2Item.TYPE2_WEAPON && item.getEnchantLevel() > Config.ENCHANT_MAX_WEAPON)
				{
					Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался одеть переточеную вещь!", IllegalPlayerAction.PUNISH_KICK);
					return;
				}
				if(Config.ENCHANT_MAX_ARMOR > 0 && item.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR && item.getEnchantLevel() > Config.ENCHANT_MAX_ARMOR)
				{
					Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался одеть переточеную вещь!", IllegalPlayerAction.PUNISH_KICK);
					return;
				}
				if(Config.ENCHANT_MAX_JEWELRY > 0 && item.getItem().getType2() == L2Item.TYPE2_ACCESSORY && item.getEnchantLevel() > Config.ENCHANT_MAX_JEWELRY)
				{
					Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался одеть переточеную вещь!", IllegalPlayerAction.PUNISH_KICK);
					return;
				}
			}
		}
		// Char cannot use pet items
		if ((item.getItem() instanceof L2Armor && item.getItem().getItemType() == L2ArmorType.PET) || (item.getItem() instanceof L2Weapon && item.getItem().getItemType() == L2WeaponType.PET))
		{
			getClient().getActiveChar().sendPacket(SystemMessageId.CANNOT_EQUIP_PET_ITEM);
			return;
		}

		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(activeChar, activeChar, true))
				return;
		}

		if (activeChar.isInOlympiadMode()) { 
			if(item.isHeroItem() || item.isOlyRestrictedItem())
			{
				activeChar.sendPacket(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT);
				activeChar.actionFail();
				return;
			}
		}

		if (item.isEquipable())
		{
			// No unequipping/equipping while the player is in special conditions
			if (activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead())
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addItemName(item);
				getClient().getActiveChar().sendPacket(sm);
				sm = null;
				return;
			}

			// Don't allow hero equipment and restricted items during Olympiad

			if (!activeChar.isGM() && !activeChar.isHero() && item.isHeroItem())
				return;
			if(item.getItem() instanceof L2Armor && !item.isEquipped()) {
				if(!Config.isAllowArmor(activeChar, (L2Armor)item.getItem())) {
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
					sm.addItemName(item);
					sendPacket(sm);
					ActionFailed();
					return;
				}
			}
			switch (item.getItem().getBodyPart())
			{
			case L2Item.SLOT_LR_HAND:
			case L2Item.SLOT_L_HAND:
			case L2Item.SLOT_R_HAND:
			{
				// prevent players to equip weapon while wearing combat flag
				if (activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemId() == Config.FORTSIEGE_COMBAT_FLAG_ID)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NO_CONDITION_TO_EQUIP));
					return;
				}
				// Prevent player to remove the weapon on special conditions
				if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC);
					return;
				}

				if (activeChar.isMounted())
				{
					activeChar.sendPacket(SystemMessageId.NO_CONDITION_TO_EQUIP);
					return;
				}

				if (activeChar.isDisarmed())
				{
					activeChar.sendPacket(SystemMessageId.NO_CONDITION_TO_EQUIP);
					return;
				}

				// Don't allow weapon/shield equipment if a cursed weapon is equiped
				if (activeChar.isCursedWeaponEquipped())
					return;

				// Don't allow other Race to Wear Kamael exclusive Weapons.
				break;
			}
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_BACK:
			case L2Item.SLOT_GLOVES:
			case L2Item.SLOT_FEET:
			case L2Item.SLOT_HEAD:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_LEGS:
			{
				break;
			}
			}

			if (activeChar.isCursedWeaponEquipped() && itemId == 6408) // Don't allow to put formal wear
				return;

			if (activeChar.isAttackingNow())
			{
				if(item.getItem() instanceof L2Weapon) 
					ThreadPoolManager.getInstance().schedule(new WeaponEquipTask(item,activeChar), activeChar.getAttackEndTime() - L2System.milliTime());
				return;
			}
			// Equip or unEquip
			if (FortSiegeManager.getInstance().isCombat(item.getItemId()))
				return; //no message
			activeChar.useEquippableItem(item, true);
		}
		else
		{
			L2Weapon weaponItem = activeChar.getActiveWeaponItem();
			int itemid = item.getItemId();
			if (itemid == 4393)
				activeChar.sendPacket(new ShowCalculator(4393));
			else if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD)
					&& ((itemid >= 6519 && itemid <= 6527) || (itemid >= 7610 && itemid <= 7613) || (itemid >= 7807 && itemid <= 7809)
							|| (itemid >= 8484 && itemid <= 8486) || (itemid >= 8505 && itemid <= 8513) || itemid == 8548))
			{
				activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				activeChar.broadcastUserInfo(true);
				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(activeChar, false);
				sendPacket(il);
				return;
			}
			else
			{
				
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
				if (handler == null){
					if(item.getEtcItem()!=null) {
						for(String skillInfo : item.getEtcItem().getSkills()) {
							String sk[] = skillInfo.split("-");
							L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(sk[0]), Integer.parseInt(sk[1]));
							if(skill!=null) {
								if(skill.checkCondition(activeChar, activeChar)) 
									activeChar.useMagic(skill, true, true);
								else if(skill.checkCondition(activeChar, activeChar.getTarget()))
									activeChar.useMagic(skill, true, true);
							}
						}
					}
				}
				else
					handler.useItem(activeChar, item);
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__14_USEITEM;
	}
}