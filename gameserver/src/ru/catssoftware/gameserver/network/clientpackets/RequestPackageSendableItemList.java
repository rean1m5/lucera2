package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.network.serverpackets.PackageSendableList;

public final class RequestPackageSendableItemList extends L2GameClientPacket
{
	private static final String	_C_9E_REQUESTPACKAGESENDABLEITEMLIST	= "[C] 9E RequestPackageSendableItemList";

	private int					_objectID;

	@Override
	protected void readImpl()
	{
		_objectID = readD();
	}

	@Override
	public void runImpl()
	{
		sendPacket(new PackageSendableList(getClient().getActiveChar(), _objectID));
	}

	@Override
	public String getType()
	{
		return _C_9E_REQUESTPACKAGESENDABLEITEMLIST;
	}
}