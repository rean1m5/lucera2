package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2RaidBossInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class Pdam implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.PDAM, L2SkillType.FATALCOUNTER };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;

		L2ItemInstance weapon	= null;
		boolean soul			= false;
		int damage				= 0;
		
		weapon = activeChar.getActiveWeaponInstance();
		if (weapon != null)
		{
			if (weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER)
			{
				soul = true;
				weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,true);
			}
		}

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isFakeDeath())
				target.stopFakeDeath(null);

			else if (target.isDead())
				continue;

			boolean dual = activeChar.isUsingDualWeapon();
			byte shld;
			// PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
			boolean crit = false;
			if (skill.getBaseCritRate() > 0)
				crit = Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar));

			if (skill.ignoreShld())
				shld = 0;
			else
				shld = Formulas.calcShldUse(activeChar, target);

			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				damage = 0;
			else
				damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, dual, soul);

			if (crit)
				damage *= 2; // PDAM Critical damage always 2x and not affected by buffs

			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, skill);
			final byte reflect = Formulas.calcSkillReflect(target, skill);

			if (!skillIsEvaded)
			{
				if (damage > 0)
				{
					activeChar.sendDamageMessage(target, damage, false, crit, false);

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
							if (Formulas.calcSkillSuccess(activeChar, target, skill, false, false, false))
							{
								skill.getEffects(activeChar, target);

								SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
								sm.addSkillName(skill);
								target.sendPacket(sm);
							}
							else
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
						}

						if (damage > 5000 && activeChar instanceof L2PcInstance)
						{
							String name = "";
							if (target instanceof L2RaidBossInstance)
								name = "RaidBoss ";
							if (target instanceof L2NpcInstance)
								name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().getNpcId() + ")";
							if (target instanceof L2PcInstance)
								name = target.getName() + "(" + target.getObjectId() + ") ";
							name += target.getLevel() + " lvl";
							if (_log.isDebugEnabled())
								_log.info(activeChar.getName() + "(" + activeChar.getObjectId() + ") " + activeChar.getLevel() + " lvl did damage " + damage + " with skill " + skill.getName() + "(" + skill.getId() + ") to " + name);
						}
					}

					// Possibility of a lethal strike
					boolean lethal = Formulas.calcLethalHit(activeChar, target, skill);

					// Make damage directly to HP
					if (!lethal && skill.getDmgDirectlyToHP())
					{
						final L2Character[] ts = {target, activeChar};

						for (L2Character targ : ts)
						{						
							if (target instanceof L2PcInstance)
							{
								L2PcInstance player = (L2PcInstance) targ;
								if (!player.isInvul())
								{
									if (damage >= player.getStatus().getCurrentHp())
									{
										if (player.isInDuel())
											player.getStatus().setCurrentHp(1);
										else
										{
											player.getStatus().setCurrentHp(0);
											if (player.isInOlympiadMode())
											{
												player.abortAttack();
												player.abortCast();
												player.getStatus().stopHpMpRegeneration();
												player.setIsDead(true);
												player.setIsPendingRevive(true);
												if (player.getPet() != null)
													player.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
											}
											else
												player.doDie(activeChar);
										}
									}
									else
										player.getStatus().setCurrentHp(player.getStatus().getCurrentHp() - damage);
								}

/*								SystemMessage smsg = new SystemMessage(SystemMessageId.S1_RECEIVED_DAMAGE_OF_S3_FROM_S2);
								smsg.addPcName(player);
								smsg.addCharName(activeChar);
								smsg.addNumber(damage);
								player.sendPacket(smsg); */
							}
							else
								target.reduceCurrentHp(damage, activeChar, skill);
							if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) == 0) // stop if no vengeance, so only target will be effected
								break;
						}						
					}
					else
					{
						target.reduceCurrentHp(damage, activeChar, skill);
						if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0) {
							int relectedDamage = (int)target.calcStat(Stats.VENGEANCE_SKILL_VALUE, damage, activeChar, skill);
							if(relectedDamage>0) 
								activeChar.reduceCurrentHp(relectedDamage, target, skill);
						}
					}
				}
				else
					activeChar.sendPacket(SystemMessageId.ATTACK_FAILED);
			}
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_DODGES_ATTACK);
					sm.addCharName(target);
					activeChar.sendPacket(sm);
				}
				if (target instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_S1_ATTACK);
					sm.addCharName(activeChar);
					target.sendPacket(sm);
				}

				// Possibility of a lethal strike despite skill is evaded
				Formulas.calcLethalHit(activeChar, target, skill);
			}

			// Increase Charges
			if (activeChar instanceof L2PcInstance && skill.getGiveCharges() > 0)
				((L2PcInstance) activeChar).increaseCharges(skill.getGiveCharges(), skill.getMaxCharges());

			//self Effect :]
			L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				//Replace old effect with new one.
				effect.exit();

			skill.getEffectsSelf(activeChar);
		}

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

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
