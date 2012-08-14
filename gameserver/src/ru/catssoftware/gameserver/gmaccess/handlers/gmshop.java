package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.BuyList;

public class gmshop extends gmHandler
{
	private static final String[] commands =
	{
		"buy",
		"gmshop"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String command = params[0];

		if (command.startsWith("buy"))
		{
			try
			{
				handleBuyRequest(admin, Integer.parseInt(params[1]));
			}
			catch (IndexOutOfBoundsException e)
			{
				admin.sendMessage("Введите ID магазина.");
			}
			return;
		}
		else if (command.equals("gmshop"))
		{
			admin.setTarget(admin);
			methods.showSubMenuPage(admin, "adminshop_menu.htm");
			return;
		}
	}

	private void handleBuyRequest(L2PcInstance activeChar, int id)
	{
		L2TradeList list = TradeListTable.getInstance().getBuyList(id);

		if (list != null)
			activeChar.sendPacket(new BuyList(list, activeChar.getAdena()));

		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}