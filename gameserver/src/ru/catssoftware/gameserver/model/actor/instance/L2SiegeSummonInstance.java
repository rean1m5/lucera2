package ru.catssoftware.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;


public class L2SiegeSummonInstance extends L2SummonInstance
{
	public static final int	SIEGE_GOLEM_ID	= 14737;
	public static final int	HOG_CANNON_ID	= 14768;
	public static final int	SWOOP_CANNON_ID	= 14839;

	private boolean onSiegeMode = false;
	public ScheduledFuture<?> changeModeThread = null;
	
	public L2SiegeSummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner, skill);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		Siege siege = SiegeManager.getInstance().getSiege(this);
		if (!getOwner().isGM() && (siege == null || !siege.getIsInProgress()) && !isInsideZone(L2Zone.FLAG_SIEGE))
		{
			unSummon(getOwner());
			getOwner().sendMessage(Message.getMessage(getOwner(), Message.MessageId.MSG_NOT_IN_SIEGE_ZONE));
		}
	}
	
	public boolean isOnSiegeMode()
	{
		return onSiegeMode;
	}
	public boolean isSiegeModeChanging()
	{
		if (changeModeThread!=null && changeModeThread.isDone())
			return true;
		return false;
	}
	public void changeSiegeMode()
	{
		if (changeModeThread!=null && !changeModeThread.isDone())
		{
			getOwner().sendMessage(Message.getMessage(getOwner(), Message.MessageId.MSG_WAIT_CHANGE_MODE_END));
			return;
		}
		getOwner().sendMessage(Message.getMessage(getOwner(), Message.MessageId.MSG_CHANGE_MODE));
		changeModeThread = ThreadPoolManager.getInstance().scheduleGeneral(new changeSiegeMode(),30000);
		setFollowStatus(false);
	}
	public void resetSiegeModeChange()
	{
		if (changeModeThread!=null && !changeModeThread.isDone())
		{
			getOwner().sendMessage(Message.getMessage(getOwner(), Message.MessageId.MSG_CHANGE_MODE_CANCEL));
			changeModeThread.cancel(true);
		}
	}
	private class changeSiegeMode implements Runnable
	{
		public void run()
		{
			getOwner().sendMessage(Message.getMessage(getOwner(), Message.MessageId.MSG_CHANGE_MODE_END));
			if (isOnSiegeMode())
			{
				onSiegeMode = false;
				setFollowStatus(true);
			}
			else
				onSiegeMode = true;
		}
	}
}
