package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.handler.ICubicSkillHandler;
import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2CubicInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Mdam implements ICubicSkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.MDAM, L2SkillType.DEATHLINK };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;

		boolean ss = false;
		boolean bss = false;
		
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
			else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
		}

		// If there is no weapon equipped, check for an active summon.
		else if (activeChar instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) activeChar;

			if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
			else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
		}
		else if (activeChar instanceof L2NpcInstance)
		{
			bss = ((L2NpcInstance) activeChar).isUsingShot(false);
			ss = ((L2NpcInstance) activeChar).isUsingShot(true);
		}

		for (L2Character target : targets)
		{
			if (target == null)
				continue;
			if(target instanceof L2Boss)
				if(Math.abs(target.getZ()-activeChar.getZ())>50)
					continue;
			if (activeChar.isPlayer() && target.isPlayer() && target.isFakeDeath())
				target.stopFakeDeath(null);

			else if (target.isDead())
				continue;

			boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill));
			int damage = (int) Formulas.calcMagicDam(activeChar, target, skill, ss, bss, mcrit);
			final byte reflect = Formulas.calcSkillReflect(target, skill);
			
			/**
			 * Отмена предэфектов.
			 * Проверка нужна для отмены предэфектов, таких как стихиные Вортексы
			 **/
			if (skill.getCancelId() > 0)
			{
				L2Effect[] effects = target.getAllEffects();
				for (L2Effect e : effects)
				{
					if (e.getSkill().getId() == skill.getCancelId())
						e.exit();
				}
			}

			if (mcrit)
				activeChar.sendPacket(SystemMessageId.CRITICAL_HIT);

			damage += calcAdditionalDamage(damage,activeChar,skill,targets);

			if (damage < 1)
				damage = 1;

			if (damage > 0)
			{
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				activeChar.sendDamageMessage(target, damage, mcrit, false, false);

				if (activeChar instanceof L2SummonInstance)
					((L2SummonInstance) activeChar).getOwner().sendPacket(new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1).addNumber(damage));

				// activate attacked effects, if any
				if (skill.getId() == 4139 && activeChar instanceof L2Summon) //big boom unsummon-destroy
				{
					L2PcInstance Owner = null;
					Owner = ((L2Summon) activeChar).getOwner();
					L2Summon Pet = null;
					Pet = Owner.getPet();
					if (Pet != null)
						Pet.unSummon(Owner);
				}
				if (skill.hasEffects())
				{
					if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(target, activeChar);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					else
					{
						// activate attacked effects, if any
						target.stopSkillEffects(skill.getId());
						if (Formulas.calcSkillSuccess(activeChar, target, skill, false, ss, bss))
							skill.getEffects(activeChar, target);
						else
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
				}
				target.reduceCurrentHp(damage, activeChar, skill);
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
					activeChar.reduceCurrentHp(damage, target, skill);
			}
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);
		}
		// self Effect :]
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
		{
			//Replace old effect with new one.
			effect.exit();
		}
		skill.getEffectsSelf(activeChar);

		if (skill.isSuicideAttack())
		{
			L2Character target = null;
			for (L2Character tmp : targets)
			{
				if (tmp != null && !(tmp instanceof L2PlayableInstance))
				{
					target = tmp;
					break;
				}
			}
			activeChar.doDie(target);
		}
	}

	public void useCubicSkill(L2CubicInstance activeCubic, L2Skill skill, L2Character... targets)
	{
		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (target.isPlayer() && target.isAlikeDead() && target.isFakeDeath())
				target.stopFakeDeath(null);

			else if (target.isAlikeDead())
				continue;

			boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, skill));
			int damage = (int) Formulas.calcMagicDam(activeCubic, target, skill, mcrit);

			// if target is reflecting the skill then no damage is done
			if ((Formulas.calcSkillReflect(target, skill) & Formulas.SKILL_REFLECT_SUCCEED) > 0)
				damage = 0;

			if (damage > 0)
			{
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				activeCubic.getOwner().sendDamageMessage(target, damage, mcrit, false, false);

				if (skill.hasEffects())
				{
					// activate attacked effects, if any
					target.stopSkillEffects(skill.getId());
					if (target.getFirstEffect(skill) != null)
						target.removeEffect(target.getFirstEffect(skill));
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
						skill.getEffects(activeCubic, target);
				}

				target.reduceCurrentHp(damage, activeCubic.getOwner(), skill);
			}
		}
	}

	protected int calcAdditionalDamage(int baseDamage, L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		int addDamage = 0;
		if(skill.getId() == 1439)
			addDamage += (baseDamage*0.1 * targets[0].getBuffCount());
		return addDamage;
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
