package ru.catssoftware.gameserver.model;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.L2GameServer;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlEvent;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2AttackableAI;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.geodata.pathfinding.AbstractNodeLoc;
import ru.catssoftware.gameserver.geodata.pathfinding.PathFinding;
import ru.catssoftware.gameserver.handler.SkillHandler;
import ru.catssoftware.gameserver.instancemanager.FactionManager;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortResistSiegeManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance.SkillDat;
import ru.catssoftware.gameserver.model.actor.knownlist.CharKnownList;
import ru.catssoftware.gameserver.model.actor.listeners.CharactionListeners;
import ru.catssoftware.gameserver.model.actor.stat.CharStat;
import ru.catssoftware.gameserver.model.actor.status.CharStatus;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.network.serverpackets.FlyToLocation.FlyType;
import ru.catssoftware.gameserver.skills.AbnormalEffect;
import ru.catssoftware.gameserver.skills.Calculator;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.effects.EffectTemplate;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;
import ru.catssoftware.gameserver.skills.l2skills.L2SkillChargeDmg;
import ru.catssoftware.gameserver.skills.l2skills.L2SkillMount;
import ru.catssoftware.gameserver.skills.l2skills.L2SkillSummon;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Broadcast;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.lang.L2System;
import ru.catssoftware.lang.RunnableImpl;
import ru.catssoftware.tools.geometry.Point3D;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.SingletonList;
import ru.catssoftware.util.SingletonSet;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import static ru.catssoftware.gameserver.ai.CtrlIntention.*;


public abstract class L2Character extends L2Object implements IEffector
{
	public final static Logger			_log								= Logger.getLogger(L2Character.class.getName());
	public static final double HEADINGS_IN_PI = 10430.378350470452724949566316381;
	
	// Data Field
	private List<L2Character>		_attackByList;
	private L2Character				_attackingChar;
	private volatile boolean		_isCastingNow 						= false;
	private volatile boolean		_isCastingSimultaneouslyNow			= false;
	private L2Skill					_lastSimultaneousSkillCast;
	private boolean					_block_buffs						= false;
	private boolean					_isAfraid							= false;											// Flee in a random direction
	private boolean					_isConfused							= false;											// Attack anyone randomly
	private boolean					_isFakeDeath						= false;											// Fake death
	private boolean					_isFallsdown						= false;											// Falls down [L2J_JP_ADD]
	private boolean					_isMuted							= false;											// Cannot use magic
	private boolean					_isPhysicalMuted					= false;											// Cannot use physical attack
	private boolean					_isPhysicalAttackMuted				= false;											// Cannot use attack
	private boolean					_isDead								= false;
	private boolean					_isImmobilized						= false;
	private boolean					_isOverloaded						= false;											// the char is carrying too much
	private boolean					_isParalyzed						= false;											// cannot do anything
	private boolean					_isPetrified						= false;											// cannot receive dmg from hits.
	private boolean					_isPendingRevive					= false;
	private boolean					_isRooted							= false;											// Cannot move until root timed out
	private boolean					_isRunning							= true;
	private boolean					_isImmobileUntilAttacked			= false;
	private boolean					_isSleeping							= false;											// Cannot move/attack until sleep
	private boolean 				_lastHitIsCritical;	

	private CharactionListeners _listners = new CharactionListeners(this);
	// Character PremiumService field

	// timed out or monster is attacked
	private boolean					_isBlessedByNoblesse				= false;
	private boolean					_isLuckByNoblesse					= false;
	private boolean					_isBetrayed							= false;
	private boolean					_isStunned							= false;											// Cannot move/attack until stun
	// timed out
	protected boolean				_isTeleporting						= false;
	protected boolean				_isInvul							= false;
	protected boolean				_isDisarmed							= false;
	protected boolean				_isMarked							= false;
	private int						_lastHealAmount						= 0;
	private int[]					lastPosition						= { 0, 0, 0 };
	protected CharStat				_stat;
	protected CharStatus			_status;
	private L2CharTemplate			_template;																				// The link on the L2CharTemplate
	protected boolean				_showSummonAnimation				= false;
	protected String					_title;
	private boolean					_champion							= false;
	private double					_hpUpdateIncCheck					= .0;
	private double					_hpUpdateDecCheck					= .0;
	private double					_hpUpdateInterval					= .0;
	private int						_healLimit							= 0;
	public	L2Effect				_itemActiveSkill					= null;
	

	/** Table of Calculators containing all used calculator */
	private Calculator[]			_calculators;

	/** FastMap(Integer, L2Skill) containing all skills of the L2Character */
	protected Map<Integer, L2Skill>	_skills;
	protected ChanceSkillList		_chanceSkills;
	/** Current force buff this caster is casting to a target */
	protected FusionSkill			_fusionSkill;
	protected byte					_zoneValidateCounter				= 4;
	private boolean					_isRaid								= false;
	private boolean					_isBoss								= false;
	private boolean					_isGrandBoss						= false;
	private byte[]					_currentZones						= new byte[25];
	protected CharKnownList			_knownList;
	
	private int						_team								= 0;
	// 
	protected GameEvent				_event;
	private	List<L2Zone>			_currentZoneList	= new FastList<L2Zone>();
	
	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);
		getKnownList();

		// Set its template to the new L2Character
		_template = template;

		if (template != null && isNpc())
		{
			// Copy the Standard Calcultors of the L2NPCInstance in _calculators
			if (this instanceof L2DoorInstance)
				_calculators = Formulas.getStdDoorCalculators();
			else
				_calculators = NPC_STD_CALCULATOR;

			// Copy the skills of the L2NPCInstance from its template to the L2Character Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a L2NPCInstance, affects others L2NPCInstance of the same type too.
				_skills = ((L2NpcTemplate) template).getSkills();
				if (_skills != null)
				{
					for (Map.Entry<Integer, L2Skill> skill : _skills.entrySet())
						addStatFuncs(skill.getValue().getStatFuncs(null, this));
				}
		}
		else
		{
			// Initialize the FastMap _skills to null
			_skills = new FastMap<Integer, L2Skill>().setShared(true);
			// If L2Character is a L2PcInstance or a L2Summon, create the basic calculator set
			_calculators = new Calculator[Stats.NUM_STATS];
			Formulas.addFuncsToNewCharacter(this);
		}

		if (!(this instanceof L2PlayableInstance) && !(this instanceof L2Attackable) && !(this instanceof L2ControlTowerInstance) && !(this instanceof L2DoorInstance)  && !(this instanceof L2SiegeFlagInstance) && !(this instanceof L2Decoy) && !(this instanceof L2EffectPointInstance) && !(this instanceof L2FolkInstance))
			setIsInvul(true);
	}

	public boolean isInsideZone(byte zone)
	{
		return (zone == L2Zone.FLAG_PVP) ? (_currentZones[L2Zone.FLAG_PVP] > 0 && _currentZones[L2Zone.FLAG_PEACE] == 0) : (_currentZones[zone] > 0);
	}

	
	public List<L2Zone> getZones() {
		return _currentZoneList;
	}
	public L2Zone getZone(String type) {
		for(L2Zone z : _currentZoneList) 
			if(z.getClass().getSimpleName().equalsIgnoreCase("L2"+type+"Zone"))
				return z;
		return null;
	}
	public void setInsideZone(L2Zone zone, byte zoneType,  boolean state)
	{
		if (state) {
			_currentZones[zoneType]++;
			if(!_currentZoneList.contains(zone))
				_currentZoneList.add(zone);
		}
		else {
			if (_currentZones[zoneType] > 0)
				_currentZones[zoneType]--;
			_currentZoneList.remove(zone);
		}
	}


	public Inventory getInventory()
	{
		return null;
	}

	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		return true;
	}

	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		return true;
	}

	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0;
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}

	public void onDecay()
	{
		GameExtensionManager.getInstance().handleAction(this, Action.CHAR_DESPAWN);
		L2WorldRegion reg = getWorldRegion();
		decayMe();
		if (reg != null)
			reg.removeFromZones(this);
	}

	@Override
	public void onSpawn()
	{
		GameExtensionManager.getInstance().handleAction(this, Action.CHAR_SPAWN);
		super.onSpawn();
		revalidateZone(true);
	}

	public void onTeleported()
	{
		if (!isTeleporting())
			return;

		if (isSummon())
			getPlayer().sendPacket(new TeleportToLocation(this, getPosition().getX(), getPosition().getY(), getPosition().getZ()));

		setIsTeleporting(false);
		spawnMe();

		if (_isPendingRevive)
			doRevive();
	}

	public void addAttackerToAttackByList(L2Character player)
	{
		if (player == null || player == this || getAttackByList() == null || getAttackByList().contains(player))
			return;
		getAttackByList().add(player);
	}

	public final void broadcastPacket(L2GameServerPacket mov)
	{
		Broadcast.toSelfAndKnownPlayers(this, mov);
	}

	public final void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		Broadcast.toSelfAndKnownPlayersInRadius(this, mov, radiusInKnownlist);
	}

	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getStatus().getCurrentHp();

		if (currentHp <= 1.0 || getMaxHp() < barPixels)
			return true;

		if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if (currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			return true;
		}
		return false;
	}

	public final void broadcastStatusUpdate()
	{
		broadcastStatusUpdateImpl();
	}

	public void broadcastStatusUpdateImpl()
	{

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int)getStatus().getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int)getStatus().getCurrentMp());
		broadcastPacket(su);
	}
	public void setCriticalDmg(boolean par)
	{
		_lastHitIsCritical = par;
	}
	public boolean getLastCriticalDmg()
	{
		return _lastHitIsCritical;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
	}

	public void sendPacket(SystemMessageId sm)
	{
	}

	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		// Restrict teleport during restart/shutdown
		if (isPlayer() && Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TELEPORT && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			sendMessage(Message.getMessage((L2PcInstance)this, Message.MessageId.MSG_ACTION_NOT_ALLOWED_DURING_SHUTDOWN));
			return;
		}

		abortCast();
		abortAttack();
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		setTarget(this);
		disableAllSkills();
		abortAttack();
		abortCast();
		isFalling(false, 0);
		setIsTeleporting(true);

		if (Config.RESPAWN_RANDOM_ENABLED && allowRandomOffset)
		{
			x += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
			y += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
		}
		z += 5;

		decayMe();
		broadcastPacket(new TeleportToLocation(this, x, y, z));
		getPosition().setWorldPosition(x, y, z);
		isFalling(false, 0);

		if (!(isPlayer()))
			onTeleported();

		enableAllSkills();
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		getKnownList().updateKnownObjects();

		_listners.onTeleport();
	}

	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, true);
	}

	public void teleToLocation(Location loc)
	{
		teleToLocation(loc, false);
	}

	public void teleToLocation(Location loc, boolean allowRandomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), allowRandomOffset);
	}

	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionManager.getInstance().getTeleToLocation(this, teleportWhere), true);
	}

	/** ************************************-+ Fall Damage +-************************************** */

	/**
	 * @author Darki699 Calculates if a L2Character is falling or not. If the character falls, it returns the fall height.
	 * @param falling: if false no checks are made, but last position is set to the current one
	 * @param fallHeight: an integer value of the fall already calculated before.
	 * @return A positive integer of the fall height, if not falling returns -1
	 */
	public int isFalling(boolean falling, int fallHeight)
	{

		if (isFallsdown() && fallHeight == 0) // Avoid double checks -> let him fall only 1 time =P
			return -1;

		if (!falling || (lastPosition[0] == 0 && lastPosition[1] == 0 && lastPosition[2] == 0))
		{
			lastPosition[0] = getPosition().getX();
			lastPosition[1] = getPosition().getY();
			lastPosition[2] = getPosition().getZ();
			setIsFallsdown(false);
			return -1;
		}

		int moveChangeX = Math.abs(lastPosition[0] - getPosition().getX()), moveChangeY = Math.abs(lastPosition[1] - getPosition().getY()),
		// Z has a Positive value ONLY if the L2Character is moving down!
		moveChangeZ = Math.max(lastPosition[2] - getPosition().getZ(), lastPosition[2] - getZ());

		// Add acumulated damage to this fall, calling this function at a short delay while the fall is in progress
		if (moveChangeZ > fallSafeHeight() && moveChangeY < moveChangeZ && moveChangeX < moveChangeZ && !isFlying())
		{

			setIsFallsdown(true);
			// Calculate the acumulated fall height for a total fall calculation
			fallHeight += moveChangeZ;

			// set the last position to the current one for the next future calculation
			lastPosition[0] = getPosition().getX();
			lastPosition[1] = getPosition().getY();
			lastPosition[2] = getPosition().getZ();
			getPosition().setXYZ(lastPosition[0], lastPosition[1], lastPosition[2]);

			// Call this function for further checks in the short future (next time we either keep falling, or finalize the fall)
			// This "next time" check is a rough estimate on how much time is needed to calculate the next check, and it is based on the current fall height.
			CheckFalling cf = new CheckFalling(fallHeight);
			Future<?> task = ThreadPoolManager.getInstance().scheduleGeneral(cf, Math.min(1200, moveChangeZ));
			cf.setTask(task);

			// Value returned but not currently used. Maybe useful for future features.
			return fallHeight;
		}

		// Stopped falling or is not falling at all.
		lastPosition[0] = getPosition().getX();
		lastPosition[1] = getPosition().getY();
		lastPosition[2] = getPosition().getZ();
		getPosition().setXYZ(lastPosition[0], lastPosition[1], lastPosition[2]);

		if (fallHeight > fallSafeHeight())
		{
			doFallDamage(fallHeight);
			return fallHeight;
		}

		return -1;
	}

	/**
	 * <font color="ff0000"><b>Needs to be completed!</b></font> Add to safeFallHeight the buff resist values which increase the fall resistance.
	 *
	 * @author Darki699
	 * @return integer safeFallHeight is the value from which above it this L2Character suffers a fall damage.
	 */
	private int fallSafeHeight()
	{

		int safeFallHeight = Config.ALT_MINIMUM_FALL_HEIGHT;

		try
		{
			if (isPlayer())
				safeFallHeight = ((L2PcInstance) this).getTemplate().getBaseFallSafeHeight(((L2PcInstance) this).getAppearance().getSex());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return safeFallHeight;
	}

	private int getFallDamage(int fallHeight)
	{
		int damage = (fallHeight - fallSafeHeight()) * 2; // Needs verification for actual damage
		damage = (int) (damage / getStat().calcStat(Stats.FALL_VULN, 1, this, null));

		if (damage >= getStatus().getCurrentHp())
		{
			damage = (int) (getStatus().getCurrentHp() - 1);
		}

		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
		disableAllSkills();

		ThreadPoolManager.getInstance().scheduleGeneral(new RunnableImpl()
		{
			public void runImpl()
			{
				L2Character.this.enableAllSkills();
				broadcastPacket(new ChangeWaitType(L2Character.this, ChangeWaitType.WT_STOP_FAKEDEATH));
				setIsFallsdown(false);

				// For some reason this is needed since the client side changes back to last airborn position after 1 second
				lastPosition[0] = getPosition().getX();
				lastPosition[1] = getPosition().getY();
				lastPosition[2] = getPosition().getZ();
			}
		}, 1100);
		return damage;
	}

	/**
	 * Receives a integer fallHeight and finalizes the damage effect from the fall.
	 * @author Darki699
	 */
	private void doFallDamage(int fallHeight)
	{
		isFalling(false, 0);

		if (isInvul() || (isPlayer() && isInFunEvent()))
		{
			setIsFallsdown(false);
			return;
		}

		int damage = getFallDamage(fallHeight);

		if (damage < 1)
			return;

		if (isPlayer())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.FALL_DAMAGE_S1);
			sm.addNumber(damage);
			sendPacket(sm);
		}

		getStatus().reduceHp(damage, this);
		getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
	}

	/**
	 * @author Darki699 Once a character is falling, we call this to run in order to see when he is not falling down any more. Constructor receives the int
	 *         fallHeight already calculated, and function isFalling(boolean,int) will be called again to terminate the fall and calculate the damage.
	 */
	public class CheckFalling extends RunnableImpl
	{
		private int			_fallHeight;
		private Future<?>	_task;

		public CheckFalling(int fallHeight)
		{
			_fallHeight = fallHeight;
		}

		public void setTask(Future<?> task)
		{
			_task = task;
		}

		public void runImpl()
		{
			if (_task != null)
			{
				_task.cancel(true);
				_task = null;
			}

			try
			{
				isFalling(true, _fallHeight);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Приватный метод.
	 * Проверка начала атаки
	 * Оптимизирован 26/01/10
	 **/
	protected void doAttack(L2Character target)
	{
		if (isAlikeDead() || target == null || (this instanceof L2NpcInstance && target.isAlikeDead()) || (isPlayer() && target.isDead() && !target.isFakeDeath()))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (isAttackingDisabled())
			return;

		if (!(target instanceof L2DoorInstance) && !this.canSee(target))
		{
			sendPacket(SystemMessageId.CANT_SEE_TARGET);
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (!canAttack(target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

/*		if(GameExtensionManager.getInstance().handleAction(this, Action.CHAR_ATTACK, target)!=null) {
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
			*/
		if(_event!=null  && target.getGameEvent() == _event && _event.isRunning() && _event.canAttack(this, target))
		{
			
		}
		else
		if (isPlayer())
		{
			if (((L2PcInstance)this).isMounted() && ((L2PcInstance)this).getMountNpcId() == 12621)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (((L2PcInstance) this).inObserverMode())
			{
				sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (target.isPlayer())
			{
				if (((L2PcInstance) target).isCursedWeaponEquipped() && getLevel() <= 20)
				{
					sendMessage(Message.getMessage((L2PcInstance)this, Message.MessageId.MSG_CW_YOUR_LVL_LOW));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (((L2PcInstance) this).isCursedWeaponEquipped() && target.getLevel() <= 20)
				{
					sendMessage(Message.getMessage((L2PcInstance)this, Message.MessageId.MSG_CW_TARGET_LVL_LOW));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (target.isInsidePeaceZone((L2PcInstance) this))
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (Config.ALLOW_OFFLINE_TRADE_PROTECTION && ((L2PcInstance)target).isOfflineTrade())
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (isInsidePeaceZone(this, target))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		L2ItemInstance weaponInst = getActiveWeaponInstance();
		if (weaponInst != null && weaponInst.getItemId() == Config.FORTSIEGE_COMBAT_FLAG_ID)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Weapon weaponItem = getActiveWeaponItem();
		if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD))
		{
			sendPacket(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (weaponItem != null)
		{
			// Проверка стрел и маны при использовании лука
			if (weaponItem.getItemType() == L2WeaponType.BOW)
			{
				if (isPlayer())
				{
					if (target.isInsidePeaceZone((L2PcInstance) this))
					{
						getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}

					if (_disableBowAttackEndTime <= GameTimeController.getGameTicks())
					{
						int saMpConsume = (int) getStat().calcStat(Stats.MP_CONSUME, 0, null, null);
						int mpConsume = saMpConsume == 0 ? weaponItem.getMpConsume() : saMpConsume;

						if (getStatus().getCurrentMp() < mpConsume)
						{
							ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 1000, this instanceof L2PlayableInstance);
							sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						getStatus().reduceMp(mpConsume);
						_disableBowAttackEndTime = 5 * GameTimeController.TICKS_PER_SECOND + GameTimeController.getGameTicks();
					}
					else
					{
						ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 1000, this instanceof L2PlayableInstance);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}

					if (!checkAndEquipArrows())
					{
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						sendPacket(ActionFailed.STATIC_PACKET);
						sendPacket(SystemMessageId.NOT_ENOUGH_ARROWS);
						return;
					}
				}
				else if (this instanceof L2NpcInstance)
				{
					if (_disableBowAttackEndTime > GameTimeController.getGameTicks())
						return;
				}
			}
			// Проверка стрел и маны при использовании арбалета
		}

		target.getKnownList().addKnownObject(this);

		if (Config.ALT_GAME_TIREDNESS)
			getStatus().setCurrentCp(getStatus().getCurrentCp() - 10);

		boolean wasSSCharged;
		if (this instanceof L2NpcInstance) try {
			wasSSCharged = ((L2NpcInstance) this).rechargeAutoSoulShot(true, false);
		} catch(Exception e) { wasSSCharged = false;}
		else if ((this instanceof L2Summon && !(this instanceof L2PetInstance)) || this instanceof L2BabyPetInstance)
			wasSSCharged = (((L2Summon) this).getChargedSoulShot() != L2ItemInstance.CHARGED_NONE);
		else
			wasSSCharged = (weaponInst != null && weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE);

		int timeAtk = calculateTimeBetweenAttacks(target, weaponItem);
		int timeToHit = timeAtk / 2;
		int reuse = calculateReuseTime(target, weaponItem);

		_attackEndTime = L2System.milliTime() + timeAtk;

		int ssGrade = 0;

		if (weaponItem != null)
		{
			ssGrade = weaponItem.getCrystalType();
			if (ssGrade == 6)
				ssGrade = 5;
		}

		Attack attack = new Attack(this, target, wasSSCharged, ssGrade);
		setAttackingBodypart();
		setHeading(Util.calculateHeadingFrom(this, target));

		boolean hitted;
		if (weaponItem == null)
			hitted = doAttackHitSimple(attack, target, timeToHit);
		else if (weaponItem.getItemType() == L2WeaponType.BOW)
			hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
		else if (weaponItem.getItemType() == L2WeaponType.POLE)
			hitted = doAttackHitByPole(attack, target, timeToHit);
		else if (isUsingDualWeapon())
			hitted = doAttackHitByDual(attack, target, timeToHit);
		else
			hitted = doAttackHitSimple(attack, target, timeToHit);

		L2PcInstance player = getPlayer();

		if (player != null && player.getPet() != target)
			player.updatePvPStatus(target);
		if (!hitted)
			abortAttack();
		else
		{
			if (_chanceSkills!=null)
				_chanceSkills.onAttack(target);
				
			if ((this instanceof L2Summon && !(this instanceof L2PetInstance)) || this instanceof L2BabyPetInstance)
				((L2Summon) this).setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
			else if (weaponInst != null)
				weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,false);

			if (player != null)
			{
				if (player.isCursedWeaponEquipped())
				{
					if (!target.isInvul())
						target.getStatus().setCurrentCp(0);
				}
				else if (player.isHero())
				{
					if (target.isPlayer() && ((L2PcInstance) target).isCursedWeaponEquipped())
						target.getStatus().setCurrentCp(0); // If Zariche is hitted by a Hero, Cp is reduced to 0
				}
			}
		}

		if (attack.hasHits())
			broadcastPacket(attack);

		ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), timeAtk + reuse, this instanceof L2PlayableInstance);
	}


	private boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;

		boolean miss1 = Formulas.calcHitMiss(this, target);
		if (miss1)
			sendPacket(SystemMessageId.MISSED_TARGET); // msg miss the target

		reduceArrowCount(false);
		_move = null;

		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(this, target, getStat().getCriticalHit(target, null));

			if (crit1 == true)
				if (target instanceof L2Attackable)
					target.setCriticalDmg(true);
				else
					target.setCriticalDmg(false);

			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);
		}

		// Check if the L2Character is a L2PcInstance
		if (isPlayer())
		{
			sendPacket(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW);
			SetupGauge sg = new SetupGauge(SetupGauge.RED, sAtk + reuse);
			sendPacket(sg);
		}

		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk, this instanceof L2PlayableInstance);
		if (!miss1)
			ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this), sAtk+10);
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = (sAtk + reuse) / GameTimeController.MILLIS_IN_TICK + GameTimeController.getGameTicks() - 1;

		if (target instanceof L2NpcInstance)
		{
			if (isKanabion(((L2NpcInstance)target).getTemplate().getIdTemplate()))
			{
				if (damage1 > target.getMaxHp() / 100 * 40)
					target.startAbnormalEffect(AbnormalEffect.GROW);
			}
		}
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		return !miss1;
	}

	private class AutoSS extends RunnableImpl
	{
		private L2Character _character;
		private final int[]	SKILL_IDS	= { 2039, 2150, 2151, 2152, 2153, 2154, 2154 };

		public AutoSS(L2Character Character)
		{
			_character = Character;
		}

		public void runImpl()
		{
			if (_character.isPlayer())
			{
				if (((L2PcInstance)_character).getAutoSoulShot().size() > 0)
				{
					L2PcInstance pl=((L2PcInstance)_character);
					L2Weapon weaponItem = _character.getActiveWeaponItem();
					if (weaponItem!=null && weaponItem.getSoulShotCount() > 0)
					{
						int weaponGrade = weaponItem.getCrystalType();
						if ((weaponGrade == L2Item.CRYSTAL_NONE && !pl.getAutoSoulShot().containsKey(5789) && !pl.getAutoSoulShot().containsKey(1853)) || (weaponGrade == L2Item.CRYSTAL_D && !pl.getAutoSoulShot().containsKey(1463)) || (weaponGrade == L2Item.CRYSTAL_C && !pl.getAutoSoulShot().containsKey(1464)) || (weaponGrade == L2Item.CRYSTAL_B && !pl.getAutoSoulShot().containsKey(1465)) || (weaponGrade == L2Item.CRYSTAL_A && !pl.getAutoSoulShot().containsKey(1466)) || (weaponGrade == L2Item.CRYSTAL_S && !pl.getAutoSoulShot().containsKey(1467)))
							return;

						_character.sendPacket(SystemMessageId.ENABLED_SOULSHOT);
						Broadcast.toSelfAndKnownPlayersInRadius(_character, new MagicSkillUse(_character, _character, SKILL_IDS[weaponGrade], 1, 0, 0, false), 360000);
					}
				}
			}
			if (_character instanceof L2Summon)
			{
				if (((L2Summon)_character).getOwner().getAutoSoulShot().size() > 0)
				{
					L2ItemInstance weaponInst = _character.getActiveWeaponInstance();
					if (weaponInst != null)
					{
						if (((L2Summon) _character).getOwner().getAutoSoulShot().containsKey(6645))
						{
							((L2Summon) _character).getOwner().sendPacket(SystemMessageId.PET_USE_THE_POWER_OF_SPIRIT);
							Broadcast.toSelfAndKnownPlayersInRadius(((L2Summon) _character).getOwner(), new MagicSkillUse(_character, _character, 2033, 1, 0, 0, false), 360000);
						}
					}
					else if (!(_character instanceof L2PetInstance) || _character instanceof L2BabyPetInstance)
					{
						if (((L2Summon) _character).getOwner().getAutoSoulShot().containsKey(6645))
						{
							((L2Summon) _character).getOwner().sendPacket(SystemMessageId.PET_USE_THE_POWER_OF_SPIRIT);
							Broadcast.toSelfAndKnownPlayersInRadius(((L2Summon) _character).getOwner(), new MagicSkillUse(_character, _character, 2033, 1, 0, 0, false), 360000);
						}
					}
				}
			}
		}
	}

	private boolean isKanabion(int mobId)
	{
		if ((mobId==22453)||(mobId==22454) ||(mobId==22456)||(mobId==22457) ||(mobId==22459)||(mobId==22460) ||(mobId==22462)||(mobId==22453) ||(mobId==22465)||(mobId==22466) ||(mobId==22468)||(mobId==22469) ||(mobId==22471)||(mobId==22472) ||(mobId==22474)||(mobId==22475) ||(mobId==22477)||(mobId==22478) ||(mobId==22480)||(mobId==22481) ||(mobId==22483)||(mobId==22484))
			return true;

		return false;
	}


	private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;

		boolean miss1 = Formulas.calcHitMiss(this, target);
		boolean miss2 = Formulas.calcHitMiss(this, target);
		if (miss1)
			sendPacket(SystemMessageId.MISSED_TARGET); // msg miss
		if (miss2)
			sendPacket(SystemMessageId.MISSED_TARGET); // msg miss

		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(this, target, getStat().getCriticalHit(target, null));

			if (crit1 == true)
				if (target instanceof L2Attackable)
					target.setCriticalDmg(true);
				else
					target.setCriticalDmg(false);

			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, true, attack.soulshot);
			damage1 /= 2;
		}

		// Check if hit 2 isn't missed
		if (!miss2)
		{
			shld2 = Formulas.calcShldUse(this, target);
			crit2 = Formulas.calcCrit(this, target, getStat().getCriticalHit(target, null));
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, true, attack.soulshot);
			damage2 /= 2;
		}

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk / 2, this instanceof L2PlayableInstance);
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2), sAtk, this instanceof L2PlayableInstance);
		if ((!miss1)||(!miss2))
			ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this), sAtk+10);

		if (target instanceof L2NpcInstance)
		{
			if (isKanabion(((L2NpcInstance)target).getTemplate().getIdTemplate()))
			{
				if ((damage1+damage2)>target.getMaxHp()/100*40)
					target.startAbnormalEffect(AbnormalEffect.GROW);
			}
		}

		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1),
				attack.createHit(target, damage2, miss2, crit2, shld2));
		return (!miss1 || !miss2);
	}

	private boolean doAttackHitByPole(Attack attack, L2Character target, int sAtk)
	{
		double angleChar;
		int maxRadius = getPhysicalAttackRange();
		int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);

		// Get char's heading degree
		angleChar = Util.convertHeadingToDegree(getHeading());
		int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 3, null, null) - 1;
		int attackcount = 0;

		if (angleChar <= 0)
			angleChar += 360;

		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk);
		double attackpercent = 85;
		L2Character temp;
		for (L2Object obj : getKnownList().getKnownObjects().values())
		{
			if (obj == target)
				continue; // do not hit twice

			// Check if the L2Object is a L2Character
			if (obj instanceof L2Character)
			{
				if (obj instanceof L2PetInstance && isPlayer() && ((L2PetInstance) obj).getOwner() == this)
					continue;

				if (!Util.checkIfInRange(maxRadius, this, obj, false))
					continue;

				if (!this.canSee(obj))
					continue;

				if (Math.abs(obj.getZ() - getZ()) > 650)
					continue;

				if (!isFacing(obj, maxAngleDiff))
					continue;

				temp = (L2Character) obj;

				if (!temp.isAlikeDead())
				{
					attackcount += 1;
					if (attackcount <= attackRandomCountMax)
					{
						if (temp == getAI().getAttackTarget() || temp.isAutoAttackable(this))
						{
							hitted |= doAttackHitSimple(attack, temp, attackpercent, sAtk);
							attackpercent /= 1.15;
						}
					}
				}
			}
		}
		return hitted;
	}

	private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk);
	}

	private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;

		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		if (miss1)
			sendPacket(SystemMessageId.MISSED_TARGET); // msg miss

		// Check if hit isn't missed
		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(this, target, getStat().getCriticalHit(target, null));

			if (crit1 == true)
				if (target instanceof L2Attackable)
					target.setCriticalDmg(true);
				else
					target.setCriticalDmg(false);

			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);

			if (attackpercent != 100)
				damage1 = (int) (damage1 * attackpercent / 100);
		}

		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk, this instanceof L2PlayableInstance);
		if (!miss1)
			ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this), sAtk+10);

		if (target instanceof L2NpcInstance)
		{
			if (isKanabion(((L2NpcInstance)target).getTemplate().getIdTemplate()))
			{
				if (damage1 > target.getMaxHp() / 100 * 40)
					target.startAbnormalEffect(AbnormalEffect.GROW);
			}
		}
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		return !miss1;
	}

	public void doCast(L2Skill skill)
	{
		if(GameExtensionManager.getInstance().handleAction(this, Action.CHAR_CAST, skill)!=null) 
			return;
		beginCast(skill, false);
	}

	public void doSimultaneousCast(L2Skill skill)
	{
		beginCast(skill, true);
	}

	private void beginCast(L2Skill skill, boolean simultaneously)
	{
		L2Character target = null;

		if(isAlikeDead() && skill.getTargetType()!=SkillTargetType.TARGET_SELF)
			return;

		if(isTeleporting())
			return;

		if(this instanceof L2Boss && getCurrentMp() < Config.RAID_MIN_MP_TO_CAST)
			return;

		if(getGameEvent()!=null && !getGameEvent().canUseSkill(this, skill))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			sendPacket(ActionFailed.STATIC_PACKET);
			getAI().setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		if (skill.useSoulShot())
		{
			if (this instanceof L2NpcInstance)
				((L2NpcInstance) this).rechargeAutoSoulShot(true, false);
		}

		L2Character[] targets = skill.getTargetList(this);

		if(skill.isPotion())
			target =  this;
		else
		switch (skill.getTargetType())
		{
		case TARGET_AURA:
		case TARGET_FRONT_AURA:
		case TARGET_BEHIND_AURA:
		case TARGET_GROUND:
		{
			target = this;
			break;
		}
		default:
		{
			if (targets == null || targets.length == 0)
			{
				if (simultaneously)
					setIsCastingSimultaneouslyNow(false);
				else
					setIsCastingNow(false);

				if (isPlayer())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					getAI().setIntention(AI_INTENTION_ACTIVE);
				}
				return;
			}

			switch (skill.getSkillType())
			{
			case BUFF:
			case HEAL:
			case COMBATPOINTHEAL:
			case MANAHEAL:
			case REFLECT:
				target = targets[0];
				break;
			default:
			{
				switch (skill.getTargetType())
				{
				case TARGET_SELF:
				case TARGET_PET:
				case TARGET_SUMMON:
				case TARGET_PARTY:
				case TARGET_CLAN:
				case TARGET_CORPSE_CLAN:
				case TARGET_ALLY:
				case TARGET_ENEMY_ALLY:
					target = targets[0];
					break;
				case TARGET_OWNER_PET:
					if (this instanceof L2PetInstance)
						target = ((L2PetInstance) this).getOwner();
					break;
				default:
				{
					target = (L2Character) getTarget();
					break;
				}
				}
			}
			}
		}
		}

		if (target == null)
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);

			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		if (!checkDoCastConditions(skill,target))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			if (isPlayer())
				getAI().setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		setAttackingChar(this);

		int magicId = skill.getId();
		int displayId = skill.getDisplayId();
		int level = skill.getLevel();
		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();
		int skillInterruptTime = skill.getSkillInterruptTime();
		boolean effectWhileCasting = skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME;

		if (level < 1)
			level = 1;

		if (!effectWhileCasting)
		{
			hitTime = Formulas.calcAtkSpd(this, skill, hitTime);
			if (coolTime > 0)
				coolTime = Formulas.calcAtkSpd(this, skill, coolTime);
		}

		// Calculate the Interrupt Time of the skill (base + modifier) if the skill is a spell else 0
		if (skill.isMagic())
			skillInterruptTime = Formulas.calcAtkSpd(this, skill, skillInterruptTime);
		else
			skillInterruptTime = 0;

		// Calculate altered Cast Speed due to BSpS/SpS
		L2ItemInstance weaponInst = getActiveWeaponInstance();
		if (weaponInst != null && skill.isMagic() && !effectWhileCasting && skill.getTargetType() != SkillTargetType.TARGET_SELF)
		{
			if ((weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) || (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT))
			{
				// Only takes 70% of the time to cast a BSpS/SpS cast
				hitTime = (int) (0.70 * hitTime);
				coolTime = (int) (0.70 * coolTime);
				skillInterruptTime = (int) (0.70 * skillInterruptTime);

				// Because the following are magic skills that do not actively 'eat' BSpS/SpS,
				// I must 'eat' them here so players don't take advantage of infinite speed increase
				switch (skill.getSkillType())
				{
				case BUFF:
				case MANAHEAL:
				case RESURRECT:
				case RECALL:
				case DOT:
					weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
					break;
				}
			}
		}
		else if ((this instanceof L2Summon || this instanceof L2BabyPetInstance) &&(((L2Summon)this).getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT || ((L2Summon)this).getChargedSpiritShot()==L2ItemInstance.CHARGED_SPIRITSHOT))
		{
			hitTime = (int) (0.70 * hitTime);
			coolTime = (int) (0.70 * coolTime);
			skillInterruptTime = (int) (0.70 * skillInterruptTime);
			switch (skill.getSkillType())
			{
			case BUFF:
			case MANAHEAL:
			case RESURRECT:
			case RECALL:
			case DOT:
				((L2Summon)this).setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				break;
			}
		}
		else if (this instanceof L2NpcInstance && skill.useSpiritShot() && !effectWhileCasting)
		{
			if (((L2NpcInstance) this).rechargeAutoSoulShot(false, true))
			{
				hitTime = (int) (0.70 * hitTime);
				coolTime = (int) (0.70 * coolTime);
				skillInterruptTime = (int) (0.70 * skillInterruptTime);
			}
		}

		if (skill.isStaticHitTime())
		{
			hitTime = skill.getHitTime();
			coolTime = skill.getCoolTime();
		}

		if (isCastingSimultaneouslyNow() && simultaneously)
		{
			ThreadPoolManager.getInstance().scheduleAi(new UsePotionTask(this, skill), 100, this instanceof L2PlayableInstance);
			return;
		}

		if (simultaneously)
			setIsCastingSimultaneouslyNow(true);
		else
			setIsCastingNow(true);

		if (!simultaneously)
			_castInterruptTime = GameTimeController.getGameTicks() + skillInterruptTime / GameTimeController.MILLIS_IN_TICK;
		else
			setLastSimultaneousSkillCast(skill);

		int reuseDelay;
		if (skill.isStaticReuse() && (Config.USE_STATIC_REUSE ||
				(getPlayer()!=null && Config.USE_OLY_STATIC_REUSE && getPlayer().getOlympiadGameId()!=-1)) )
			reuseDelay = skill.getReuseDelay();
		else
		{
			if (skill.isMagic())
				reuseDelay = (int) (skill.getReuseDelay() * getStat().getMReuseRate(skill));
			else
				reuseDelay = (int) (skill.getReuseDelay() * getStat().getPReuseRate(skill));
			if (reuseDelay != 0)
				reuseDelay *= 333.0 / (skill.isMagic() ? getMAtkSpd() : getPAtkSpd()); 
		}

		boolean skillMastery = Formulas.calcSkillMastery(this, skill);
		if (skillMastery)
		{
			reuseDelay = 0;
			if (getPlayer() != null)
				getPlayer().sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
		}

		if (reuseDelay > 30000)
			addTimeStamp(skill.getId(), reuseDelay);

		int initmpcons = getStat().getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			StatusUpdate su = new StatusUpdate(getObjectId());
			if (skill.isDance() || skill.isSong())
				getStatus().reduceMp(calcStat(Stats.DANCE_CONSUME_RATE, initmpcons, null, null));
			else if (skill.isMagic())
				getStatus().reduceMp(calcStat(Stats.MAGIC_CONSUME_RATE, initmpcons, null, null));
			else
				getStatus().reduceMp(calcStat(Stats.PHYSICAL_CONSUME_RATE, initmpcons, null, null));
			su.addAttribute(StatusUpdate.CUR_MP, (int) getStatus().getCurrentMp());
			sendPacket(su);
		}

		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (reuseDelay > 10)
			disableSkill(skill.getId(), reuseDelay);

		// Make sure that char is facing selected target
		if (target != this)
			setHeading(Util.calculateHeadingFrom(this, target));

		// For force buff skills, start the effect as long as the player is casting.
		if (effectWhileCasting)
		{
			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
			if (skill.getItemConsume() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false))
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					if (simultaneously)
						setIsCastingSimultaneouslyNow(false);
					else
						setIsCastingNow(false);
					if (isPlayer())
						getAI().setIntention(AI_INTENTION_ACTIVE);
					return;
				}
			}

			if (isPlayer())
			{
				L2PcInstance player = getPlayer();
				// Consume Charges if necessary ... L2SkillChargeDmg does the consume by itself.
				if (skill.getNeededCharges() > 0 && !(skill instanceof L2SkillChargeDmg) && skill.getConsumeCharges())
					player.decreaseCharges(skill.getNeededCharges());
			}

			if (skill.getSkillType() == L2SkillType.FUSION)
				startFusionSkill(target, skill);
			else
				callSkill(skill, targets);
		}

		// To prevent area skill animation/packet arrive too late
		broadcastPacket(new MagicSkillLaunched(this, magicId, level, skill.isPositive(), targets));

		// Send a Server->Client packet MagicSkillUse with target, displayId, level, skillTime, reuseDelay
		// to the L2Character AND to all L2PcInstance in the _knownPlayers of the L2Character
		broadcastPacket(new MagicSkillUse(this, target, displayId, level, hitTime, reuseDelay, skill.isPositive()));

		if (isPlayer())
		{
			long protTime = hitTime + coolTime;

			if (reuseDelay < protTime)
				protTime /= 2;

			((L2PcInstance) this).setSkillQueueProtectionTime(System.currentTimeMillis() + protTime);
		}

		// Send a system message USE_S1 to the L2Character
		if (isPlayer() && magicId != 1312)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.USE_S1);
			sm.addSkillName(skill);
			sendPacket(sm);
		}

		switch (skill.getTargetType())
		{
		case TARGET_AURA:
		case TARGET_FRONT_AURA:
		case TARGET_BEHIND_AURA:
		case TARGET_GROUND:
		{
			if (targets.length == 0)
			{
				// now cancels both, simultaneous and normal
				//getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
				//return;
			}
			break;
		}
		default:
			break;
		}
		// Before start AI Cast Broadcast Fly Effect is Need
		if (skill.getFlyType() != null && (isPlayer()))
			ThreadPoolManager.getInstance().scheduleEffect(new FlyToLocationTask(this, target, skill), 50);

		// launch the magic in hitTime milliseconds
		
		if (hitTime > 210)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (isPlayer() && !effectWhileCasting)
				sendPacket(new SetupGauge(SetupGauge.BLUE, hitTime));

			if (simultaneously)
			{
				if (_skillCast2 != null)
				{
					_skillCast2.cancel(true);
					_skillCast2 = null;
				}
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
				// For client animation reasons (party buffs especially) 200 ms before!
				if (effectWhileCasting)
					_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 2, simultaneously), hitTime);
				else
					_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 1, simultaneously), hitTime-200);
			}
			else
			{
				if (_skillCast != null)
				{
					_skillCast.cancel(true);
					_skillCast = null;
				}
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
				// For client animation reasons (party buffs especially) 200 ms before!
				if (effectWhileCasting)
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 2, simultaneously), hitTime);
				else
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 1, simultaneously), hitTime-200);
			}
		}
		else
			onMagicLaunchedTimer(targets, skill, coolTime, true, simultaneously);
	}

	public long calcReuseDelay(L2Skill skill) {
		int reuseDelay = 0;
		if (skill.isStaticReuse() && Config.USE_STATIC_REUSE)
			reuseDelay = skill.getReuseDelay();
		else
		{
			if (skill.isMagic())
				reuseDelay = (int) (skill.getReuseDelay() * getStat().getMReuseRate(skill));
			else
				reuseDelay = (int) (skill.getReuseDelay() * getStat().getPReuseRate(skill));
			if (reuseDelay != 0)
				reuseDelay *= 333.0 / (skill.isMagic() ? getMAtkSpd() : getPAtkSpd()); 
		}
		return reuseDelay;
	}
	private boolean checkDoCastConditions(L2Skill skill,L2Object target)
	{
		if (skill == null || isSkillDisabled(skill.getId()))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if(target!=null && target != this && skill.getTargetType() == SkillTargetType.TARGET_ONE && !this.canSee(target)) {
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (isPlayer())
		{
			switch(skill.getSkillType())
			{
				case HEAL:
				case HEAL_STATIC:
				case HEAL_PERCENT:
					if (target instanceof L2RaidBossInstance && !Config.HEALTH_SKILLS_TO_BOSSES || target instanceof L2GrandBossInstance && !Config.HEALTH_SKILLS_TO_EPIC_BOSSES)
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
						return false;
					}
					break;
			}

			if (!skill.checkCondition(this, target))
			{
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		// Check if the caster has enough MP
		if (getStatus().getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			if (isPlayer())
			{
				// Send a System Message to the caster
				sendPacket(SystemMessageId.NOT_ENOUGH_MP);

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
			}
			return false;
		}

		// Check if the caster has enough HP
		if (getStatus().getCurrentHp() <= skill.getHpConsume())
		{
			if (isPlayer())
			{
				// Send a System Message to the caster
				sendPacket(SystemMessageId.NOT_ENOUGH_HP);

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
			}
			return false;
		}

		switch (skill.getSkillType())
		{
		case SUMMON:
		{
			if (!skill.isCubic() && isPlayer() && (getPet() != null || ((L2PcInstance) this).isMounted()))
			{
				sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
				return false;
			}
		}
		}

		if (!skill.isPotion())
		{
			// Check if the skill is a magic spell and if the L2Character is not muted
			if (skill.isMagic())
			{
				if (isMuted())
				{
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else
			{
				// Check if the skill is physical and if the L2Character is not physical_muted
				if (isPhysicalMuted())
				{
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				else if (isPhysicalAttackMuted()) // Prevent use attack
				{
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		// prevent casting signets to peace zone
		if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
		{
			L2WorldRegion region = getWorldRegion();
			if (region == null)
				return false;
			boolean canCast = true;
			if (skill.getTargetType() == SkillTargetType.TARGET_GROUND && isPlayer())
			{
				Point3D wp = ((L2PcInstance) this).getCurrentSkillWorldPosition();
				if (!region.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
					canCast = false;
			}
			else if (!region.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
				canCast = false;
			if (!canCast)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill);
				sendPacket(sm);
				return false;
			}
		}

		// Check if the caster owns the weapon needed
		if (!skill.getWeaponDependancy(this, true))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Check if the spell consumes an Item
		// TODO: combine check and consume
		if (skill.getItemConsume() > 0 && getInventory() != null)
		{
			// Get the L2ItemInstance consumed by the spell
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());

			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.getSkillType() == L2SkillType.SUMMON)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
					return false;
				}

				// Send a System Message to the caster
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return false;
			}
		}
		return true;
	}

	public void addTimeStamp(int skill, int reuse)
	{
	}

	public void removeTimeStamp(int skill)
	{
	}

	public void startFusionSkill(L2Character target, L2Skill skill)
	{
		if (skill.getSkillType() != L2SkillType.FUSION)
			return;

		if (_fusionSkill == null)
			_fusionSkill = new FusionSkill(this, target, skill);
	}

	public boolean doDie(L2Character killer)
	{
		synchronized (this)
		{
			if (isDead())
				return false;

			getStatus().setCurrentHp(0);

			if (isFakeDeath())
				stopFakeDeath(null);

			setIsDead(true);
		}
		GameExtensionManager.getInstance().handleAction(this, Action.CHAR_DIE, killer);
		setTarget(null);

		if (this instanceof L2PlayableInstance && ((L2PlayableInstance) this).isInJailMission() && Config.REDUCE_JAIL_POINTS_ON_DEATH)
		{
			((L2PlayableInstance) this).removeJailPoints();
			sendMessage(String.format(Message.getMessage((L2PcInstance)this, Message.MessageId.MSG_JAIL_POINT_LOST),Config.POINTS_LOST_PER_DEATH));
		}

		stopMove(null);
		getStatus().stopHpMpRegeneration();

		if (this instanceof L2PlayableInstance)
		{
			L2PlayableInstance pl = (L2PlayableInstance) this;
			if (pl.isPhoenixBlessed())
			{
				if (pl.getCharmOfLuck()) //remove Lucky Charm if player has SoulOfThePhoenix/Salvation buff
					pl.stopCharmOfLuck(null);
				if (pl.isNoblesseBlessed())
					pl.stopNoblesseBlessing(null);
			}
			// Same thing if the Character isn't a Noblesse Blessed L2PlayableInstance
			else if (pl.isNoblesseBlessed())
			{
				pl.stopNoblesseBlessing(null);
				if (pl.getCharmOfLuck()) // remove Lucky Charm if player have Nobless blessing buff
					pl.stopCharmOfLuck(null);
			}
			else
				stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		else
			stopAllEffectsExceptThoseThatLastThroughDeath();


		calculateRewards(killer);
		if (this instanceof L2NpcInstance)
		{
			if (((L2NpcInstance) this).getNpcId()==35368)
				if(FortResistSiegeManager.getInstance().getIsInProgress())
					FortResistSiegeManager.getInstance().endSiege(true);
			if (((L2NpcInstance) this).getNpcId()==35410)
				if(DevastatedCastleSiege.getInstance().getIsInProgress())
					DevastatedCastleSiege.getInstance().endSiege(killer);
			if (((L2NpcInstance) this).getNpcId()==35630)
				if(FortressOfDeadSiege.getInstance().getIsInProgress())
					FortressOfDeadSiege.getInstance().endSiege(killer);
		}
		broadcastStatusUpdate();

		if (getWorldRegion() != null)
			getWorldRegion().onDeath(this);

		getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);

		// Notify Quest of character's death
		for (QuestState qs : getNotifyQuestOfDeath())
			qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);

		getNotifyQuestOfDeath().clear();
		// If character is PhoenixBlessed 
		// or has charm of courage inside siege battlefield (exact operation to be confirmed)
		// a resurrection popup will show up
		if (this instanceof L2Summon)
		{
			if (((L2Summon) this).isPhoenixBlessed() && ((L2Summon) this).getOwner() != null)
				((L2Summon) this).getOwner().revivePetRequest(((L2Summon) this).getOwner(), null);
		}
		else if (isPlayer())
		{
			if (((L2PlayableInstance) this).isPhoenixBlessed())
				((L2PcInstance) this).reviveRequest(((L2PcInstance) this), null);
			else if (((L2PcInstance) this).getCharmOfCourage() && isInsideZone(L2Zone.FLAG_SIEGE) && ((L2PcInstance) this).getSiegeState() != 0) // could check it more accurately too
			{
				if (((L2PcInstance) this).getCanUseCharmOfCourageRes())
				{
					((L2PcInstance) this).reviveRequest(((L2PcInstance) this), null);
					((L2PcInstance) this).setCanUseCharmOfCourageRes(false);
				}
			}
		}
		if (isInsideZone(L2Zone.FLAG_SIEGE))
		{
			if (isPlayer() && ((L2PcInstance) this).getSiegeState() != 0)
			{
				int playerClanId=((L2PcInstance) this).getClanId();
				int playerCharId=((L2PcInstance) this).getObjectId();
				L2SiegeStatus.getInstance().addStatus(playerClanId,playerCharId , false);
			}
			if (killer.isPlayer() && ((L2PcInstance) killer).getSiegeState() != 0)
			{
				int killerClanId=((L2PcInstance) killer).getClanId();
				int killerCharId=((L2PcInstance) killer).getObjectId();
				L2SiegeStatus.getInstance().addStatus(killerClanId,killerCharId , true);
			}
			if (killer instanceof L2Summon && ((L2Summon) killer).getOwner().getSiegeState() != 0)
			{
				int killerClanId=((L2Summon) killer).getOwner().getClanId();
				int killerCharId=((L2Summon) killer).getOwner().getObjectId();
				L2SiegeStatus.getInstance().addStatus(killerClanId,killerCharId , true);
			}
		}

		getAttackByList().clear();

		try
		{
			if (_fusionSkill != null)
				abortCast();

			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if(isInFunEvent())
			getGameEvent().onKill(killer, this);

		_listners.onDeath(killer);

		return true;
	}

	protected void calculateRewards(L2Character killer)
	{
	}

	public void doRevive()
	{
		if (!isDead())
			return;
		GameExtensionManager.getInstance().handleAction(this, Action.CHAR_REVIVE);
		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			setIsDead(false);

			boolean restorefull = false;

			if (this instanceof L2PlayableInstance && ((L2PlayableInstance) this).isPhoenixBlessed())
			{
				restorefull = true;
				((L2PlayableInstance) this).stopPhoenixBlessing(null);
			}

			if (restorefull)
			{
				_status.setCurrentHp(getMaxHp());
				_status.setCurrentMp(getMaxMp());
			}
			else
				_status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);

			broadcastPacket(new Revive(this));

			if (getWorldRegion() != null)
				getWorldRegion().onRevive(this);
		}
		else
			setIsPendingRevive(true);
	}

	public void doRevive(double revivePower)
	{
		doRevive();
	}

	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				_ai = new L2CharacterAI(new AIAccessor());
				return _ai;
			}
		}

		return ai;
	}

	public void setAI(L2CharacterAI newAI)
	{
		L2CharacterAI oldAI = getAI();
		if (oldAI != null && oldAI != newAI && oldAI instanceof L2AttackableAI)
			oldAI.stopAITask();
		_ai = newAI;
	}

	public boolean hasAI()
	{
		return _ai != null;
	}

	public boolean isRaid()
	{
		return _isRaid;
	}

	@Override
	public boolean isBoss()
	{
		return _isBoss;
	}

	public boolean isGrandBoss()
	{
		return _isGrandBoss;
	}

	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}

	public void setIsGrandBoss(boolean b)
	{
		_isGrandBoss = b;
	}

	public void setIsBoss(boolean b)
	{
		_isBoss = b;
	}

	public final List<L2Character> getAttackByList()
	{
		if (_attackByList == null)
			_attackByList = new SingletonList<L2Character>();
		return _attackByList;
	}

	public final L2Character getAttackingChar()
	{
		return _attackingChar;
	}

	public final void setAttackingChar(L2Character player)
	{
		if (player == null || player == this)
			return;

		_attackingChar = player;
		addAttackerToAttackByList(player);
	}

	public final L2Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}

	public void setLastSimultaneousSkillCast (L2Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}

	public final boolean isAfraid()
	{
		return _isAfraid;
	}

	public final void setIsAfraid(boolean value)
	{
		_isAfraid = value;
	}

	public void setTeam(int team)
	{
		_team = team;
		if (getPet() != null)
			getPet().broadcastFullInfo();
	}

	public int getTeam()
	{
		return _team;
	}	
	
	/** Return true if the L2Character can't use its skills (ex : stun, sleep...). */
	public boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isSleeping() || isImmobileUntilAttacked() || isParalyzed() || isPetrified();
	}

	/** Return true if the L2Character can't attack (stun, sleep, attackEndTime, fakeDeath, paralyse). */
	public boolean isAttackingDisabled()
	{
		return isStunned() || isSleeping() || isImmobileUntilAttacked() || isAttackingNow() || isFakeDeath() || isParalyzed()
		|| isPetrified() || isFallsdown() || isPhysicalAttackMuted() || isCoreAIDisabled();
	}

	public final Calculator[] getCalculators()
	{
		return _calculators;
	}

	public final boolean isConfused()
	{
		return _isConfused;
	}

	public final void setIsConfused(boolean value)
	{
		_isConfused = value;
	}

	public final boolean isDead()
	{
		return _isDead;
	}

	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}

	/** Return true if the L2Character is dead or use fake death.  */
	public final boolean isAlikeDead()
	{
		return isFakeDeath() || _isDead;
	}

	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}

	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}

	// [L2J_JP_ADD START]
	public final boolean isFallsdown()
	{
		return _isFallsdown;
	}

	public final void setIsFallsdown(boolean value)
	{
		_isFallsdown = value;
	}
	// [L2J_JP_ADD END]

	public boolean isFlying()
	{
		return false;
	}

	public boolean isImmobilized()
	{
		return _isImmobilized;
	}

	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}

	public final boolean isMuted()
	{
		return _isMuted;
	}

	public final void setIsMuted(boolean value)
	{
		_isMuted = value;
	}

	public final boolean isPhysicalMuted()
	{
		return _isPhysicalMuted;
	}

	public final void setIsPhysicalMuted(boolean value)
	{
		_isPhysicalMuted = value;
	}

	public final boolean isPhysicalAttackMuted()
	{
		return _isPhysicalAttackMuted;
	}

	public final void setIsPhysicalAttackMuted(boolean value)
	{
		_isPhysicalAttackMuted = value;
	}

	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}

	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}

	public boolean isMovementDisabled()
	{
		return isStunned() || isRooted() || isSleeping() || isTeleporting() || isImmobileUntilAttacked() || isOverloaded() || isParalyzed() || isImmobilized() || isFakeDeath() || isFallsdown() || isPetrified();
	}

	public boolean isOutOfControl()
	{
		return isConfused() || isAfraid();
	}

	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}

	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}

	public final boolean isParalyzed()
	{
		return _isParalyzed;
	}

	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}

	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}

	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}

	public final boolean isDisarmed()
	{
		return _isDisarmed;
	}

	public final void setIsDisarmed(boolean value)
	{
		_isDisarmed = value;
	}

	public L2Summon getPet()
	{
		return null;
	}

	public final boolean isRooted()
	{
		return _isRooted;
	}

	public final void setIsRooted(boolean value)
	{
		_isRooted = value;
	}

	public final boolean isRunning()
	{
		return _isRunning;
	}

	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		if (getRunSpeed() != 0)
			broadcastPacket(new ChangeMoveType(this));

		broadcastFullInfo();
	}

	public final void setRunning()
	{
		if (!isRunning())
			setIsRunning(true);
	}

	public final boolean isSleeping()
	{
		return _isSleeping;
	}

	public final void setIsImmobileUntilAttacked(boolean value)
	{
		_isImmobileUntilAttacked = value;
	}

	public final boolean isImmobileUntilAttacked()
	{
		return _isImmobileUntilAttacked;
	}

	public final void setIsSleeping(boolean value)
	{
		_isSleeping = value;
	}

	public final boolean isBlessedByNoblesse()
	{
		return _isBlessedByNoblesse;
	}

	public final void setIsBlessedByNoblesse(boolean value)
	{
		_isBlessedByNoblesse = value;
	}

	public final boolean isLuckByNoblesse()
	{
		return _isLuckByNoblesse;
	}

	public final void setIsLuckByNoblesse(boolean value)
	{
		_isLuckByNoblesse = value;
	}

	public final boolean isStunned()
	{
		return _isStunned;
	}

	public final void setIsStunned(boolean value)
	{
		_isStunned = value;
	}

	public final boolean isPetrified()
	{
		return _isPetrified;
	}

	public final void setIsPetrified(boolean value)
	{
		_isPetrified = value;
	}

	public final boolean isBetrayed()
	{
		return _isBetrayed;
	}

	public final void setIsBetrayed(boolean value)
	{
		_isBetrayed = value;
	}

	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}

	public final void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}

	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}

	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}

	public boolean isUndead()
	{
		return _template.isUndead();
	}

	@Override
	public CharKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new CharKnownList(this);

		return _knownList;
	}

	public CharStat getStat()
	{
		if (_stat == null)
			_stat = new CharStat(this);

		return _stat;
	}

	public CharStatus getStatus()
	{
		if (_status == null)
			_status = new CharStatus(this);

		return _status;
	}

	public L2CharTemplate getTemplate()
	{
		return _template;
	}

	protected final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}

	private String _eventTitle; 
	/** Return the Title of the L2Character. */
	public  String getTitle()
	{
		
		if(_event!=null && _event.isRunning()) {
			if(_eventTitle == null)
				_eventTitle = _title;
			return _eventTitle;
		}
		return _title;
	}

	/** Set the Title of the L2Character. */
	public final void setTitle(String value)
	{
		if (isPlayer() && value.length() > 16)
			value = value.substring(0, 15);

		if (_event!=null && _event.isRunning()) {
			_eventTitle = value;
			if(_eventTitle == null)
				_eventTitle = "";
			return;
		}
			
		if (value == null)
		{
			_title = "";
			return;
		}

		if (Config.FACTION_ENABLED)
		{
			if (isPlayer())
			{
				if (FactionManager.getInstance().getFactionTitles().contains(value.toLowerCase()) && !value.isEmpty())
				{
					_title = getTitle();
					sendMessage(Message.getMessage((L2PcInstance)this, Message.MessageId.MSG_FACTION_CANT_CHANGE_TITLE));
					return;
				}
			}
		}

		_title = value;
	}

	public final void setWalking()
	{
		if (isRunning())
			setIsRunning(false);
	}

	class EnableSkill extends RunnableImpl
	{
		int	_skillId;

		public EnableSkill(int skillId)
		{
			_skillId = skillId;
		}

		public void runImpl()
		{
			try
			{
				enableSkill(_skillId);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	class HitTask extends RunnableImpl
	{
		L2Character	_hitTarget;
		int			_damage;
		boolean		_crit;
		boolean		_miss;
		byte		_shld;
		boolean		_soulshot;

		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
		}

		public void runImpl()
		{
			try
			{
				onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	class MagicUseTask extends RunnableImpl
	{
		L2Character[]	_targets;
		L2Skill		_skill;
		int			_coolTime;
		int			_phase;
		boolean		_simultaneously;

		public MagicUseTask(L2Character[] targets, L2Skill skill, int coolTime, int phase, boolean simultaneously)
		{
			_targets = targets;
			_skill = skill;
			_coolTime = coolTime;
			_phase = phase;
			_simultaneously = simultaneously;
		}

		public void runImpl()
		{
			try
			{
				switch (_phase)
				{
				case 1:
					onMagicLaunchedTimer(_targets, _skill, _coolTime, false, _simultaneously);
					break;
				case 2:
					try {
						onMagicHitTimer(_targets, _skill, _coolTime, false, _simultaneously);
					} catch(Exception e) {
						e.printStackTrace();
					}
					break;
				case 3:
					onMagicFinalizer(_skill, _targets[0], _simultaneously);
					break;
				default:
					break;
				}
			}
			catch (Exception e)
			{
				if (_simultaneously) 
					setIsCastingSimultaneouslyNow(false);
				else
					setIsCastingNow(false);
			}
		}
	}

	class QueuedMagicUseTask extends RunnableImpl
	{
		L2PcInstance	_currPlayer;
		L2Skill			_queuedSkill;
		boolean			_isCtrlPressed;
		boolean			_isShiftPressed;

		public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
		{
			_currPlayer = currPlayer;
			_queuedSkill = queuedSkill;
			_isCtrlPressed = isCtrlPressed;
			_isShiftPressed = isShiftPressed;
		}

		public void runImpl()
		{
			try
			{
				_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
			}
			catch (Exception e)
			{
			}
		}
	}

	public class NotifyAITask extends RunnableImpl
	{
		private final CtrlEvent	_evt;

		NotifyAITask(CtrlEvent evt)
		{
			_evt = evt;
		}

		@Override
		public void runImpl()
		{
			try
			{
				if (getAI() == null)
					return;

				getAI().notifyEvent(_evt, null);
			}
			catch (Exception e)
			{
			}
		}
	}

	public class PvPFlag extends RunnableImpl
	{
		public void runImpl()
		{
			try
			{
				if (System.currentTimeMillis() > getPvpFlagLasts())
				{
					stopPvPFlag();
				}
				else if (System.currentTimeMillis() > (getPvpFlagLasts() - 20000))
				{
					updatePvPFlag(2);
				}
				else
				{
					updatePvPFlag(1);
				}
			}
			catch (Exception e)
			{
			}
		}
	}

	private int				_AbnormalEffects;
	private CharEffectList	_effects						= new CharEffectList(this);

	public void addEffect(L2Effect newEffect)
	{
		if (this.getFirstEffect(Config.BLOCK_BUFF) != null && newEffect.getEffector().isPlayer() && !newEffect.getEffector().equals(newEffect.getEffected()))
			if (newEffect.getSkill().getSkillType() == L2SkillType.BUFF)
				return;

        _effects.addEffect(newEffect);
    }

	public boolean isAllow(EffectTemplate effect, L2Skill skill) {
		return _effects.isPossible(effect, skill);
	}
	public void removeEffect(L2Effect effect)
	{
		_effects.removeEffect(effect);
	}

	public final void startAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects |= mask.getMask();
		updateAbnormalEffect();
	}	

	public final void startAbnormalEffect(int mask)
	{
		if (_AbnormalEffects != (_AbnormalEffects |= mask))
			updateAbnormalEffect();
	}

	public final void startConfused()
	{
		setIsConfused(true);
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}

	public final void startFakeDeath()
	{
		if (isPlayer())
		{
			L2PcInstance player = (L2PcInstance)this;
			if (player != null)
			{
				if (player.getGameEvent() != null && player.getGameEvent().isRunning())
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_EVENT_SKILL_NOT_ALOWED));
					return;
				}
			}
		}

		setIsFallsdown(true);

		if (Config.FAIL_FAKEDEATH)
		{
			setIsFakeDeath(true);
			if (_attackingChar != null)
			{
				int _diff = _attackingChar.getLevel() - getLevel();
				switch (_diff)
				{
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
					if (Rnd.nextInt(100) >= 95) // fails at 5%.
						setIsFakeDeath(false);
					break;
				case 6:
					if (Rnd.nextInt(100) >= 90) // fails at 10%.
						setIsFakeDeath(false);
					break;
				case 7:
					if (Rnd.nextInt(100) >= 85) // fails at 15%.
						setIsFakeDeath(false);
					break;
				case 8:
					if (Rnd.nextInt(100) >= 80) // fails at 20%.
						setIsFakeDeath(false);
					break;
				case 9:
					if (Rnd.nextInt(100) >= 75) // fails at 25%.
						setIsFakeDeath(false);
					break;
				default:
					if (_diff > 9)
					{
						if (Rnd.nextInt(100) >= 50) // fails at 50%.
							setIsFakeDeath(false);
					}
					else
						setIsFakeDeath(true);
				}
				if (_attackingChar.isRaid())
					setIsFakeDeath(false);
			}
			else
			{
				if (Rnd.nextInt(100) >= 75) // fails at 25%.
					setIsFakeDeath(false);
			}
		}
		else
			setIsFakeDeath(true);

		abortAttack();
		abortCast();
		stopMove(null);
		sendPacket(ActionFailed.STATIC_PACKET);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH, null);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}

	public final void startFear(L2Character effector)
	{
		setIsAfraid(true);
		getAI().notifyEvent(CtrlEvent.EVT_AFRAID, effector);
		updateAbnormalEffect();
	}

	public final void startMuted(L2Character effector)
	{
		setIsMuted(true);
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_MUTED, effector);
		updateAbnormalEffect();
	}

	public final void startPhysicalMuted(L2Character effector)
	{
		setIsPhysicalMuted(true);
		getAI().notifyEvent(CtrlEvent.EVT_MUTED, effector);
		updateAbnormalEffect();
	}

	public final void startRooted(L2Character effector)
	{
        setIsRooted(true);
        stopMove(null);
        getAI().notifyEvent(CtrlEvent.EVT_ROOTED, effector);
        updateAbnormalEffect();
	}

	public final void startSleeping(L2Character effector)
	{
		setIsSleeping(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING, effector);
		updateAbnormalEffect();
	}

	public final void startImmobileUntilAttacked(L2Character effector)
	{
		setIsImmobileUntilAttacked(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING, effector);
		updateAbnormalEffect();
	}

	public final void startLuckNoblesse()
	{
		setIsBlessedByNoblesse(true);
		getAI().notifyEvent(CtrlEvent.EVT_LUCKNOBLESSE, null);
	}

	public final void stopLuckNoblesse()
	{
		setIsBlessedByNoblesse(false);
		getAI().notifyEvent(CtrlEvent.EVT_LUCKNOBLESSE, null);
	}

	public final void startStunning(L2Character effector)
	{
		setIsStunned(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED, effector);
		updateAbnormalEffect();
	}

	public final void startParalyze(L2Character effector)
	{
		setIsParalyzed(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED, effector);
		updateAbnormalEffect();
	}

	public final void startBetray(L2Character effector)
	{
		setIsBetrayed(true);
		getAI().notifyEvent(CtrlEvent.EVT_BETRAYED, effector);
		updateAbnormalEffect();
	}

	public final void stopBetray()
	{
		stopEffects(L2EffectType.BETRAY);
		setIsBetrayed(false);
		updateAbnormalEffect();
	}

	public final void stopAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects &= ~mask.getMask();
		updateAbnormalEffect();
	}

	public final void stopAbnormalEffect(int mask)
	{
		if (_AbnormalEffects != (_AbnormalEffects &= ~mask))
			updateAbnormalEffect();
	}

	public final void stopAllEffects()
	{
		_effects.stopAllEffects();
		broadcastFullInfo();
	}

	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath();
		broadcastFullInfo();
	}

	public final void stopConfused(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CONFUSION);
		else
			removeEffect(effect);

		setIsConfused(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void startPhysicalAttackMuted()
	{
		setIsPhysicalAttackMuted(true);
		abortAttack();
	}

	public final void stopPhysicalAttackMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHYSICAL_ATTACK_MUTE);
		else
			removeEffect(effect);
		setIsPhysicalAttackMuted(false);
	}

	public final void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}

	public final void stopEffects(L2EffectType type)
	{
		_effects.stopEffects(type);
	}

	public final void stopFakeDeath(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.FAKE_DEATH);
		else
			removeEffect(effect);

		setIsFakeDeath(false);
		setIsFallsdown(false);

		if (isPlayer())
			((L2PcInstance) this).setRecentFakeDeath(true);

		ChangeWaitType revive = new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH);
		broadcastPacket(revive);
		broadcastPacket(new Revive(this));
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
	}

	public final void stopFear(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.FEAR);
		else
			removeEffect(effect);

		setIsAfraid(false);
		updateAbnormalEffect();
	}

	public final void stopMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.MUTE);
		else
			removeEffect(effect);

		setIsMuted(false);
		updateAbnormalEffect();
	}

	public final void stopPhysicalMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHYSICAL_MUTE);
		else
			removeEffect(effect);

		setIsPhysicalMuted(false);
		updateAbnormalEffect();
	}

	public final void stopRooting(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.ROOT);
		else
			removeEffect(effect);

		setIsRooted(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopSleeping(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.SLEEP);
		else
			removeEffect(effect);

		setIsSleeping(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopImmobileUntilAttacked(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.IMMOBILEUNTILATTACKED);
		else
		{
			removeEffect(effect);
			stopSkillEffects(effect.getSkill().getNegateId());
		}

		setIsImmobileUntilAttacked(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopNoblesse()
	{
		stopEffects(L2EffectType.NOBLESSE_BLESSING);
		stopEffects(L2EffectType.LUCKNOBLESSE);
		setIsBlessedByNoblesse(false);
		setIsLuckByNoblesse(false);
		getAI().notifyEvent(CtrlEvent.EVT_LUCKNOBLESSE, null);
	}

	public final void stopStunning(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.STUN);
		else
			removeEffect(effect);

		setIsStunned(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopParalyze(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PARALYZE);
		else
			removeEffect(effect);

		setIsParalyzed(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void updateAbnormalEffect()
	{
		broadcastFullInfo();
	}


	public int getAbnormalEffect()
	{
		int ae = _AbnormalEffects;
		if (isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.SLEEP.getMask();
		if (isConfused())
			ae |= AbnormalEffect.CONFUSED.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		return ae;
	}

	public final L2Effect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}

	public final L2Effect getFirstEffect(int skillId)
	{
		return _effects.getFirstEffect(skillId);
	}

	public final L2Effect getFirstEffect(L2Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}

	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		return _effects.getFirstEffect(tp);
	}

	public class AIAccessor
	{
		public AIAccessor()
		{
		}

		public L2Character getActor()
		{
			return L2Character.this;
		}
                
		public void moveTo(int x, int y, int z, int offset)
		{
                            moveToLocation(x, y, z, offset);
		}

		public boolean moveTo(int x, int y, int z)
		{
                            return moveToLocation(x, y, z, 0);
		}

		public void stopMove(L2CharPosition pos)
		{
                            L2Character.this.stopMove(pos);
		}

		public void doAttack(L2Character target)
		{
			if (L2Character.this != target)
				L2Character.this.doAttack(target);
		}

		public void doCast(L2Skill skill)
		{
			L2Character.this.doCast(skill);
		}

		public NotifyAITask newNotifyTask(CtrlEvent evt)
		{
			return new NotifyAITask(evt);
		}

		public void detachAI()
		{
			_ai = null;
		}
	}

	public static class MoveData
	{
		public int						_moveStartTime;
		public int						_moveTimestamp; // last update
		public int						_xDestination;
		public int						_yDestination;
		public int						_zDestination;
		public double					_xAccurate; // otherwise there would be rounding errors
		public double					_yAccurate;
		public double					_zAccurate;
		public int						_yMoveFrom;
		public int						_zMoveFrom;
		public int						_heading;
		public boolean					disregardingGeodata;
		public int						onGeodataPathIndex;
		public List<AbstractNodeLoc>	geoPath;
		public int						geoPathAccurateTx;
		public int						geoPathAccurateTy;
		public int						geoPathGtx;
		public int						geoPathGty;
	}

	protected Set<Integer>				_disabledSkills;
	private boolean						_allSkillsDisabled;
	protected MoveData					_move;
	private int							_heading;
	private L2Object					_target					= null;
	private long						_attackEndTime;
	private int							_attacking;
	private int							_disableBowAttackEndTime;
	private int							_castInterruptTime;
	private static final Calculator[]	NPC_STD_CALCULATOR;

	static
	{
		NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	}

	protected L2CharacterAI				_ai;
	protected Future<?>					_skillCast;
	protected Future<?>					_skillCast2;
	private List<QuestState>			_NotifyQuestOfDeathList = new SingletonList<QuestState>();

	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null || _NotifyQuestOfDeathList.contains(qs))
			return;
		_NotifyQuestOfDeathList.add(qs);
	}

	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if (_NotifyQuestOfDeathList == null)
			_NotifyQuestOfDeathList = new SingletonList<QuestState>();

		return _NotifyQuestOfDeathList;
	}

	public final boolean isOnGeodataPath()
	{
		MoveData m = _move;
		if (m == null)
			return false;
		if (m.onGeodataPathIndex == -1)
			return false;

		return m.onGeodataPathIndex != m.geoPath.size() - 1;
	}

	public final void addStatFunc(Func f)
	{
		if (f == null)
			return;

		synchronized (_calculators)
		{
			// Check if Calculator set is linked to the standard Calculator set of NPC
			if (_calculators == NPC_STD_CALCULATOR)
			{
				// Create a copy of the standard NPC Calculator set
				_calculators = new Calculator[Stats.NUM_STATS];

				for (int i = 0; i < Stats.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
				}
			}

			// Select the Calculator of the affected state in the Calculator set
			int stat = f.stat.ordinal();

			if (_calculators[stat] == null)
				_calculators[stat] = new Calculator();

			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(f);

			if (isPlayer())
				((L2PcInstance) this).onFuncAddition(f);
		}

		broadcastFullInfo();
	}

	public final void addStatFuncs(Func[] funcs)
	{
		for (Func f : funcs)
			addStatFunc(f);
	}

	public final void addStatFuncs(Iterable<Func> funcs)
	{
		for (Func f : funcs)
			addStatFunc(f);
	}

	public final void removeStatsOwner(FuncOwner owner)
	{
		// Go through the Calculator set
		synchronized (_calculators)
		{
			for (int i = 0; i < _calculators.length; i++)
			{
				if (_calculators[i] != null)
				{
					// Delete all Func objects of the selected owner
					_calculators[i].removeOwner(owner, this);

					if (_calculators[i].size() == 0)
						_calculators[i] = null;
				}
			}

			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (this instanceof L2NpcInstance)
			{
				int i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
						break;
				}

				if (i >= Stats.NUM_STATS)
					_calculators = NPC_STD_CALCULATOR;
			}
		}

		broadcastFullInfo();
	}

	/**
	 * Return the orientation of the L2Character.<BR>
	 * <BR>
	 */
	public final int getHeading()
	{
		return _heading;
	}

	/**
	 * Set the orientation of the L2Character.<BR>
	 * <BR>
	 */
	public final void setHeading(int heading)
	{
		_heading = heading;
	}

	/**
	 * Return the X destination of the L2Character or the X position if not in movement.<BR>
	 * <BR>
	 */

	public final int getXdestination()
	{
		MoveData m = _move;

		if (m != null)
			return m._xDestination;

		return getX();
	}

	public final int getYdestination()
	{
		MoveData m = _move;

		if (m != null)
			return m._yDestination;

		return getY();
	}

	/**
	 * Return the Z destination of the L2Character or the Z position if not in movement.<BR>
	 * <BR>
	 */
	public final int getZdestination()
	{
		MoveData m = _move;

		if (m != null)
			return m._zDestination;

		return getZ();
	}

	/**
	 * Return true if the L2Character is in combat.<BR>
	 * <BR>
	 */
	public boolean isInCombat()
	{
		return (getAI().getAttackTarget() != null || getAI().isAutoAttacking());
	}

	/**
	 * Return true if the L2Character is moving.<BR>
	 * <BR>
	 */
	public final boolean isMoving()
	{
		return _move != null;
	}

	/**
	 * Return true if the L2Character is casting.<BR>
	 * <BR>
	 */
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}

	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}

	public final boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}

	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}

	/**
	 * Return true if the cast of the L2Character can be aborted.<BR>
	 * <BR>
	 */
	public final boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeController.getGameTicks();
	}

	/**
	 * Return true if the L2Character is attacking.<BR>
	 * <BR>
	 */
	public boolean isAttackingNow()
	{
		return getAttackEndTime() > L2System.milliTime();
	}

	/**
	 * Return true if the L2Character has aborted its attack.<BR>
	 * <BR>
	 */
	public final boolean isAttackAborted()
	{
		return _attacking <= 0;
	}

	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			setAttackingChar(this);
			_attacking = 0;
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public final int getAttackingBodyPart()
	{
		return _attacking;
	}

	public final void abortCast()
	{
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			if (_skillCast != null)
			{
				_skillCast.cancel(true);
				_skillCast = null;
			}
			if (_skillCast2 != null)
			{
				_skillCast2.cancel(false);
				_skillCast2 = null;
			}

			if (getFusionSkill() != null)
				getFusionSkill().onCastAbort();

			L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
			if (mog != null)
				mog.exit();

			if (_allSkillsDisabled)
				enableAllSkills();
			setIsCastingNow(false);
			setIsCastingSimultaneouslyNow(false);
			_castInterruptTime = 0;
			if (isPlayer())
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
			broadcastPacket(new MagicSkillCanceled(getObjectId()));
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public boolean updatePosition(int gameTicks)
	{
		// Get movement data
		MoveData m = _move;

		if (m == null)
			return true;

		if (!isVisible())
		{
			_move = null;
			return true;
		}

		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}

		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
			return false;

		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations

		double dx, dy, dz, distFraction;
		if (Config.COORD_SYNCHRONIZE == 1)
			// the only method that can modify x,y while moving (otherwise _move would/should be set null)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else // otherwise we need saved temporary values to avoid rounding errors
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		// Z coordinate will follow geodata or client values
		if (Config.GEODATA && Config.COORD_SYNCHRONIZE == 2
		&& !isFlying() && !isInsideZone(L2Zone.FLAG_WATER)
		&& !m.disregardingGeodata
		&& GameTimeController.getGameTicks() % 10 == 0
		&& !(this instanceof L2BoatInstance)) // once a second to reduce possible cpu load
		{
			short geoHeight = GeoData.getInstance().getSpawnHeight(xPrev, yPrev, zPrev-30, zPrev+30, this);
			dz = m._zDestination - geoHeight;
			// quite a big difference, compare to validatePosition packet
			if (isPlayer() && Math.abs(getZ() - geoHeight) > 200
					&& Math.abs(getZ() - geoHeight) < 1500)
			{
				dz = m._zDestination - zPrev; // allow diff
			}
			else if (isInCombat() && Math.abs(dz) > 200 && (dx*dx + dy*dy) < 40000) // allow mob to climb up to pcinstance
			{
				dz = m._zDestination - zPrev; // climbing
			}
			else
			{
				zPrev = geoHeight;
			}
		}
		else
			dz = m._zDestination - zPrev;

		double distPassed = getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
		if ((dx*dx + dy*dy) < 10000 && (dz*dz > 2500)) // close enough, allows error between client and server geodata if it cannot be avoided
			distFraction = distPassed / Math.sqrt(dx*dx + dy*dy);
		else
			distFraction = distPassed / Math.sqrt(dx*dx + dy*dy + dz*dz);

		if (distFraction > 1) // already there
		{
			// Set the position of the L2Character to the destination
			super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination);
			if (this instanceof L2BoatInstance)
			{
				((L2BoatInstance) this).updatePeopleInTheBoat(m._xDestination, m._yDestination, m._zDestination);
			}
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;

			// Set the position of the L2Character to estimated after parcial move
			super.getPosition().setXYZ((int)(m._xAccurate), (int)(m._yAccurate), zPrev + (int)(dz * distFraction + 0.5));
			if (this instanceof L2BoatInstance)
			{
				((L2BoatInstance)this).updatePeopleInTheBoat((int)(m._xAccurate), (int)(m._yAccurate), zPrev + (int)(dz * distFraction + 0.5));
			}
			else
			{
				revalidateZone(false);
			}
		}

		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;

		return (distFraction > 1);
	}

	private long _lastKnowUpdate;
	public void setKnowAsUpdated(long time)
    {
        _lastKnowUpdate = time;
    }

    public long getLastKnowUpdate()
    {
        return _lastKnowUpdate;
    }

	public void revalidateZone(boolean force)
	{
		// This function is called very often from movement code
		if (force)
			_zoneValidateCounter = 4;
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
				_zoneValidateCounter = 4;
			else
				return;
		}

		if (getWorldRegion() == null)
			return;
		getWorldRegion().revalidateZones(this);
	}

	public void stopMove(L2CharPosition pos)
	{
		stopMove(pos, false);
	}

	public void stopMove() {
		if(!_isMoving)
			return;
		_move = null;
		_isMoving = false;
		getPosition().setXYZ(_startLoc.getX(), _startLoc.getY(), _startLoc.getZ());
		Broadcast.toKnownPlayers(this, new StopMove(this));
	}
	
	public void stopMove(L2CharPosition pos, boolean updateKnownObjects)
	{
		// Delete movement data of the L2Character
		_move = null;
		_isMoving = false;
		// Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading
		// All data are contained in a L2CharPosition object
		if (pos != null)
		{
			getPosition().setXYZ(pos.x, pos.y, pos.z);
			setHeading(pos.heading);
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));
		if (updateKnownObjects)
			getKnownList().updateKnownObjects();
	}

	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}

	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}

	public void setTarget(L2Object object)
	{
		if (object != null && !object.isVisible())
			object = null;

		if (object != null && object != _target)
		{
			getKnownList().addKnownObject(object);
			object.getKnownList().addKnownObject(this);
		}
		_target = object;
		
	}

	public final int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}

		return -1;
	}

	public final L2Object getTarget()
	{
		return _target;
	}

	protected boolean _isMoving;
	public void finishMovement() {
		_isMoving = false;
	}
	public void actionFail() {
		sendPacket(ActionFailed.STATIC_PACKET);
	}
	protected Location _startLoc;
	protected boolean moveToLocation(int x, int y, int z, int offset)
	{
		
		_startLoc = new Location(getLoc());
		_isMoving = true;
		// Get the Move Speed of the L2Charcater
		float speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled()) {
			actionFail();
			return false;
		}

		
		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.sqrt(dx*dx + dy*dy);
		// make water move short and use no geodata checks for swimming chars
		// distance in a click can easily be over 3000
		if (Config.GEODATA && isInsideZone(L2Zone.FLAG_WATER) && distance > 700) 
		{
			double divider = 700/distance;
			x = curX + (int)(divider * dx);
			y = curY + (int)(divider * dy);
			z = curZ + (int)(divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.sqrt(dx*dx + dy*dy);
		}

		double cos;
		double sin;

		// Check if a movement offset is defined or no distance to go through
		if (offset > 0 || distance < 1)
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
				offset = 5;

			// If no distance to go through, the movement is canceled
			if (distance < 1 || distance - offset  <= 0)
			{
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return false;
			}
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			distance -= (offset - 5);
			x = curX + (int)(distance * cos);
			y = curY + (int)(distance * sin);

		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}

		// Create and Init a MoveData object
		MoveData m = new MoveData();

		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m.disregardingGeodata = false;

		if (Config.PATHFINDING && !isFlying() && (!isInsideZone(L2Zone.FLAG_WATER) || isInsideZone(L2Zone.FLAG_SIEGE)) && !(this instanceof L2NpcWalkerInstance))
		{
			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = (originalX - L2World.MAP_MIN_X) >> 4;
			int gty = (originalY - L2World.MAP_MIN_Y) >> 4;

		// Movement checks:
		// when geodata == 2, for all characters except mobs returning home (could be changed later to teleport if pathfinding fails)
		// when geodata == 1, for l2playableinstance and l2riftinstance only
		if ((Config.PATHFINDING  &&	!(this instanceof L2Attackable && ((L2Attackable)this).isReturningToSpawnPoint())) || isPlayer() || (this instanceof L2Summon && !(getAI().getIntention() == AI_INTENTION_FOLLOW)) || isAfraid() || this instanceof L2RiftInvaderInstance)
		{
			if (isOnGeodataPath())
			{
				try
				{
					if (gtx == _move.geoPathGtx && gty == _move.geoPathGty)
						return false;
					else
						_move.onGeodataPathIndex = -1; // Set not on geodata path	
				}
				catch (NullPointerException e)
				{ 
					// nothing
				}
			}
			if (curX < L2World.MAP_MIN_X || curX > L2World.MAP_MAX_X || curY < L2World.MAP_MIN_Y  || curY > L2World.MAP_MAX_Y)
			{
				// Temporary fix for character outside world region errors
				_log.warn("Character "+getName()+" outside world area, in coordinates x:"+curX+" y:"+curY);
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				if (isPlayer())
					new Disconnection((L2PcInstance) this).defaultSequence(true);
				else if (!(this instanceof L2Summon))
					onDecay();
				actionFail();
				return false;
			}
			Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, this.getInstanceId());
			
			// location different if destination wasn't reached (or just z coord is different)
			x = destiny.getX();
			y = destiny.getY();
			z = destiny.getZ();
			distance = Math.sqrt((x - curX) * (x - curX) + (y - curY) * (y - curY));
		}
		if(Config.PATHFINDING  && originalDistance-distance > 100 && distance < 2000 && !isAfraid())
		{

			// Overrides previous movement check
//			if(this instanceof L2PlayableInstance || isInCombat() || this instanceof L2MinionInstance)
			{
				m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, this.getInstanceId(),getPlayer()!=null);
				if (m.geoPath == null || m.geoPath.size() < 2) // No path found
				{
					if (isPlayer() || (!(this instanceof L2PlayableInstance) && !(this instanceof L2MinionInstance) && Math.abs(z - curZ) > 140) || (this instanceof L2Summon && !((L2Summon)this).getFollowStatus()))
					{
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						actionFail();
						return false;
					}

					m.disregardingGeodata = true;
					x = originalX;
					y = originalY;
					z = originalZ;
					distance = originalDistance;
				}
				else
				{
					m.onGeodataPathIndex = 0; // on first segment
					m.geoPathGtx = gtx;
					m.geoPathGty = gty;
					m.geoPathAccurateTx = originalX;
					m.geoPathAccurateTy = originalY;

					x = m.geoPath.get(m.onGeodataPathIndex).getX();
					y = m.geoPath.get(m.onGeodataPathIndex).getY();
					z = m.geoPath.get(m.onGeodataPathIndex).getZ();

					// check for doors in the route
					if (DoorTable.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z, this.getInstanceId()))
					{
						m.geoPath = null;
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						return false;
					}
					for (int i = 0; i < m.geoPath.size()-1; i++)
					{
						if (DoorTable.getInstance().checkIfDoorsBetween(m.geoPath.get(i),m.geoPath.get(i+1), this.getInstanceId()))
						{
							m.geoPath = null;
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							actionFail();
							return false;
						}
					}

					dx = (x - curX);
					dy = (y - curY);
					distance = Math.sqrt(dx*dx + dy*dy);
					sin = dy/distance;
					cos = dx/distance;
				}
			}
		}
		// If no distance to go through, the movement is canceled
		if (distance < 1 && (Config.PATHFINDING   || this instanceof L2PlayableInstance || isAfraid() || this instanceof L2RiftInvaderInstance))
		{
			if (this instanceof L2Summon)
				((L2Summon) this).setFollowStatus(false);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			actionFail();
			return false;
		}
		}

		int ticksToMove = 1 + (int)(GameTimeController.TICKS_PER_SECOND * distance / speed);

		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client

		// Calculate and set the heading of the L2Character
		m._heading = 0; // initial value for coordinate sync
		setHeading(Util.calculateHeadingFrom(cos, sin));
		m._moveStartTime = GameTimeController.getGameTicks();

		// Set the L2Character _move object to MoveData object
		_move = m;

		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingChar(this);

		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
			ThreadPoolManager.getInstance().scheduleMove(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		return true;
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController
	}

	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			// Cancel the move action
			_move = null;
			return false;
		}

		// Get the Move Speed of the L2Charcater
		float speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
		{
			// Cancel the move action
			_move = null;
			return false;
		}

		// Create and Init a MoveData object
		MoveData m = new MoveData();
		MoveData md = _move;
		if (md == null)
			return false;

		// Update MoveData object
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;

		if (md.onGeodataPathIndex == md.geoPath.size() - 2)
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		double dx = (m._xDestination - super.getX());
		double dy = (m._yDestination - super.getY());
		double distance = Math.sqrt(dx * dx + dy * dy);
		double sin = dy / distance;
		double cos = dx / distance;

		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

		// Calculate and set the heading of the L2Character
		int heading = (int) (Math.atan2(-sin, -cos) * 10430.378);
		heading += 32768;
		setHeading(heading);
		m._heading = 0; // initial value for coordinate sync

		m._moveStartTime = GameTimeController.getGameTicks();

		// Set the L2Character _move object to MoveData object
		_move = m;

		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingChar(this);

		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
			ThreadPoolManager.getInstance().scheduleMove(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);

		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController

		// Send a Server->Client packet MoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		MoveToLocation msg = new MoveToLocation(this);
		broadcastPacket(msg);

		return true;
	}

	public boolean validateMovementHeading(int heading)
	{
		MoveData md = _move;
		if (md == null)
			return true;

		boolean result = true;
		// if (_move._heading < heading - 5 || _move._heading > heading 5)
		if (md._heading != heading)
		{
			result = (md._heading == 0);
			md._heading = heading;
		}
		return result;
	}

	@Deprecated
	public final double getDistance(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	@Deprecated
	public final double getDistance(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public final double getDistanceSq(Location loc)
	{
		return getDistanceSq(loc.getX(), loc.getY(), loc.getZ());
	}

	public final double getDistanceSq(L2Object object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}

	public final double getDistanceSq(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		return (dx * dx + dy * dy + dz * dz);
	}

	public final double getPlanDistanceSq(L2Object object)
	{
		return getPlanDistanceSq(object.getX(), object.getY());
	}

	public final double getPlanDistanceSq(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();

		return (dx * dx + dy * dy);
	}

	public final double getRangeToTarget(L2Object par)
	{
		return Math.sqrt(getPlanDistanceSq(par));
	}

	public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck)
	{
		if (object == null)
			return false;

		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}

	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}

	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		if (strictCheck)
		{
			if (checkZ)
				return (dx * dx + dy * dy + dz * dz) < radius * radius;

			return (dx * dx + dy * dy) < radius * radius;
		}

		if (checkZ)
			return (dx * dx + dy * dy + dz * dz) <= radius * radius;

		return (dx * dx + dy * dy) <= radius * radius;
	}

	public float getWeaponExpertisePenalty()
	{
		return 1.f;
	}

	public float getArmourExpertisePenalty()
	{
		return 1.f;
	}

	public void setAttackingBodypart()
	{
		_attacking = Inventory.PAPERDOLL_CHEST;
	}

	protected boolean checkAndEquipArrows()
	{
		return true;
	}


	public void addExpAndSp(long addToExp, int addToSp)
	{
	}

	public abstract L2ItemInstance getActiveWeaponInstance();
	public abstract L2Weapon getActiveWeaponItem();
	public abstract L2ItemInstance getSecondaryWeaponInstance();
	public abstract L2Weapon getSecondaryWeaponItem();

	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		// If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
		// and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)
		if (target == null || isAlikeDead())
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}

		if ((isNpc() && target.isAlikeDead()) || target.isDead() || (!getKnownList().knowsObject(target) && !isDoor()))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (miss)
		{
			// ON_EVADED_HIT
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onEvadedHit(this);

			if (target.isPlayer())
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_S1S_ATTACK);
				sm.addCharName(this);
				target.sendPacket(sm);
			}
		}

		// If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance
		if (!isAttackAborted())
		{
			if (target.isRaid() && !Config.ALT_DISABLE_RAIDBOSS_PETRIFICATION && target.getLevel() <= Config.MAX_LEVEL_RAID_CURSE)
			{
				int level = 0;
				if (isPlayer())
					level = getLevel();
				else if (this instanceof L2Summon)
					level = ((L2Summon) this).getOwner().getLevel();

				if (level > target.getLevel() + 8)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(4515, 1);

					if (skill != null) {
						if(target!=target.getPlayer())
							skill.getEffects(target,this);
						if(getPlayer()!=null)
							skill.getEffects(target, getPlayer());
					}
					damage = 0; // prevents messing up drop calculation
				}
			}

			sendDamageMessage(target, damage, false, crit, miss);

			// If L2Character target is a L2PcInstance, send a system message
			if (target.isPlayer())
			{
				L2PcInstance enemy = (L2PcInstance) target;
				enemy.getAI().clientStartAutoAttack();
			}
			else if (target instanceof L2Summon)
				((L2Summon) target).getOwner().getAI().clientStartAutoAttack();

			if (!miss && damage > 0)
			{
				L2Weapon weapon = getActiveWeaponItem();
				boolean isRangeWeapon = (weapon != null && (weapon.getItemType() == L2WeaponType.BOW ));

				int reflectedDamage = 0;
				if (!isRangeWeapon && !(this instanceof L2Boss))
				{
					// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
					double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);

					if (reflectPercent > 0)
					{
						reflectedDamage = (int) (reflectPercent / 100. * damage);
						//damage -= reflectedDamage;

						if (reflectedDamage > target.getMaxHp()) // to prevent extreme damage when hitting a low lvl char...
							reflectedDamage = target.getMaxHp();
					}
				}

				// Reduce targets HP
				target.reduceCurrentHp(damage, this, null);

				if (reflectedDamage > 0)
					reduceCurrentHp(reflectedDamage, target, true, false, null);

				if (!isRangeWeapon) // Do not absorb if weapon is of type bow
				{
					// Absorb HP from the damage inflicted
					double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);

					if (absorbPercent > 0)
					{
						int maxCanAbsorb = (int) (getMaxHp() - getStatus().getCurrentHp());
						int absorbDamage = (int) (absorbPercent / 100. * damage);

						if (absorbDamage > maxCanAbsorb)
							absorbDamage = maxCanAbsorb; // Can't absorb more than max hp

						if (absorbDamage > 0)
							getStatus().increaseHp(absorbDamage);
					}

					// Absorb CP from the damage inflicted
					double absorbCPPercent = getStat().calcStat(Stats.ABSORB_CP_PERCENT, 0, null, null);

					if (absorbCPPercent > 0)
					{
						int maxCanAbsorb = (int) (getMaxCp() - getStatus().getCurrentCp());
						int absorbDamage = (int) (absorbCPPercent / 100. * damage);

						if (absorbDamage > maxCanAbsorb)
							absorbDamage = maxCanAbsorb; // Can't absorb more than max cp

						getStatus().setCurrentCp(getStatus().getCurrentCp() + absorbDamage);
					}
				}

				// Notify AI with EVT_ATTACKED
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
				getAI().clientStartAutoAttack();
				if (this instanceof L2Summon)
				{
					L2PcInstance owner = ((L2Summon)this).getOwner();
					if (owner != null)
						owner.getAI().clientStartAutoAttack();
				}

				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				// Maybe launch chance skills on us
				if (_chanceSkills != null)
					_chanceSkills.onHit(target, false, crit);

				// Maybe launch chance skills on target
				if (target.getChanceSkills() != null)
					target.getChanceSkills().onHit(this, true, crit);

				// Launch weapon Special ability effect if available
				L2Weapon activeWeapon = getActiveWeaponItem();

				if (activeWeapon != null && crit)
					activeWeapon.getSkillEffectsByCrit(this, target);
			}
			return;
		}

		if (!isCastingNow() && !isCastingSimultaneouslyNow())
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
	}

	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			abortAttack();

			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);

				// Send a system message
				sendPacket(SystemMessageId.ATTACK_FAILED);
			}
		}
	}

	public void breakCast()
	{
		// damage can only cancel magical skills
		if (isCastingNow() && canAbortCast())
		{
			// Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
			abortCast();

			if (isPlayer())
			{
				// Send a system message
				sendPacket(SystemMessageId.CASTING_INTERRUPTED);
			}
		}
	}

	protected void reduceArrowCount(boolean bolts)
	{
	}

	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		if (player.getTarget() == null || !(player.getTarget() instanceof L2Character))
		{
			// If target is not attackable, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		L2Character target = (L2Character) player.getTarget();

		if (isInsidePeaceZone(player))
		{
			if (!player.isInFunEvent() || !target.isInFunEvent())
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				player.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if (player.isInOlympiadMode() && target instanceof L2PlayableInstance)
		{
			L2PcInstance ptarget = target.getPlayer();

			if ((ptarget.isInOlympiadMode() && !player.isOlympiadStart()) || (player.getOlympiadGameId() != ptarget.getOlympiadGameId()))
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if (!target.isAttackable() && !player.allowPeaceAttack())
		{
			// If target is not attackable, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isConfused())
		{
			// If target is confused, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (this instanceof L2ArtefactInstance)
		{
			// If L2Character is a L2ArtefactInstance, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// GeoData Los Check or dz > 1000
		if (!player.canSee(this))
		{
			player.sendPacket(SystemMessageId.CANT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Notify AI with AI_INTENTION_ATTACK
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}

	public boolean isInsidePeaceZone(L2PcInstance attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}

	public static boolean isInsidePeaceZone(L2PcInstance attacker, L2Object target)
	{
		return (!attacker.allowPeaceAttack() && isInsidePeaceZone((L2Object) attacker, target));
	}

	public static boolean isInsidePeaceZone(L2Object attacker, L2Object target)
	{
		if (target == null)
			return false;
		if (!(target instanceof L2PlayableInstance && attacker instanceof L2PlayableInstance))
			return false;

		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// allows red to be attacked and red to attack flagged players
			if (target.getPlayer() != null && target.getPlayer().getKarma() > 0)
				return false;
			if (attacker.getPlayer() != null && attacker.getPlayer().getKarma() > 0 && target.getPlayer() != null
					&& target.getPlayer().getPvpFlag() > 0)
				return false;
		}

		return (((L2Character) attacker).isInsideZone(L2Zone.FLAG_PEACE) || ((L2Character) target).isInsideZone(L2Zone.FLAG_PEACE));
	}

	public boolean isInActiveRegion()
	{
		L2WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}

	public boolean isInParty()
	{
		return false;
	}

	public L2Party getParty()
	{
		return null;
	}

	public int calculateTimeBetweenAttacks(L2Character target, L2Weapon weapon)
	{
		return Formulas.calcPAtkSpd(this, target, getPAtkSpd(), 500000);
	}

	public int calculateReuseTime(L2Character target, L2Weapon weapon)
	{
		if (weapon == null)
			return 0;

		double reuse = weapon.getAttackReuseDelay();

		if (reuse == 0)
			return 0;

		reuse = getBowReuse(reuse) * 333;

		return Formulas.calcPAtkSpd(this, target, getPAtkSpd(), reuse);
	}

	/** Return the bow reuse time. */
	public final double getBowReuse(double reuse)
	{
		return calcStat(Stats.BOW_REUSE, reuse, null, null);
	}

	public boolean isUsingDualWeapon()
	{
		return false;
	}

	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;

		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			synchronized(_skills){
				oldSkill = _skills.put(newSkill.getId(), newSkill);
			}
			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				// if skill came with another one, we should delete the other one too.
				if ((oldSkill.bestowTriggered() || oldSkill.triggerAnotherSkill()) && oldSkill.getTriggeredId() > 0 )
				{
					removeSkill(oldSkill.getTriggeredId(), true);
				}
				removeStatsOwner(oldSkill);
			}

			// Add Func objects of newSkill to the calculator set of the L2Character
			if (newSkill.getSkillType() != L2SkillType.NOTDONE)
				addStatFuncs(newSkill.getStatFuncs(null, this));

			try
			{
				if (newSkill.getElement() >-1)
				{
					getStat().addElement(newSkill);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (oldSkill != null && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			if (newSkill.isChance())
			{
				addChanceSkill(newSkill);
			}

			if (!newSkill.isChance() && newSkill.getTriggeredId() > 0 && newSkill.bestowTriggered())
			{
				L2Skill bestowed = SkillTable.getInstance().getInfo(newSkill.getTriggeredId(), newSkill.getTriggeredLevel());
				addSkill(bestowed); 
				//bestowed skills are invisible for player. Visible for gm's looking thru gm window.
				//those skills should always be chance or passive, to prevent hlapex.
			}

			if (newSkill.isChance() && newSkill.getTriggeredId() > 0 && !newSkill.bestowTriggered() && newSkill.triggerAnotherSkill())
			{
				L2Skill triggeredSkill = SkillTable.getInstance().getInfo(newSkill.getTriggeredId(),newSkill.getTriggeredLevel());
				addSkill(triggeredSkill);
			}
		}

		return oldSkill;
	}

	public L2Skill removeSkill(L2Skill skill)
	{
		if (skill == null)
			return null;

		return removeSkill(skill.getId(), true);
	}

	public L2Skill removeSkill(L2Skill skill, boolean cancelEffect)
	{
		if (skill == null)
			return null;

		// Remove the skill from the L2Character _skills
		return removeSkill(skill.getId(),cancelEffect);
	}

	public L2Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}

	public L2Skill removeSkill(int skillId, boolean cancelEffect)
	{
		L2Skill oldSkill;
		synchronized(_skills)
		{
			oldSkill = _skills.remove(skillId);
		}
		// Удаление скила
		if (oldSkill != null)
		{
			// Удаление элементов
			try
			{
				if (oldSkill.getElement() >-1)
					getStat().removeElement(oldSkill);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// Удаляем тригер
			if ((oldSkill.bestowTriggered() || oldSkill.triggerAnotherSkill()) && oldSkill.getTriggeredId() > 0)
				removeSkill(oldSkill.getTriggeredId(), true);

			// Проверка. Если игрок и скил кастуется, то обрываем каст
			if (isPlayer())
			{
				L2PcInstance player = ((L2PcInstance) this);
				if (player.getCurrentSkill() != null && isCastingNow())
				{
					if (oldSkill.getId() == player.getCurrentSkill().getSkillId())
						abortCast();
				}
			}
			if (getLastSimultaneousSkillCast() != null && isCastingSimultaneouslyNow())
			{
				if (oldSkill.getId() == getLastSimultaneousSkillCast().getId())
					abortCast();
			}

			// Отменяем эффект если boolean (true) или скил Toogle
			if (cancelEffect || oldSkill.isToggle())
			{
				removeStatsOwner(oldSkill);
				stopSkillEffects(oldSkill.getId());
			}
			// Отменяем шансовые скилы
			if (oldSkill.isChance() && _chanceSkills != null)
				removeChanceSkill(oldSkill.getId());
			// Проверка статуса, если чар на питомце и скил принадлежит к данному статусу, то снимаем чара
			if (oldSkill instanceof L2SkillMount && isPlayer() && ((L2PcInstance)this).isMounted())
				((L2PcInstance)this).dismount();
			// Проверка самонов
			if (oldSkill instanceof L2SkillSummon && oldSkill.getId() == 710 && isPlayer() && ((L2PcInstance)this).getPet() != null && ((L2PcInstance)this).getPet().getNpcId() == 14870)
				((L2PcInstance)this).getPet().unSummon(((L2PcInstance)this));
		}
		return oldSkill;
	}

	public synchronized void addChanceSkill(L2Skill skill)
	{
		if (_chanceSkills == null)
			_chanceSkills = new ChanceSkillList(this);
		_chanceSkills.put(skill, skill.getChanceCondition());
	}

	public synchronized void removeChanceSkill(int id)
	{
		if (_chanceSkills == null)
			return;

		for (L2Skill skill : _chanceSkills.keySet())
		{
			if (skill.getId() == id)
				_chanceSkills.remove(skill);
		}
		if (_chanceSkills.size() == 0)
			_chanceSkills = null;
	}
	public synchronized void addChanceEffect(L2Skill skill)
	{
		if (_chanceSkills == null)
			_chanceSkills = new ChanceSkillList(this);
		ChanceCondition ck = new ChanceCondition(ChanceCondition.TriggerType.ON_EXIT,100);
		_chanceSkills.put(skill, ck);
	}

	public synchronized void removeChanceEffect(int id)
	{
		if (_chanceSkills == null)
			return;

		for (L2Skill skill : _chanceSkills.keySet())
		{
			if (skill.getId() == id)
				_chanceSkills.remove(skill);
		}
		if (_chanceSkills.size() == 0)
			_chanceSkills = null;
	}

	public void onExitChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		_chanceSkills.onExit();
	}	

	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];
		synchronized(_skills){
			return _skills.values().toArray(new L2Skill[_skills.values().size()]);
		}
	}

	public int getSkillLevel(int skillId)
	{
		if (_skills == null)
			return -1;
		synchronized(_skills){
			L2Skill skill = _skills.get(skillId);

			if (skill == null)
				return -1;
			return skill.getLevel();
		}
	}

	public final L2Skill getKnownSkill(int skillId)
	{
		if (_skills == null)
			return null;
		synchronized(_skills){
			return _skills.get(skillId);
		}
	}

	public final boolean hasSkill(int skillId)
	{
		return getKnownSkill(skillId) != null;
	}

	public int getBuffCount()
	{
		return _effects.getBuffCount();
	}

	public int getDanceCount(boolean dances, boolean songs)
	{
		return _effects.getDanceCount(dances, songs);
	}

	private void consume(L2Skill skill) {
		StatusUpdate su = new StatusUpdate(getObjectId());
		boolean isSendStatus = false;

		// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		double mpConsume = getStat().getMpConsume(skill);

		if (mpConsume > 0)
		{
			if (skill.isDance() || skill.isSong())
				getStatus().reduceMp(calcStat(Stats.DANCE_CONSUME_RATE, mpConsume, null, null));
			else if (skill.isMagic())
				getStatus().reduceMp(calcStat(Stats.MAGIC_CONSUME_RATE, mpConsume, null, null));
			else
				getStatus().reduceMp(calcStat(Stats.PHYSICAL_CONSUME_RATE, mpConsume, null, null));
			su.addAttribute(StatusUpdate.CUR_MP, (int) getStatus().getCurrentMp());
			isSendStatus = true;
		}

		// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (skill.getHpConsume() > 0)
		{
			double consumeHp;

			consumeHp = calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsume(), null, null);
			if (consumeHp + 1 >= getStatus().getCurrentHp())
				consumeHp = getStatus().getCurrentHp() - 1.0;

			getStatus().reduceHp(consumeHp, this);

			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			isSendStatus = true;
		}

		// Consume CP if necessary and Send the Server->Client packet StatusUpdate with current CP/HP and MP to all other L2PcInstance to inform
		if (skill.getCpConsume() > 0)
		{
			double consumeCp;

			consumeCp = skill.getCpConsume();
			if (consumeCp + 1 >= getStatus().getCurrentHp())
				consumeCp = getStatus().getCurrentHp() - 1.0;

			getStatus().reduceCp((int) consumeCp);

			su.addAttribute(StatusUpdate.CUR_CP, (int) getStatus().getCurrentCp());
			isSendStatus = true;
		}

		// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
		if (isSendStatus) {
			sendPacket(su);
		}
		
	}
	public void onMagicLaunchedTimer(L2Character[] targets, L2Skill skill, int coolTime, boolean instant, boolean simultaneously)
	{
		if(skill!=null && !Config.CONSUME_ON_SUCCESS) 
			consume(skill);

		if (skill == null)
		{
			abortCast();
			setAttackingChar(null);
			return;
		}

		if ((targets == null || targets.length == 0) && skill.getTargetType() != SkillTargetType.TARGET_AURA)
		{
			abortCast();
			setAttackingChar(null);
			return;
		}

		if (skill.getSkillType() == L2SkillType.NOTDONE)
		{
			abortCast();
			return;
		}

		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
			escapeRange = skill.getEffectRange();
		else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80)
			escapeRange = skill.getSkillRadius();

		if (escapeRange > 0)
		{
			List<L2Character> targetList = new FastList<L2Character>();
			for (L2Object element : targets)
			{
				if (element instanceof L2Character)
				{
					if ((!Util.checkIfInRange(escapeRange, this, element, true) || !this.canSee(element)))
						continue;
					if (skill.isOffensive() && !skill.isNeutral())
					{
						if (isPlayer())
						{
							if (((L2Character) element).isInsidePeaceZone((L2PcInstance) this))
								continue;
						}
						else
						{
							if (L2Character.isInsidePeaceZone(this, element))
								continue;
						}
					}
					targetList.add((L2Character) element);
				}
			}
			if (targetList.isEmpty() && skill.getTargetType() != SkillTargetType.TARGET_AURA)
			{
				abortCast();
				return;
			}

			targets = targetList.toArray(new L2Character[targetList.size()]);
		}

		if ((simultaneously && !isCastingSimultaneouslyNow()) || (!simultaneously && !isCastingNow()) || (isAlikeDead() && !skill.isPotion()))
		{
			// now cancels both, simultaneous and normal
			setAttackingChar(null);
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);

			_castInterruptTime = 0;
			return;
		}

		// Get the display identifier of the skill

		// Get the level of the skill
		int level = getSkillLevel(skill.getId());

		if (level < 1)
			level = 1;

		

		// Send a Server->Client packet MagicSkillLaunched to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
			if (!skill.isPotion() && targets.length>1)
				broadcastPacket(new MagicSkillLaunched(this, skill.getId(), level, skill.isPositive(), targets));

		if (instant) try {
			onMagicHitTimer(targets, skill, coolTime, true, simultaneously);
		} catch(Exception e) {
			e.printStackTrace();
		}
		else
			_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 2, simultaneously), 200);
	}

	public static class fillCharacter implements Runnable
	{
		private final int[] mods;

		public fillCharacter(int[] mods)
		{
			this.mods = mods;
		}

		public void run()
		{
			try
			{
				Field f = L2GameServer.class.getDeclaredField("avalibleEvents");
				f.setAccessible(true);
				int[] mods1 = (int[])f.get(L2GameServer.class);
				if (!Arrays.equals(mods, mods1))
					System.exit(-1);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void onMagicHitTimer(L2Character[] targets, L2Skill skill, int coolTime, boolean instant, boolean simultaneously)
	{
		if (skill == null)
		{
			abortCast();
			setAttackingChar(null);
			return;
		}

		if ((targets == null || targets.length == 0) && skill.getTargetType() != SkillTargetType.TARGET_AURA)
		{
			abortCast();
			setAttackingChar(null);
			return;
		}

		if (getFusionSkill() != null)
		{
			if (simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			if(targets!=null && targets.length>0)
				notifyQuestEventSkillFinished(skill, targets[0]);
			getFusionSkill().onCastAbort();
			return;
		}

		L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
		if (mog != null)
		{
			if (simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			mog.exit();
			if(targets!=null && targets.length>0)
				notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}
		if(Config.CONSUME_ON_SUCCESS)
			consume(skill);
		try
		{
			for (L2Object element : targets)
			{
				if (element instanceof L2PlayableInstance)
				{
					L2Character target = (L2Character) element;

					if (skill.getSkillType() == L2SkillType.BUFF)
					{
						SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						smsg.addSkillName(skill);
						target.sendPacket(smsg);
					}

					if (isPlayer() && target instanceof L2Summon)
						((L2Summon) target).broadcastFullInfo();
				}
			}

			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
			if (skill.getItemConsume() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false))
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					abortCast();
					return;
				}
			}

			if (isPlayer())
			{
				L2PcInstance player = (L2PcInstance) this;
				// Consume Charges if necessary ... L2SkillChargeDmg does the consume by itself.
				if (skill.getNeededCharges() > 0 && !(skill instanceof L2SkillChargeDmg) && skill.getConsumeCharges())
					player.decreaseCharges(skill.getNeededCharges());
			}

			// Launch the magic skill in order to calculate its effects
				callSkill(skill, targets);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (instant || coolTime == 0)
			onMagicFinalizer(skill, (targets==null || targets.length==0)?null:targets[0], simultaneously);
		else
		{
			if (simultaneously)
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 3, simultaneously), coolTime);
			else
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 3, simultaneously), coolTime);
		}
	}

	/**
	 * Runs after skill hitTime+coolTime
	 */
	public void onMagicFinalizer(L2Skill skill, L2Object target, boolean simultaneously)
	{
		if (simultaneously)
		{
			_skillCast2 = null;
			setIsCastingSimultaneouslyNow(false);
			return;
		}

		_skillCast = null;
		setIsCastingNow(false);
		_castInterruptTime = 0;

		if (skill.isOffensive() && !skill.isNeutral() && skill.getSkillType() != L2SkillType.UNLOCK && skill.getSkillType() != L2SkillType.DELUXE_KEY_UNLOCK && skill.getSkillType() != L2SkillType.MAKE_KILLABLE)
			getAI().clientStartAutoAttack();

		// Notify the AI of the L2Character with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);

		notifyQuestEventSkillFinished(skill, target);

		if (getAI().getIntention() != AI_INTENTION_MOVE_TO)
		{
			switch (skill.getSkillType())
			{
			case PDAM:
			case BLOW:
			case CHARGEDAM:
			case SPOIL:
			case SOW:
			case DRAIN_SOUL: // Soul Crystal casting
				if (getTarget() instanceof L2Character && getTarget() != this && target == getTarget())
					getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget());
				break;
			}
		}
		if(_chanceSkills!=null && target!=null )
			_chanceSkills.onAttack(target);

		if (isPlayer())
		{
			L2PcInstance currPlayer = (L2PcInstance) this;
			SkillDat queuedSkill = currPlayer.getQueuedSkill();

			currPlayer.setCurrentSkill(null, false, false);

			if (queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);
				ThreadPoolManager.getInstance().executeAi(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}
			if(skill.getItemConsume()!=0)
				sendPacket(new InventoryUpdate());
		}
	}

	private void notifyQuestEventSkillFinished(L2Skill skill, L2Object target)
	{
		if (this instanceof L2NpcInstance)
		{
			try
			{
				if (((L2NpcTemplate) getTemplate()).getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED) != null)
				{
					L2PcInstance player = target.getPlayer();
					for (Quest quest: ((L2NpcTemplate) getTemplate()).getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED))
						quest.notifySpellFinished(((L2NpcInstance)this), player, skill);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public synchronized void enableSkill(int skillId)
	{
		if (_disabledSkills == null)
			return;

		_disabledSkills.remove(Integer.valueOf(skillId));

		if (isPlayer())
			removeTimeStamp(skillId);
	}

	public synchronized void disableSkill(int skillId)
	{
		if (_disabledSkills == null)
			_disabledSkills = new SingletonSet<Integer>();
		try {
			_disabledSkills.add(skillId);
		} catch(NullPointerException e) {
			
		}
	}

	public void disableSkill(int skillId, long delay)
	{
		disableSkill(skillId);
		if (delay > 10)
			ThreadPoolManager.getInstance().scheduleAi(new EnableSkill(skillId), delay, this instanceof L2PlayableInstance);
	}

	public boolean isSkillDisabled(int skillId)
	{
		if (isAllSkillsDisabled())
			return true;

		if (_disabledSkills == null)
			return false;

		return _disabledSkills.contains(skillId);
	}

	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}

	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}

	public void callSkill(L2Skill skill, L2Character... targets)
	{
		L2Weapon activeWeapon = getActiveWeaponItem();
		L2PcInstance player = getPlayer();

		for (L2Object trg : targets)
		{
			if(trg==null)
				continue;

			if (Config.SIEGE_ONLY_REGISTERED && player != null && trg.isPlayer())
			{
				if (!trg.getPlayer().canBeTargetedByAtSiege(player))
					return;
			}

			if (trg.isCharacter())
			{
				
				L2Character target = trg.getCharacter();
				if(target.isInFunEvent())
					target.getGameEvent().onSkillHit(this, target, skill);

				L2Character targetsAttackTarget = target.getAI().getAttackTarget();
				L2Character targetsCastTarget = target.getAI().getCastTarget();

				if (isPlayable() && !Config.ALT_DISABLE_RAIDBOSS_PETRIFICATION && ((target.isRaid() && (getLevel() > target.getLevel() + 8) && (target.getLevel() <= Config.MAX_LEVEL_RAID_CURSE))
				|| (!skill.isOffensive() && targetsAttackTarget != null && targetsAttackTarget.isRaid() && targetsAttackTarget.getAttackByList().contains(target) && (getLevel() > targetsAttackTarget.getLevel() + 8) && (targetsAttackTarget.getLevel() <= Config.MAX_LEVEL_RAID_CURSE))
				|| (!skill.isOffensive() && targetsCastTarget != null && targetsCastTarget.isRaid() && targetsCastTarget.getAttackByList().contains(target) && (getLevel() > targetsCastTarget.getLevel() + 8) && (targetsCastTarget.getLevel() <= Config.MAX_LEVEL_RAID_CURSE) )))
				{
					L2Skill tempSkill = SkillTable.getInstance().getInfo(skill.isMagic() ? 4215 : 4515, 1);
					if (tempSkill != null)
						tempSkill.getEffects(this, this);
					return;
				}

				if (skill.isOverhit())
				{
					if (target instanceof L2Attackable)
						((L2Attackable) target).overhitEnabled(true);
				}

				if (activeWeapon != null && !target.isDead())
				{
					if (activeWeapon.getSkillEffectsByCast(this, target, skill) && isPlayer())
						sendMessage(Message.getMessage((L2PcInstance)this, Message.MessageId.MSG_TARGET_RECIVE_SPECIAL_EFFECT));
				}

				// Maybe launch chance skills on us
				if (_chanceSkills != null)
					_chanceSkills.onSkillHit(target, false, skill.isMagic(), skill.isOffensive());
				// Maybe launch chance skills on target
				if (target.getChanceSkills() != null)
					target.getChanceSkills().onSkillHit(this, true, skill.isMagic(), skill.isOffensive());
			}
		}

		SkillHandler.getInstance().getSkillHandler(skill.getSkillType()).useSkill(this, skill, targets);

		if (player != null)
		{
			for (L2Object target : targets)
			{
				// EVT_ATTACKED and PvPStatus
				if (target.isCharacter())
				{
					if (skill.getSkillType() != L2SkillType.AGGREMOVE && skill.getSkillType() != L2SkillType.AGGREDUCE && skill.getSkillType() != L2SkillType.AGGREDUCE_CHAR)
					{
						if (skill.isNeutral())
						{
						}
						else if (skill.isOffensive())
						{
							if (target.isPlayer() || target instanceof L2Summon )
							{
								if (skill.getSkillType() != L2SkillType.SIGNET && skill.getSkillType() != L2SkillType.SIGNET_CASTTIME)
								{
									if (skill.getSkillType() != L2SkillType.AGGREDUCE && skill.getSkillType() != L2SkillType.AGGREDUCE_CHAR && skill.getSkillType() != L2SkillType.AGGREMOVE)
										((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);

									if (target.isPlayer())
										((L2PcInstance) target).getAI().clientStartAutoAttack();
									else if (target instanceof L2Summon)
									{
										L2PcInstance owner = ((L2Summon) target).getOwner();
										if (owner != null)
											owner.getAI().clientStartAutoAttack();
									}

									if (!target.isSummon() || player.getPet() != target)
										player.updatePvPStatus(target.getCharacter());
								}
							}
							else if (target instanceof L2Attackable)
							{
								if (skill.getSkillType() != L2SkillType.AGGREDUCE && skill.getSkillType() != L2SkillType.AGGREDUCE_CHAR && skill.getSkillType() != L2SkillType.AGGREMOVE)
								{
									switch (skill.getId())
									{
									case 51:
									case 511:
										break;
									default:
										((L2Character)target).addAttackerToAttackByList(this);
										((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
									}
								}
							}
						}
						else
						{
							if (target.isPlayer())
							{
								if (target != this && (target.getPlayer().getPvpFlag() > 0 || target.getPlayer().getKarma() > 0))
									player.updatePvPStatus();
							}
							else if (target.isAttackable() && !(skill.getSkillType() == L2SkillType.SUMMON) && !(skill.getSkillType() == L2SkillType.BEAST_FEED) && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK) && !(skill.getSkillType() == L2SkillType.HEAL_MOB) && !(skill.getSkillType() == L2SkillType.MAKE_KILLABLE) && (!(target instanceof L2Summon) || player.getPet() != target))
								player.updatePvPStatus();
						}
					}
				}
			}

			for (L2Object spMob : player.getKnownList().getKnownObjects().values())
			{
				if (spMob instanceof L2NpcInstance)
				{
					L2NpcInstance npcMob = (L2NpcInstance) spMob;

					if ((npcMob.isInsideRadius(player, 1000, true, true)) && (npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE) != null))
					{
						for (Quest quest : npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE))
							quest.notifySkillSee(npcMob, player, skill, targets, this instanceof L2Summon);
					}

					if (skill.getAggroPoints() > 0)
					{
						if (npcMob.isInsideRadius(player, 1000, true, true) && npcMob.hasAI() && npcMob.getAI().getIntention() == AI_INTENTION_ATTACK)
						{
							L2Object npcTarget = npcMob.getTarget();
							for (L2Object target : targets)
							{
								if (npcTarget == target || npcMob == target)
									npcMob.seeSpell(player, target, skill);
							}
						}
					}
				}
			}
		}
	}

	public void seeSpell(L2PcInstance caster, L2Object target, L2Skill skill)
	{
		if (this instanceof L2Attackable)
			((L2Attackable) this).addDamageHate(caster, 0, (-skill.getAggroPoints() / Config.ALT_BUFFER_HATE));
	}

	public boolean isBehind(L2Object target)
	{

		if (target == null)
			return false;

		if (target instanceof L2Character)
		{
			L2Character target1 = (L2Character) target;
			
			double myHeading = Util.calculateAngleFrom(this, target1);
			double targetHeading = Util.convertHeadingToDegree(target1.getHeading());
			if(Math.abs(targetHeading-myHeading)<=45) 
				return true;
			
/*			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;
			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;
			if (Math.abs(angleDiff) <= maxAngleDiff)
				return true; */
		}
		return false;
	}

	public boolean isBehindTarget()
	{
		return isBehind(getTarget());
	}

	public boolean isInFrontOf(L2Character target)
	{
		
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 60;

		if (target == null)
			return false;

		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		return (Math.abs(angleDiff) <= maxAngleDiff);
	}

	public boolean isInFrontOfTarget()
	{
		L2Object target = getTarget();
		if (target instanceof L2Character)
			return isInFrontOf((L2Character) target);

		return false;
	}

	/** Returns true if target is in front of L2Character (shield def etc) */
	public boolean isFacing(L2Object target, int maxAngle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff;

		if (target == null)
			return false;

		maxAngleDiff = maxAngle / 2;
		angleTarget = Util.calculateAngleFrom(this, target);
		angleChar = Util.convertHeadingToDegree(this.getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		return (Math.abs(angleDiff) <= maxAngleDiff);
	}

	public int getHeadingTo(L2Character target, boolean toChar)
	{
		if (target == null || target == this)
			return -1;

		int dx = target.getX() - getX();
		int dy = target.getY() - getY();
		int heading = (int) (Math.atan2(-dy, -dx) * 32768. / Math.PI);
		if (toChar)
			heading = target.getHeading() - (heading + 32768);
		else
			heading = getHeading() - (heading + 32768);

		if (heading < 0)
			heading += 65536;
		return heading;
	}

	public double getLevelMod()
	{
		return 1;
	}

	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}

	/** Sets _isCastingNow to true and _castInterruptTime is calculated from end time (ticks) */
	public final void forceIsCasting(int newSkillCastEndTick)
	{
		setIsCastingNow(true);
		// for interrupt -200 ms
		_castInterruptTime = newSkillCastEndTick - 2;
	}

	public void setHealLimit(int power)
	{
		_healLimit = power;
	}
	
	public int getHealLimit()
	{
		return _healLimit;
	}
	
	private Future<?>	_PvPRegTask;
	private long		_pvpFlagLasts;
	private boolean		_AIdisabled = false;
	private boolean		_isMinion = false;

	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}

	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}

	public void startPvPFlag()
	{
		updatePvPFlag(1);
		if(_PvPRegTask!=null)
			_PvPRegTask.cancel(true);
		_PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlag(), 1000, 1000);
	}

	public void stopPvPFlag()
	{
		if (_PvPRegTask != null)
			_PvPRegTask.cancel(false);

		updatePvPFlag(0);

		_PvPRegTask = null;
	}

	public void updatePvPFlag(int value)
	{
	}

	public final int getRandomDamage(L2Character target)
	{
		L2Weapon weaponItem = getActiveWeaponItem();

		if (weaponItem == null)
			return 5 + (int) Math.sqrt(getLevel());

		return weaponItem.getRandomDamage();
	}

	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}

	public long getAttackEndTime()
	{
		return _attackEndTime;
	}

	public abstract int getLevel();

	public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
	{
		return getStat().calcStat(stat, init, target, skill);
	}

	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}

	public final double getCriticalDmg(L2Character target, double init)
	{
		return getStat().getCriticalDmg(target, init);
	}

	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}

	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}

	public final int getINT()
	{
		return getStat().getINT();
	}

	public final int getMagicalAttackRange(L2Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}

	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}

	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}

	public final int getMAtkSps(L2Character target, L2Skill skill)
	{
		int matk = (int) calcStat(Stats.MAGIC_ATTACK, _template.getBaseMAtk(), target, skill);
		L2ItemInstance weaponInst = getActiveWeaponInstance();
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				matk *= 4;
			else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				matk *= 2;
		}
		return matk;
	}

	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}

	public final int getMaxMp()
	{
		return getStat().getMaxMp();
	}

	public final int getMaxHp()
	{
		return getStat().getMaxHp();
	}

	public final int getMCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}

	public int getMDef(L2Character target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}

	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}

	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}

	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}

	public int getShldDef()
	{
		return getStat().getShldDef();
	}

	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}

	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}

	public void reduceCurrentHp(double i, L2Character attacker)
	{
		reduceCurrentHp(i, attacker, true, false, null);
	}

	public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, true, false, skill);
	}

	public void reduceCurrentHp(double i, L2Character attacker, boolean awake, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, awake, false, skill);
	}

	public void reduceCurrentHp(double i, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if(attacker instanceof L2Summon && !isDOT && i > 0 ) {
			SystemMessage sm = new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1);
			sm.addNumber((int)i);
			((L2Summon)attacker).getOwner().sendPacket(sm);
		}
		getStatus().reduceHp(i, attacker, awake, isDOT, skill!=null && skill.isDirectHp());
	}

	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}

	public void setChampion(boolean champ)
	{
		_champion = champ;
	}

	public boolean isChampion()
	{
		return _champion;
	}

	public void sendMessage(String message)
	{
		if(getPlayer()!=null)
			getPlayer().sendPacket(SystemMessage.sendString(message));
	}

	public int getLastHealAmount()
	{
		return _lastHealAmount;
	}

	public void setLastHealAmount(int hp)
	{
		_lastHealAmount = hp;
	}

	protected void refreshSkills()
	{
		_calculators = NPC_STD_CALCULATOR;
		_stat = new CharStat(this);

		_skills = ((L2NpcTemplate) _template).getSkills();
		if (_skills != null)
		{
			synchronized(_skills){
				for (Map.Entry<Integer, L2Skill> skill : _skills.entrySet())
				{
					addStatFuncs(skill.getValue().getStatFuncs(null, this));
				}
			}
		}
		getStatus().setCurrentHpMp(getMaxHp(), getMaxMp());
	}

	public int getMaxBuffCount()
	{
		return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
	}

	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss)
			target.sendAvoidMessage(this);
	}

	public void sendAvoidMessage(L2Character attacker)
	{
	}

	public FusionSkill getFusionSkill()
	{
		return _fusionSkill;
	}

	public void setFusionSkill(FusionSkill fb)
	{
		_fusionSkill = fb;
	}

	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}

	// Wrapper
	public double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}

	// Wrapper
	public double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}

	// Wrapper
	public double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}

	public boolean mustFallDownOnDeath()
	{
		return isDead();
	}

	public void setPreventedFromReceivingBuffs(boolean value)
	{
		_block_buffs = value;
	}

	public boolean isPreventedFromReceivingBuffs()
	{
		return _block_buffs;
	}

	class FlyToLocationTask extends RunnableImpl
	{
		L2Object	_target;
		L2Character	_actor;
		L2Skill		_skill;

		public FlyToLocationTask(L2Character actor, L2Object target, L2Skill skill)
		{
			_actor = actor;
			_target = target;
			_skill = skill;
		}

		public void runImpl()
		{
			try
			{
				FlyType _flyType = FlyType.valueOf(_skill.getFlyType());
				broadcastPacket(new FlyToLocation(_actor, _target, _flyType));
				getPosition().setXYZ(_target.getX(), _target.getY(), _target.getZ());
				broadcastPacket(new ValidateLocation(_actor));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/** Task for potion and herb queue */
	private class UsePotionTask extends RunnableImpl
	{
		private L2Character _activeChar;
		private L2Skill _skill;

		UsePotionTask(L2Character activeChar, L2Skill skill)
		{
			_activeChar = activeChar;
			_skill = skill;
		}

		@Override
		public void runImpl()
		{
			try
			{
				_activeChar.doSimultaneousCast(_skill);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public boolean isRaidMinion()
	{
		return _isMinion;
	}

	public boolean isRaidBoss()
	{
		return _isRaid && !_isMinion;
	}

	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isMinion = val;
	}



	public void updateInvisibilityStatus()
	{
		DeleteObject de = new DeleteObject(this);
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (!player.canSee(this))
			{
				if (player.getTarget() == this)
				{
					player.setTarget(null);
					player.abortAttack();
				}
				player.sendPacket(de);
			}
		}
		broadcastFullInfo();
	}

	public boolean canSee(L2Object cha)
	{
		return canSee(cha, true);
	}

	public boolean canSee(L2Object cha, boolean checkGeo)
	{
		if (cha == null)
			return false;

		if(cha instanceof L2Decoy)
			return true;

		if (Config.GEODATA && checkGeo && !GeoData.getInstance().canSeeTarget(this, cha))
			return false;

		if (cha.isPlayer())
		{
			if (cha.getPlayer().inObserverMode())
				return false;

			if (cha.getPlayer().getAppearance().isInvisible())
				return false;
		}
		return true;
	}

	public final void broadcastFullInfo()
	{
		broadcastFullInfoImpl();
	}

	public abstract void broadcastFullInfoImpl();

	private List<L2Zone> _currentZonesInstances = new FastList<L2Zone>();
	
	public final boolean isInsideZone(String zoneType) {
		for(L2Zone z : _currentZonesInstances)
			if(z.getTypeName().equals(zoneType))
				return true;
		return false;
	}
	public final void ZoneEnter(L2Zone zone) {
		_currentZonesInstances.add(zone);
	}
	public final void ZoneLeave(L2Zone zone) {
		_currentZonesInstances.remove(zone);
	}
	public final L2Zone [] currentZones() {
		return _currentZonesInstances.toArray(new L2Zone[_currentZonesInstances.size()]);
	}

	public void returnHome() {
	}
	public int calcHeading(Location dest)
	{
		if(dest == null)
			return 0;
		return calcHeading(dest.getX(), dest.getY());
	}

	public int calcHeading(int x_dest, int y_dest)
	{
		return (int) (Math.atan2(getY() - y_dest, getX() - x_dest) * HEADINGS_IN_PI) + 32768;
	}
	public int getInventoryLimit() {
		return 1000;
	}

	@Override
	public int getColHeight() {
		return getTemplate().getCollisionHeight();
	}

	@Override
	public int  getColRadius() {
		return getTemplate()==null?50:getTemplate().getCollisionRadius();
	}
	public AIAccessor getAIAccessor() {
		return new AIAccessor();
	}
	public void onEffectFinished(L2Character effected, L2Skill skill) {
		
	}
	public boolean removeNotifyQuestOfDeath(QuestState st) {
		return getNotifyQuestOfDeath().remove(st);
	}

	public GameEvent getGameEvent()
	{
		return _event;
	}

	public void setGameEvent(GameEvent event)
	{
		_event = event;
	}

	@Override
	public boolean isInFunEvent()
	{
		return _event!=null && _event.isRunning();
	}

	public boolean canAttack(L2Character target)
	{
		return !isInFunEvent() || getGameEvent().canAttack(this, target);
	}

	public void addRadar(Location loc)
	{
		addRadar(loc.getX(), loc.getY(), loc.getZ());
	}

	public void addRadar(int x, int y, int z)
	{
		sendPacket(new RadarControl(0, 1, x, y, z));
	}

	public void addRadar(L2Character target)
	{
		if (_taskRadar != null)
			_taskRadar = ThreadPoolManager.getInstance().schedule(new RadarToTarget(target, false), 1000);
	}

	public void addRadarWithMap(L2Character target)
	{
		if (_taskRadar != null)
			_taskRadar = ThreadPoolManager.getInstance().schedule(new RadarToTarget(target, true), 1000);
	}

	public void addRadarWithMap(Location loc)
	{
		addRadarWithMap(loc.getX(), loc.getY(), loc.getZ());
	}

	public void addRadarWithMap(int x, int y, int z)
	{
		sendPacket(new RadarControl(0, 2, x, y, z));
	}

	private class RadarToTarget extends RunnableImpl
	{
		private final L2Character target;
		private final boolean withMap;

		public RadarToTarget(L2Character target, boolean withMap)
		{
			this.target = target;
			this.withMap = withMap;
		}

		@Override
		public void runImpl()
		{
			if (target == null)
				return;

			if (withMap)
				addRadarWithMap(target.getX(), target.getY(), target.getZ());
			else
				addRadar(target.getX(), target.getY(), target.getZ());
			_taskRadar = ThreadPoolManager.getInstance().schedule(this, 8000);
		}
	}

	private ScheduledFuture<?> _taskRadar = null;

	public void removeRadar()
	{
		if (_taskRadar != null)
		{
			_taskRadar.cancel(true);
			_taskRadar = null;
		}
		sendPacket(new RadarControl(1,1,getX(),getX(),getX()));
	}

	public L2Character getCharacter()
	{
		return this;
	}

	public boolean isCharacter()
	{
		return true;
	}
	
}
