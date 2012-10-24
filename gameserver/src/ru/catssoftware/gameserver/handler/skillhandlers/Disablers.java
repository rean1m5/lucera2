package ru.catssoftware.gameserver.handler.skillhandlers;

import javolution.util.FastList;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2AttackableAI;
import ru.catssoftware.gameserver.datatables.HeroSkillTable;
import ru.catssoftware.gameserver.datatables.NobleSkillTable;
import ru.catssoftware.gameserver.handler.ICubicSkillHandler;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.base.Experience;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.effects.EffectBuff;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.tools.random.Rnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Disablers implements ICubicSkillHandler
{
	private static final L2SkillType[]	SKILL_IDS		=
	{
			L2SkillType.STUN,
			L2SkillType.ROOT,
			L2SkillType.SLEEP,
			L2SkillType.CONFUSION,
			L2SkillType.AGGDAMAGE,
			L2SkillType.AGGREDUCE,
			L2SkillType.AGGREDUCE_CHAR,
			L2SkillType.AGGREMOVE,
			L2SkillType.MUTE,
			L2SkillType.FAKE_DEATH,
			L2SkillType.CONFUSE_MOB_ONLY,
			L2SkillType.NEGATE,
			L2SkillType.CANCEL,
			L2SkillType.CANCEL_DEBUFF,
			L2SkillType.PARALYZE,
			L2SkillType.UNSUMMON_ENEMY_PET,
			L2SkillType.BETRAY,
			L2SkillType.CANCEL_TARGET,
			L2SkillType.ERASE,
			L2SkillType.MAGE_BANE,
			L2SkillType.WARRIOR_BANE,
			L2SkillType.DISARM,
			L2SkillType.STEAL_BUFF
	};


	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		L2SkillType type = skill.getSkillType();

		boolean ss = false;
		boolean sps = false;
		boolean bss = false;

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if (activeChar instanceof L2PcInstance)
		{
			if (weaponInst == null && skill.isOffensive())
			{
				activeChar.sendMessage(Message.getMessage((L2PcInstance) activeChar, Message.MessageId.MSG_CANNOT_REMOVE_WEAPON));
				return;
			}
		}

		if (weaponInst != null)
		{
			if (skill.isMagic())
			{
				if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
				else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					sps = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
			{
				ss = true;
				if (skill.getId() != 1020) // vitalize
					weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,true);
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (activeChar instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) activeChar;

			if (skill.isMagic())
			{
				if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
				else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					sps = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
			{
				ss = true;
				activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
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
			

			if (target.isDead() || target.isInvul() || target.isPetrified()) //bypass if target is null, invul or dead
				continue;

			if((target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance) && skill.getSkillType()!=L2SkillType.AGGDAMAGE) {
				continue;
			}
			if(activeChar.getActingPlayer()!=null)
				if(target == activeChar.getActingPlayer().getPet() && type!=L2SkillType.NEGATE)
					continue;
					
			// With Mystic Immunity you can't be buffed/debuffed
			if (target.isPreventedFromReceivingBuffs())
				continue;

			switch (type)
			{
			case CANCEL_TARGET:
			{
				if (target instanceof L2NpcInstance)
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, 50);

				target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				target.breakAttack();
				target.breakCast();
				target.abortAttack();
				target.abortCast();
				target.setTarget(null);
				if (activeChar instanceof L2PcInstance && Rnd.get(100) < skill.getLandingPercent())
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
				break;
			}
			case BETRAY:
			{
				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					skill.getEffects(activeChar, target);
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
				break;
			}
			case UNSUMMON_ENEMY_PET:
			{
				if (target instanceof L2Summon && Rnd.get(100) < skill.getLandingPercent())
				{
					L2PcInstance targetOwner;
					targetOwner = ((L2Summon) target).getOwner();
					L2Summon Pet;
					Pet = targetOwner.getPet();
					Pet.unSummon(targetOwner);
				}
				break;
			}
			case FAKE_DEATH:
			{
				// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
				skill.getEffects(activeChar, target);
				break;
			}
			case ROOT:
			case DISARM:
			case STUN:
			{
				if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					target = activeChar;

				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					skill.getEffects(activeChar, target);
				else
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}
				break;
			}
			case SLEEP:
			case PARALYZE: //use same as root for now
			{
				if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					target = activeChar;

				if (target instanceof L2NpcInstance)
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, 50);
				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					skill.getEffects(activeChar, target);
				else
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}
				break;
			}
			case CONFUSION:
			case MUTE:
			{
				if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					target = activeChar;

				if (target instanceof L2NpcInstance)
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, 50);
				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				{
					// stop same type effect if available
					L2Effect[] effects = target.getAllEffects();
					for (L2Effect e : effects)
					{
						if (e.getSkill().getSkillType() == type)
							e.exit();
					}
					skill.getEffects(activeChar, target);
				}
				else
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}
				break;
			}
			case CONFUSE_MOB_ONLY:
			{
				// do nothing if not on mob
				if (target instanceof L2Attackable)
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getSkill().getSkillType() == type)
								e.exit();
						}
						skill.getEffects(activeChar, target);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
				}
				else
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				break;
			}
			case AGGDAMAGE:
			{
				if (skill.getId() == 51 && target instanceof L2PcInstance)
				{
					target.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					break;
				}
				if (target instanceof L2GrandBossInstance)
				{
						double power = (skill.getPower()/100);
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) power);
						break;
				}
				if (target instanceof L2PcInstance && Rnd.get(100) < 75)
				{
					L2PcInstance pc = ((L2PcInstance) target);
					if ((pc.getPvpFlag() != 0 || pc.isInOlympiadMode() || pc.isInCombat() || pc.isInsideZone(L2Zone.FLAG_PVP)))
					{
						pc.setTarget(activeChar);
						pc.abortAttack();
						pc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
					}
				}
				if (target instanceof L2Attackable && skill.getId() != 368)
				{
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) skill.getPower());
					break;
				}
				if (target instanceof L2Attackable)
				{
					if (skill.getId() == 368)
					{
						if (target instanceof L2PcInstance)
						{
							L2PcInstance pc = ((L2PcInstance) target);
							if (pc.getPvpFlag() != 0 || pc.isInOlympiadMode() || pc.isInCombat() || pc.isInsideZone(L2Zone.FLAG_PVP))
							{
								target.setTarget(activeChar);
								target.getAI().setAutoAttacking(true);
								if (target instanceof L2PcInstance)
									target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
							}
						}
						target.setTarget(activeChar);
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(activeChar, activeChar);
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					}
				}
				break;
			}
			case AGGREDUCE:
			{
				//these skills needs to be rechecked
				if (target instanceof L2Attackable)
				{
					skill.getEffects(activeChar, target);

					double aggdiff = ((L2Attackable) target).getHating(activeChar)
							- target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);

					if (skill.getPower() > 0)
						((L2Attackable) target).reduceHate(null, (int) skill.getPower());
					else if (aggdiff > 0)
						((L2Attackable) target).reduceHate(null, (int) aggdiff);
				}
				break;
			}
			case AGGREDUCE_CHAR:
			{
				//these skills needs to be rechecked
				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				{
					if (target instanceof L2Attackable)
					{
						L2Attackable targ = (L2Attackable) target;
						targ.stopHating(activeChar);
						if (targ.getMostHated() == null)
						{
							if (targ.getAI() instanceof L2AttackableAI)
								((L2AttackableAI) targ.getAI()).setGlobalAggro(-25);
							targ.clearAggroList();
							targ.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							targ.setWalking();
						}
					}
					skill.getEffects(activeChar, target);
				}
				else
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addString(target.getName());
						sm.addSkillName(skill.getId());
						activeChar.sendPacket(sm);
					}
					target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
				}
				break;
			}
			case AGGREMOVE:
			{
				// 1034 = repose, 1049 = requiem
				//these skills needs to be rechecked
				if (target instanceof L2Attackable && !target.isRaid())
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					}
				}
				else
					target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
				break;
			}
			case ERASE: // Doesn't affect siege golem, wild hog cannon or swoop cannon
			{
				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss) && !(target instanceof L2SiegeSummonInstance))
				{
					L2PcInstance summonOwner;
					L2Summon summonPet;
					summonOwner = ((L2Summon) target).getOwner();
					summonPet = summonOwner.getPet();
					if (summonPet != null)
					{
						ThreadPoolManager.getInstance().schedule(new UnSummon(summonPet,summonOwner), 1);
						summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
					}
				}
				else
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}

				break;
			}
			case MAGE_BANE:
			{
				if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					target = activeChar;

				if (!Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					continue;
				}

				L2Effect[] effects = target.getAllEffects();
				for (L2Effect e : effects)
				{
					if ((e.getStackType().equals("casting_time_down")) || (e.getStackType().equals("ma_up")))
						e.exit();
				}
				break;
			}

			case WARRIOR_BANE:
			{
				if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					target = activeChar;

				if (!Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					continue;
				}

				L2Effect[] effects = target.getAllEffects();
				for (L2Effect e : effects)
				{
					if ((e.getStackType().equals("speed_up")) || (e.getStackType().equals("attack_time_down")))
						e.exit();
				}
				break;
			}
			case CANCEL_DEBUFF:
			{
				L2Effect[] effects = target.getAllEffects();

				if (effects.length == 0)
					break;

				int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
				for (L2Effect e : effects)
				{
					if (e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
					{
						//Do not remove raid curse skills
						if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082 && e.getSkill().getId() != 5660)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
				}
				break;
			}
			case STEAL_BUFF:
			{
				if (!(target instanceof L2PlayableInstance))
					return;

				L2Effect[] effects = target.getAllEffects();

				if (effects == null || effects.length < 1)
					return;

				List<L2Effect> list = Arrays.asList(effects);
				Collections.reverse(list);
				list.toArray(effects);

				FastList<L2Effect> toSteal = new FastList<L2Effect>();
				int count = 0;
				int lastSkill = 0;

				for (L2Effect e : effects)
				{
					// None buffs
					if (e == null)
						continue;
					// Skip not buffs
					if (!(e instanceof EffectBuff))
						continue;
					// Skip debuff || race skills
					if (e.getSkill().getSkillType() == L2SkillType.HEAL || e.getSkill().isToggle() || e.getSkill().isDebuff() || e.getSkill().isPotion() || e.isHerbEffect())
						continue;
					// Skip hero || noble skills
					if (HeroSkillTable.isHeroSkill(e.getSkill().getId()) || NobleSkillTable.isNobleSkill(e.getSkill().getId()))
						continue;
					// Add effect to list
					if (e.getSkill().getId() == lastSkill)
					{
						if (count == 0)
							count = 1;
						toSteal.add(e);
					}
					else if (count < skill.getPower())
					{
						toSteal.add(e);
						count++;
					}
					else
						break;
				}
				if (!toSteal.isEmpty())
					stealEffects(activeChar, target, toSteal);
				break;
			}
			case CANCEL:
			{
				boolean reflect = false;
				if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED) {
					target = activeChar;
					reflect = true;
				}

				if(target.getActingPlayer() == activeChar && !reflect)
					continue;
				/*if(Config.OLD_CANCEL_MODE)
				{*/
				if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
				{
					L2Effect[] effects = target.getAllEffects();

					List<L2Effect> remove = new ArrayList<L2Effect>();

					for (L2Effect e : effects)
					{
						switch (e.getSkill().getId())
						{
							case 110:
							case 111:
							case 1323:
							case 1325:
							case 1418:
							case 4082:
							case 4215:
							case 4515:
							case 5182:
								continue;
						}

						switch (e.getSkill().getSkillType())
						{
							case BUFF:
							case HEAL_PERCENT:
							case REFLECT:
							case COMBATPOINTHEAL:
								remove.add(e);
								break;
						}
					}

					for (int i = 0; i < Rnd.get(1, skill.getMaxNegatedEffects()) && !remove.isEmpty(); i++)
						remove.remove(Rnd.get(remove.size())).exit();
					remove.clear();
				}
				else
				{
					if (activeChar.isPlayer())
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}

				/*} else {
					L2Effect[] effects = target.getAllEffects();
					int max = skill.getMaxNegatedEffects();
					if (max == 0)
						max = 24; //this is for RBcancells and stuff...
					else {
						max  = Rnd.get(skill.getMaxNegatedEffects());
						if(max==0) max = 1;
					}
					if (effects.length < max)
							max = effects.length;
					for(int i=effects.length-1;i>=0;i--) {
						L2Effect e = effects[i];
						if(!e.isBuff())
							continue;
						switch (e.getSkill().getId())
						{
							case 110:
							case 111:
							case 1323:
							case 1325:
							case 4082:
							case 4215:
							case 4515:
							case 5182:
								continue;
						}
						if(Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
							e.exit();
							max--;
						}
						if(max==0)
							break;

					}
				}*/
				break;

			}
			case NEGATE:
			{
/*				if (Form ulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
					target = activeChar;
*/
				if (skill.getNegateId() > 0)
				{
					L2Effect[] effects = target.getAllEffects();
					for (L2Effect e : effects)
					{
						if (e.getSkill().getId() == skill.getNegateId())
							e.exit();
					}
				}
				// fishing potion
				else if (skill.getId() == 2275)
				{
					negateEffect(target, L2SkillType.BUFF, skill.getNegateLvl(), skill.getNegateId(), -1);
					break;
				}
				// purify
				else
				{
					int removedBuffs = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;

					for (String stat : skill.getNegateStats())
					{
						if (removedBuffs > skill.getMaxNegatedEffects())
							break;

						if (stat.equals("buff") || stat.equals("heal_percent"))
						{
							int lvlmodifier = 52 + skill.getMagicLevel() * 2;
							if (skill.getMagicLevel() == 12)
								lvlmodifier = (Experience.MAX_LEVEL - 1);
							int landrate = 90;
							if ((target.getLevel() - lvlmodifier) > 0)
								landrate = 90 - 4 * (target.getLevel() - lvlmodifier);

							landrate = (int) activeChar.calcStat(Stats.CANCEL_VULN, landrate, target, null);

							if (Rnd.get(100) < landrate)
								removedBuffs += negateEffect(target, L2SkillType.BUFF, -1, skill.getMaxNegatedEffects());
						}
						else if (removedBuffs < skill.getMaxNegatedEffects())
						{
							L2SkillType negativeType = L2SkillType.valueOf(stat.toUpperCase());
							switch (negativeType)
							{
								case DEBUFF:
								case WEAKNESS:
								case STUN:
								case SLEEP:
								case CONFUSION:
								case MUTE:
								case FEAR:
								case PARALYZE:
								case ROOT:
									removedBuffs += negateEffect(target, negativeType, -1, skill.getMaxNegatedEffects());
									break;
								case POISON:
								case BLEED:
								case DEATH_MARK:
								case MDAM:
									removedBuffs += negateEffect(target, negativeType, skill.getNegateLvl(), skill.getMaxNegatedEffects());
									break;
								case HEAL:
									SkillHandler.getInstance().getSkillHandler(negativeType).useSkill(activeChar, skill, target);
									break;

							}
						}
					}
				}
			}
			}

			//Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);

		}
		// self Effect :]
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
			//Replace old effect with new one.
			effect.exit();
		skill.getEffectsSelf(activeChar);
	}

	public void useCubicSkill(L2CubicInstance activeCubic, L2Skill skill, L2Character... targets)
	{

		L2SkillType type = skill.getSkillType();

		for (L2Character target : targets)
		{
			if (target == null)
				continue;

			if (target.isDead()) //bypass if target is null or dead
				continue;

			switch (type)
			{
			case STUN:
			case PARALYZE:
			case ROOT:
				if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
					skill.getEffects(activeCubic, target);
				break;
			case CANCEL_DEBUFF:
				L2Effect[] effects = target.getAllEffects();

				if (effects.length == 0)
					break;

				int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
				for (L2Effect e : effects)
				{
					if (e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
					{
						//Do not remove raid curse skills
						if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082 && e.getSkill().getId() != 5660)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
				}
				break;
			case AGGDAMAGE:
				if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
				{
					if (target instanceof L2Attackable)
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeCubic.getOwner(), (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					skill.getEffects(activeCubic, target);
				}
				else{}
				break;
			}
		}
	}

	private int negateEffect(L2Character target, L2SkillType type, double negateLvl, int maxRemoved)
	{
		return negateEffect(target, type, negateLvl, 0, maxRemoved);
	}

	private int negateEffect(L2Character target, L2SkillType type, double negateLvl, int skillId, int maxRemoved)
	{
		L2Effect[] effects = target.getAllEffects();
		int count = (maxRemoved <= 0) ? -2 : 0;
		for (L2Effect e : effects)
		{
			if (negateLvl == -1) // if power is -1 the effect is always removed without power/lvl check ^^
			{
				if (e.getSkill().getSkillType() == type || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type))
				{
					if (skillId != 0)
					{
						if (skillId == e.getSkill().getId() && count < maxRemoved)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					else if (count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
			}
			else
			{
				boolean cancel = false;
				if (e.getSkill().getEffectType() != null && e.getSkill().getEffectAbnormalLvl() >= 0)
				{
					if (e.getSkill().getEffectType() == type && e.getSkill().getEffectAbnormalLvl() <= negateLvl)
						cancel = true;
				}
				else if (e.getSkill().getSkillType() == type && e.getSkill().getAbnormalLvl() <= negateLvl)
					cancel = true;
				if (cancel)
				{
					if (skillId != 0)
					{
						if (skillId == e.getSkill().getId() && count < maxRemoved)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					else if (count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
			}
		}

		return (maxRemoved <= 0) ? count + 2 : count;
	}

	private void stealEffects(L2Character stealer, L2Character stolen, FastList<L2Effect> stolenEffects)
	{
		for (L2Effect eff : stolenEffects)
		{
			// if eff time is smaller than 1 sec, will not be stolen, just to save CPU,
			// avoid synchronization(?) problems and NPEs
			if (eff.getPeriod() - eff.getTime() < 1)
				 continue;

			Env env = new Env();
			env.player = stolen;
			env.target = stealer;
			env.skill = eff.getSkill();
			L2Effect e = eff.getEffectTemplate().getStolenEffect(env, eff);

			// Since there is a previous check that limits allowed effects to those which come from SkillType.BUFF,
			// it is not needed another check for SkillType
			if (stealer instanceof L2PcInstance && e != null)
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				smsg.addSkillName(eff);
				stealer.sendPacket(smsg);
			}
			// Finishing stolen effect
			eff.exit();
		}
	}

	private L2Effect[] sortEffects(L2Effect[] initial)
	{
		//this is just classic insert sort
		//If u can find better sort for max 20-30 units, rewrite this... :)
		int min, index = 0;
		L2Effect pom;
		for (int i = 0; i < initial.length; i++)
		{
			min = initial[i].getSkill().getMagicLevel();
			for (int j = i; j < initial.length; j++)
			{
				if (initial[j].getSkill().getMagicLevel() <= min)
				{
					min = initial[j].getSkill().getMagicLevel();
					index = j;
				}
			}
			pom = initial[i];
			initial[i] = initial[index];
			initial[index] = pom;
		}
		return initial;
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	static class UnSummon implements Runnable
	{
		L2Summon summonPet;
		L2PcInstance summonOwner;
		public UnSummon(L2Summon sum,L2PcInstance pc)
		{
			summonPet = sum;
			summonOwner = pc;
		}
		public void run()
		{
			summonPet.unSummon(summonOwner);
		}
	}
	
}
