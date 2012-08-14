package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;

public class MagicSkillUse extends L2GameServerPacket
{
	private static final String	_S__5A_MAGICSKILLUSER	= "[S] 5A MagicSkillUser";
	private boolean				_isPositive				= false;
	private int					_targetId;
	private int					_skillId;
	private int					_skillLevel;
	private int					_skillTime;
	private int					_reuseDelay;
	private int					_charObjId, _x, _y, _z;

	public MagicSkillUse(L2Character cha, L2Character target, int skillId, int skillLevel, int skillTime, int reuseDelay, boolean isPositive)
	{
		_charObjId = cha.getObjectId();
		_targetId = target.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_skillTime = skillTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_isPositive = isPositive;
	}

	public MagicSkillUse(L2Character cha, int skillId, int skillLevel, int skillTime, int reuseDelay, boolean isPositive)
	{
		this(cha, cha, skillId, skillLevel, skillTime, reuseDelay, isPositive);
	}

	public MagicSkillUse(L2Character cha, L2Character target, L2Skill skill, int skillTime, int reuseDelay, boolean isPositive)
	{
		this(cha, target, skill.getDisplayId(), skill.getLevel(), skillTime, reuseDelay, skill.isPositive());
	}

	public MagicSkillUse(L2Character cha, L2Skill skill, int skillTime, int reuseDelay, boolean isPositive)
	{
		this(cha, cha, skill.getDisplayId(), skill.getLevel(), skillTime, reuseDelay, skill.isPositive());
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x48);
		writeD(_charObjId);
		writeD(_targetId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_skillTime);
		writeD(_reuseDelay);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeH(0x00);
		writeH(0x00); // unknown loop but not AoE
		if (client.getProtocolVer()>=83)
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
		}
	}
	
	public boolean isPositive()
	{
		return _isPositive;
	}

	
	@Override
	public boolean canBroadcast(L2PcInstance  player)
	{
		return true;
	}

	@Override
	public String getType()
	{
		return _S__5A_MAGICSKILLUSER;
	}
}