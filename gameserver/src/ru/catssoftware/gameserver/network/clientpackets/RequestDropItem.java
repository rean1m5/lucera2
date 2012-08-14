package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class RequestDropItem extends L2GameClientPacket
{
	private static final String	_C__12_REQUESTDROPITEM	= "[C] 12 RequestDropItem";

	private int					_objectId, _count, _x, _y, _z;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
	
		if (activeChar == null || activeChar.isDead())
			return;

		if (!FloodProtector.tryPerformAction(activeChar, Protected.DROPITEM))
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			activeChar.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			return;
		}

		L2ItemInstance item = activeChar.checkItemManipulation(_objectId, _count, "Drop");

		if (item == null)
		{
			_log.info("Error while droping item for char " + activeChar.getName() + " (validity check).");
			ActionFailed();
			return;
		}
		if (item.getItemId() != 9693)
		{
			if ((!Config.ALLOW_DISCARDITEM && !activeChar.isGM()) || (!item.isDropable() && !activeChar.isGM()))
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		if (item.getItemId() == Config.FORTSIEGE_COMBAT_FLAG_ID)
			return;
		if (item.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTED_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		if (Config.ALT_STRICT_HERO_SYSTEM)
		{
			if (item.isHeroItem())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		if (_count > item.getCount())
		{
			activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}
		if (Config.PLAYER_SPAWN_PROTECTION > 0 && activeChar.isInvul() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		if (_count <= 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] count <= 0! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		if (!item.isStackable() && _count > 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] count > 1 but item is not stackable! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		if (activeChar.isProcessingTransaction() || activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}
		if (activeChar.isFishing())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
			return;
		}
		if (activeChar.isCastingNow())
		{
			if (activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		if (activeChar.isCastingSimultaneouslyNow())
		{
			if (activeChar.getLastSimultaneousSkillCast() != null && activeChar.getLastSimultaneousSkillCast().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}
		if (L2Item.TYPE2_QUEST == item.getItem().getType2() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_EXCHANGE_ITEM);
			return;
		}
		if (!activeChar.isInsideRadius(_x, _y, 150, false) || Math.abs(_z - activeChar.getZ()) > 50)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_DISTANCE_TOO_FAR);
			return;
		}
		if (item.isEquipped())
		{
			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				activeChar.checkSSMatch(null, element);
				iu.addModifiedItem(element);
			}
			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo();
		}

		activeChar.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false);
	}

	@Override
	public String getType()
	{
		return _C__12_REQUESTDROPITEM;
	}
}