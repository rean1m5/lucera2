package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestRecipeShopMessageSet extends L2GameClientPacket
{
	private static final String	_C__B1_RequestRecipeShopMessageSet	= "[C] b1 RequestRecipeShopMessageSet";

	private String				_name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		/*if (player.getCreateList() == null)
		{
		    player.setCreateList(new L2ManufactureList());
		}*/
		if (player.getCreateList() != null)
		{
			player.getCreateList().setStoreName(_name);
		}

	}

	@Override
	public String getType()
	{
		return _C__B1_RequestRecipeShopMessageSet;
	}
}
