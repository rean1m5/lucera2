package ru.catssoftware.gameserver.ai;

import javolution.util.FastMap;
import javolution.util.FastMap.Entry;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.threadmanager.FIFOExecutableQueue;


public class L2DoorAI extends L2CharacterAI
{

	public L2DoorAI(L2DoorInstance.AIAccessor accessor)
	{
		super(accessor);
	}

	@Override
	protected void onIntentionIdle()
	{
	}

	@Override
	protected void onIntentionActive()
	{
	}

	@Override
	protected void onIntentionRest()
	{
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
	}

	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
	}

	@Override
	protected void onIntentionMoveTo(Location destination)
	{
	}

	@Override
	protected void onIntentionFollow(L2Character target)
	{
	}

	@Override
	protected void onIntentionPickUp(L2Object item)
	{
	}

	@Override
	protected void onIntentionInteract(L2Object object)
	{
	}

	@Override
	protected void onEvtThink()
	{
	}

	private GuardNotificationQueue _guardNotificationTasks;

	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		if (_guardNotificationTasks == null)
			_guardNotificationTasks = new GuardNotificationQueue();

		_guardNotificationTasks.add(attacker);
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
	}

	@Override
	protected void onEvtStunned(L2Character attacker)
	{
	}

	@Override
	protected void onEvtSleeping(L2Character attacker)
	{
	}

	@Override
	protected void onEvtRooted(L2Character attacker)
	{
	}

	@Override
	protected void onEvtReadyToAct()
	{
	}

	@Override
	protected void onEvtUserCmd(Object arg0, Object arg1)
	{
	}

	@Override
	protected void onEvtArrived()
	{
	}

	@Override
	protected void onEvtArrivedRevalidate()
	{
	}

	@Override
	protected void onEvtArrivedBlocked(Location blocked_at_pos)
	{
	}

	@Override
	protected void onEvtForgetObject(L2Object object)
	{
	}

	@Override
	protected void onEvtCancel()
	{
	}

	@Override
	protected void onEvtDead()
	{
	}

	private final class GuardNotificationQueue extends FIFOExecutableQueue
	{
		private final FastMap<L2Character, Integer> _map = new FastMap<L2Character, Integer>();

		private void add(L2Character attacker)
		{
			synchronized (_map)
			{
				Entry<L2Character, Integer> entry = _map.getEntry(attacker);

				if (entry != null)
					entry.setValue(entry.getValue() + 15);
				else
					_map.put(attacker, 15);
			}

			execute();
		}

		@Override
		protected boolean isEmpty()
		{
			synchronized (_map)
			{
				return _map.isEmpty();
			}
		}

		@Override
		protected void removeAndExecuteFirst()
		{
			L2Character attacker = null;
			int aggro = 0;

			synchronized (_map)
			{
				Entry<L2Character, Integer> first = _map.head().getNext();

				attacker = first.getKey();
				aggro = first.getValue();

				_map.remove(attacker);
			}

			getActor().getKnownList().updateKnownObjects();

			for (L2Object obj : getActor().getKnownList().getKnownObjects().values())
			{
				if (obj instanceof L2SiegeGuard)
				{
					L2SiegeGuard guard = (L2SiegeGuard) obj;

					if (Math.abs(attacker.getZ() - guard.getZ()) < 200)
					{
						if (getActor().isInsideRadius(guard, guard.getFactionRange(), false, true))
							guard.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, aggro);
					}
				}
			}
		}
	}
}