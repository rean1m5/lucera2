package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.RecipeController;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class RequestRecipeItemMakeSelf extends L2GameClientPacket
{
	private static final String	_C__AF_REQUESTRECIPEITEMMAKESELF	= "[C] AF RequestRecipeItemMakeSelf";

	private int					_id;

	@Override
	protected void readImpl()
	{
		_id = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_CREATEITEM && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			activeChar.sendPacket(SystemMessageId.FUNCTION_INACCESSIBLE_NOW);
			ActionFailed();
			return;
		}

		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessageId.PRIVATE_STORE_UNDER_WAY);
			return;
		}

		if (activeChar.isInCraftMode())
		{
			activeChar.sendMessage("Попробуйте позже");
			return;
		}

		RecipeController.getInstance().requestMakeItem(activeChar, _id);
	}

	@Override
	public String getType()
	{
		return _C__AF_REQUESTRECIPEITEMMAKESELF;
	}
}
