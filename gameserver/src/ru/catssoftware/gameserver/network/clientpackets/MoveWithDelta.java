package ru.catssoftware.gameserver.network.clientpackets;

public class MoveWithDelta extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int	_dx;
	@SuppressWarnings("unused")
	private int	_dy;
	@SuppressWarnings("unused")
	private int	_dz;

	@Override
	protected void readImpl()
	{
		_dx = readD();
		_dy = readD();
		_dz = readD();
	}

	@Override
	protected void runImpl(){}

	@Override
	public String getType()
	{
		return "[C] 0x41 MoveWithDelta";
	}
}
