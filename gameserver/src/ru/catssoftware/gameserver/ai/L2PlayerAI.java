package ru.catssoftware.gameserver.ai;

import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_REST;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Character.AIAccessor;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2StaticObjectInstance;


public class L2PlayerAI extends L2CharacterAI
{
	private volatile boolean	_thinking;				// to prevent recursive thinking

	IntentionCommand			_nextIntention	= null;

	public L2PlayerAI(AIAccessor accessor)
	{
		super(accessor);
	}

	void saveNextIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}

	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}

	@Override
	public void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// do nothing unless CAST intention
		// however, forget interrupted actions when starting to use an offensive skill
		if (intention != AI_INTENTION_CAST || (arg0 != null && ((L2Skill) arg0).isOffensive()))
		{
			_nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		// do nothing if next intention is same as current one.
		if (intention == _intention && arg0 == _intentionArg0 && arg1 == _intentionArg1)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		// save current intention so it can be used after cast
		saveNextIntention(_intention, _intentionArg0, _intentionArg1);
		super.changeIntention(intention, arg0, arg1);
	}

	/**
	 * Launch actions corresponding to the Event ReadyToAct.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
	 *
	 */
	@Override
	protected void onEvtReadyToAct()
	{
		// Launch actions corresponding to the Event Think
		if (_nextIntention != null)
		{
			setIntention(_nextIntention._crtlIntention, _nextIntention._arg0, _nextIntention._arg1);
			_nextIntention = null;
		}
		super.onEvtReadyToAct();
	}

	/**
	 * Launch actions corresponding to the Event Cancel.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
	 *
	 */
	@Override
	protected void onEvtCancel()
	{
		_nextIntention = null;
		super.onEvtCancel();
	}

	/**
	 * Finalize the casting of a skill. This method overrides L2CharacterAI method.<BR><BR>
	 *
	 * <B>What it does:</B>
	 * Check if actual intention is set to CAST and, if so, retrieves latest intention
	 * before the actual CAST and set it as the current intention for the player
	 */
	@Override
	protected void onEvtFinishCasting()
	{
		if (getIntention() == AI_INTENTION_CAST)
		{
			// run interrupted or next intention
			IntentionCommand nextIntention = _nextIntention;
			if (nextIntention != null)
			{
				if (nextIntention._crtlIntention != AI_INTENTION_CAST) // previous state shouldn't be casting
					setIntention(nextIntention._crtlIntention, nextIntention._arg0, nextIntention._arg1);
				else
					setIntention(AI_INTENTION_IDLE);
			}
			else
				// set intention to idle if skill doesn't change intention.
				setIntention(AI_INTENTION_IDLE);
		}
	}

	@Override
	protected void onIntentionRest()
	{
		if (getIntention() != AI_INTENTION_REST)
		{
			changeIntention(AI_INTENTION_REST, null, null);
			setTarget(null);
			if (getAttackTarget() != null)
				setAttackTarget(null);
			clientStopMoving(null);
		}
	}

	@Override
	protected void onIntentionActive()
	{
		setIntention(AI_INTENTION_IDLE);
	}

	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<BR><BR>
	 *
	 * <B><U> Actions</U> : </B><BR><BR>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
	 * <li>Set the Intention of this AI to AI_INTENTION_MOVE_TO </li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast) </li><BR><BR>
	 *
	 */
	@Override
	protected void onIntentionMoveTo(L2CharPosition pos)
	{
		if (getIntention() == AI_INTENTION_REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
			clientActionFailed();
			return;
		}

		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			clientActionFailed();
			saveNextIntention(AI_INTENTION_MOVE_TO, pos, null);
			return;
		}

		// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
		changeIntention(AI_INTENTION_MOVE_TO, pos, null);

		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();

		// Abort the attack of the L2Character and send Server->Client ActionFailed packet
		_actor.abortAttack();

		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
		moveTo(pos.x, pos.y, pos.z);
	}

	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		_clientMoving = false;

		super.clientNotifyDead();
	}

	private void thinkAttack()
	{
		L2Character target = getAttackTarget();
		if (target == null)
		{
			clientActionFailed();
			return;
		}

		if (checkTargetLostOrDead(target))
		{
			// Notify the target
			setAttackTarget(null);
			clientActionFailed();
			return;
		}

		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			clientActionFailed();
			return;
		}

		_accessor.doAttack(target);
	}

	private void thinkCast()
	{
		L2Character target = getCastTarget();

		if (_skill.getTargetType() == SkillTargetType.TARGET_GROUND && _actor instanceof L2PcInstance)
		{
			if (maybeMoveToPosition(((L2PcInstance) _actor).getCurrentSkillWorldPosition(), _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setIsCastingNow(false);
				return;
			}
		}
		else
		{
			if (checkTargetLost(target))
			{
				if (_skill.isOffensive() && getAttackTarget() != null)
				{
					//Notify the target
					setCastTarget(null);
				}
				_actor.setIsCastingNow(false);
				return;
			}
			if (target != null && maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setIsCastingNow(false);
				clientActionFailed();
				return;
			}
		}

		if (_skill.getHitTime() > 50)
			clientStopMoving(null);

		L2Object oldTarget = _actor.getTarget();
		if (oldTarget != null && target != null && oldTarget != target)
		{
			// Replace the current target by the cast target
			_actor.setTarget(getCastTarget());
			// Launch the Cast of the skill
			_accessor.doCast(_skill);
			// Restore the initial target
			_actor.setTarget(oldTarget);
		}
		else
			_accessor.doCast(_skill);
	}

	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}
		L2Object target = getTarget();
		if (checkTargetLost(target))
		{
			clientActionFailed();
			return;
		}
		if (maybeMoveToPawn(target, 36))
		{
			clientActionFailed();
			return;
		}
		setIntention(AI_INTENTION_IDLE);
		((L2PcInstance.AIAccessor) _accessor).doPickupItem(target);
	}

	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}
		L2Object target = getTarget();
		if (checkTargetLost(target))
		{
			clientActionFailed();
			return;
		}
		if (maybeMoveToPawn(target, 36))
		{
			clientActionFailed();
			return;
		}
		if (!(target instanceof L2StaticObjectInstance))
			((L2PcInstance.AIAccessor) _accessor).doInteract((L2Character) target);
		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void onEvtThink()
	{
		if (_thinking && getIntention() != AI_INTENTION_CAST) // casting must always continue
		{
			clientActionFailed();
			return;
		}

		_thinking = true;
		try
		{
			if (getIntention() == AI_INTENTION_ATTACK)
				thinkAttack();
			else if (getIntention() == AI_INTENTION_CAST)
				thinkCast();
			else if (getIntention() == AI_INTENTION_PICK_UP)
				thinkPickUp();
			else if (getIntention() == AI_INTENTION_INTERACT)
				thinkInteract();
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtArrivedRevalidate()
	{
		getActor().getKnownList().updateKnownObjects();
		super.onEvtArrivedRevalidate();
	}
}
