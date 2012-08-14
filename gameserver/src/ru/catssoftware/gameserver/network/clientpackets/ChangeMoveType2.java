package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class ChangeMoveType2 extends L2GameClientPacket
{
	private static final String	_C__1C_CHANGEMOVETYPE2	= "[C] 1C ChangeMoveType2";

	private boolean				_typeRun;

	/**
	 * packet type id 0x1c
	 *
	 * sample
	 *
	 * 1d
	 * 01 00 00 00 // type (0 = walk, 1 = run)
	 *
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_typeRun = readD() == 1;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (_typeRun)
			player.setRunning();
		else
			player.setWalking();
	}

	@Override
	public String getType()
	{
		return _C__1C_CHANGEMOVETYPE2;
	}
}
