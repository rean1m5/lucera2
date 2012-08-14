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

import ru.catssoftware.util.StatsSet;

/**
 * Base template for all type of characters
 * this template has property that will be set by setters.
 * <br/>
 * <br/>
 * <font color="red">
 * <b>Property don't change in the time, this is just a template, not the currents status
 * of characters !</b>
 * </font>
 */
public class L2CharTemplate
{
	// BaseStats
	private int		baseSTR;
	private int		baseCON;
	private int		baseDEX;
	private int		baseINT;
	private int		baseWIT;
	private int		baseMEN;
	private float	baseHpMax;
	private float	baseCpMax;
	private float	baseMpMax;

	/** HP Regen base */
	private float	baseHpReg;

	/** MP Regen base */
	private float	baseMpReg;

	private int		basePAtk;
	private int		baseMAtk;
	private int		basePDef;
	private int		baseMDef;
	private int		basePAtkSpd;
	private int		baseMAtkSpd;
	private float	baseMReuseRate;
	private int		baseShldDef;
	private int		baseAtkRange;
	private int		baseShldRate;
	private int		baseCritRate;
	private int		baseMCritRate;
	private int		baseRunSpd;
	private int		baseWalkSpd;
	// SpecialStats
	private int		baseBreath;
	private int		baseAggression;
	private int		baseBleed;
	private int		basePoison;
	private int		baseStun;
	private int		baseRoot;
	private int		baseMovement;
	private int		baseConfusion;
	private int		baseSleep;
	private int		baseFire;
	private int		baseWind;
	private int		baseWater;
	private int		baseEarth;
	private int		baseHoly;
	private int		baseDark;

	public double	baseAggressionVuln;
	public double	baseBleedVuln;
	public double	basePoisonVuln;
	public double	baseStunVuln;
	public double	baseRootVuln;
	public double	baseMovementVuln;
	public double	baseConfusionVuln;
	public double	baseSleepVuln;
	public double	baseFireVuln;
	public double	baseWindVuln;
	public double	baseWaterVuln;
	public double	baseEarthVuln;
	public double	baseHolyVuln;
	public double	baseDarkVuln;
	public double	baseCritVuln;

	public double	baseCancelVuln;

	private boolean	isUndead;

	private int		baseMpConsumeRate;
	private int		baseHpConsumeRate;

	private double	collisionRadius;
	private double	collisionHeight;
	private double	fCollisionRadius;
	private double	fCollisionHeight;

	/**
	 * Empty constructor (we have to use setter to initialize the object).
	 *
	 * Be carefull, setter don't do the same verification that instantiation with statset {@link #L2CharTemplate(StatsSet)}
	 * Don't use it !
	 * This constructor is designed for hibernate
	 */
	public L2CharTemplate()
	{
	}

	public L2CharTemplate(StatsSet set)
	{
		// Base stats
		baseSTR = set.getInteger("baseSTR");
		baseCON = set.getInteger("baseCON");
		baseDEX = set.getInteger("baseDEX");
		baseINT = set.getInteger("baseINT");
		baseWIT = set.getInteger("baseWIT");
		baseMEN = set.getInteger("baseMEN");
		baseHpMax = set.getFloat("baseHpMax");
		baseCpMax = set.getFloat("baseCpMax");
		baseMpMax = set.getFloat("baseMpMax");
		baseHpReg = set.getFloat("baseHpReg");
		baseMpReg = set.getFloat("baseMpReg");
		basePAtk = set.getInteger("basePAtk");
		baseMAtk = set.getInteger("baseMAtk");
		basePDef = set.getInteger("basePDef");
		baseMDef = set.getInteger("baseMDef");
		basePAtkSpd = set.getInteger("basePAtkSpd");
		baseMAtkSpd = set.getInteger("baseMAtkSpd");
		baseMReuseRate = set.getFloat("baseMReuseDelay", 1.f);
		baseShldDef = set.getInteger("baseShldDef");
		baseAtkRange = set.getInteger("baseAtkRange");
		baseShldRate = set.getInteger("baseShldRate");
		baseCritRate = set.getInteger("baseCritRate");
		baseMCritRate = set.getInteger("baseMCritRate", 8);
		baseRunSpd = set.getInteger("baseRunSpd");
		baseWalkSpd = set.getInteger("baseWalkSpd");

		// SpecialStats
		baseBreath = set.getInteger("baseBreath", 100);
		baseAggression = set.getInteger("baseAggression", 0);
		baseBleed = set.getInteger("baseBleed", 0);
		basePoison = set.getInteger("basePoison", 0);
		baseStun = set.getInteger("baseStun", 0);
		baseRoot = set.getInteger("baseRoot", 0);
		baseMovement = set.getInteger("baseMovement", 0);
		baseConfusion = set.getInteger("baseConfusion", 0);
		baseSleep = set.getInteger("baseSleep", 0);
		baseFire = set.getInteger("baseFire", 0);
		baseWind = set.getInteger("baseWind", 0);
		baseWater = set.getInteger("baseWater", 0);
		baseEarth = set.getInteger("baseEarth", 0);
		baseHoly = set.getInteger("baseHoly", 0);
		baseDark = set.getInteger("baseDark", 0);
		baseAggressionVuln = set.getInteger("baseAggressionVuln", 1);
		baseBleedVuln = set.getInteger("baseBleedVuln", 1);
		basePoisonVuln = set.getInteger("basePoisonVuln", 1);
		baseStunVuln = set.getInteger("baseStunVuln", 1);
		baseRootVuln = set.getInteger("baseRootVuln", 1);
		baseMovementVuln = set.getInteger("baseMovementVuln", 1);
		baseConfusionVuln = set.getInteger("baseConfusionVuln", 1);
		baseSleepVuln = set.getInteger("baseSleepVuln", 1);
		baseFireVuln = set.getInteger("baseFireVuln", 1);
		baseWindVuln = set.getInteger("baseWindVuln", 1);
		baseWaterVuln = set.getInteger("baseWaterVuln", 1);
		baseEarthVuln = set.getInteger("baseEarthVuln", 1);
		baseHolyVuln = set.getInteger("baseHolyVuln", 1);
		baseDarkVuln = set.getInteger("baseDarkVuln", 1);
		baseCritVuln = set.getInteger("baseCritVuln", 1);
		baseCancelVuln = set.getInteger("baseCancelVuln", 1);

		isUndead = (set.getInteger("isUndead", 0) == 1);

		//C4 Stats
		baseMpConsumeRate = set.getInteger("baseMpConsumeRate", 0);
		baseHpConsumeRate = set.getInteger("baseHpConsumeRate", 0);

		// Geometry
		//L2EMU_EDIT Visor123
		collisionRadius = set.getDouble("collision_radius", 10);
		collisionHeight = set.getDouble("collision_height", 10);
		fCollisionRadius = set.getDouble("fcollision_radius", 10);
		fCollisionHeight = set.getDouble("fcollision_height", 10);
		//L2EMU_EDIT
	}

	/**
	 * @return the baseAggression
	 */
	public int getBaseAggression()
	{
		return baseAggression;
	}

	/**
	 * @param baseAggression the baseAggression to set
	 */
	public void setBaseAggression(int _baseAggression)
	{
		baseAggression = _baseAggression;
	}

	/**
	 * @return the baseAggressionRes
	 */
	public double getBaseAggressionVuln()
	{
		return baseAggressionVuln;
	}

	/**
	 * @param baseAggressionRes the baseAggressionRes to set
	 */
	public void setBaseAggressionRes(double _baseAggressionVuln)
	{
		baseAggressionVuln = _baseAggressionVuln;
	}

	/**
	 * @return the baseAtkRange
	 */
	public int getBaseAtkRange()
	{
		return baseAtkRange;
	}

	/**
	 * @param baseAtkRange the baseAtkRange to set
	 */
	public void setBaseAtkRange(int _baseAtkRange)
	{
		baseAtkRange = _baseAtkRange;
	}

	/**
	 * @return the baseBleed
	 */
	public int getBaseBleed()
	{
		return baseBleed;
	}

	/**
	 * @param baseBleed the baseBleed to set
	 */
	public void setBaseBleed(int _baseBleed)
	{
		baseBleed = _baseBleed;
	}

	/**
	 * @return the baseBleedRes
	 */
	public double getBaseBleedVuln()
	{
		return baseBleedVuln;
	}

	/**
	 * @param baseBleedRes the baseBleedRes to set
	 */
	public void setBaseBleedVuln(double _baseBleedVuln)
	{
		baseBleedVuln = _baseBleedVuln;
	}

	/**
	 * @return the baseBreath
	 */
	public int getBaseBreath()
	{
		return baseBreath == 0 ? 100 : baseBreath;
	}

	/**
	 * @param baseBreath the baseBreath to set
	 */
	public void setBaseBreath(int _baseBreath)
	{
		baseBreath = _baseBreath;
	}

	/**
	 * @return the baseCON
	 */
	public int getBaseCON()
	{
		return baseCON;
	}

	/**
	 * @param baseCON the baseCON to set
	 */
	public void setBaseCON(int _baseCON)
	{
		baseCON = _baseCON;
	}

	/**
	 * @return the baseConfusion
	 */
	public int getBaseConfusion()
	{
		return baseConfusion;
	}

	/**
	 * @param baseConfusion the baseConfusion to set
	 */
	public void setBaseConfusion(int _baseConfusion)
	{
		baseConfusion = _baseConfusion;
	}

	/**
	 * @return the baseConfusionRes
	 */
	public double getBaseConfusionVuln()
	{
		return baseConfusionVuln;
	}

	/**
	 * @param baseConfusionRes the baseConfusionRes to set
	 */
	public void setBaseConfusionVuln(double _baseConfusionVuln)
	{
		baseConfusionVuln = _baseConfusionVuln;
	}

	/**
	 * @return the baseCpMax
	 */
	public float getBaseCpMax()
	{
		return baseCpMax;
	}

	/**
	 * @param baseCpMax the baseCpMax to set
	 */
	public void setBaseCpMax(float _baseCpMax)
	{
		baseCpMax = _baseCpMax;
	}

	/**
	 * @return the baseCritRate
	 */
	public int getBaseCritRate()
	{
		return baseCritRate;
	}

	/**
	 * @param baseCritRate the baseCritRate to set
	 */
	public void setBaseCritRate(int _baseCritRate)
	{
		baseCritRate = _baseCritRate;
	}

	/**
	 * @return the baseMCritRate
	 */
	public int getBaseMCritRate()
	{
		return baseMCritRate;
	}

	/**
	 * @param baseMCritRate the baseMCritRate to set
	 */
	public void setBaseMCritRate(int _baseMCritRate)
	{
		baseMCritRate = _baseMCritRate;
	}

	/**
	 * @return the baseDark
	 */
	public int getBaseDark()
	{
		return baseDark;
	}

	/**
	 * @param baseDark the baseDark to set
	 */
	public void setBaseDark(int _baseDark)
	{
		baseDark = _baseDark;
	}

	/**
	 * @return the baseDarkRes
	 */
	public double getBaseDarkVuln()
	{
		return baseDarkVuln;
	}

	/**
	 * @param baseDarkRes the baseDarkRes to set
	 */
	public void setBaseDarkVuln(double _baseDarkVuln)
	{
		baseDarkVuln = _baseDarkVuln;
	}

	/**
	 * @return the baseDEX
	 */
	public int getBaseDEX()
	{
		return baseDEX;
	}

	/**
	 * @param baseDEX the baseDEX to set
	 */
	public void setBaseDEX(int _baseDEX)
	{
		baseDEX = _baseDEX;
	}

	/**
	 * @return the baseEarth
	 */
	public int getBaseEarth()
	{
		return baseEarth;
	}

	/**
	 * @param baseEarth the baseEarth to set
	 */
	public void setBaseEarth(int _baseEarth)
	{
		baseEarth = _baseEarth;
	}

	/**
	 * @return the baseEarthRes
	 */
	public double getBaseEarthVuln()
	{
		return baseEarthVuln;
	}

	/**
	 * @param baseEarthRes the baseEarthRes to set
	 */
	public void setBaseEarthVuln(double _baseEarthVuln)
	{
		baseEarthVuln = _baseEarthVuln;
	}

	/**
	 * @return the baseFire
	 */
	public int getBaseFire()
	{
		return baseFire;
	}

	/**
	 * @param baseFire the baseFire to set
	 */
	public void setBaseFire(int _baseFire)
	{
		baseFire = _baseFire;
	}

	/**
	 * @return the baseFireRes
	 */
	public double getBaseFireVuln()
	{
		return baseFireVuln;
	}

	/**
	 * @param baseFireRes the baseFireRes to set
	 */
	public void setBaseFireVuln(double _baseFireVuln)
	{
		baseFireVuln = _baseFireVuln;
	}

	/**
	 * @return the baseHoly
	 */
	public int getBaseHoly()
	{
		return baseHoly;
	}

	/**
	 * @param baseHoly the baseHoly to set
	 */
	public void setBaseHoly(int _baseHoly)
	{
		baseHoly = _baseHoly;
	}

	/**
	 * @return the baseHolyRes
	 */
	public double getBaseHolyVuln()
	{
		return baseHolyVuln;
	}

	/**
	 * @param baseHolyRes the baseHolyRes to set
	 */
	public void setBaseHolyVuln(double _baseHolyVuln)
	{
		baseHolyVuln = _baseHolyVuln;
	}

	/**
	 * @return the baseHpConsumeRate
	 */
	public int getBaseHpConsumeRate()
	{
		return baseHpConsumeRate;
	}

	/**
	 * @param baseHpConsumeRate the baseHpConsumeRate to set
	 */
	public void setBaseHpConsumeRate(int _baseHpConsumeRate)
	{
		baseHpConsumeRate = _baseHpConsumeRate;
	}

	/**
	 * @return the baseHpMax
	 */
	public float getBaseHpMax()
	{
		return baseHpMax;
	}

	/**
	 * @param baseHpMax the baseHpMax to set
	 */
	public void setBaseHpMax(float _baseHpMax)
	{
		baseHpMax = _baseHpMax;
	}

	/**
	 * @return the baseHpReg
	 */
	public float getBaseHpReg()
	{
		return baseHpReg;
	}

	/**
	 * @param baseHpReg the baseHpReg to set
	 */
	public void setBaseHpReg(float _baseHpReg)
	{
		baseHpReg = _baseHpReg;
	}

	/**
	 * @return the baseINT
	 */
	public int getBaseINT()
	{
		return baseINT;
	}

	/**
	 * @param baseINT the baseINT to set
	 */
	public void setBaseINT(int _baseINT)
	{
		baseINT = _baseINT;
	}

	/**
	 * @return the baseMAtk
	 */
	public int getBaseMAtk()
	{
		return baseMAtk;
	}

	/**
	 * @param baseMAtk the baseMAtk to set
	 */
	public void setBaseMAtk(int _baseMAtk)
	{
		baseMAtk = _baseMAtk;
	}

	/**
	 * @return the baseMAtkSpd
	 */
	public int getBaseMAtkSpd()
	{
		return baseMAtkSpd;
	}

	/**
	 * @param baseMAtkSpd the baseMAtkSpd to set
	 */
	public void setBaseMAtkSpd(int _baseMAtkSpd)
	{
		baseMAtkSpd = _baseMAtkSpd;
	}

	/**
	 * @return the baseMDef
	 */
	public int getBaseMDef()
	{
		return baseMDef;
	}

	/**
	 * @param baseMDef the baseMDef to set
	 */
	public void setBaseMDef(int _baseMDef)
	{
		baseMDef = _baseMDef;
	}

	/**
	 * @return the baseMEN
	 */
	public int getBaseMEN()
	{
		return baseMEN;
	}

	/**
	 * @param baseMEN the baseMEN to set
	 */
	public void setBaseMEN(int _baseMEN)
	{
		baseMEN = _baseMEN;
	}

	/**
	 * @return the baseMovement
	 */
	public int getBaseMovement()
	{
		return baseMovement;
	}

	/**
	 * @param baseMovement the baseMovement to set
	 */
	public void setBaseMovement(int _baseMovement)
	{
		baseMovement = _baseMovement;
	}

	/**
	 * @return the baseMovementRes
	 */
	public double getBaseMovementVuln()
	{
		return baseMovementVuln;
	}

	/**
	 * @param baseMovementRes the baseMovementRes to set
	 */
	public void setBaseMovementVuln(double _baseMovementVuln)
	{
		baseMovementVuln = _baseMovementVuln;
	}

	/**
	 * @return the baseMpConsumeRate
	 */
	public int getBaseMpConsumeRate()
	{
		return baseMpConsumeRate;
	}

	/**
	 * @param baseMpConsumeRate the baseMpConsumeRate to set
	 */
	public void setBaseMpConsumeRate(int _baseMpConsumeRate)
	{
		baseMpConsumeRate = _baseMpConsumeRate;
	}

	/**
	 * @return the baseMpMax
	 */
	public float getBaseMpMax()
	{
		return baseMpMax;
	}

	/**
	 * @param baseMpMax the baseMpMax to set
	 */
	public void setBaseMpMax(float _baseMpMax)
	{
		baseMpMax = _baseMpMax;
	}

	/**
	 * @return the baseMpReg
	 */
	public float getBaseMpReg()
	{
		return baseMpReg;
	}

	/**
	 * @param baseMpReg the baseMpReg to set
	 */
	public void setBaseMpReg(float _baseMpReg)
	{
		baseMpReg = _baseMpReg;
	}

	/**
	 * @return the baseMReuseRate
	 */
	public float getBaseMReuseRate()
	{
		return baseMReuseRate == 0.f ? 1.f : baseMReuseRate;
	}

	/**
	 * @param baseMReuseRate the baseMReuseRate to set
	 */
	public void setBaseMReuseRate(float _baseMReuseRate)
	{
		baseMReuseRate = _baseMReuseRate;
	}

	/**
	 * @return the basePAtk
	 */
	public int getBasePAtk()
	{
		return basePAtk;
	}

	/**
	 * @param basePAtk the basePAtk to set
	 */
	public void setBasePAtk(int _basePAtk)
	{
		basePAtk = _basePAtk;
	}

	/**
	 * @return the basePAtkSpd
	 */
	public int getBasePAtkSpd()
	{
		return basePAtkSpd;
	}

	/**
	 * @param basePAtkSpd the basePAtkSpd to set
	 */
	public void setBasePAtkSpd(int _basePAtkSpd)
	{
		basePAtkSpd = _basePAtkSpd;
	}

	/**
	 * @return the basePDef
	 */
	public int getBasePDef()
	{
		return basePDef;
	}

	/**
	 * @param basePDef the basePDef to set
	 */
	public void setBasePDef(int _basePDef)
	{
		basePDef = _basePDef;
	}

	/**
	 * @return the basePoison
	 */
	public int getBasePoison()
	{
		return basePoison;
	}

	/**
	 * @param basePoison the basePoison to set
	 */
	public void setBasePoison(int _basePoison)
	{
		basePoison = _basePoison;
	}

	/**
	 * @return the basePoisonRes
	 */
	public double getBasePoisonVuln()
	{
		return basePoisonVuln;
	}

	/**
	 * @param basePoisonRes the basePoisonRes to set
	 */
	public void setBasePoisonVuln(double _basePoisonVuln)
	{
		basePoisonVuln = _basePoisonVuln;
	}

	/**
	 * @return the baseRoot
	 */
	public int getBaseRoot()
	{
		return baseRoot;
	}

	/**
	 * @param baseRoot the baseRoot to set
	 */
	public void setBaseRoot(int _baseRoot)
	{
		baseRoot = _baseRoot;
	}

	/**
	 * @return the baseRootRes
	 */
	public double getBaseRootVuln()
	{
		return baseRootVuln;
	}

	/**
	 * @param baseRootRes the baseRootRes to set
	 */
	public void setBaseRootVuln(double _baseRootVuln)
	{
		baseRootVuln = _baseRootVuln;
	}

	/**
	 * @return the baseRunSpd
	 */
	public int getBaseRunSpd()
	{
		return baseRunSpd;
	}

	/**
	 * @return the baseWalkSpd
	 */
	public int getBaseWalkSpd()
	{
		return baseWalkSpd;
	}

	/**
	 * @param baseRunSpd the baseRunSpd to set
	 */
	public void setBaseRunSpd(int _baseRunSpd)
	{
		baseRunSpd = _baseRunSpd;
	}

	/**
	 * @return the baseShldDef
	 */
	public int getBaseShldDef()
	{
		return baseShldDef;
	}

	/**
	 * @param baseShldDef the baseShldDef to set
	 */
	public void setBaseShldDef(int _baseShldDef)
	{
		baseShldDef = _baseShldDef;
	}

	/**
	 * @return the baseShldRate
	 */
	public int getBaseShldRate()
	{
		return baseShldRate;
	}

	/**
	 * @param baseShldRate the baseShldRate to set
	 */
	public void setBaseShldRate(int _baseShldRate)
	{
		baseShldRate = _baseShldRate;
	}

	/**
	 * @return the baseSleep
	 */
	public int getBaseSleep()
	{
		return baseSleep;
	}

	/**
	 * @param baseSleep the baseSleep to set
	 */
	public void setBaseSleep(int _baseSleep)
	{
		baseSleep = _baseSleep;
	}

	/**
	 * @return the baseSleepRes
	 */
	public double getBaseSleepVuln()
	{
		return baseSleepVuln;
	}

	/**
	 * @param baseSleepRes the baseSleepRes to set
	 */
	public void setBaseSleepVuln(double _baseSleepVuln)
	{
		baseSleepVuln = _baseSleepVuln;
	}

	/**
	 * @return the baseSTR
	 */
	public int getBaseSTR()
	{
		return baseSTR;
	}

	/**
	 * @param baseSTR the baseSTR to set
	 */
	public void setBaseSTR(int _baseSTR)
	{
		baseSTR = _baseSTR;
	}

	/**
	 * @return the baseStun
	 */
	public int getBaseStun()
	{
		return baseStun;
	}

	/**
	 * @param baseStun the baseStun to set
	 */
	public void setBaseStun(int _baseStun)
	{
		baseStun = _baseStun;
	}

	/**
	 * @return the baseStunRes
	 */
	public double getBaseStunVuln()
	{
		return baseStunVuln;
	}

	/**
	 * @param baseStunRes the baseStunRes to set
	 */
	public void setBaseStunVuln(double _baseStunVuln)
	{
		baseStunVuln = _baseStunVuln;
	}

	/**
	 * @return the baseWater
	 */
	public int getBaseWater()
	{
		return baseWater;
	}

	/**
	 * @param baseWater the baseWater to set
	 */
	public void setBaseWater(int _baseWater)
	{
		baseWater = _baseWater;
	}

	/**
	 * @return the baseWaterRes
	 */
	public double getBaseWaterVuln()
	{
		return baseWaterVuln;
	}

	/**
	 * @param baseWaterRes the baseWaterRes to set
	 */
	public void setBaseWaterVuln(double _baseWaterVuln)
	{
		baseWaterVuln = _baseWaterVuln;
	}

	/**
	 * @return the baseWind
	 */
	public int getBaseWind()
	{
		return baseWind;
	}

	/**
	 * @param baseWind the baseWind to set
	 */
	public void setBaseWind(int _baseWind)
	{
		baseWind = _baseWind;
	}

	/**
	 * @return the baseWindRes
	 */
	public double getBaseWindVuln()
	{
		return baseWindVuln;
	}

	/**
	 * @param baseWindRes the baseWindRes to set
	 */
	public void setBaseWindVuln(double _baseWindVuln)
	{
		baseWindVuln = _baseWindVuln;
	}

	/**
	 * @return the baseWIT
	 */
	public int getBaseWIT()
	{
		return baseWIT;
	}

	/**
	 * @param baseWIT the baseWIT to set
	 */
	public void setBaseWIT(int _baseWIT)
	{
		baseWIT = _baseWIT;
	}

	/**
	 * @return the integer collisionHeight
	 */
	public int getCollisionHeight()
	{
		return (int) collisionHeight;
	}

	/**
	 * @return the collisionHeight
	 */
	public double getdCollisionHeight()
	{
		return collisionHeight;
	}

	/**
	 * @return the fCollisionHeight
	 */
	public double getFCollisionHeight()
	{
		return fCollisionHeight;
	}

	/**
	 * @param collisionHeight the collisionHeight to set
	 */
	public void setCollisionHeight(double _collisionHeight)
	{
		collisionHeight = _collisionHeight;
	}

	/**
	 * @return the integer collisionRadius
	 */
	public int getCollisionRadius()
	{
		return (int) collisionRadius;
	}

	/**
	 * @return the collisionRadius
	 */
	public double getdCollisionRadius()
	{
		return collisionRadius;
	}

	/**
	 * @return the fCollisionRadius
	 */
	public double getFCollisionRadius()
	{
		return fCollisionRadius;
	}

	/**
	 * @param collisionRadius the collisionRadius to set
	 */
	public void setCollisionRadius(double _collisionRadius)
	{
		collisionRadius = _collisionRadius;
	}

	/**
	 * @return the isUndead
	 */
	public boolean isUndead()
	{
		return isUndead;
	}

	/**
	 * @param isUndead the isUndead to set
	 */
	public void setUndead(boolean _isUndead)
	{
		isUndead = _isUndead;
	}
}