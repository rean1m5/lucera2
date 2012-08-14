package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.RecipeController;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestRecipeBookOpen extends L2GameClientPacket
{
	private static final String	_C__AC_REQUESTRECIPEBOOKOPEN	= "[C] AC RequestRecipeBookOpen";

	private boolean				_isDwarvenCraft;

	@Override
	protected void readImpl()
	{
		_isDwarvenCraft = (readD() == 0);
	}

	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() == null)
			return;
		getClient().getActiveChar()._bbsMultisell = 0;
		if (getClient().getActiveChar().getPrivateStoreType() != 0)
		{
			getClient().getActiveChar().sendPacket(SystemMessageId.PRIVATE_STORE_UNDER_WAY);
			return;
		}

		RecipeController.getInstance().requestBookOpen(getClient().getActiveChar(), _isDwarvenCraft);
	}

	@Override
	public String getType()
	{
		return _C__AC_REQUESTRECIPEBOOKOPEN;
	}
}
