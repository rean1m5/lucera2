package ru.catssoftware.gameserver.model;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.model.itemcontainer.PcInventory;


public class CharSelectInfoPackage
{
	private String	_name;
	private int		_charId				= 0x00030b7a;
	private int[][]	_paperdoll;
	private long	_exp, _deleteTimer, _lastAccess																	= 0;
	private int		_sp, _clanId, _karma, _pkKills, _augmentationId, _race, _classId, _baseClassId, _face, _hairStyle,
					_hairColor, _sex, _level, _maxHp, _maxMp, _objectId							= 0;
	private boolean	_isBanned																						= false;
	private double	_currentHp,_currentMp																			= 0;

	public CharSelectInfoPackage(int objectId, String name)
	{
		setObjectId(objectId);
		_name = name;
		_paperdoll = PcInventory.restoreVisibleInventory(objectId);
	}

	public int getObjectId()
	{
		return _objectId;
	}

	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}

	public int getCharId()
	{
		return _charId;
	}

	public void setCharId(int charId)
	{
		_charId = charId;
	}

	public int getClanId()
	{
		return _clanId;
	}

	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}

	public int getClassId()
	{
		return _classId;
	}

	public int getBaseClassId()
	{
		return _baseClassId;
	}

	public void setClassId(int classId)
	{
		_classId = classId;
	}

	public void setBaseClassId(int baseClassId)
	{
		_baseClassId = baseClassId;
	}

	public double getCurrentHp()
	{
		return _currentHp;
	}

	public void setCurrentHp(double currentHp)
	{
		_currentHp = currentHp;
	}

	public double getCurrentMp()
	{
		return _currentMp;
	}

	public void setCurrentMp(double currentMp)
	{
		_currentMp = currentMp;
	}

	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	public void setLastAccess(long lastAccess)
	{
		_lastAccess = lastAccess;
	}

	public long getExp()
	{
		return _exp;
	}

	public void setExp(long exp)
	{
		_exp = exp;
	}

	public int getFace()
	{
		return _face;
	}

	public void setFace(int face)
	{
		_face = face;
	}

	public int getHairColor()
	{
		return _hairColor;
	}

	public void setHairColor(int hairColor)
	{
		_hairColor = hairColor;
	}

	public int getHairStyle()
	{
		return _hairStyle;
	}

	public void setHairStyle(int hairStyle)
	{
		_hairStyle = hairStyle;
	}

	public int getPaperdollObjectId(int slot)
	{
		return _paperdoll[slot][0];
	}

	public int getPaperdollItemId(int slot)
	{
		return _paperdoll[slot][1];
	}

	public int getPaperdollItemDisplayId(int slot)
	{
		return _paperdoll[slot][3];
	}

	public int getLevel()
	{
		return _level;
	}

	public void setLevel(int level)
	{
		_level = level;
	}

	public int getMaxHp()
	{
		return _maxHp;
	}

	public void setMaxHp(int maxHp)
	{
		_maxHp = maxHp;
	}

	public int getMaxMp()
	{
		return _maxMp;
	}

	public void setMaxMp(int maxMp)
	{
		_maxMp = maxMp;
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public int getRace()
	{
		return _race;
	}

	public void setRace(int race)
	{
		_race = race;
	}

	public int getSex()
	{
		return _sex;
	}

	public void setSex(int sex)
	{
		_sex = sex;
	}

	public int getSp()
	{
		return _sp;
	}

	public void setSp(int sp)
	{
		_sp = sp;
	}

	public int getKarma()
	{
		return _karma;
	}

	public void setKarma(int karma)
	{
		_karma = karma;
	}

	public int getEnchantEffect()
	{
		if(Config.ENCHANT_LIMIT_AURA_SELF)
			return Math.min(Config.ENCHANT_LIMIT_AURA_LEVEL, _paperdoll[Inventory.PAPERDOLL_RHAND][2]);
		return _paperdoll[Inventory.PAPERDOLL_RHAND][2];
	}

	public void setAugmentationId(int augmentationId)
	{
		_augmentationId = augmentationId;
	}

	public int getAugmentationId()
	{
		return _augmentationId;
	}

	public void setPkKills(int PkKills)
	{
		_pkKills = PkKills;
	}

	public int getPkKills()
	{
		return _pkKills;
	}
	
	public boolean isBanned()
	{
		return _isBanned;
	}
	
	public void setBanned(boolean val)
	{
		_isBanned  = val;
	}
}