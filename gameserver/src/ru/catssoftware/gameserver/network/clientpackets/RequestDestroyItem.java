package ru.catssoftware.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.PetDataTable;
import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.util.Util;


public class RequestDestroyItem extends L2GameClientPacket
{
	private static final String	_C__59_REQUESTDESTROYITEM	= "[C] 59 RequestDestroyItem";

	private int					_objectId, _count;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (!getClient().checkKeyProtection())
		{
			ActionFailed();
			return;
		}

		if (_count < 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + " пытался удалить вещи, сумма < 0",
					Config.DEFAULT_PUNISH);
			return;
		}

		int count = _count;

		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}

		L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);

		// if we can't find requested item, its actually a cheat!
		if (itemToRemove == null)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}
		// Cannot discard item that the skill is consuming
		if (activeChar.isCastingNow())
		{
			if (activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getItemId())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}

		// Cannot discard item that the skill is consuming
		if (activeChar.isCastingSimultaneouslyNow())
		{
			if (activeChar.getLastSimultaneousSkillCast() != null && activeChar.getLastSimultaneousSkillCast().getItemConsumeId() == itemToRemove.getItemId())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
				return;
			}
		}

		int itemId = itemToRemove.getItemId();
		if (itemToRemove.isWear() || (!itemToRemove.isDestroyable() && !activeChar.isGM())
				|| (CursedWeaponsManager.getInstance().isCursed(itemId) && !activeChar.isGM()))
		{
			if (itemToRemove.isHeroItem())
				activeChar.sendPacket(SystemMessageId.HERO_WEAPONS_CANT_DESTROYED);
			else
				activeChar.sendPacket(SystemMessageId.CANNOT_DISCARD_THIS_ITEM);
			return;
		}

		if (Config.ALT_STRICT_HERO_SYSTEM)
		{
			if (itemToRemove.isHeroItem() && !activeChar.isGM())
			{
				activeChar.sendPacket(SystemMessageId.HERO_WEAPONS_CANT_DESTROYED);
				return;
			}
		}

		if (!itemToRemove.isStackable() && count > 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "Удаление стыкующихся вещей, сумма > 1, имя игрока " + activeChar.getName() + ".",
				Config.DEFAULT_PUNISH);
			return;
		}

		if (_count > itemToRemove.getCount())
			count = itemToRemove.getCount();

		if (itemToRemove.isEquipped())
		{
			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				activeChar.checkSSMatch(null, element);

				iu.addModifiedItem(element);
			}
			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo(true);
		}

		if (PetDataTable.isPetItem(itemId))
		{
			Connection con = null;
			try
			{
				if (activeChar.getPet() != null && activeChar.getPet().getControlItemId() == _objectId)
				{
					activeChar.getPet().unSummon(activeChar);
				}

				// if it's a pet control item, delete the pet
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
				statement.setInt(1, _objectId);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("could not delete pet objectid: ", e);
			}
			finally
			{
				try
				{
					if (con != null)
						con.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}

		L2ItemInstance removedItem = activeChar.getInventory().destroyItem("Destroy", _objectId, count, activeChar, null);

		if (removedItem == null)
			return;

		activeChar.getInventory().updateInventory(removedItem);

		L2World world = L2World.getInstance();
		world.removeObject(removedItem);
	}

	@Override
	public String getType()
	{
		return _C__59_REQUESTDESTROYITEM;
	}
}
