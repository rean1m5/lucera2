package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

public class RequestLinkHtml extends L2GameClientPacket
{
	private static final String	REQUESTLINKHTML__C__20	= "[C] 20 RequestLinkHtml";
	private String				_link;

	@Override
	protected void readImpl()
	{
		_link = readS();
	}

	@Override
	public void runImpl()
	{
		L2PcInstance actor = getClient().getActiveChar();
		if (actor == null)
			return;

		_link = readS();

		if (_link.contains("..") || !_link.contains(".htm"))
		{
			_log.warn("[RequestLinkHtml] hack by " + actor.getName() + "? link contains prohibited characters: '" + _link + "', skipped");
			return;
		}

		try
		{
			String filename = "data/html/"+_link;
			NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setFile(filename);
			sendPacket(msg);
		}
		catch (Exception e)
		{
			_log.warn("Bad RequestLinkHtml: "+e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public String getType()
	{
		return REQUESTLINKHTML__C__20;
	}
}
