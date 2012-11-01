package ru.catssoftware.gameserver.ai;

import org.apache.log4j.Logger;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;
import ru.catssoftware.gameserver.taskmanager.AttackStanceTaskManager;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Util;

import static ru.catssoftware.gameserver.ai.CtrlIntention.*;


public abstract class AbstractAI implements Ctrl
{
	protected static final Logger _log = Logger.getLogger(AbstractAI.class);

	private static final int FOLLOW_INTERVAL = 1000;
	private static final int ATTACK_FOLLOW_INTERVAL = 500;

	private static final class FollowTaskManager extends AbstractIterativePeriodicTaskManager<AbstractAI>
	{
		private static final FollowTaskManager _instance = new FollowTaskManager();
		
		private static FollowTaskManager getInstance()
		{
			return _instance;
		}
		
		private FollowTaskManager()
		{
			super(FOLLOW_INTERVAL);
		}
		
		@Override
		protected void callTask(AbstractAI task)
		{
			task.followTarget();
		}
		
		@Override
		protected String getCalledMethodName()
		{
			return "followTarget()";
		}
	}

	private static final class AttackFollowTaskManager extends AbstractIterativePeriodicTaskManager<AbstractAI>
	{
		private static final AttackFollowTaskManager _instance = new AttackFollowTaskManager();
		
		private static AttackFollowTaskManager getInstance()
		{
			return _instance;
		}
		
		private AttackFollowTaskManager()
		{
			super(ATTACK_FOLLOW_INTERVAL);
		}
		
		@Override
		protected void callTask(AbstractAI task)
		{
			task.followTarget();
		}
		
		@Override
		protected String getCalledMethodName()
		{
			return "attackFollowTarget()";
		}
	}

	private L2Character _followTarget;

	private int _followRange;

	public synchronized final void followTarget()
	{
		final double distance = Util.calculateDistance(_actor, _followTarget, true);
		
		// TODO: fix Z axis follow support, moveToLocation needs improvements
		// Does not allow targets to follow on infinite distance -> fix for "follow me bug".
		// if the target is too far (maybe also teleported)
		if (distance > (_actor.isPlayer() && _followTarget.isPlayer() ? 2000 : 3000))
		{
			if (_actor instanceof L2Summon)
				((L2Summon)_actor).setFollowStatus(false);
			
			setIntention(AI_INTENTION_IDLE);
			return;
		}
		else if (distance > _followRange)
		{
			moveToPawn(_followTarget, _followRange);
		}
	}

	public final synchronized void startFollow(L2Character target)
	{
		if (target == null)
		{
			stopFollow();
			return;
		}
		
		_followTarget = target;
		_followRange = 60;
		
		FollowTaskManager.getInstance().startTask(this);


		followTarget();
	}

	public final synchronized void startFollow(L2Character target, int range)
	{
		if (target == null)
		{
			stopFollow();
			return;
		}
		
		_followTarget = target;
		_followRange = range;
		
		AttackFollowTaskManager.getInstance().startTask(this);


		followTarget();
	}

	public final synchronized void stopFollow()
	{
		FollowTaskManager.getInstance().stopTask(this);
		AttackFollowTaskManager.getInstance().stopTask(this);
		
		_followTarget = null;
	}

	/** The character that this AI manages */
	protected final L2Character _actor;

	/** An accessor for private methods of the actor */
	protected final L2Character.AIAccessor _accessor;

	/** Current long-term intention */
	protected CtrlIntention _intention = AI_INTENTION_IDLE;

	/** Current long-term intention parameter */
	protected Object _intentionArg0 = null;

	/** Current long-term intention parameter */
	protected Object _intentionArg1 = null;

	/** Flags about client's state, in order to know which messages to send */
	protected boolean _clientMoving;

	/** Flags about client's state, in order to know which messages to send */
	protected boolean _clientAutoAttacking;

	/** Flags about client's state, in order to know which messages to send */
	protected int _clientMovingToPawnOffset;

	/** Different targets this AI maintains */
	private L2Object _target;
	private L2Character _castTarget;
	protected L2Character _attackTarget;

	/** The skill we are currently casting by INTENTION_CAST */
	L2Skill _skill;

	/** Different internal state flags */
	private int _moveToPawnTimeout;

	/**
	 * Constructor of AbstractAI.<BR>
	 * <BR>
	 * 
	 * @param accessor The AI accessor of the L2Character
	 */
	protected AbstractAI(L2Character.AIAccessor accessor)
	{
		_accessor = accessor;

		// Get the L2Character managed by this Accessor AI
		_actor = accessor.getActor();
	}

	/**
	 * Return the L2Character managed by this Accessor AI.<BR>
	 * <BR>
	 */
	public L2Character getActor()
	{
		return _actor;
	}

	/**
	 * Return the current Intention.<BR>
	 * <BR>
	 */
	public CtrlIntention getIntention()
	{
		return _intention;
	}

	protected synchronized void setCastTarget(L2Character target)
	{
		_castTarget = target;
	}

	/**
	 * Return the current cast target.<BR>
	 * <BR>
	 */
	public L2Character getCastTarget()
	{
		return _castTarget;
	}

	protected synchronized void setAttackTarget(L2Character target)
	{
		_attackTarget = target;
	}

	/**
	 * Return current attack target.<BR>
	 * <BR>
	 */
	public L2Character getAttackTarget()
	{
		return _attackTarget;
	}

	/**
	 * Set the Intention of this AbstractAI.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is USED by AI classes</B></FONT><BR>
	 * <BR>
	 * <B><U> Overridden in </U> : </B><BR>
	 * <B>L2AttackableAI</B> : Create an AI Task executed every 1s (if necessary)<BR>
	 * <B>L2PlayerAI</B> : Stores the current AI intention parameters to later restore it if necessary<BR>
	 * <BR>
	 *
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	public void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_intention = intention;
		_intentionArg0 = arg0;
		_intentionArg1 = arg1;
	}

	public Object getIntentionArg0()
	{
		return _intentionArg0;
	}

	public Object getIntentionArg1()
	{
		return _intentionArg1;
	}

	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR>
	 * <BR>
	 * 
	 * @param intention The new Intention to set to the AI
	 */
	public final void setIntention(CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}

	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR>
	 * <BR>
	 * 
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 */
	public final void setIntention(CtrlIntention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}

	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR>
	 * <BR>
	 * 
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 * @param arg1 The second parameter of the Intention (optional target)
	 */
	public final void setIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// Stop the follow mode if necessary
		
		L2CharTemplate template = _actor.getTemplate();
		if(template instanceof L2NpcTemplate) {
			Quest[] qlst = ((L2NpcTemplate)template).getEventQuests(Quest.QuestEventType.ON_INTENTION_CHANGE);
			if(qlst!=null)
				for(Quest q : qlst )
					q.notifyIntentionChange(_actor, intention);
		}
		if (intention != AI_INTENTION_FOLLOW && intention != AI_INTENTION_ATTACK)
			stopFollow();

		// Launch the onIntention method of the L2CharacterAI corresponding to the new Intention
		switch (intention)
		{
			case AI_INTENTION_IDLE:
				onIntentionIdle();
				break;
			case AI_INTENTION_ACTIVE:
				onIntentionActive();
				break;
			case AI_INTENTION_REST:
				onIntentionRest();
				break;
			case AI_INTENTION_ATTACK:
				onIntentionAttack((L2Character) arg0);
				break;
			case AI_INTENTION_CAST:
				onIntentionCast((L2Skill) arg0, (L2Object) arg1);
				break;
			case AI_INTENTION_MOVE_TO:
				onIntentionMoveTo((L2CharPosition) arg0);
				break;
			case AI_INTENTION_MOVE_TO_IN_A_BOAT:
				onIntentionMoveToInABoat((L2CharPosition) arg0, (L2CharPosition) arg1);
				break;
			case AI_INTENTION_FOLLOW:
				onIntentionFollow((L2Character) arg0);
				break;
			case AI_INTENTION_PICK_UP:
				onIntentionPickUp((L2Object) arg0);
				break;
			case AI_INTENTION_INTERACT:
				onIntentionInteract((L2Object) arg0);
				break;
		}
	}

	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character
	 * attack and is stunned, he will attack again after the stunned period)</B></FONT><BR>
	 * <BR>
	 * 
	 * @param evt The event whose the AI must be notified
	 */
	public final void notifyEvent(CtrlEvent evt)
	{
		notifyEvent(evt, null, null);
	}

	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character
	 * attack and is stunned, he will attack again after the stunned period)</B></FONT><BR>
	 * <BR>
	 * 
	 * @param evt The event whose the AI must be notified
	 * @param arg0 The first parameter of the Event (optional target)
	 */
	public final void notifyEvent(CtrlEvent evt, Object arg0)
	{
		notifyEvent(evt, arg0, null);
	}

	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character
	 * attack and is stunned, he will attack again after the stunned period)</B></FONT><BR>
	 * <BR>
	 * 
	 * @param evt The event whose the AI must be notified
	 * @param arg0 The first parameter of the Event (optional target)
	 * @param arg1 The second parameter of the Event (optional target)
	 */
	public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
	{
		if (!_actor.isVisible() || !_actor.hasAI())
			return;

		GameExtensionManager.getInstance().handleAction(_actor, Action.CHAR_INTENTIONCHANGE, evt,arg0,arg1);
		switch (evt)
		{
			case EVT_THINK:
				onEvtThink();
				break;
			case EVT_ATTACKED:
				onEvtAttacked((L2Character) arg0);
				break;
			case EVT_AGGRESSION:
				onEvtAggression((L2Character) arg0, ((Number) arg1).intValue());
				break;
			case EVT_STUNNED:
				onEvtStunned((L2Character) arg0);
				break;
			case EVT_PARALYZED:
				onEvtParalyzed((L2Character) arg0);
				break;
			case EVT_SLEEPING:
				onEvtSleeping((L2Character) arg0);
				break;
			case EVT_ROOTED:
				onEvtRooted((L2Character) arg0);
				break;
			case EVT_CONFUSED:
				onEvtConfused((L2Character) arg0);
				break;
			case EVT_MUTED:
				onEvtMuted((L2Character) arg0);
				break;
			case EVT_READY_TO_ACT:
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
					onEvtReadyToAct();
				break;
			case EVT_USER_CMD:
				onEvtUserCmd(arg0, arg1);
				break;
			case EVT_ARRIVED:
				// happens e.g. from stopmove but we don't process it if we're casting
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
					onEvtArrived();
				break;
			case EVT_ARRIVED_REVALIDATE:
				// this is disregarded if the char is not moving any more
				if (_actor.isMoving())
					onEvtArrivedRevalidate();
				break;
			case EVT_ARRIVED_BLOCKED:
				onEvtArrivedBlocked((L2CharPosition) arg0);
				break;
			case EVT_FORGET_OBJECT:
				onEvtForgetObject((L2Object) arg0);
				break;
			case EVT_CANCEL:
				onEvtCancel();
				break;
			case EVT_DEAD:
				onEvtDead();
				break;
			case EVT_FAKE_DEATH:
				onEvtFakeDeath();
				break;
			case EVT_FINISH_CASTING:
				onEvtFinishCasting();
				break;
		}
	}

	protected abstract void onIntentionIdle();

	protected abstract void onIntentionActive();

	protected abstract void onIntentionRest();

	protected abstract void onIntentionAttack(L2Character target);

	protected abstract void onIntentionCast(L2Skill skill, L2Object target);

	protected abstract void onIntentionMoveTo(L2CharPosition destination);

	protected abstract void onIntentionMoveToInABoat(L2CharPosition destination, L2CharPosition origin);

	protected abstract void onIntentionFollow(L2Character target);

	protected abstract void onIntentionPickUp(L2Object item);

	protected abstract void onIntentionInteract(L2Object object);

	protected abstract void onEvtThink();

	protected abstract void onEvtAttacked(L2Character attacker);

	protected abstract void onEvtAggression(L2Character target, int aggro);

	protected abstract void onEvtStunned(L2Character attacker);

	protected abstract void onEvtParalyzed(L2Character attacker);

	protected abstract void onEvtSleeping(L2Character attacker);

	protected abstract void onEvtRooted(L2Character attacker);

	protected abstract void onEvtConfused(L2Character attacker);

	protected abstract void onEvtMuted(L2Character attacker);

	protected abstract void onEvtReadyToAct();

	protected abstract void onEvtUserCmd(Object arg0, Object arg1);

	protected abstract void onEvtArrived();

	protected abstract void onEvtArrivedRevalidate();

	protected abstract void onEvtArrivedBlocked(L2CharPosition blocked_at_pos);

	protected abstract void onEvtForgetObject(L2Object object);

	protected abstract void onEvtCancel();

	protected abstract void onEvtDead();

	protected abstract void onEvtFakeDeath();

	protected abstract void onEvtFinishCasting();

	/**
	 * Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void clientActionFailed()
	{
		if (_actor.isPlayer())
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void moveToPawn(L2Object pawn, int offset)
	{
		// Check if actor can move
		if (!_actor.isMovementDisabled())
		{
			if (offset < 10)
				offset = 10;

			
			Location l = Util.findPointForDistance(_actor.getLoc(), pawn.getLoc(), -offset);
			if(!canMoveTo(l))
					return;
			// prevent possible extra calls to this function (there is none?),
			// also don't send movetopawn packets too often
			boolean sendPacket = true;
			if (_clientMoving && _target == pawn)
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (GameTimeController.getGameTicks() < _moveToPawnTimeout)
						return;
					sendPacket = false;
				}
				else if (_actor.isOnGeodataPath())
				{
					// minimum time to calculate new route is 2 seconds
					if (GameTimeController.getGameTicks() < (_moveToPawnTimeout + 10))
						return;
				}
			}

			// Set AI movement data
			_clientMoving = true;
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = GameTimeController.getGameTicks();
			_moveToPawnTimeout += 1000 / GameTimeController.MILLIS_IN_TICK;

			if (pawn == null || _accessor == null)
				return;

			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
			_accessor.moveTo(pawn.getX(), pawn.getY(), pawn.getZ(), offset);

			if (!_actor.isMoving())
			{
				_actor.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Send a Server->Client packet MoveToPawn/MoveToLocation to the actor and all L2PcInstance in its _knownPlayers
			if (pawn instanceof L2Character)
			{
				if (_actor.isOnGeodataPath())
				{
					_actor.broadcastPacket(new MoveToLocation(_actor));
					_clientMovingToPawnOffset = 0;
				}
				else if (sendPacket) // don't repeat unnecessarily
					_actor.broadcastPacket(new MoveToPawn(_actor, (L2Character) pawn, offset));
			}
			else
				_actor.broadcastPacket(new MoveToLocation(_actor));
		}
		else
			clientActionFailed();
	}

	/**
	 * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation
	 * <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void moveTo(int x, int y, int z)
	{
		// Check if actor can move
		if (!_actor.isMovementDisabled())
		{
			// Set AI movement data
			_clientMoving = true;
			_clientMovingToPawnOffset = 0;

			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
			if(_accessor.moveTo(x, y, z)) {
				MoveToLocation msg = new MoveToLocation(_actor);
				_actor.broadcastPacket(msg);
			}
		}
		else
			clientActionFailed();
	}

	protected void moveToInABoat(L2CharPosition destination, L2CharPosition origin)
	{
		// Chek if actor can move
		if (!_actor.isMovementDisabled())
		{
			/*  // Set AI movement data
			 _clientMoving = true;
			 _clientMoving_to_pawn_offset = 0;

			 // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
			 _accessor.moveTo(((L2PcInstance)_actor).getBoat().getX() - destination.x,((L2PcInstance)_actor).getBoat().getY()- destination.y,((L2PcInstance)_actor).getBoat().getZ() - destination.z);
			 */
			// Send a Server->Client packet MoveToLocation to the actor and all L2PcInstance in its _knownPlayers
			//CharMoveToLocation msg = new MoveToLocation(_actor);
			if (((L2PcInstance)_actor).getBoat() != null)
			{
				MoveToLocationInVehicle msg = new MoveToLocationInVehicle(_actor, destination, origin);
				_actor.broadcastPacket(msg);
			}
			
		}
		else
		{
			clientActionFailed();
		}
	}

	/**
	 * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation
	 * <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void clientStopMoving(L2CharPosition pos)
	{
		// Stop movement of the L2Character
		if (_actor.isMoving())
			_accessor.stopMove(pos);

		_clientMovingToPawnOffset = 0;

		if (_clientMoving || pos != null)
		{
			_clientMoving = false;

			// Send a Server->Client packet StopMove to the actor and all L2PcInstance in its _knownPlayers
			StopMove msg = new StopMove(_actor);
			_actor.broadcastPacket(msg);

			if (pos != null)
			{
				// Send a Server->Client packet StopRotation to the actor and all L2PcInstance in its _knownPlayers
				StopRotation sr = new StopRotation(_actor.getObjectId(), pos.heading, 65535);
				_actor.sendPacket(sr);
				_actor.broadcastPacket(sr);
			}
		}
	}

	// Client has already arrived to target, no need to force StopMove packet
	protected void clientStoppedMoving()
	{
		if (_clientMovingToPawnOffset > 0) // movetoPawn needs to be stopped
		{
			_clientMovingToPawnOffset = 0;
			StopMove msg = new StopMove(_actor);
			_actor.broadcastPacket(msg);
		}
		_clientMoving = false;
		_actor.finishMovement();
	}

	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}

	public void setAutoAttacking(boolean isAutoAttacking)
	{
		_clientAutoAttacking = isAutoAttacking;
		if (_actor instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _actor;
			if (summon.getOwner() != null)
				summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
			return;
		}
	}

	/**
	 * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	public void clientStartAutoAttack()
	{
		if (_actor instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _actor;
			if (summon.getOwner() != null)
				summon.getOwner().getAI().clientStartAutoAttack();
			return;
		}
		if (!isAutoAttacking())
		{
			if (_actor.isPlayer() && ((L2PcInstance) _actor).getPet() != null)
				((L2PcInstance) _actor).getPet().broadcastPacket(new AutoAttackStart(((L2PcInstance) _actor).getPet().getObjectId()));
			// Send a Server->Client packet AutoAttackStart to the actor and all L2PcInstance in its _knownPlayers
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}

	/**
	 * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	public void clientStopAutoAttack()
	{
		if (_actor instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _actor;
			if (summon.getOwner() != null)
				summon.getOwner().getAI().clientStopAutoAttack();
			return;
		}
		if (_actor.isPlayer())
		{
			if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor) && isAutoAttacking())
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
		}
		else if (isAutoAttacking())
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}

	/**
	 * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die
	 * <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 */
	protected void clientNotifyDead()
	{
		// Send a Server->Client packet Die to the actor and all L2PcInstance in its _knownPlayers
		Die msg = new Die(_actor);
		_actor.broadcastPacket(msg);

		// Init AI
		_intention = AI_INTENTION_IDLE;
		_target = null;
		_castTarget = null;
		setAttackTarget(null);

		// Cancel the follow task if necessary
		stopFollow();
	}

	/**
	 * Update the state of this actor client side by sending Server->Client packet MoveToPawn/MoveToLocation and
	 * AutoAttackStart to the L2PcInstance player.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * 
	 * @param player The L2PcIstance to notify with state of this L2Character
	 */
	public void describeStateToPlayer(L2PcInstance player)
	{
		if (_clientMoving)
		{
			if (_clientMovingToPawnOffset != 0 && _followTarget != null)
			{
				// Send a Server->Client packet MoveToPawn to the actor and all L2PcInstance in its _knownPlayers
				MoveToPawn msg = new MoveToPawn(_actor, _followTarget, _clientMovingToPawnOffset);
				player.sendPacket(msg);
			}
			else
			{
				// Send a Server->Client packet MoveToLocation to the actor and all L2PcInstance in its _knownPlayers
				MoveToLocation msg = new MoveToLocation(_actor);
				player.sendPacket(msg);
			}
		}
	}

	protected L2Character getFollowTarget()
	{
		return _followTarget;
	}

	protected L2Object getTarget()
	{
		return _target;
	}

	protected boolean canMoveTo(Location loc) {
		return true;
	}
	protected synchronized void setTarget(L2Object target)
	{
		_target = target;
	}
}