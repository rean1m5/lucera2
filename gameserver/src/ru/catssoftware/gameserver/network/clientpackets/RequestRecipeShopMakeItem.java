package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.RecipeController;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.util.Util;

public class RequestRecipeShopMakeItem extends L2GameClientPacket
{
	private static final String	_C__AF_REQUESTRECIPESHOPMAKEITEM	= "[C] B6 RequestRecipeShopMakeItem";

	private int					_id;
	private int					_recipeId;
	@SuppressWarnings("unused")
	private int					_unknown;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_recipeId = readD();
		_unknown = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Object object = null;

		// Get object from target
		if (activeChar.getTargetId() == _id)
			object = activeChar.getTarget();

		// Get object from world
		if (object == null)
			object = L2World.getInstance().getPlayer(_id);

		if (object != null && !object.isPlayer())
			return;

		L2PcInstance manufacturer = (L2PcInstance) object;

		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessageId.PRIVATE_STORE_UNDER_WAY);
			return;
		}
		if (manufacturer.getPrivateStoreType() != 5)
			return;

		if (activeChar.isInCraftMode() || manufacturer.isInCraftMode())
		{
			activeChar.sendMessage("Currently in Craft Mode");
			return;
		}
		if (manufacturer.isInDuel() || activeChar.isInDuel())
		{
			activeChar.sendPacket(SystemMessageId.CANT_CRAFT_DURING_COMBAT);
			return;
		}
		if(activeChar.getInventoryLimit()-activeChar.getInventory().getSize()<=1) {
			activeChar.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}

		if (Util.checkIfInRange(150, activeChar, manufacturer, true))
			RecipeController.getInstance().requestManufactureItem(manufacturer, _recipeId, activeChar);
	}

	@Override
	public String getType()
	{
		return _C__AF_REQUESTRECIPESHOPMAKEITEM;
	}
}