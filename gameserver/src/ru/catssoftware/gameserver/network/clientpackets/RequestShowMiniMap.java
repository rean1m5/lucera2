package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.serverpackets.ShowMiniMap;

public class RequestShowMiniMap extends L2GameClientPacket
{
	private static final String	_C__cd_REQUESTSHOWMINIMAP	= "[C] cd RequestShowMiniMap";

	/**
	 */
	@Override
	protected void readImpl(){}

	@Override
	protected final void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		if (activeChar.isMiniMapOpen())
			activeChar.setMiniMapOpen(false);
		else
		{
			if (activeChar.isInsideZone(L2Zone.FLAG_NOMAP))
			{
				activeChar.sendMessage("Тут недоступна карта");
				return;
			}
			activeChar.setMiniMapOpen(true);
		}
		activeChar.sendPacket(new ShowMiniMap());
	}

	@Override
	public String getType()
	{
		return _C__cd_REQUESTSHOWMINIMAP;
	}
}
