package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.ICubicSkillHandler;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2CubicInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2RaidBossInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Continuous implements ICubicSkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	=
	{
		L2SkillType.BUFF,
		L2SkillType.DEBUFF,
		L2SkillType.DOT,
		L2SkillType.MDOT,
		L2SkillType.POISON,
		L2SkillType.BLEED,
		L2SkillType.HOT,
		L2SkillType.CPHOT,
		L2SkillType.MPHOT,
		L2SkillType.FEAR,
		L2SkillType.CONT,
		L2SkillType.WEAKNESS,
		L2SkillType.REFLECT,
		L2SkillType.UNDEAD_DEFENSE,
		L2SkillType.AGGDEBUFF,
		L2SkillType.FUSION,
		L2SkillType.BAD_BUFF
	};

	private L2Skill						_skill;

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		boolean acted = true;
		boolean consumeSoul = true;
		boolean ss = false;
		boolean sps = false;
		boolean bss = false;

		L2PcInstance player = null;
		if (activeChar.isPlayer())
			player = (L2PcInstance) activeChar;

		if (skill.getEffectId() != 0)
		{
			int skillLevel = (int)skill.getEffectLvl();
			int skillEffectId = skill.getEffectId();

			if (skillLevel == 0)
				_skill = SkillTable.getInstance().getInfo(skillEffectId, 1);
			else
				_skill = SkillTable.getInstance().getInfo(skillEffectId, skillLevel);

			if (_skill != null)
				skill = _skill;
		}

		for (L2Character target : targets)
		{
			if (target == null)
				continue;
			
			if((target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance) && skill.getSkillType()==L2SkillType.FEAR) {
				continue;
			}
			if(target instanceof L2Summon && target.getPlayer()==activeChar) {
				if(skill.getSkillType()==L2SkillType.FEAR)
					continue;
			}
			switch (skill.getSkillType())
			{
				case BUFF:
					if(Math.abs(target.getZ()-activeChar.getZ())>200)
						continue;
				case HOT:
				case CPHOT:
				case MPHOT:
				case AGGDEBUFF:
				case CONT:
				case UNDEAD_DEFENSE:
				case BAD_BUFF:
					break;
				default:
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;
					break;
			}

			if (target.isPreventedFromReceivingBuffs())
				continue;

			if (skill.getSkillType() == L2SkillType.BUFF && !(activeChar instanceof L2ClanHallManagerInstance))
			{
				if (target != activeChar)
				{
					if (target.isPlayer() && ((L2PcInstance) target).isCursedWeaponEquipped())
						continue;
					else if (player != null && player.isCursedWeaponEquipped())
						continue;
				}
			}
			if (skill.isOffensive() || skill.isDebuff())
			{
				if (player != null)
				{
					L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
					if (weaponInst != null && consumeSoul)
					{
						if (skill.isMagic())
						{
							if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
							{
								bss = true;
								if (skill.getId() != 1020) // vitalize
								{
									weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
									consumeSoul = false;
								}
							}
							else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
							{
								sps = true;
								if (skill.getId() != 1020) // vitalize
								{
									weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
									consumeSoul = false;
								}
							}
						}
						else if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
						{
							ss = true;
							if (skill.getId() != 1020) // vitalize
							{
								weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,true);
								consumeSoul = false;
							}
						}
					}
				}
				else if (activeChar instanceof L2Summon)
				{
					L2Summon activeSummon = (L2Summon) activeChar;
					if (activeSummon != null && consumeSoul)
					{
						if (skill.isMagic())
						{
							if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
							{
								bss = true;
								activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
								consumeSoul = false;
							}
							else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
							{
								sps = true;
								activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
								consumeSoul = false;
							}
						}
						else if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
						{
							ss = true;
							activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
							consumeSoul = false;
						}
					}
				}
				else if (activeChar instanceof L2NpcInstance)
				{
					bss = ((L2NpcInstance) activeChar).isUsingShot(false);
					ss = ((L2NpcInstance) activeChar).isUsingShot(true);
				}

				acted = Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss);
			}

			if (acted)
			{
				if (skill.isToggle())
				{
					L2Effect[] effects = target.getAllEffects();
					if (effects != null)
					{
						for (L2Effect e : effects)
						{
							if (e != null)
							{
								if (e.getSkill().getId() == skill.getId())
								{
									e.exit();
									return;
								}
							}
						}
					}
				}
				skill.getEffects(activeChar, target);

				if (skill.getSkillType() == L2SkillType.AGGDEBUFF)
				{
					if (target instanceof L2Attackable)
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) skill.getPower());
					else if (target instanceof L2PlayableInstance)
					{
						if (target.getTarget() == activeChar)
							target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
						else
							target.setTarget(activeChar);
					}
				}
				// Possibility of a lethal strike
				if (Formulas.calcLethalHit(activeChar, target, skill))
				{
					if (skill.getSkillType() == L2SkillType.FEAR && skill.getLevel() >=301 && skill.getLevel() <=330)
					{
						switch (skill.getId())
						{
							case 1400: // Turn Undead
								target.reduceCurrentHp(skill.getLevel(), activeChar, skill);
								break;
							case 450: // Banish Seraph
								target.reduceCurrentHp(skill.getLevel(), activeChar, skill);
								break;
							case 405: // Banish Undead
								target.reduceCurrentHp(skill.getLevel(), activeChar, skill);
								break;
							default:
								break;
						}
					}
				}
			}
			else if (activeChar.isPlayer())
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
				sm.addString(target.getName());
				sm.addSkillName(skill.getId());
				activeChar.sendPacket(sm);
			}
		}

		// Increase Charges
		if (activeChar.isPlayer() && skill.getGiveCharges() > 0)
			((L2PcInstance) activeChar).increaseCharges(skill.getGiveCharges(), skill.getMaxCharges());

		// self Effect :]
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
			//Replace old effect with new one.
			effect.exit();
		skill.getEffectsSelf(activeChar);
	}

	public void useCubicSkill(L2CubicInstance activeCubic, L2Skill skill, L2Character... targets)
	{
		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (skill.isOffensive())
			{
				boolean acted = Formulas.calcCubicSkillSuccess(activeCubic, target, skill);
				if (!acted)
				{
					activeCubic.getOwner().sendPacket(SystemMessageId.ATTACK_FAILED);
					continue;
				}
			}
			skill.getEffects(activeCubic, target);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
