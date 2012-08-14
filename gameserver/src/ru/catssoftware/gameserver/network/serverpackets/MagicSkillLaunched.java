package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;

public final class MagicSkillLaunched extends L2GameServerPacket
{
	private static final String		_S__8E_MAGICSKILLLAUNCHED	= "[S] 8E MagicSkillLaunched";
	private final int				_charObjId;
	private final int				_skillId;
	private final int				_skillLevel;
	private final L2Object[]		_targets;

	public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel, boolean isPositive, L2Object... targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_targets = targets;
	}

	public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel, boolean isPositive)
	{
		this(cha, skillId, skillLevel, isPositive, cha.getTarget());
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x76);
		writeD(_charObjId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_targets.length);

		if (_targets.length == 0)
			writeD(0);
		else
		{
			for (L2Object target : _targets)
				writeD(target == null ? 0 : target.getObjectId());
		}
	}

	@Override
	public String getType()
	{
		return _S__8E_MAGICSKILLLAUNCHED;
	}
}