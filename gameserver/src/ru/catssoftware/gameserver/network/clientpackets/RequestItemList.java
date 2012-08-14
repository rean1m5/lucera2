package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;

public class RequestItemList extends L2GameClientPacket
{
	private static final String	_C__0F_REQUESTITEMLIST	= "[C] 0F RequestItemList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		if (getClient() != null && getClient().getActiveChar() != null && !getClient().getActiveChar().isInvetoryDisabled())
		{
			L2PcInstance pc = getClient().getActiveChar();
			if(pc.getTrading()) {
				pc.cancelActiveTrade();
				pc.setTrading(false);
			}
			ItemList il = new ItemList(getClient().getActiveChar(), true);
			sendPacket(il);
		}
	}

	@Override
	public String getType()
	{
		return _C__0F_REQUESTITEMLIST;
	}
}
