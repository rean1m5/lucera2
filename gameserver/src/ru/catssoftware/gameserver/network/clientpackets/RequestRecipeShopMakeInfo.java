package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.RecipeShopItemInfo;

public class RequestRecipeShopMakeInfo extends L2GameClientPacket
{
	private static final String	_C__B5_RequestRecipeShopMakeInfo	= "[C] b5 RequestRecipeShopMakeInfo";

	private int _objectId;
	private int _recipeId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_recipeId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Object obj = null;

		// Get object from target
		if (activeChar.getTargetId() == _objectId)
			obj = activeChar.getTarget();

		// Get object from world
		if (obj == null)
			obj = L2World.getInstance().getPlayer(_objectId);

		if (!(obj instanceof L2PcInstance))
			return;

		activeChar.sendPacket(new RecipeShopItemInfo((L2PcInstance) obj, _recipeId));
	}

	@Override
	public String getType()
	{
		return _C__B5_RequestRecipeShopMakeInfo;
	}
}