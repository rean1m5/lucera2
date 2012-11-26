package ru.catssoftware.gameserver.templates.skills;

import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.l2skills.*;
import ru.catssoftware.util.StatsSet;

import java.lang.reflect.Constructor;


public enum L2SkillType
{
	PDAM,
	MDAM,
	CPDAM,
	AGGDAMAGE,
	DOT,
	HOT,
	MHOT,
	BLEED,
	POISON,
	CPHOT,
	MPHOT,
	BUFF,
	DEBUFF,
	STUN,
	ROOT,
	CONT,
	FUSION,
	CONFUSION,
	PARALYZE,
	FEAR,
	SLEEP,
	DEATH_MARK,
	HEAL,
	HEAL_MOB,
	COMBATPOINTHEAL,
	MANAHEAL,
	MANAHEAL_PERCENT,
	MANARECHARGE,
	RESURRECT,
	PASSIVE,
	UNLOCK,
	GIVE_SP,
	NEGATE,
	CANCEL,
	CANCEL_DEBUFF,
	AGGREDUCE,
	AGGREMOVE,
	AGGREDUCE_CHAR,
	CONFUSE_MOB_ONLY,
	DEATHLINK,
	BLOW,
	FATALCOUNTER,
	DETECT_WEAKNESS,
	ENCHANT_ARMOR,
	ENCHANT_WEAPON,
	ENCHANT_ATTRIBUTE,
	FEED_PET,
	HEAL_PERCENT,
	HEAL_STATIC,
	DEATH_PENALTY,
	LUCK,
	MANADAM,
	MAKE_KILLABLE,
	MDOT,
	MUTE,
	RECALL,
	REFLECT,
	SUMMON_FRIEND,
	SOULSHOT,
	SPIRITSHOT,
	SPOIL,
	SWEEP,
	WEAKNESS,
	DISARM,
	STEAL_BUFF,
	BAD_BUFF,
	DEATHLINK_PET,
	MANA_BY_LEVEL,
	FAKE_DEATH,
	SIEGEFLAG,
	TAKECASTLE,
	TAKEFORT,
	UNDEAD_DEFENSE,
	BEAST_FEED,
	DRAIN_SOUL,
	COMMON_CRAFT,
	DWARVEN_CRAFT,
	WEAPON_SA,
	DELUXE_KEY_UNLOCK,
	SOW,
	HARVEST,
	CHARGESOUL,
	GET_PLAYER,
	FISHING,
	PUMPING,
	REELING,
	CANCEL_TARGET,
	AGGDEBUFF,
	COMBATPOINTPERCENTHEAL,
	SUMMONCP,
	SUMMON_TREASURE_KEY,
	SUMMON_CURSED_BONES,
	EXTRACTABLE,
	EXTRACTABLE_COMBO,
	ERASE,
	MAGE_BANE,
	WARRIOR_BANE,
	STRSIEGEASSAULT,
	RAID_DESCRIPTION,
	UNSUMMON_ENEMY_PET,
	BETRAY,
	BALANCE_LIFE,
	SERVER_SIDE,
	REMOVE_TRAP,
	SHIFT_TARGET,
	INSTANT_JUMP,
	GARDEN_KEY_UNLOCK,
	CLAN_GATE,
	ZAKEN_MOVE,
	MOUNT(L2SkillMount.class),
	CHARGEDAM(L2SkillChargeDmg.class),
	CHARGE_NEGATE(L2SkillChargeNegate.class),
	CREATE_ITEM(L2SkillCreateItem.class),
	DRAIN(L2SkillDrain.class),
	LUCKNOBLESSE(L2SkillCreateItem.class),
	SIGNET(L2SkillSignet.class),
	SIGNET_CASTTIME(L2SkillSignetCasttime.class),
	SUMMON(L2SkillSummon.class),
	DUMMY,
	SCRIPT(L2ScriptSkill.class),
	COREDONE,
	CUSTOM,
	PET_SUMMON,
	NOTDONE;


	private final Constructor<? extends L2Skill> _constructor;

	private L2SkillType()
	{
		this(L2Skill.class);
	}
	
	private L2SkillType(Class<? extends L2Skill> clazz)
	{
		try
		{
			_constructor = clazz.getConstructor(StatsSet.class);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public L2Skill makeSkill(StatsSet set) throws Exception
	{
		return _constructor.newInstance(set);
	}
}