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
package ru.catssoftware.gameserver.templates.item;

import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.StatsSet;
import javolution.util.FastList;

/**
 * This class is dedicated to the management of weapons.
 *
 * @version $Revision: 1.4.2.3.2.5 $ $Date: 2005/04/02 15:57:51 $
 */
public final class L2Weapon extends L2Equip
{
	private final int		_soulShotCount;
	private final int		_spiritShotCount;
	private final int		_pDam;
	private final int		_rndDam;
	private final int		_critical;
	private final double	_hitModifier;
	private final int		_avoidModifier;
	private final int		_shieldDef;
	private final double	_shieldDefRate;
	private final int		_atkSpeed;
	private final int		_atkReuse;
	private final int		_mpConsume;
	private final int		_mDam;
	private final int		_changeWeaponId;

	// Attached skills (e.g. Special Abilities)
	private L2Skill[]		_onCastSkills;
	private int[]			_onCastChances;
	private L2Skill[]		_onCritSkills;
	private int[]			_onCritChances;
	private L2Skill[]		_enchant4Skills; // skill that activates when item is enchanted +4 (for duals)

	/**
	 * Constructor for Weapon.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_soulShotCount & _spiritShotCount</LI>
	 * <LI>_pDam & _mDam & _rndDam</LI>
	 * <LI>_critical</LI>
	 * <LI>_hitModifier</LI>
	 * <LI>_avoidModifier</LI>
	 * <LI>_shieldDes & _shieldDefRate</LI>
	 * <LI>_atkSpeed & _AtkReuse</LI>
	 * <LI>_mpConsume</LI>
	 * <LI>_races & _classes & _sex</LI>
	 * <LI>_sIds & _sLvls</LI>
	 * @param type : L2ArmorType designating the type of armor
	 * @param set : StatsSet designating the set of couples (key,value) characterizing the weapon
	 * @see L2Item constructor
	 */
	public L2Weapon(L2WeaponType type, StatsSet set)
	{
		super(type, set);
		_soulShotCount = set.getInteger("soulshots");
		_spiritShotCount = set.getInteger("spiritshots");
		_pDam = set.getInteger("p_dam");
		_rndDam = set.getInteger("rnd_dam");
		_critical = set.getInteger("critical");
		_hitModifier = set.getDouble("hit_modify");
		_avoidModifier = set.getInteger("avoid_modify");
		_shieldDef = set.getInteger("shield_def");
		_shieldDefRate = set.getDouble("shield_def_rate");
		_atkSpeed = set.getInteger("atk_speed");
		_atkReuse = set.getInteger("atk_reuse", initAtkReuse(type));
		_mpConsume = set.getInteger("mp_consume");
		_mDam = set.getInteger("m_dam");
		_changeWeaponId = set.getInteger("change_weaponId");

		String[] enchant4SkillDefs = set.getString("skills_enchant4").split(";");
		String[] onCastSkillDefs = set.getString("skills_onCast").split(";");
		String[] onCritSkillDefs = set.getString("skills_onCrit").split(";");

		FastList<L2Skill> enchant4Skills = null;
		FastList<WeaponSkill> onCastSkills = null;
		FastList<WeaponSkill> onCritSkills = null;

		// Enchant4 skills
		if (enchant4SkillDefs != null && enchant4SkillDefs.length > 0)
			enchant4Skills = parseSkills(enchant4SkillDefs, "enchant4", "weapon");

		// OnCast skills (chance)
		if (onCastSkillDefs != null && onCastSkillDefs.length > 0)
			onCastSkills = parseChanceSkills(onCastSkillDefs, "onCast", "weapon");

		// OnCrit skills (chance)
		if (onCritSkillDefs != null && onCritSkillDefs.length > 0)
			onCritSkills = parseChanceSkills(onCritSkillDefs, "onCrit", "weapon");

		if (enchant4Skills != null && !enchant4Skills.isEmpty())
			_enchant4Skills = enchant4Skills.toArray(new L2Skill[enchant4Skills.size()]);
		if (onCastSkills != null && !onCastSkills.isEmpty())
		{
			_onCastSkills = new L2Skill[onCastSkills.size()];
			_onCastChances = new int[onCastSkills.size()];
			int i = 0;
			for (WeaponSkill ws : onCastSkills)
			{
				_onCastSkills[i] = ws.skill;
				_onCastChances[i] = ws.chance;
				i++;
			}
		}
		if (onCritSkills != null && !onCritSkills.isEmpty())
		{
			_onCritSkills = new L2Skill[onCritSkills.size()];
			_onCritChances = new int[onCritSkills.size()];
			int i = 0;
			for (WeaponSkill ws : onCritSkills)
			{
				_onCritSkills[i] = ws.skill;
				_onCritChances[i] = ws.chance;
				i++;
			}
		}
	}

	private int initAtkReuse(L2WeaponType type)
	{
		// http://www.l2p.bravehost.com/endL2P/misc.html
		// Normal bows have a base Weapon Delay of 1500 - Like Draconic Bow (atkSpd == 293)
		// Yumi bows have a base Weapon Delay of 820 - Like Soul Bow (atkSpd == 227)

		if (type == L2WeaponType.BOW)
		{
			if (_atkSpeed == 293)
				return 1500;
			if (_atkSpeed == 227)
				return 820;
		}

		return 0;
	}

	/**
	 * Returns the type of Weapon
	 * @return L2WeaponType
	 */
	@Override
	public L2WeaponType getItemType()
	{
		return (L2WeaponType) super._type;
	}

	/**
	 * Returns the ID of the Etc item after applying the mask.
	 * @return int : ID of the Weapon
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	/**
	 * Returns the quantity of SoulShot used.
	 * @return int
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}

	/**
	 * Returns the quatity of SpiritShot used.
	 * @return int
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}

	/**
	 * Returns the physical damage.
	 * @return int
	 */
	public int getPDamage()
	{
		return _pDam;
	}

	/**
	 * Returns the random damage inflicted by the weapon
	 * @return int
	 */
	public int getRandomDamage()
	{
		return _rndDam;
	}

	/**
	 * Returns the attack speed of the weapon
	 * @return int
	 */
	public int getAttackSpeed()
	{
		return _atkSpeed;
	}

	/**
	 * Return the Attack Reuse Delay of the L2Weapon.<BR><BR>
	 * @return int
	 */
	public int getAttackReuseDelay()
	{
		return _atkReuse;
	}

	/**
	 * Returns the avoid modifier of the weapon
	 * @return int
	 */
	public int getAvoidModifier()
	{
		return _avoidModifier;
	}

	/**
	 * Returns the rate of critical hit
	 * @return int
	 */
	public int getCritical()
	{
		return _critical;
	}

	/**
	 * Returns the hit modifier of the weapon
	 * @return double
	 */
	public double getHitModifier()
	{
		return _hitModifier;
	}

	/**
	 * Returns the magical damage inflicted by the weapon
	 * @return int
	 */
	public int getMDamage()
	{
		return _mDam;
	}

	/**
	 * Returns the MP consumption with the weapon
	 * @return int
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}

	/**
	 * Returns the shield defense of the weapon
	 * @return int
	 */
	public int getShieldDef()
	{
		return _shieldDef;
	}

	/**
	 * Returns the rate of shield defense of the weapon
	 * @return double
	 */
	public double getShieldDefRate()
	{
		return _shieldDefRate;
	}

	/**
	* Returns skill that player get when has equiped weapon +4  or more  (for duals SA)
	* @return
	*/
	public L2Skill[] getEnchant4Skills()
	{
		return _enchant4Skills;
	}

	/**
	* Returns the Id in wich weapon this weapon can be changed
	* @return
	*/
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}

	/**
	* Returns effects of skills associated with the item to be triggered onHit.
	* @param caster : L2Character pointing out the caster
	* @param target : L2Character pointing out the target
	* @param crit : boolean tells whether the hit was critical
	* @return L2Effect[] : array of effects generated by the skill
	*/
	public void getSkillEffectsByCrit(L2Character caster, L2Character target)
	{
		if (_onCritSkills == null)
			return;

		for (int i = 0; i < _onCritSkills.length; i++)
		{
			L2Skill skill = _onCritSkills[i];

			if (!(Rnd.get(100) < _onCritChances[i]))
				continue;

			if (!Formulas.calcSkillSuccess(caster, target, skill, false, false, false))
				continue;

			L2Effect effect = target.getFirstEffect(skill.getId());
			if (effect != null)
				effect.exit();
			skill.getEffects(caster, target);
		}
	}

	/**
	* Returns effects of skills associated with the item to be triggered onCast.
	* @param caster : L2Character pointing out the caster
	* @param target : L2Character pointing out the target
	* @param trigger : L2Skill pointing out the skill triggering this action
	* @return L2Effect[] : array of effects generated by the skill
	*/
	public boolean getSkillEffectsByCast(L2Character caster, L2Character target, L2Skill trigger)
	{
		if (_onCastSkills == null)
			return false;
		
		boolean affected = false;
		for (int i = 0; i < _onCastSkills.length; i++)
		{
			L2Skill skill = _onCastSkills[i];

			if (trigger.isOffensive() != skill.isOffensive())
				continue; // Trigger only same type of skill

			if (trigger.isToggle() || trigger.isPotion())
				continue; // No buffing with toggle skills or potions

			if (trigger.getId() >= 1320 && trigger.getId() <= 1322)
				continue; // No buffing with Common and Dwarven Craft

			if (!(Rnd.get(100) < _onCastChances[i]))
				continue;

			
			if (skill.isOffensive())
			{
				if (!Formulas.calcSkillSuccess(caster, target, skill, false, false, false))
					continue;
			}

			L2Character[] targets = new L2Character[] { target };

			try
			{
				// Launch the magic skill and calculate its effects
				SkillHandler.getInstance().getSkillHandler(skill.getSkillType()).useSkill(caster, skill, targets);
				
				affected = true;
				
				// notify quests of a skill use
				if (caster instanceof L2PcInstance)
				{
					// Mobs in range 1000 see spell
					for (L2Object spMob : caster.getKnownList().getKnownObjects().values())
					{
						if (spMob instanceof L2NpcInstance)
						{
							L2NpcInstance npcMob = (L2NpcInstance) spMob;

							if (npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE) != null)
								for (Quest quest : npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE))
									quest.notifySkillSee(npcMob, (L2PcInstance) caster, skill, targets, false);
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.error(e.getMessage(), e);
			}
		}
		
		return affected;
	}

	public boolean isMagic()
	{
		float mDam = getMDamage();
		float pDam = getPDamage();
		return mDam / pDam > 0.6;
	}
}
