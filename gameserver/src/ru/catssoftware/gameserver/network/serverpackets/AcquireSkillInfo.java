package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import javolution.util.FastList;

public class AcquireSkillInfo extends L2GameServerPacket
{
	private static final String	_S__91_AQUIRESKILLINFO	= "[S] 91 AquireSkillInfo [dddd d (dddd)]";
	private FastList<Req>		_reqs;
	private int					_id, _level, _spCost, _mode;

	private class Req
	{
		public int	itemId;
		public int	count;
		public int	type;
		public int	unk;

		public Req(int pType, int pItemId, int pCount, int pUnk)
		{
			itemId = pItemId;
			type = pType;
			count = pCount;
			unk = pUnk;
		}
	}

	public AcquireSkillInfo(int id, int level, int spCost, int mode)
	{
		_reqs = new FastList<Req>();
		_id = id;
		_level = level;
		_spCost = spCost;
		_mode = mode;
	}

	public void addRequirement(int type, int id, int count, int unk)
	{
		_reqs.add(new Req(type, id, count, unk));
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x8b);
		writeD(_id);
		writeD(_level);
		writeD(_spCost);
		writeD(_mode); //c4

		writeD(_reqs.size());

		for (Req temp : _reqs)
		{
			writeD(temp.type);
			writeD(temp.itemId);
			writeD(temp.count);
			writeD(temp.unk);
		}
	}

	@Override
	public String getType()
	{
		return _S__91_AQUIRESKILLINFO;
	}
}
