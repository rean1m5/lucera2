package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExSendManorList;
import javolution.util.FastList;

public class RequestManorList extends L2GameClientPacket
{
	private static final String	_C__FE_08_REQUESTMANORLIST	= "[S] FE:08 RequestManorList";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		FastList<String> manorsName = new FastList<String>();
		manorsName.add("gludio");
		manorsName.add("dion");
		manorsName.add("giran");
		manorsName.add("oren");
		manorsName.add("aden");
		manorsName.add("innadril");
		manorsName.add("goddard");
		manorsName.add("rune");
		manorsName.add("schuttgart");
		ExSendManorList manorlist = new ExSendManorList(manorsName);
		player.sendPacket(manorlist);
	}

	@Override
	public String getType()
	{
		return _C__FE_08_REQUESTMANORLIST;
	}
}