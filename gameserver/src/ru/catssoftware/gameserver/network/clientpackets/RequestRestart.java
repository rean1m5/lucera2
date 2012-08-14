package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.L2GameClient.GameClientState;
import ru.catssoftware.gameserver.network.serverpackets.CharSelectionInfo;
import ru.catssoftware.gameserver.network.serverpackets.RestartResponse;

public final class RequestRestart extends L2GameClientPacket
{
	private static final String _C__46_REQUESTRESTART = "[C] 46 RequestRestart";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		final L2GameClient client = getClient();
		final L2PcInstance activeChar = client.getActiveChar();

		if (activeChar == null)
			return;

		if (!activeChar.canLogout())
		{
			ActionFailed();
			return;
		}
		activeChar._inWorld=false;
		new Disconnection(client, activeChar).deleteMe();

		client.setState(GameClientState.AUTHED);
		sendPacket(new RestartResponse());
		CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
		sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}

	@Override
	public String getType()
	{
		return _C__46_REQUESTRESTART;
	}
}