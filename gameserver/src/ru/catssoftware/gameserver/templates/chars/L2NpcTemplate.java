/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.templates.chars;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2DropCategory;
import ru.catssoftware.gameserver.model.L2DropData;
import ru.catssoftware.gameserver.model.L2MinionData;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.util.StatsSet;

public final class L2NpcTemplate extends L2CharTemplate
{
	private final static Logger					_log		= Logger.getLogger(L2NpcTemplate.class.getName());

	private int									_npcId;
	private int									_idTemplate;
	private String								_type;
	private String								_name;
	private boolean								_serverSideName;
	private String								_title;
	private boolean								_serverSideTitle;
	private String								_sex;
	private byte								_level;
	private int									_rewardExp;
	private int									_rewardSp;
	private int									_aggroRange;
	private int									_rhand;
	private int									_lhand;
	private int									_armor;
	private String								_factionId;
	private int									_factionRange;
	private int									_absorbLevel;
	private AbsorbCrystalType					_absorbType;
	private short								_ss;
	private short								_bss;
	private short								_ssRate;
	private int									_npcFaction;
	private String								_npcFactionName;
//	private String								_jClass;
	private ru.catssoftware.gameserver.model.base.Race								_npcRace;	
	
	private AIType								_ai;
	private boolean								_isQuestMonster;
	private boolean								_dropHerbs;

	private Race								_race;
	private List<L2DropCategory>				_categories	= null;
	private List<L2MinionData>					_minions	= null;
	private List<ClassId>						_teachInfo;
	private Map<Integer, L2Skill>				_skills;
	private Map<Stats, Double>					_vulnerabilities;
	private Map<Quest.QuestEventType, Quest[]>	_questEvents;
	public static enum AbsorbCrystalType
	{
		LAST_HIT, FULL_PARTY, PARTY_ONE_RANDOM
	}
	public static enum AIType
	{
		FIGHTER, ARCHER, BALANCED, MAGE, HEALER
	}
	public static enum Race
	{
		UNDEAD,
		MAGICCREATURE,
		BEAST,
		ANIMAL,
		PLANT,
		HUMANOID,
		SPIRIT,
		ANGEL,
		DEMON,
		DRAGON,
		GIANT,
		BUG,
		FAIRIE,
		HUMAN,
		ELVE,
		DARKELVE,
		ORC,
		DWARVE,
		OTHER,
		NONLIVING,
		SIEGEWEAPON,
		DEFENDINGARMY,
		MERCENARIE,
		UNKNOWN,
		KAMAEL,
		NONE
	}

	public L2NpcTemplate(StatsSet set)
	{
		super(set);
		_npcId = set.getInteger("npcId");
		_idTemplate = set.getInteger("idTemplate");
		_type = set.getString("type");
		_name = set.getString("name");
		_serverSideName = set.getBool("serverSideName");
		_title = set.getString("title");
		_isQuestMonster = _title.equalsIgnoreCase("Quest Monster");
		_serverSideTitle = set.getBool("serverSideTitle");
		_sex = set.getString("sex");
		_level = set.getByte("level");
		_rewardExp = set.getInteger("rewardExp");
		_rewardSp = set.getInteger("rewardSp");
		_aggroRange = set.getInteger("aggroRange");
		_rhand = set.getInteger("rhand");
		_lhand = set.getInteger("lhand");
		_armor = set.getInteger("armor");
		setFactionId(set.getString("factionId", null));
		_factionRange = set.getInteger("factionRange");
		_absorbLevel = set.getInteger("absorb_level", 0);
		_absorbType = AbsorbCrystalType.valueOf(set.getString("absorb_type"));
		_ss = (short) set.getInteger("ss", 0);
		_bss = (short) set.getInteger("bss", 0);
		_ssRate = (short) set.getInteger("ssRate", 0);
		_npcFaction = set.getInteger("NPCFaction", 0);
		_npcFactionName = set.getString("NPCFactionName", "Devine Clan");
		try {
			_npcRace = ru.catssoftware.gameserver.model.base.Race.values()[set.getInteger("race")];
		} catch(Exception e) {
			_npcRace = ru.catssoftware.gameserver.model.base.Race.NPC;
		}
//		_jClass = set.getString("jClass");
		_dropHerbs = set.getBool("drop_herbs", false);

		String ai = set.getString("AI", "fighter");
		if (ai.equalsIgnoreCase("archer"))
			_ai = AIType.ARCHER;
		else if (ai.equalsIgnoreCase("balanced"))
			_ai = AIType.BALANCED;
		else if (ai.equalsIgnoreCase("mage"))
			_ai = AIType.MAGE;
		else
			_ai = AIType.FIGHTER;

		_race = null;
		_teachInfo = null;
	}

	public void addTeachInfo(ClassId classId)
	{
		if (_teachInfo == null)
			_teachInfo = new FastList<ClassId>();
		_teachInfo.add(classId);
	}

	public List<ClassId> getTeachInfo()
	{
		return _teachInfo;
	}

	public boolean canTeach(ClassId classId)
	{
		if (_teachInfo == null)
			return false;

		if (classId.level() == 3)
			return _teachInfo.contains(classId.getParent());

		return _teachInfo.contains(classId);
	}

	public void addDropData(L2DropData drop, int categoryType)
	{
		if (drop.isQuestDrop()){}
		else
		{
			if (_categories == null)
				_categories = new FastList<L2DropCategory>();
			synchronized (_categories)
			{
				boolean catExists = false;
				for (L2DropCategory cat : _categories)
					if (cat.getCategoryType() == categoryType)
					{
						if (_type.equalsIgnoreCase("L2RaidBoss"))
							cat.addDropData(drop, 1);
						else if (_type.equalsIgnoreCase("L2GrandBoss"))
							cat.addDropData(drop, 2);
						else
							cat.addDropData(drop, 0);

						catExists = true;
						break;
					}
				if (!catExists)
				{
					L2DropCategory cat = new L2DropCategory(categoryType);
					
					if (_type.equalsIgnoreCase("L2RaidBoss"))
						cat.addDropData(drop, 1);
					else if (_type.equalsIgnoreCase("L2GrandBoss"))
						cat.addDropData(drop, 2);
					else
						cat.addDropData(drop, 0);

					_categories.add(cat);
				}
			}
		}
	}

	public void addRaidData(L2MinionData minion)
	{
		if (_minions == null)
			_minions = new FastList<L2MinionData>();
		_minions.add(minion);
	}

	public void addSkill(L2Skill skill)
	{
		if (_skills == null)
			_skills = new FastMap<Integer, L2Skill>();
		_skills.put(skill.getId(), skill);
	}

	public void addVulnerability(Stats id, double vuln)
	{
		if (_vulnerabilities == null)
			_vulnerabilities = new FastMap<Stats, Double>();
		_vulnerabilities.put(id, vuln);
	}

	public double getVulnerability(Stats id)
	{
		if (_vulnerabilities == null || _vulnerabilities.get(id) == null)
			return 1;
		return _vulnerabilities.get(id);
	}

	public double removeVulnerability(Stats id)
	{
		return _vulnerabilities.remove(id);
	}

	public List<L2DropCategory> getDropData()
	{
		return _categories;
	}

	public List<L2DropData> getAllDropData()
	{
		if (_categories == null)
			return null;
		FastList<L2DropData> lst = new FastList<L2DropData>();
		for (L2DropCategory tmp : _categories)
			lst.addAll(tmp.getAllDrops());
		return lst;
	}

	public synchronized void clearAllDropData()
	{
		if (_categories == null)
			return;
		while (!_categories.isEmpty())
		{
			_categories.get(0).clearAllDrops();
			_categories.remove(0);
		}
		_categories.clear();
	}

	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}

	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}

	public void addQuestEvent(Quest.QuestEventType EventType, Quest q)
	{
		if (_questEvents == null)
			_questEvents = new FastMap<Quest.QuestEventType, Quest[]>();

		if (_questEvents.get(EventType) == null)
		{
			_questEvents.put(EventType, new Quest[]
			{ q });
		}
		else
		{
			Quest[] _quests = _questEvents.get(EventType);
			int len = _quests.length;

			if (!EventType.isMultipleRegistrationAllowed())
			{
				if (_quests[0].getName().equals(q.getName()))
					_quests[0] = q;
				else
					_log.warn("Quest event not allowed in multiple quests.  Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + _name
							+ "\" and quest \"" + q.getName() + "\".");
			}
			else
			{
				Quest[] tmp = new Quest[len + 1];
				for (int i = 0; i < len; i++)
				{
					if (_quests[i].getName().equals(q.getName()))
					{
						_quests[i] = q;
						return;
					}
					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
		}
	}

	public Quest[] getEventQuests(Quest.QuestEventType EventType)
	{
		if (_questEvents == null)
			return null;
		return _questEvents.get(EventType);
	}

	public void setRace(int raceId)
	{
		switch (raceId)
		{
		case 1:
			_race = L2NpcTemplate.Race.UNDEAD;
			break;
		case 2:
			_race = L2NpcTemplate.Race.MAGICCREATURE;
			break;
		case 3:
			_race = L2NpcTemplate.Race.BEAST;
			break;
		case 4:
			_race = L2NpcTemplate.Race.ANIMAL;
			break;
		case 5:
			_race = L2NpcTemplate.Race.PLANT;
			break;
		case 6:
			_race = L2NpcTemplate.Race.HUMANOID;
			break;
		case 7:
			_race = L2NpcTemplate.Race.SPIRIT;
			break;
		case 8:
			_race = L2NpcTemplate.Race.ANGEL;
			break;
		case 9:
			_race = L2NpcTemplate.Race.DEMON;
			break;
		case 10:
			_race = L2NpcTemplate.Race.DRAGON;
			break;
		case 11:
			_race = L2NpcTemplate.Race.GIANT;
			break;
		case 12:
			_race = L2NpcTemplate.Race.BUG;
			break;
		case 13:
			_race = L2NpcTemplate.Race.FAIRIE;
			break;
		case 14:
			_race = L2NpcTemplate.Race.HUMAN;
			break;
		case 15:
			_race = L2NpcTemplate.Race.ELVE;
			break;
		case 16:
			_race = L2NpcTemplate.Race.DARKELVE;
			break;
		case 17:
			_race = L2NpcTemplate.Race.ORC;
			break;
		case 18:
			_race = L2NpcTemplate.Race.DWARVE;
			break;
		case 19:
			_race = L2NpcTemplate.Race.OTHER;
			break;
		case 20:
			_race = L2NpcTemplate.Race.NONLIVING;
			break;
		case 21:
			_race = L2NpcTemplate.Race.SIEGEWEAPON;
			break;
		case 22:
			_race = L2NpcTemplate.Race.DEFENDINGARMY;
			break;
		case 23:
			_race = L2NpcTemplate.Race.MERCENARIE;
			break;
		case 24:
			_race = L2NpcTemplate.Race.UNKNOWN;
			break;
		case 25:
			_race = L2NpcTemplate.Race.KAMAEL;
			break;
		default:
			_race = L2NpcTemplate.Race.NONE;
			break;
		}
	}

	public L2NpcTemplate.Race getRace()
	{
		if (_race == null)
			_race = L2NpcTemplate.Race.NONE;

		return _race;
	}
	
	public ru.catssoftware.gameserver.model.base.Race getNpcRace() {
		return _npcRace;
	}
	public int getNpcFaction()
	{
		return _npcFaction;
	}

	public void setNpcFaction(int npcFaction)
	{
		_npcFaction = npcFaction;
	}

	public String getNpcFactionName()
	{
		return _npcFactionName;
	}

	public int getAbsorbLevel()
	{
		return _absorbLevel;
	}

	public void setAbsorbLevel(int absorb_level)
	{
		_absorbLevel = absorb_level;
	}

	public AbsorbCrystalType getAbsorbType()
	{
		return _absorbType;
	}

	public void setAbsorbType(AbsorbCrystalType absorb_type)
	{
		_absorbType = absorb_type;
	}

	public int getAggroRange()
	{
		return _aggroRange;
	}

	public void setAggroRange(int aggroRange)
	{
		_aggroRange = aggroRange;
	}

	public int getArmor()
	{
		return _armor;
	}

	public void setArmor(int armor)
	{
		_armor = armor;
	}

	public String getFactionId()
	{
		return _factionId;
	}

	public void setFactionId(String factionId)
	{
		if (factionId == null)
		{
			_factionId = null;
			return;
		}
		_factionId = factionId.intern();
	}

	public int getFactionRange()
	{
		return _factionRange;
	}

	public void setFactionRange(int factionRange)
	{
		_factionRange = factionRange;
	}

	public int getIdTemplate()
	{
		return _idTemplate;
	}

	public void setIdTemplate(int idTemplate)
	{
		_idTemplate = idTemplate;
	}

	public byte getLevel()
	{
		return _level;
	}

	public void setLevel(byte level)
	{
		_level = level;
	}

	public int getLhand()
	{
		return _lhand;
	}

	public void setLhand(int lhand)
	{
		_lhand = lhand;
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public int getNpcId()
	{
		return _npcId;
	}

	public void setNpcId(int npcId)
	{
		_npcId = npcId;
	}

	public int getRewardExp()
	{
		return _rewardExp;
	}

	public void setRewardExp(int rewardExp)
	{
		_rewardExp = rewardExp;
	}

	public int getRewardSp()
	{
		return _rewardSp;
	}

	public void setRewardSp(int rewardSp)
	{
		_rewardSp = rewardSp;
	}

	public int getRhand()
	{
		return _rhand;
	}

	public void setRhand(int rhand)
	{
		_rhand = rhand;
	}

	public boolean isServerSideName()
	{
		return _serverSideName;
	}

	public void setServerSideName(boolean serverSideName)
	{
		_serverSideName = serverSideName;
	}

	public boolean isServerSideTitle()
	{
		return _serverSideTitle;
	}

	public void setServerSideTitle(boolean serverSideTitle)
	{
		_serverSideTitle = serverSideTitle;
	}

	public String getSex()
	{
		return _sex;
	}

	public void setSex(String sex)
	{
		_sex = sex;
	}

	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public String getType()
	{
		return _type;
	}

	public void setType(String type)
	{
		_type = type;
	}

	public void setNPCFactionName(String factionName)
	{
		_npcFactionName = (factionName == null ? "Devine Clan" : factionName);
	}

/*	public String getJClass()
	{
		return _jClass;
	}

	public void setJClass(String class1)
	{
		_jClass = class1;
	}
*/
	public short getSS()
	{
		return _ss;
	}

	public short getBSS()
	{
		return _bss;
	}

	public short getSSRate()
	{
		return _ssRate;
	}

	public AIType getAI()
	{
		return _ai;
	}

	public void setSS(short ss)
	{
		_ss = ss;
	}

	public void setBSS(short bss)
	{
		_bss = bss;
	}

	public void setSSRate(short ssrate)
	{
		_ssRate = ssrate;
	}

	public void setAI(AIType type)
	{
		_ai = type;
	}

	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	public boolean dropHerbs()
	{
		return _dropHerbs;
	}
	
	public boolean isCustom()
	{
		return _npcId != _idTemplate;
	}
}