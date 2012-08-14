package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.PetNameTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;

public class RequestChangePetName extends L2GameClientPacket
{
	private static final String	REQUESTCHANGEPETNAME__C__89	= "[C] 89 RequestChangePetName";

	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		final L2Summon pet = activeChar.getPet();
		if (pet == null)
			return;

		if (pet.getName() != null && pet.getName().trim().length() != 0)
		{
			activeChar.sendPacket(SystemMessageId.NAMING_YOU_CANNOT_SET_NAME_OF_THE_PET);
			return;
		}
		else if (PetNameTable.getInstance().doesPetNameExist(_name, pet.getTemplate().getNpcId()))
		{
			activeChar.sendPacket(SystemMessageId.NAMING_ALREADY_IN_USE_BY_ANOTHER_PET);
			return;
		}
		else if (_name.length() < 3 || _name.length() > 8)
		{
			activeChar.sendPacket(SystemMessageId.NAMING_PETNAME_UP_TO_8CHARS);
			return;
		}
		else if (!Config.CLAN_ALLY_NAME_PATTERN.matcher(_name).matches())
		{
			activeChar.sendPacket(SystemMessageId.NAMING_PETNAME_CONTAINS_INVALID_CHARS);
			return;
		}
		pet.setName(_name);
		pet.broadcastFullInfo();

		// set the flag on the control item to say that the pet has a name
		if (pet instanceof L2PetInstance)
		{
			L2ItemInstance controlItem = pet.getOwner().getInventory().getItemByObjectId(pet.getControlItemId());
			if (controlItem != null)
			{
				controlItem.setCustomType2(1);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(controlItem);
				activeChar.sendPacket(iu);
			}
		}
	}

	@Override
	public String getType()
	{
		return REQUESTCHANGEPETNAME__C__89;
	}
}