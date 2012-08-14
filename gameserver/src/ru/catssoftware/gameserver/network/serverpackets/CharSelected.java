package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class CharSelected extends L2GameServerPacket
{
	private static final String	_S__0B_activeCharRSELECTED	= "[S] 0b CharSelected [sdsddddddddddffdqdddddddddddd ddddddddddddddddddddddddddd ff ddd c hh d]";
	private L2PcInstance		_activeChar;
	private int					_sessionId;

	public CharSelected(L2PcInstance cha, int sessionId)
	{
		_activeChar = cha;
		_sessionId = sessionId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x15);

		writeS(_activeChar.getName());
		writeD(_activeChar.getObjectId()); // ??
		writeS(_activeChar.getTitle());
		writeD(_sessionId);
		writeD(_activeChar.getClanId());
		writeD(0x00); //??
		writeD(_activeChar.getAppearance().getSex() ? 1 : 0);
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.getClassId().getId());
		writeD(0x01); // active ??
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());

		writeF(_activeChar.getCurrentHp());
		writeF(_activeChar.getCurrentMp());
		writeD(_activeChar.getSp());
		writeQ(_activeChar.getExp());
		writeD(_activeChar.getLevel());
		writeD(_activeChar.getKarma()); // thx evill33t
		writeD(0x0); //?
		writeD(_activeChar.getINT());
		writeD(_activeChar.getStat().getSTR());
		writeD(_activeChar.getStat().getCON());
		writeD(_activeChar.getStat().getMEN());
		writeD(_activeChar.getStat().getDEX());
		writeD(_activeChar.getStat().getWIT());
		for(int i = 0; i < 30; i++)
		{
			writeD(0x00);
		}
		//		writeD(0); //c3
		//writeD(0); //c3
		//		writeD(0); //c3

		writeD(0x00); //c3  work
		writeD(0x00); //c3  work

		// extra info
		writeD(GameTimeController.getInstance().getGameTime()); // in-game time

		writeD(0x00); //

		writeD(0x00); //c3

		writeD(0x00); //c3 InspectorBin
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3

		writeD(0x00); //c3 InspectorBin for 528 client
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
		writeD(0x00); //c3
	}

	@Override
	public String getType()
	{
		return _S__0B_activeCharRSELECTED;
	}
}