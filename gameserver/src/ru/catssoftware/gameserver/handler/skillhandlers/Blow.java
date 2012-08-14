package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;

public class Blow implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	=
	{
		L2SkillType.BLOW
	};

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		boolean soul = false;
		L2ItemInstance weapon = null;

		weapon = activeChar.getActiveWeaponInstance();
		if (weapon != null)
		{
			soul = (weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && (weapon.getItemType() == L2WeaponType.DAGGER));
			if (soul)
				weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,true);
		}

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (target.isAlikeDead())
				continue;

			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, skill);
			byte _successChance = Config.BLOW_SIDE;

			if (activeChar.isBehindTarget())
				_successChance = Config.BLOW_BEHIND;
			else if (activeChar.isInFrontOfTarget())
				_successChance = Config.BLOW_FRONT;

			// If skill requires Crit or skill requires behind,
			// Calculate chance based on DEX, Position and on self BUFF
			boolean success = true;
			if ((skill.getCondition() & L2Skill.COND_BEHIND) != 0)
				success = (_successChance == Config.BLOW_BEHIND);
			if ((skill.getCondition() & L2Skill.COND_CRIT) != 0)
				success = (success && Formulas.calcBlow(activeChar, target, _successChance));
                        
			if (skill.getId() == 30){
				if (target.getFirstEffect(358)!=null && activeChar.isBehindTarget())
						success = true;
			}
                        
			if (!skillIsEvaded && success)
			{
				final byte reflect = Formulas.calcSkillReflect(target, skill);

				if (skill.hasEffects())
				{
					if (reflect == Formulas.SKILL_REFLECT_SUCCEED)
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(target, activeChar);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}
				byte shld = Formulas.calcShldUse(activeChar, target);
				boolean crit = false;
				if (Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar)))
					crit = true;

				double damage = (int) Formulas.calcBlowDamage(activeChar, target, skill, shld, soul);
				
				
				if (crit)
				{
					damage *= 2;
					// Vicious Stance is special after C5, and only for BLOW skills
					// Adds directly to damage
					L2Effect vicious = activeChar.getFirstEffect(312);
					if (vicious != null && damage > 1)
					{
						for (Func func : vicious.getStatFuncs())
						{
							Env env = new Env();
							env.player = activeChar;
							env.target = target;
							env.skill = skill;
							env.value = damage;
							func.calcIfAllowed(env);
							damage = (int) env.value;
						}
					}
				}

				if (skill.getDmgDirectlyToHP() && target instanceof L2PcInstance)
				{
					final L2Character[] ts = {target, activeChar};
					for (L2Character targ : ts)
					{

						L2PcInstance player = (L2PcInstance) targ;
						if (!player.isInvul() && !player.isPetrified())
						{
							// Check and calculate transfered damage
							L2Summon summon = player.getPet();
							if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, player, summon, true))
							{
								int tDmg = (int) damage * (int) player.getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;

								// Only transfer dmg up to current HP, it should not be killed
								if (summon.getStatus().getCurrentHp() < tDmg)
									tDmg = (int) summon.getStatus().getCurrentHp() - 1;
								if (tDmg > 0)
								{
									summon.reduceCurrentHp(tDmg, activeChar, skill);
									damage -= tDmg;
								}
							}

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
							{
								player.getStatus().setCurrentHp(player.getStatus().getCurrentHp() - damage);
								if (player.isSleeping())
									player.stopSleeping(null);
								// Add Olympiad damage
								if (activeChar instanceof L2PcInstance && ((L2PcInstance) activeChar).isInOlympiadMode())
									((L2PcInstance) activeChar).addOlyDamage((int) damage);
							}
						}
						SystemMessage smsg = new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG);
						smsg.addPcName((L2PcInstance)activeChar);
						smsg.addNumber((int) damage);
						player.sendPacket(smsg); 
						if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) == 0)
							break;
					}
				}
				else
				{
					target.reduceCurrentHp(damage, activeChar, true, skill);
					if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0) {
						int relectedDamage = (int)target.calcStat(Stats.VENGEANCE_SKILL_VALUE, damage, activeChar, skill);
						if(relectedDamage>0) 
							activeChar.reduceCurrentHp(relectedDamage, target, skill);
					}
				}

				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				if (activeChar instanceof L2PcInstance)
				{
					L2PcInstance activePlayer = (L2PcInstance) activeChar;
					activePlayer.sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG);
					sm.addNumber((int) damage);
					activePlayer.sendPacket(sm);

					if (activePlayer.isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == activePlayer.getOlympiadGameId())
						Olympiad.getInstance().notifyCompetitorDamage(activePlayer, (int) damage, activePlayer.getOlympiadGameId());
				}
			}

			// Sending system messages
			if (skillIsEvaded)
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
			}

			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);

			L2Effect effect = activeChar.getFirstEffect(skill.getId());
			// Self Effect
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			skill.getEffectsSelf(activeChar);
			skill.getEffects(activeChar, target);
		}
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}