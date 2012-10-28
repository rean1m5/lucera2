package ru.catssoftware.gameserver.skills;


import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.SevenSignsFestival;
import ru.catssoftware.gameserver.instancemanager.*;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.base.PlayerState;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.conditions.ConditionPlayerState;
import ru.catssoftware.gameserver.skills.conditions.ConditionUsingItemType;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate.Race;
import ru.catssoftware.gameserver.templates.chars.L2PcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Armor;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

 /**
  * Класс содержит основные формулы для расчета урона, регенерации....
  * Исправления класса произведены 18/05/2010, CatsSoftware
  * @author m095
  */

public final class Formulas
{
	protected static final Logger		_log					= Logger.getLogger(L2Character.class.getName());
	
	private static final int		HP_REGENERATE_PERIOD	= 3000;
	public static final byte 		SKILL_REFLECT_FAILED 	= 0;
	public static final byte 		SKILL_REFLECT_SUCCEED 	= 1;
	public static final byte 		SKILL_REFLECT_VENGEANCE = 2;
	private static final byte 		MELEE_ATTACK_RANGE 		= 40;
	public static int				MAX_STAT_VALUE			= 100;

	private static final double[]	STRCompute				= new double[] { 1.036, 34.845 };
	private static final double[]	INTCompute				= new double[] { 1.020, 31.375 };
	private static final double[]	DEXCompute				= new double[] { 1.009, 19.360 };
	private static final double[]	WITCompute				= new double[] { 1.050, 20.000 };
	private static final double[]	CONCompute				= new double[] { 1.030, 27.632 };
	private static final double[]	MENCompute				= new double[] { 1.010, -0.060 };
	protected static final double[]	WITbonus				= new double[MAX_STAT_VALUE];
	protected static final double[]	MENbonus				= new double[MAX_STAT_VALUE];
	protected static final double[]	INTbonus				= new double[MAX_STAT_VALUE];
	protected static final double[]	STRbonus				= new double[MAX_STAT_VALUE];
	protected static final double[]	DEXbonus				= new double[MAX_STAT_VALUE];
	protected static final double[]	CONbonus				= new double[MAX_STAT_VALUE];
	protected static final double[]	sqrtMENbonus			= new double[MAX_STAT_VALUE];
	protected static final double[]	sqrtCONbonus			= new double[MAX_STAT_VALUE];

	static
	{
		for (int i = 0; i < STRbonus.length; i++)
			STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < INTbonus.length; i++)
			INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < DEXbonus.length; i++)
			DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < WITbonus.length; i++)
			WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < CONbonus.length; i++)
			CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < MENbonus.length; i++)
			MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;

		for (int i = 0; i < sqrtCONbonus.length; i++)
			sqrtCONbonus[i] = Math.sqrt(CONbonus[i]);
		for (int i = 0; i < sqrtMENbonus.length; i++)
			sqrtMENbonus[i] = Math.sqrt(MENbonus[i]);
	}

	/* --------------- ОСНОВНЫЕ МЕТОДЫ РАСЧЕТА ЗНАЧЕНИЯ FUNC STATS --------------- */
	static class FuncAddLevel3 extends Func
	{
		static final FuncAddLevel3[]	_instancies	= new FuncAddLevel3[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncAddLevel3(stat);
			return _instancies[pos];
		}

		private FuncAddLevel3(Stats pStat)
		{
			super(pStat, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value += env.player.getLevel() / 3.0;
		}
	}

	static class FuncMultLevelMod extends Func
	{
		static final FuncMultLevelMod[]	_instancies	= new FuncMultLevelMod[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{

			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncMultLevelMod(stat);
			return _instancies[pos];
		}

		private FuncMultLevelMod(Stats pStat)
		{
			super(pStat, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= env.player.getLevelMod();
		}
	}

	static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[]	_instancies	= new FuncMultRegenResting[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();

			if (_instancies[pos] == null)
				_instancies[pos] = new FuncMultRegenResting(stat);

			return _instancies[pos];
		}

		private FuncMultRegenResting(Stats pStat)
		{
			super(pStat, 0x20, null, new ConditionPlayerState(PlayerState.RESTING, true));
		}

		@Override
		public void calc(Env env)
		{
			env.value *= 1.45;
		}
	}

	static class FuncPAtkMod extends Func
	{
		static final FuncPAtkMod	_fpa_instance	= new FuncPAtkMod();

		static Func getInstance()
		{
			return _fpa_instance;
		}

		private FuncPAtkMod()
		{
			super(Stats.POWER_ATTACK, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= STRbonus[env.player.getStat().getSTR()] * env.player.getLevelMod();
		}
	}

	static class FuncMAtkMod extends Func
	{
		static final FuncMAtkMod	_fma_instance	= new FuncMAtkMod();

		static Func getInstance()
		{
			return _fma_instance;
		}

		private FuncMAtkMod()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			double intb = INTbonus[env.player.getINT()];
			double lvlb = env.player.getLevelMod();
			env.value *= (lvlb * lvlb) * (intb * intb);
		}
	}

	static class FuncMDefMod extends Func
	{
		static final FuncMDefMod	_fmm_instance	= new FuncMDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
					env.value -= 5;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
					env.value -= 5;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
					env.value -= 9;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
					env.value -= 9;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
					env.value -= 13;
			}
			env.value *= MENbonus[env.player.getStat().getMEN()] * env.player.getLevelMod();
		}
	}

	static class FuncPDefMod extends Func
	{
		static final FuncPDefMod	_fmm_instance	= new FuncPDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				boolean hasMagePDef = (p.getClassId().isMage() || p.getClassId().getId() == 0x31); // orc mystics are a special case
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
					env.value -= 12;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) != null)
					env.value -= hasMagePDef ? 15 : 31;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null)
					env.value -= hasMagePDef ? 8 : 18;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
					env.value -= 8;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
					env.value -= 7;
			}
			env.value *= env.player.getLevelMod();
		}
	}

	static class FuncGatesPDefMod extends Func
	{
		static final FuncGatesPDefMod _fmm_instance = new FuncGatesPDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncGatesPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				env.value *= Config.ALT_SIEGE_DAWN_GATES_PDEF_MULT;
			else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				env.value *= Config.ALT_SIEGE_DUSK_GATES_PDEF_MULT;
		}
	}

	static class FuncGatesMDefMod extends Func
	{
		static final FuncGatesMDefMod _fmm_instance = new FuncGatesMDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncGatesMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				env.value *= Config.ALT_SIEGE_DAWN_GATES_MDEF_MULT;
			else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				env.value *= Config.ALT_SIEGE_DUSK_GATES_MDEF_MULT;
		}
	}

	static class FuncBowAtkRange extends Func
	{
		private static final FuncBowAtkRange	_fbarInstance	= new FuncBowAtkRange();

		static Func getInstance()
		{
			return _fbarInstance;
		}

		private FuncBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null, new ConditionUsingItemType(L2WeaponType.BOW.mask()));
		}

		@Override
		public void calc(Env env)
		{
			env.value += 460;
		}
	}


	static class FuncAtkAccuracy extends Func
	{
		static final FuncAtkAccuracy	_faaInstance	= new FuncAtkAccuracy();

		static Func getInstance()
		{
			return _faaInstance;
		}

		private FuncAtkAccuracy()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			env.value += Math.sqrt(p.getStat().getDEX()) * 6;
			env.value += p.getLevel();
			if (p instanceof L2Summon)
				env.value += (p.getLevel() < 60) ? 4 : 5;
			/*if (p.getLevel() > 77)
				env.value += (p.getLevel() - 77);
			if (p.getLevel() > 69)
				env.value += (p.getLevel() - 69);*/
		}
	}

	static class FuncAtkEvasion extends Func
	{
		static final FuncAtkEvasion	_faeInstance	= new FuncAtkEvasion();

		static Func getInstance()
		{
			return _faeInstance;
		}

		private FuncAtkEvasion()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			env.value += Math.sqrt(p.getStat().getDEX()) * 6;
			env.value += p.getLevel();
			/*if (p.getLevel() > 77)
				env.value += (p.getLevel() - 77);
			if (p.getLevel() > 69)
				env.value += (p.getLevel() - 69);*/
		}
	}

	static class FuncAtkCritical extends Func
	{
		static final FuncAtkCritical	_facInstance	= new FuncAtkCritical();

		static Func getInstance()
		{
			return _facInstance;
		}

		private FuncAtkCritical()
		{
			super(Stats.CRITICAL_RATE, 0x09, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			if (p instanceof L2SummonInstance)
				env.value = 40;
			else if (p instanceof L2PcInstance && p.getActiveWeaponInstance() == null)
				env.value = 40 * DEXbonus[p.getStat().getDEX()];
			else if (p instanceof L2PcInstance)
			{
				env.value *= DEXbonus[p.getStat().getDEX()];
				env.value *= 10;
			}
			env.baseValue = env.value;
		}
	}

	static class FuncMAtkCritical extends Func
	{
		static final FuncMAtkCritical	_fac_instance	= new FuncMAtkCritical();

		static Func getInstance()
		{
			return _fac_instance;
		}

		private FuncMAtkCritical()
		{
			super(Stats.MCRITICAL_RATE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance && env.player.getActiveWeaponInstance() != null)
				env.value *= WITbonus[env.player.getStat().getWIT()];
			else if (env.player instanceof L2Summon)
				env.value = 8; // TODO: needs retail value
		}
	}

	static class FuncMoveSpeed extends Func
	{
		static final FuncMoveSpeed	_fmsInstance	= new FuncMoveSpeed();

		static Func getInstance()
		{
			return _fmsInstance;
		}

		private FuncMoveSpeed()
		{
			super(Stats.RUN_SPEED, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= DEXbonus[p.getStat().getDEX()];
		}
	}

	static class FuncPAtkSpeed extends Func
	{
		static final FuncPAtkSpeed	_fasInstance	= new FuncPAtkSpeed();

		static Func getInstance()
		{
			return _fasInstance;
		}

		private FuncPAtkSpeed()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= DEXbonus[p.getStat().getDEX()];
		}
	}

	static class FuncMAtkSpeed extends Func
	{
		static final FuncMAtkSpeed	_fasInstance	= new FuncMAtkSpeed();

		static Func getInstance()
		{
			return _fasInstance;
		}

		private FuncMAtkSpeed()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= WITbonus[p.getStat().getWIT()];
		}
	}

	static class FuncMaxLoad extends Func
	{
		static final FuncMaxLoad	_fmsInstance	= new FuncMaxLoad();

		static Func getInstance()
		{
			return _fmsInstance;
		}

		private FuncMaxLoad()
		{
			super(Stats.MAX_LOAD, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getStat().getCON()];
		}
	}

	static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR	_fhInstance	= new FuncHennaSTR();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatSTR();
		}
	}

	static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX	_fhInstance	= new FuncHennaDEX();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatDEX();
		}
	}

	static class FuncHennaINT extends Func
	{
		static final FuncHennaINT	_fhInstance	= new FuncHennaINT();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatINT();
		}
	}

	static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN	_fhInstance	= new FuncHennaMEN();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatMEN();
		}
	}

	static class FuncHennaCON extends Func
	{
		static final FuncHennaCON	_fhInstance	= new FuncHennaCON();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatCON();
		}
	}

	static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT	_fhInstance	= new FuncHennaWIT();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatWIT();
		}
	}

	static class FuncMaxHpAdd extends Func
	{
		static final FuncMaxHpAdd	_fmhaInstance	= new FuncMaxHpAdd();

		static Func getInstance()
		{
			return _fmhaInstance;
		}

		private FuncMaxHpAdd()
		{
			super(Stats.MAX_HP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.getClassBaseLevel();
			double hpmod = t.getLvlHpMod() * lvl;
			double hpmax = (t.getLvlHpAdd() + hpmod) * lvl;
			double hpmin = (t.getLvlHpAdd() * lvl) + hpmod;
			env.value += (hpmax + hpmin) / 2;
		}
	}

	static class FuncMaxHpMul extends Func
	{
		static final FuncMaxHpMul	_fmhmInstance	= new FuncMaxHpMul();

		static Func getInstance()
		{
			return _fmhmInstance;
		}

		private FuncMaxHpMul()
		{
			super(Stats.MAX_HP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getStat().getCON()];
		}
	}

	static class FuncMaxCpAdd extends Func
	{
		static final FuncMaxCpAdd	_fmcaInstance	= new FuncMaxCpAdd();

		static Func getInstance()
		{
			return _fmcaInstance;
		}

		private FuncMaxCpAdd()
		{
			super(Stats.MAX_CP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.getClassBaseLevel();
			double cpmod = t.getLvlCpMod() * lvl;
			double cpmax = (t.getLvlCpAdd() + cpmod) * lvl;
			double cpmin = (t.getLvlCpAdd() * lvl) + cpmod;
			env.value += (cpmax + cpmin) / 2;
		}
	}

	static class FuncMaxCpMul extends Func
	{
		static final FuncMaxCpMul	_fmcmInstance	= new FuncMaxCpMul();

		static Func getInstance()
		{
			return _fmcmInstance;
		}

		private FuncMaxCpMul()
		{
			super(Stats.MAX_CP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getStat().getCON()];
		}
	}

	static class FuncMaxMpAdd extends Func
	{
		static final FuncMaxMpAdd	_fmmaInstance	= new FuncMaxMpAdd();

		static Func getInstance()
		{
			return _fmmaInstance;
		}

		private FuncMaxMpAdd()
		{
			super(Stats.MAX_MP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.getClassBaseLevel();
			double mpmod = t.getLvlMpMod() * lvl;
			double mpmax = (t.getLvlMpAdd() + mpmod) * lvl;
			double mpmin = (t.getLvlMpAdd() * lvl) + mpmod;
			env.value += (mpmax + mpmin) / 2;
		}
	}

	static class FuncMaxMpMul extends Func
	{
		static final FuncMaxMpMul	_fmmmInstance	= new FuncMaxMpMul();

		static Func getInstance()
		{
			return _fmmmInstance;
		}

		private FuncMaxMpMul()
		{
			super(Stats.MAX_MP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= MENbonus[p.getStat().getMEN()];
		}
	}

	/**
	 * Основные статы для NPC
	 * @return
	 */
	public static Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];

		// Точность
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		// Уклонение
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		return std;
	}

	/**
	 * Основные статы для Doors
	 * @return
	 */
	public static Calculator[] getStdDoorCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];

		// Точность
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		// Уклонение
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		// Физ.защита
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());
		// Маг.защита
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());
		return std;
	}

	/**
	 * Добавление основных статов для characters
	 * @param cha
	 */
	public static void addFuncsToNewCharacter(L2Character cha)
	{
		if (cha instanceof L2PcInstance)
		{
			cha.addStatFunc(FuncMaxHpAdd.getInstance());
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpAdd.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpAdd.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			cha.addStatFunc(FuncBowAtkRange.getInstance());
			if (Config.LEVEL_ADD_LOAD)
				cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.MAX_LOAD));
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncMaxLoad.getInstance());
			cha.addStatFunc(FuncHennaSTR.getInstance());
			cha.addStatFunc(FuncHennaDEX.getInstance());
			cha.addStatFunc(FuncHennaINT.getInstance());
			cha.addStatFunc(FuncHennaMEN.getInstance());
			cha.addStatFunc(FuncHennaCON.getInstance());
			cha.addStatFunc(FuncHennaWIT.getInstance());
		}
		else if (cha instanceof L2PetInstance)
		{
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
		}
		else if (cha instanceof L2Summon)
		{
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
		}
	}


	/* --------------- ОСНОВНЫЕ МЕТОДЫ РАСЧЕТА ЗНАЧЕНИЯ REGEN STATS --------------- */

	/**
	 * Метод получения периода регенерации
	 * @param cha
	 * @return
	 */
	public static int getRegeneratePeriod(L2Character cha)
	{
		if (cha instanceof L2DoorInstance)
			return HP_REGENERATE_PERIOD * 100; // 5 mins

		return HP_REGENERATE_PERIOD; // 3s
	}

	/**
	 * Метод расчета регенерации HP
	 * Формула исправлена 18/05/2010, CatsSoftware
	 * @param cha
	 * @return
	 */
	public static final double calcHpRegen(L2Character cha)
	{

		if (cha == null)
			return 1;

		/* Базовые значения */
		double init = cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier;
		double hpRegenBonus = 0;

		/* Множители по конфигам */
		if (cha.isRaid())
			hpRegenMultiplier = Config.RAID_HP_REGEN_MULTIPLIER;
		else if (cha instanceof L2PcInstance)
			hpRegenMultiplier = Config.PLAYER_HP_REGEN_MULTIPLIER;
		else if (cha instanceof L2PcInstance)
			hpRegenMultiplier = Config.PET_HP_REGEN_MULTIPLIER;
		else
			hpRegenMultiplier = Config.NPC_HP_REGEN_MULTIPLIER;

		/* У чемпионов свой множитель дополнительный */
		if (cha.isChampion())
			hpRegenMultiplier *= Config.CHAMPION_HP_REGEN;
		
		/* Формулы для игрока */
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
	
			/* значения по уровню персонажа */
			if (player.getLevel() >= 71)
				init = 8.5;
			else if (player.getLevel() >= 61)
				init = 7.5;
			else if (player.getLevel() >= 51)
				init = 6.5;
			else if (player.getLevel() >= 41)
				init = 5.5;
			else if (player.getLevel() >= 31)
				init = 4.5;
			else if (player.getLevel() >= 21)
				init = 3.5;
			else if (player.getLevel() >= 11)
				init = 2.5;
			else
				init = 2.0;

			/* значения на фестивале */
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				init *= calcFestivalRegenModifier(player);
			else
			{
				double siegeModifier = calcSiegeRegenModifer(player);
				if (siegeModifier > 0)
					init *= siegeModifier;
			}

			/* значения в зоне Mother Tree */
			if (player.isInsideZone(L2Zone.FLAG_MOTHERTREE))
				hpRegenBonus += 2;
			
			/* значения в кланн холах */
			int clanHallId = player.getClan() == null ? 0 : player.getClan().getHasHideout();
			if (player.isInsideZone(L2Zone.FLAG_CLANHALL) && clanHallId > 0)
			{
				ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallId);
				if (clansHall != null)
				{
					L2Zone zone = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Clanhall, player.getX(), player.getY());
					int zoneChId = zone == null ? -1 : zone.getClanhallId();
	
					if (clanHallId == zoneChId && clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			/* значения в замках */
			int castleId = player.getClan() == null ? 0 : player.getClan().getHasCastle();
			if (player.isInsideZone(L2Zone.FLAG_CASTLE) && castleId > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(castleId);
				if (castle != null)
				{
					L2Zone zone = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Castle, player.getX(), player.getY());
					int zoneCsId = zone == null ? -1 : zone.getCastleId();

					if (castleId == zoneCsId && castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
						hpRegenMultiplier *= 1 + castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			/* значения в фортах */
			int fortId = player.getClan() == null ? 0 : player.getClan().getHasFort();
			if (player.isInsideZone(L2Zone.FLAG_FORT) && fortId > 0)
			{
				Fort fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					L2Zone zone = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Fort, player.getX(), player.getY());
					int zoneFdId = zone == null ? -1 : zone.getFortId();
					
					if (fortId == zoneFdId && fort.getFunction(Fort.FUNC_RESTORE_HP) != null)
						hpRegenMultiplier *= 1 + fort.getFunction(Fort.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			/* значения по состоянию чара */
			if (player.isSitting() && player.getLevel() < 41)
			{
				init *= 1.5;
				hpRegenBonus += (40 - player.getLevel()) * 0.7;
			}
			else if (player.isSitting())
				init *= 2.5;
			else if (player.isRunning())
				init *= 0.7;
			else if (player.isMoving())
				init *= 1.1;
			else
				init *= 1.5;

			/* бонус от значения CON */
			init *= cha.getLevelMod() * CONbonus[cha.getStat().getCON()];
		}
		else if (cha instanceof L2PetInstance)
			init = ((L2PetInstance) cha).getPetData().getPetRegenHP();
		
		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}

	/**
	 * Метод расчета регенерации MP
	 * Формула исправлена 18/05/2010, CatsSoftware
	 * @param cha
	 * @return
	 */
	public static final double calcMpRegen(L2Character cha)
	{
		if (cha == null)
			return 1;


		/* Базовые значения */
		double init = cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier;
		double mpRegenBonus = 0;

		/* Множители по конфигам */
		if (cha.isRaid())
			mpRegenMultiplier = Config.RAID_MP_REGEN_MULTIPLIER;
		else if (cha instanceof L2PcInstance)
			mpRegenMultiplier = Config.PLAYER_MP_REGEN_MULTIPLIER;
		else if (cha instanceof L2PcInstance)
			mpRegenMultiplier = Config.PET_MP_REGEN_MULTIPLIER;
		else
			mpRegenMultiplier = Config.NPC_MP_REGEN_MULTIPLIER;

		/* Формулы для игрока */
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			/* значения по уровню персонажа */
			if (player.getLevel() >= 71)
				init = 3.0;
			else if (player.getLevel() >= 61)
				init = 2.7;
			else if (player.getLevel() >= 51)
				init = 2.4;
			else if (player.getLevel() >= 41)
				init = 2.1;
			else if (player.getLevel() >= 31)
				init = 1.8;
			else if (player.getLevel() >= 21)
				init = 1.5;
			else if (player.getLevel() >= 11)
				init = 1.2;
			else
				init = 0.9;

			/* значения на фестивале */
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				init *= calcFestivalRegenModifier(player);

			/* значения в зоне Mother Tree */
			if (player.isInsideZone(L2Zone.FLAG_MOTHERTREE))
				mpRegenBonus += 2;

			/* значения в кланн холах */
			int clanHallId = player.getClan() == null ? 0 : player.getClan().getHasHideout();
			if (player.isInsideZone(L2Zone.FLAG_CLANHALL) && clanHallId > 0)
			{
				ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallId);
				if (clansHall != null)
				{
					L2Zone zone = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Clanhall, player.getX(), player.getY());
					int zoneChId = zone == null ? -1 : zone.getClanhallId();
	
					if (clanHallId == zoneChId && clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			/* значения в замках */
			int castleId = player.getClan() == null ? 0 : player.getClan().getHasCastle();
			if (player.isInsideZone(L2Zone.FLAG_CASTLE) && castleId > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(castleId);
				if (castle != null)
				{
					L2Zone zone = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Castle, player.getX(), player.getY());
					int zoneCsId = zone == null ? -1 : zone.getCastleId();

					if (castleId == zoneCsId && castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
						mpRegenMultiplier *= 1 + castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			/* значения в фортах */
			int fortId = player.getClan() == null ? 0 : player.getClan().getHasFort();
			if (player.isInsideZone(L2Zone.FLAG_FORT) &&  fortId > 0)
			{
				Fort fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					L2Zone zone = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Fort, player.getX(), player.getY());
					int zoneFdId = zone == null ? -1 : zone.getFortId();
					
					if (fortId == zoneFdId && fort.getFunction(Fort.FUNC_RESTORE_MP) != null)
						mpRegenMultiplier *= 1 + fort.getFunction(Fort.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			/* значения по состоянию чара */
			if (player.isSitting())
				init *= 2.5;
			else if (player.isRunning())
				init *= 0.7;
			else if (player.isMoving())
				init *= 1.1;
			else
				init *= 1.5;

			/* бонус от значения MEN */
			init *= cha.getLevelMod() * MENbonus[cha.getStat().getMEN()];
		}
		else if (cha instanceof L2PetInstance)
			init = ((L2PetInstance) cha).getPetData().getPetRegenMP();

		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}

	/**
	 * Метод расчета регенерации CP
	 * Формула исправлена 18/05/2010, CatsSoftware
	 * @param cha
	 * @return
	 */
	public static final double calcCpRegen(L2Character cha)
	{
		/* Базовые значения */
		double init = cha.getTemplate().getBaseHpReg();
		double cpRegenMultiplier = Config.PLAYER_CP_REGEN_MULTIPLIER;
		double cpRegenBonus = 0;

		/* Формулы для игрока */
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			/* значения по уровню персонажа */
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

			/* значения по состоянию чара */
			if (player.isSitting())
				init *= 1.5;
			else if (!player.isMoving())
				init *= 1.1;
			else if (player.isRunning())
				init *= 0.7;
		}
		/* Формулы для остальных */
		else
		{
			/* значения по состоянию */
			if (!cha.isMoving())
				init *= 1.1;
			else if (cha.isRunning())
				init *= 0.7;
		}

		/* бонус от значения CON */
		init *= cha.getLevelMod() * CONbonus[cha.getStat().getCON()];

		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null) * cpRegenMultiplier + cpRegenBonus;
	}

	@SuppressWarnings("deprecation")
	public static final double calcFestivalRegenModifier(L2PcInstance activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;

		if (festivalId < 0)
			return 0;

		if (oracle == SevenSigns.CABAL_DAWN)
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		else
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];

		double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);
		return 1.0 - (distToCenter * 0.0005);
	}

	public static final double calcSiegeRegenModifer(L2PcInstance activeChar)
	{
		if (activeChar == null || activeChar.getClan() == null)
			return 0;

		Siege siege = SiegeManager.getInstance().getSiege(activeChar);
		if (siege == null || !siege.getIsInProgress())
			return 0;

		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
		if (siegeClan == null || siegeClan.getFlag().size() == 0 || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().valueOf(siegeClan.getFlag().head().getNext()), true))
			return 0;

		return 1.5;
	}


	/* --------------- ОСНОВНЫЕ МЕТОДЫ РАСЧЕТА ЗНАЧЕНИЯ ATTACK DAMAGE --------------- */

	/**
	 * Метод расчета дамага для оружия типа Кинжалы, скилов Blow
	 * @param attacker - атакующий L2Character
	 * @param target - цель атакующего L2Character
	 * @param skill - текущий скилл L2Skill
	 * @param shld - наличие щита Byte
	 * @param ss - наличие соулшотов Boolean
	 * @return Damage
	 */
	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss)
	{
		boolean isPvP = (attacker instanceof L2PlayableInstance) && (target instanceof L2PlayableInstance);
		double power = skill.getPower();
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);

		damage *= calcSkillVulnerability(target, skill);
		damage += calcValakasAttribute(attacker, target, skill);

		// Применяем множитель соулшолотов
		if (ss)
			damage *= 2.;
		
		// Проверка наличия щита + примнение формулы защиты щитом
		switch (shld)
		{
			case 1:
				defence += target.getShldDef();
				break;
			case 2:
				return 1;
		}
		
		// Применяем множитель соулшолотов по скилу
		if (ss && skill.getSSBoost() > 0)
			power *= skill.getSSBoost();
		
		// Формула расчета dmg (криты, критикал бонус..)
		damage = (attacker.calcStat(Stats.CRITICAL_DAMAGE, (damage + power), target, skill) + (attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.5)) * (target.calcStat(Stats.CRIT_VULN, target.getTemplate().baseCritVuln, target, skill));
		
		// Данные сопротивления для NPC
		if (target instanceof L2NpcInstance)
			damage *= ((L2NpcInstance) target).getTemplate().getVulnerability(Stats.DAGGER_WPN_VULN);
		
		// Расчет dmg
		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		damage *= 70. / defence;
		damage += Rnd.nextDouble() * attacker.getRandomDamage(target);	

		// Применяем бонус PVP
		if (isPvP)
		{
			damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			damage /= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}
		
		if (target instanceof L2PcInstance)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if (armor != null)
			{
				if (((L2PcInstance) target).isWearingHeavyArmor())
					damage *= Config.ALT_DAGGER_DMG_VS_HEAVY;
				if (((L2PcInstance) target).isWearingLightArmor())
					damage *= Config.ALT_DAGGER_DMG_VS_LIGHT;
				if (((L2PcInstance) target).isWearingMagicArmor())
					damage *= Config.ALT_DAGGER_DMG_VS_ROBE;
			}
		}
		else
			damage *= Config.ALT_DAGGER_DMG_VS_OTHER;

		return damage < 1 ? 1. : damage;
	}

	/**
	 * Метод расчета физического дамага
	 * @param attacker - атакующий L2Character
	 * @param target - цель атакующего L2Character
	 * @param skill - текущий скилл L2Skill
	 * @param shld - наличие щита Byte
	 * @param crit - критический удар Boolean
	 * @param dual - двйные клинки Boolean
	 * @param ss - наличие соулшотов Boolean
	 * @return Damage
	 */
	public static final double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean crit, boolean dual, boolean ss)
	{
		boolean isPvP = (attacker instanceof L2PlayableInstance) && (target instanceof L2PlayableInstance);
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		damage*=calcSkillVulnerability(target, skill);
		damage+=calcValakasAttribute(attacker, target, skill);

		// Проверка наличия щита + примнение формулы защиты щитом
		switch (shld)
		{
			case 1:
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
					defence += target.getShldDef();
				break;
			case 2:
				return 1.;
		}	
		
		// Применяем множитель соулшолотов
		if (ss)
			damage *= 2;	
		
		// Расчет данных в зависимости от скила
		if (skill != null)
		{
			double skillpower = skill.getPower();
			if (skill.getSkillType() == L2SkillType.FATALCOUNTER)
				skillpower *= (3.5 * (1 - attacker.getStatus().getCurrentHp() / attacker.getMaxHp()));

			float ssboost = skill.getSSBoost();
			if (ssboost <= 0)
				damage += skillpower;
			else if (ssboost > 0)
			{
				if (ss)
				{
					skillpower *= ssboost;
					damage += skillpower;
				}
				else
					damage += skillpower;
			}
		}

		if(Config.USE_LEVEL_PENALTY && skill!=null && !skill.isItemSkill() && skill.getMagicLvl() > 40 && attacker instanceof L2PlayableInstance) {
			int lvl = attacker.getActingPlayer().getLevel();
			int sklvl = skill.getLevel()>100?76:skill.getMagicLvl(); 
			if(lvl - sklvl  < -2 ) { 
				damage *= (1/(skill.getMagicLvl()-lvl));
				crit = false;
			}
			
		} 
	
		// defence modifier depending of the attacker weapon
		L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
			case BOW:
				stat = Stats.BOW_WPN_VULN;
				break;
			case BLUNT:
			case BIGBLUNT:
				stat = Stats.BLUNT_WPN_VULN;
				break;
			case DAGGER:
				stat = Stats.DAGGER_WPN_VULN;
				break;
			case DUAL:
				stat = Stats.DUAL_WPN_VULN;
				break;
			case DUALFIST:
				stat = Stats.DUALFIST_WPN_VULN;
				break;
			case ETC:
				stat = Stats.ETC_WPN_VULN;
				break;
			case FIST:
				stat = Stats.FIST_WPN_VULN;
				break;
			case POLE:
				stat = Stats.POLE_WPN_VULN;
				break;
			case SWORD:
				stat = Stats.SWORD_WPN_VULN;
				break;
			case BIGSWORD:
				stat = Stats.BIGSWORD_WPN_VULN;
				break;
			case PET:
				stat = Stats.PET_WPN_VULN;
				break;
			}
		}
		
		// Расчет критического урона
		if (crit)
		{
			
			damage *= 2 * attacker.getCriticalDmg(target, 1) * target.calcStat(Stats.CRIT_VULN, target.getTemplate().baseCritVuln, target, skill);
			damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill);
			if(weapon!=null && weapon.getItemType() ==L2WeaponType.BOW )
				damage *= attacker.getStat().getBowCritRate();
		}
		damage *= 70. / defence;
	
		// Дамаг самона уменьшаем на 10%
		if (attacker instanceof L2Summon && target instanceof L2PcInstance)
			damage *= 0.9;		
		
		// Расчет статов
		if (stat != null)
		{
			damage = target.calcStat(stat, damage, target, null);
			if (target instanceof L2NpcInstance)
				damage *= ((L2NpcInstance) target).getTemplate().getVulnerability(stat);
		}

		// Рандомный множитель, и делим на 10
		damage += Rnd.nextDouble() * damage / 10;

		// Альтернативная форума для щитов
		if (shld > 0 && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0) damage = 0;
		}

		// Проверка статов (НПЦ)
		if (target instanceof L2NpcInstance)
		{
			Race raceType = ((L2NpcInstance) target).getTemplate().getRace();
			if (raceType != null)
			{
				switch (raceType)
				{
					case UNDEAD:
						damage *= attacker.getStat().getPAtkUndead(target);
						break;
					case BEAST:
						damage *= attacker.getStat().getPAtkMonsters(target);
						break;
					case ANIMAL:
						damage *= attacker.getStat().getPAtkAnimals(target);
						break;
					case PLANT:
						damage *= attacker.getStat().getPAtkPlants(target);
						break;
					case DRAGON:
						damage *= attacker.getStat().getPAtkDragons(target);
						break;
					case BUG:
						damage *= attacker.getStat().getPAtkInsects(target);
						break;
					case GIANT:
						damage *= attacker.getStat().getPAtkGiants(target);
						break;
					case MAGICCREATURE:
						damage *= attacker.getStat().getPAtkMagic(target);
						break;
					default:
						break;
				}
			}
		}
		// Проверка статов (НПЦ)
		if (attacker instanceof L2NpcInstance)
		{
			Race raceType = ((L2NpcInstance) attacker).getTemplate().getRace();
			if (raceType != null)
			{
				switch (raceType)
				{
					case UNDEAD:
						damage /= target.getStat().getPDefUndead(attacker);
						break;
					case BEAST:
						damage /= target.getStat().getPDefMonsters(attacker);
						break;
					case ANIMAL:
						damage /= target.getStat().getPDefAnimals(attacker);
						break;
					case PLANT:
						damage /= target.getStat().getPDefPlants(attacker);
						break;
					case DRAGON:
						damage /= target.getStat().getPDefDragons(attacker);
						break;
					case BUG:
						damage /= target.getStat().getPDefInsects(attacker);
						break;
					case GIANT:
						damage /= target.getStat().getPDefGiants(attacker);
						break;
					case MAGICCREATURE:
						damage /= target.getStat().getPDefMagic(attacker);
						break;
					default:
						break;
				}
			}
		}
		
		// Проверка дамага
		if (damage > 0 && damage < 1)
			damage = 1;
		else if (damage < 0)
			damage = 0;		
		
		// Применяем формулу PVP
		if (isPvP)
		{
			if (skill == null)
			{	
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
				damage /= target.calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
				damage /= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);				
			}			
		}
		
		// Применяем формулу арбалетов, луков от CT 2.2
		if (Config.USE_BOW_CROSSBOW_DISTANCE_PENALTY && attacker instanceof L2PcInstance && weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
				{
					double distance = target.getRangeToTarget(attacker);
					double maxDistance = attacker.getPhysicalAttackRange();
					if (distance > maxDistance)
						distance = maxDistance;
					double factor = distance / maxDistance;
					double calcfactor = Config.BOW_CROSSBOW_DISTANCE_PENALTY + (factor * (1 - Config.BOW_CROSSBOW_DISTANCE_PENALTY));
					damage*=calcfactor;
					break;
				}
			}
		}		

		// Коректировка дамага по конфигам
		if (skill==null)
		{
			if (attacker instanceof L2PcInstance)
			{
				if (((L2PcInstance) attacker).getClassId().isMage())
					damage *= Config.ALT_MAGES_PHYSICAL_DAMAGE_MULTI;
				else
					damage *= Config.ALT_FIGHTERS_PHYSICAL_DAMAGE_MULTI;
			}
			else if (attacker instanceof L2Summon)
				damage *= Config.ALT_PETS_PHYSICAL_DAMAGE_MULTI;
			else if (attacker instanceof L2NpcInstance)
				damage *= Config.ALT_NPC_PHYSICAL_DAMAGE_MULTI;
		}
		else
		{
			if (attacker instanceof L2PcInstance)
			{
				if (target instanceof L2PcInstance)
				{
					L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
					if (armor != null)
					{
						if (((L2PcInstance) target).isWearingHeavyArmor())
							damage *= Config.ALT_FIGHTERS_SKILL_DAMAGE_HEAVY_MULTI;
						if (((L2PcInstance) target).isWearingLightArmor())
							damage *= Config.ALT_FIGHTERS_SKILL_DAMAGE_LIGHT_MULTI;
						if (((L2PcInstance) target).isWearingMagicArmor())
							damage *= Config.ALT_FIGHTERS_SKILL_DAMAGE_ROBE_MULTI;
					}
				}
				else
					damage *= Config.ALT_FIGHTERS_SKILL_DAMAGE_OTHER_MULTI;
			}
		}
		return damage;
	}

	/**
	 * Метод расчета магического дамага для кубика
	 * @param attacker - атакующий L2Character
	 * @param target - цель атакующего L2Character
	 * @param skill - текущий скилл L2Skill
	 * @param mcrit - магический крит Boolean
	 * @return Damage
	 */
	public static final double calcMagicDam(L2CubicInstance attacker, L2Character target, L2Skill skill, boolean mcrit)
	{
		// Если цель в инвуле, то дамаг сводим к 0
		if (target.isInvul())
			return 0;

		// Получаем данные м.атаки и м.защиты
		double mAtk = attacker.getMAtk();
		double mDef = target.getMDef(attacker.getOwner(), skill);

		L2PcInstance owner = attacker.getOwner();

		/* Считаем Damage: 91 * Квадратный Корень числа (mAtk) / mDef * skillPower */
		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower() * calcSkillVulnerability(target, skill);

		
		// Вычисляем щанс неудачной атаки, применяем формулу
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && target.getLevel() - skill.getMagicLevel() <= 9)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
				else
					owner.sendPacket(SystemMessageId.ATTACK_FAILED);
				damage /= 2;
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1);
				sm.addString(target.getName()+ " успешно сопротивляется магии "+skill.getName());
				owner.sendPacket(sm);
				damage = 1;
			}

			if (target instanceof L2PcInstance)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.RESISTED_C1_DRAIN);
					sm.addPcName(owner);
					target.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.RESISTED_C1_MAGIC);
					sm.addPcName(owner);
					target.sendPacket(sm);
				}
			}
		}
		// Вычисление множителя магического крита
		else if (mcrit)
			damage *= 4;

		// Возвращаем дамаг
		return damage;
	}

	/**
	 * Метод расчета магического дамага
	 * @param attacker - атакующий L2Character
	 * @param target - цель атакующего L2Character
	 * @param skill - текущий скилл L2Skill
	 * @param ss - наличие соулшотов Boolean
	 * @param bss - наличие блесс.соулшотов Boolean
	 * @param mcrit - магический крит Boolean
	 * @return Damage
	 */
	public static final double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss, boolean mcrit)
	{
		// Получаем данные м.атаки и м.защиты
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		boolean isPvP = (attacker instanceof L2PlayableInstance) && (target instanceof L2PlayableInstance);

		// Проверка активированых сосок
		// Если активированы BlessedSoulShot, то множим дамаг на 4. Если SouShot, то на 2
		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;
		// Получаем мощность скила
		double power = skill.getPower();
		// Если скил КДЛ, то применяем форумулу КДЛА
		if (skill.getSkillType() == L2SkillType.DEATHLINK)
		{
			double part = attacker.getStatus().getCurrentHp() / attacker.getMaxHp();
			power *= (Math.pow(1.7165 - part, 2) * 0.577);
		}
		
		/* Считаем Damage: 91 * Квадратный Корень числа (mAtk) / mDef * skillPower */
		double damage = 91 * Math.sqrt(mAtk) / mDef * power * calcSkillVulnerability(target, skill);

		// Начиная с хроник С5, самоны теряют 10% дамага в PVP
		if (attacker instanceof L2Summon && target instanceof L2PcInstance)
			damage *= 0.9;
		if(Config.USE_LEVEL_PENALTY && skill!=null && !skill.isItemSkill() && skill.getMagicLvl() > 40 && attacker instanceof L2PlayableInstance) {
			int lvl = attacker.getActingPlayer().getLevel();
			int sklvl = skill.getLevel()>100?76:skill.getMagicLvl(); 
			if(lvl - sklvl  < -2 )  
				damage *= (1/(skill.getMagicLvl()-lvl));
			
		}

		// Вычисляем щанс неудачной атаки, применяем формулу
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			SystemMessage sm;
			if (attacker instanceof L2PcInstance)
			{
			
				L2PcInstance attOwner = attacker.getActingPlayer();
				if (calcMagicSuccess(attacker, target, skill) && (target.getLevel() - attacker.getLevel()) <= 9)
				{
					sm = new SystemMessage(SystemMessageId.S1);
					sm.addString(target.getName()+ " успешно сопротивляется магии "+skill.getName());
					attOwner.sendPacket(sm);

					damage /= 2;
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1);
					sm.addString(target.getName()+ " успешно сопротивляется магии "+skill.getName());
					attOwner.sendPacket(sm);

					if (mcrit)
						damage = 1;
					else
						damage = Rnd.nextBoolean() ? 1 : 0;

					return damage;
				}
			}
		

			if (target instanceof L2PcInstance)
			{
				sm = new SystemMessage(SystemMessageId.S1);
				sm.addString(target.getName()+ " успешно сопротивляется магии "+skill.getName());
				target.getActingPlayer().sendPacket(sm);
			}
		}
		if(!Config.USE_CHAR_LEVEL_MOD && attacker instanceof L2PlayableInstance && target instanceof L2PlayableInstance)
			damage /= 2;
		// Вычисление множителя магического крита
		if (mcrit)
			damage *= Config.MCRIT_RATE;

		
		// Добавляем рандодомный дамаг
		damage += Rnd.nextDouble() * attacker.getRandomDamage(target);
		
		// Применяем PVP бонус
		if (isPvP)
		{
			if (!(skill.isItemSkill() && Config.ALT_ITEM_SKILLS_NOT_INFLUENCED))
			{
				if (skill.isMagic())
				{	
					damage *= attacker.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
					damage /= target.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null);
				}
				else
				{
					damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
					damage /= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
				}
			}
		}

		// Применяем множитель дамага из конфига для игроков
		if (attacker instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).getClassId().isMage())
				damage *= Config.ALT_MAGES_MAGICAL_DAMAGE_MULTI;
			else
				damage *= Config.ALT_FIGHTERS_MAGICAL_DAMAGE_MULTI;
		}
		// Применяем множитель дамага из конфига для самонов
		else if (attacker instanceof L2Summon)
			damage *= Config.ALT_PETS_MAGICAL_DAMAGE_MULTI;
		// Применяем множитель дамага из конфига для NPC
		else if (attacker instanceof L2NpcInstance)
			damage *= Config.ALT_NPC_MAGICAL_DAMAGE_MULTI;

		if(attacker instanceof L2PcInstance && attacker.getLevel()>40) {
			if(skill!=null && (attacker.getLevel() - skill.getMagicLvl())>20) {
				damage /= 50;
				if(damage<1)
					damage = 1;
			}
		}
		// Возвращаем дамаг
		return damage;
	}

	
	/* --------------- ОСНОВНЫЕ МЕТОДЫ РАСЧЕТА ЗНАЧЕНИЯ CHANCE METHOD'S --------------- */
	
	/**
	 * @param rate
	 * @return
	 */
	public static final boolean calcCrit(double rate)
	{
		return rate > Rnd.get(1000);
	}

	/**
	 * @param activeChar
	 * @param target
	 * @param chance
	 * @return
	 */
	public static final boolean calcBlow(L2Character activeChar, L2Character target, int chance)
	{
		double rate = activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getStat().getDEX() - 20) / 100), target, null);
		boolean success = rate > Rnd.get(100);

		if(activeChar instanceof L2PcInstance && ((L2PcInstance) activeChar).isShowSkillChance() && !Config.SHOW_DEBUFF_ONLY)
		{
			if (rate > 100)
				rate = 100;
			else if (rate < 0)
				rate = 0;

			if(success)
				((L2PcInstance) activeChar).sendMessage(String.format(Message.getMessage(((L2PcInstance) activeChar), Message.MessageId.MSG_SKILL_CHANS_SUCCES),(int)rate));
			else
				((L2PcInstance) activeChar).sendMessage(String.format(Message.getMessage(((L2PcInstance) activeChar), Message.MessageId.MSG_SKILL_CHANS),(int)rate));
		}

		return success;
	}

	/**
	 * @param activeChar
	 * @param target
	 * @param baseLethal
	 * @param magiclvl
	 * @return
	 */
	public static final double calcLethal(L2Character activeChar, L2Character target, int baseLethal, int magiclvl)
	{
		double chance = 0;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + activeChar.getLevel()) / 2) - 1 - target.getLevel();

			if (delta >= -3)
				chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
			else if (delta < -3 && delta >= -9)
				chance = (-3) * (baseLethal / (delta));
			else
				chance = baseLethal / 15;
		}
		else
			chance = (baseLethal * ((double)activeChar.getLevel() / target.getLevel()));
		return activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
	}

	/**
	 * @param activeChar
	 * @param target
	 * @param skill
	 * @return
	 */
	public static final boolean calcLethalHit(L2Character activeChar, L2Character target, L2Skill skill)
	{
		// Проверка цели, инвул и пертификация не дает шанса леталу
		if (target.isInvul() || target.isPetrified())
			return false;

		// Проверяем иммунитет.
		if (target.isBoss() || target.isDoor() || target.isNpc() && target.getNpc().isLethalImmune())
			return false;

		// Расчет общего щанса летала
		int chance = 100;
		double rate = 0;
		switch (skill.getLethalType())
		{
			case 1:
				rate = Config.ALT_LETHAL_RATE_OTHER;
				break;
			case 2:
				rate = Config.ALT_LETHAL_RATE_DAGGER;
				break;
			case 3:
				rate = Config.ALT_LETHAL_RATE_ARCHERY;
				break;
		}
		chance *= target.calcStat(Stats.LETHAL_VULN, 1, target, skill);
		// Расчет летал страйка
		if (skill.getLethalChance2() > 0 && Rnd.get(chance) < (rate*calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel())))
		{
			if (target.isPlayer())
			{
				target.getStatus().setCurrentHp(1);
				target.getStatus().setCurrentCp(1);
				activeChar.sendPacket(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL);
				target.sendPacket(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL);
			}
			else if (target.isNpc())
			{
				target.reduceCurrentHp(target.getStatus().getCurrentHp() - 1, activeChar, skill);
				activeChar.sendPacket(SystemMessageId.LETHAL_STRIKE);
			}
		}
		// Расчет халф килла
		else if (skill.getLethalChance1() > 0 && Rnd.get(chance) < (rate*calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel())))
		{
			if (target.isPlayer())
				target.getStatus().setCurrentCp(1);
			else
				target.reduceCurrentHp(target.getStatus().getCurrentHp() / 2, activeChar, skill);
		}
		return false;
	}

	/**
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return
	 */
	public static final boolean calcCrit(L2Character attacker, L2Character target, double rate)
	{
		int critHit = Rnd.get(1000);
		if (attacker instanceof L2PcInstance)
		{
			if (attacker.isBehindTarget())
				critHit = Rnd.get(700);
			else if (!attacker.isFacing(target, 60) && !attacker.isBehindTarget())
				critHit = Rnd.get(800);
			critHit = Rnd.get(900);
		}
		return rate > critHit;
	}

	/**
	 * @param mRate
	 * @return
	 */
	public static final boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}

	/**
	 * @param target
	 * @param dmg
	 * @return
	 */
	public static final boolean calcAtkBreak(L2Character target, double dmg)
	{
		if (target.isRaid() || target.isInvul() || dmg <= 0)
			return false;

		if (target.getFusionSkill() != null)
			return true;

		double init = 0;

		if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow())
			init = 15;
		else if (Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow() && target.getActiveWeaponItem() != null && target.getActiveWeaponItem().getItemType() == L2WeaponType.BOW)
			init = 15;

		else
			return false;

		init += Math.sqrt(13 * dmg);

		init -= (MENbonus[target.getStat().getMEN()] * 100 - 100);

		double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);

		if (rate > 99)
			rate = 99;
		else if (rate < 1)
			rate = 1;

		return Rnd.get(100) < rate;
	}

	/**
	 * @param attacker
	 * @param target
	 * @param atkSpd
	 * @param base
	 * @return
	 */
	public static final int calcPAtkSpd(L2Character attacker, L2Character target, double atkSpd, double base)
	{
		if (attacker instanceof L2PcInstance)
			base *= Config.ALT_ATTACK_DELAY;

		if (atkSpd < 10)
			atkSpd = 10;

		return (int) (base / atkSpd);
	}

	/**
	 * @param attacker
	 * @param skill
	 * @param time
	 * @return
	 */
	public static final int calcAtkSpd(L2Character attacker, L2Skill skill, double time)
	{
		if (skill.isItemSkill() && Config.ALT_ITEM_SKILLS_NOT_INFLUENCED)
			return (int) time;
		else if (skill.isMagic())
			return (int) (time * 333 / attacker.getMAtkSpd());
		else
			return (int) (time * 333 / attacker.getPAtkSpd());
	}

	/**
	 * @param attacker
	 * @param target
	 * @return
	 */
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		int delta = attacker.getAccuracy() - target.getEvasionRate(attacker);
		int chance;
		if (delta >= 10)
			chance = 980;
		else
		{
			switch (delta)
			{
				case 9:
					chance = 975;
					break;
				case 8:
					chance = 970;
					break;
				case 7:
					chance = 965;
					break;
				case 6:
					chance = 960;
					break;
				case 5:
					chance = 955;
					break;
				case 4:
					chance = 945;
					break;
				case 3:
					chance = 935;
					break;
				case 2:
					chance = 925;
					break;
				case 1:
					chance = 915;
					break;
				case 0:
					chance = 905;
					break;
				case -1:
					chance = 890;
					break;
				case -2:
					chance = 875;
					break;
				case -3:
					chance = 860;
					break;
				case -4:
					chance = 845;
					break;
				case -5:
					chance = 830;
					break;
				case -6:
					chance = 815;
					break;
				case -7:
					chance = 800;
					break;
				case -8:
					chance = 785;
					break;
				case -9:
					chance = 770;
					break;
				case -10:
					chance = 755;
					break;
				case -11:
					chance = 735;
					break;
				case -12:
					chance = 715;
					break;
				case -13:
					chance = 695;
					break;
				case -14:
					chance = 675;
					break;
				case -15:
					chance = 655;
					break;
				case -16:
					chance = 625;
					break;
				case -17:
					chance = 595;
					break;
				case -18:
					chance = 565;
					break;
				case -19:
					chance = 535;
					break;
				case -20:
					chance = 505;
					break;
				case -21:
					chance = 455;
					break;
				case -22:
					chance = 405;
					break;
				case -23:
					chance = 355;
					break;
				case -24:
					chance = 305;
					break;
				default:
					chance = 275;
			}
			if (!attacker.isInFrontOfTarget())
			{
				if (attacker.isBehindTarget())
					chance *= 1.2;
				else
					chance *= 1.1;

				if (chance > 980)
					chance = 980;
			}
		}
		int seed = 0;
		boolean seedCaled = false;
		for(int i = 0;i<10;i++) {
			seed =  Rnd.get(1000);
			if(seed > 0 ) { seedCaled = true; break; }
		}
		if(!seedCaled) {
			_log.warn("Impossible! 10 iterations got 0 for "+attacker.getName()+" to "+target.getName());
		}
		return chance < Rnd.get(1000);
	}

	/**
	 * @param attacker
	 * @param target
	 * @param sendSysMsg
	 * @return
	 */
	public static byte calcShldUse(L2Character attacker, L2Character target, boolean sendSysMsg)
	{
		int dex = target.getStat().getDEX();
		if(dex>=MAX_STAT_VALUE)
			dex = MAX_STAT_VALUE-1;
		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * DEXbonus[dex];

		if (shldRate == 0.0)
			return 0;

		L2ItemInstance shieldInst = target.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (shieldInst==null || shieldInst.getItemType()!= L2WeaponType.NONE)
			return 0;

		double shldAngle = target.calcStat(Stats.SHIELD_ANGLE, 60, null, null);

		if (shldAngle < 360 && (!target.isFacing(attacker, (int) shldAngle)))
			return 0;

		if (attacker != null && attacker.getActiveWeaponItem() != null && attacker.getActiveWeaponItem().getItemType() == L2WeaponType.BOW)
			shldRate *= 1.5;

		byte shldSuccess = 0;

		if (shldRate > 0 && 100 - Config.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100))
			shldSuccess = 2;
		else if (shldRate > Rnd.get(100))
			shldSuccess = 1;

		if (sendSysMsg && target instanceof L2PcInstance)
		{
			L2PcInstance enemy = (L2PcInstance)target;
			switch (shldSuccess)
			{
				case 1:
					enemy.sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				case 2:
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}
		return shldSuccess;
	}

	/**
	 * @param attacker
	 * @param target
	 * @return
	 */
	public static byte calcShldUse(L2Character attacker, L2Character target)
	{
		return calcShldUse(attacker, target, true);
	}

	/**
	 * @param actor
	 * @param target
	 * @param skill
	 * @return
	 */
	public static boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		double defence = 0;
		double attack = 0;

		if (skill.isActive() && skill.isOffensive() && !skill.isNeutral())
			defence = target.getMDef(actor, skill);

		if (actor instanceof L2PcInstance)
			attack = 3.7 * actor.getMAtk(target, skill) * calcSkillVulnerability(target, skill);
		else
			attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(target, skill);

		double d = attack - defence;
		d /= attack + defence;
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}

	/**
	 * @param target
	 * @param skill
	 * @param type
	 * @return
	 */
	public static double calcSkillVulnerability(L2Character target, L2Skill skill)
	{
		if (skill != null)
			return calcSkillVulnerability(target, skill, skill.getSkillType());

		return calcSkillVulnerability(target, null, null);
	}

	public static double calcSkillVulnerability(L2Character target, L2Skill skill, L2SkillType type)
	{
		double multiplier = 1;

		if (skill != null)
		{
			switch (skill.getElement())
			{
			case L2Skill.ELEMENT_EARTH:
				multiplier = target.calcStat(Stats.EARTH_VULN, multiplier, target, skill);
				break;
			case L2Skill.ELEMENT_FIRE:
				multiplier = target.calcStat(Stats.FIRE_VULN, multiplier, target, skill);
				break;
			case L2Skill.ELEMENT_WATER:
				multiplier = target.calcStat(Stats.WATER_VULN, multiplier, target, skill);
				break;
			case L2Skill.ELEMENT_WIND:
				multiplier = target.calcStat(Stats.WIND_VULN, multiplier, target, skill);
				break;
			case L2Skill.ELEMENT_HOLY:
				multiplier = target.calcStat(Stats.HOLY_VULN, multiplier, target, skill);
				break;
			case L2Skill.ELEMENT_DARK:
				multiplier = target.calcStat(Stats.DARK_VULN, multiplier, target, skill);
				break;
			}

			// Finally, calculate skilltype vulnerabilities
			if (type != null)
			{
				switch (type)
				{
				case BLEED:
					multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
					break;
				case POISON:
					multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
					break;
				case STUN:
					multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
					break;
				case PARALYZE:
					multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
					break;
				case ROOT:
					multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
					break;
				case SLEEP:
					multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
					break;
				case MUTE:
				case FEAR:
				case BETRAY:
				case AGGREDUCE_CHAR:
					multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
					break;
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
					multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
					break;
				case DEBUFF:
				case WEAKNESS:
					multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
					break;
				case CANCEL:
					multiplier = target.calcStat(Stats.CANCEL_VULN, multiplier, target, null);
					break;
				}
			}
		}
		return multiplier;
	}

	/**
	 * 
	 * @param skill
	 * @param attacker
	 * @param target
	 * @return
	 */
	public static double calcSkillProficiency(L2Skill skill, L2Character attacker, L2Character target)
	{
		double multiplier = 1;
		if (skill != null)
		{
			L2SkillType type = skill.getSkillType();
			if (type == L2SkillType.PDAM || type == L2SkillType.MDAM || type == L2SkillType.DRAIN || type == L2SkillType.WEAPON_SA)
				type = skill.getEffectType();
			
			if (type != null)
			{
				switch (type)
				{
					case BLEED:
						multiplier = attacker.calcStat(Stats.BLEED_PROF, multiplier, target, null);
						break;
					case POISON:
						multiplier = attacker.calcStat(Stats.POISON_PROF, multiplier, target, null);
						break;
					case STUN:
						multiplier = attacker.calcStat(Stats.STUN_PROF, multiplier, target, null);
						break;
					case PARALYZE:
						multiplier = attacker.calcStat(Stats.PARALYZE_PROF, multiplier, target, null);
						break;
					case ROOT:
						multiplier = attacker.calcStat(Stats.ROOT_PROF, multiplier, target, null);
						break;
					case SLEEP:
						multiplier = attacker.calcStat(Stats.SLEEP_PROF, multiplier, target, null);
						break;
					case MUTE:
					case FEAR:
					case ERASE:
					case BETRAY:
					case AGGREDUCE_CHAR:
						multiplier = attacker.calcStat(Stats.DERANGEMENT_PROF, multiplier, target, null);
						break;
					case CONFUSION:
					case CONFUSE_MOB_ONLY:
						multiplier = attacker.calcStat(Stats.CONFUSION_PROF, multiplier, target, null);
						break;
					case DEBUFF:
					case WEAKNESS:
						multiplier = attacker.calcStat(Stats.DEBUFF_PROF, multiplier, target, null);
						break;
					default:
						break;
				}
			}
		}
		return multiplier;
	}

	/**
	 * @param type
	 * @param target
	 * @return
	 */
	public static double calcSkillStatModifier(L2SkillType type, L2Character target)
	{
		double multiplier = 1;
		if (type == null) return multiplier;
		try 
		{
			switch (type)
			{
				case STUN:
				case BLEED:
				case POISON:
					multiplier = 2 - Math.sqrt(CONbonus[target.getStat().getCON()]);
					break;
				case SLEEP:
				case DEBUFF:
				case WEAKNESS:
				case ERASE:
				case ROOT:
				case MUTE:
				case FEAR:
				case BETRAY:
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
				case AGGREDUCE_CHAR:
				case PARALYZE:
					multiplier = 2 - Math.sqrt(MENbonus[target.getStat().getMEN()]);
					break;
				default:
					return multiplier;
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}

		if (multiplier < 0)
			multiplier = 0;
		return multiplier;
	}

	/**
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param ss
	 * @param sps
	 * @param bss
	 * @return
	 */
	public static boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean sps, boolean bss)
	{
		if (skill.isMagic() && target.isPreventedFromReceivingBuffs())
			return false;
		if (skill.isBadBuff())
			return true;
		
		int value = skill.getActivateRate() == -1 ? (int) skill.getPower() : skill.getActivateRate();
		int lvlDepend = skill.getLevelDepend();
		
		L2SkillType type = skill.getSkillType();

		if (type == L2SkillType.PDAM || type == L2SkillType.MDAM || type == L2SkillType.DRAIN || type == L2SkillType.WEAPON_SA)
		{
			if (skill.getActivateRate() == -1)
				value = skill.getEffectPower();
			type = skill.getEffectType();
		}
		if (type == null)
		{
			if (skill.getSkillType() == L2SkillType.PDAM)
				type = L2SkillType.STUN;
			else if (skill.getSkillType() == L2SkillType.MDAM)
				type = L2SkillType.PARALYZE;
		}
		if (value == 0)
		{
			value = (type == L2SkillType.PARALYZE) ? 50 : (type == L2SkillType.FEAR) ? 40 : 80;
		}
		/*if (lvlDepend == 0)
		{
			lvlDepend = 1;
		}*/

		double statmodifier = calcSkillStatModifier(type, target);
		double resmodifier = calcSkillVulnerability(target, skill, type);

		int ssmodifier = 100;
		if (bss)
			ssmodifier = 200;
		else if (sps || ss)
			ssmodifier = 150;

		int rate = (int) (value * statmodifier);
		if (skill.isMagic())
		{
			rate += (int) (Math.pow((double) attacker.getMAtk(target, skill) / (target.getMDef(attacker, skill)), 0.1) * 100) - 100;
		}
		if (ssmodifier != 100)
		{
			if (rate > 10000 / (100 + ssmodifier))
				rate = 100 - (100 - rate) * 100 / ssmodifier;
			else
				rate = rate * ssmodifier / 100;
		}
		if (lvlDepend > 0)
		{
			double delta = 0;
			int attackerLvlmod = attacker.getLevel();
			int targetLvlmod = target.getLevel();

			if (attackerLvlmod >= 70)
				attackerLvlmod = ((attackerLvlmod - 69) * 2) + 70;
			if (targetLvlmod >= 70)
				targetLvlmod = ((targetLvlmod - 69) * 2) + 70;

			if (skill.getMagicLevel() == 0)
				delta = attackerLvlmod - targetLvlmod;
			else
				delta = ((skill.getMagicLevel() + attackerLvlmod) / 2) - targetLvlmod;

			double deltamod = 1;

			if (delta < -3)
			{
				if (delta <= -20)
					deltamod = 0.05;
				else
				{
					deltamod = 1 - ((-1) * (delta / 20));
					if (deltamod >= 1)
						deltamod = 0.05;
				}
			}
			else
				deltamod = 1 + ((delta + 3) / 75);

			if (deltamod < 0)
				deltamod *= -1;

			rate *= deltamod;
		}
		if (rate > 99)
			rate = 99;
		else if (rate < 1)
			rate = 1;

		rate *= resmodifier * calcSkillProficiency(skill, attacker, target);

		boolean success = Rnd.get(100) <= rate;
		if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isShowSkillChance())
			if(success)
				((L2PcInstance) attacker).sendMessage(String.format(Message.getMessage(((L2PcInstance) attacker), Message.MessageId.MSG_SKILL_CHANS_SUCCES),(int)rate));
			else
				((L2PcInstance) attacker).sendMessage(String.format(Message.getMessage(((L2PcInstance) attacker), Message.MessageId.MSG_SKILL_CHANS),rate));

		return success;
	}

	/**
	 * @param ch
	 * @return
	 */
/*	public static double calcSPRuneModifed(L2Character ch)
	{
		L2PcInstance player=null;
		if (ch instanceof L2PcInstance)
			player = (L2PcInstance)ch;
		if (ch instanceof L2Summon)
			player = ((L2Summon)ch).getOwner();
		if (player==null)
			return 1.0;
		
		// Руны дающие 10% прироста (в сумме не более 50%)
		int allRunes1Id[] = {22058,22059,22060,22061};
		int itemsCnt=0;
		for (int runeId : allRunes1Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					itemsCnt++;
			}
		}
		// Руны дающие 30% прироста
		int allRunes2Id[] = {20341,20343,20345};
		boolean rune30=false;
		for (int runeId : allRunes2Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					rune30=true;
			}
		}
		// Руны дающие 50% прироста
		int allRunes3Id[] = {20342,20344,20346};
		boolean rune50=false;
		for (int runeId : allRunes3Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					rune50=true;
			}
		}
		if(itemsCnt>5)
			itemsCnt=5;
		if (rune30 && itemsCnt<3)
			itemsCnt=3;
		if (rune50)
			itemsCnt=5;
		if (itemsCnt>0)
			return (1.0+((double)itemsCnt/10.0));
		return 1.0;
	}
*/	
	/**
	 * @param ch
	 * @return
	 */
/*	public static double calcExpRuneModifed(L2Character ch)
	{
		L2PcInstance player=null;
		if (ch instanceof L2PcInstance)
			player = (L2PcInstance)ch;
		if (ch instanceof L2Summon)
			player = ((L2Summon)ch).getOwner();
		if (player==null)
			return 1.0;
		
		// Руны дающие 10% прироста (в сумме не более 50%)
		int allRunes1Id[] = {22054,22055,22056,22057};
		int itemsCnt=0;
		for (int runeId : allRunes1Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					itemsCnt++;
			}
		}
		// Руны дающие 30% прироста
		int allRunes2Id[] = {20335,20337,20339};
		boolean rune30=false;
		for (int runeId : allRunes2Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					rune30=true;
			}
		}
		// Руны дающие 50% прироста
		int allRunes3Id[] = {20336,20338,20340};
		boolean rune50=false;
		for (int runeId : allRunes3Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					rune50=true;
			}
		}
		if(itemsCnt>5)
			itemsCnt=5;
		if (rune30 && itemsCnt<3)
			itemsCnt=3;
		if (rune50)
			itemsCnt=5;
		if (itemsCnt>0)
			return (1.0+((double)itemsCnt/10.0));
		return 1.0;
	}	
*/	
	/**
	 * @param ch
	 * @return
	 */
/*	public static int calcCrystallRuneModifed(L2Character ch)
	{
		L2PcInstance player=null;
		if (ch instanceof L2PcInstance)
			player = (L2PcInstance)ch;
		if (player==null)
			return 0;
		// Руны дающие снижение на 1 уровнь  при расчете штрафа за грейд (в сумме не более 5 уровней)
		int allRunes1Id[] = {22062,22063,22064,22065};
		int itemsCnt=0;
		for (int runeId : allRunes1Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					itemsCnt++;
			}
		}
		// Руны дающие снижение на 3 уровня при расчете штрафа за грейд
		int allRunes2Id[] = {20347,20349,20351};
		boolean rune30=false;
		for (int runeId : allRunes2Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					rune30=true;
			}
		}
		// Руны дающие снижение на 3 уровня при расчете штрафа за грейд
		int allRunes3Id[] = {20348,20350,20352};
		boolean rune50=false;
		for (int runeId : allRunes3Id)
		{
			for(L2ItemInstance item : player.getInventory().getAllItemsByItemId(runeId))
			{
				if (TimedItemControl.getInstance().isActiveItem(item))
					rune50=true;
			}
		}
		if(itemsCnt>5)
			itemsCnt=5;
		if (rune30 && itemsCnt<3)
			itemsCnt=3;
		if (rune50)
			itemsCnt=5;
		if (itemsCnt>0)
			return itemsCnt;
		return 0;
	}	
*/
	/**
	 * @param ch
	 * @return
	 */
	public static double calcWeightRuneModifed(L2Character ch)
	{
		return 1;
	}	
	
	/**
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return
	 */
	public static boolean calcMagicSuccess(L2Character attacker, L2Character target, L2Skill skill)
	{
		double lvlDifference = (target.getLevel() - (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()));
		if(!Config.USE_CHAR_LEVEL_MOD && attacker.getActingPlayer()!= null && target.getActingPlayer()!=null)
			lvlDifference = lvlDifference > 1 ? 1 : lvlDifference; 
		int rate = Math.round((float) (Math.pow(1.3, lvlDifference) * 100));

		boolean success = rate < Rnd.get(10000);
		if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isShowSkillChance() && !Config.SHOW_DEBUFF_ONLY)
		{
			if (rate > 10000)
				rate = 10000;
			else if (rate < 0)
				rate = 0;

			if(success)
				((L2PcInstance) attacker).sendMessage(String.format(Message.getMessage(((L2PcInstance) attacker), Message.MessageId.MSG_SKILL_CHANS_SUCCES),(100 - rate/100)));
			else
				((L2PcInstance) attacker).sendMessage(String.format(Message.getMessage(((L2PcInstance) attacker), Message.MessageId.MSG_SKILL_CHANS),(100 - rate/100)));
		}

		return success;
	}

	/**
	 * @param skill
	 * @return
	 */
	public static boolean calculateUnlockChance(L2Skill skill)
	{
		int level = skill.getLevel();
		int chance = 0;
		switch (level)
		{
			case 1:
				chance = 30;
				break;
			case 2:
				chance = 50;
				break;
			case 3:
				chance = 75;
				break;
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				chance = 100;
				break;
		}
		return Rnd.get(120) <= chance;
	}

	/**
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param ss
	 * @param bss
	 * @return
	 */
	public static double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;

		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker) * (mp / 97)) / mDef;
		damage *= calcSkillVulnerability(target, skill);
		return damage;
	}

	/**
	 * @param baseRestorePercent
	 * @param casterWIT
	 * @return
	 */
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, int casterWIT)
	{
		double restorePercent = baseRestorePercent;
		double modifier = WITbonus[casterWIT];

		if (restorePercent != 100 && restorePercent != 0)
		{
			restorePercent = baseRestorePercent * modifier;

			if (restorePercent - baseRestorePercent > 20.0)
				restorePercent = baseRestorePercent + 20.0;
		}

		if (restorePercent > 100)
			restorePercent = 100;
		if (restorePercent < baseRestorePercent)
			restorePercent = baseRestorePercent;

		return restorePercent;
	}

	/**
	 * @param activeChar
	 * @return
	 */
	public static double getINTBonus(L2Character activeChar)
	{
		return INTbonus[activeChar.getStat().getINT()];
	}

	/**
	 * @param activeChar
	 * @return
	 */
	public static double getSTRBonus(L2Character activeChar)
	{
		return STRbonus[activeChar.getStat().getSTR()];
	}

	/**
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param shld
	 * @return
	 */
	public static boolean calcCubicSkillSuccess(L2CubicInstance attacker, L2Character target, L2Skill skill)
	{
		L2SkillType type = skill.getSkillType();

		if (target.isRaid() && (type == L2SkillType.CONFUSION || type == L2SkillType.MUTE || type == L2SkillType.PARALYZE || type == L2SkillType.ROOT || type == L2SkillType.FEAR || type == L2SkillType.SLEEP || type == L2SkillType.STUN || type == L2SkillType.DEBUFF || type == L2SkillType.AGGDEBUFF))
			return false; // these skills should not work on RaidBoss

		// if target reflect this skill then the effect will fail
		if (calcSkillReflect(target, skill) != SKILL_REFLECT_FAILED)
			return false;

		int value = (int) skill.getPower();
		int lvlDepend = skill.getLevelDepend();

		if (type == L2SkillType.PDAM || type == L2SkillType.MDAM)
		{
			value = skill.getEffectPower();
			type = skill.getEffectType();
		}

		if (value == 0)
			value = (type == L2SkillType.PARALYZE) ? 50 : (type == L2SkillType.FEAR) ? 40 : 80;
		if (lvlDepend == 0)
			lvlDepend = (type == L2SkillType.PARALYZE || type == L2SkillType.FEAR) ? 1 : 2;

		double statmodifier = calcSkillStatModifier(type, target);
		double resmodifier = calcSkillVulnerability(target, skill);

		int rate = (int) ((value * statmodifier) * resmodifier);
		if (skill.isMagic())
		{
			rate += (int) (Math.pow((double) attacker.getMAtk() / (target.getMDef(attacker.getOwner(), skill)), 0.2) * 100) - 100;
		}

		//lvl modifier.
		if (lvlDepend > 0)
		{
			double delta = 0;
			int attackerLvlmod = attacker.getOwner().getLevel();
			int targetLvlmod = target.getLevel();

			if (attackerLvlmod >= 70)
				attackerLvlmod = ((attackerLvlmod - 69) * 2) + 70;
			if (targetLvlmod >= 70)
				targetLvlmod = ((targetLvlmod - 69) * 2) + 70;

			if (skill.getMagicLevel() == 0)
				delta = attackerLvlmod - targetLvlmod;
			else
				delta = ((skill.getMagicLevel() + attackerLvlmod) / 2) - targetLvlmod;

			double deltamod = 1;

			if (delta + 3 < 0)
			{
				if (delta <= -20)
					deltamod = 0.05;
				else
				{
					deltamod = 1 - ((-1) * (delta / 20));
					if (deltamod >= 1)
						deltamod = 0.05;
				}
			}
			else
				deltamod = 1 + ((delta + 3) / 75);

			if (deltamod < 0)
				deltamod *= -1;

			rate *= deltamod;
		}

		if (rate > 99)
			rate = 99;
		else if (rate < 1)
			rate = 1;
		boolean success = Rnd.get(100) < rate; 
		if(attacker.getOwner().isShowSkillChance()) 
			attacker.getOwner().sendMessage("Cubic skill "+skill.getName()+" chance is "+rate+", "+(success?"success":"unsuccess"));
		
		return success;
	}

	/**
	 * @param target
	 * @param skill
	 * @return
	 */
	public static boolean calcPhysicalSkillEvasion(L2Character target, L2Skill skill)
	{
		if (skill.isMagic() && skill.getSkillType() != L2SkillType.BLOW || skill.getCastRange() > 40)
			return false;

		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill);
	}

	/**
	 * @param actor
	 * @param skill
	 * @return
	 */
	public static boolean calcSkillMastery(L2Character actor, L2Skill skill)
	{
		if (skill.getSkillType() == L2SkillType.FISHING)
			return false;
		
		if (skill.isChance())
			return false;

		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);

		if (actor instanceof L2PcInstance)
		{
			if (((L2PcInstance)actor).isMageClass())
				val *= getINTBonus(actor);
			else
				val *= getSTRBonus(actor);
		}
		return Rnd.get(100) < val;
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return
	 */
    public static byte calcSkillReflect(L2Character target, L2Skill skill)
    {
		if (!skill.canBeReflected())
			return SKILL_REFLECT_FAILED;
		
		// only magic and melee skills can be reflected
		if (!skill.isMagic() && 
				(skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE))
			return SKILL_REFLECT_FAILED;

		byte reflect = SKILL_REFLECT_FAILED;
		// check for non-reflected skilltypes, need additional retail check
		switch (skill.getSkillType())
		{
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
				return SKILL_REFLECT_FAILED;
			// these skill types can deal damage
			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
				final Stats stat = skill.isMagic() ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE;
				final double venganceChance = target.getStat().calcStat(stat, 0, target, skill);
				if (venganceChance > Rnd.get(100))
					reflect |= SKILL_REFLECT_VENGEANCE;					
				break;
		}

		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		
		if( Rnd.get(100) < reflectChance)
			reflect |= SKILL_REFLECT_SUCCEED;

		return reflect;
    }
    
    /**
     * Метод расчета бонусов для скилов Valakas
     * @param attacker
     * @param target
     * @param skill
     * @return
     */
    public static double calcValakasAttribute(L2Character attacker, L2Character target, L2Skill skill)
    {
    	double calcPower = 0;  
    	double calcDefen = 0;  

    	if (skill != null && skill.getAttributeName().contains("valakas"))
    	{
    		calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
    		calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
    	}
    	else
    	{
    		calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
    		if (calcPower > 0)
    		{
    			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
        		calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
    		}
    	}
    	return calcPower - calcDefen;
    }
}
