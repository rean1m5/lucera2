package ru.catssoftware.gameserver.network.serverpackets;
public class AcquireSkillDone extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x25); // ActionFailed
	}

	@Override
	public String getType()
	{
		return "[S] 94 AcquireSkillDone";
	}
}