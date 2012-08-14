package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestGiveItemToPet extends L2GameClientPacket
{
	private static final String REQUESTCIVEITEMTOPET__C__8B = "[C] 8B RequestGiveItemToPet";
	private int _objectId, _amount;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_amount = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar(); 
	
		if (player == null || !(player.getPet() instanceof L2PetInstance))
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			return;
		}
	
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && player.getKarma() > 0)
			return;

		if (player.getPrivateStoreType() != 0)
		{
			sendPacket(SystemMessageId.ITEMS_CANNOT_BE_DISCARDED_OR_DESTROYED_WHILE_OPERATING_PRIVATE_STORE_OR_WORKSHOP);
			return;
		}

		if (player.getRequest().getRequestPacket() instanceof TradeRequest || player.getRequest().getRequestPacket() instanceof TradeDone)
		{
			sendPacket(SystemMessageId.CANNOT_DISCARD_OR_DESTROY_ITEM_WHILE_TRADING);
			return;
		}

		L2PetInstance pet = (L2PetInstance) player.getPet(); 
		if (pet.isDead())
		{
			sendPacket(SystemMessageId.CANNOT_GIVE_ITEMS_TO_DEAD_PET);
			return;
		}

		if (_amount < 0 || _objectId < 0)
			return;

		L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;

		if (!item.isDropable() || !item.isDestroyable() || !item.isTradeable())
		{
			sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		if (!item.isAvailable(player, true))
		{
			sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return;
		}
		if (item.isAugmented())
		{
			sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		if (Config.ALT_STRICT_HERO_SYSTEM && item.isHeroItem())
		{
			sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		int id = item.getItemId();
		if (id == 10612 || id == 10280 || id == 10281 || id == 10282 || id == 10283 || id == 10284 || id == 10285 || id == 10286 || id == 10287 || id == 10288 || id == 10289
		|| id == 10290 || id == 10291 || id == 10292 || id == 10293 || id == 10294 || id == 13002 || id == 13046 || id == 13047 || id == 13042 || id == 13043 || id == 13044)
		{
			sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		if (!pet.getInventory().validateCapacity(item))
		{
			pet.getOwner().sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
			return;
		}
		if (!pet.getInventory().validateWeight(item,_amount))
		{
			pet.getOwner().sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
			return;
		}
		if (player.transferItem("Transfer", _objectId, _amount, pet.getInventory(), pet) == null)
			_log.info("Invalid item transfer request: [Pet]" + pet.getName() + " --> " + player.getName());
	}

	@Override
	public String getType()
	{
		return REQUESTCIVEITEMTOPET__C__8B;
	}
}