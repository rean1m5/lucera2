package ru.catssoftware.gameserver.network.serverpackets;

/**
 * @author  KenM
 */

public class ExDuelAskStart extends L2GameServerPacket
{
	private static final String	_S__FE_4C_EXDUELASKSTART	= "[S] FE:4c ExDuelAskStart [sd]";

	private String				_requestorName;
	private int					_partyDuel;

	public ExDuelAskStart(String requestor, int partyDuel)
	{
		_requestorName = requestor;
		_partyDuel = partyDuel;
	}

	/**
	 * @see ru.catssoftware.gameserver.network.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x4b);
		writeS(_requestorName);
		writeD(_partyDuel);
	}

	/**
	 * @see ru.catssoftware.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_4C_EXDUELASKSTART;
	}
}
