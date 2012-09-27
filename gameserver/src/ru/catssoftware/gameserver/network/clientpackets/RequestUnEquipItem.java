package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.CTF.CTF;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;

public class RequestUnEquipItem extends L2GameClientPacket
{
	private static final String	_C__11_REQUESTUNEQUIPITEM	= "[C] 11 RequestUnequipItem";
	private int					_slot;

	@Override
	protected void readImpl()
	{
		_slot = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		activeChar._inWorld = true;
		
		L2ItemInstance item = activeChar.getInventory().getPaperdollItemByL2ItemId(_slot);
		if (item == null || item.isWear())
			return;


		if (_slot == L2Item.SLOT_LR_HAND && activeChar.isCursedWeaponEquipped())
			return;
		if( item.getItemId() == 6718 && activeChar.getGameEvent() == CTF.getInstance())
			return;
		
		if (item.getItemId() == Config.FORTSIEGE_COMBAT_FLAG_ID)
			return;

		if (activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead())
			return;

		if (activeChar.isAttackingNow() || activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
			return;

		L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(_slot);

		for (L2ItemInstance element : unequiped)
		{
			activeChar.checkSSMatch(null, element);
			activeChar.getInventory().updateInventory(element);
		}

		activeChar.broadcastUserInfo(true);

		if (unequiped.length > 0)
		{
			SystemMessage sm = null;
			if (unequiped[0].getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(unequiped[0].getEnchantLevel());
				sm.addItemName(unequiped[0]);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(unequiped[0]);
			}
			activeChar.sendPacket(sm);
		}
	}

	@Override
	public String getType()
	{
		return _C__11_REQUESTUNEQUIPITEM;
	}
}
