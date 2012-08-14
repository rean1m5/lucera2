package ru.catssoftware.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;
import org.apache.log4j.Logger;


import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2HelperPetInstance extends L2BabyPetInstance 
{
	private final static Logger _log = Logger.getLogger(L2HelperPetInstance.class.getName());
	
	private ScheduledFuture<?>	_timeTask;
	private int					_timeConsume;
	
	public L2HelperPetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner, control);
		_timeConsume = getPetData().getPetFeedNormal();
	}

	@Override
	public synchronized void startFeed()
	{
		stopFeed();
		if (!isDead() && getOwner().getPet() == this)
			_timeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimeTask(), 10000, 10000);
	}	

	@Override
	public synchronized void stopFeed()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
			_timeTask = null;
		}
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		//Эти петы не набирают опыт.
	}	
	
	@Override
	public final boolean isHungry()
	{
		return false;
	}	
	
	class TimeTask implements Runnable
	{
		public void run()
		{
			if (getOwner() == null || getOwner().getPet() == null || getOwner().getPet().getObjectId() != getObjectId())
			{
				stopFeed();
				return;
			}
			else if (getCurrentFed() > _timeConsume)
				setCurrentFed(getCurrentFed() - _timeConsume);
			else
				setCurrentFed(0);

			if (getCurrentFed() == 0)
			{
				getOwner().sendMessage(Message.getMessage(getOwner(), Message.MessageId.MSG_FEED_TIME_END));
				stopFeed();
				_log.info("Helper pet deleted for player :" + getOwner().getName() + " Control Item Id :" + getControlItemId());
				deleteMe(getOwner());
			}
			switch (getCurrentFed())
			{
			case 1799:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "300"));
				break;
			case 1440:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "240"));
				break;
			case 1080:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "180"));
				break;
			case 720:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "120"));
				break;
			case 360:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "60"));
				break;
			case 180:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "30"));
				break;
			case 90:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "15"));
				break;
			case 60:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "10"));
				break;
			case 30:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "5"));
				break;
			case 6:
				getOwner().sendMessage(String.format(Message.getMessage(getOwner(), Message.MessageId.MSG_PET_UNSUMMON_IN_MINUTES), "1"));
				break;
			}
			broadcastStatusUpdate();
		}
	}	
}
