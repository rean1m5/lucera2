package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ConfirmDlg;

public class RequestDeleteMacro extends L2GameClientPacket
{
	private int					_id;

	private static final String	_C__C2_REQUESTDELETEMACRO	= "[C] C2 RequestDeleteMacro";

	@Override
	protected void readImpl()
	{
		_id = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getActiveChar();
		if (player == null)
			return;
		new ConfirmDlg(SystemMessageId.WISH_TO_DELETE_MACRO.getId());
		player.deleteMacro(_id);
		player.sendMessage("Макрос успешно удален");
	}

	@Override
	public String getType()
	{
		return _C__C2_REQUESTDELETEMACRO;
	}
}