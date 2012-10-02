package ru.catssoftware.gameserver.model;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.effects.EffectCharmOfCourage;
import ru.catssoftware.gameserver.skills.effects.EffectTemplate;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.util.LinkedBunch;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class CharEffectList
{
	private static Logger _log = Logger.getLogger(CharEffectList.class);
	private static final L2Effect[]			EMPTY_EFFECTS	= new L2Effect[0];
	private FastList<L2Effect>				_buffs;
	private FastList<L2Effect>				_debuffs;
	protected Map<String, List<L2Effect>>	_stackedEffects;
	private L2Character						_owner;

	public CharEffectList(L2Character owner)
	{
		_owner = owner;
	}

	public final L2Effect[] getAllEffects()
	{
		if ((_buffs == null || _buffs.isEmpty()) && (_debuffs == null || _debuffs.isEmpty()))
			return EMPTY_EFFECTS;
		LinkedBunch<L2Effect> temp = new LinkedBunch<L2Effect>();
		
		for (L2Effect eff : _buffs) try {
				if (eff != null)
					temp.add(eff);
		} catch(NullPointerException npe) {
		}
		
		for (L2Effect eff : _debuffs ) try {
				if (eff != null)
					temp.add(eff);
		} catch(NullPointerException npe) {
		}
		return temp.moveToArray(new L2Effect[temp.size()]);
	}

	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		L2Effect[] effects = getAllEffects();

		L2Effect eventNotInUse = null;
		for (L2Effect e : effects)
		{
			if (e.getEffectType() == tp)
			{
				if (e.getInUse())
					return e;
				eventNotInUse = e;
			}
		}
		return eventNotInUse;
	}

	public final L2Effect getFirstEffect(L2Skill skill)
	{
		L2Effect[] effects = getAllEffects();

		L2Effect eventNotInUse = null;
		for (L2Effect e : effects)
		{
			if (e.getSkill() == skill)
			{
				if (e.getInUse())
					return e;
				eventNotInUse = e;
			}
		}
		return eventNotInUse;
	}

	public final L2Effect getFirstEffect(int skillId)
	{
		L2Effect[] effects = getAllEffects();

		L2Effect eventNotInUse = null;
		for (L2Effect e : effects)
		{
			if (e.getSkill().getId() == skillId)
			{
				if (e.getInUse())
					return e;
				eventNotInUse = e;
			}
		}
		return eventNotInUse;
	}

	public int getBuffCount()
	{
		if (_buffs == null)
			return 0;
		int buffCount = 0;
		synchronized(_buffs)
		{
			for (L2Effect e : _buffs)
			{
				if(e.isBuff())
					buffCount++;
			}
		}
		return buffCount;
	}

	public int getDanceCount(boolean dances, boolean songs)
	{
		if (_buffs == null)
			return 0;
		int danceCount = 0;
		synchronized(_buffs)
		{
			for (L2Effect e : _buffs)
			{
				if (e != null && ((e.getSkill().isDance() && dances) || (e.getSkill().isSong() && songs)) && e.getInUse())
					danceCount++;
			}
		}
		return danceCount;
	}

	public final void stopAllEffects()
	{
		L2Effect[] effects = getAllEffects();

		for (L2Effect e : effects)
		{
			if (e != null && e.getSkill().getId() != 5660)
				e.exit(null);
		}
	}

	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();

		// Exit them
		for (L2Effect e : effects)
		{
			if (e != null)
			{
				if (e instanceof EffectCharmOfCourage)
					continue;
				e.exit(e.getEffector());
			}
		}
	}

	public final void stopEffects(L2EffectType type)
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();

		// Go through all active skills effects
		for (L2Effect e : effects)
		{
			// Stop active skills effects of the selected type
			if (e.getEffectType() == type)
				e.exit(e.getEffector());
		}
	}

	public final void stopSkillEffects(int skillId)
	{
		// Get all skills effects on the L2Character
		L2Effect[] effects = getAllEffects();

		for (L2Effect e : effects)
		{
			if (e.getSkill().getId() == skillId)
				e.exit(e.getEffector());
		}
	}

	/**
	 * Removes the first buff of this list.
	 *
	 * @param s Is the skill that is being applied.
	 */
	private void removeFirstBuff(L2Skill checkSkill)
	{
		if (getBuffCount() >= _owner.getMaxBuffCount())
		{
			if (checkSkill.getSkillType() != L2SkillType.BUFF && checkSkill.getSkillType() != L2SkillType.REFLECT && checkSkill.getSkillType() != L2SkillType.HEAL_PERCENT && checkSkill.getSkillType() != L2SkillType.MANAHEAL_PERCENT)
				return;
		}
		else
			return;

		L2Effect[] effects = getAllEffects();
		L2Effect removeMe = null;

		for (L2Effect e : effects)
		{
			if (e == null)
				continue;

			if (e.getSkill().bestowed())
				continue;

			switch (e.getSkill().getSkillType())
			{
				case BUFF:
				case DEBUFF:
				case REFLECT:
				case HEAL_PERCENT:
				case MANAHEAL_PERCENT:
					break;
				default:
					continue;
			}

			//don't remove charm of courage
			if (e.getEffectType() == L2EffectType.CHARMOFCOURAGE)
				continue;

			if (e.getSkill().getId().equals(checkSkill.getId()))
			{
				removeMe = e;
				break;
			}
			else if (removeMe == null)
				removeMe = e;
		}
		if (removeMe != null)
			removeMe.exit(removeMe.getEffector());
	}

	public final void removeEffect(L2Effect effect)
	{
		if (effect == null || (_buffs == null && _debuffs == null))
			return;

		FastList<L2Effect> effectList = effect.getSkill().isDebuff() ? _debuffs : _buffs;

		synchronized (effectList)
		{
			if (_stackedEffects == null)
				return;

			// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
			List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());

			if (stackQueue == null || stackQueue.size() < 1)
				return;

			// Get the identifier of the first stacked effect of the stack group selected
			L2Effect frontEffect = stackQueue.get(0);

			// Remove the effect from the stack group
			boolean removed = stackQueue.remove(effect);

			if (removed)
			{
				// Check if the first stacked effect was the effect to remove
				if (frontEffect == effect)
				{
					// Remove all its Func objects from the L2Character calculator set
					_owner.removeStatsOwner(effect);

					// Check if there's another effect in the Stack Group
					if (!stackQueue.isEmpty())
					{
						// Add its list of Funcs to the Calculator set of the L2Character
						for (L2Effect e : effectList)
						{
							if (e == stackQueue.get(0))
							{
								// Add its list of Funcs to the Calculator set of the L2Character
								_owner.addStatFuncs(e.getStatFuncs());
								// Set the effect to In Use
								e.setInUse(true);
								break;
							}
						}
					}
				}
				if (stackQueue.isEmpty())
					_stackedEffects.remove(effect.getStackType());
				else
					_stackedEffects.put(effect.getStackType(), stackQueue); // Update the Stack Group table _stackedEffects of the L2Character
			}

			// Remove the active skill L2effect from _effects of the L2Character
			// The Integer key of _effects is the L2Skill Identifier that has created the effect
			for (L2Effect e : effectList)
			{
				if (e == effect)
				{
					effectList.remove(e);
					if (_owner instanceof L2PcInstance)
					{
						SystemMessage sm;
						if (effect.getSkill().isToggle())
							sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ABORTED);
						else
							sm = new SystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED);
						sm.addSkillName(effect);
						_owner.sendPacket(sm);
					}
					break;
				}
			}
		}
	}

	public boolean isPossible(EffectTemplate newEffect,L2Skill skill) {
		
		if(_stackedEffects==null)
				return true;
		synchronized (_stackedEffects) {
			if(!_stackedEffects.containsKey(newEffect.stackType))
				return true;
			List<L2Effect> eff = _stackedEffects.get(newEffect.stackType);
			return eff == null || eff.isEmpty() || eff.get(0).getSkill().getEffectLevel() <= skill.getEffectLevel();
		}
		
	}
	public  void addEffect(L2Effect newEffect)
	{
		if (newEffect == null)
			return;

		synchronized (this)
		{
			if (_buffs == null)
				_buffs = new FastList<L2Effect>();
			if (_debuffs == null)
				_debuffs = new FastList<L2Effect>();
			if (_stackedEffects == null)
				_stackedEffects = new FastMap<String, List<L2Effect>>();
		}

		FastList<L2Effect> effectList = newEffect.getSkill().isDebuff() ? _debuffs : _buffs;
		L2Effect tempEffect = null;
		boolean stopNewEffect = false;

		synchronized (effectList)
		{
			// Check for same effects
			for (L2Effect e : effectList)
			{
				if (e != null && e.getSkill().getId().equals(newEffect.getSkill().getId()) && e.getEffectType() == newEffect.getEffectType() && e.getStackOrder() == newEffect.getStackOrder())
				{
					if (!newEffect.getSkill().isDebuff())
					{
						tempEffect = e; // exit this
						break;
					}

					// Started scheduled timer needs to be canceled.
					stopNewEffect = true;
					break;
				}
			}
		}

		if (tempEffect != null)
		{
			synchronized (this)
			{
				L2Skill skill = newEffect.getSkill();
				if (skill != null)
				{
					if (skill.bestowed() || skill.bestowTriggered() || skill.isChance())
					{
						newEffect.stopEffectTask();
						return;
					}
				}
				tempEffect.exit(newEffect.getEffector());
			}
		}

		if (newEffect.getEffectType() == L2EffectType.NOBLESSE_BLESSING && _owner.isInFunEvent())
			newEffect.stopEffectTask();

		// if max buffs, no herb effects are used, even if they would replace one old
		if (stopNewEffect || (getBuffCount() >= _owner.getMaxBuffCount() && newEffect.isHerbEffect()))
		{ 
			newEffect.stopEffectTask(); 
			return; 
		}

		// Remove first buff when buff list is full
		L2Skill tempSkill = newEffect.getSkill();
		if (!_stackedEffects.containsKey(newEffect.getStackType()) && !tempSkill.isDebuff() && !tempSkill.bestowed() && !(tempSkill.getId() > 4360 && tempSkill.getId() < 4367)) {
				removeFirstBuff(tempSkill);
		}

		if (getBuffCount() >= _owner.getMaxBuffCount() && newEffect.isBuff())
		{
			newEffect.stopEffectTask();
			return;
		}


		synchronized (effectList)
		{
			// Add the L2Effect to all effect in progress on the L2Character
			if (!newEffect.getSkill().isToggle() && !newEffect.getSkill().isDebuff())
			{
				int pos = 0;
				for (L2Effect e : effectList)
				{
					if (e != null)
					{
						int skillid = e.getSkill().getId();
						if (!e.getSkill().isToggle() && (!(skillid > 4360  && skillid < 4367)))
							pos++;
					}
					else break;
				}
				effectList.add(pos, newEffect);
			}
			else
				effectList.addLast(newEffect);
		}

		// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
		List<L2Effect> stackQueue = _stackedEffects.get(newEffect.getStackType());
		if (stackQueue == null)
			stackQueue = new FastList<L2Effect>();

		L2Effect[] allEffects = getAllEffects();

		tempEffect = null;
		if (!stackQueue.isEmpty())
		{
			// Get the first stacked effect of the Stack group selected
			for (L2Effect e : allEffects)
			{
				if (e == stackQueue.get(0))
				{
					tempEffect = e;
					break;
				}
			}
		}

		// Add the new effect to the stack group selected at its position
		stackQueue = effectQueueInsert(newEffect, stackQueue);

		if (stackQueue == null)
			return;

		// Update the Stack Group table _stackedEffects of the L2Character
		_stackedEffects.put(newEffect.getStackType(), stackQueue);

		// Get the first stacked effect of the Stack group selected
		L2Effect tempEffect2 = null;
		for (L2Effect e : allEffects)
		{
			if (e == stackQueue.get(0))
			{
				tempEffect2 = e;
				break;
			}
		}

		if (tempEffect != tempEffect2)
		{
			if (tempEffect != null)
			{
				// Remove all Func objects corresponding to this stacked effect from the Calculator set of the L2Character
				_owner.removeStatsOwner(tempEffect);

				// Set the L2Effect to Not In Use
				tempEffect.setInUse(false);
			}
			if (tempEffect2 != null)
			{
				// Set this L2Effect to In Use
				tempEffect2.setInUse(true);

				// Add all Func objects corresponding to this stacked effect to the Calculator set of the L2Character
				_owner.addStatFuncs(tempEffect2.getStatFuncs());
			}
		}
	}

	private List<L2Effect> effectQueueInsert(L2Effect newStackedEffect, List<L2Effect> stackQueue)
	{
		FastList<L2Effect> effectList = newStackedEffect.getSkill().isDebuff() ? _debuffs : _buffs;

		// Get the L2Effect corresponding to the effect identifier from the L2Character _effects
		if (_buffs == null && _debuffs == null)
			return null;

		// Create an Iterator to go through the list of stacked effects in progress on the L2Character
		Iterator<L2Effect> queueIterator = stackQueue.iterator();

		int i = 0;
		while (queueIterator.hasNext())
		{
			L2Effect cur = queueIterator.next();
			if (newStackedEffect.getStackOrder() < cur.getStackOrder())
				i++;
			else
				break;
		}

		// Add the new effect to the Stack list in function of its position in the Stack group
		stackQueue.add(i, newStackedEffect);

		// skill.exit() could be used, if the users don't wish to see "effect
		// removed" always when a timer goes off, even if the buff isn't active
		// any more (has been replaced). but then check e.g. npc hold and raid petrification.
		if (Config.EFFECT_CANCELING && !newStackedEffect.isHerbEffect() && stackQueue.size() > 1)
		{
			synchronized(effectList)
			{
				// only keep the current effect, cancel other effects
				for (L2Effect e : effectList)
				{
					if (e == stackQueue.get(1))
					{
						effectList.remove(e);
						break;
					}
				}
			}
			stackQueue.remove(1);
		}
		return stackQueue;
	}
}