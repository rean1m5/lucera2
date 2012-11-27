package ru.catssoftware.gameserver.model;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.instancemanager.CoupleManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.entity.Couple;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.skills.conditions.Condition;
import ru.catssoftware.gameserver.skills.effects.EffectTemplate;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;
import ru.catssoftware.gameserver.skills.funcs.FuncTemplate;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.lang.L2Integer;
import ru.catssoftware.util.LinkedBunch;
import ru.catssoftware.util.StatsSet;

import java.util.List;
import java.util.StringTokenizer;

public class L2Skill implements FuncOwner
{
	protected static Logger	_log						= Logger.getLogger(L2Skill.class.getName());

	public static final int	SKILL_CUBIC_MASTERY			= 143;
	public static final int	SKILL_LUCKY					= 194;
	public static final int	SKILL_CREATE_COMMON			= 1320;
	public static final int	SKILL_CREATE_DWARVEN		= 172;
	public static final int	SKILL_CRYSTALLIZE			= 248;
	public static final int	SKILL_DIVINE_INSPIRATION	= 1405;
	public static final int	SKILL_CLAN_LUCK				= 390;
	public static final int	SKILL_SOUL_MASTERY			= 467;

	public static final int	SKILL_FAKE_INT				= 9001;
	public static final int	SKILL_FAKE_WIT				= 9002;
	public static final int	SKILL_FAKE_MEN				= 9003;
	public static final int	SKILL_FAKE_CON				= 9004;
	public static final int	SKILL_FAKE_DEX				= 9005;
	public static final int	SKILL_FAKE_STR				= 9006;

	public static enum SkillOpType
	{
		OP_PASSIVE, OP_ACTIVE, OP_TOGGLE, OP_CHANCE
	}

	/** Target types of skills : SELF, PARTY, CLAN, PET... */
	public static enum SkillTargetType
	{
		TARGET_NONE,
		TARGET_SELF,
		TARGET_ONE,
		TARGET_PET,
		TARGET_SUMMON,
		TARGET_PARTY,
		TARGET_ALLY,
		TARGET_CLAN,
		TARGET_AREA,
		TARGET_FRONT_AREA,
		TARGET_BEHIND_AREA,
		TARGET_AURA,
		TARGET_FRONT_AURA,
		TARGET_BEHIND_AURA,
		TARGET_CORPSE,
		TARGET_CORPSE_ALLY,
		TARGET_CORPSE_CLAN,
		TARGET_CORPSE_PLAYER,
		TARGET_CORPSE_PET,
		TARGET_AREA_CORPSE_MOB,
		TARGET_CORPSE_MOB,
		TARGET_AREA_CORPSES,
		TARGET_MULTIFACE,
		TARGET_AREA_UNDEAD,
		TARGET_ITEM,
		TARGET_UNLOCKABLE,
		TARGET_HOLY,
		TARGET_FLAGPOLE,
		TARGET_PARTY_MEMBER,
		TARGET_PARTY_OTHER,
		TARGET_ENEMY_SUMMON,
		TARGET_OWNER_PET,
		TARGET_ENEMY_ALLY,
		TARGET_ENEMY_PET,
		TARGET_GATE,
		TARGET_COUPLE,
		TARGET_MOB,
		TARGET_AREA_MOB,
		TARGET_KNOWNLIST,
		TARGET_GROUND,
		TARGET_PIG
	}

	// elements
	public final static int			ELEMENT_FIRE			= 0;
	public final static int			ELEMENT_WATER			= 1;
	public final static int			ELEMENT_WIND			= 2;
	public final static int			ELEMENT_EARTH			= 3;
	public final static int			ELEMENT_HOLY			= 4;
	public final static int			ELEMENT_DARK			= 5;

	// conditional values
	public final static int			COND_RUNNING			= 0x0001;
	public final static int			COND_WALKING			= 0x0002;
	public final static int			COND_SIT				= 0x0004;
	public final static int			COND_BEHIND				= 0x0008;
	public final static int			COND_CRIT				= 0x0010;
	public final static int			COND_LOWHP				= 0x0020;
	public final static int			COND_ROBES				= 0x0040;
	public final static int			COND_CHARGES			= 0x0080;
	public final static int			COND_SHIELD				= 0x0100;
	public final static int			COND_GRADEA				= 0x010000;
	public final static int			COND_GRADEB				= 0x020000;
	public final static int			COND_GRADEC				= 0x040000;
	public final static int			COND_GRADED				= 0x080000;
	public final static int			COND_GRADES				= 0x100000;

	// these two build the primary key
	private final Integer			_id;
	private final int				_level;

	/** Identifier for a skill that client can't display */
	private final int				_displayId;

	private final int				_refId;

	// not needed, just for easier debug
	private final String			_name;
	private final String 			_attribute;

	private final SkillOpType		_operateType;
	private final boolean			_magic;
	private boolean					_itemSkill;
	private final boolean			_physic;
	private final boolean			_staticReuse;
	private final boolean			_staticHitTime;
	private final int				_mpConsume;
	private final int				_mpInitialConsume;
	private final int				_hpConsume;
	private final int				_cpConsume;

	private final int				_itemConsume;
	private final int				_itemConsumeId;
	// item consume count over time
	private final int				_itemConsumeOT;
	// item consume id over time
	private final int				_itemConsumeIdOT;
	// how many times to consume an item
	private final int				_itemConsumeSteps;

	private final int				_targetConsume;
	private final int				_targetConsumeId;

	// for summon spells:
	// a) What is the total lifetime of summons (in millisecs)
	private final int				_summonTotalLifeTime;
	// b) how much lifetime is lost per second of idleness (non-fighting)
	private final int				_summonTimeLostIdle;
	// c) how much time is lost per second of activity (fighting)
	private final int				_summonTimeLostActive;
	// item consume time in milliseconds
	private final int				_itemConsumeTime;
	private final boolean			_isCubic;
	private final int				_cubicSkillsLevel;
	private final boolean			_useAlways;

	private final int				_feed;
	private int						_effectLevel;
	// cubic AI
	private final int				_activationtime;
	private final int				_activationchance;

	private final int				_castRange;
	private final int				_effectRange;

	// Abnormal levels for skills and their canceling, e.g. poison vs negate
	private final int				_abnormalLvl; // e.g. poison or bleed lvl 2

	private final int				_negateLvl; // abnormalLvl is negated with negateLvl
	private final int				_negateId; // cancels the effect of skill ID
	private final String[]			_negateStats; // lists the effect types that are canceled
	private final int				_maxNegatedEffects; // maximum number of effects to negate

	private final int				_cancelId; // ID скила, который следует отменить при использовании скила
	// all times in milliseconds
	private final int				_hitTime;
	private final int				_skillInterruptTime;
	private final int				_coolTime;
	private final int				_reuseDelay;
	// for item skills delay on equip
	private final int				_equipDelay;

	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	private final SkillTargetType	_targetType;
	// base success chance
	private final double			_power;
	private final int				_levelDepend;

	// Kill by damage over time
	private final boolean			_killByDOT;

	// Скилл будет пулять по ХП, пропуская CP.
	private final boolean			_directHp;
	// Возможно ли снять эффекты этого скила.
	private final boolean			_isCancelable;

	// Effecting area of the skill, in radius.
	// The radius center varies according to the _targetType:
	// "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
	private final int				_skillRadius;

	private final L2SkillType		_skillType;
	private final L2SkillType		_effectType; // additional effect has a type
	private final int				_effectAbnormalLvl; // abnormal level for the additional effect type, e.g. poison lvl 1
	private final int				_effectPower;
	private final int				_effectId;
	private final float				_effectLvl; // normal effect level
	private final int				_skill_landing_percent;

	private final boolean			_ispotion;

	private final int				_element;
	private final int				_elementPower;

	private final boolean			_isSuicideAttack;
	private final int				_activateRate;
	private final int				_magicLevel;

	private final int				_condition;
	private final boolean			_overhit;
	private final boolean			_ignoreShld;
	private final int				_weaponsAllowed;
	private final int				_armorsAllowed;

	private final int				_addCrossLearn;							// -1 disable, otherwice SP price for others classes, default 1000
	private final float				_mulCrossLearn;							// multiplay for others classes, default 2
	private final float				_mulCrossLearnRace;						// multiplay for others races, default 2
	private final float				_mulCrossLearnProf;						// multiplay for fighter/mage missmatch, default 3
	private final FastList<ClassId>	_canLearn;									// which classes can learn
	private final FastList<Integer>	_teachers;									// which NPC teaches
	private final boolean			_isOffensive;
	private final boolean			_isNeutral;

	private final int				_needCharges;
	private final int				_giveCharges;
	private final int				_maxCharges;
	private final boolean			_consumeCharges;
	private final boolean			_continueAfterMax;

	private final int				_triggeredId;
	private final int				_triggeredLevel;
	private final int				_triggeredCount;

	private final boolean			_bestow;
	private final boolean			_bestowed;

	private final int				_soulConsume;
	private final int				_soulMaxConsume;
	private final int				_numSouls;
	private final int				_expNeeded;
	private final int				_critChance;

	private int						_duration;

	private final int				_baseCritRate;								// percent of success for skill critical hit (especially for PDAM & BLOW -
	// they're not affected by rCrit values or buffs). Default loads -1 for all
	// other skills but 0 to PDAM & BLOW
	private final int				_lethalEffect1;							// percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only
	// for PDAM skills)
	private final int				_lethalEffect2;							// percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only
	// for PDAM skills)
	private final boolean			_directHpDmg;								// If true then dmg is being make directly
	private final boolean			_isDance;									// If true then casting more dances will cost more MP
	private final boolean			_isSong;									// If true then casting more songs will cost more MP
	private final int				_nextDanceCost;
	private final float				_sSBoost;									// If true skill will have SoulShot boost (power*2)

	private final int				_timeMulti;

	private final boolean			_isAdvanced;								// Used by siege flag summon skills

	private final int				_minPledgeClass;

	private final int				_aggroPoints;

	protected Condition				_preCondition;	
	protected FuncTemplate[]		_funcTemplates;
	protected EffectTemplate[]		_effectTemplates;
	protected EffectTemplate[]		_effectTemplatesSelf;

	//Attached skills for Special Abilities
	protected L2Skill[]				_skillsOnCast;
	protected int[]					_skillsOnCastId, _skillsOnCastLvl;
	protected int					timesTriggered			= 1;

	protected ChanceCondition		_chanceCondition;

	// Flying support
	private final String			_flyType;
	private final int				_flyRadius;
	private final float				_flyCourse;

	private final boolean			_isDebuff;
	private final boolean 			_canBeReflected;
	
	private final int				_lethalType;

	public L2Skill(StatsSet set)
	{
		_id = L2Integer.valueOf(set.getInteger("skill_id"));
		_level = set.getInteger("level");
		
		_attribute = set.getString("attribute","");

		_displayId = set.getInteger("displayId", _id);
		_name = set.getString("name").intern();
		if(set.getSet().containsKey("effectLevel"))
			_effectLevel = set.getInteger("effectLevel");
		else 
			_effectLevel = 0;
		_skillType = set.getEnum("skillType", L2SkillType.class);
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_targetType = set.getEnum("target", SkillTargetType.class);
		_magic = set.getBool("isMagic", isSkillTypeMagic());
		_itemSkill = set.getBool("isItem", false);
		_physic = set.getBool("isPhysic", false);
		_ispotion = set.getBool("isPotion", false);
		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);
		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_cpConsume = set.getInteger("cpConsume", 0);
		_itemConsume = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);
		_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000); // 20 minutes default
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);
		_isCubic = set.getBool("isCubic", false);
		_cubicSkillsLevel = set.getInteger("cubicSkillsLevel", 0);
		_activationtime = set.getInteger("activationtime", 8);
		_activationchance = set.getInteger("activationchance", 30);

		_refId = set.getInteger("referenceId", _itemConsumeId);

		_castRange = set.getInteger("castRange", 0);
		_effectRange = set.getInteger("effectRange", -1);

		_abnormalLvl = set.getInteger("abnormalLvl", -1);
		_effectAbnormalLvl = set.getInteger("effectAbnormalLvl", -1); // support for a separate effect abnormal lvl, e.g. poison inside a different skill
		_negateLvl = set.getInteger("negateLvl", -1);
		String[] negateStats = set.getString("negateStats", "").split(" ");
		if (negateStats.length > 0)
		{
			FastList<String> stats = new FastList<String>();
			for (String stat : negateStats)
				stats.add(stat.toLowerCase().intern());
			_negateStats = stats.toArray(new String[stats.size()]);
		}
		else
			_negateStats = negateStats;
		_negateId = set.getInteger("negateId", 0);
		_maxNegatedEffects = set.getInteger("maxNegated", 0);

		_cancelId = set.getInteger("cancelId", 0);

		_killByDOT = set.getBool("killByDOT", false);

		_hitTime = set.getInteger("hitTime", 0);
		_coolTime = set.getInteger("coolTime", 0);
		_skillInterruptTime = isMagic() ? getHitTime() / 2 : 0;
		_reuseDelay = set.getInteger("reuseDelay", 0);
		_equipDelay = set.getInteger("equipDelay", 0);

		_isDance = set.getBool("isDance", false);
		_isSong = set.getBool("isSong", false);

		_chMagic = set.getBool("isChMagic", false);
		_heroMagic = set.getBool("isHeroMagic", false);
		_bufferMagic = set.getBool("isBufferMagic", false);
		_is5MinMagic = set.getBool("is5MinMagic", false);
		_useAlways = set.getBool("useAlways",false);

		_directHp = set.getBool("directHp", false);
		_isCancelable = set.getBool("isCancelable", true);

		//if is dance magic adds the proper time multi
		if (_isDance)
			_timeMulti = Config.ALT_DANCE_TIME;

		//if is dance song adds the proper time multi
		else if (_isSong)
			_timeMulti = Config.ALT_SONG_TIME;

		//if is a buffer magic adds the proper time multi
		else if (_bufferMagic)
			_timeMulti = Config.ALT_BUFFER_TIME;

		//if is a clan hall buffer magic adds the proper time multi
		else if (_chMagic)
			_timeMulti = Config.ALT_CH_TIME;

		//if is a hero magic adds the proper time multi
		else if (_heroMagic)
			_timeMulti = Config.ALT_HERO_TIME;

		//if it is a efefct of 4 minuts or less add proper multi
		else if (_is5MinMagic)
			_timeMulti = Config.ALT_5MIN_TIME;

		//if none skill is non eof before cases set time multi to 0.
		else
			_timeMulti = 1; // we dont need to add any additional timer here.

		_skillRadius = set.getInteger("skillRadius", 80);

		_power = _id != 2005 ? set.getFloat("power", 0.f): Config.MANAHEAL_POWER;

		_levelDepend = set.getInteger("lvlDepend", 0);

		_isAdvanced = set.getBool("isAdvanced", false); // Used by siege flag summon skills
		_isDebuff = set.getBool("isDebuff", false);
		_feed = set.getInteger("feed", 0); // Used for pet food

		_effectType = set.getEnum("effectType", L2SkillType.class, null);
		_effectPower = set.getInteger("effectPower", 0);
		_effectId = set.getInteger("effectId", 0);
		_effectLvl = set.getFloat("effectLevel", 0.f);
		_skill_landing_percent = set.getInteger("skill_landing_percent", 0);

		_element = set.getInteger("element", -1);
		_elementPower = set.getInteger("elementPower", 20);
		
		_activateRate = set.getInteger("activateRate", -1);
		_magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(_id, _level));

		_ignoreShld = set.getBool("ignoreShld", false);
		_condition = set.getInteger("condition", 0);
		_overhit = set.getBool("overHit", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		_weaponsAllowed = set.getInteger("weaponsAllowed", 0);
		_armorsAllowed = set.getInteger("armorsAllowed", 0);

		_addCrossLearn = set.getInteger("addCrossLearn", 1000);
		_mulCrossLearn = set.getFloat("mulCrossLearn", 2.f);
		_mulCrossLearnRace = set.getFloat("mulCrossLearnRace", 2.f);
		_mulCrossLearnProf = set.getFloat("mulCrossLearnProf", 3.f);
		_isOffensive = set.getBool("offensive", isSkillTypeOffensive());
		_isNeutral = set.getBool("neutral", false);

		_needCharges = set.getInteger("needCharges", 0);
		_giveCharges = set.getInteger("giveCharges", 0);
		_maxCharges = set.getInteger("maxCharges", 0);
		_consumeCharges = set.getBool("consumeCharges", true);
		_continueAfterMax = set.getBool("continueAfterMax", false);

		_minPledgeClass = set.getInteger("minPledgeClass", 0);

		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 1);
		int triggeredCount = set.getInteger("triggeredCount", 1);

		if (_triggeredId == 0) // no triggered skill
			_triggeredCount = 0; // so set count to zero
		else if (triggeredCount == 0) // there is a skill, but count is zero
			_triggeredCount = 1; // then set it to one
		else
			// there is a skill, and the count is valid
			_triggeredCount = triggeredCount; // so just set it

		if (_operateType == SkillOpType.OP_CHANCE)
			_chanceCondition = ChanceCondition.parse(set);

		_bestow = set.getBool("bestowTriggered", false);
		_bestowed = set.getBool("bestowed", false);

		_numSouls = set.getInteger("num_souls", 0);
		_soulConsume = set.getInteger("soulConsumeCount", 0);
		_soulMaxConsume = set.getInteger("soulMaxConsumeCount", 0);
		_expNeeded = set.getInteger("expNeeded", 0);
		_critChance = set.getInteger("critChance", 0);

		_duration = set.getInteger("duration", 0);
		_baseCritRate = set.getInteger("baseCritRate", (_skillType == L2SkillType.PDAM || _skillType == L2SkillType.BLOW) ? 0 : -1);
		_lethalEffect1 = set.getInteger("lethal1", 0);
		_lethalEffect2 = set.getInteger("lethal2", 0);
		_lethalType = set.getInteger("LethalType", 1); // 1 - Other class, 2 - Dagger class, 3 - Archery class
		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_nextDanceCost = set.getInteger("nextDanceCost", 0);
		_canBeReflected = set.getBool("canBeReflected", true);
		_sSBoost = set.getFloat("SSBoost", 1.f);

		_aggroPoints = set.getInteger("aggroPoints", 0);

		_flyType = set.getString("flyType", null);
		_flyRadius = set.getInteger("flyRadius", 200);
		_flyCourse = set.getFloat("flyCourse", 0);

		String canLearn = set.getString("canLearn", null);
		if (canLearn == null)
		{
			_canLearn = null;
		}
		else
		{
			_canLearn = new FastList<ClassId>();
			StringTokenizer st = new StringTokenizer(canLearn, " \r\n\t,;");
			while (st.hasMoreTokens())
			{
				String cls = st.nextToken();
				try
				{
					_canLearn.add(ClassId.valueOf(cls));
				}
				catch (Exception e)
				{
					_log.fatal("Bad class " + cls + " to learn skill", e);
				}
			}
		}

		String teachers = set.getString("teachers", null);
		if (teachers == null)
		{
			_teachers = null;
		}
		else
		{
			_teachers = new FastList<Integer>();
			StringTokenizer st = new StringTokenizer(teachers, " \r\n\t,;");
			while (st.hasMoreTokens())
			{
				String npcid = st.nextToken();
				try
				{
					_teachers.add(Integer.parseInt(npcid));
				}
				catch (Exception e)
				{
					_log.fatal("Bad teacher id " + npcid + " to teach skill", e);
				}
			}
		}
	}

	public void useSkill(L2Character caster, L2Character... targets)
	{
		caster.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (caster.isPlayer())
			caster.sendMessage(String.format(Message.getMessage((L2PcInstance) caster, Message.MessageId.MSG_SKILL_NOT_IMPLEMENTED), getId()));
	}

	public final boolean isPotion()
	{
		return _ispotion;
	}

	public final int getEffectLevel() {
		return _effectLevel;
	}
	public final int getArmorsAllowed()
	{
		return _armorsAllowed;
	}

	public final L2SkillType getSkillType()
	{
		return _skillType;
	}

	public final int getActivateRate()
	{
		return _activateRate;
	}

	public final int getMagicLevel()
	{
		return _magicLevel;
	}

	public final int getElement()
	{
		return _element;
	}

	public int getElementPower()
	{
		return _elementPower;
	}
	
	/**
	 * Return the target type of the skill : SELF, PARTY, CLAN, PET...<BR>
	 * <BR>
	 */
	public final SkillTargetType getTargetType()
	{
		return _targetType;
	}

	public final int getCondition()
	{
		return _condition;
	}

	public final boolean ignoreShld()
	{
		return _ignoreShld;
	}

	public final boolean isOverhit()
	{
		return _overhit;
	}

	public final boolean killByDOT()
	{
		return _killByDOT;
	}

	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}

	/**
	 * Return the power of the skill.<BR>
	 * <BR>
	 */
	public final double getPower(L2Character activeChar)
	{
		return _power;
	}

	public final double getPower()
	{
		return _power;
	}

	public final String[] getNegateStats()
	{
		return _negateStats;
	}

	public final int getAbnormalLvl()
	{
		return _abnormalLvl;
	}

	public final int getNegateLvl()
	{
		return _negateLvl;
	}

	public final int getNegateId()
	{
		return _negateId;
	}

	public final int getCancelId()
	{
		return _cancelId;
	}

	public final int getMaxNegatedEffects()
	{
		return _maxNegatedEffects;
	}

	public final int getMagicLvl()
	{
		return _magicLevel;
	}

	public final int getEffectAbnormalLvl()
	{
		return _effectAbnormalLvl;
	}

	public int getTriggeredId()
	{
		return _triggeredId;
	}

	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}

	public int getTriggeredCount()
	{
		return _triggeredCount;
	}

	public boolean bestowTriggered()
	{
		return _bestow;
	}

	public boolean bestowed()
	{
		return _bestowed;
	}

	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}

	public L2Skill getTriggeredSkill()
	{
		return SkillTable.getInstance().getInfo(_triggeredId, _triggeredLevel); // is there any skill with bigger level than one?! :$
	}

	public final int getLevelDepend()
	{
		return _levelDepend;
	}

	/**
	 * Return the skill landing percent probability.<BR>
	 * <BR>
	 */
	public final int getLandingPercent()
	{
		return _skill_landing_percent;
	}

	/**
	 * Return the additional effect power or base probability.<BR>
	 * <BR>
	 */
	public final int getEffectPower()
	{
		return _effectPower;
	}

	/**
	* Return the additional effect Id.<BR><BR>
	*/
	public final int getEffectId()
	{
		return _effectId;
	}

	/**
	 * Return the additional effect level.<BR>
	 * <BR>
	 */
	public final float getEffectLvl()
	{
		return _effectLvl;
	}

	/**
	 * Return the additional effect skill type (ex : STUN, PARALYZE,...).<BR>
	 * <BR>
	 */
	public final L2SkillType getEffectType()
	{
		return _effectType;
	}

	/**
	 * @return Returns the timeMulti.
	 */
	public final int getTimeMulti()
	{
		return _timeMulti;
	}

	/**
	 * @return Returns the castRange.
	 */
	public final int getCastRange()
	{
		return _castRange;
	}

	/**
	 * @return Returns the effectRange.
	 */
	public final int getEffectRange()
	{
		return _effectRange;
	}

	/**
	 * @return Returns the hitTime.
	 */
	public final int getHitTime()
	{
		return _hitTime;
	}

	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return _hpConsume;
	}

	/**
	 * @return Returns the cpConsume.
	 */
	public final int getCpConsume()
	{
		return _cpConsume;
	}

	/**
	 * @return Returns the id.
	 */
	public final Integer getId()
	{
		return _id;
	}

	public int getDisplayId()
	{
		return _displayId;
	}

	public int getReferenceItemId()
	{
		return _refId;
	}

	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}

	/**
	* @return Returns the _targetConsumeId.
	*/
	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}

	/**
	* @return Returns the targetConsume.
	*/
	public final int getTargetConsume()
	{
		return _targetConsume;
	}

	/**
	 * @return Returns the itemConsume.
	 */
	public final int getItemConsume()
	{
		return _itemConsume;
	}

	/**
	 * @return Returns the itemConsumeId.
	 */
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	/**
	 * @return Returns the itemConsume count over time.
	 */
	public final int getItemConsumeOT()
	{
		return _itemConsumeOT;
	}

	/**
	 * @return Returns the itemConsumeId over time.
	 */
	public final int getItemConsumeIdOT()
	{
		return _itemConsumeIdOT;
	}

	/**
	 * @return Returns the itemConsume count over time.
	 */
	public final int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}

	/**
	 * @return Returns the itemConsume count over time.
	 */
	public final int getTotalLifeTime()
	{
		return _summonTotalLifeTime;
	}

	/**
	 * @return Returns the itemConsume count over time.
	 */
	public final int getTimeLostIdle()
	{
		return _summonTimeLostIdle;
	}

	/**
	 * @return Returns the itemConsumeId over time.
	 */
	public final int getTimeLostActive()
	{
		return _summonTimeLostActive;
	}

	public final boolean isCubic()
	{
		return _isCubic;
	}

	public final int getCubicSkillLevel()
	{
		return _cubicSkillsLevel;
	}	
	/**
	 * @return Returns the itemConsume time in milliseconds.
	 */
	public final int getItemConsumeTime()
	{
		return _itemConsumeTime;
	}

	/**
	* @return Returns the activation time for a cubic.
	*/
	public final int getActivationTime()
	{
		return _activationtime;
	}

	/**
	 * @return Returns the activation chance for a cubic.
	 */
	public final int getActivationChance()
	{
		return _activationchance;
	}

	/**
	 * @return Returns the level.
	 */
	public final int getLevel()
	{
		return _level;
	}

	/**
	 * @return Returns the magic.
	 */
	public final boolean isMagic()
	{
		return _magic;
	}

	public final void markAsItemSkill() {
		_itemSkill = true;
	}
	public final boolean isItemSkill()
	{
		return _itemSkill;
	}

	/**
	 * @return Returns if the skill is Physical.
	 */
	public final boolean isPhysical()
	{
		return _physic;
	}

	/**
	 * @return Returns true to set static reuse.
	 */
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}

	/**
	 * @return Returns true to set static hittime.
	 */
	public final boolean isStaticHitTime()
	{
		return _staticHitTime;
	}

	/**
	 * @return Returns the mpConsume.
	 */
	public final int getMpConsume()
	{
		return _mpConsume;
	}

	/**
	 * @return Returns the mpInitialConsume.
	 */
	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}

	/**
	 * @return Returns the name.
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * @return Returns the reuseDelay.
	 */
	public final int getReuseDelay()
	{
		return _reuseDelay;
	}

	public final int getEquipDelay()
	{
		return _equipDelay;
	}

	public final int getCoolTime()
	{
		return _coolTime;
	}

	public final int getSkillInterruptTime()
	{
		return _skillInterruptTime;
	}

	public final int getSkillRadius()
	{
		return _skillRadius;
	}

	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}

	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}

	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}

	public final boolean isChance()
	{
		return _operateType == SkillOpType.OP_CHANCE;
	}

	public ChanceCondition getChanceCondition()
	{
		return _chanceCondition;
	}

	public final boolean isDance()
	{
		return _isDance;
	}

	public final boolean isSong()
	{
		return _isSong;
	}

	public final int getNextDanceMpCost()
	{
		return _nextDanceCost;
	}

	public final boolean isAdvanced()
	{
		return _isAdvanced;
	}

	/**
	*@return Returns the boolean _isDebuff.
	*/
	public final boolean isDebuff()
	{
		return _isDebuff;
	}

	public final float getSSBoost()
	{
		return _sSBoost;
	}

	public final int getAggroPoints()
	{
		return _aggroPoints;
	}

	public final boolean useSoulShot()
	{
		boolean result = false;

		if ((!isMagic()) && (getSkillType() != L2SkillType.PUMPING) || (getSkillType() != L2SkillType.REELING))
			result = true;
		return result;
	}

	public final boolean useSpiritShot()
	{
		return isMagic();
	}

	public final boolean useFishShot()
	{
		return ((getSkillType() == L2SkillType.PUMPING) || (getSkillType() == L2SkillType.REELING));
	}

	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}

	public final int getCrossLearnAdd()
	{
		return _addCrossLearn;
	}

	public final float getCrossLearnMul()
	{
		return _mulCrossLearn;
	}

	public final float getCrossLearnRace()
	{
		return _mulCrossLearnRace;
	}

	public final float getCrossLearnProf()
	{
		return _mulCrossLearnProf;
	}

	public final boolean getCanLearn(ClassId cls)
	{
		return _canLearn == null || _canLearn.contains(cls);
	}

	public final boolean canTeachBy(int npcId)
	{
		return _teachers == null || _teachers.contains(npcId);
	}

	public final boolean isPvpSkill()
	{
		switch (_skillType)
		{
			case DOT:
			case BLEED:
			case CONFUSION:
			case POISON:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case FEAR:
			case SLEEP:
			case MDOT:
			case MANADAM:
			case MUTE:
			case WEAKNESS:
			case PARALYZE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case CANCEL_TARGET:
			case BETRAY:
			case DISARM:
			case STEAL_BUFF:
			case BAD_BUFF:
			case AGGDAMAGE:
			case DELUXE_KEY_UNLOCK:
			case FATALCOUNTER:
			case MAKE_KILLABLE:
			case AGGREDUCE_CHAR: //L2CatsSoftware fix
				return true;
			default:
				return false;
		}
	}

	public final boolean isOffensive()
	{
		return _isOffensive;
	}

	public final boolean isBadBuff()
	{
		switch (_skillType)
		{
			case BAD_BUFF:
				return true;
			default:
				return false;
		}
	}

	public final boolean isNeutral()
	{
		return _isNeutral;
	}

	public final int getNeededCharges()
	{
		return _needCharges;
	}

	public boolean getConsumeCharges()
	{
		return _consumeCharges;
	}

	public boolean getContinueAfterMax()
	{
		return _continueAfterMax;
	}

	public final int getGiveCharges()
	{
		return _giveCharges;
	}

	public final int getMaxCharges()
	{
		return _maxCharges;
	}

	public final int getNumSouls()
	{
		return _numSouls;
	}

	public final int getMaxSoulConsumeCount()
	{
		return _soulMaxConsume;
	}
	
	public final int getSoulConsumeCount()
	{
		return _soulConsume;
	}

	public final int getExpNeeded()
	{
		return _expNeeded;
	}

	public final int getCritChance()
	{
		return _critChance;
	}

	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}

	public final int getLethalChance1()
	{
		return _lethalEffect1;
	}

	public final int getLethalChance2()
	{
		return _lethalEffect2;
	}

	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}

	public final int getFeed()
	{
		return _feed;
	}

	public final String getFlyType()
	{
		return _flyType;
	}

	public final int getFlyRadius()
	{
		return _flyRadius;
	}

	public final float getFlyCourse()
	{
		return _flyCourse;
	}

	public final int getDuration()
	{
		return _duration;
	}

	public final void setDuration(int dur)
	{
		_duration = dur;
	}

	public final static boolean skillLevelExists(int skillId, int level)
	{
		return SkillTable.getInstance().getInfo(skillId, level) != null;
	}

	public final boolean isSkillTypeMagic()
	{
		switch (getSkillType())
		{
			case MDAM:
			case HEAL:
			case SUMMON_FRIEND:
			case BALANCE_LIFE:
				return true;
			default:
				return false;
		}
	}
	
	public final boolean isSkillTypeOffensive()
	{
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
			case CPDAM:
			case DOT:
			case BLEED:
			case POISON:
			case AGGDAMAGE:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case CONFUSION:
			case ERASE:
			case BLOW:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case CHARGEDAM:
			case CONFUSE_MOB_ONLY:
			case DEATHLINK:
			case FATALCOUNTER:
			case DETECT_WEAKNESS:
			case MDOT:
			case MANADAM:
			case MUTE:
			case SOULSHOT:
			case SPIRITSHOT:
			case SPOIL:
			case WEAKNESS:
			case MANA_BY_LEVEL:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case UNSUMMON_ENEMY_PET:
			case CANCEL_TARGET:
			case BETRAY:
			case SOW:
			case HARVEST:
			case DISARM:
			case STEAL_BUFF:
			case BAD_BUFF:
			case INSTANT_JUMP:
				return true;
			default:
				return this.isDebuff();
		}
	}

	public final boolean isPositive()
	{
		switch (_skillType)
		{
			case BUFF:
			case HEAL:
			case HEAL_PERCENT:
			case HOT:
			case MANAHEAL:
			case MANARECHARGE:
			case COMBATPOINTHEAL:
			case COMBATPOINTPERCENTHEAL:
			case REFLECT:
			case SHIFT_TARGET:
			case DEATH_PENALTY:
				return true;
			default:
				return false;
		}
	}

	public final boolean isNeedWeapon()
	{
		return (_skillType == L2SkillType.MDAM);
	}

	private String _weaponDependancyMessage;

	public final boolean getWeaponDependancy(L2Character activeChar, boolean message)
	{
		int weaponsAllowed = getWeaponsAllowed();
		if (weaponsAllowed == 0)
			return true;

		L2Weapon weapon = activeChar.getActiveWeaponItem();
		if (weapon != null && (weapon.getItemType().mask() & weaponsAllowed) != 0)
			return true;

		L2Weapon weapon2 = activeChar.getSecondaryWeaponItem();
		if (weapon2 != null && (weapon2.getItemType().mask() & weaponsAllowed) != 0)
			return true;

		if (message)
		{
			if (_weaponDependancyMessage == null)
			{
				StringBuilder sb = new StringBuilder();
				for (L2WeaponType wt : L2WeaponType.VALUES)
				{
					if ((wt.mask() & weaponsAllowed) != 0)
					{
						if (sb.length() != 0)
							sb.append('/');

						sb.append(wt);
					}
				}
				sb.append(".");

				_weaponDependancyMessage = getName() + " " + Message.getMessage((L2PcInstance)activeChar, Message.MessageId.MSG_WEAPON_MAY_USE_WITH) + " " + sb.toString();
				_weaponDependancyMessage = _weaponDependancyMessage.intern();
			}

			activeChar.sendMessage(_weaponDependancyMessage);
		}

		return false;
	}

	public final boolean ownedFuncShouldBeDisabled(L2Character activeChar)
	{
		if (isOffensive())
			return false;

		return !(isDance() || isSong()) && !getWeaponDependancy(activeChar, false);

	}

	public boolean checkCondition(L2Character activeChar, L2Object target)
	{
		Condition preCondition = _preCondition;
		if (preCondition == null)
			return true;

		Env env = new Env();
		env.player = activeChar;
		if (target instanceof L2Character)
			env.target = (L2Character) target;
		env.skill = this;

		if (preCondition.test(env))
			return true;

		if (activeChar.isPlayer())
			preCondition.sendMessage((L2PcInstance) activeChar, this);
		return false;
	}

	public final L2Character[] getTargetList(L2Character activeChar, boolean onlyFirst)
	{
		// Init to null the target of the skill
		L2Character target = null;

		// Get the L2Objcet targeted by the user of the skill at this moment
		L2Object objTarget = activeChar.getTarget();
		// If the L2Object targeted is a L2Character, it becomes the L2Character target
		if (objTarget != null && objTarget instanceof L2Character)
		{
			target = (L2Character) objTarget;
		}

		return getTargetList(activeChar, onlyFirst, target);
	}

	public final L2Character[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		LinkedBunch<L2Character> targetList = new LinkedBunch<L2Character>();

		try {
		// Get the target type of the skill
		// (ex : ONE, SELF, HOLY, PET, AURA, AURA_CLOSE, AREA, MULTIFACE, PARTY, CLAN, CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN, UNLOCKABLE, ITEM, UNDEAD)
		SkillTargetType targetType = getTargetType();

		// Get the type of the skill
		// (ex : PDAM, MDAM, DOT, BLEED, POISON, HEAL, HOT, MANAHEAL, MANARECHARGE, AGGDAMAGE, BUFF, DEBUFF, STUN, ROOT, RESURRECT, PASSIVE...)
		L2SkillType skillType = getSkillType();

		switch (targetType)
		{
			// The skill can only be used on the L2Character targeted, or on the caster itself
			case TARGET_ONE:
			{
				// automaticly selects caster if no target is selected (only positive skills)
				if (isPositive() && target == null)
					target = activeChar;

				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
					case CANCEL:
					case CANCEL_DEBUFF:
					case REFLECT:
					case COMBATPOINTHEAL:
					case COMBATPOINTPERCENTHEAL:
					case MAGE_BANE:
					case WARRIOR_BANE:
					case BETRAY:
					case BALANCE_LIFE:
						canTargetSelf = true;
						break;
				}

				// Check for null target or any other invalid target
				if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}
				if (target!=activeChar && !activeChar.canSee(target))
					return null;
				return new L2Character[] { target };
			}
			case TARGET_SELF:
			case TARGET_GROUND:
			{
				return new L2Character[] { activeChar };
			}
			case TARGET_HOLY:
			{
				if (target != null && activeChar.isPlayer())
				{
					if (target instanceof L2ArtefactInstance)
						return new L2Character[] { target };
				}

				return null;
			}
			case TARGET_FLAGPOLE:
			{
				return new L2Character[] { activeChar };
			}
			case TARGET_COUPLE:
			{
				if (target != null && target.isPlayer())
				{
					int _chaid = activeChar.getObjectId();
					int targetId = target.getObjectId();
					for (Couple cl : CoupleManager.getInstance().getCouples())
					{
						if ((cl.getPlayer1Id() == _chaid && cl.getPlayer2Id() == targetId) || (cl.getPlayer2Id() == _chaid && cl.getPlayer1Id() == targetId))
							return new L2Character[] { target };
					}
				}
				return null;
			}
			case TARGET_PET:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead())
					return new L2Character[] { target };
				return null;
			}
			case TARGET_SUMMON:
			{
				target = activeChar.getPet(); 
				if (target != null && !target.isDead() && target instanceof L2SummonInstance)
					return new L2Character[] {target};
				return null;
			}
			case TARGET_OWNER_PET:
			{
				if (activeChar instanceof L2Summon)
				{
					target = ((L2Summon) activeChar).getOwner();
					if (target != null && !target.isDead())
						return new L2Character[] { target };
				}
				return null;
			}
			case TARGET_ENEMY_PET:
			{
				if (target != null && target instanceof L2Summon)
				{
					L2Summon targetPet;
					targetPet = (L2Summon) target;
					if (activeChar.isPlayer() && activeChar.getPet() != targetPet && !targetPet.isDead() && targetPet.getOwner().getPvpFlag() != 0)
					{
						return new L2Character[] { target };
					}
				}
				return null;
			}
			case TARGET_CORPSE_PET:
			{
				if (activeChar.isPlayer())
				{
					target = activeChar.getPet();
					if (target != null && target.isDead())
					{
						return new L2Character[] { target };
					}
				}
				return null;
			}
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			{
				int radius = getSkillRadius();
				boolean srcInPvP = activeChar.isInsideZone(L2Zone.FLAG_PVP) && !activeChar.isInsideZone(L2Zone.FLAG_SIEGE);

				L2PcInstance src = activeChar.getPlayer();
				
				for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					boolean checkPeace = true;

					if (!activeChar.canSee(obj))
						continue;

					try
					{
						if (obj.isPlayer())
						{
							if (obj.getPlayer().inObserverMode())
								continue;

							if (obj.getPlayer().isGM() && obj != activeChar)
								continue;

							if (src != null && src.getParty() != null && src.getParty().getMemberById(obj.getObjectId())!=null)
								continue;
						}

						if(obj.isCharacter() && activeChar.isInFunEvent())
						{
							if(!activeChar.getGameEvent().canBeSkillTarget(activeChar, obj.getCharacter(), this))
								continue;

							if(obj.getCharacter().getGameEvent() != null && obj.getCharacter().getGameEvent().equals(activeChar.getGameEvent()))
								checkPeace = false;
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						continue;
					}
					if (obj instanceof L2Attackable || obj instanceof L2PlayableInstance)
					{
						L2Character cha = (L2Character) obj;
						boolean targetInPvP = cha.isInsideZone(L2Zone.FLAG_PVP) && !cha.isInsideZone(L2Zone.FLAG_SIEGE);

						if (obj == activeChar || obj == src || ((L2Character) obj).isDead())
							continue;
	
						if (src != null)
						{
							switch (targetType)
							{
								case TARGET_FRONT_AURA:
									if (!cha.isInFrontOf(activeChar))
										continue;
									break;
								case TARGET_BEHIND_AURA:
									if (!cha.isBehind(activeChar))
										continue;
									break;
							}

							if (!activeChar.canSee(obj))
								continue;

							if (obj.isPlayer())
							{
								if (!src.checkPvpSkill(obj, this))
									continue;
								
								if(src.isInOlympiadMode() && !src.isOlympiadStart())
									continue;
								if (checkPeace && ((L2PcInstance) obj).isInsideZone(L2Zone.FLAG_PEACE) && !src.allowPeaceAttack())
									continue;
								if (!((L2PcInstance) obj).isVisible())
									continue;
								if ((src.getParty() != null && ((L2PcInstance) obj).getParty() != null)
										&& src.getParty().getPartyLeaderOID() == ((L2PcInstance) obj).getParty().getPartyLeaderOID())
									continue;
								if (!srcInPvP && !targetInPvP)
								{
									if (src.getAllyId() == ((L2PcInstance) obj).getAllyId() && src.getAllyId() != 0)
										continue;
									if (src.getClanId() != 0 && src.getClanId() == ((L2PcInstance) obj).getClanId())
										continue;
								}
							}
							else if (obj instanceof L2Summon)
							{
								L2PcInstance trg = ((L2Summon) obj).getOwner();
								if (trg == src)
									continue;
								if(src.isInOlympiadMode() && !src.isOlympiadStart())
									continue;
								if (!src.checkPvpSkill(trg, this))
									continue;
								if (checkPeace &&  ((L2Summon) obj).isInsideZone(L2Zone.FLAG_PEACE) && !src.allowPeaceAttack())
									continue;
								if (!((L2Summon) obj).isVisible())
									continue;
								if ((src.getParty() != null && trg.getParty() != null) && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
									continue;
								if (!srcInPvP && !targetInPvP)
								{
									if (src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
										continue;
									if (src.getClanId() != 0 && src.getClanId() == trg.getClanId())
										continue;
								}
							}
						}
						else
						{
							if (!(obj instanceof L2PlayableInstance) && !activeChar.isConfused())
								continue;
						}
						if (!Util.checkIfInRange(radius, activeChar, obj, true))
							continue;

						if (!onlyFirst)
							targetList.add((L2Character) obj);
						else
							return new L2Character[]
							{ (L2Character) obj };
					}
				}
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA:
			case TARGET_FRONT_AREA:
			case TARGET_BEHIND_AREA:
			{
				if ((!(target instanceof L2Attackable || target instanceof L2PlayableInstance)) || (getCastRange() >= 0 && (target == activeChar || target.isAlikeDead())))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}

				L2Character cha;
				if (getCastRange() >= 0)
				{
					cha = target;

					if (!onlyFirst)
						targetList.add(cha);
					else
						return new L2Character[] { cha };
				}
				else
					cha = activeChar;

				boolean effectOriginIsL2PlayableInstance = (cha instanceof L2PlayableInstance);
				boolean srcIsSummon = (activeChar instanceof L2Summon);

				L2PcInstance src = activeChar.getPlayer();

				int radius = getSkillRadius();

				boolean srcInPvP = activeChar.isInsideZone(L2Zone.FLAG_PVP) && !activeChar.isInsideZone(L2Zone.FLAG_SIEGE);
				
				for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					boolean checkPeace  = true;
					if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance))
						continue;
					if (obj == cha)
						continue;
					target = (L2Character) obj;
					if(obj instanceof  L2Character)
						if(activeChar.getGameEvent()!=null && activeChar.getGameEvent().isRunning())
							if (!activeChar.getGameEvent().canBeSkillTarget(activeChar, (L2Character)obj, this))
								continue;
							else
								if(activeChar.getGameEvent()!=null && activeChar.getGameEvent().isRunning() && ((L2Character)obj).getGameEvent()!=null && ((L2Character)obj).getGameEvent().isRunning())
									checkPeace = false;

					boolean targetInPvP = target.isInsideZone(L2Zone.FLAG_PVP) && !target.isInsideZone(L2Zone.FLAG_SIEGE);

					if (!activeChar.canSee(target))
						continue;

					if (!target.isDead() && (target != activeChar))
					{
						if (!Util.checkIfInRange(radius, obj, cha, true))
							continue;

						if (src != null)
						{
							switch (targetType)
							{
								case TARGET_FRONT_AREA:
									if (!cha.isInFrontOf(activeChar))
										continue;
									break;
								case TARGET_BEHIND_AREA:
									if (!cha.isBehind(activeChar))
										continue;
									break;
							}

							if (obj.isPlayer())
							{
								L2PcInstance trg = (L2PcInstance) obj;
								if (trg == src)
									continue;
								if ((src.getParty() != null && trg.getParty() != null) && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
									continue;

								if(!trg.isVisible())
									continue;
								if (checkPeace && trg.isInsideZone(L2Zone.FLAG_PEACE) )
									continue;

								if (!srcInPvP && !targetInPvP)
								{
									if (src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
										continue;

									if (src.getClan() != null && trg.getClan() != null)
									{
										if (src.getClan().getClanId() == trg.getClan().getClanId())
											continue;
									}

									if (!src.checkPvpSkill(obj, this, srcIsSummon))
										continue;
								}
							}
							if (obj instanceof L2Summon)
							{
								L2PcInstance trg = ((L2Summon) obj).getOwner();
								if (trg == src)
									continue;

								if ((src.getParty() != null && trg.getParty() != null) && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
									continue;

								if (!srcInPvP && !targetInPvP)
								{
									if (src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
										continue;

									if (src.getClan() != null && trg.getClan() != null)
									{
										if (src.getClan().getClanId() == trg.getClan().getClanId())
											continue;
									}

									if (!src.checkPvpSkill(trg, this, srcIsSummon))
										continue;
								}

								if (checkPeace && trg.isInsideZone(L2Zone.FLAG_PEACE))
									continue;
							}
						}
						else // Skill user is not L2PlayableInstance
						{
							if (effectOriginIsL2PlayableInstance && // If effect starts at L2PlayableInstance and
									!(obj instanceof L2PlayableInstance)) // Object is not L2PlayableInstance
								continue;
						}
						targetList.add((L2Character) obj);
					}
				}

				if (targetList.size() == 0)
					return null;

				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA_UNDEAD:
			{
				L2Character cha;
				int radius = getSkillRadius();
				if (getCastRange() >= 0 && (target instanceof L2NpcInstance || target instanceof L2SummonInstance) && target.isUndead() && !target.isAlikeDead())
				{
					cha = target;

					if (!onlyFirst)
						targetList.add(cha); // Add target to target list
					else
						return new L2Character[] { cha };
				}
				else
					cha = activeChar;

				for (L2Object obj : cha.getKnownList().getKnownObjects().values())
				{
					if (obj instanceof L2NpcInstance)
						target = (L2NpcInstance) obj;
					else if (obj instanceof L2SummonInstance)
						target = (L2SummonInstance) obj;
					else
						continue;

					if (!activeChar.canSee(target))
						continue;

					if (!target.isAlikeDead()) // If target is not dead/fake death and not self
					{
						if (!target.isUndead())
							continue;
						if (!Util.checkIfInRange(radius, cha, obj, true)) // Go to next obj if obj isn't in range
							continue;

						if (!onlyFirst)
							targetList.add((L2Character) obj); // Add obj to target lists
						else
							return new L2Character[] { (L2Character) obj };
					}
				}

				if (targetList.size() == 0)
					return null;
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY:
			{
				if (onlyFirst)
					return new L2Character[] { activeChar };

				targetList.add(activeChar);

				L2PcInstance player = null;

				if (activeChar instanceof L2Summon)
				{
					player = ((L2Summon) activeChar).getOwner();
					targetList.add(player);
				}
				else if (activeChar.isPlayer())
				{
					player = (L2PcInstance) activeChar;
					if (activeChar.getPet() != null)
						targetList.add(activeChar.getPet());
				}

				if (activeChar.getParty() != null)
				{
					// Get all visible objects in a spheric area near the L2Character
					// Get a list of Party Members
					List<L2PcInstance> partyList = activeChar.getParty().getPartyMembers();

					for (L2PcInstance partyMember : partyList)
					{
						if (player == null || partyMember == null || partyMember == player)
							continue;

						if (player.isInDuel() && player.getDuelId() != partyMember.getDuelId())
							continue;

						if (getSkillRadius() > -1 && !activeChar.canSee(partyMember))
							continue;

						if(activeChar.getGameEvent()!=null && !activeChar.getGameEvent().canBeSkillTarget(activeChar, partyMember, this))
							continue;

						if (!partyMember.isDead() && (getSkillRadius() == -1 || Util.checkIfInRange(getSkillRadius(), activeChar, partyMember, true)))
						{
							targetList.add(partyMember);

							if (partyMember.getPet() != null && !partyMember.getPet().isDead())
								targetList.add(partyMember.getPet());
						}
					}
				}
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if ((target != null && target == activeChar) || (target != null && activeChar.getParty() != null && target.getParty() != null
				&& activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID()) || (target != null
				&& activeChar.isPlayer() && target instanceof L2Summon && activeChar.getPet() == target) || (target != null
				&& activeChar instanceof L2Summon && target.isPlayer() && activeChar == target.getPet()))
				{
					if (!target.isDead())
						return new L2Character[] { target };

					return null;
				}
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return null;
			}
			case TARGET_PARTY_OTHER:
			{
				if (target != null && target != activeChar && activeChar.getParty() != null && target.getParty() != null && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
				{
					if (!target.isDead())
					{
						if (target.isPlayer())
						{
							L2PcInstance player = (L2PcInstance) target;
							switch (getId())
							{
								case 426:
									if (!player.isMageClass())
										return new L2Character[] { target };
									return null;
								case 427:
									if (player.isMageClass())
										return new L2Character[] { target };
									return null;
							}
						}
						return new L2Character[] { target };
					}
					return null;
				}
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return null;
			}
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			{
				if (activeChar.isPlayable())
				{
					int radius = getSkillRadius();
					L2PcInstance player = activeChar.getPlayer();
					if (player == null)
						return null;

					L2Clan clan = player.getClan();

					if (player.isInOlympiadMode())
					{
						if (player.getPet() == null)
							return new L2Character[] { player };

						return new L2Character[] { player, player.getPet() };
					}
					if (targetType != SkillTargetType.TARGET_CORPSE_ALLY)
					{
						if (!onlyFirst)
							targetList.add(player);
						else
							return new L2Character[] { player };
					}
					if (activeChar.getPet() != null)
					{
						if ((targetType != SkillTargetType.TARGET_CORPSE_ALLY) && !(activeChar.getPet().isDead()))
							targetList.add(activeChar.getPet());
					}
					if (clan != null)
					{
						// Get all visible objects in a spheric area near the L2Character
						// Get Clan Members
						for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
						{
							if (obj == player || !obj.isPlayer())
								continue;

							L2PcInstance newTarget = obj.getPlayer();

							if ((newTarget.getAllyId() == 0 || newTarget.getAllyId() != player.getAllyId()) && (newTarget.getClan() == null || newTarget.getClanId() != player.getClanId()))
								continue;

							if (player.isInDuel() && (player.getDuelId() != newTarget.getDuelId() || (player.getParty() != null && player.getParty() != newTarget.getParty())))
								continue;

							if(activeChar.getGameEvent()!=null && !activeChar.getGameEvent().canBeSkillTarget(activeChar, newTarget, this))
								continue;

							L2Summon pet = newTarget.getPet();
							if (pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst && (targetType == SkillTargetType.TARGET_ALLY && !pet.isDead()) && player.checkPvpSkill(newTarget, this))
								targetList.add(pet);

							if (targetType == SkillTargetType.TARGET_CORPSE_ALLY)
							{
								if (!newTarget.isDead())
									continue;
								// Siege battlefield resurrect has been made possible for participants
/*								if (getSkillType() == L2SkillType.RESURRECT)
								{
									if (newTarget.isInsideZone(L2Zone.FLAG_SIEGE) && newTarget.getSiegeState() == 0)
										continue;
								} */
							}

							if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
								continue;

							// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
							if (!player.checkPvpSkill(newTarget, this))
								continue;

							if (!onlyFirst)
								targetList.add(newTarget);
							else
								return new L2Character[] { newTarget };
						}
					}
				}
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_ENEMY_ALLY:
			{
				int radius = getSkillRadius();
				L2Character newTarget;

				if (getCastRange() > -1 && target != null)
					newTarget = target;
				else
					newTarget = activeChar;

				if (newTarget != activeChar || isSkillTypeOffensive())
					targetList.add(newTarget);

				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (obj == newTarget || obj == activeChar)
						continue;

					if (obj instanceof L2Attackable)
					{
						if (!obj.isAlikeDead())
						{
							// Don't add this target if this is a PC->PC pvp casting and pvp condition not met
							if (activeChar.isPlayer() && !((L2PcInstance) activeChar).checkPvpSkill(obj, this))
								continue;

							// check if both attacker and target are L2PcInstances and if they are in same party or clan
							if ((activeChar.isPlayer() && obj.isPlayer()) && (((L2PcInstance) activeChar).getClanId() != ((L2PcInstance) obj).getClanId() || (((L2PcInstance) activeChar).getAllyId() != ((L2PcInstance) obj).getAllyId() && ((activeChar.getParty() != null && obj.getParty() != null) && activeChar.getParty().getPartyLeaderOID() != obj.getParty().getPartyLeaderOID()))))
								continue;

							targetList.add(obj);
						}
					}
				}
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_CLAN:
			case TARGET_CLAN:
			{
				if (activeChar instanceof L2PlayableInstance)
				{
					int radius = getSkillRadius();
					L2PcInstance player = activeChar.getPlayer();
					if (player == null)
						return null;

					L2Clan clan = player.getClan();

					if (player.isInOlympiadMode())
					{
						if (player.getPet() == null)
							return new L2Character[] { player };

						return new L2Character[] { player, player.getPet() };
					}

					if (targetType != SkillTargetType.TARGET_CORPSE_CLAN)
					{
						if (!onlyFirst)
							targetList.add(player);
						else
							return new L2Character[] { player };
					}

					if (activeChar.getPet() != null)
					{
						if ((targetType != SkillTargetType.TARGET_CORPSE_ALLY) && !(activeChar.getPet().isDead()))
							targetList.add(activeChar.getPet());
					}
					if (clan != null)
					{
						for (L2ClanMember member : clan.getMembers())
						{
							L2PcInstance newTarget = member.getPlayerInstance();

							if (newTarget == null || newTarget == player)
								continue;

							if (player.isInDuel() && (player.getDuelId() != newTarget.getDuelId() || (player.getParty() == null && player.getParty() != newTarget.getParty())))
								continue;
							if(activeChar.getGameEvent()!=null && !activeChar.getGameEvent().canBeSkillTarget(activeChar, newTarget, this))
								continue;

							L2Summon pet = newTarget.getPet();
							if (pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst && (targetType == SkillTargetType.TARGET_CLAN && !pet.isDead()) && player.checkPvpSkill(newTarget, this))
								targetList.add(pet);

							if (targetType == SkillTargetType.TARGET_CORPSE_CLAN)
							{
								if (!newTarget.isDead())
									continue;
								if (getSkillType() == L2SkillType.RESURRECT)
								{
									Siege siege = SiegeManager.getInstance().getSiege(newTarget);
									if (siege != null && siege.getIsInProgress())
									{
										if (!newTarget.getCharmOfCourage() || player.getSiegeState() == 0)
											continue;
									}
								}
							}
							if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
								continue;

							// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
							if (!player.checkPvpSkill(newTarget, this))
								continue;

							if (!onlyFirst)
								targetList.add(newTarget);
							else
								return new L2Character[] { newTarget };
						}
					}
				}
				else if (activeChar instanceof L2NpcInstance)
				{
					// for buff purposes, returns one unbuffed friendly mob nearby or mob itself?
					L2NpcInstance npc = (L2NpcInstance) activeChar;
					for (L2Object newTarget : activeChar.getKnownList().getKnownObjects().values())
					{
						if (newTarget instanceof L2NpcInstance && ((L2NpcInstance) newTarget).getFactionId() == npc.getFactionId())
						{
							if (!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true))
								continue;
							if (((L2NpcInstance) newTarget).getFirstEffect(this) != null)
							{
								targetList.add((L2NpcInstance) newTarget);
								break;
							}
						}
					}
					if (targetList.isEmpty())
						targetList.add(activeChar);
				}

				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_MULTIFACE:
			{
				return getMultiFaceTargetList(activeChar);
			}
			case TARGET_CORPSE_PLAYER:
			{
				if (target != null && target.isDead())
				{
					L2PcInstance player = null;

					if (activeChar.isPlayer())
						player = (L2PcInstance) activeChar;
					L2PcInstance targetPlayer = null;

					if (target.isPlayer())
						targetPlayer = (L2PcInstance) target;
					L2PetInstance targetPet = null;

					if (target instanceof L2PetInstance)
						targetPet = (L2PetInstance) target;

					if (player != null && (targetPlayer != null || targetPet != null))
					{
						boolean condGood = true;

						if (getSkillType() == L2SkillType.RESURRECT)
						{
							// check target is not in a active siege zone
							Siege siege = null;

							if (targetPlayer != null)
								siege = SiegeManager.getInstance().getSiege(targetPlayer);
							else if (targetPet != null)
								siege = SiegeManager.getInstance().getSiege(targetPet);

							if (siege != null && siege.getIsInProgress() &&
									targetPlayer != null &&
									(!targetPlayer.getCharmOfCourage() || player.getSiegeState() == 0))
							{
								condGood = false;
								player.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
							}

							if (targetPlayer != null)
							{
								if (targetPlayer.isReviveRequested())
								{
									player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been
									// proposed.
									condGood = false;
								}
							}
							else if (targetPet != null)
							{
								if (targetPet.getOwner() != player)
								{
									condGood = false;
									player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_YOU_NOT_PET_OWNER));
								}
							}
						}

						if (condGood)
						{
							if (!onlyFirst)
							{
								targetList.add(target);
								return targetList.moveToArray(new L2Character[targetList.size()]);
							}

							return new L2Character[] { target };
						}
					}
				}
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return null;
			}
			case TARGET_CORPSE_MOB:
			{
				if (!(target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}

				// Corpse mob only available for half time
				switch (getSkillType())
				{
					case DRAIN:
					case SUMMON:
					{
						if (DecayTaskManager.getInstance().hasDecayTask(target))
						{
							if (DecayTaskManager.getInstance().getRemainingDecayTime(target) < 0.5)
							{
								activeChar.sendPacket(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED);
								return null;
							}
						}
					}
				}
				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.moveToArray(new L2Character[targetList.size()]);
				}
				return new L2Character[] { target };
			}
			case TARGET_AREA_CORPSE_MOB:
			{
				if ((!(target instanceof L2Attackable)) || !target.isDead())
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}
				if (!onlyFirst)
					targetList.add(target);
				else
					return new L2Character[] { target };

				boolean srcInArena = (activeChar.isInsideZone(L2Zone.FLAG_PVP) && !activeChar.isInsideZone(L2Zone.FLAG_SIEGE));

				L2PcInstance src = null;
				if (activeChar.isPlayer())
					src = (L2PcInstance) activeChar;
				L2PcInstance trg;

				int radius = getSkillRadius();
				for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance) || ((L2Character) obj).isDead() || obj == activeChar)
						continue;

					boolean targetInPvP = obj.getCharacter().isInsideZone(L2Zone.FLAG_PVP);

					if (!Util.checkIfInRange(radius, target, obj, true))
						continue;

					if (!activeChar.canSee(obj))
						continue;

					if ((obj.isPlayer() || obj.isSummon()) && src != null)
					{
						trg = obj.getPlayer();

						if (trg.equals(src))
							continue;

						if ((src.getParty() != null && trg.getParty() != null) && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
							continue;

						if (trg.isInsideZone(L2Zone.FLAG_PEACE))
							continue;

						if (!srcInArena && !targetInPvP)
						{
							if (src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
								continue;

							if (src.getClan() != null && trg.getClan() != null)
							{
								if (src.getClan().getClanId() == trg.getClan().getClanId())
									continue;
							}

							if (!src.checkPvpSkill(obj, this))
								continue;
						}
					}
					targetList.add(obj.getCharacter());
				}
				if (targetList.size() == 0)
					return null;
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA_CORPSES:
			{
				if (!(target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}
				if (!onlyFirst)
					targetList.add(target);
				else
					return new L2Character[]
					{ target };

				int radius = getSkillRadius();
				if (activeChar.getKnownList() != null)
				{
					for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
					{
						if (obj == null || !(obj instanceof L2Attackable))
							continue;

						L2Character cha = (L2Character) obj;

						if (!cha.isDead() || !Util.checkIfInRange(radius, target, cha, true))
							continue;

						if (!activeChar.canSee(cha))
							continue;

						targetList.add(cha);
					}
				}
				if (targetList.size() == 0)
					return null;
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_UNLOCKABLE:
			{
				if (!(target instanceof L2DoorInstance) && !(target instanceof L2ChestInstance))
					return null;

				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.moveToArray(new L2Character[targetList.size()]);
				}
				return new L2Character[] { target };
			}
			case TARGET_ENEMY_SUMMON:
			{
				if (target instanceof L2Summon)
				{
					L2Summon targetSummon = (L2Summon) target;
					if (activeChar.isPlayer() && activeChar.getPet() != targetSummon && !targetSummon.isDead()
							&& (targetSummon.getOwner().getPvpFlag() != 0 || targetSummon.getOwner().getKarma() > 0)
							|| (targetSummon.getOwner().isInsideZone(L2Zone.FLAG_PVP) && activeChar.isInsideZone(L2Zone.FLAG_PVP)))
						return new L2Character[] { targetSummon };
				}
				return null;
			}
			case TARGET_GATE:
			{
				// Check for null target or any other invalid target
				if (target == null || target.isDead() || !(target instanceof L2DoorInstance))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}
				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[]
				{ target };
			}
			case TARGET_MOB:
			{
				// Check for null target or any other invalid target
				if (target == null || target.isDead() || !(target instanceof L2Attackable))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					return null;
				}
				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[]
				{ target };
			}
			case TARGET_KNOWNLIST:
			{
				if (target != null && target.getKnownList() != null)
					for (L2Object obj : target.getKnownList().getKnownObjects().values())
					{
						if (obj instanceof L2Attackable || obj instanceof L2PlayableInstance)
							return new L2Character[]
							{ (L2Character) obj };
					}

				if (targetList.size() == 0)
					return null;
				return targetList.moveToArray(new L2Character[targetList.size()]);
			}
			case TARGET_PIG:
			{
				if (target != null && target instanceof L2NpcInstance)
				{
					int npcId = ((L2NpcInstance) target).getNpcId();
					if ((npcId >= 13031) && (npcId <= 13035))
						return new L2Character[] { target };
				}
				return null;
			}
			default:
				return null;
		}
		}
		finally
		{
			targetList.clear();
		}
	}

	public final L2Character[] getMultiFaceTargetList(L2Character activeChar)
	{
		LinkedBunch<L2Character> targetList = new LinkedBunch<L2Character>();
		L2Object target;
		L2Object FirstTarget;
		L2PcInstance tgOwner;
		L2Clan acClan;
		L2Clan tgClan;
		L2Party acPt = activeChar.getParty();
		int radius = getSkillRadius();

		if (getCastRange() <= 0)
			target = activeChar;
		else
			target = activeChar.getTarget();
		FirstTarget = target;

		if (target == null || !(target instanceof L2Character))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return null;
		}

		int newHeading = getNewHeadingToTarget(activeChar, (L2Character) target);

		if (target.getObjectId() != activeChar.getObjectId())
		{
			if (!((L2Character) target).isAlikeDead())
				targetList.add((L2Character) target);
			else
			{
				activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				return null;
			}
		}

		if (!(activeChar instanceof L2PlayableInstance))
		{
			for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
			{
				if (obj.isPlayer())
					if (((L2PcInstance)obj).inObserverMode())
						continue;
				if (obj instanceof L2PlayableInstance)
				{
					if (!(Util.checkIfInRange(radius, target, obj, true)))
						continue;
					else if (isBehindFromCaster(newHeading, (L2Character) FirstTarget, (L2Character) target))
						continue;
					else if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);

				}
			}
			if (targetList.size() == 0)
				return null;
			return targetList.moveToArray(new L2Character[targetList.size()]);
		}

		if (activeChar.getPlayer() != null)
			acClan = activeChar.getPlayer().getClan();
		else
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return null;
		}

		if (activeChar.isInsideZone(L2Zone.FLAG_SIEGE))
		{
			for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
			{
				if (!(obj instanceof L2PlayableInstance))
					continue;
				if (!(Util.checkIfInRange(radius, target, obj, true)))
					continue;
				else if (isBehindFromCaster(newHeading, (L2Character) FirstTarget, (L2Character) obj))
					continue;

				if (obj.isPlayer())
				{
					if (((L2PcInstance)obj).inObserverMode())
						continue;
					tgClan = ((L2PcInstance) obj).getClan();

					if (acPt != null)
					{
						if (activeChar.getParty().getPartyMembers().contains(obj))
							continue;
						else if (!((L2Character) obj).isAlikeDead())
							targetList.add((L2Character) obj);
					}
					else if (tgClan != null)
					{
						if (tgClan.getClanId() == acClan.getClanId())
							continue;
						else if (tgClan.getAllyId() == acClan.getAllyId())
							continue;
						else if (!((L2Character) obj).isAlikeDead())
							targetList.add((L2Character) obj);
					}
					else if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
				else if (obj instanceof L2Summon)
				{
					tgOwner = ((L2Summon) obj).getOwner();
					tgClan = tgOwner.getClan();

					if (acPt != null)
					{
						if (activeChar.getParty().getPartyMembers().contains(tgOwner))
							continue;
						else if (!((L2Character) obj).isAlikeDead())
							targetList.add((L2Character) obj);
					}
					else if (tgClan != null)
					{
						if (tgClan.getClanId() == acClan.getClanId())
							continue;
						else if (tgClan.getAllyId() == acClan.getAllyId())
							continue;
						else if (!((L2Character) obj).isAlikeDead())
							targetList.add((L2Character) obj);
					}
					else if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
				else if (obj instanceof L2Attackable)
				{
					if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
			}
		}
		else if (activeChar.isInsideZone(L2Zone.FLAG_STADIUM) || activeChar.isInsideZone(L2Zone.FLAG_PVP))
		{
			for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
			{
				if (!(obj instanceof L2PlayableInstance))
					continue;
				if (!(Util.checkIfInRange(radius, target, obj, true)))
					continue;
				else if (isBehindFromCaster(newHeading, (L2Character) FirstTarget, (L2Character) obj))
					continue;

				if (obj.isPlayer())
				{
					if (obj.getPlayer().inObserverMode())
						continue;
					if (acPt != null)
					{
						if (activeChar.getParty().inParty(obj.getPlayer()) && !obj.getPlayer().isAlikeDead())
							targetList.add((L2Character) obj);
					}
					else if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
				else if (obj instanceof L2Summon)
				{
					tgOwner = ((L2Summon) obj).getOwner();

					if (acPt != null)
					{
						if (activeChar.getParty().getPartyMembers().contains(tgOwner))
							continue;
						else if (!((L2Character) obj).isAlikeDead())
							targetList.add((L2Character) obj);
					}
					else if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
				else if (obj instanceof L2Attackable)
				{
					if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
				else
				{
					continue;
				}
			}
		}
		else
		{
			for (L2Object obj : activeChar.getKnownList().getKnownObjects().values())
			{
				if (!(obj instanceof L2Character))
					continue;
				if (obj.isPlayer())
					if (((L2PcInstance)obj).inObserverMode())
						continue;
				if (!(Util.checkIfInRange(radius, target, obj, true)))
					continue;
				else if (isBehindFromCaster(newHeading, (L2Character) FirstTarget, (L2Character) obj))
					continue;

				if (obj instanceof L2MonsterInstance)
				{
					if (!((L2Character) obj).isAlikeDead())
						targetList.add((L2Character) obj);
				}
			}
		}

		if (targetList.size() == 0)
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return null;
		}

		return targetList.moveToArray(new L2Character[targetList.size()]);
	}

	protected int getNewHeadingToTarget(L2Character caster, L2Character target)
	{
		if (caster == null || target == null)
			return 0;

		double befHeading = Util.convertHeadingToDegree(caster.getHeading());
		if (befHeading > 360)
			befHeading -= 360;

		int dx = caster.getX() - target.getX();
		int dy = caster.getY() - target.getY();

		double dist = Math.sqrt(dx * dx + dy * dy);

		if (dist == 0)
			dist = 0.01;

		double sin = dy / dist;
		double cos = dx / dist;
		int heading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);
		return heading;
	}

	public boolean isBehindFromCaster(int heading, L2Character caster, L2Character target)
	{
		if (caster == null || target == null)
			return true;

		double befHeading = Util.convertHeadingToDegree(heading);
		if (befHeading > 360)
			befHeading -= 360;
		else if (befHeading < 0)
			befHeading += 360;

		int dx = caster.getX() - target.getX();
		int dy = caster.getY() - target.getY();

		double dist = Math.sqrt(dx * dx + dy * dy);

		if (dist == 0)
			dist = 0.01;

		double sin = dy / dist;
		double cos = dx / dist;
		int newheading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);

		double aftHeading = Util.convertHeadingToDegree(newheading);
		if (aftHeading > 360)
			aftHeading -= 360;
		else if (aftHeading < 0)
			aftHeading += 360;

		double diffHeading = Math.abs(aftHeading - befHeading);
		if (diffHeading > 360)
			diffHeading -= 360;
		else if (diffHeading < 0)
			diffHeading += 360;
		return (diffHeading > 90) && (diffHeading < 270);

	}

	public final L2Character[] getTargetList(L2Character activeChar)
	{
		return getTargetList(activeChar, false);
	}

	public final L2Character getFirstOfTargetList(L2Character activeChar)
	{
		L2Character[] targets;

		targets = getTargetList(activeChar, true);
		if (targets == null || targets.length == 0)
			return null;
		return targets[0];
	}

	public final Func[] getStatFuncs(L2Effect effect, L2Character player)
	{
		if (!(player.isPlayer()) && !(player instanceof L2Attackable) && !(player instanceof L2Summon))
			return Func.EMPTY_ARRAY;

		if (_funcTemplates == null)
			return Func.EMPTY_ARRAY;

		LinkedBunch<Func> funcs = new LinkedBunch<Func>();
		for (FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.skill = this;
			Func f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.size() == 0)
			return Func.EMPTY_ARRAY;
		return funcs.moveToArray(new Func[funcs.size()]);
	}

	public boolean hasEffects()
	{
		return (_effectTemplates != null && _effectTemplates.length > 0);
	}

	public final L2Effect[] getEffects(L2Character effector, L2Character effected) {
		return getEffects(effector, effected,effector);
	}
	public synchronized final L2Effect[] getEffects(L2Character effector, L2Character effected, IEffector effectorObj)
	{
		if (effector == null)
		{
			_log.warn("Method getEffects: effector is null.");
			Thread.dumpStack();
			return L2Effect.EMPTY_ARRAY;
		}

		if (effected == null)
		{
			_log.warn("Method getEffects: effected is null.");
			Thread.dumpStack();
			return L2Effect.EMPTY_ARRAY;
		}

		if (isPassive())
			return L2Effect.EMPTY_ARRAY;

		if (_effectTemplates == null)
			return L2Effect.EMPTY_ARRAY;

		// doors and siege flags cannot receive any effects
		if (effected instanceof L2DoorInstance || effected instanceof L2SiegeFlagInstance)
			return L2Effect.EMPTY_ARRAY;

		if (effector != effected)
		{
			if (effected.isInvul())
				return L2Effect.EMPTY_ARRAY;
		}
		if(getSkillType() == L2SkillType.BUFF && getTargetType()!= SkillTargetType.TARGET_SELF && effected.isPreventedFromReceivingBuffs())
			return L2Effect.EMPTY_ARRAY;
		
		if(isActive() &&  getSkillType() == L2SkillType.BUFF && effected.getPlayer()!=null) {
			int maxBuffs = effected.getMaxBuffCount();
			
			if (effected.getBuffCount()>=maxBuffs) {
				int firstBuff = 0;
				boolean remove = true;
				for(L2Effect e : effected.getAllEffects()) {
					if(e.getSkill()!=null && e.getSkill().isActive()) {
						if(e.getSkill().getSkillType() == L2SkillType.BUFF && firstBuff==0)
							firstBuff = e.getSkill().getId();
						if(e.getSkill().getId().equals(getId())) {
							remove = false;
							break;
						}
					}
				}
				if(remove && firstBuff!=0) 
					effected.stopSkillEffects(firstBuff);
			}
		}
		LinkedBunch<L2Effect> effects = new LinkedBunch<L2Effect>();

		boolean skillMastery = false;

		if (!isToggle() && effector.isPlayer() && Formulas.calcSkillMastery(effector, this))
			skillMastery = true;

		
		for (EffectTemplate et : _effectTemplates)
		{
			if(!effected.isAllow(et, this))
				return L2Effect.EMPTY_ARRAY;
			Env env = new Env();
			env.player = effector;
			env.target = effected;
			env.object = effectorObj;
			env.skill = this;
			env.skillMastery = skillMastery;
			L2Effect e = et.getEffect(env);
			if (e != null)
				effects.add(e);
		}
		if (effects.size() == 0)
			return L2Effect.EMPTY_ARRAY;
		return effects.moveToArray(new L2Effect[effects.size()]);
	}

	public final void getEffects(L2CubicInstance effector, L2Character effected)
	{
		if (isPassive())
			return;
		if (_effectTemplates == null)
			return;
		// doors and siege flags cannot receive any effects
		if (effected instanceof L2DoorInstance || effected instanceof L2SiegeFlagInstance)
			return;
		if ((!effector.equals(effected)) && effected.isInvul())
			return;
		if ((isDebuff() || isOffensive()) && effector.getOwner() == effected.getPlayer())
			return;
		for (EffectTemplate et : _effectTemplates)
		{
			Env env = new Env();
			env.player = effector.getOwner();
			env.cubic = effector;
			env.target = effected;
			env.skill = this;
			et.getEffect(env);
		}
	}

	public final void getEffectsSelf(L2Character effector)
	{
		if (isPassive())
			return;

		if (_effectTemplatesSelf == null)
			return;

		for (EffectTemplate et : _effectTemplatesSelf)
		{
			Env env = new Env();
			env.player = effector;
			env.target = effector;
			env.skill = this;
			et.getEffect(env);
		}
	}

	public final void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{ f };
		}
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public final void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{ effect };
		}
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}

	public final void attachSelf(EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new EffectTemplate[]
			{ effect };
		}
		else
		{
			int len = _effectTemplatesSelf.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesSelf = tmp;
		}
	}

	public final void attach(Condition c)
	{
		Condition old = _preCondition;
		if (old != null)
			_log.fatal("Replaced " + old + " condition with " + c + " condition at skill: " + this);
		_preCondition = c;
	}

	@Override
	public String toString()
	{
		return _name + "[id=" + _id + ",lvl=" + _level + " item="+isItemSkill()+" ]";
	}

	private final boolean	_chMagic;		// If true this is a buff used by a ch manager
	private final boolean	_heroMagic;	// If true this is a buff used by a hero
	private final boolean	_bufferMagic;	// If true this is a buff used by a buffer
	private final boolean	_is5MinMagic;

	public boolean isHeroMagic()
	{
		return _heroMagic;
	}

	public boolean isChMagic()
	{
		return _chMagic;
	}

	public boolean isBufferMagic()
	{
		return _bufferMagic;
	}

	public boolean is5MinMagic()
	{
		return _is5MinMagic;
	}

	public EffectTemplate[] getEffectTempate()
	{
		return _effectTemplates;
	}

	public String generateUniqueStackType()
	{
		int count = _effectTemplates == null ? 0 : _effectTemplates.length;
		count += _effectTemplatesSelf == null ? 0 : _effectTemplatesSelf.length;
		return _id + "-" + count;
	}

	@Override
	public String getFuncOwnerName()
	{
		return getName();
	}

	@Override
	public final L2Skill getFuncOwnerSkill()
	{
		return this;
	}
	public boolean canBeReflected()
	{
		return _canBeReflected;
	}

	public int getLethalType()
	{
		return _lethalType;
	}
	
	public String getAttributeName()
	{
		return _attribute;
	}

	public boolean useAlways() {
		return _useAlways;
	}

	public boolean isDirectHp()
	{
		return _directHp;
	}

	public boolean isCancelable()
	{
		return _isCancelable;
	}

	@Override
	public int hashCode()
	{
		return getId() * 1023 + getLevel();
	}
}
