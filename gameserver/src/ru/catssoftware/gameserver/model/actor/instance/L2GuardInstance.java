package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Guard;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.L2WorldRegion;
import ru.catssoftware.gameserver.model.actor.knownlist.GuardKnownList;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.taskmanager.AbstractIterativePeriodicTaskManager;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public final class L2GuardInstance extends L2Guard
{
	private static final class GuardReturnHomeManager extends AbstractIterativePeriodicTaskManager<L2GuardInstance>
	{
		private static final GuardReturnHomeManager _instance = new GuardReturnHomeManager();
		
		private static GuardReturnHomeManager getInstance()
		{
			return _instance;
		}
		
		private GuardReturnHomeManager()
		{
			super(RETURN_INTERVAL);
		}
		
		@Override
		protected void callTask(L2GuardInstance task)
		{
			if (task.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				task.returnHome();
		}
		
		@Override
		protected String getCalledMethodName()
		{
			return "returnHome()";
		}
	}

	private static final int	RETURN_INTERVAL	= 60000;


	/**
	 * Constructor of L2GuardInstance (use L2Character and L2NpcInstance
	 * constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the
	 * L2GuardInstance (copy skills from template to object and link
	 * _calculators to NPC_STD_CALCULATOR)</li> <li>Set the name of the
	 * L2GuardInstance</li> <li>Create a RandomAnimation Task that will be
	 * launched after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 * 
	 * @param objectId Identifier of the object to initialized
	 * @param template Template to apply to the NPC
	 */
	public L2GuardInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // Init knownlist

		GuardReturnHomeManager.getInstance().startTask(this);
	}

	@Override
	public final GuardKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new GuardKnownList(this);

		return (GuardKnownList) _knownList;
	}

	/**
	 * Return True if hte attacker is a L2MonsterInstance.<BR><BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return attacker instanceof L2MonsterInstance;
	}

	/**
	 * Notify the L2GuardInstance to return to its home location
	 * (AI_INTENTION_MOVE_TO) and clear its _aggroList.<BR><BR>
	 */
	@Override
	public void returnHome()
	{
		if (!isDead())
			if (!isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), 150, false))
			{
				clearAggroList();

				teleToLocation(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz());
			}
	}

	/**
	 * Set the home location of its L2GuardInstance.<BR><BR>
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		// Check the region where this mob is, do not activate the AI if region is inactive.
		L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if ((region != null) && (!region.isActive()))
			getAI().stopAITask();
	}

	/**
	 * Return the pathfile of the selected HTML file in function of the
	 * L2GuardInstance Identifier and of the page number.<BR><BR>
	 * <B><U> Format of the pathfile </U> :</B><BR><BR>
	 * <li>if page number = 0 : <B>data/html/guard/12006.htm</B> (npcId-page
	 * number)</li> <li>if page number > 0 : <B>data/html/guard/12006-1.htm</B>
	 * (npcId-page number)</li><BR><BR>
	 * 
	 * @param npcId The Identifier of the L2NpcInstance whose text must be
	 *            display
	 * @param val The number of the page to display
	 */
	@Override
	protected String getHtmlFolder() {
		return "guard";
	}

	

	/**
	 * Manage actions when a player click on the L2GuardInstance.<BR><BR>
	 * <B><U> Actions on first click on the L2GuardInstance (Select it)</U>
	 * :</B><BR><BR>
	 * <li>Set the L2GuardInstance as target of the L2PcInstance player (if
	 * necessary)</li> <li>Send a Server->Client packet MyTargetSelected to the
	 * L2PcInstance player (display the select window)</li> <li>Set the
	 * L2PcInstance Intention to AI_INTENTION_IDLE</li> <li>Send a
	 * Server->Client packet ValidateLocation to correct the L2GuardInstance
	 * position and heading on the client</li><BR><BR>
	 * <B><U> Actions on second click on the L2GuardInstance (Attack it/Interact
	 * with it)</U> :</B><BR><BR>
	 * <li>If L2PcInstance is in the _aggroList of the L2GuardInstance, set the
	 * L2PcInstance Intention to AI_INTENTION_ATTACK</li> <li>If L2PcInstance is
	 * NOT in the _aggroList of the L2GuardInstance, set the L2PcInstance
	 * Intention to AI_INTENTION_INTERACT (after a distance verification) and
	 * show message</li><BR><BR>
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li>Client packet : Action, AttackRequest</li><BR><BR>
	 * 
	 * @param player The L2PcInstance that start an action on the
	 *            L2GuardInstance
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2GuardInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The color to display in the select window is White
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the L2PcInstance is in the _aggroList of the L2GuardInstance
			if (containsTarget(player))
			{
				// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				// Calculate the distance between the L2PcInstance and the L2NpcInstance
				if (!canInteract(player))
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				else
				{
					// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
					// to display a social action of the L2GuardInstance on their client
					SocialAction sa = new SocialAction(getObjectId(), Rnd.nextInt(8));
					broadcastPacket(sa);

					// Open a chat window on client with the text of the L2GuardInstance
					Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
					if ((qlsa != null) && (qlsa.length > 0))
						player.setLastQuestNpcObject(getObjectId());
					Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
					if ((qlst != null) && (qlst.length == 1))
						qlst[0].notifyFirstTalk(this, player);
					else
						showChatWindow(player, 0);
				}
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public L2GuardInstance getGuard()
	{
		return this;
	}

	public boolean isGuard()
	{
		return true;
	}
}