package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.HennaTable;
import ru.catssoftware.gameserver.datatables.HennaTreeTable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Henna;
import ru.catssoftware.gameserver.util.Util;

public class RequestHennaEquip extends L2GameClientPacket
{
	private static final String	_C__BC_RequestHennaEquip	= "[C] bc RequestHennaEquip";
	private int					_symbolId;

	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Henna temp = HennaTable.getInstance().getTemplate(_symbolId);
		if (temp == null)
			return;

		/* Prevents henna drawing exploit:
		   1) talk to L2SymbolMakerInstance
		   2) RequestHennaList
		   3) Don't close the window and go to a GrandMaster and change your subclass
		   4) Get SymbolMaker range again and press draw
		   You could draw any kind of henna just having the required subclass...
		 */
		boolean cheater = true;
		for (L2Henna h : HennaTreeTable.getInstance().getAvailableHenna(activeChar))
		{
			if (h.getSymbolId() == temp.getSymbolId())
			{
				cheater = false;
				break;
			}
		}

		L2ItemInstance item = activeChar.getInventory().getItemByItemId(temp.getItemId());

		int count = (item == null ? 0 : item.getCount());

		if (!cheater && count >= temp.getAmount() && activeChar.getAdena() >= temp.getPrice() && activeChar.addHenna(temp))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addItemName(temp.getItemId());
			activeChar.sendPacket(sm);
			activeChar.reduceAdena("Henna", temp.getPrice(), activeChar.getLastFolkNPC(), true);
			L2ItemInstance dye = activeChar.getInventory().destroyItemByItemId("Henna", temp.getItemId(), temp.getAmount(), activeChar,
				activeChar.getLastFolkNPC());
			activeChar.getInventory().updateInventory(dye);
			activeChar.sendPacket(SystemMessageId.SYMBOL_ADDED);
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
			if (!activeChar.isGM() && cheater)
				Util.handleIllegalPlayerAction(activeChar, "Exploit attempt: Character " + activeChar.getName() + " of account " + activeChar.getAccountName()
						+ " tryed to add a forbidden henna.", Config.DEFAULT_PUNISH);
		}
	}

	@Override
	public String getType()
	{
		return _C__BC_RequestHennaEquip;
	}
}
