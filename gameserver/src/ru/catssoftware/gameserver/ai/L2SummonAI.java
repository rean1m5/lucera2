package ru.catssoftware.gameserver.ai;

import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.instancemanager.grandbosses.QueenAntManager;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.L2Character.AIAccessor;
import ru.catssoftware.gameserver.model.actor.instance.L2SiegeSummonInstance;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.serverpackets.AutoAttackStop;
import ru.catssoftware.gameserver.taskmanager.AttackStanceTaskManager;


public class L2SummonAI extends L2CharacterAI
{
	private volatile boolean	_thinking; // to prevent recursive thinking
	private boolean				_startFollow	= ((L2Summon) _actor).getFollowStatus();

	public L2SummonAI(AIAccessor accessor)
	{
		super(accessor);
	}
	
	@Override
	public void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		super.changeIntention(intention, arg0, arg1);
	}

	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}

	@Override
	protected void onIntentionActive()
	{
		L2Summon summon = (L2Summon) _actor;
		if (_startFollow)
			setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
		else {
			super.onIntentionActive();
		}
	}

	private boolean checkZone() {
		if (_actor.isInsideZone(L2Zone.FLAG_QUEEN))
		{
			if (_actor.getPlayer().getLevel()>QueenAntManager.SAFE_LEVEL)
			{
				_actor.abortAttack();
				_actor.abortCast();
				_actor.getAI().setIntention(AI_INTENTION_FOLLOW,_actor.getPlayer());
				SkillTable.getInstance().getInfo(4515, 1).getEffects(_actor.getPlayer(), _actor.getPlayer());
//				_actor.getPlayer().teleToLocation(TeleportWhereType.Town);
				_actor.abortAttack();
				_actor.abortCast();
				super.changeIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
				return false;
			}
		}
		return true;
	}
	private void thinkAttack()
	{
		if(!checkZone())
			return;
		if (checkTargetLostOrDead(getAttackTarget()))
		{
			setAttackTarget(null);
			return;
		}
		if (_actor instanceof L2SiegeSummonInstance && ((L2SiegeSummonInstance)_actor).isOnSiegeMode())
			return;
		if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange()))
			return;
		clientStopMoving(null);
		_accessor.doAttack(getAttackTarget());
	}

	private void thinkCast()
	{
		if(!checkZone())
			return;
		L2Summon summon = (L2Summon) _actor;
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		boolean val = _startFollow;
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
			return;
		clientStopMoving(null);
		summon.setFollowStatus(false);
		setIntention(AI_INTENTION_IDLE);
		_startFollow = val;
		_accessor.doCast(_skill);
	}

	private void thinkPickUp()
	{
		if (checkTargetLost(getTarget()))
			return;
		if (maybeMoveToPawn(getTarget(), 36))
			return;
		if(!checkZone())
			return;

		setIntention(AI_INTENTION_IDLE);
		((L2Summon.AIAccessor) _accessor).doPickupItem(getTarget());
	}

	private void thinkInteract()
	{
		if (checkTargetLost(getTarget()))
			return;
		if (maybeMoveToPawn(getTarget(), 36))
			return;

		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void onEvtCancel()
	{
		_actor.abortCast();

		// Stop an AI Follow Task
		stopFollow();

		if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	}
	@Override
	protected void onEvtThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
			return;
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case AI_INTENTION_ATTACK:
					thinkAttack();
					break;
				case AI_INTENTION_CAST:
					thinkCast();
					break;
				case AI_INTENTION_PICK_UP:
					thinkPickUp();
					break;
				case AI_INTENTION_INTERACT:
					thinkInteract();
					break;
			}
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtFinishCasting()
	{
		if (getIntention() != AI_INTENTION_ATTACK)
			((L2Summon)_actor).setFollowStatus(_startFollow);
	}

	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case AI_INTENTION_ACTIVE:
			case AI_INTENTION_FOLLOW:
			case AI_INTENTION_IDLE:
				((L2Summon) _actor).setFollowStatus(_startFollow);
		}
	}

	public void setStartFollowController(boolean val)
	{
		_startFollow = val;
	}
}
