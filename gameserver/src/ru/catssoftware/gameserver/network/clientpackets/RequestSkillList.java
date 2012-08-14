package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class RequestSkillList extends L2GameClientPacket
{
	private static final String	_C__3F_REQUESTSKILLLIST	= "[C] 3F RequestSkillList";
	//private final static Logger _log = Logger.getLogger(RequestSkillList.class.getName());
	@SuppressWarnings("unused")
	private int					_unk1;
	@SuppressWarnings("unused")
	private int					_unk2;
	@SuppressWarnings("unused")
	private int					_unk3;

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance cha = getClient().getActiveChar();

		if (cha == null)
			return;
		cha.sendSkillList();
	}

	@Override
	public String getType()
	{
		return _C__3F_REQUESTSKILLLIST;
	}
}
