package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.ai.L2ControllableMobAI;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author littlecrow
 */
public class L2ControllableMobInstance extends L2MonsterInstance
{
	private boolean				_isInvul;
	private L2ControllableMobAI	_aiBackup;

	protected class ControllableAIAcessor extends AIAccessor
	{
		@Override
		public void detachAI()
		{
			// do nothing, AI of controllable mobs can't be detached automatically
		}
	}

	@Override
	public boolean isAggressive()
	{
		return true;
	}

	@Override
	public int getAggroRange()
	{
		// force mobs to be aggro
		return 500;
	}

	public L2ControllableMobInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_aiBackup == null)
				{
					_ai = new L2ControllableMobAI(new ControllableAIAcessor());
					_aiBackup = (L2ControllableMobAI) _ai;
				}
				else
				{
					_ai = _aiBackup;
				}
				return _ai;
			}
		}
		return ai;
	}

	@Override
	public boolean isInvul()
	{
		return _isInvul;
	}

	public void setInvul(boolean isInvul)
	{
		_isInvul = isInvul;
	}

	@Override
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (isInvul() || isDead() || isPetrified())
			return;

		if (awake)
		{
			if (isSleeping())
				stopSleeping(null);
			if (isImmobileUntilAttacked())
				stopImmobileUntilAttacked(null);
		}

		i = getStatus().getCurrentHp() - i;

		if (i < 0)
			i = 0;

		getStatus().setCurrentHp(i);

		if (getStatus().getCurrentHp() < 0.5) // Die
		{
			stopMove(null);
			doDie(attacker);
			getStatus().setCurrentHp(0);
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		removeAI();
		return true;
	}

	@Override
	public void deleteMe()
	{
		removeAI();
		super.deleteMe();
	}

	/**
	 * Definitively remove AI
	 */
	protected void removeAI()
	{
		synchronized (this)
		{
			if (_aiBackup != null)
			{
				_aiBackup.setIntention(CtrlIntention.AI_INTENTION_IDLE);
				_aiBackup = null;
				_ai = null;
			}
		}
	}
}
