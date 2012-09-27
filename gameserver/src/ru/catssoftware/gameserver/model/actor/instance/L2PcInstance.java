package ru.catssoftware.gameserver.model.actor.instance;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.*;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.ai.L2PlayerAI;
import ru.catssoftware.gameserver.ai.L2SummonAI;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.*;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.gmaccess.handlers.editchar;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.handler.ItemHandler;
import ru.catssoftware.gameserver.handler.skillhandlers.SummonFriend;
import ru.catssoftware.gameserver.handler.skillhandlers.TakeCastle;
import ru.catssoftware.gameserver.handler.skillhandlers.TakeFort;
import ru.catssoftware.gameserver.instancemanager.*;
import ru.catssoftware.gameserver.instancemanager.leaderboards.ArenaManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.L2Skill.SkillTargetType;
import ru.catssoftware.gameserver.model.actor.appearance.PcAppearance;
import ru.catssoftware.gameserver.model.actor.knownlist.PcKnownList;
import ru.catssoftware.gameserver.model.actor.reference.ClearableReference;
import ru.catssoftware.gameserver.model.actor.reference.ImmutableReference;
import ru.catssoftware.gameserver.model.actor.stat.PcStat;
import ru.catssoftware.gameserver.model.actor.status.PcStatus;
import ru.catssoftware.gameserver.model.base.*;
import ru.catssoftware.gameserver.model.entity.*;
import ru.catssoftware.gameserver.model.entity.faction.FactionMember;
import ru.catssoftware.gameserver.model.itemcontainer.*;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.State;
import ru.catssoftware.gameserver.model.restriction.AvailableRestriction;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.model.zone.L2TradeZone;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.clientpackets.RequestActionUse;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.network.serverpackets.EffectInfoPacket.EffectInfoPacketList;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.conditions.ConditionGameTime;
import ru.catssoftware.gameserver.skills.conditions.ConditionPlayerHp;
import ru.catssoftware.gameserver.skills.effects.EffectFusion;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.taskmanager.AttackStanceTaskManager;
import ru.catssoftware.gameserver.taskmanager.OfflineManager;
import ru.catssoftware.gameserver.taskmanager.SQLQueue;
import ru.catssoftware.gameserver.templates.chars.L2PcTemplate;
import ru.catssoftware.gameserver.templates.item.*;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Broadcast;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.gameserver.util.PcAction;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.geometry.Point3D;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.LinkedBunch;
import ru.catssoftware.util.SingletonList;
import ru.catssoftware.util.SingletonMap;
import ru.catssoftware.util.StatsSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// import ru.catssoftware.gameserver.model.BypassManager;
/*import ru.catssoftware.gameserver.model.BypassManager.BypassType;
import ru.catssoftware.gameserver.model.BypassManager.DecodedBypass;
import ru.catssoftware.gameserver.model.BypassManager.EncodedBypass;
*/
//import ru.catssoftware.gameserver.network.serverpackets.TradeOtherDone;


public class L2PcInstance extends L2PlayableInstance
{
	/* параметры игрока */
	public int			_bbsMultisell						= 0;
	public boolean			_inWorld							= false;
	public boolean 			_inSepulture						= false;
	private boolean			_showTraders						= true;
	private boolean			_showBuffAnimation					= true;
//	private boolean 		_isWinLastHero 						= false;
	private long			_endOfflineTime						= 0;
	/* параметры доступа */
	private boolean			_GmStatus							= false;
	private boolean			_AllowFixRes						= false;
	private boolean			_AllowAltG							= false;
	private boolean			_AllowPeaceAtk						= false;
	private boolean			_isBanned							= false;
	/* параметры эвентов */
	private int				_dmKills							= 0;
	
	// Character Skill SQL String Definitions:
    private static final String	RESTORE_SKILLS_FOR_CHAR			= "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String	ADD_NEW_SKILL					= "INSERT INTO character_skills (charId,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)";
	private static final String	UPDATE_CHARACTER_SKILL_LEVEL	= "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String	DELETE_SKILL_FROM_CHAR			= "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String	DELETE_CHAR_SKILLS				= "DELETE FROM character_skills WHERE charId=? AND class_index=?";

	// Character Skill Save SQL String Definitions:
	private static final String	ADD_SKILL_SAVE					= "REPLACE INTO character_skills_save (charId,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String	RESTORE_SKILL_SAVE				= "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay,systime FROM character_skills_save WHERE charId=? AND class_index=? AND restore_type=? ORDER BY buff_index ASC";
	private static final String	DELETE_SKILL_SAVE				= "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";

	// Character Character SQL String Definitions:
	private static final String	UPDATE_CHARACTER				= "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,race=?,classid=?,deletetime=?,title=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,in_jail=?,jail_timer=?,newbie=?,nobless=?,pledge_rank=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?, pccaffe_points=?,  isBanned=?, hwid=? WHERE charId=?";
	private static final String	RESTORE_CHARACTER				= "SELECT account_name, charId, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, fame, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, rec_have, rec_left, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, in_jail, jail_timer, newbie, nobless, pledge_rank, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally, clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,  pccaffe_points,  isBanned,hwid FROM characters WHERE charId=?";
	private static final String CREATE_CHARACTER				= "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,pledge_rank,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	// Character ServiceHero Status String Definitions:
	private static final String	RESTORE_HEROSERVICE				= "SELECT enddate FROM character_herolist WHERE charId=?";
	private static final String	UPDATE_HEROSERVICE				= "UPDATE character_herolist SET enddate=? WHERE charId=?";
	
	// Character Subclass SQL String Definitions:
	private static final String	RESTORE_CHAR_SUBCLASSES			= "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
	private static final String	ADD_CHAR_SUBCLASS				= "REPLACE INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String	UPDATE_CHAR_SUBCLASS			= "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
	private static final String	DELETE_CHAR_SUBCLASS			= "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";

	// Character Henna SQL String Definitions:
	private static final String	RESTORE_CHAR_HENNAS				= "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String	ADD_CHAR_HENNA					= "REPLACE INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String	DELETE_CHAR_HENNA				= "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	private static final String	DELETE_CHAR_HENNAS				= "DELETE FROM character_hennas WHERE charId=? AND class_index=?";

	// Character Shortcut SQL String Definitions:
	private static final String	DELETE_CHAR_SHORTCUTS			= "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";

	// Character Recommendation SQL String Definitions:
	private static final String	RESTORE_CHAR_RECOMS				= "SELECT charId,target_id FROM character_recommends WHERE charId=?";
	private static final String	ADD_CHAR_RECOM					= "REPLACE INTO character_recommends (charId,target_id) VALUES (?,?)";
	private static final String	DELETE_CHAR_RECOMS				= "DELETE FROM character_recommends WHERE charId=?";

	private static final String	CREATE_CHAR_DATA				= "INSERT INTO character_data values (?,?,?)";
	private static final String	STORE_CHAR_DATA				    = "UPDATE character_data set valueData=? where charId=? and valueName=?";
	private static final String	LOAD_CHAR_DATA			        = "SELECT valueName,valueData from character_data where charId=?";
	
	
	public static final int		REQUEST_TIMEOUT					= 15;

	public static final int		STORE_PRIVATE_NONE				= 0;
	public static final int		STORE_PRIVATE_SELL				= 1;
	public static final int		STORE_PRIVATE_BUY				= 3;
	public static final int		STORE_PRIVATE_MANUFACTURE		= 5;
	public static final int		STORE_PRIVATE_PACKAGE_SELL		= 8;
	
	
	/** The table containing all minimum level needed for each Expertise (None, D, C, B, A, S, S80)*/
	private static final int[]	EXPERTISE_LEVELS				=
	{
			SkillTreeTable.getInstance().getExpertiseLevel(0), //NONE
			SkillTreeTable.getInstance().getExpertiseLevel(1),	//D
			SkillTreeTable.getInstance().getExpertiseLevel(2),	//C
			SkillTreeTable.getInstance().getExpertiseLevel(3),	//B
			SkillTreeTable.getInstance().getExpertiseLevel(4),	//A
			SkillTreeTable.getInstance().getExpertiseLevel(5)	//S
	};

	private static final int[]	COMMON_CRAFT_LEVELS				= { 5, 20, 28, 36, 43, 49, 55, 62 };
	public int []_seeds = new int[3];
	public int _lastSkill = 0;
	private StatsSet _dynaicData = new StatsSet();
	
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		public L2PcInstance getPlayer()
		{
			return L2PcInstance.this;
		}

		public void doPickupItem(L2Object object)
		{
			L2PcInstance.this.doPickupItem(object);
		}

		public void doInteract(L2Character target)
		{
			L2PcInstance.this.doInteract(target);
		}

		@Override
		public void doAttack(L2Character target)
		{
			_inWorld = true;
			super.doAttack(target);

			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
			L2Effect silentMove = getPlayer().getFirstEffect(L2EffectType.SILENT_MOVE);
			if (silentMove != null)
				silentMove.exit();
		}

		@Override
		public void doCast(L2Skill skill)
		{
			_inWorld = true;
			super.doCast(skill);
			
			// Cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
			if (skill == null)
				return;
			if (!skill.isOffensive())
				return;

			if (getPlayer().isSilentMoving() && skill.getId()!=51 && skill.getId()!=511)
			{
				L2Effect silentMove = getPlayer().getFirstEffect(L2EffectType.SILENT_MOVE);
				if (silentMove != null)
					silentMove.exit();
			}

			switch (skill.getTargetType())
			{
			case TARGET_GROUND:
				return;
				default:
				{
					for (L2CubicInstance cubic : getCubics().values())
					{
						if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
							cubic.doAction();
					}
				}
				break;
			}
		}
	}

	private L2GameClient					_client;
	private final PcAppearance				_appearance;
	protected boolean						_protectedSitStand					= false;
	private long							_expBeforeDeath;
	private int								_karma;
	private int								_pvpKills;
	private int								_pkKills;
	private byte							_pvpFlag;

	/** The Siege state of the L2PcInstance */
	private byte							_siegeState							= 0;
	private int								_lastCompassZone;
	private boolean							_isIn7sDungeon						= false;
	private int								_subPledgeType						= 0;

	/** L2PcInstance's pledge rank*/
	private int								_pledgeRank;

	/** Level at which the player joined the clan as an accedemy member*/
	private int								_lvlJoinedAcademy					= 0;
	private int								_curWeightPenalty					= 0;
	private long							_deleteTimer;
	private PcInventory						_inventory;
	private PcWarehouse						_warehouse;
	private PcFreight						_freight;
	
	/** true if the L2PcInstance is sitting */
	private boolean							_waitTypeSitting;

	/** true if the L2PcInstance is using the relax skill */
	private boolean							_relax;

	/** true if the L2PcInstance is in a boat */
	private boolean							_inBoat;

	/** Last NPC Id talked on a quest */
	private int								_questNpcObject						= 0;

	/** Bitmask used to keep track of one-time/newbie quest rewards */
	private int								_newbie;
	private Map<String, QuestState>			_quests								= new SingletonMap<String, QuestState>();
	private ShortCuts						_shortCuts;
	private MacroList						_macroses;
	private TradeList						_activeTradeList;
	private ItemContainer					_activeWarehouse;
	private L2ManufactureList				_createList;
	private TradeList						_sellList;
	private TradeList						_buyList;
	private List<L2PcInstance>				_snoopListener; // list of GMs snooping this player
	private List<L2PcInstance>				_snoopedPlayer; // list of players being snooped by this GM

	/*Private Store type (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int								_privatestore;
	private ClassId							_skillLearningClassId;
	private final L2Henna[]					_henna								= new L2Henna[3];
	private int								_hennaSTR;
	private int								_hennaINT;
	private int								_hennaDEX;
	private int								_hennaMEN;
	private int								_hennaWIT;
	private int								_hennaCON;
	private boolean							_isRidingStrider					= false;
	private boolean							_isRidingRedStrider					= false;
	private boolean							_isFlyingWyvern						= false;
	private L2Summon						_summon								= null;
	private L2Decoy							_decoy								= null;
	private L2TamedBeastInstance			_tamedBeast							= null;
	private L2Radar							_radar;
	private boolean							_lookingForParty;
	private boolean							_partyMatchingAllLevels;
	private int								_partyMatchingRegion;
	private L2PartyRoom						_partyRoom;	
	private L2Party							_party;
	private int								_clanId;
	private L2Clan							_clan;
	private int								_apprentice							= 0;
	private int								_sponsor							= 0;
	private long							_clanJoinExpiryTime;
	private long							_clanCreateExpiryTime;
	private long							_onlineTime;
	private long							_onlineBeginTime;
	private boolean							_messageRefusal						= false;	// message refusal mode
	private boolean							_dietMode							= false;	// ignore weight penalty
	private boolean							_tradeRefusal						= false;	// Trade refusal
	private boolean							_exchangeRefusal					= false;	// Exchange refusal
	private L2PcInstance					_activeRequester;
	private long							_requestExpireTime					= 0;
	private L2Request						_request;
	private L2ItemInstance					_arrowItem;

	// Used for protection after teleport
	private long							_protectEndTime						= 0;

	// protects a char from agro mobs when getting up from fake death
	private long							_recentFakeDeathEndTime				= 0;

	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equipped) */
	private L2Weapon						_fistsWeaponItem;

	private long							_uptime;
	private String							_accountName;
	private final Map<Integer, String>		_chars								= new SingletonMap<Integer, String>();

	/** The table containing all L2RecipeList of the L2PcInstance */
	private Map<Integer, L2RecipeList>		_dwarvenRecipeBook					= new FastMap<Integer, L2RecipeList>();
	private Map<Integer, L2RecipeList>		_commonRecipeBook					= new FastMap<Integer, L2RecipeList>();

	private int								_mountType;
	private int								_mountNpcId;
	private int								_mountLevel;

	/** The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5)*/
	private int								_expertiseIndex;
	private int								_itemExpertiseIndex;	
	private int								_expertisePenalty					= 0;

	private boolean							_isEnchanting						= false;
	private L2ItemInstance					_activeEnchantItem					= null;
	private L2ItemInstance					_activeEnchantAttrItem				= null;
	private static final byte				ONLINE_STATE_LOADED					= 0;
	private static final byte				ONLINE_STATE_ONLINE					= 1;
	private static final byte				ONLINE_STATE_DELETED				= 2;
	private byte 							_isOnline							= ONLINE_STATE_LOADED;
	
	protected boolean						_inventoryDisable					= false;

	protected Map<Integer, L2CubicInstance>	_cubics								= new SingletonMap<Integer, L2CubicInstance>().setShared();

	/** The L2FolkInstance corresponding to the last Folk which one the player talked. */
	private L2FolkInstance					_lastFolkNpc						= null;

	protected final Map<Integer, Integer>	_activeSoulShots					= new SingletonMap<Integer, Integer>().setShared();
	private int								_clanPrivileges						= 0;

	/** L2PcInstance's pledge class (knight, Baron, etc.)*/
	private int								_pledgeClass						= 0;

	/** Location before entering Observer Mode */
	private int								_obsX;
	private int								_obsY;
	private int								_obsZ;
	private boolean							_observerMode						= false;
	private int								_observMode							= 0;

	/** Total amount of damage dealt during a olympiad fight */
	private int								_olyDamage							= 0;

	
	public int								_telemode							= 0;

	/** new loto ticket **/
	private int								_loto[]								= new int[5];

	/** new race ticket **/
	private int								_race[]								= new int[2];

	private BlockList						_blockList;
	private L2FriendList					_friendList;

	private boolean							_fishing							= false;
	private int								_fishx								= 0;
	private int								_fishy								= 0;
	private int								_fishz								= 0;

	private int								_wantsPeace							= 0;

	//Death Penalty Buff Level
	private int								_deathPenaltyBuffLevel				= 0;

	// Self resurrect during siege
	private boolean							_charmOfCourage						= false;
	private boolean							_canUseCharmOfCourageRes			= true;
	private boolean							_canUseCharmOfCourageItem			= true;



	private boolean							_hero								= false;
	private boolean							_noble								= false;
	private boolean							_inOlympiadMode						= false;
	private boolean							_olympiadStart						= false;
	private int								_olympiadGameId						= -1;
	private int								_olympiadSide						= -1;
	private int								_olympiadOpponentId					= 0;

	/** Duel */
	private int								_duelState							= Duel.DUELSTATE_NODUEL;
	private boolean							_isInDuel							= false;
	private int								_duelId								= 0;
	private int								_noDuelReason						= 0;

	/** ally with ketra or varka related vars*/
	private int								_alliedVarkaKetra					= 0;

	/** The list of sub-classes this character has. */
	private Map<Integer, SubClass>			_subClasses;
	protected int							_baseClass;
	protected int							_activeClass;
	protected int							_classIndex							= 0;

	/** data for mounted pets */
	private int								_controlItemId;
	private L2PetData						_data;
	private int								_curFeed;
	protected Future<?>						_mountFeedTask;
	private ScheduledFuture<?>				_dismountTask;

	private long							_lastAccess;
	private int								_boatId;

	private ScheduledFuture<?>				_taskRentPet;
	private ScheduledFuture<?>				_taskWater;
	private L2BoatInstance					_boat;
	private Point3D							_inBoatPosition;



	private L2Fishing						_fishCombat;

	/** Stored from last ValidatePosition **/
	private Point3D							_lastServerPosition;

	/** Previous coordinate sent to party in ValidatePosition **/
	private Point3D							_lastPartyPosition;

	/** The number of recommendation obtained by the L2PcInstance */
	private int								_recomHave;

	/** The number of recommendation that the L2PcInstance can give */
	private int								_recomLeft;

	/** Date when recommendation points were updated last time */
	private long							_lastRecomUpdate;

	/** List with the recommendations that I've give */
	private List<Integer>					_recomChars							= new SingletonList<Integer>();

	private boolean							_inCrystallize;

	private boolean							_inCraftMode;

	private boolean							_isSummoning;
	/** Current skill in use. Note that L2Character has _lastSkillCast, but this has the button presses */
	private SkillDat						_currentSkill;
	private SkillDat						_currentPetSkill;

	/** Skills queued because a skill is already in progress */
	private SkillDat						_queuedSkill;

	/** Store object used to summon the strider you are mounting **/
	private int								_mountObjectID						= 0;

	/** character VIP **/

	private boolean							_inJail								= false;
	private long							_jailTimer							= 0;

	private boolean							_maried								= false;
	private int								_partnerId							= 0;
	private int								_coupleId							= 0;
	private boolean							_engagerequest						= false;
	private int								_engageid							= 0;
	private boolean							_maryrequest						= false;
	private boolean							_maryaccepted						= false;
	private int								_clientRevision						= 0;
	private FactionMember					_faction;
	private boolean							_isAway								= false;
	public boolean							_buffBlocked;
	public String							_originalTitleAway;

	/* Flag to disable equipment/skills while wearing formal wear **/
	private boolean							_IsWearingFormalWear				= false;

	private L2StaticObjectInstance			_objectSittingOn;

	// Absorbed Souls

	// Force charges
	private int								_charges							= 0;
	private ScheduledFuture<?>				_chargeTask							= null;

	// WorldPosition used by TARGET_SIGNET_GROUND
	private Point3D							_currentSkillWorldPosition;

	public int								_fame								= 0;
	private ScheduledFuture<?>				_fameTask;

	// Vitality Level of this L2PcInstance

	// Censor status
	private boolean							_isOfflineTrade						= false;

	
	private boolean							_miniMapOpen;
	
	// Все стартовые сообщения пользователя
	private FastList<String>				_userMessages						= new FastList<String>();
	
	// Очередь отправленных сообщений
	private FastList<String>				_messageQueue 						= new FastList<String>();
	private  int 							_queueDepth							= 5 + Rnd.get(3);
	

	private StatsSet						_characterData						= new StatsSet();
	
	private String							_hwid;
	private int 							_pccaffe;
	public  int 							_lastUseItem;
	/** Skill casting information (used to queue when several skills are cast in a short time) **/
	public class SkillDat
	{
		private L2Skill	_skill;
		private boolean	_ctrlPressed;
		private boolean	_shiftPressed;

		protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
		{
			_skill = skill;
			_ctrlPressed = ctrlPressed;
			_shiftPressed = shiftPressed;
		}

		public boolean isCtrlPressed()
		{
			return _ctrlPressed;
		}

		public boolean isShiftPressed()
		{
			return _shiftPressed;
		}

		public L2Skill getSkill()
		{
			return _skill;
		}

		public int getSkillId()
		{
			return (getSkill() != null) ? getSkill().getId() : -1;
		}
	}

	@Override
	public final boolean isAllSkillsDisabled()
	{
		return super.isAllSkillsDisabled() || _protectedSitStand;
	}

	@Override
	public final boolean isAttackingDisabled()
	{
		return super.isAttackingDisabled() || _protectedSitStand;
	}

	/** ShortBuff clearing Task */
	private ScheduledFuture<?>	_shortBuffTask	= null;

	private final class ShortBuffTask implements Runnable
	{
		public void run()
		{
			sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
		}
	}

	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create a new L2PcInstance with an account name </li>
	 * <li>Set the name, the Hair Style, the Hair Color and  the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param hairStyle The hair style Identifier of the L2PcInstance
	 * @param hairColor The hair color Identifier of the L2PcInstance
	 * @param face The face type Identifier of the L2PcInstance
	 *
	 * @return The L2PcInstance added to the database or null
	 *
	 */
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face,
			boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);

		// Set the name of the L2PcInstance
		player.setName(name);

		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());

		//Kept for backwards compabitility.
		player.setNewbie(1);

		// Add the player in the characters table of the database
		boolean ok = player.createDb();

		if (!ok)
			return null;

		return player;
	}

	public String getAccountName()
	{
		if (getClient() == null)
			return "disconnected";
		return getClient().getAccountName();
	}
	
	public String getHWid()
	{
		if (getClient() == null) 
			return _hwid;
		_hwid = getClient().getHWid();
		return _hwid;
	}

	public String getHost()
	{
		if (getClient() == null)
			return "disconnected";
		return getClient().getHostAddress();
	}

	public int getRelation(L2PcInstance target)
	{
		int result = 0;

		if (getClan() != null)
			result |= RelationChanged.RELATION_CLAN_MEMBER;

		if (isClanLeader())
			result |= RelationChanged.RELATION_LEADER;

		L2Party party = getParty();
		if (party != null && party == target.getParty())
		{
			result |= RelationChanged.RELATION_HAS_PARTY;

			switch (party.getPartyMembers().indexOf(this))
			{
				case 0:
					result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
					break;
				case 1:
					result |= RelationChanged.RELATION_PARTY4; // 0x8
					break;
				case 2:
					result |= RelationChanged.RELATION_PARTY3+RelationChanged.RELATION_PARTY2+RelationChanged.RELATION_PARTY1; // 0x7
					break;
				case 3:
					result |= RelationChanged.RELATION_PARTY3+RelationChanged.RELATION_PARTY2; // 0x6
					break;
				case 4:
					result |= RelationChanged.RELATION_PARTY3+RelationChanged.RELATION_PARTY1; // 0x5
					break;
				case 5:
					result |= RelationChanged.RELATION_PARTY3; // 0x4
					break;
				case 6:
					result |= RelationChanged.RELATION_PARTY2+RelationChanged.RELATION_PARTY1; // 0x3
					break;
				case 7:
					result |= RelationChanged.RELATION_PARTY2; // 0x2
					break;
				case 8:
					result |= RelationChanged.RELATION_PARTY1; // 0x1
					break;
			}
		}

		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
				result |= RelationChanged.RELATION_ENEMY;
			else
				result |= RelationChanged.RELATION_ALLY;
			if (getSiegeState() == 1)
				result |= RelationChanged.RELATION_ATTACKER;
		}

		if (getClan() != null && target.getClan() != null)
		{
			if (target.getSubPledgeType() != L2Clan.SUBUNIT_ACADEMY && getSubPledgeType() != L2Clan.SUBUNIT_ACADEMY && target.getClan().isAtWarWith(getClan().getClanId()))
			{
				if (target.getClient() != null)
				{
					result |= RelationChanged.RELATION_1SIDED_WAR;
					if (getClan().isAtWarWith(target.getClan().getClanId()))
						result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		return result;
	}

	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	/**
	 * Constructor of L2PcInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance </li>
	 * <li>Set the name of the L2PcInstance</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the account including this L2PcInstance
	 *
	 */
	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		getStat(); // init stats
		getStatus(); // init status
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();

		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;

		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());

		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		getWarehouse();
		getFreight();

	}

	@Override
	public final PcKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new PcKnownList(this);

		return (PcKnownList) _knownList;
	}

	public static void disconnectIfOnline(int objectId)
	{
		L2PcInstance onlinePlayer = L2World.getInstance().findPlayer(objectId);
		
		if (onlinePlayer == null)
			return;

		onlinePlayer.store(true);
		if(onlinePlayer.isOfflineTrade())
			OfflineManager.getInstance().removeTrader(onlinePlayer);

		new Disconnection(onlinePlayer).defaultSequence(true);
	}
	
	@Override
	public final PcStat getStat()
	{
		if (_stat == null)
			_stat = new PcStat(this);

		return (PcStat) _stat;
	}

	@Override
	public final PcStatus getStatus()
	{
		if (_status == null)
			_status = new PcStatus(this);

		return (PcStatus) _status;
	}

	public final PcAppearance getAppearance()
	{
		return _appearance;
	}

	/**
	 * Return the base L2PcTemplate link to the L2PcInstance.<BR><BR>
	 */
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}

	/** Return the L2PcTemplate link to the L2PcInstance. */
	@Override
	public final L2PcTemplate getTemplate()
	{
		return (L2PcTemplate) super.getTemplate();
	}

	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}

	/**
	 * Return the AI of the L2PcInstance (create it if necessary).<BR><BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}

	/**
	 * Return the _newbie rewards state of the L2PcInstance.<BR><BR>
	 */
	public int getNewbie()
	{
		return _newbie;
	}

	/**
	 * Set the _newbie rewards state of the L2PcInstance.<BR><BR>
	 *
	 * @param newbieRewards The Identifier of the _newbie state<BR><BR>
	 *
	 */
	public void setNewbie(int newbieRewards)
	{
		_newbie = newbieRewards;
	}

	//L2EMU_EDIT
	/**
	 * Check if the L2PcInstance already have take the itemId for the questId.<BR><BR> 
	 */
/*	public boolean getQuestItem(int questId, int itemId)
	{
		Connection con = null;
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection(con);

			PreparedStatement statement = con.prepareStatement("SELECT itemId FROM character_quests_item WHERE charId = ? AND questId = ? AND itemId = ?");
			statement.setInt(1, this._charId);
			statement.setInt(2, questId);
			statement.setInt(3, itemId);
			ResultSet rset = statement.executeQuery();
			boolean getItem = rset.next();
			rset.close();
			con.close();
			return getItem;
		}
		catch (Exception e)
		{
			_log.warn("Could not read char quest items: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}

		return false;
	}

	public void setQuestItem(int questId, int itemId)
	{
		Connection con = null;
		try
		{
			// Insert the quest itemid 
			con = L2DatabaseFactory.getInstance().getConnection(con);

			PreparedStatement statement = con.prepareStatement("INSERT INTO character_quests_item (charId, questId, itemID) values(?,?,?);");
			statement.setInt(1, this._charId);
			statement.setInt(2, questId);
			statement.setInt(3, itemId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not write char quest items: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
*/	//L2EMU_EDIT

	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
		
	}
	public void checkInventory() {
	    for(L2ItemInstance item : getInventory().getItems())
	    	if(item.isEquipped() && item.getItem() instanceof L2Armor)
	    		if(!Config.isAllowArmor(this, (L2Armor)item.getItem()))
	    			getInventory().unEquipItemInSlot(item.getLocationSlot());
	}

	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}

	public boolean isInStoreMode()
	{
		return (getPrivateStoreType() > 0);
	}

	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}

	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}

	/**
	 * Return a table containing all Common L2Recipe of the L2PcInstance.<BR><BR>
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}

	/**
	 * Return a table containing all Dwarf L2Recipe of the L2PcInstance.<BR><BR>
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}

	/**
	 * Add a new L2Recipe to the table _commonrecipebook containing all L2Recipe of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2RecipeList to add to the _recipebook
	 *
	 */
	public void registerCommonRecipeList(L2RecipeList recipe)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
	}

	/**
	 * Add a new L2Recipe to the table _recipebook containing all L2Recipe of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2Recipe to add to the _recipebook
	 *
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
	}

	/**
	 * @param RecipeID The Identifier of the L2Recipe to check in the player's recipe books
	 *
	 * @return
	 * <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			return true;
		else return _commonRecipeBook.containsKey(recipeId);
	}

	/**
	 * Tries to remove a L2Recipe from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2Recipe of the L2PcInstance <BR><BR>
	 *
	 * @param RecipeID The Identifier of the L2Recipe to remove from the _recipebook
	 *
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			_dwarvenRecipeBook.remove(recipeId);
		else if (_commonRecipeBook.containsKey(recipeId))
			_commonRecipeBook.remove(recipeId);
		else
			_log.warn("Attempted to remove unknown RecipeList: " + recipeId);

		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
	}

	/**
	 * Returns the Id for the last talked quest NPC.<BR><BR>
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}

	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}

	/**
	 * Return the QuestState object corresponding to the quest name.<BR><BR>
	 *
	 * @param quest The name of the quest
	 *
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}

	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.<BR><BR>
	 *
	 * @param qs The QuestState to add to _quest
	 *
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}

	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.<BR><BR>
	 *
	 * @param quest The name of the quest
	 *
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}

	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len + 1];
		System.arraycopy(questStateArray, 0, tmp, 0, len);
		tmp[len] = state;
		return tmp;
	}

	/**
	 * Return a table containing all Quest in progress from the table _quests.<BR><BR>
	 */
	public Quest[] getAllActiveQuests()
	{
		LinkedBunch<Quest> quests = new LinkedBunch<Quest>();

		for (String qname : _quests.keySet())
		{
			QuestState qs = _quests.get(qname);
			if(qs == null || qs.getQuest()==null) {
				_quests.remove(qname);
				continue;
			}
			int questId = qs.getQuest().getQuestIntId();
			if ((questId > 999) || (questId < 1))
				continue;

			if (!qs.isStarted() && !Config.DEVELOPER)
				continue;

			quests.add(qs.getQuest());
		}

		return quests.moveToArray(new Quest[quests.size()]);
	}

	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR><BR>
	 *
	 * @param npcId The Identifier of the L2Attackable attacked
	 *
	 */
	public QuestState[] getQuestsForAttacks(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
		{
			// Check if the Identifier of the L2Attackable attck is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[] { getQuestState(quest.getName()) };
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR><BR>
	 *
	 * @param npcId The Identifier of the L2Attackable killed
	 *
	 */
	public QuestState[] getQuestsForKills(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[] { getQuestState(quest.getName()) };
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.<BR><BR>
	 *
	 * @param npcId The Identifier of the NPC
	 *
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (Quest quest : quests)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
							states = new QuestState[]
							{ getQuestState(quest.getName()) };
						else
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
					}
				}
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
			event = "";
		if (!_quests.containsKey(quest))
			return retval;
		QuestState qs = getQuestState(quest);
		if (qs == null && event.length() == 0)
			return retval;
		if (qs == null)
		{
			Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
				return retval;
			qs = q.newQuestState(this);
		}
		if (qs != null)
		{
			if (getLastQuestNpcObject() > 0)
			{
				L2Object object = getKnownList().getKnownObject(getLastQuestNpcObject());
				if (object instanceof L2NpcInstance && isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false))
				{
					L2NpcInstance npc = (L2NpcInstance) object;
					QuestState[] states = getQuestsForTalk(npc.getNpcId());
					if (states != null)
					{
						for (QuestState state : states)
						{
							if (state.getQuest().getName().equals(qs.getQuest().getName()))
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
									showQuestWindow(quest, State.getStateName(qs.getState()));

								retval = qs;
							}
						}
						sendPacket(new QuestList(this));
					}
				}
			}
		}

		return retval;
	}

	/**
	 * FIXME: move this from L2PcInstance, there is no reason to have this here
	 * @param questId
	 * @param stateId
	 */
	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
		String content = HtmCache.getInstance().getHtm(path,this);

		if (content != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}

		sendPacket(ActionFailed.STATIC_PACKET);
	}

	private ShortCuts getShortCuts()
	{
		if (_shortCuts == null)
			_shortCuts = new ShortCuts(this);
		
		return _shortCuts;
	}
	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR><BR>
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return getShortCuts().getAllShortCuts();
	}

	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).<BR><BR>
	 *
	 * @param slot The slot in which the shortCuts is equipped
	 * @param page The page of shortCuts containing the slot
	 *
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return getShortCuts().getShortCut(slot, page);
	}

	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR><BR>
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		getShortCuts().registerShortCut(shortcut);
	}

	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR><BR>
	 */
	public void deleteShortCut(int slot, int page)
	{
		getShortCuts().deleteShortCut(slot, page);
	}

	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR><BR>
	 */
	public void registerMacro(L2Macro macro)
	{
		getMacroses().registerMacro(macro);
	}

	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR><BR>
	 */
	public void deleteMacro(int id)
	{
		getMacroses().deleteMacro(id);
	}

	/**
	 * Return all L2Macro of the L2PcInstance.<BR><BR>
	 */
	public MacroList getMacroses()
	{
		if (_macroses == null)
			_macroses = new MacroList(this);
		return _macroses;
	}

	/**
	 * Set the siege state of the L2PcInstance.<BR><BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
		broadcastRelationChanged();
	}

	/**
	 * Get the siege state of the L2PcInstance.<BR><BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}

	/**
	 * Set the PvP Flag of the L2PcInstance.<BR><BR>
	 */
	public void setPvpFlag(int pvpFlag)
	{
		if(_pvpFlag != (byte) pvpFlag) {  
			_pvpFlag = (byte) pvpFlag;
			broadcastFullInfo();
		}
	}

	public byte getPvpFlag()
	{
		return _pvpFlag;
	}

	@Override
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
			return;

		setPvpFlag(value);

		sendPacket(new UserInfo(this));
		broadcastRelationChanged();
	}

	@Override
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

		if (Config.ALLOW_WATER)
			checkWaterState();

		if (isInsideZone(L2Zone.FLAG_SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
				return;
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
			sendPacket(cz);
		}
		else if (isInsideZone(L2Zone.FLAG_PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
				return;
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
			sendPacket(cz);
		}
		else if (isIn7sDungeon())
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
				return;
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
			sendPacket(cz);
		}
		else if (isInsideZone(L2Zone.FLAG_PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
				return;
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
			sendPacket(cz);
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
				return;
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
				updatePvPStatus();
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
			sendPacket(cz);
		}
	}

	/**
	 * Return true if the L2PcInstance can Craft Dwarven Recipes.<BR><BR>
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}

	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}

	public boolean hasDwarvenCrystallize()
	{
		return getSkillLevel(L2Skill.SKILL_CRYSTALLIZE) >= 1;
	}
	
	/**
	 * Return true if the L2PcInstance can Craft Dwarven Recipes.<BR><BR>
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}

	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}

	/**
	 * Return the PK counter of the L2PcInstance.<BR><BR>
	 */
	public int getPkKills()
	{
		return _pkKills;
	}

	/**
	 * Set the PK counter of the L2PcInstance.<BR><BR>
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	/**
	 * Return the _deleteTimer of the L2PcInstance.<BR><BR>
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR><BR>
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	/**
	 * Return the current weight of the L2PcInstance.<BR><BR>
	 */
	public int getCurrentLoad()
	{
		return getInventory().getTotalWeight();
	}

	/**
	 * Return date of last update of recomPoints
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}

	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}

	/**
	 * Return the number of recommendation obtained by the L2PcInstance.<BR><BR>
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}

	/**
	 * Increment the number of recommendation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
			_recomHave++;
	}

	/**
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
			_recomHave = 255;
		else if (value < 0)
			_recomHave = 0;
		else
			_recomHave = value;
	}

	/**
	 * Return the number of recommendation that the L2PcInstance can give.<BR><BR>
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}

	/**
	 * Increment the number of recommendation that the L2PcInstance can give.<BR><BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
			_recomLeft--;
	}

	public void giveRecom(L2PcInstance target)
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.error("Failed updating character recommendations.", e);
			}
			finally
			{
				try
				{
					if (con != null)
						con.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}

	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}

	/**
	 * Set the exp of the L2PcInstance before a death
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}

	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}

	/**
	 * Return the Karma of the L2PcInstance.<BR><BR>
	 */
	public int getKarma()
	{
		return _karma;
	}

	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR><BR>
	 */
	public void setKarma(int karma)
	{
		if (karma < 0)
			karma = 0;
		if (_karma == 0 && karma > 0)
		{
			for (L2Object object : getKnownList().getKnownObjects().values())
			{
				if (!(object instanceof L2GuardInstance))
					continue;

				if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
			}
		}

		// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
		else if (_karma > 0 && karma == 0)
			setKarmaFlag(0);
		_karma = karma;
		broadcastKarma();
	}

	/**
	 * Return the max weight that the L2PcInstance can load.<BR><BR>
	 */
	public int getMaxLoad()
	{
		return (int) (calcStat(Stats.MAX_LOAD, 69000, this, null) * Config.ALT_WEIGHT_LIMIT * Formulas.calcWeightRuneModifed(this));
	}

	public int getExpertisePenalty()
	{
		return _expertisePenalty;
	}

	public int getWeightPenalty()
	{
		if (_dietMode)
			return 0;
		return _curWeightPenalty;
	}

	/**
	 * Update the overloaded status of the L2PcInstance.<BR><BR>
	 */
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		int newWeightPenalty = 0;

		if (maxLoad > 0)
		{
			setIsOverloaded(getCurrentLoad() > maxLoad && !_dietMode);
			int weightproc = getCurrentLoad() * 1000 / maxLoad;
			weightproc = (int) calcStat(Stats.WEIGHT_LIMIT, weightproc, this, null);

			if (weightproc < 500 || _dietMode)
				newWeightPenalty = 0;
			else if (weightproc < 666)
				newWeightPenalty = 1;
			else if (weightproc < 800)
				newWeightPenalty = 2;
			else if (weightproc < 1000)
				newWeightPenalty = 3;
			else
				newWeightPenalty = 4;
		}

		if (_curWeightPenalty != newWeightPenalty)
		{
			_curWeightPenalty = newWeightPenalty;
			if (newWeightPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
			else
				super.removeSkill(getKnownSkill(4270));

			sendEtcStatusUpdate();
		}
	}

	public void refreshExpertisePenalty()
	{
		if (Config.GRADE_PENALTY)
		{
			int newPenalty = 0;

			for (L2ItemInstance item : getInventory().getItems())
			{
				if (item != null)
				{
					if (item.isEquipped() && item.getItem() != null)
					{
						int crystaltype = item.getItem().getCrystalType();

						if (crystaltype > newPenalty)
							newPenalty = crystaltype;
					}
				}
			}

			newPenalty = newPenalty - getItemExpertiseIndex();
			if (newPenalty <= 0)
				newPenalty = 0;

			if (getExpertisePenalty() != newPenalty || hasSkill(4267) != (newPenalty > 0))
			{
				_expertisePenalty = newPenalty;

				if (newPenalty > 0)
					super.addSkill(SkillTable.getInstance().getInfo(4267, 1));
				else
					super.removeSkill(getKnownSkill(4267));

				sendEtcStatusUpdate();
			}
		}
	}

	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}

	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}

	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.<BR><BR>
	 */
	public ClassId getClassId()
	{
		return getTemplate().getClassId();
	}

	
	public  void  academyCheck(int Id)
	{
		if ((getSubPledgeType() == -1 || getLvlJoinedAcademy() != 0) && _clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third && !isSubClassActive())
		{
			if (getLvlJoinedAcademy() <= 16)
				_clan.setReputationScore(_clan.getReputationScore() + 650, true);
			else if (getLvlJoinedAcademy() >= 39)
				_clan.setReputationScore(_clan.getReputationScore() + 190, true);
			else
				_clan.setReputationScore(_clan.getReputationScore() + (650 - (getLvlJoinedAcademy() - 16) * 20), true);
			if(_clan==null)
				return;
			_clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
			setLvlJoinedAcademy(0);
			//oust pledge member from the academy, cuz he has finished his 2nd class transfer
			SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
			msg.addString(getName());
			_clan.broadcastToOnlineMembers(msg);
			_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));

			_clan.removeClanMember(getObjectId(), 0);
			_clan = null;
			
			sendPacket(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED);
			// receive graduation gift
			getInventory().addItem("Gift", 8181, 1, this, null); // give academy circlet
		}
	}

	/**
	 * Set the template of the L2PcInstance.<BR><BR>
	 *
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 *
	 */
	public void setClassId(int Id)
	{
		academyCheck(Id);
		getStat().resetModifiers();
		if (isSubClassActive())
			getSubClasses().get(_classIndex).setClassId(Id);
		setClassTemplate(Id);

		setTarget(this);
		// Animation: Production - Clan / Transfer
		MagicSkillUse msu = new MagicSkillUse(this, this, 5103, 1, 1196, 0, false);
		broadcastPacket(msu);
		// Update class icon in party and clan
		broadcastClassIcon();

		if (Config.AUTO_LEARN_SKILLS)
			rewardSkills();
		checkInventory();		
		intemediateStore();
	}

	public void checkSSMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
	{
		if (unequipped == null)
			return;

		if (unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON
				&& (equipped == null || equipped.getItem().getCrystalType() != unequipped.getItem().getCrystalType()))
		{
			for (L2ItemInstance ss : getInventory().getItems())
			{
				int _itemId = ss.getItemId();

				if (((_itemId >= 2509 && _itemId <= 2514) || (_itemId >= 3947 && _itemId <= 3952) || (_itemId <= 1804 && _itemId >= 1808) || _itemId == 5789
						|| _itemId == 5790 || _itemId == 1835)
						&& ss.getItem().getCrystalType() == unequipped.getItem().getCrystalType())
				{
					sendPacket(new ExAutoSoulShot(_itemId, 0));

					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(ss.getItemName());
					sendPacket(sm);
				}
			}
		}
	}

	public synchronized void useEquippableItem(L2ItemInstance item, boolean abortAttack)
	{
		L2ItemInstance[] items = null;
		boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;
		L2ItemInstance old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);

		if (old == null)
			old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

		checkSSMatch(item, old);
		int bodyPart = item.getItem().getBodyPart();

		if (isEquiped)
		{
			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
			}
			sendPacket(sm);

			if (bodyPart == L2Item.SLOT_L_EAR || bodyPart == L2Item.SLOT_LR_EAR || bodyPart == L2Item.SLOT_L_FINGER || bodyPart == L2Item.SLOT_LR_FINGER)
			{
				getInventory().setPaperdollItem(item.getLocationSlot(), null);
				sendPacket(new ItemList(this, false));
			}

			if (bodyPart == L2Item.SLOT_DECO)
				items = getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
			else
				items = getInventory().unEquipItemInBodySlotAndRecord(bodyPart);
		}
		else
		{
			L2ItemInstance tempItem = getInventory().getPaperdollItemByL2ItemId(bodyPart);

			if (tempItem != null && tempItem.isWear())
				return;

			else if (bodyPart == 0x4000)
			{
				tempItem = getInventory().getPaperdollItem(7);
				if (tempItem != null && tempItem.isWear())
					return;

				tempItem = getInventory().getPaperdollItem(8);
				if (tempItem != null && tempItem.isWear())
					return;
			}
			else if (bodyPart == 0x8000)
			{
				tempItem = getInventory().getPaperdollItem(10);
				if (tempItem != null && tempItem.isWear())
					return;

				tempItem = getInventory().getPaperdollItem(11);
				if (tempItem != null && tempItem.isWear())
					return;
			}

			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
				sm.addItemName(item);
			}
			sendPacket(sm);

			if ((bodyPart & L2Item.SLOT_HEAD) > 0 || (bodyPart & L2Item.SLOT_NECK) > 0 || (bodyPart & L2Item.SLOT_L_EAR) > 0 || (bodyPart & L2Item.SLOT_R_EAR) > 0 || (bodyPart & L2Item.SLOT_L_FINGER) > 0 || (bodyPart & L2Item.SLOT_R_FINGER) > 0 || (bodyPart & L2Item.SLOT_R_BRACELET) > 0 || (bodyPart & L2Item.SLOT_L_BRACELET) > 0 ||(bodyPart & L2Item.SLOT_DECO) > 0)
				sendPacket(new UserInfo(this));

			items = getInventory().equipItemAndRecord(item);
			if (item.getItem().getType2() == L2Item.TYPE2_WEAPON)
			{
				item.setChargedSoulshot(L2ItemInstance.CHARGED_NONE, true);
				item.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
			item.decreaseMana(false);
		}
		sm = null;

		refreshExpertisePenalty();
		InventoryUpdate iu = new InventoryUpdate();
		iu.addEquipItems(items);
		sendPacket(iu);

		if (abortAttack)
			abortAttack();

		broadcastUserInfo(true);
		if (getInventoryLimit() != oldInvLimit)
			sendPacket(new ExStorageMaxCount(this));
	}

	/** Return the Experience of the L2PcInstance. */
	public long getExp()
	{
		return getStat().getExp();
	}

	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		if (scroll == null)
			setIsEnchanting(false);

		_activeEnchantItem = scroll;
	}

	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}

	public void setIsEnchanting(boolean val)
	{
		_isEnchanting = val;
	}

	public boolean isEnchanting()
	{
		return _isEnchanting;
	}

	public void setActiveEnchantAttrItem(L2ItemInstance stone)
	{
		_activeEnchantAttrItem = stone;
	}

	public L2ItemInstance getActiveEnchantAttrItem()
	{
		return _activeEnchantAttrItem;
	}
	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equipped).<BR><BR>
	 *
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
	 *
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}

	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equipped).<BR><BR>
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equipped).<BR><BR>
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			//human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			//human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			//elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			//elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			//dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			//dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			//orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			//orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			//dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
		}

		return weaponItem;
	}

	public void calcExpertiseLevel()
	{
		int lvl = getLevel();
		for (int i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
				setExpertiseIndex(i);
			if (lvl  >= EXPERTISE_LEVELS[i])
				setItemExpertiseIndex(i);
		}
	}	
	
	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Level of the L2PcInstance </li>
	 * <li>If L2PcInstance Level is 5, remove beginner Lucky skill </li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR><BR>
	 *
	 */
	public void rewardSkills()
	{
		// Get the Level of the L2PcInstance
		int lvl = getLevel();

		// Remove beginner Lucky skill
		if (lvl > 9)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(194, 1);
			removeSkill(skill);
		}

		// Calculate the current higher Expertise of the L2PcInstance
		calcExpertiseLevel();

		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill);
		}

		// Active skill dwarven craft
		if (getSkillLevel(1321) < 1 && getRace() == Race.Dwarf)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1321, 1);
			addSkill(skill);
		}

		// Active skill common craft
		if (getSkillLevel(1322) < 1)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1322, 1);
			addSkill(skill);
		}

		for (int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if (lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < (i + 1))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i + 1));
				addSkill(skill);
			}
		}

		// Auto-Learn skills if activated
		if (Config.AUTO_LEARN_SKILLS)
		{
			if (isCursedWeaponEquipped())
				return;

			if(Config.AUTO_LEARN_MAX_LEVEL>0 && getLevel() > Config.AUTO_LEARN_MAX_LEVEL)
				return;

			giveAvailableSkills();
		}

		refreshOverloaded();
		refreshExpertisePenalty();
		removeHighSkills();

		sendSkillList();
	}

	/** Set the Experience value of the L2PcInstance. */
	public void setExp(long exp)
	{
		getStat().setExp(exp);
	}

	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load

		// Add noble skills if noble
		if (isNoble())
			setNoble(true);

		// Add Hero skills if hero
		if (isHero())
			setHero(true);

		if (getClan() != null)
		{
			setPledgeClass(L2ClanMember.getCurrentPledgeClass(this));
			getClan().addSkillEffects(this, false);
			PledgeSkillList psl = new PledgeSkillList(getClan());
			sendPacket(psl);
			if (getClan().getLevel() >= Config.SIEGE_CLAN_MIN_LEVEL && isClanLeader())
				SiegeManager.getInstance().addSiegeSkills(this);
			if (getClan().getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(getClan()).giveResidentialSkills(this);
			if (getClan().getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(getClan()).giveResidentialSkills(this);
		}
		getInventory().reloadEquippedItems();
		restoreDeathPenaltyBuffLevel();
	}

	/**
	 * Give all available skills to the player.<br><br>
	 *
	 */
	public void giveAvailableSkills()
	{
		int unLearnable = 0;
		int skillCounter = 0;

		// Get available skills
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		while (skills.length != unLearnable)
		{
			unLearnable = 0;
			for (L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if (sk == null || !sk.getCanLearn(getClassId()) || (sk.getId() == L2Skill.SKILL_DIVINE_INSPIRATION && !Config.AUTO_LEARN_DIVINE_INSPIRATION) || sk.getId() == L2Skill.SKILL_LUCKY)
				{
					unLearnable++;
					continue;
				}

				if (getSkillLevel(sk.getId()) == -1)
					skillCounter++;

				addSkill(sk, true);
			}
			skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		}

		if (skillCounter > 0)
			sendMessage(String.format(Message.getMessage(this, Message.MessageId.MSG_SKILLS_LEARN), skillCounter));
	}

	/**
	 * Return the Race object of the L2PcInstance.<BR><BR>
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
			return getTemplate().getRace();

		L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.getRace();
	}

	public L2Radar getRadar()
	{
		if (_radar == null)
			_radar = new L2Radar(this);
		return _radar;
	}

	/** Return the SP amount of the L2PcInstance. */
	public int getSp()
	{
		return getStat().getSp();
	}

	/** Set the SP amount of the L2PcInstance. */
	public void setSp(int sp)
	{
		super.getStat().setSp(sp);
	}

	public boolean isOnVehicle() {
		return false;
	}
	/**
	 * Return true if this L2PcInstance is a clan leader in
	 * ownership of the passed castle
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();
		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
				return true;
		}
		return false;
	}

	/**
	 * Return the Clan Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR><BR>
	 */
	public int getClanCrestId()
	{
		if (_clan != null && _clan.hasCrest())
			return _clan.getCrestId();
		return 0;
	}

	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null && _clan.hasCrestLarge())
			return _clan.getCrestLargeId();
		return 0;
	}

	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}

	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}

	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}

	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}

	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}

	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.<BR><BR>
	 */
	@Override
	public PcInventory getInventory()
	{
		if (_inventory == null)
			_inventory = new PcInventory(this);
		return _inventory;
	}

	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.<BR><BR>
	 */
	public void removeItemFromShortCut(int objectId)
	{
		getShortCuts().deleteShortCutByObjectId(objectId);
	}

	/**
	 * Return true if the L2PcInstance is sitting.<BR><BR>
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
		
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}

	/**
	 * While animation is shown, you may NOT move/use skills/sit/stand again in retail.<BR><BR>
	 * @author SaveGame
	 */
	private class ProtectSitDownStandUp implements Runnable
	{
		public void run()
		{
			_protectedSitStand = false;
		}
	}

	public void sitDown()
	{
		sitDown(true);
	}

	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	private boolean _sitWhenArrived;
	
	@Override
	protected boolean moveToLocation(int x, int y, int z, int offset) {
		_sitWhenArrived = false;
		return super.moveToLocation(x, y, z, offset);
	}
	@Override
	public void finishMovement() {
		super.finishMovement();
		if(_sitWhenArrived)
			sitDown(true);
		_sitWhenArrived = false;
	}
	
	public void sitDown(boolean force)
	{
		if ((isCastingNow() || isCastingSimultaneouslyNow()) && !_relax)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
			return;
		} 
		if(_isMoving) {
			_sitWhenArrived = true;
			return;
		}
		if (!(_waitTypeSitting || super.isAttackingDisabled() || isOutOfControl() || isImmobilized() || (!force && _protectedSitStand)))
		{
			breakAttack();
			_waitTypeSitting = true;
			getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			//fix by SaveGame
			_protectedSitStand = true;
			ThreadPoolManager.getInstance().scheduleGeneral(new ProtectSitDownStandUp(), 2333);
		}
	}

	public void standUp()
	{
		standUp(true);
	}

	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	public void standUp(boolean force)
	{
		if(_event!=null && !_event.canDoAction(this, RequestActionUse.ACTION_SIT_STAND))
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_FORBIDEN_BY_ADMIN));
		else if (_isAway)
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_USE_BACK_COMMAND));
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead() && (!_protectedSitStand || force))
		{
			if (_relax)
			{
				setRelax(false);
				stopEffects(L2EffectType.RELAXING);
			}
			_waitTypeSitting = false;
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			//fix by SaveGame
			_protectedSitStand = true;
			ThreadPoolManager.getInstance().scheduleGeneral(new ProtectSitDownStandUp(), 2333);
		}
	}

	/**
	 * Set the value of the _relax value. Must be true if using skill Relax and false if not.
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}

	/**
	 * Return the PcWarehouse object of the L2PcInstance.<BR><BR>
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null) {
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		return _warehouse;
	}

	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
			_warehouse.deleteMe();
		_warehouse = null;
	}

	/**
	 * Return the PcFreight object of the L2PcInstance.<BR><BR>
	 */
	public PcFreight getFreight()
	{
		if (_freight == null){
			_freight = new PcFreight(this);
			_freight.restore();
		}
		return _freight;
	}

	/**
	 * Return the Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getCharId()
	{
		return getObjectId();
	}

	public void setCharId(int charId)
	{
	}

	/**
	 * Return the Adena amount of the L2PcInstance.<BR><BR>
	 */
	public int getAdena()
	{
		return getInventory().getAdena();
	}

	/**
	 * Return the Ancient Adena amount of the L2PcInstance.<BR><BR>
	 */
	public int getAncientAdena()
	{
		return getInventory().getAncientAdena();
	}

	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			if (_inventory.getAdena() == Integer.MAX_VALUE)
			{
				sendPacket(SystemMessageId.EXCEEDED_THE_MAXIMUM);
				return;
			}
			else if (_inventory.getAdena() >= (Integer.MAX_VALUE - count))
			{
				count = Integer.MAX_VALUE - _inventory.getAdena();
				_inventory.addAdena(process, count, this, reference);
			}
			else if (_inventory.getAdena() < (Integer.MAX_VALUE - count))
				_inventory.addAdena(process, count, this, reference);
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S1_ADENA);
				sm.addNumber(count);
				sendPacket(sm);
			}

			// Send update packet
			getInventory().updateInventory(getInventory().getAdenaInstance());
		}
	}

	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage)
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return false;
		}

		if (count > 0)
		{
			L2ItemInstance adenaItem = getInventory().getAdenaInstance();
			getInventory().reduceAdena(process, count, this, reference);

			// Send update packet
			getInventory().updateInventory(adenaItem);

			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}

		return true;
	}

	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
			sm.addNumber(count);
			sendPacket(sm);
		}

		if (count > 0)
		{
			getInventory().addAncientAdena(process, count, this, reference);
			getInventory().updateInventory(getInventory().getAncientAdenaInstance());
		}
	}

	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);

			return false;
		}

		if (count > 0)
		{
			getInventory().reduceAncientAdena(process, count, this, reference);
			getInventory().updateInventory(getInventory().getAncientAdenaInstance());
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}

		return true;
	}

	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean UpdateIL)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item);
					sm.addNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item);
					sendPacket(sm);
				}
			}

			// Add the item to inventory
			L2ItemInstance newitem = getInventory().addItem(process, item, this, reference);

			// do treatments after adding this item
			processAddItem(UpdateIL, newitem);
			return newitem;
		}
		return null;
	}

	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public L2ItemInstance addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage, boolean UpdateIL)
	{
		if (count > 0)
		{
			// Add the item to inventory
			L2ItemInstance newItem = getInventory().addItem(process, itemId, count, this, reference);

			// Sends message to client if requested
			if (sendMessage)
				sendMessageForNewItem(newItem, count, process);

			processAddItem(UpdateIL, newItem);
			return newItem;
		}
		return null;
	}

	/**
	 * @param UpdateIL
	 * @param newitem
	 */
	private void processAddItem(boolean UpdateIL, L2ItemInstance newitem)
	{
		if (newitem == null)
			return;
		// If over capacity, drop the item
		if (!isGM() && !getInventory().validateCapacity(0))
			dropItem("InvDrop", newitem, null, true);
		// Cursed Weapon
		else if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			CursedWeaponsManager.getInstance().activate(this, newitem); 
		// Combat Flag
		else if (FortSiegeManager.getInstance().isCombat(newitem.getItemId()))
		{
			if (FortSiegeManager.getInstance().activateCombatFlag(this, newitem))
			{
				Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
					fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.S1), getName()+" взял флаг");
			}
		}

		//Auto use herbs - autoloot
		else if (newitem.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(newitem.getItemId());
			if (handler == null)
				_log.warn("No item handler registered for item ID " + newitem.getItemId() + ".");
			else 
				handler.useItem(this, newitem);
			return;
		}

		//Update current load as well
		if (UpdateIL)
		{
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
		}

		// Send inventory update packet
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(newitem);
			sendPacket(playerIU);
		}
	}

	/**
	 * @param item : L2ItemInstance Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param process : String Identifier of process triggering this action
	 */
	private void sendMessageForNewItem(L2ItemInstance item, int count, String process)
	{
		if (item == null)
			return;

		if (count > 1)
		{
			if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addNumber(count);
				sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
				sm.addItemName(item);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}
		else
		{
			if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S1);
				sm.addItemName(item);
				sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
				sm.addItemName(item);
				sendPacket(sm);
			}
		}
	}

	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		addItem(process, item, reference, sendMessage, true);
	}

	public boolean addItem(String process, int []itemsId, int []counts, L2Object reference, boolean sendMessage)
	{
		if(itemsId.length==0 || itemsId.length != counts.length)
			return false;
		for(int i=0;i<itemsId.length;i++)
			if(addItem(process, itemsId[i], counts[i], reference, sendMessage, true)==null)
				return false;
		return true;
	}
	public boolean addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		return(addItem(process, itemId, count, reference, sendMessage, true)!=null);
	}

	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return this.destroyItem(process, item, item.getCount(), reference, sendMessage);
	}

	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage)
	{
		item = getInventory().destroyItem(process, item, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}

		// Send inventory update packet
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(item);
			sm.addNumber(count);
			sendPacket(sm);
		}

		return true;
	}

	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}

		return destroyItem(process, item, count, reference, sendMessage);
	}

	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getCount() < count)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}

		return destroyItem(null, item, count, reference, sendMessage);
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = getInventory().getItemByItemId(itemId);
		if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}

		// Send inventory update packet
		getInventory().updateInventory(item);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(item);
			sm.addNumber(count);
			sendPacket(sm);
		}
		return true;
	}

	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{

		// Go through all Items of the inventory
		for (L2ItemInstance item : getInventory().getItems())
		{
			// Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
				if (item.isEquipped())
					getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());

				if (getInventory().destroyItem(process, item, this, reference) == null)
				{
					_log.warn("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}

				// Send an Unequipped Message in system window of the player for each Item
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
				sendPacket(sm);
			}
		}

		// Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
		ItemList il = new ItemList(getInventory().getItems(), true);
		sendPacket(il);

		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _knownPlayers
		broadcastUserInfo(true);

		// Sends message to client if requested
		sendPacket(SystemMessageId.NO_LONGER_TRYING_ON);
	}

	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
			return null;
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
			return null;
		// Send inventory update packet
		{
			InventoryUpdate playerIU = new InventoryUpdate();

			if (oldItem.getCount() > 0 && oldItem != newItem)
				playerIU.addModifiedItem(oldItem);
			else
				playerIU.addRemovedItem(oldItem);
			sendPacket(playerIU);
		}

		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);

		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();

			{
				InventoryUpdate playerIU = new InventoryUpdate();

				if (newItem.getCount() > count)
					playerIU.addModifiedItem(newItem);
				else
					playerIU.addNewItem(newItem);

				targetPlayer.sendPacket(playerIU);
			}

			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();

			if (newItem.getCount() > count)
				petIU.addModifiedItem(newItem);
			else
				petIU.addNewItem(newItem);
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}

		return newItem;
	}

	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		item = getInventory().dropItem(process, item, this, reference);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return false;
		}

		item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ());

		if (Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
				ItemsAutoDestroy.getInstance().addItem(item);
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
				item.setProtected(false);
			else
				item.setProtected(true);
		}
		else
			item.setProtected(true);

		// Send inventory update packet
		getInventory().updateInventory(item);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}

		return true;
	}

	public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance olditem = getInventory().getItemByObjectId(objectId);
		L2ItemInstance item = getInventory().dropItem(process, objectId, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);

			return null;
		}

		item.dropMe(this, x, y, z);
		// destroy  item droped from inventory by player when DESTROY_PLAYER_INVENTORY_DROP is set to true
		if (Config.DESTROY_PLAYER_INVENTORY_DROP)
		{
			if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			{
				if ((Config.AUTODESTROY_ITEM_AFTER > 0 && item.getItemType() != L2EtcItemType.HERB)
						|| (Config.HERB_AUTO_DESTROY_TIME > 0 && item.getItemType() == L2EtcItemType.HERB))
				{
					// check if item is equipable
					if (item.isEquipable())
					{
						// delete only when Configvalue DESTROY_EQUIPABLE_PLAYER_ITEM is set to true
						if (Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
							ItemsAutoDestroy.getInstance().addItem(item);
					}
					else
						ItemsAutoDestroy.getInstance().addItem(item);
				}
			}
			item.setProtected(false);
		}
		// Avoids it from beeing removed by the auto item destroyer
		else
			item.setDropTime(0);

		// Send inventory update packet
		getInventory().updateInventory(olditem);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		return item;
	}

	public L2ItemInstance checkItemManipulation(int objectId, int count, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			_log.debug(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}

		if (count < 0 || (count > 1 && !item.isStackable()))
		{
			_log.debug(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}

		if (count > item.getCount())
		{
			_log.debug(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			return null;
		}

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			return null;
		}

		// cannot drop/trade wear-items
		if (item.isWear())
			return null;

		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
			return null;

		return item;
	}

	/**
	 * Set _protectEndTime according settings.
	 */
	public void setProtection(boolean protect)
	{
		_protectEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public long getProtection()
	{
		return _protectEndTime;
	}

	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}

	/**
	 * Get the client owner of this char.<BR><BR>
	 */
	public L2GameClient getClient()
	{
		return _client;
	}

	/**
	 * Set the active connection with the client.<BR><BR>
	 */
	
	public void setClient(L2GameClient client)
	{
		_client = client;
		updateOnlineStatus();
	}

	public Point3D getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}

	public void setCurrentSkillWorldPosition(Point3D worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}

	public static int checkClass()
	{
		return _REVID$$;
	}

	public boolean canBeTargetedByAtSiege(L2PcInstance player)
	{
		Siege siege = SiegeManager.getInstance().getSiege(this);
		if (siege != null && siege.getIsInProgress())
		{
			L2Clan selfClan = getClan();
			L2Clan oppClan = player.getClan();
			if (selfClan != null && oppClan != null)
			{
				boolean self = false;
				for (L2SiegeClan clan : siege.getAttackerClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == selfClan || cl.getAllyId() == getAllyId())
					{
						self = true;
						break;
					}
				}

				for (L2SiegeClan clan : siege.getDefenderClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == selfClan || cl.getAllyId() == getAllyId())
					{
						self = true;
						break;
					}
				}

				boolean opp = false;
				for (L2SiegeClan clan : siege.getAttackerClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == oppClan || cl.getAllyId() == player.getAllyId())
					{
						opp = true;
						break;
					}
				}

				for (L2SiegeClan clan : siege.getDefenderClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == oppClan || cl.getAllyId() == player.getAllyId())
					{
						opp = true;
						break;
					}
				}

				return self && opp;
			}

			return false;
		}

		return true;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (player == null)
			return;

		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_PC_ITERACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			player.sendMessage(Message.getMessage(this, Message.MessageId.MSG_ACTION_NOT_ALLOWED_DURING_SHUTDOWN));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (Config.SIEGE_ONLY_REGISTERED && !canBeTargetedByAtSiege(player))
		{
			player.sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_TARGET_WHEN_TARGET_IN_SIEGE));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (!Config.ALT_AWAY_ALLOW_INTERFERENCE && isAway())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (((_inOlympiadMode && !player._inOlympiadMode) || (!_inOlympiadMode && player._inOlympiadMode) || ((_inOlympiadMode && player._inOlympiadMode) && (_olympiadGameId != player._olympiadGameId))) && !player.isGM())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.getGameEvent() !=_event)
			if((player.getGameEvent() != null && !player.getGameEvent().canInteract(player, this)) ||
			   (_event!=null && !_event.canInteract(player, this)) && !player.isGM()	) {
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
				
		if (player.isOutOfControl())
			player.sendPacket(ActionFailed.STATIC_PACKET);

		if (player.getTarget() != this)
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			if (player != this) {
				player.sendPacket(new ValidateLocation(this));
			} 
		}
		else
		{
			if (player != this) {
				player.sendPacket(new ValidateLocation(this));
				
			}

			if (getPrivateStoreType() != 0)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
			{
				if (!isGM() && isAutoAttackable(player))
				{
					if ((isCursedWeaponEquipped() && player.getLevel() < 21) || (player.isCursedWeaponEquipped() && getLevel() < 21))
						player.sendPacket(ActionFailed.STATIC_PACKET);
					else
					{
						if (Config.GEODATA)
						{
							if (GeoData.getInstance().canSeeTarget(player, this))
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
								player.onActionRequest();
							}
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							player.onActionRequest();
						}
					}
				}
				else
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					if (Config.GEODATA)
					{
						if (GeoData.getInstance().canSeeTarget(player, this))
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
					else
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
		}
	}

	@Override
	public void onActionShift(L2PcInstance player)
	{
		if (player == null)
			return;

		player.sendPacket(ActionFailed.STATIC_PACKET);

		if (player.isGM())
		{
			if (this != player.getTarget())
			{
				player.setTarget(this);
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
				if (player != this)
					player.sendPacket(new ValidateLocation(this));
			}
			else {
				editchar.sendHtml(player, this, "charinfo_menu.htm");
			}
		}
		else
		{
			if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_PC_ITERACTION && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
			{
				player.sendMessage(Message.getMessage(this, Message.MessageId.MSG_ACTION_NOT_ALLOWED_DURING_SHUTDOWN));
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (Config.SIEGE_ONLY_REGISTERED && !canBeTargetedByAtSiege(player))
			{
				player.sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_TARGET_WHEN_TARGET_IN_SIEGE));
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (!Config.ALT_AWAY_ALLOW_INTERFERENCE && isAway())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (((_inOlympiadMode && !player._inOlympiadMode) || (!_inOlympiadMode && player._inOlympiadMode) || ((_inOlympiadMode && player._inOlympiadMode) && (_olympiadGameId != player._olympiadGameId))) && !player.isGM())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (player.isOutOfControl())
				player.sendPacket(ActionFailed.STATIC_PACKET);

			if (player.getTarget() != this)
			{
				player.setTarget(this);
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
				if (player != this)
					player.sendPacket(new ValidateLocation(this));
			}
			else
			{
				if (player != this)
					player.sendPacket(new ValidateLocation(this));

				if (getPrivateStoreType() != 0)
				{
					if(player.isInsideRadius(this, player.getPhysicalAttackRange(), false, false))
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					if (!isGM() && isAutoAttackable(player))
					{
						if ((isCursedWeaponEquipped() && player.getLevel() < 21) || (player.isCursedWeaponEquipped() && getLevel() < 21))
							player.sendPacket(ActionFailed.STATIC_PACKET);
						else
						{
							if (player.isInsideRadius(this, player.getPhysicalAttackRange(), false, false))
							{
								if (Config.GEODATA)
								{
									if (GeoData.getInstance().canSeeTarget(player, this))
									{
										player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
										player.onActionRequest();
									}
								}
								else
								{
									player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
									player.onActionRequest();
								}
							}
						}
					}
				}
			}			
		}
	}
		
	/**
	 * Returns true if cp update should be done, false if not
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getStatus().getCurrentCp();

		if (currentCp <= 1.0 || getMaxCp() < barPixels)
			return true;

		if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns true if mp update should be done, false if not
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getStatus().getCurrentMp();

		if (currentMp <= 1.0 || getMaxMp() < barPixels)
			return true;

		if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			return true;
		}
		return false;
	}

	@Override
	public final void broadcastStatusUpdateImpl()
	{
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int)getStatus().getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int)getStatus().getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int)getStatus().getCurrentCp());
		broadcastPacket(su);
		
		if (isInParty() && (needHpUpdate(352) || needMpUpdate(352) || needCpUpdate(352)))
			getParty().broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));

		if (isInOlympiadMode())
		{
			for (L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				if (player.getOlympiadGameId() == getOlympiadGameId() && player.isOlympiadStart())
					player.sendPacket(new ExOlympiadUserInfo(this, 1));
			}
			if (isOlympiadStart())
			{
				final List<L2PcInstance> spectators = Olympiad.getInstance().getSpectators(getOlympiadGameId());

				if (spectators != null && !spectators.isEmpty())
				{
					final ExOlympiadUserInfo eoui = new ExOlympiadUserInfo(this);

					for (L2PcInstance spectator : spectators)
						spectator.sendPacket(eoui);
				}
			}
		}
		if (isInDuel())
			DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
	}

	public boolean isAway()
	{
		return _isAway;
	}

	public void setIsAway(boolean state)
	{
		_isAway = state;
	}

	@Override
	public void updateEffectIconsImpl()
	{
		final EffectInfoPacketList list = new EffectInfoPacketList(this);

		sendPacket(new MagicEffectIcons(list));

		if (isInParty())
			getParty().broadcastToPartyMembers(this, new PartySpelled(list));

		if (isInOlympiadMode() && isOlympiadStart())
		{
			final List<L2PcInstance> spectators = Olympiad.getInstance().getSpectators(getOlympiadGameId());

			if (spectators != null && !spectators.isEmpty())
			{
				final ExOlympiadSpelledInfo os = new ExOlympiadSpelledInfo(list);

				for (L2PcInstance spectator : spectators)
					spectator.sendPacket(os);
			}
		}
	}

	public final void broadcastUserInfo()
	{
		broadcastUserInfo(false);
	}

	public final void broadcastUserInfo(boolean charInfo)
	{
		sendPacket(new UserInfo(this));
		if (charInfo)
			broadcastCharInfo();
	}

	public final void broadcastCharInfo()
	{
		Broadcast.toKnownPlayers(this,new CharInfo(this));
	}

	public final void broadcastTitleInfo()
	{
		broadcastFullInfo();
	}

	/**
	 * Return the Alliance Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getAllyId()
	{
		return (_clan == null) ? 0 : _clan.getAllyId();
	}

	public int getAllyCrestId()
	{
		if (getClanId() == 0)
			return 0;
		if (getClan().getAllyId() == 0)
			return 0;
		return getClan().getAllyCrestId();
	}


	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.<BR><BR>
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		final L2GameClient client = _client;
		if (client != null)
			client.sendPacket(packet);
	}

	/**
	 * Sends a SystemMessage without any parameter added. No instancing at all!
	 */
	@Override
	public void sendPacket(SystemMessageId sm)
	{
		sendPacket(sm.getSystemMessage());
	}

	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);

			if (temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL)
				sendPacket(new PrivateStoreListSell(this, temp));
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
				sendPacket(new PrivateStoreListBuy(this, temp));
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopSellList(this, temp));
		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
				target.onAction(this);
		}
	}

	/**
	 * Manage AutoLoot Task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param target The L2ItemInstance dropped
	 *
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if (isInParty())
			getParty().distributeItem(this, item, false, target);
		else if (item.getItemId() == 57)
			addAdena("Loot", item.getCount(), target, true);
		else
			addItem("Loot", item.getItemId(), item.getCount(), target, true, false);
	}

	/**
	 * Manage Pickup Task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance </li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets </li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param object The L2ItemInstance to pick up
	 *
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
			return;

		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		// Check if the L2Object to pick up is a L2ItemInstance
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warn("trying to pickup wrong target." + getTarget());
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		// Send a Server->Client packet ActionFailed to this L2PcInstance
//		sendPacket(ActionFailed.STATIC_PACKET);

		// Send a Server->Client packet StopMove to this L2PcInstance
//		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
//		sendPacket(sm);

		
			// Check if the target to pick up is visible
			if(!target.canPickup(this)) {
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;

			}

			if (((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(SystemMessageId.SLOTS_FULL);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (isInvul() && !isGM())
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (getActiveTradeList() != null)
			{
				sendPacket(SystemMessageId.CANNOT_PICKUP_OR_USE_ITEM_WHILE_TRADING);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);

				if (target.getItemId() == 57)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addNumber(target.getCount());
					sendPacket(smsg);
				}
				else if (target.getCount() > 1)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addNumber(target.getCount());
					sendPacket(smsg);
				}
				else
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target);
					sendPacket(smsg);
				}
				return;
			}

			// Cursed Weapons
			if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()) && isCursedWeaponEquipped())
			{
				ItemTable.getInstance().destroyItem("Pickup CW", target, this, null);
				CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId());
				cw.increaseKills(cw.getStageKills());
				return;
			}

			// You can pickup only 1 combat flag
			if (FortSiegeManager.getInstance().isCombat(target.getItemId()))
			{
				if (!FortSiegeManager.getInstance().checkIfCanPickup(this))
					return;
			}

			// Remove the L2ItemInstance from the world and send server->client GetItem packets
		target.pickupMe(this);
		
		//Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getItemId());
			if (handler == null)
				_log.warn("No item handler registered for item ID " + target.getItemId() + ".");
			else
				handler.useItem(this, target);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
			addItem("Pickup", target, null, true);
		else if (FortSiegeManager.getInstance().isCombat(target.getItemId()))
			addItem("Pickup", target, null, true);
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if (target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					msg.addPcName(this);
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target);
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					msg.addPcName(this);
					msg.addItemName(target);
					broadcastPacket(msg, 1400);
				}
			}

			// Check if a Party is in progress
			if (isInParty())
				getParty().distributeItem(this, target);
			// Target is adena
			else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			// Target is regular item
			else {
				addItem("Pickup", target, null, true);
			}
		}
	}

	/**
	 * Set a target.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character </li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character </li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li><BR><BR>
	 *
	 * @param newTarget The L2Object to target
	 *
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		if (newTarget != null)
		{
			boolean isParty = (((newTarget instanceof L2PcInstance) && isInParty() && getParty().getPartyMembers().contains(newTarget)));

			// Check if the new target is visible
			if (!isParty && !newTarget.isVisible())
				newTarget = null;

			// Prevents /target exploiting
			if (newTarget != null && !isParty && Math.abs(newTarget.getZ() - getZ()) > 1000)
				newTarget = null;
		}

		if (!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if ((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
				newTarget = null;

			// Can't target and attack rift invaders if not in the same room
			else if (isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

				if (newTarget != null
						&& !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom)
								.checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
					newTarget = null;
			}
		}

		// Get the current target
		L2Object oldTarget = getTarget();
		//L2EMU_EDIT
		if (oldTarget != null && newTarget != null)
		//L2EMU_EDIT
		{
			if (oldTarget.equals(newTarget))
				return; // no target change

			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
		}

		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if (newTarget instanceof L2Character)
		{
			TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			broadcastPacket(my);
		}

		if (newTarget == null && getTarget() != null)
			broadcastPacket(new TargetUnselected(this));

		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}

	/**
	 * Return the active weapon instance (always equipped in the right hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}

	/**
	 * Return the active weapon item (always equipped in the right hand).<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		return (L2Weapon) weapon.getItem();
	}

	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}

	public L2ItemInstance getLegsArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}

	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if (armor == null)
			return null;

		return (L2Armor) armor.getItem();
	}

	public L2Armor getActiveLegsArmorItem()
	{
		L2ItemInstance legs = getLegsArmorInstance();

		if (legs == null)
			return null;

		return (L2Armor) legs.getItem();
	}

	public boolean isWearingHeavyArmor()
	{
		if ((getChestArmorInstance() != null) && getLegsArmorInstance() != null)
		{
			L2ItemInstance legs = getLegsArmorInstance();
			L2ItemInstance armor = getChestArmorInstance();
			if (legs.getItemType() == L2ArmorType.HEAVY && (armor.getItemType() == L2ArmorType.HEAVY))
				return true;
		}
		if (getChestArmorInstance() != null)
		{
			L2ItemInstance armor = getChestArmorInstance();

			if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR
					&& armor.getItemType() == L2ArmorType.HEAVY)
				return true;
		}

		return false;
	}

	public boolean isWearingLightArmor()
	{
		if ((getChestArmorInstance() != null) && getLegsArmorInstance() != null)
		{
			L2ItemInstance legs = getLegsArmorInstance();
			L2ItemInstance armor = getChestArmorInstance();
			if (legs.getItemType() == L2ArmorType.LIGHT && (armor.getItemType() == L2ArmorType.LIGHT))
				return true;
		}
		if (getChestArmorInstance() != null)
		{
			L2ItemInstance armor = getChestArmorInstance();

			if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR
					&& armor.getItemType() == L2ArmorType.LIGHT)
				return true;
		}

		return false;
	}

	public boolean isWearingMagicArmor()
	{
		if ((getChestArmorInstance() != null) && getLegsArmorInstance() != null)
		{
			L2ItemInstance legs = getLegsArmorInstance();
			L2ItemInstance armor = getChestArmorInstance();
			if (legs.getItemType() == L2ArmorType.MAGIC && (armor.getItemType() == L2ArmorType.MAGIC))
				return true;
		}
		if (getChestArmorInstance() != null)
		{
			L2ItemInstance armor = getChestArmorInstance();

			if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR
					&& armor.getItemType() == L2ArmorType.MAGIC)
				return true;
		}

		return false;
	}

	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}

	public void setIsWearingFormalWear(boolean value)
	{
		_IsWearingFormalWear = value;
	}

	/**
	 * Return the secondary weapon instance (always equipped in the left hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	/**
	 * Return the secondary weapon item (always equipped in the left hand) or the fists weapon.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		L2Item item = weapon.getItem();

		if (item instanceof L2Weapon)
			return (L2Weapon) item;

		return null;
	}

	/** 
	 * Ребят, не бейте ногами, это временная затычка<br>
	 * А то я с изменениям L2PcInstance не доделаю модель эвентов<br>
	 * P.S. Ни одного байта не пострадало<br>
	 * @param killer
	 */
	public void onPvPPkKill(L2Character killer) {
		boolean clanWarKill = false;
		boolean playerKill = false;
		boolean charmOfCourage = getCharmOfCourage();
		L2PcInstance pk = killer.getActingPlayer();

		if (pk != null)
		{
			clanWarKill = (pk.getClan() != null && getClan() != null && !isAcademyMember() && !pk.isAcademyMember() && _clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(getClanId()));
			playerKill = true;
		}

		boolean srcInPvP = isInsideZone(L2Zone.FLAG_PVP) && !isInsideZone(L2Zone.FLAG_SIEGE);

		if (killer instanceof L2PcInstance && srcInPvP && Config.ARENA_ENABLED)
		{
			ArenaManager.getInstance().onKill(killer.getObjectId(), killer.getName());
			ArenaManager.getInstance().onDeath(getObjectId(), getName());
		}

		if (!srcInPvP)
		{
			if (pk == null || !pk.isCursedWeaponEquipped())
			{
				onDieDropItem(killer); // Check if any item should be dropped

				if (!srcInPvP)
				{
					if (Config.ALT_GAME_DELEVEL)
					{
						if (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9)
							deathPenalty(clanWarKill, playerKill, charmOfCourage);
					}
					else
					{
						if (!(isInsideZone(L2Zone.FLAG_PVP) && !isInsideZone(L2Zone.FLAG_PVP)) || pk == null)
							onDieUpdateKarma();
					}
				}
			}
			if (pk != null)
			{
				if (Config.ALT_ANNOUNCE_PK)
				{
					if (getPvpFlag() == 0)
					{
						String announcetext = "Игрок " + pk.getName() + " убил в PK игрока " + getName() + ".";
						if (Config.ALT_ANNOUNCE_PK_NORMAL_MESSAGE)
							Announcements.getInstance().announceToPlayers(announcetext);
						else
							Announcements.getInstance().announceToAll(announcetext);
					}
				}
				if (clanWarKill)
				{
					if (getClan().getReputationScore() >= 0)
					{
						pk.getClan().setReputationScore(pk.getClan().getReputationScore() + Config.ALT_REPUTATION_SCORE_PER_KILL, true);
						getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
						pk.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(pk.getClan()));
					}
					if (pk.getClan().getReputationScore() >= Config.ALT_REPUTATION_SCORE_PER_KILL)
					{
						_clan.setReputationScore(_clan.getReputationScore() - Config.ALT_REPUTATION_SCORE_PER_KILL, true);
						getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
						pk.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(pk.getClan()));

						// L2CatsSoftware DevTeam clan system fix
						// When clan member die from var, all clan member gived message info
						SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_WAS_KILLED_AND_S2_POINTS_DEDUCTED_FROM_REPUTATION);
						sm.addString(getName());
						sm.addString(Config.ALT_REPUTATION_SCORE_PER_KILL_SM);
						_clan.broadcastToOnlineMembers(sm);
					}
				}
			}
		}
		else if (pk != null && Config.ALT_ANNOUNCE_PK)
		{
			if (Config.ALT_ANNOUNCE_PK_NORMAL_MESSAGE)
				Announcements.getInstance().announceToPlayers(pk.getName() + " has defeated " + getName());
			else
				Announcements.getInstance().announceToAll(pk.getName() + " has defeated " + getName());
		}
		
	}
	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty </li>
	 * <li>If necessary, unsummon the Pet of the killed L2PcInstance </li>
	 * <li>Manage Karma gain for attacker and Karma loss for the killed L2PcInstance </li>
	 * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
	 * <li>Kill the L2PcInstance </li><BR><BR>
	 *
	 *
	 * @param killer The L2Character who attacks
	 *
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (isMounted())
			stopFeed();
		_lastKilledPlayer = 0;
		synchronized (this)
		{
			if (isFakeDeath())
				stopFakeDeath(null);
			_charges = 0;
			sendPacket(new EtcStatusUpdate(this));
		}
		if (getActiveEnchantItem()!=null)
		{
			sendPacket(new ExPutEnchantTargetItemResult(2, 0, 0));
			setActiveEnchantItem(null);
		}
		if (getActiveEnchantAttrItem()!=null)
		{
			sendPacket(new ExAttributeEnchantResult(0));
			setActiveEnchantAttrItem(null);			
		}

		setExpBeforeDeath(0);

		if (isCursedWeaponEquipped())
			CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
		if (isCombatFlagEquipped())
			FortSiegeManager.getInstance().dropCombatFlag(this);

		if (getClan() != null)
		{
			Castle castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if (castle != null)
				castle.destroyClanGate();
		}
		
		if (killer != null)
		{
			onPvPPkKill(killer);

		}

		// Force Charges
		_charges = 0; //empty charges

		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
		}

		if (_fusionSkill != null)
			abortCast();

		for (L2Character character : getKnownList().getKnownCharacters())
			if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
				character.abortCast();

		if (isInParty() && getParty().isInDimensionalRift())
			getParty().getDimensionalRift().memberDead(this);

		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);

		// When the player has been annihilated, the player is banished from the Four Sepulcher.
		return true;
	}


	/** UnEnquip on skills with disarm effect **/
	public void onDisarm(L2PcInstance target)
	{
		target.getInventory().unEquipItemInBodySlotAndRecord(14);
	}

	private static int _REVID$$ = 115;

	private void onDieDropItem(L2Character killer)
	{
		if ((_event!=null && _event.isRunning()) || killer == null)
			return;

		L2PcInstance pk = killer.getActingPlayer();
		if (pk != null && getKarma() <= 0 && pk.getClan() != null && getClan() != null
				&& (pk.getClan().isAtWarWith(getClanId())))
			return;

		if ((!isInsideZone(L2Zone.FLAG_PVP) || pk == null) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKillerNpc = (killer instanceof L2NpcInstance);
			int pkLimit = Config.KARMA_PK_LIMIT;

			int dropEquip;
			int dropEquipWeapon;
			int dropItem;
			int dropLimit;
			int dropPercent;

			if (getKarma() == 0 || getPkKills() < pkLimit)
				return;
			
			dropPercent = Config.KARMA_RATE_DROP;
			dropEquip = Config.KARMA_RATE_DROP_EQUIP;
			dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
			dropItem = Config.KARMA_RATE_DROP_ITEM;
			dropLimit = Config.KARMA_DROP_LIMIT;
			

			int dropCount = 0;
			while (dropPercent > 0 && Rnd.get(100) < dropPercent && dropCount < dropLimit)
			{
				int itemDropPercent = 0;
				List<Integer> nonDroppableList = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				List<Integer> nonDroppableListPet = Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS;

				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop
					if (!itemDrop.isDropable() || itemDrop.getItemId() == 57 || // Adena
							itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST || // Quest Items
							nonDroppableList.contains(itemDrop.getItemId()) || // Item listed in the non droppable item list
							nonDroppableListPet.contains(itemDrop.getItemId()) || // Item listed in the non droppable pet item list
							getPet() != null && getPet().getControlItemId() == itemDrop.getItemId() // Control Item of active pet
					)
						continue;

					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getLocationSlot());
					}
					else
						itemDropPercent = dropItem; // Item in inventory

					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);
						dropCount++;
						break;
					}
					if(dropCount>=Config.KARMA_DROP_LIMIT)
						break;
				}
			}
			// player can drop adena against other player
			if (Config.ALT_PLAYER_CAN_DROP_ADENA && !isKillerNpc && Config.PLAYER_RATE_DROP_ADENA > 0 && 100 >= Config.PLAYER_RATE_DROP_ADENA
					&& !(killer instanceof L2PcInstance && ((L2PcInstance) killer).isGM()))
			{
				L2ItemInstance itemDrop = getInventory().getAdenaInstance();
				int iCount = getInventory().getAdena();
				// adena count depends on config
				iCount = iCount / 100 * Config.PLAYER_RATE_DROP_ADENA;
				// drop only adena this time
				if (itemDrop != null && itemDrop.getItemId() == 57) // Adena
					dropItem("DieDrop", itemDrop.getObjectId(), iCount, getPosition().getX() + Rnd.get(50) - 25, getPosition().getY() + Rnd.get(50) - 25, getPosition().getZ() + 20, killer, true);
			}
		}
	}

	private void onDieUpdateKarma()
	{
		if (getKarma() > 0)
		{
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel();
			karmaLost *= (getLevel() / 100.0);
			karmaLost = Math.round(karmaLost);
			if (karmaLost < 0)
				karmaLost = 1;
			setKarma(getKarma() - (int) karmaLost);
			broadcastFullInfo();
		}
	}

	public void onKillUpdatePvPKarma(L2Character target)
	{
		if (target == null)
			return;
		if (!(target instanceof L2PlayableInstance))
			return;
		if (_event!=null && _event.isRunning())
			return;
		L2PcInstance targetPlayer = target.getActingPlayer();

		if (targetPlayer == null)
			return; // Target player is null
		if (targetPlayer == this)
			return; // Target player is self
		if(isInOlympiadMode() && targetPlayer.isInOlympiadMode())
			return;
		if (isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			return;
		}

		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
			return;

		// If in Arena, do nothing
		if (isInsideZone(L2Zone.FLAG_PVP))
			return;

		// Check if it's pvp
		if ((checkIfPvP(target) && targetPlayer.getPvpFlag() != 0) || (isInsideZone(L2Zone.FLAG_PVP) && targetPlayer.isInsideZone(L2Zone.FLAG_PVP)))
		{
			if (target instanceof L2PcInstance)
			{
				_nowKilled = targetPlayer.getObjectId();
				increasePvpKills(targetPlayer.getLevel());
				if (Config.FACTION_ENABLED && targetPlayer.getSide() != getSide() && targetPlayer.getSide() != 0 && getSide() != 0 && Config.FACTION_KILL_REWARD)
					increaseFactionKillPoints(targetPlayer.getLevel(), false);
			}
		}
		else
		{
			// check factions
			if (Config.FACTION_ENABLED && targetPlayer.getSide() != getSide() && targetPlayer.getSide() != 0 
			&& getSide() != 0 && Config.FACTION_KILL_REWARD && target instanceof L2PcInstance)
			{
				// give faction pk points
				increaseFactionKillPoints(targetPlayer.getLevel(), true);
				// no karma
				return;
			}

			// check about wars
			boolean clanWarKill = (targetPlayer.getClan() != null && getClan() != null && !isAcademyMember() && !(targetPlayer.isAcademyMember()) && _clan.isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getClan().isAtWarWith(_clan.getClanId()));
			if (clanWarKill && target instanceof L2PcInstance)
			{
				// 'Both way war' -> 'PvP Kill'
				increasePvpKills(targetPlayer.getLevel());
				return;
			}
			if (clanWarKill && target instanceof L2Summon)
				return;
			// 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0) // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL && target instanceof L2PcInstance)
					increasePvpKills(targetPlayer.getLevel());
			}
			else if (targetPlayer.getPvpFlag() == 0) // Target player doesn't have karma
			{
				increaseKarma(target.getLevel());
				if (target instanceof L2PcInstance)
					setPkKills(getPkKills() + 1);
				// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
				sendPacket(new UserInfo(this));
				//Unequip adventurer items
				if (getInventory().getPaperdollItemId(7) >= 7816 && getInventory().getPaperdollItemId(7) <= 7831)
				{
					L2ItemInstance invItem = getInventory().getItemByItemId(getInventory().getPaperdollItemId(7));
					if (invItem.isEquipped())
					{
						L2ItemInstance[] unequiped = getInventory().unEquipItemInSlotAndRecord(invItem.getLocationSlot());
						InventoryUpdate iu = new InventoryUpdate();
						for (L2ItemInstance itm : unequiped)
							iu.addModifiedItem(itm);
						sendPacket(iu);
					}
					refreshExpertisePenalty();
					sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_EQUIP_THIS_ITEM_WHEN_YOUR_PK_COUNT_IS_GREATER_THAN_OR_EQUAL_TO_ONE);
				}
			}
		}
	}

	/**
	 * Increase the faction points depending on level
	 * PK Kills give half the points of a PVP Kill
	 */
	public void increaseFactionKillPoints(int level, boolean pk)
	{
		int points;
		points = (level / getLevel()) * (Config.FACTION_KILL_RATE / 100);
		if (pk)
			points /= 2;
		_faction.addFactionPoints(points);
		sendMessage("Вы получили " + String.valueOf(points) + " Facion Points");
	}

	/**
	 * Increase the pvp kills count and send the info to the player
	 *
	 */
	private int _lastKilledPlayer = 0;
	private int _nowKilled = 0;
	private long _lastReward = 0;
	public void increasePvpKills(int level)
	{
		if (_event!=null && _event.isRunning())
			return;

		// Add karma to attacker and increase its PK counter
		setPvpKills(getPvpKills() + 1);

		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
		try {
		if (Config.ALLOW_PVP_REWARD && _lastKilledPlayer != _nowKilled)
		{
			if(Config.PVP_CHECK_HWID) {
				if(getHWid()==null)
					return;
				L2PcInstance viktim = L2World.getInstance().getPlayer(_nowKilled);
				if(viktim==null || viktim.getHWid()==null)
					return;
				if(L2GameClient.isSameHWID(viktim.getHWid(), getHWid()))
					return;
			}
				
			if(System.currentTimeMillis()-_lastReward < Config.PVP_REWARD_TIME*1000)
				return;
			if (level >= Config.PVP_REWARD_LEVEL)
			{
				_lastReward  = System.currentTimeMillis(); 
				addItem("Loot", Config.PVP_REWARD_ITEM_ID, Config.PVP_REWARD_ITEM_AMMOUNT, this, true);
				sendMessage(Message.getMessage(this, Message.MessageId.MSG_EARN_PVP_BONUS));
			}
		}
		} finally {
			_lastKilledPlayer = _nowKilled;
		}
	}

	/**
	 * Increase karma
	 *
	 * @param targLVL : level of the killed player
	 */
	public void increaseKarma(int targLVL)
	{
		if (_event!=null && _event.isRunning())
			return;

		int baseKarma = (int) (Config.KARMA_MIN_KARMA * Config.KARMA_RATE);
		int newKarma = baseKarma;
		int karmaLimit = (int) (Config.KARMA_MAX_KARMA * Config.KARMA_RATE);

		int pkLVL = getLevel();
		int pkPKCount = getPkKills();

		int lvlDiffMulti = 0;
		int pkCountMulti = 0;

		// Check if the attacker has a PK counter greater than 0
		if (pkPKCount > 0)
			pkCountMulti = pkPKCount / 2;
		else
			pkCountMulti = 1;
		if (pkCountMulti < 1)
			pkCountMulti = 1;

		// Calculate the level difference Multiplier between attacker and killed L2PcInstance
		if (pkLVL > targLVL)
			lvlDiffMulti = pkLVL / targLVL;
		else
			lvlDiffMulti = 1;
		if (lvlDiffMulti < 1)
			lvlDiffMulti = 1;

		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		newKarma = (int) (newKarma * pkCountMulti * lvlDiffMulti * Config.KARMA_RATE);

		// Make sure newKarma is less than karmaLimit and higher than baseKarma
		if (newKarma < baseKarma)
			newKarma = baseKarma;
		if (newKarma > karmaLimit)
			newKarma = karmaLimit;

		// Fix to prevent overflow (=> karma has a  max value of 2 147 483 647)
		if (getKarma() > (Integer.MAX_VALUE - newKarma))
			newKarma = Integer.MAX_VALUE - getKarma();

		// Add karma to attacker
		setKarma(getKarma() + newKarma);
	}

	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a Player Killer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level

		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;

		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
			karmaLost = Integer.MAX_VALUE;
		else
			karmaLost = (int) expGained;

		if (karmaLost < Config.KARMA_LOST_BASE)
			karmaLost = Config.KARMA_LOST_BASE;
		if (karmaLost > getKarma())
			karmaLost = getKarma();

		return karmaLost;
	}

	public void updatePvPStatus()
	{
		if (_event!=null && _event.isRunning())
			return;

		if (isInsideZone(L2Zone.FLAG_PVP))
			return;
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);

		if (getPvpFlag() == 0)
			startPvPFlag();
	}

	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = target.getActingPlayer();

		if (player_target == null)
			return;
		if (_event!=null && _event.isRunning())
			return;

		if ((isInDuel() && player_target.getDuelId() == getDuelId()))
			return;
		if ((!isInsideZone(L2Zone.FLAG_PVP) || !player_target.isInsideZone(L2Zone.FLAG_PVP)) && player_target.getKarma() == 0)
		{
			if (checkIfPvP(player_target))
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			else
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			if (getPvpFlag() == 0)
				startPvPFlag();
		}
	}

	/**
	 * Restore the specified % of experience this L2PcInstance has
	 * lost and sends a Server->Client StatusUpdate packet.<BR><BR>
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp((int) Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}

	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Calculate the Experience loss </li>
	 * <li>Set the value of _expBeforeDeath </li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary </li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience </li><BR><BR>
	 *
	 */
	public void deathPenalty(boolean atwar, boolean killed_by_pc, boolean charmOfCourage)
	{
		//FIXME: Need Correct Penalty

		// Get the level of the L2PcInstance
		final int lvl = getLevel();

		byte level = (byte) getLevel();

		int clan_luck = getSkillLevel(L2Skill.SKILL_CLAN_LUCK);

		double clan_luck_modificator = 1.0;

		if (!killed_by_pc)
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.8;
					break;
				case 2:
					clan_luck_modificator = 0.8;
					break;
				case 1:
					clan_luck_modificator = 0.88;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;
			}
		}
		else
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.5;
					break;
				case 2:
					clan_luck_modificator = 0.5;
					break;
				case 1:
					clan_luck_modificator = 0.5;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;
			}
		}

		//The death steal you some Exp
		double percentLost = (clan_luck_modificator);

		switch (level)
		{
			case 80:
			case 79:
			case 78:
				percentLost = (1.5 * clan_luck_modificator);
				break;
			case 77:
				percentLost = (2.0 * clan_luck_modificator);
				break;
			case 76:
				percentLost = (2.5 * clan_luck_modificator);
				break;
			default:
				if (level < 40)
					percentLost = (7.0 * clan_luck_modificator);
				else if (level >= 40 && level <= 75)
					percentLost = (4.0 * clan_luck_modificator);
				break;
		}

		if (getKarma() > 0)
			percentLost *= Config.RATE_KARMA_EXP_LOST;

		if (isFestivalParticipant() || atwar)
			percentLost /= 4.0;

		// Calculate the Experience loss
		long lostExp = 0;
		if (_event==null || _event.canLostExpOnDie())
		{
			if (lvl < Experience.MAX_LEVEL)
				lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
			else
				lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost / 100);

			if (killed_by_pc)
				lostExp = (long) calcStat(Stats.LOST_EXP_PVP, lostExp, null, null);
			else
				lostExp = (long) calcStat(Stats.LOST_EXP, lostExp, null, null);
		}

		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());

		if (charmOfCourage && getSiegeState() > 0 && isInsideZone(L2Zone.FLAG_SIEGE))
			return;

		if (killed_by_pc && ((isInsideZone(L2Zone.FLAG_PVP) && !isInsideZone(L2Zone.FLAG_SIEGE))
				|| (isInsideZone(L2Zone.FLAG_SIEGE) && getSiegeState() > 0)))
			return;

		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}

	public void deathPenalty(boolean atWar, boolean killedByPc)
	{
		deathPenalty(atWar, killedByPc, getCharmOfCourage());
	}

	public boolean isLookingForParty()
	{
		return _lookingForParty;
	}	
	
	public void setLookingForParty(boolean matching)
	{
		_lookingForParty = matching;
	}	
	
	public boolean getPartyMatchingLevelRestriction()
	{
		return !_partyMatchingAllLevels;
	}
	
	public void setPartyMatchingShowClass(boolean par) {
		
	}

	public int getPartyMatchingRegion()
	{
		return _partyMatchingRegion;
	}
	public void setPartyMatchingRegion(int region)
	{
		_partyMatchingRegion = region;
	}
	
	public L2PartyRoom getPartyRoom()
	{
		return _partyRoom;
	}

	/**
	 * Set the _partyRoom object of the L2PcInstance (without joining it).
	 * @param room new party room
	 */
	public void setPartyRoom(L2PartyRoom room)
	{
		_partyRoom = room;
	}	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the RegenActive flag to false </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
	 *
	 */
	public void stopAllTimers()
	{
		getStatus().stopHpMpRegeneration();
		stopWarnUserTakeBreak();
//		stopAutoSaveTask();
		stopWaterTask();
		stopFeed();
		clearPetData();
		storePetFood(_mountNpcId);
		stopChargeTask();
		stopPvPFlag();
		stopJailTask();
		stopFameTask();
	}

	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR><BR>
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}

	/**
	 * Return the L2Decoy of the L2PcInstance or null.<BR><BR>
	 */

	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}

	/**
	 * Set the L2Decoy of the L2PcInstance.<BR><BR>
	 */

	/**
	 * Set the L2Trap of this L2PcInstance<BR><BR>
	 * @param trap
	 */

	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR><BR>
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}

	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}

	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public L2Request getRequest()
	{
		if (_request == null)
			_request = new L2Request(this);
		return _request;
	}

	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
		if(_activeRequester!=null)
			ThreadPoolManager.getInstance().schedule(new Runnable() {
				public void run() {
					_activeRequester = null;
					_requestExpireTime = 0;
				}
			},REQUEST_TIMEOUT * 10000 );
	}

	/**
	 * Return true if last request is expired.
	 * @return
	 */
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > System.currentTimeMillis());
	}

	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public L2PcInstance getActiveRequester()
	{
		return _activeRequester;
	}

	/**
	 * Return true if a transaction is in progress.<BR><BR>
	 */
	public boolean isProcessingRequest()
	{
		return _requestExpireTime > System.currentTimeMillis() || _activeRequester != null;
	}

	/**
	 * Return true if a transaction is in progress.<BR><BR>
	 */
	public boolean isProcessingTransaction()
	{
		return _requestExpireTime > System.currentTimeMillis() ||  _activeTradeList != null;
	}

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		setActiveEnchantItem(null);
		_requestExpireTime = System.currentTimeMillis() + REQUEST_TIMEOUT * 1000;
		partner.setActiveRequester(this);
	}

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}

	/**
	 * Return active Warehouse.<BR><BR>
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}

	/**
	 * Select the TradeList to be used in next activity.<BR><BR>
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}

	/**
	 * Return active TradeList.<BR><BR>
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}

	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		setActiveEnchantItem(null);
		SystemMessage msg = new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}

	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CONFIRMED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		//sendPacket(new TradeOtherDone());
	}

	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
			return;

		_activeTradeList.lock();
		_activeTradeList = null;
		sendPacket(new TradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANCELED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}

	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull) {
			getInventory().updateDatabase();
			sendPacket(SystemMessageId.TRADE_SUCCESSFUL);
		}
	}

	public void startTrade(L2PcInstance partner)
	{
		setActiveEnchantItem(null);
		onTradeStart(partner);
		partner.onTradeStart(this);
	}

	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
			return;
		setTrading(false);
		setActiveEnchantItem(null);
		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
			partner.onTradeCancel(this);
		onTradeCancel(this);
	}

	/**
	 * Return the _createList object of the L2PcInstance.<BR><BR>
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}

	/**
	 * Set the _createList object of the L2PcInstance.<BR><BR>
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}

	/**
	 * Return the _buyList object of the L2PcInstance.<BR><BR>
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
			_sellList = new TradeList(this);
		return _sellList;
	}

	/**
	 * Return the _buyList object of the L2PcInstance.<BR><BR>
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
			_buyList = new TradeList(this);
		return _buyList;
	}

	/**
	 * Set the Private Store type of the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
	}

	/**
	 * Return the Private Store type of the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}

	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.<BR><BR>
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}

	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.<BR><BR>
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}

	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.<BR><BR>
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");

		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_subPledgeType = 0;
			_pledgeRank = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			return;
		}

		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}

		_clanId = clan.getClanId();
	}

	/**
	 * Return the _clan object of the L2PcInstance.<BR><BR>
	 */
	public L2Clan getClan()
	{
		return _clan;
	}

	/**
	 * Return true if the L2PcInstance is the leader of its clan.<BR><BR>
	 */
	public boolean isClanLeader()
	{
		return (getClan() != null) && getObjectId() == getClan().getLeaderId();
	}

	/**
	 * Disarm the player's weapon and shield.<BR><BR>
	 */
	public boolean disarmWeapons()
	{
		if (isCursedWeaponEquipped())
			return false;

		// Unequip the weapon
		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
			wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (wpn != null)
		{
			if (wpn.isWear())
				return false;

			L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequipped)
				iu.addModifiedItem(element);
			sendPacket(iu);

			abortAttack();
			refreshExpertisePenalty();
			broadcastUserInfo(true);

			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequipped.length > 0)
			{
				SystemMessage sm = null;
				if (unequipped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequipped[0].getEnchantLevel());
					sm.addItemName(unequipped[0]);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequipped[0]);
				}
				sendPacket(sm);
			}
		}

		// Unequip the shield
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isWear())
				return false;

			L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequipped)
				iu.addModifiedItem(element);
			sendPacket(iu);

			abortAttack();
			refreshExpertisePenalty();
			broadcastUserInfo(true);

			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequipped.length > 0)
			{
				SystemMessage sm = null;
				if (unequipped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequipped[0].getEnchantLevel());
					sm.addItemName(unequipped[0]);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequipped[0]);
				}
				sendPacket(sm);
			}
		}
		return true;
	}

	/**
	 * Reduce the number of arrows/bolts owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).<BR><BR>
	 */
	@Override
	protected void reduceArrowCount(boolean bolts)
	{
		L2ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		if (arrows == null)
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			sendPacket(new ItemList(this, false));
			return;
		}

		if(!Config.CONSUME_SPIRIT_SOUL_SHOTS)
			return;

		// Adjust item quantity
		if (arrows.getCount() > 1)
		{
			synchronized (arrows)
			{
				arrows.changeCountWithoutTrace(-1, this, null);
				arrows.setLastChange(L2ItemInstance.MODIFIED);

				// could do also without saving, but let's save approx 1 of 10
				getInventory().refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			getInventory().destroyItem("Consume", arrows, this, null);

			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;

			sendPacket(new ItemList(this, false));
			return;
		}

		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(arrows);
			sendPacket(iu);
		}
	}

	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return true.<BR><BR>
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equipped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the L2ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
				// Send inventory update packet
				getInventory().updateInventory(_arrowItem);
			}
		}
		else
			// Get the L2ItemInstance of arrows equipped in left hand
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		return _arrowItem != null;
	}

	/**
	 * Equip bolts needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return true.<BR><BR>
	 */

	public boolean mount(L2Summon pet)
	{
		if (!isInsideRadius(pet, 80, true, false))
		{
			sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_STRIDER_TO_MOUNT);
			return false;
		}

		if (!GeoData.getInstance().canSeeTarget(this, pet))
		{
			sendPacket(SystemMessageId.CANT_SEE_TARGET);
			return false;
		}

		if (!disarmWeapons())
			return false;

		for (L2Effect e : getAllEffects())
		{
			if (e != null && e.getSkill().isToggle() && e.getSkill().getId() != 5399)
				e.exit();
		}

		Ride mount = new Ride(this, true, pet.getTemplate().getNpcId());
		setMount(pet.getNpcId(), pet.getLevel(), mount.getMountType());
		setMountObjectID(pet.getControlItemId());
		clearPetData();
		startFeed(pet.getNpcId());
		broadcastPacket(mount);
		broadcastUserInfo(true);
		pet.unSummon(this);
		return true;
	}

	public boolean remount(L2PcInstance player)
	{
		Ride dismount = new Ride(this, false, 0);
		Ride mount = new Ride(this, true, getMountNpcId());

		player.sendPacket(dismount);
		player.sendPacket(mount);
		return true;
	}

	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons())
			return false;

		for (L2Effect e : getAllEffects())
		{
			if (e != null && e.getSkill().isToggle() && e.getSkill().getId() != 5399)
				e.exit();
		}

		Ride mount = new Ride(this, true, npcId);
		if (setMount(npcId, getLevel(), mount.getMountType()))
		{
			clearPetData();
			setMountObjectID(controlItemObjId);
			broadcastPacket(mount);
			// Notify self and others about speed change
			broadcastUserInfo(true);
			if (useFood)
				startFeed(npcId);
			return true;
		}
		return false;
	}

	public boolean mountPlayer(L2Summon pet)
	{
		if (pet != null && pet.isMountable() && !isMounted() && !isBetrayed() && !pet.isOutOfControl())
		{
			if (_event!=null && !_event.canDoAction(this, RequestActionUse.ACTION_MOUNT))
			{
				return false;
			}
			
			else if (isParalyzed() || isPetrified())
			{
				// You cannot mount a steed while petrified.
				return false;
			}
			else if (isDead())
			{
				//A strider cannot be ridden when dead
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
				return false;
			}
			else if (pet.isDead())
			{
				//A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted() || pet.isParalyzed() || pet.isPetrified())
			{
				//A strider in battle cannot be ridden
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
				return false;
			}
			else if (isInCombat())
			{
				//A strider cannot be ridden while in battle
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}
			else if (isSitting() || isInsideZone(L2Zone.FLAG_WATER))
			{
				//A strider can be ridden only when standing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
				return false;
			}
			else if (isFishing())
			{
				//You can't mount, dismount, break and drop items while fishing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
				return false;
			}
			else if (isInDuel())
			{
				// You cannot mount a steed while in a duel.
				return false;
			}
			else if (isCursedWeaponEquipped())
			{
				// no message needed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (getInventory().getItemByItemId(Config.FORTSIEGE_COMBAT_FLAG_ID) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (isCastingNow())
			{
				// You cannot mount a steed while skill casting.
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}
			else if (!Util.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_STRIDER_TO_MOUNT);
				return false;
			}
			else if (!pet.isDead() && !isMounted())
				mount(pet);
		}
		else if (isRentedPet())
			stopRentPet();
		else if (isMounted())
		{
			if (isInCombat() && getMountType()!=1) // в бою можно слезть со Страйдера
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (getMountType() == 2 && isInsideZone(L2Zone.FLAG_NOLANDING))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.NO_DISMOUNT_HERE));
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
				return false;
			}
			else if (ObjectRestrictions.getInstance().checkRestriction(this, AvailableRestriction.PlayerUnmount))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage(Message.getMessage(this, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
				return false;
			}
			else
				dismount();
		}
		return true;
	}

	public boolean dismount()
	{
		sendPacket(new SetupGauge(3, 0, 0));
		int petId = _mountNpcId;
		if (setMount(0, 0, 0 ))
		{
			stopFeed();
			clearPetData();

			if (isFlying())
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			Ride dismount = new Ride(this, false, 0);
			broadcastPacket(dismount);
			setMountObjectID(0);
			storePetFood(petId);
			
			// Notify self and others about speed change
			broadcastUserInfo(true);
			return true;
		}
		return false;
	}

	/**
	 * Return true if the L2PcInstance use a dual weapon.<BR><BR>
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		L2Weapon weaponItem = getActiveWeaponItem();

		if (weaponItem == null)
			return false;
		if (weaponItem.getItemType() == L2WeaponType.DUAL)
			return true;
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
			return true;
		else if (weaponItem.getItemId() == 248) // orc fighter fists
			return true;
		else return weaponItem.getItemId() == 252;
	}

	public void setUptime(long time)
	{
		_uptime = time;
	}

	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}

	public long getOnlineTime()
	{
		long totalOnlineTime = _onlineTime;

		if (_onlineBeginTime > 0)
			totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;

		return totalOnlineTime;
	}

	/**
	 * Return true if the L2PcInstance is invulnerable.<BR><BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || _protectEndTime > GameTimeController.getGameTicks();
	}

	public boolean isMiniMapOpen()
	{
		return _miniMapOpen; 
	}
	
	public void setMiniMapOpen(boolean par)
	{
		_miniMapOpen = par;		
	}

	/**
	 * Return true if the L2PcInstance has a Party in progress.<BR><BR>
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}

	/**
	 * Set the _party object of the L2PcInstance (without joining it).<BR><BR>
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}

	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR><BR>
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
			// First set the party otherwise this wouldn't be considered
			// as in a party into the L2Character.updateEffectIcons() call.
			_party = party;
			if (!party.addPartyMember(this))
				_party = null;
		}
	}

	/**
	 * Manage the Leave Party task of the L2PcInstance.<BR><BR>
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this,false);
			_party = null;
		}
	}

	/**
	 * Return the _party object of the L2PcInstance.<BR><BR>
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}

	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}

	@Override
	public double getLevelMod()
	{
		return (89 + getLevel()) / 100.0;
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 * @param flag
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		broadcastRelationChanged();
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 */
	public void broadcastKarma()
	{
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);
		broadcastUserInfo(true);
	}

	/**
	 * Set the online Flag to true or false and update the characters table of the database with online status and lastAccess (called when login and logout).<BR><BR>
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		final byte value = isOnline ? ONLINE_STATE_ONLINE : ONLINE_STATE_DELETED;
		
		if (_isOnline != value)
		{
			_isOnline = value;
			
			// Update the characters table of the database with online status and lastAccess (called when login and logout)
			updateOnlineStatus();
		}
	}

	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}

	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).<BR><BR>
	 */
	public void updateOnlineStatus()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();

			int nbPlayerIG = L2World.getInstance().getAllPlayersCount();
			int maxPlayer = RecordTable.getInstance().getMaxPlayer();
			if (nbPlayerIG > maxPlayer)
			{
				ServerData.getInstance().getData().set("Records.MaxPlayers",nbPlayerIG);
				ServerData.getInstance().getData().set("Records.RecordDate",Calendar.getInstance().getTime().toString());
			}
		}
		catch (Exception e)
		{
			_log.error("Failed updating character online status.", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Create a new player in the characters table of the database.<BR><BR>
	 */
	private boolean createDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(CREATE_CHARACTER);
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getStatus().getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getStatus().getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getStatus().getCurrentMp());
			statement.setInt(11, getAppearance().getFace());
			statement.setInt(12, getAppearance().getHairStyle());
			statement.setInt(13, getAppearance().getHairColor());
			statement.setInt(14, getAppearance().getSex() ? 1 : 0);
			statement.setLong(15, getExp());
			statement.setInt(16, getSp());
			statement.setInt(17, getKarma());
			statement.setInt(18, getFame());
			statement.setInt(19, getPvpKills());
			statement.setInt(20, getPkKills());
			statement.setInt(21, getClanId());
			statement.setInt(22, getRace().ordinal());
			statement.setInt(23, getClassId().getId());
			statement.setLong(24, getDeleteTimer());
			statement.setInt(25, hasDwarvenCraft() ? 1 : 0);
			statement.setString(26, getTitle());
			statement.setInt(27, isOnline());
			statement.setInt(28, isIn7sDungeon() ? 1 : 0);
			statement.setInt(29, getClanPrivileges());
			statement.setInt(30, getWantsPeace());
			statement.setInt(31, getBaseClass());
			statement.setInt(32, getNewbie());
			statement.setInt(33, isNoble() ? 1 : 0);
			statement.setLong(34, 0);
			statement.setLong(35, System.currentTimeMillis());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Could not insert char data: ", e);
			return false;
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		return true;
	}



	private void createHSdb(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_herolist (charId,enddate) values(?,?)");
			statement.setInt(1, player.getObjectId());
			statement.setLong(2, 0);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Could not insert char data: ", e);
			return;
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static L2PcInstance load(int objectId)
	{
		disconnectIfOnline(objectId);
		
		L2PcInstance player = null;
		Connection con = null;

		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection(con);

			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();

			double currentHp = 1, currentMp = 1, currentCp = 1;
			if (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex") != 0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);

				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");
				player._baseExp = rset.getLong("exp");
				player._baseSP = rset.getInt("sp");
				player._baseLevel = rset.getByte("level");
				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				
				player.getStat().setLevel((byte)player._baseLevel);
				player.getStat().setSp(rset.getInt("sp"));
				player.setWantsPeace(rset.getInt("wantspeace"));
				player.setHeading(rset.getInt("heading"));
				player.setKarma(rset.getInt("karma"));
				player._pccaffe = rset.getInt("pccaffe_points");
				player._hwid = rset.getString("hwid");
				player.setFame(rset.getInt("fame"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
					player.setClanJoinExpiryTime(0);
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
					player.setClanCreateExpiryTime(0);

				int clanId = rset.getInt("clanid");

				if (clanId > 0)
					player.setClan(ClanTable.getInstance().getClan(clanId));

				player.setDeleteTimer(rset.getLong("deletetime"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie"));
				player.setNoble(rset.getInt("nobless") == 1);

				String title = rset.getString("title");
				if (title == null)
					player.setTitle(" ");
				else
					player.setTitle(title);

				
				player.setIsBanned(rset.getBoolean("isBanned"));

				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());

				// Only 1 line needed for each and their values only have to be set once as long as you don't die before it's set.
				currentHp = rset.getDouble("curHp");
				currentMp = rset.getDouble("curMp");
				currentCp = rset.getDouble("curCp");

				//Check recs
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));
				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));

				player._classIndex = 0;
				try
				{
					player.setBaseClass(rset.getInt("base_class"));
				}
				catch (Exception e)
				{
					player.setBaseClass(activeClassId);
				}

				// Restore Subclass Data (cannot be done earlier in function)
				if (restoreSubClassData(player))
				{
					if (activeClassId != player.getBaseClass())
					{
						for (SubClass subClass : player.getSubClasses().values())
						{
							if (subClass.getClassId() == activeClassId)
								player._classIndex = subClass.getClassIndex();
						}
					}
				}
				if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass())
				{
					// Subclass in use but doesn't exist in DB -
					// a possible restart-while-modifysubclass cheat has been attempted.
					// Switching to use base class
					player.setClassId(player.getBaseClass());
					_log.warn("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
				}
				else
					player._activeClass = activeClassId;

				player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
				player.setInJail(rset.getInt("in_jail") == 1);
				player.setJailTimer(rset.getLong("jail_timer"));
				if (player.isInJail())
					player.setJailTimer(rset.getLong("jail_timer"));
				else
					player.setJailTimer(0);

				CursedWeaponsManager.getInstance().onEnter(player);

				player.setNoble(rset.getBoolean("nobless"));
				player.setSubPledgeType(rset.getInt("subpledge"));
				player.setPledgeRank(rset.getInt("pledge_rank"));
				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				if (player.getClan() != null)
				{
					if (player.getClan().getLeaderId() != player.getObjectId())
					{
						if (player.getPledgeRank() == 0)
							player.setPledgeRank(5);
						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPledgeRank()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPledgeRank(1);
					}
				}
				else
					player.setClanPrivileges(L2Clan.CP_NOTHING);

				player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
				player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));
				player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.getPosition().setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();

				while (chars.next())
				{
					Integer charId = chars.getInt("charId");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}
				chars.close();
				stmt.close();
			}

			rset.close();
			statement.close();

			if (player == null)
				return null;

			// Retrieve from the database all secondary data of this L2PcInstance
			// and reward expertise/lucky skills if necessary.
			player.restoreCharData(con);
			player.rewardSkills();
			player.loadSetting(con);
			
			// buff and status icons
			if (Config.STORE_SKILL_COOLTIME)
				player.restoreEffects();

			if (player.getAllEffects() != null)
			{
				for (L2Effect e : player.getAllEffects())
				{
					if (e.getEffectType() == L2EffectType.HEAL_OVER_TIME)
					{
						player.stopEffects(L2EffectType.HEAL_OVER_TIME);
						player.removeEffect(e);
					}
					else if (e.getEffectType() == L2EffectType.COMBAT_POINT_HEAL_OVER_TIME)
					{
						player.stopEffects(L2EffectType.COMBAT_POINT_HEAL_OVER_TIME);
						player.removeEffect(e);
					}
				}
			}

			// Restore current Cp, HP and MP values
			player.getStatus().setCurrentCp(currentCp);
			player.getStatus().setCurrentHp(currentHp);
			player.getStatus().setCurrentMp(currentMp);

			if (currentHp < 0.5)
			{
				player.setIsDead(true);
				player.getStatus().stopHpMpRegeneration();
			}

			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if (player.getPet() != null)
				player.getPet().setOwner(player);

			// Update the overloaded status of the L2PcInstance
			player.refreshOverloaded();
			// Update the expertise status of the L2PcInstance
			player.refreshExpertisePenalty();
			// Set UpTime
			player.setUptime(System.currentTimeMillis());
		}
		catch (Exception e)
		{
			_log.error("Failed loading character.", e);
			e.printStackTrace();	
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		return player;
	}

	private static void HStimeOver(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(UPDATE_HEROSERVICE);
			statement.setLong(1, 0);
			statement.setInt(2, player.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("HeroService: Could not increase data");
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void restoreHeroServiceData(L2PcInstance player)
	{
		boolean sucess = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(RESTORE_HEROSERVICE);
			statement.setInt(1, player.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				sucess = true;
				long enddate = rset.getLong("enddate");

				if (enddate <= System.currentTimeMillis())
				{
					if (enddate != 0)
						PcAction.deleteHeroItems(player);
					HStimeOver(player);
				}
				else
				{
					player.setHero(true);
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("HeroService: Could not restore HeroStatus data for " + player.getName() + ": ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		if (sucess == false)
			player.createHSdb(player);
	}


	/**
	 * Restores sub-class data for the L2PcInstance, used to check the current
	 * class index for the character.
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
			statement.setInt(1, player.getObjectId());

			ResultSet rset = statement.executeQuery();
			int i = 1;
			while (rset.next())
			{
				SubClass subClass = new SubClass();
				subClass.setClassId(rset.getInt("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setClassIndex(rset.getInt("class_index"));

				// Enforce the correct indexing of _subClasses against their class indexes.
				player.getSubClasses().put(subClass.getClassIndex(), subClass);
				i++;
				if(i>Config.MAX_SUBCLASS)
					break;
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Could not restore classes for " + player.getName() + ": ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		return true;
	}

	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData(Connection con) throws SQLException
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
			restoreSkills(con);

			// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
			getMacroses().restore(con);

			// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
			getShortCuts().restore(con);

			// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
			restoreHenna(con);

			// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
			if (Config.ALT_RECOMMEND)
				restoreRecom(con);

			// Retrieve from the database the recipe book of this L2PcInstance.
			restoreRecipeBook(con);
			
	}

	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.<BR><BR>
	 */
	private void restoreRecom(Connection con)
	{

		try
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("could not restore recommendations: ", e);
		}
	}

	/**
	 * Store recipe book data for this L2PcInstance, if not on an active sub-class.
	 */
	private void storeRecipeBook(Connection con) throws SQLException
	{
		// If the player is on a sub-class don't even attempt to store a recipe book.
		//if (isSubClassActive()) return;
		if (getCommonRecipeBook().length == 0 && getDwarvenRecipeBook().length == 0)
			return;

			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();

			L2RecipeList[] recipes = getCommonRecipeBook();

			for (L2RecipeList element : recipes)
			{
				statement = con.prepareStatement("REPLACE INTO character_recipebook (charId, id, type) values(?,?,0)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, element.getId());
				statement.execute();
				statement.close();
			}

			recipes = getDwarvenRecipeBook();
			for (int count = 0; count < recipes.length; count++)
			{
				statement = con.prepareStatement("REPLACE INTO character_recipebook (charId, id, type) values(?,?,1)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipes[count].getId());
				statement.execute();
				statement.close();
			}
	}

	/**
	 * Restore recipe book data for this L2PcInstance.
	 */
	private void restoreRecipeBook(Connection con)
	{

		try
		{
			PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE charId=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();

			L2RecipeList recipe;
			while (rset.next())
			{
				recipe = RecipeController.getInstance().getRecipeList(rset.getInt("id"));

				if (rset.getInt("type") == 1)
					registerDwarvenRecipeList(recipe);
				else
					registerCommonRecipeList(recipe);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Could not restore recipe book data:", e);
		}
	}

	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR><BR>
	 */
	public long _lastStore = 0;

	
	public synchronized void intemediateStore() {
		if(System.currentTimeMillis()>_lastStore+300000)
			return;
		_lastStore = System.currentTimeMillis();
		store(false);
	}
	public synchronized void store()
	{
		store(false);
	}

	public synchronized void store(boolean items)
	{
		 
		Connection con = null;
		try {
			con = L2DatabaseFactory.getInstance().getConnection();

			if (_jailTask != null){
				long delay = _jailTask.getDelay(TimeUnit.MILLISECONDS);
				if (delay < 0)
					delay = 0;
				setJailTimer(delay);
			}
			try {
				storeCharBase(con);
			} catch(Exception e) {
				_log.error("L2PcInstance: Error saving character "+getName()+", bugload possible",e);
				e.printStackTrace();
			}	
			try {
				storeCharSub(con);
			} catch(Exception e) {
				_log.error("L2PcInstance: Error saving character "+getName()+" subclasses, not fatal",e);
			}	
			try {	
				storeRecipeBook(con);
			} catch(Exception e) {
				_log.error("L2PcInstance: Error saving character "+getName()+" recipies, not fatal",e);
			}	
			try {
				saveSettingInDb(con);
			} catch(Exception e) {
				_log.error("L2PcInstance: Error saving character "+getName()+" settings, not fatal",e);
			}	
				
			if (items) { 
				getInventory().updateDatabase();
				if(_warehouse!=null)
					_warehouse.updateDatabase();
				if(_freight!=null)
					_freight.updateDatabase();
				SQLQueue.getInstance().run();
			}
			
		} catch(SQLException e) {
			_log.error("L2PcInstance: Connection to DB lost",e);
		} finally {
			if(con!=null) try {
				con.close();
			} catch(SQLException e) {}
		}
		
	}

	private void storeCharBase(Connection con) throws SQLException 
	{
			long totalOnlineTime = _onlineTime;
			if (_onlineBeginTime > 0)
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;

			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER);
			statement.setInt(1, _classIndex==0?getLevel():_baseLevel);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getStatus().getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getStatus().getCurrentMp());
			statement.setInt(8, getAppearance().getFace());
			statement.setInt(9, getAppearance().getHairStyle());
			statement.setInt(10, getAppearance().getHairColor());
			statement.setInt(11, getHeading());
			statement.setInt(12, _observerMode ? _obsX : getX());
			statement.setInt(13, _observerMode ? _obsY : getY());
			statement.setInt(14, _observerMode ? _obsZ : getZ());
			statement.setLong(15, _classIndex==0?getStat().getExp():_baseExp);
			statement.setLong(16, getExpBeforeDeath());
			statement.setInt(17, _classIndex==0?getStat().getSp():_baseSP);
			statement.setInt(18, getKarma());
			statement.setInt(19, getFame());
			statement.setInt(20, getPvpKills());
			statement.setInt(21, getPkKills());
			statement.setInt(22, getRecomHave());
			statement.setInt(23, getRecomLeft());
			statement.setInt(24, getClanId());
			statement.setInt(25, getRace().ordinal());
			statement.setInt(26, getClassId().getId());
			statement.setLong(27, getDeleteTimer());
			statement.setString(28, _title != null ? _title : "");
			statement.setInt(29, isOnline());
			statement.setInt(30, isIn7sDungeon() ? 1 : 0);
			statement.setInt(31, getClanPrivileges());
			statement.setInt(32, getWantsPeace());
			statement.setInt(33, getBaseClass());
			statement.setLong(34, totalOnlineTime);
			statement.setInt(35, isInJail() ? 1 : 0);
			statement.setLong(36, getJailTimer());
			statement.setInt(37, getNewbie());
			statement.setInt(38, isNoble() ? 1 : 0);
			statement.setLong(39, getPledgeRank());
			statement.setInt(40, getSubPledgeType());
			statement.setLong(41, getLastRecomUpdate());
			statement.setInt(42, getLvlJoinedAcademy());
			statement.setLong(43, getApprentice());
			statement.setLong(44, getSponsor());
			statement.setInt(45, getAllianceWithVarkaKetra());
			statement.setLong(46, getClanJoinExpiryTime());
			statement.setLong(47, getClanCreateExpiryTime());
			statement.setString(48, getName());
			statement.setLong(49, getDeathPenaltyBuffLevel());
			statement.setLong(50, _pccaffe);
			statement.setInt(51, isBanned() ? 1 : 0);
			statement.setString(52, getHWid());
			statement.setInt(53, getObjectId());
			statement.execute();
			statement.close();
	}
	
	private void storeCharSub(Connection con) throws SQLException
	{
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);

			if (getTotalSubClasses() > 0)
			{
				for (SubClass subClass : getSubClasses().values())
				{
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());

					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());

					statement.execute();
				}
			}
			statement.close();
	}

	private void storeEffect()
	{
		if (!Config.STORE_SKILL_COOLTIME)
			return;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);

			// Delete all current stored effects for char to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();

			int buff_index = 0;

			statement = con.prepareStatement(ADD_SKILL_SAVE);

			Set<Integer> storedSkills = new HashSet<Integer>();

			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			for (L2Effect effect : getAllEffects())
			{
				if (effect != null && !effect.isHerbEffect() && effect.getInUse() && !effect.getSkill().isToggle())
				{
					if (effect instanceof EffectFusion)
						continue;
					int skillId = effect.getSkill().getId();
					if (!storedSkills.add(skillId))
						continue;

					statement.setInt(1, getObjectId());
					statement.setInt(2, skillId);
					statement.setInt(3, effect.getSkill().getLevel());
					statement.setInt(4, effect.getCount());
					statement.setInt(5, effect.getTime());
					if (_reuseTimeStamps.containsKey(skillId))
					{
						TimeStamp t = _reuseTimeStamps.get(skillId);
						statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
						statement.setLong(7, t.hasNotPassed() ? t.getStamp() : 0 );
					}
					else
					{
						statement.setLong(6, 0);
						statement.setLong(7, 0);
					}
					statement.setInt(8, 0);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			// Store the reuse delays of remaining skills which
			// lost effect but still under reuse delay. 'restore_type' 1.
			for (TimeStamp t : _reuseTimeStamps.values())
			{
				if (t.hasNotPassed())
				{
					int skillId = t.getSkill();
					if (!storedSkills.add(skillId))
						continue;

					statement.setInt(1, getObjectId());
					statement.setInt(2, skillId);
					statement.setInt(3, -1);
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setLong(7, t.getStamp());
					statement.setInt(8, 1);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Could not store char effect data: ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Return true if the L2PcInstance is on line.<BR><BR>
	 */
	public int isOnline()
	{
		
		if(isOfflineTrade())
			return 2;
		return getClient()==null?0:1;
	}

	public byte getOnlineState()
	{
		return _isOnline;
	}
	
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}

	public L2Skill addSkill(L2Skill newSkill, boolean save)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);

		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (save)
			storeSkill(newSkill, oldSkill, -1);

		return oldSkill;
	}

	@Override
	public L2Skill addSkill(L2Skill newSkill)
	{
		return addSkill(newSkill, false);
	}

	@Override
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		return (store) ? removeSkill(skill) : super.removeSkill(skill, true);
	}


	public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect)
	{
		if (store)
			return removeSkill(skill);

		return super.removeSkill(skill, cancelEffect);
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the skill from the L2Character _skills </li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2PcInstance : Save update in the character_skills table of the database</li><BR><BR>
	 *
	 * @param skill The L2Skill to remove from the L2Character
	 *
	 * @return The L2Skill removed
	 *
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);

		Connection con = null;

		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);

			if (oldSkill != null)
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Error could not delete skill: ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		if (isCursedWeaponEquipped())
			return oldSkill;

		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}

		return oldSkill;
	}

	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database.
	 * <BR><BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 */
	private void updateSkill(Connection con, L2Skill newSkill, int classIndex) throws SQLException {
		PreparedStatement statement;
		statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
		statement.setInt(1, newSkill.getLevel());
		statement.setInt(2, newSkill.getId());
		statement.setInt(3, getObjectId());
		statement.setInt(4, classIndex);
		try {
			statement.execute();
		} finally {
			statement.close();
		}
	}
	private void addSkill(Connection con, L2Skill newSkill, int classIndex) throws SQLException {
		PreparedStatement statement;
		statement = con.prepareStatement(ADD_NEW_SKILL);
		statement.setInt(1, getObjectId());
		statement.setInt(2, newSkill.getId());
		statement.setInt(3, newSkill.getLevel());
		statement.setString(4, newSkill.getName());
		statement.setInt(5, classIndex);
		try {
			statement.execute();
		} finally {
			statement.close();
		}
	}
	
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		if (newSkill == null || newSkill.getId() > 369 && newSkill.getId() < 392)
			return;

		int classIndex = _classIndex;

		if (newClassIndex > -1)
			classIndex = newClassIndex;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			if (oldSkill != null)
				updateSkill(con,newSkill,classIndex);
			else try {
				addSkill(con,newSkill,classIndex);
			} catch(SQLException e) {
				if(e.getClass().getSimpleName().equals("MySQLIntegrityConstraintViolationException"))
					updateSkill(con,newSkill,classIndex);
			}
		}
		catch (Exception e)
		{
			_log.error("Error could not store char skills: ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
        
	/**
	 * Метод обновления скилов персонажа
	 * Проверяет скилы на сооветствие лвлу персонажа
	 * Если мин. уровень скила > уровня персонажа + 9 - удаляет или обновляет скил, если тот есть на данном уровне
	 * @param level
	 **/

	private void removeHighSkills()
	{
		try
		{
			// Сюда будем записывать проверенные нами скилы.
			Map<Integer, Integer> allowSkills = new HashMap<Integer, Integer>();

			// Собираем все доступные для изучения скилы, для доступных классов.
			for (ClassId classId : getClassId().getAllTree())
				if (classId != null)
					getAvalibleSkills(classId, allowSkills);


			L2Skill skill;
			int skillId, level;

			for (Entry<Integer, Integer> entry : allowSkills.entrySet())
			{
				skillId = entry.getKey();
				level = entry.getValue();

				// Получаем скилл, если он у нас изучен.
				skill = getKnownSkill(skillId);

				// Если не изучен или уровень скила в пределах нормы - пропускаем.
				if (skill == null || skill.getMagicLevel() - getLevel() < 9)
					continue;

				// Если в доступных скилах уровень 0 - удаляем.
				if (level == 0)
					// Удаляем скилл, если мы его еще не можем изучить.
					removeSkill(skill);
				else
				{
					// Из таблицы со скилами запрашиваем этот же скилл, но с уровнем из коллекции.
					skill = SkillTable.getInstance().getInfo(skill.getId(), level);
					addSkill(skill, true);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Записываем список доступных скилов для указанного класса в коллекцию.
	 * @param classId - требуемый класс.
	 * @param allowSkills - коллекция, куда записываем информацию о изучаемом скиле (ID, LEVEL)
	 * Если Level = 0 - мы не можем изучить скилл.
	 */
	private void getAvalibleSkills(ClassId classId, Map<Integer, Integer> allowSkills)
	{
		// Сбор разрешенных для класса игрока скилов
		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(classId);

		int level;

		// Проходим по списку доступных для данного класса скилов.
		for (L2SkillLearn sk : skillTree)
		{
			// Нужный момент! Если разница между минимальным уровнем и нашим уровнем меньше - 9
			// записываем данный скилл в коллекцию, которую в последствии будем анализровать.
			if (getLevel() >= sk.getMinLevel())
			{
				// Текущий максимальный доступный уровень скилла, берем либо из коллекции (если добавляли)
				// либо это - 0 (т.е. скил не доступен для изучения).
				level = allowSkills.containsKey(sk.getId()) ? allowSkills.get(sk.getId()) : 0;

				// Добавляем в коллекцию либо текущий уровень, либо новый, если он больше текущего.
				allowSkills.put(sk.getId(), Math.max(level, sk.getLevel()));
			}
			// Если в коллекции нет скила, который не соответствует требованию - добавляем с уровнем 0,
			// чтобы в последствии его удалить (если не будет доступного уровня).
			else if (!allowSkills.containsKey(sk.getId()))
					allowSkills.put(sk.getId(), 0);
		}
	}
        
	/**
	 * Метод проверки скилов<br>
	 * Проверяет скилы по классу, если скил непринадлежит классу, то удаляем<br>
	 * Исключает хиро скилы, ноблес скилы...
	 **/
	public void checkAllowedSkills()
	{
		// Скилы у GM'a не проверяются
		if (isGM())
			return;
		// Скилы у VIP при включенном конфиге не проверяются

		// Сбор разрешенных для класса игрока скилов
		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(getClassId());

		// Число удаленных скилов
		int Count = 0;

		// Цикл проверки скилов
		skill_loop: for (L2Skill skill : getAllSkills())
		{
			// Проверка NPE
			if (skill == null)
				continue skill_loop;
			// Получаем ID скила
			int skillid = skill.getId();

			// Проверка на изученные скилы для класса
			for (L2SkillLearn sk1 : skillTree)
			{
				if (sk1.getId() == skillid)
					continue skill_loop;
			}
			// Если дворянин и скил дворянский, то пропускаем
			if (isNoble() && NobleSkillTable.isNobleSkill(skillid))
				continue skill_loop;
			// Если герой и скил геройский, то пропускаем
			if (isHero() && HeroSkillTable.isHeroSkill(skillid))
				continue skill_loop;
			// Проверка сабовых скилов и т.д.
			if (AdditionalSkillTable.getInstance().isExSkill(skillid))
				continue skill_loop;
			// Exclude cursed weapon skills
			if (isCursedWeaponEquipped() && skillid == CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).getSkillId())
				continue skill_loop;
			// Exclude clan skills
			if (getClan() != null && (skillid >= 370 && skillid <= 391))
				continue skill_loop;
			// Exclude seal of ruler / build siege hq
			if (getClan() != null && getClan().getLeaderId() == getObjectId() && (skillid == 246 || skillid == 247))
				continue skill_loop;
			// Exclude fishing skills and common skills + dwarfen craft
			if (skillid >= 1312 && skillid <= 1322)
				continue skill_loop;
			if (skillid >= 1368 && skillid <= 1373)
				continue skill_loop;
			// Exclude sa / enchant bonus / penality etc. skills
			if (skillid >= 3000 && skillid < 7000)
				continue skill_loop;
			// Exclude SA from Pvp Amor
			if (skillid >= 8193 && skillid < 8233)
				continue skill_loop;
			// Exclude Skills from AllowedSkills in options.properties
			if (Config.ALLOWED_SKILLS_LIST.contains(skillid))
				continue skill_loop;

			// Remove skill
			if (skill != null)
			{
				removeSkill(skill);
				Count++;
			}
		}

		if (Count > 0)
			sendMessage(String.format(Message.getMessage(this, Message.MessageId.MSG_SKILL_REMOVED_ADMIN_INFORMED), Count));
	}
        
	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.<BR><BR>
	 */
	private void restoreSkills(Connection con)
	{

		try
		{
			boolean isAcumulative = Config.ACUMULATIVE_SUBCLASS_SKILLS;

			// Retrieve all skills of this L2PcInstance from the database
			PreparedStatement statement = con.prepareStatement(isAcumulative ? ACUMULATE_SKILLS_FOR_CHAR_SUB : RESTORE_SKILLS_FOR_CHAR);
			statement.setInt(1, getObjectId());
			if (!isAcumulative)
				statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");

				if (id > 9000 && id < 9007)
					continue; // fake skills for base stats

				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);

				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}

			rset.close();
			statement.close();

			// Restore clan skills
			if (_clan != null)
				_clan.addSkillEffects(this, false);
		}
		catch (Exception e)
		{
			_log.error("Could not restore character skills: ", e);
		}
	}

	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.<BR><BR>
	 */
	public void restoreEffects()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			ResultSet rset;
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 0);
			rset = statement.executeQuery();

			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");

				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");

				long remainingTime = systime - System.currentTimeMillis();

				if (skillId == -1 || effectCount == -1 || effectCurTime == -1 || reuseDelay < 0)
					continue;

				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
				if (skill != null)
					skill.getEffects(this, this);

				if (remainingTime > 10)
				{
					disableSkill(skillId, remainingTime);
					addTimeStamp(new TimeStamp(skillId, reuseDelay, systime));
				}

				for (L2Effect effect : getAllEffects())
				{
					if (effect.getSkill().getId() == skillId)
					{
						effect.setCount(effectCount);
						effect.setFirstTime(effectCurTime);
					}
					if (effect.getEffectType() == L2EffectType.CHARMOFCOURAGE)
					{
						setCanUseCharmOfCourageItem(false);
						setCanUseCharmOfCourageRes(false);
					}
				}
			}
			rset.close();
			statement.close();
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 1);
			rset = statement.executeQuery();

			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");
				
				long remainingTime = systime - System.currentTimeMillis();
				
				if (remainingTime < 10)
					continue;

				disableSkill(skillId, remainingTime);
				addTimeStamp(new TimeStamp(skillId, reuseDelay, systime));
			}
			rset.close();
			statement.close();

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Could not restore active effect data: ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.<BR><BR>
	 */
	private void restoreHenna(Connection con)
	{

		try
		{
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			for (int i = 0; i < 3; i++)
				_henna[i] = null;

			while (rset.next())
			{
				int slot = rset.getInt("slot");

				if (slot < 1 || slot > 3)
					continue;

				_henna[slot - 1] = HennaTable.getInstance().getTemplate(rset.getInt("symbol_id"));

			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Failed restoing character hennas.", e);
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}

	/**
	 * Return the number of Henna empty slot of the L2PcInstance.<BR><BR>
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
				totalSlots--;
		}

		if (totalSlots <= 0)
			return 0;

		return totalSlots;
	}

	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 */
	public boolean removeHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return false;

		slot--;

		if (_henna[slot] == null)
			return false;

		L2Henna henna = _henna[slot];
		_henna[slot] = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("Failed removing character henna.", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();

		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));

		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));

		// Add the recovered dyes to the player's inventory and notify them.
		L2ItemInstance dye = getInventory().addItem("Henna", henna.getItemId(), henna.getAmount() / 2, this, null);
		getInventory().updateInventory(dye);

		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(henna.getItemId());
		sm.addNumber(henna.getAmount() / 2);
		sendPacket(sm);

		return true;
	}

	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 */
	public boolean addHenna(L2Henna henna)
	{
		if (getHennaEmptySlots() <= 0)
		{
			sendPacket(SystemMessageId.SYMBOLS_FULL);
			return false;
		}

		boolean allow = false;
		for (L2Henna tmp : HennaTreeTable.getInstance().getAvailableHenna(this))
		{
			if (tmp == henna)
			{
				allow = true;
				break;
			}
		}

		if (!allow)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_WRONG_CLASS));
			return false;
		}

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;

				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();

				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection(con);
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.error("Failed saving character henna.", e);
				}
				finally
				{
					try
					{
						if (con != null)
							con.close();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
				}

				// Send Server->Client HennaInfo packet to this L2PcInstance
				HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);

				// Send Server->Client UserInfo packet to this L2PcInstance
				UserInfo ui = new UserInfo(this);
				sendPacket(ui);

				return true;
			}
		}

		return false;
	}

	/**
	 * Calculate Henna modifiers of this L2PcInstance.<BR><BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
				continue;
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}

		if (_hennaINT > 5)
			_hennaINT = 5;
		if (_hennaSTR > 5)
			_hennaSTR = 5;
		if (_hennaMEN > 5)
			_hennaMEN = 5;
		if (_hennaCON > 5)
			_hennaCON = 5;
		if (_hennaWIT > 5)
			_hennaWIT = 5;
		if (_hennaDEX > 5)
			_hennaDEX = 5;
	}

	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.<BR><BR>
	 */
	public L2Henna getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return null;

		return _henna[slot - 1];
	}

	/**
	 * Return the INT Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	/**
	 * Return the STR Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	/**
	 * Return the CON Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	/**
	 * Return the MEN Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	/**
	 * Return the WIT Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	/**
	 * Return the DEX Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	public void setSummonning(boolean par)
	{
		_isSummoning = par;
	}

	public boolean isSummoning()
	{
		return _isSummoning;
	}

	@Override
	public void setIsCastingNow(boolean value)
	{
		if (!value)
		{
			_isSummoning = false;
			_currentSkill = null;
		}
		super.setIsCastingNow(value);
	}

	/**
	 * Метод определения автоматической атаки
	 * Оптимизирован 10.03.2010
	 **/
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker ==null || attacker == this || attacker == getPet())
			return false;
		if (attacker instanceof L2MonsterInstance)
			return true;
		if (getParty() != null && getParty().getPartyMembers().contains(attacker))
			return false;
		if (isCursedWeaponEquipped())
			return true;
		if(attacker.getGameEvent()!=null && attacker.getGameEvent().canAttack(attacker, this))
			return true;
		if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isInOlympiadMode())
			return isInOlympiadMode() && isOlympiadStart() && ((L2PcInstance) attacker).getOlympiadGameId() == getOlympiadGameId();
		if (getClan() != null && attacker != null && getClan().isMember(attacker.getObjectId()))
			return false;
		if (attacker instanceof L2PlayableInstance && isInsideZone(L2Zone.FLAG_PEACE))
			return false;
		if (getKarma() > 0 || getPvpFlag() > 0)
			return true;

		if (attacker instanceof L2PcInstance || attacker instanceof L2Summon)
		{
		
			L2PcInstance attackTarget = attacker.getActingPlayer();

			if (attackTarget == null)
				return false;

			if (getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == attackTarget.getDuelId())
				return true;
			if (isInsideZone(L2Zone.FLAG_PVP) && attacker.isInsideZone(L2Zone.FLAG_PVP))
				return true;
			if (attackTarget.isCursedWeaponEquipped())
				return true;

			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					if (siege.checkIsDefender(attackTarget.getClan()) && siege.checkIsDefender(getClan()))
						return false;
					if (siege.checkIsAttacker(attackTarget.getClan()) && siege.checkIsAttacker(getClan()))
						return false;
				}
				if (getClan() != null && attackTarget.getClan() != null && (getClan().isAtWarWith(attackTarget.getClanId()) && attackTarget.getClan().isAtWarWith(getClanId()) && getWantsPeace() == 0 && attackTarget.getWantsPeace() == 0 && !isAcademyMember()))
					return true;
			}
			if(attacker.getGameEvent()!=null && attacker.getGameEvent().canAttack(attacker, this))
				return true;
		}
		else if (attacker instanceof L2SiegeGuardInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}
		else if (attacker instanceof L2FortSiegeGuardInstance)
		{
			if (getClan() != null)
			{
				FortSiege siege = FortSiegeManager.getInstance().getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}
		return false;
	}

	/**
	 * Метод использования скила
	 * Оптимизирован 10.03.2010
	 **/
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (skill == null)
			return;

		if (skill.isPassive() || skill.isChance() || skill.bestowed())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (isCastingNow())
		{
			SkillDat currentSkill = getCurrentSkill();
			if (currentSkill != null && skill.getId() == currentSkill.getSkillId())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			setQueuedSkill(skill, forceUse, dontMove);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		setIsCastingNow(true);
		setCurrentSkill(skill, forceUse, dontMove);

		if (getQueuedSkill() != null)
			setQueuedSkill(null, false, false);

		if (!checkUseMagicConditions(skill, forceUse, dontMove))
		{
			setIsCastingNow(false);
			return;
		}

		L2Character target = null;
		switch (skill.getTargetType())
		{
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_SELF:
				target = this;
				break;
			default:
				target = skill.getFirstOfTargetList(this);
				break;
		}
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Метод проверки гтовности к использования магии
	 * Оптимизирован 10.03.2010
	 **/
	private boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (isOutOfControl())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (isDead())
		{
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (inObserverMode())
		{
			sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (isSitting() && !skill.isPotion())
		{
			sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (skill.isToggle())
		{
			L2Effect effect = getFirstEffect(skill);

			if (effect != null)
			{
				effect.exit();
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		if (isFakeDeath())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		L2SkillType sklType = skill.getSkillType();
		if (isFishing() && (sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
		{
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			return false;
		}

		switch (skill.getId())
		{
			case 13:
			case 299:
			case 448:
				if ((!SiegeManager.getInstance().checkIfOkToSummon(this, false) && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false)) || SevenSigns.getInstance().checkSummonConditions(this))
					return false;
		}

		L2Object target = null;
		SkillTargetType sklTargetType = skill.getTargetType();
		Point3D worldPosition = getCurrentSkillWorldPosition();

		if (sklTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		switch (sklTargetType)
		{
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_CORPSE_CLAN:
			case TARGET_GROUND:
			case TARGET_SELF:
				target = this;
				break;
			case TARGET_PET:
			case TARGET_SUMMON:
				target = getPet();
				break;
			default:
				target = getTarget();
				break;
		}

		if (target == null)
		{
			sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (target instanceof L2DoorInstance)
		{
			L2DoorInstance door = (L2DoorInstance) target;
			boolean isCastleDoor = (door.getCastle() != null && door.getCastle().getSiege().getIsInProgress());
			boolean isFortDoor = (door.getFort() != null && door.getFort().getSiege().getIsInProgress() && !door.getIsCommanderDoor());
			if (!isCastleDoor && !isFortDoor && !(door.isUnlockable() && (skill.getSkillType() == L2SkillType.UNLOCK )))
				return false;
		}

		if (isInDuel())
		{
			if (!((target instanceof L2PcInstance && ((L2PcInstance) target).getDuelId() == getDuelId()) ||
					(target instanceof L2Summon && ((L2Summon) target).getOwner().getDuelId() == getDuelId())))
			{
				sendPacket(SystemMessageId.INCORRECT_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}


		if (isSkillDisabled(skill.getId()))
		{
			SystemMessage sm = null;
			TimeStamp ts = (_reuseTimeStamps == null) ? null : _reuseTimeStamps.get(skill.getId()); 
			if (ts != null)
			{
/*				int remainingTime = (int)(ts.getRemaining() / 1000);
				int hours = remainingTime / 3600;
				int minutes = (remainingTime % 3600) / 60;
				int seconds = (remainingTime % 60); */
				sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
			}
			sendPacket(sm);
			return false;
		}


		if (_charges < skill.getNeededCharges())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
			sendPacket(sm);
			return false;
		}

		if (skill.getGiveCharges() > 0 && _charges >= skill.getMaxCharges() && !skill.getContinueAfterMax())
		{
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (!skill.checkCondition(this, target))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		if (isFishing() && (sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
		{
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			return false;
		}

		if (skill.isOffensive())
		{
			if (isInsidePeaceZone(this, target) && !allowPeaceAttack() ) {
				if(!skill.useAlways())
				{
					if (!isInFunEvent() || !target.isInFunEvent() )
					{
						sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				} else forceUse = false;
			}

			if (isInOlympiadMode() && !isOlympiadStart() && sklTargetType != SkillTargetType.TARGET_AURA)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			if (!target.isAttackable() && !allowPeaceAttack())
			{
				if (!isInFunEvent() || !target.isInFunEvent() && target!=this)
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}

			if (!target.isAutoAttackable(this) && !forceUse && !isInFunEvent() && !isInDuel())
			{

				switch (sklTargetType)
				{
					case TARGET_AURA:
					case TARGET_FRONT_AURA:
					case TARGET_BEHIND_AURA:
					case TARGET_CLAN:
					case TARGET_ALLY:
					case TARGET_PARTY:
					case TARGET_SELF:
					case TARGET_GROUND:
						break;
					default:
/*						L2PcInstance targetPlayer = target.getActingPlayer();
						if(targetPlayer!=null) {
							break;
//							|| targetPlayer.getOlympiadGameId()!= getOlympiadGameId()) {
						} */
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
						
				}
			}

			if (dontMove)
			{
				if (sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
					{
						sendPacket(SystemMessageId.TARGET_TOO_FAR);
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
				{
					sendPacket(SystemMessageId.TARGET_TOO_FAR);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		if (skill.isPvpSkill() && target instanceof L2PlayableInstance &&
			sklTargetType == SkillTargetType.TARGET_ONE && !isInOlympiadMode())
		{
			boolean srcInPvP = isInsideZone(L2Zone.FLAG_PVP) && !isInsideZone(L2Zone.FLAG_SIEGE);
			boolean targetInPvP = ((L2PlayableInstance)target).isInsideZone(L2Zone.FLAG_PVP) && !((L2PlayableInstance)target).isInsideZone(L2Zone.FLAG_SIEGE);
			boolean stop = false;
			if (target instanceof L2PcInstance)
			{
				if ((getParty() != null && ((L2PcInstance) target).getParty() != null) && getParty().getPartyLeaderOID() == ((L2PcInstance) target).getParty().getPartyLeaderOID())
					stop = true;
				if (!srcInPvP && !targetInPvP)
				{
					if (getClanId() != 0 && getClanId() == ((L2PcInstance) target).getClanId())
						stop = true;
					if (getAllyId() != 0 && getAllyId() == ((L2PcInstance) target).getAllyId())
						stop = true;
				}
				if ((isInFunEvent() && target.isInFunEvent()) || (isInDuel() && ((L2PcInstance) target).getDuelId() == getDuelId()))
					stop = false;
			}
			else if (target instanceof L2Summon)
			{
				L2PcInstance trg = ((L2Summon) target).getOwner();
				if (trg == this)
					stop = true;
				if ((getParty() != null && trg.getParty() != null) && getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
					stop = true;
				if (!srcInPvP && !targetInPvP)
				{
					if (getClanId() != 0 && getClanId() == trg.getClanId())
						stop = true;
					if (getAllyId() != 0 && getAllyId() == trg.getAllyId())
						stop = true;
				}
				if ((isInFunEvent() && trg.isInFunEvent()) || (isInDuel() && ((L2PcInstance) trg).getDuelId() == getDuelId()))
					stop = false;
			}
			if (stop)
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		if (skill.getSkillType() == L2SkillType.INSTANT_JUMP)
		{
			if (isRooted())
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill.getId());
				sendPacket(sm);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			if (isInsideZone(L2Zone.FLAG_PEACE))
			{
				sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		if (!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse && !skill.isNeutral())
		{
			switch (sklTargetType)
			{
				case TARGET_PET:
				case TARGET_SUMMON:
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
				case TARGET_CLAN:
				case TARGET_SELF:
				case TARGET_PARTY:
				case TARGET_ALLY:
				case TARGET_CORPSE_MOB:
				case TARGET_AREA_CORPSE_MOB:
				case TARGET_GROUND:
					break;
				default:
					switch (sklType)
					{
					case BEAST_FEED:
					case DELUXE_KEY_UNLOCK:
					case UNLOCK:
					case GARDEN_KEY_UNLOCK:
					case MAKE_KILLABLE:
						break;
					default:
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
			}
		}

		if (sklType == L2SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance) && !(target instanceof L2ChestInstance))
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		if (sklType == L2SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getIsSpoiledBy();

			if (((L2Attackable) target).isDead())
			{
				if (!((L2Attackable) target).isSpoil())
				{
					sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}

				if (getObjectId() != spoilerId && !isInLooterParty(spoilerId))
				{
					sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		if (sklType == L2SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		switch (sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill) && !allowPeaceAttack())
				{
					if (!isInFunEvent() || !target.isInFunEvent())
					{
						sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
		}

		if ((sklTargetType == SkillTargetType.TARGET_HOLY && (!TakeCastle.checkIfOkToCastSealOfRule(this, false))) || (sklTargetType == SkillTargetType.TARGET_FLAGPOLE && !TakeFort.checkIfOkToCastFlagDisplay(this, false, skill, getTarget())) || (sklType == L2SkillType.SIEGEFLAG && (!SiegeManager.checkIfOkToPlaceFlag(this, false) && !FortSiegeManager.checkIfOkToPlaceFlag(this, false))) || (sklType == L2SkillType.STRSIEGEASSAULT && (!SiegeManager.checkIfOkToUseStriderSiegeAssault(this, false) && !FortSiegeManager.checkIfOkToUseStriderSiegeAssault(this, false))))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}

		if (skill.getCastRange() > 0)
		{
			if (sklTargetType == SkillTargetType.TARGET_GROUND)
			{
				if (!GeoData.getInstance().canSeeTarget(this, worldPosition))
				{
					sendPacket(SystemMessageId.CANT_SEE_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANT_SEE_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		return true;
	}

	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = (L2PcInstance) L2World.getInstance().findObject(LooterId);

		// if L2PcInstance is in a CommandChannel
		if (isInParty() && getParty().isInCommandChannel() && looter != null)
			return getParty().getCommandChannel().getMembers().contains(looter);

		if (isInParty() && looter != null)
			return getParty().getPartyMembers().contains(looter);

		return false;
	}

	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return false if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object obj, L2Skill skill)
	{
		return checkPvpSkill(obj,skill,false);
	}

	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param obj L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @param srcIsSummon is L2Summon - caster?
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object obj, L2Skill skill,boolean srcIsSummon)
	{
		// check for PC->PC Pvp status
		if (obj != this && // target is not self and
				obj instanceof L2PcInstance && // target is L2PcInstance and
				!(isInDuel() && ((L2PcInstance) obj).getDuelId() == getDuelId()) && // self is not in a duel and attacking opponent
				!isInsideZone(L2Zone.FLAG_PVP) && // Pc is not in PvP zone
				!((L2PcInstance) obj).isInsideZone(L2Zone.FLAG_PVP) // target is not in PvP zone
		)
		{
			L2PcInstance target = (L2PcInstance) obj;
			if (skill.isPvpSkill()) // pvp skill
			{
				if (getClan() != null && target.getClan() != null)
				{
					if (getClan().isAtWarWith(target.getClan().getClanId())
							&& target.getClan().isAtWarWith(getClan().getClanId()))
						return true; // in clan war player can attack whites even with sleep etc.
				}
				if (target.getPvpFlag() == 0 && //   target's pvp flag is not set and
						target.getKarma() == 0 //   target has no karma
				)
					return false;
			}
			else if ((getCurrentSkill() != null && !getCurrentSkill().isCtrlPressed() && skill.isOffensive() && !srcIsSummon)
					|| (getCurrentPetSkill() != null && !getCurrentPetSkill().isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			{
				if (getClan() != null && target.getClan() != null)
				{
					if(getClan().isAtWarWith(target.getClan().getClanId()) && target.getClan().isAtWarWith(getClan().getClanId()))
						return true; // in clan war player can attack whites even without ctrl
				}
				if (target.getPvpFlag() == 0 && //   target's pvp flag is not set and
						target.getKarma() == 0 //   target has no karma
				)
					return false;
			}
		}
		return true;
	}
	/**
	 * Return true if the L2PcInstance is a Mage.<BR><BR>
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}

	public boolean isMounted()
	{
		return _mountType > 0;
	}

	public boolean checkCanLand()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(L2Zone.FLAG_NOLANDING))
			return false;

		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		return !(SiegeManager.getInstance().checkIfInZone(this)
				&& !(getClan() != null && CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan()) && this == getClan().getLeader().getPlayerInstance()));
	}

	/**
	 * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send a Server->Client packet InventoryUpdate to the L2PcInstance.<BR><BR>
	 * @return false if the change of mount type false
	 */
	public boolean setMount(int npcId, int npcLevel, int mountType)
	{
		switch (mountType)
		{
		case 0:
			setIsRidingStrider(false);
			setIsRidingRedStrider(false);
			setIsFlying(false);
			isFalling(false, 0); // Initialize the fall just incase dismount was made while in-air
			break; //Dismounted
		case 1:
			if (npcId >= 12526 && npcId <= 12528)
				setIsRidingStrider(true);
			else if (npcId >= 16038 && npcId <= 16040)
				setIsRidingRedStrider(true);
			if (isNoble())
			{
				L2Skill striderAssaultSkill = SkillTable.getInstance().getInfo(325, 1);
				addSkill(striderAssaultSkill); // not saved to DB
			}
			break;
		case 2:
			setIsFlying(true);
			break; //Flying Wyvern
		}

		_mountType = mountType;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;

		return true;
	}

	/**
	 * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).
	 */
	public int getMountType()
	{
		return _mountType;
	}

	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.<BR><BR>
	 */
	public void tempInvetoryDisable()
	{
		_inventoryDisable = true;

		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}

	/**
	 * Return true if the Inventory is disabled.<BR><BR>
	 */
	public boolean isInvetoryDisabled()
	{
		return _inventoryDisable;
	}

	class InventoryEnable implements Runnable
	{
		public void run()
		{
			_inventoryDisable = false;
		}
	}

	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}

	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.<BR><BR>
	 */
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int totalLifeTime)
	{
		L2CubicInstance cubic = new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, totalLifeTime);

		_cubics.put(id, cubic);
	}

	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.<BR><BR>
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}

	/**
	 * Return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.<BR><BR>
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}

	@Override
	public String toString()
	{
		return "player " + getName();
	}

	/**
	 * Return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).<BR><BR>
	 */
	public int getEnchantEffect(boolean self)
	{
		L2ItemInstance wpn = getActiveWeaponInstance();

		if (wpn == null)
			return 0;

		if(self && Config.ENCHANT_LIMIT_AURA_SELF)
			return Math.min(Config.ENCHANT_LIMIT_AURA_LEVEL, wpn.getEnchantLevel());
		if(!self && Config.ENCHANT_LIMIT_AURA_OTHER)
			return Math.min(Config.ENCHANT_LIMIT_AURA_LEVEL, wpn.getEnchantLevel());
		return Math.min(127, wpn.getEnchantLevel());
	}

	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR><BR>
	 */
	public void setLastFolkNPC(L2FolkInstance folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}

	/**
	 * Return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR><BR>
	 */
	public L2FolkInstance getLastFolkNPC()
	{
		return _lastFolkNpc;
	}

	/**
	 * Return true if L2PcInstance is a participant in the Festival of Darkness.<BR><BR>
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}

	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.put(itemId, itemId);
	}

	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}

	public Map<Integer, Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}

	public void rechargeAutoSoulShot(boolean physical, boolean magic, boolean summon,boolean animation)
	{
		L2ItemInstance item;
		IItemHandler handler;

		if (_activeSoulShots == null || _activeSoulShots.size() == 0)
			return;

		Collection<Integer> vals = _activeSoulShots.values();

		synchronized (_activeSoulShots)
		{
			for (int itemId : vals)
			{
				item = getInventory().getItemByItemId(itemId);

				if (item != null)
				{
					if (magic)
					{
						if (!summon)
						{
							if ((itemId >= 2509 && itemId <= 2514) || (itemId >= 3947 && itemId <= 3952) || itemId == 5790)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								if (handler != null)
									handler.useItem(this, item, animation);
							}
						}
						else
						{
							if (itemId == 6646 || itemId == 6647)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								if (handler != null)
									handler.useItem(this, item, animation);
							}
						}
					}

					if (physical)
					{
						if (!summon)
						{
							if ((itemId >= 1463 && itemId <= 1467) || itemId == 1835 || itemId == 5789)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								if (handler != null)
									handler.useItem(this, item, animation);
							}
						}
						else
						{
							if (itemId == 6645)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								if (handler != null)
									handler.useItem(this, item, animation);
							}
						}
					}
				}
				else
					removeAutoSoulShot(itemId);
			}
		}
	}
	private ScheduledFuture<?>	_taskWarnUserTakeBreak;

	class WarnUserTakeBreak implements Runnable
	{
		public void run()
		{
			sendPacket(SystemMessageId.PLAYING_FOR_LONG_TIME);
		}
	}

	class RentPetTask implements Runnable
	{
		public void run()
		{
			stopRentPet();
		}
	}

	public ScheduledFuture<?>	_taskforfish;

	class WaterTask implements Runnable
	{
		public void run()
		{
			double reduceHp = getMaxHp() / 100.0;

			if (reduceHp < 1)
				reduceHp = 1;

			reduceCurrentHp(reduceHp, L2PcInstance.this, false, false, null);
			// Reduced hp, because not rest
			SystemMessage sm = new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int) reduceHp);
			sendPacket(sm);

		}
	}

	class LookingForFishTask implements Runnable
	{
		boolean	_isNoob, _isUpperGrade;
		int		_fishType, _fishGutsCheck, _gutsCheckTime;
		long	_endTaskTime;

		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}

		public void run()
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				endFishing(false);
				return;
			}

			if (!GameTimeController.getInstance().isNowNight() && _lure.isNightLure())
				return;

			int check = Rnd.get(1000);
			if (_fishGutsCheck > check)
			{
				stopLookingForFishTask();
				startFishCombat(_isNoob, _isUpperGrade);
			}
		}
	}

	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}

	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	
	public void enterObserverMode(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();
		
		setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
		setIsInvul(true);
		getAppearance().setInvisible();
		sendPacket(new ObservationMode(x, y, z));
		teleToLocation(x, y, z, false);

		_observerMode = true;
		setObserveMode(1);
		updateInvisibilityStatus();
	}
	private void setObserveMode(int val) {

		_observMode = val;
		if(_observMode!=0 && _decoy == null) {
			_decoy = new L2Decoy(this);
			_decoy.setTitle("Смотрю");
			_decoy.sitDown();
			_decoy.spawnMe(_obsX, _obsY, _obsZ);
		} 
		else if(_decoy!=null) {
			_decoy.deleteMe(this);
			_decoy = null;
		}
	}

	public void enterOlympiadObserverMode(int x, int y, int z, int id, boolean storeCoords)
	{
		if (getPet() != null)
			getPet().unSummon(this);
		if (!getCubics().isEmpty())
		{
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			getCubics().clear();
		}
		if (getParty() != null)
			getParty().removePartyMember(this);

		_olympiadGameId = id;
		if (isSitting())
			standUp();
		if (storeCoords)
		{
			_obsX = getX();
			_obsY = getY();
			_obsZ = getZ();
		}
		setTarget(null);
		setIsInvul(true);
		getAppearance().setInvisible();
		teleToLocation(x, y, z, false);
		sendPacket(new ExOlympiadMode(3));
		_observerMode = true;
		setObserveMode(2);
		updateInvisibilityStatus();
	}

	// [L2J_JP ADD SANDMAN]
	public void enterMovieMode()
	{
		setTarget(null);
		stopMove(null);
		setIsInvul(true);
		setIsImmobilized(true);
		sendPacket(new CameraMode(1));
	}

	public void leaveMovieMode()
	{
		if (!isGM())
			setIsInvul(false);
		setIsImmobilized(false);
		sendPacket(new CameraMode(0));
	}

	/**
	 * yaw:North=90, south=270, east=0, west=180<BR>
	 * pitch > 0:looks up,pitch < 0:looks down<BR>
	 * time:faster that small value is.<BR>
	 */
	public void specialCamera(L2Object target, int dist, int yaw, int pitch, int time, int duration)
	{
		sendPacket(new SpecialCamera(target.getObjectId(), dist, yaw, pitch, time, duration));
	}
	// L2JJP END

	@Override
	public void onSpawn() {
		getKnownList(); // init knownlist
		super.onSpawn();
	}
	
	public void leaveObserverMode()
	{
		setTarget(null);
		getPosition().setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);

		if (!isGM())
		{
			getAppearance().setVisible();
			setIsInvul(false);
		}
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		teleToLocation(_obsX, _obsY, _obsZ);
		sendPacket(new ObservationReturn(this));
		_observerMode = false;
		setObserveMode(0);
		broadcastUserInfo(true);
	}

	public void leaveOlympiadObserverMode()
	{
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_obsX, _obsY, _obsZ);
		if (!isGM())
		{
			getAppearance().setVisible();
			setIsInvul(false);
		}
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		Olympiad.removeSpectator(_olympiadGameId, this);
		_olympiadGameId = -1;
		_observerMode = false;
		setObserveMode(0);
		broadcastUserInfo(true);
	}

	public void updateNameTitleColor()
	{
		broadcastUserInfo(true);
	}

	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}

	public int getOlympiadSide()
	{
		return _olympiadSide;
	}

	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}

	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}

	public int getOlyDamage()
	{
		return _olyDamage;
	}

	public void setOlyDamage(int dmg)
	{
		_olyDamage = dmg;
	}

	public void addOlyDamage(int dmg)
	{
		_olyDamage = _olyDamage + dmg;
	}

	public void reduceOlyDamage(int dmg)
	{
		if (_olyDamage - dmg < 0)
			_olyDamage = 0;
		else
			_olyDamage = _olyDamage - dmg;
	}

	public int getObsX()
	{
		return _obsX;
	}

	public int getObsY()
	{
		return _obsY;
	}

	public int getObsZ()
	{
		return _obsZ;
	}

	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public int getObservMode()
	{
		return _observMode;
	}
	
	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}

	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}

	public int getLoto(int i)
	{
		return _loto[i];
	}

	public void setRace(int i, int val)
	{
		_race[i] = val;
	}

	public int getRace(int i)
	{
		return _race[i];
	}


	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}

	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendEtcStatusUpdate();
	}

	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}

	public boolean getDietMode()
	{
		return _dietMode;
	}

	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}

	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}

	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public BlockList getBlockList()
	{
		if (_blockList == null)
			_blockList = new BlockList(this);
		return _blockList;
	}

	public L2FriendList getFriendList()
	{
		if (_friendList == null)
			_friendList = new L2FriendList(this);
		return _friendList;
	}

	public void setHero(boolean hero)
	{
		if (hero && _baseClass == _activeClass)
		{
			for (L2Skill s : HeroSkillTable.getInstance().getHeroSkills())
				addSkill(s, false); //Dont Save Hero skills to Sql
		}
		else
		{
			for (L2Skill s : HeroSkillTable.getInstance().getHeroSkills())
				super.removeSkill(s); //Just Remove skills without deleting from Sql
		}
		_hero = hero;
		sendSkillList();
	}


	public void setIsInOlympiadMode(boolean b)
	{
		_inOlympiadMode = b;
	}

	public void setIsOlympiadStart(boolean b)
	{
		_olympiadStart = b;
		// clear olympiad damage incase its not the first match since init of l2pcisntance
		if (b)
			setOlyDamage(0);
	}

	public boolean isOlympiadStart()
	{
		return _olympiadStart;
	}

	public boolean isHero()
	{
		return _hero;
	}

	

	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}

	public void setNoble(boolean val)
	{
		if (val)
		{
			for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
				addSkill(s, false); //Dont Save Noble skills to Sql
		}
		else
		{
			for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
				super.removeSkill(s); //Just Remove skills without deleting from Sql
		}
		_noble = val;
		sendSkillList();
		intemediateStore();
	}

	public boolean isInDuel()
	{
		return _isInDuel;
	}

	public int getDuelId()
	{
		return _duelId;
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}

	/**
	 * This returns a SystemMessage stating why
	 * the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		// This is somewhat hacky - but that case should never happen anyway...
		if (_noDuelReason == 0)
			_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL.getId();

		SystemMessage sm = new SystemMessage(SystemMessageId.getSystemMessageId(_noDuelReason));
		sm.addPcName(this);
		_noDuelReason = 0;
		return sm;
	}

	/**
	 * Checks if this player might join / start a duel.
	 * To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isInJail())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE.getId();
			return false;
		}
		if (isDead() || isAlikeDead() || (getStatus().getCurrentHp() < getStat().getMaxHp() / 2 || getStatus().getCurrentMp() < getStat().getMaxMp() / 2))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT.getId();
			return false;
		}
		if (isInDuel())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL.getId();
			return false;
		}
		if (isInOlympiadMode() || Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD.getId();
			return false;
		}
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE.getId();
			return false;
		}
		if (getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE.getId();
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER.getId();
			return false;
		}
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING.getId();
			return false;
		}
		if (isInsideZone(L2Zone.FLAG_PVP) || isInsideZone(L2Zone.FLAG_PEACE) || SiegeManager.getInstance().checkIfInZone(this))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA.getId();
			return false;
		}
		return true;
	}

	public boolean isNoble()
	{
		return _noble;
	}

	public int getSubLevel()
	{
		if (isSubClassActive())
		{
			int lvl = getLevel();
			return lvl;
		}
		return 0;
	}

	// Baron, Wise Man etc, calculated on EnterWorld and when rank is changing
	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
	}

	public int getPledgeClass()
	{
		return _pledgeClass;
	}

	public void setSubPledgeType(int typeId)
	{
		_subPledgeType = typeId;
	}

	public int getSubPledgeType()
	{
		return _subPledgeType;
	}

	public int getPledgeRank()
	{
		return _pledgeRank;
	}

	public void setPledgeRank(int rank)
	{
		_pledgeRank = rank;
	}

	public int getApprentice()
	{
		return _apprentice;
	}

	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}

	public int getSponsor()
	{
		return _sponsor;
	}

	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}

	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}

	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}

	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}

	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}

	public int getWantsPeace()
	{
		return _wantsPeace;
	}

	public boolean isFishing()
	{
		return _fishing;
	}

	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}

	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		// [-5,-1] varka, 0 neutral, [1,5] ketra
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}

	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}

	public boolean isAlliedWithVarka()
	{
		return (_alliedVarkaKetra < 0);
	}

	public boolean isAlliedWithKetra()
	{
		return (_alliedVarkaKetra > 0);
	}

	public void sendSkillList()
	{
		L2Skill[] array = getAllSkills();
		List<L2Skill> skills = new ArrayList<L2Skill>(array.length);

		for (L2Skill s : array)
		{
			if (s == null)
				continue;

			if (s.getId() > 9000 && s.getId() < 9007)
				continue; // Fake skills to change base stats

			if (s.bestowed())
				continue;

			if (s.getSkillType() == L2SkillType.NOTDONE)
			{
				switch (Config.SEND_NOTDONE_SKILLS)
				{
				case 1:
					continue;
				case 2:
					if (!isGM())
						continue;
				}
			}

			skills.add(s);
		}

		sendPacket(new SkillList(skills));
	}

	/** Section for mounted pets */
	class FeedTask implements Runnable
	{
		public void run()
		{
			try
			{
				if (!isMounted())
				{
					stopFeed();
					return;
				}

				if (getCurrentFeed() > getFeedConsume())
					setCurrentFeed(getCurrentFeed() - getFeedConsume());
				else
				{
					// go back to pet control item, or simply said, unsummon it
					setCurrentFeed(0);
					stopFeed();
					dismount();
					sendPacket(new SystemMessage(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED));
				}

				int[] foodIds = PetDataTable.getFoodItemId(getMountNpcId());
				if (foodIds[0] == 0)
					return;
				L2ItemInstance food = null;
				food = getInventory().getItemByItemId(foodIds[0]);

				// use better strider food if exists
				if (PetDataTable.isStrider(getMountNpcId()))
				{
					if (getInventory().getItemByItemId(foodIds[1]) != null)
						food = getInventory().getItemByItemId(foodIds[1]);
				}
				if (food != null && isHungry())
				{
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getItemId());
					if (handler != null)
					{
						handler.useItem(L2PcInstance.this, food);
						SystemMessage sm = new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
						sm.addItemName(food.getItemId());
						sendPacket(sm);
					}
				}
			}
			catch (Exception e)
			{
				_log.fatal("Mounted Pet [NpcId: "+getMountNpcId()+"] a feed task error has occurred", e);
			}
		}
	}

	protected synchronized void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
			return;

                if (Config.PET_FOOD){
                    if (getPet() != null)
                    {
                            setCurrentFeed(getPet().getCurrentFed());
                            _controlItemId = getPet().getControlItemId();
                            sendPacket(new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
                            if (!isDead())
                                    _mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
                    }
                    else if (_canFeed)
                    {
                            setCurrentFeed(getMaxFeed());
                            SetupGauge sg = new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume());
                            sendPacket(sg);
                            if (!isDead())
                                    _mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
                    }
                }
	}

	protected synchronized void stopFeed()
	{
		if (_mountFeedTask != null)
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
	}

	protected final void clearPetData()
	{
		_data = null;
	}

	protected final L2PetData getPetData(int npcId)
	{
		if (_data == null && getPet() != null)
			_data = PetDataTable.getInstance().getPetData(getPet().getNpcId(), getPet().getLevel());
		else if (_data == null && npcId > 0)
			_data = PetDataTable.getInstance().getPetData(npcId, getLevel());

		return _data;
	}

	public int getCurrentFeed()
	{
		return _curFeed;
	}

	protected int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
			return getPetData(_mountNpcId).getPetFeedBattle();
		else
			return getPetData(_mountNpcId).getPetFeedNormal();
	}

	public void setCurrentFeed(int num)
	{
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		SetupGauge sg = new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume());
		sendPacket(sg);
	}

	protected int getMaxFeed()
	{
		return getPetData(_mountNpcId).getPetMaxFeed();
	}

	protected boolean isHungry()
	{
            boolean need = Config.PET_FOOD;
            
                if (need){
                    return _canFeed ? (getCurrentFeed() < (0.55 * getPetData(getMountNpcId()).getPetMaxFeed())) : false;
                }else{
                    return false;                  
                }
	}

	public class dismount implements Runnable
	{
		public void run()
		{
			try
			{
				dismount();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void enteredNoLanding()
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new L2PcInstance.dismount(), 5000);
	}

	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(false);
			_dismountTask = null;
		}
	}

	public void storePetFood(int petId)
	{
		if (_controlItemId != 0 && petId != 0)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?");
				statement.setInt(1, getCurrentFeed());
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				statement.close();
				_controlItemId = 0;
			}
			catch (Exception e)
			{
				_log.fatal("Failed to store Pet [NpcId: "+petId+"] data", e);
			}
			finally
			{
				try
				{
					if (con != null)
						con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>)
	 * for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only
	 * done by the calling of setActiveClass() method as that should be the only way to do so.
	 *
	 * @param int classId
	 * @param int classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (getTotalSubClasses() == Config.MAX_SUBCLASS || classIndex == 0)
			return false;

		if (getSubClasses().containsKey(classIndex))
			return false;

		// Note: Never change _classIndex in any method other than setActiveClass().

		store(true);
		SubClass newClass = new SubClass();
		newClass.setClassId(classId);
		newClass.setClassIndex(classIndex);

		Connection con = null;

		try
		{
			// Store the basic info about this new sub-class.
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, newClass.getClassId());
			statement.setLong(3, newClass.getExp());
			statement.setInt(4, newClass.getSp());
			statement.setInt(5, newClass.getLevel());
			statement.setInt(6, newClass.getClassIndex()); // <-- Added
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("WARNING: Could not add character sub class for " + getName() + ": " + e);
			return false;
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}

		// Commit after database INSERT incase exception is thrown.
		getSubClasses().put(newClass.getClassIndex(), newClass);

		ClassId subTemplate = ClassId.values()[classId];
		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);

		if (skillTree == null)
			return true;

		Map<Integer, L2Skill> prevSkillList = new HashMap<Integer, L2Skill>();

		for (L2SkillLearn skillInfo : skillTree)
		{
			if (skillInfo.getMinLevel() <= 40)
			{
				L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
				L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());

				if (prevSkill != null && (prevSkill.getLevel() >= newSkill.getLevel()))
					continue;

				if(newSkill!=null) {
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				} else 
					_log.info("L2PcInstance: Skill "+skillInfo.getId()+" not found for character "+getName()+" ("+getClassId()+")");
			}
		}
		getStat().resetModifiers();
		return true;
	}

	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
	 * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
	 *
	 * @param int classIndex
	 * @param int newClassId
	 * @return boolean subclassAdded
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;

			// Remove all henna info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			// Remove all shortcuts info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			// Remove all effects info stored for this sub-class.
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			// Remove all skill info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SKILLS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			// Remove all basic info stored about this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);

			// This must be done in order to maintain data consistency.
			getSubClasses().remove(classIndex);
			return false;
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		try {
			L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getSubClasses().get(classIndex).getClassDefinition());
			for(L2SkillLearn sk : skills)
				removeSkill(sk.getId());
			for(L2Effect e : getAllEffects())
				e.exit();
		} catch(Exception e) {
			
		}
		getSubClasses().remove(classIndex);
		
		return addSubClass(newClassId, classIndex);
	}

	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}

	public Map<Integer, SubClass> getSubClasses()
	{
		if (_subClasses == null)
			_subClasses = new FastMap<Integer, SubClass>().setShared(true);

		return _subClasses;
	}

	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}

	public int getBaseClass()
	{
		return _baseClass;
	}

	public int getActiveClass()
	{
		return _activeClass;
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	private void setClassTemplate(int classId)
	{
		_activeClass = classId;

		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

		if (t == null)
		{
			_log.fatal("Missing template for classId: " + classId);
			throw new Error();
		}

		// Set the template of the L2PcInstance
		setTemplate(t);
	}

	/**
	 * Changes the character's class based on the given class index.
	 * <BR><BR>
	 * An index of zero specifies the character's original (base) class,
	 * while indexes 1-3 specifies the character's sub-classes respectively.
	 *
	 * @param classIndex
	 */
	private int _baseLevel;
	private long _baseExp;
	private int _baseSP;
	public synchronized boolean setActiveClass(int classIndex)
	{
		// Remove active item skills before saving char to database
		// because next time when choosing this class, weared items can
		// be different
		
		for (L2ItemInstance temp : getInventory().getAugmentedItems())
		{
			if (temp != null && temp.isEquipped())
				temp.getAugmentation().removeBonus(this);
			
		}
		
		// Remove class circlets (can't equip circlets while being in subclass)
		L2ItemInstance circlet = getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIRALL);
		if (circlet != null)
		{
			if (((circlet.getItemId() >= 9397 && circlet.getItemId() <= 9408) || circlet.getItemId() == 10169) && circlet.isEquipped())
			{
				L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(circlet.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance element : unequipped)
					iu.addModifiedItem(element);
				sendPacket(iu);
			}
		}

		// Delete a force buff upon class change.
		if(_fusionSkill != null)
			abortCast();

		// Stop casting for any player that may be casting a force buff on this l2pcinstance.
		for (L2Character character : getKnownList().getKnownCharacters())
		{
			if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
				character.abortCast();
		}

		/*
		 * 1. Call store() before modifying _classIndex to avoid skill effects rollover.
		 * 2. Register the correct _classId against applied 'classIndex'.
		 */
		if(_classIndex==0) {
			_baseLevel = getLevel();
			_baseExp = getExp();
			_baseSP = getSp();
		}
		store();
		//_reuseTimeStamps.clear();
		
		if (classIndex == 0)
		{
			setClassTemplate(getBaseClass());
		}
		else
		{
			try
			{
				setClassTemplate(getSubClasses().get(classIndex).getClassId());
			}
			catch (Exception e)
			{
				_log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
				return false;
			}
		}

		_classIndex = classIndex;

		if (isInParty())
		{
			if (Config.MAX_PARTY_LEVEL_DIFFERENCE > 0)
			{
				for (L2PcInstance p : getParty().getPartyMembers())
				{
					if (Math.abs(p.getLevel() - getLevel()) > Config.MAX_PARTY_LEVEL_DIFFERENCE)
					{
						getParty().removePartyMember(this);
						sendMessage(Message.getMessage(this, Message.MessageId.MSG_REMOVE_FROM_PARTY_BIG_LVL_DIF));
						break;
					}
				}
			}
			else
				getParty().recalculatePartyLevel();
		}

		/*
		 * Update the character's change in class status.
		 *
		 * 1. Remove any active cubics from the player.
		 * 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
		 * 3. Remove all existing skills.
		 * 4. Restore all the learned skills for the current class from the database.
		 * 5. Restore effect/buff data for the new class.
		 * 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
		 * 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
		 * 8. Restore shortcut data related to this class.
		 * 9. Resend a class change animation effect to broadcast to all nearby players.
		 * 10.Unsummon any active servitor from the player.
		 */

		if (getPet() != null && getPet() instanceof L2SummonInstance)
			getPet().unSummon(this);

		if (!getCubics().isEmpty())
		{
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			getCubics().clear();
		}

		abortCast();

		for (L2Skill oldSkill : getAllSkills())
			super.removeSkill(oldSkill);

		stopAllEffectsExceptThoseThatLastThroughDeath();
	    Connection con = null;
	    try {
	    	con = L2DatabaseFactory.getInstance().getConnection();
	    	restoreDeathPenaltyBuffLevel();
	    	restoreSkills(con);
	    	regiveTemporarySkills();
	    	rewardSkills();
		// Prevents some issues when changing between subclases that shares skills  
	    //	if (_disabledSkills != null && !_disabledSkills.isEmpty())
	    //		_disabledSkills.clear();
	    	restoreEffects();
	    	updateEffectIcons();

		//if player has quest 422: Repent Your Sins, remove it
	    	QuestState st = getQuestState("422_RepentYourSins");

	    	if (st != null)
	    		st.exitQuest(true);

	    	for (int i = 0; i < 3; i++)
	    		_henna[i] = null;

	    	restoreHenna(con);
	    	sendPacket(new HennaInfo(this));

	    	if (getStatus().getCurrentHp() > getMaxHp())
	    		getStatus().setCurrentHp(getMaxHp());
	    	if (getStatus().getCurrentMp() > getMaxMp())
	    		getStatus().setCurrentMp(getMaxMp());
	    	if (getStatus().getCurrentCp() > getMaxCp())
	    		getStatus().setCurrentCp(getMaxCp());
	    	getInventory().restoreEquipedItemsPassiveSkill();
	    	getInventory().restoreArmorSetPassiveSkill();
	    	getStat().resetModifiers();
		    getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_LR_HAND);
		    getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_R_HAND);
		    getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_L_HAND);
		    sendPacket(new ItemList(this,false));
	    	broadcastUserInfo(true);
	    	refreshOverloaded();
	    	refreshExpertisePenalty();
	    	setExpBeforeDeath(0);
	    	getShortCuts().restore(con);
	    	sendPacket(new ShortCutInit(this));
	    	broadcastPacket(new SocialAction(getObjectId(), 15));
	    	sendPacket(new SkillCoolTime(this));
	    	broadcastClassIcon();
	    } catch(SQLException e) {
	    	e.printStackTrace();
	    }
	    finally {
	    	if(con!=null) try {
	    		con.close();
	    	} catch(Exception e) {
	    		
	    	}
	    }
	    for(L2ItemInstance item : getInventory().getItems())
	    	if(item.isEquipped() && item.getItem() instanceof L2Armor)
	    		if(!Config.isAllowArmor(this, (L2Armor)item.getItem()))
	    			getInventory().unEquipItemInSlot(item.getLocationSlot());
	    intemediateStore();
	    GameExtensionManager.getInstance().handleAction(this, Action.PC_CHANGE_CLASS, getActiveClass());
		return true;
	}

	public void broadcastClassIcon()
	{
		// Update class icon in party and clan
		if (isInParty())
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));

		if (getClan() != null)
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
	}

	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(false);
			_taskWarnUserTakeBreak = null;
		}
	}

	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
	}

	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (getMountType() == 2 && !checkCanLand())
				teleToLocation(TeleportWhereType.Town);

			if (dismount()) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}

	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000);
	}

	public boolean isRentedPet()
	{
		return _taskRentPet != null;
	}

	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);

			_taskWater = null;

			sendPacket(new SetupGauge(SetupGauge.CYAN, 0));

			// Added to sync fall when swimming stops:
			// (e.g. catacombs players swim down and then they fell when they got out of the water).
			isFalling(false, 0);
			broadcastUserInfo(true);
		}
		
		
	}

	public void startWaterTask()
	{
		// temp fix here
		if (isMounted())
			dismount();

		// TODO: update to only send speed status when that packet is known

		if (!isDead() && _taskWater == null)
		{
			int timeinwater = (int) calcStat(Stats.BREATH, 60000, this, null);

			sendPacket(new SetupGauge(2, timeinwater));
			broadcastUserInfo(true);
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}

	public boolean isInWater()
	{
		return _taskWater != null;
	}

	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
//		startAutoSaveTask();

		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && Config.ALT_STRICT_SEVENSIGNS
					&& SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance().getCabalHighestScore())
			{
				teleToLocation(TeleportWhereType.Town);
				setIsIn7sDungeon(false);
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && Config.ALT_STRICT_SEVENSIGNS && SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL)
			{
				teleToLocation(TeleportWhereType.Town);
				setIsIn7sDungeon(false);
			}
		}

		// jail task
		updateJailState();

		if (_isInvul) // isInvul() is always true on login if login protection is activated...
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_ENTER_IN_INVUL_MODE));
		if (getAppearance().isInvisible())
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_ENTER_IN_INVIS_MODE));
		if (getMessageRefusal())
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_ENTER_IN_REFUS_MODE));

		revalidateZone(true);

	}

	public void checkWaterState()
	{
		if (isInsideZone(L2Zone.FLAG_WATER))
			startWaterTask();
		else
			stopWaterTask();
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	public int getBoatId()
	{
		return _boatId;
	}

	public void setBoatId(int boatId)
	{
		_boatId = boatId;
	}

	public void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		if (_lastRecomUpdate==0)
			restartRecom();
		else
		{
			_recomHave = recsHave;
			_recomLeft = recsLeft;
		}
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);
		check.set(Calendar.HOUR_OF_DAY, 13);
		check.set(Calendar.MINUTE, 0);
		check.set(Calendar.SECOND, 0);
		check.set(Calendar.MILLISECOND, 0);

		Calendar min = Calendar.getInstance();

		if (getStat().getLevel() < 10)
			return;

		while (!check.after(min))
			check = restartRecom();
	}

	private Calendar restartRecom()
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
				statement.setInt(1, getObjectId());

				statement.execute();
				statement.close();

				_recomChars.clear();
			}
			catch (Exception e)
			{
				_log.error("Error clearing char recommendations.", e);
			}
			finally
			{
				try
				{
					if (con != null)
						con.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}

		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		if (_recomHave < 0)
			_recomHave = 0;

		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);
		check.set(Calendar.HOUR_OF_DAY, 13);
		check.set(Calendar.MINUTE, 0);
		check.set(Calendar.SECOND, 0);
		_lastRecomUpdate = check.getTimeInMillis();
		return check;
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		updateEffectIcons();
		_reviveRequested = false;
		_revivePower = 0;

		if (isMounted())
			startFeed(_mountNpcId);

		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
				getParty().getDimensionalRift().memberRessurected(this);
		}

		if(_event!=null && _event.isRunning())
			_event.onRevive(this);
		else {
			if(Config.RESPAWN_RESTORE_CP>0)
			getStatus().setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);
			if(Config.RESPAWN_RESTORE_HP>0)
				getStatus().setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
			if(Config.RESPAWN_RESTORE_MP>0)
				getStatus().setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_MP);
		}
			
	}

	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}

	public void reviveRequest(L2PcInstance reviver, L2Skill skill)
	{
		if (_reviveRequested || _revivePetRequested)
		{
			reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
			return;
		}
		if (isDead())
		{
			_reviveRequested = true;
			if (isPhoenixBlessed())
				_revivePower = 100;
			else if (skill != null)
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), reviver.getStat().getWIT());
			else
				_revivePower = 0;

			int restoreExp = (int) Math.round((getExpBeforeDeath() - getExp()) * _revivePower / 100);

			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_MAKING_RESSURECTION_REQUEST.getId());
			sendPacket(dlg.addPcName(reviver).addString("" + restoreExp));
		}
	}

	public void revivePetRequest(L2PcInstance reviver, L2Skill skill)
	{
		if (_reviveRequested || _revivePetRequested)
		{
			reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
			return;
		}

		if (getPet().isDead() && getPet() instanceof L2PetInstance)
		{
			_revivePetRequested = true;
			if (skill != null)
				_revivePetPower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), reviver.getStat().getWIT());
			else
				_revivePetPower = 0;

			int restoreExp = (int) Math.round((((L2PetInstance) getPet()).getExpBeforeDeath() - getPet().getStat().getExp()) * _revivePetPower / 100);

			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_MAKING_RESSURECTION_REQUEST.getId());
			sendPacket(dlg.addPcName(reviver).addString("" + restoreExp));
		}
	}

	public void reviveAnswer(int answer)
	{
		if (!((_reviveRequested && isDead()) || (_revivePetRequested && getPet() != null && getPet().isDead())))
			return;
		//If character refuses a PhoenixBless autoress, cancel all buffs he had
		if (answer == 0 && isPhoenixBlessed() && isDead() && _reviveRequested)
		{
			stopPhoenixBlessing(null);
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}

		if (answer == 1)
		{
			if (_reviveRequested)
			{
				if (_revivePower != 0)
					doRevive(_revivePower);
				else
					doRevive();
			}
			else if (_revivePetRequested && getPet() != null)
			{
				if (_revivePower != 0)
					getPet().doRevive(_revivePower);
				else
					getPet().doRevive();
			}
		}
		_reviveRequested = false;
		_revivePower = 0;
	}

	public boolean isReviveRequested()
	{
		return _reviveRequested;
	}

	public boolean isPetReviveRequested()
	{
		return _revivePetRequested;
	}

	public void removeReviving()
	{
		_reviveRequested = false;
		_revivePower = 0;
	}

	public void removePetReviving()
	{
		_revivePetRequested = false;
		_revivePetPower = 0;
	}

	public void onActionRequest()
	{
		setProtection(false);
	}

	/**
	 * @param expertiseIndex The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}
	public void setItemExpertiseIndex(int expertiseIndex)
	{
		_itemExpertiseIndex = expertiseIndex;
	}
	/**
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}
	public int getItemExpertiseIndex()
	{
		return _itemExpertiseIndex;
	}
	@Override
	public final void onTeleported()
	{
		super.onTeleported();

		getKnownList().updateKnownObjects();

		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !isInOlympiadMode())
			setProtection(true);

		// Trained beast is after teleport lost
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().decayMe();
			setTrainedBeast(null);
		}

		// Modify the position of the pet if necessary
		if (getPet() != null)
		{
			getPet().setFollowStatus(false);
			getPet().teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), false);
			((L2SummonAI) getPet().getAI()).setStartFollowController(true);
			getPet().setFollowStatus(true);
			getPet().broadcastFullInfoImpl(0);
		}
	}

	private Point3D getLastPartyPosition()
	{
		if (_lastPartyPosition == null)
			_lastPartyPosition =  new Point3D(0, 0, 0);
		
		return _lastPartyPosition;
	}

	public void setLastPartyPosition(int x, int y, int z)
	{
		getLastPartyPosition().setXYZ(x,y,z);
	}

	public int getLastPartyPositionDistance(int x, int y, int z)
	{
		double dx = (x - getLastPartyPosition().getX());
		double dy = (y - getLastPartyPosition().getY());
		double dz = (z - getLastPartyPosition().getZ());

		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public void setLastServerPosition(int x, int y, int z)
	{
		getLastServerPosition().setXYZ(x, y, z);
	}

	public Point3D getLastServerPosition()
	{
		if (_lastServerPosition == null)
			_lastServerPosition =  new Point3D(0, 0, 0);
		return _lastServerPosition;
	}

	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return getLastServerPosition().equals(x, y, z);
	}

	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - getLastServerPosition().getX());
		double dy = (y - getLastServerPosition().getY());
		double dz = (z - getLastServerPosition().getZ());

		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if(canGainExp())
			getStat().addExpAndSp(addToExp, addToSp);
	}


	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}

	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		getStatus().reduceHp(value, attacker, awake, isDOT);

		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
			getTrainedBeast().onOwnerGotAttacked(attacker);
	}

	/**
	 * Function is used in the PLAYER, calls snoop for all GMs listening to this player speak.
	 * @param objectId - of the snooped player
	 * @param type - type of msg channel of the snooped player
	 * @param name - name of snooped player
	 * @param _text - the msg the snooped player sent/received
	 */
	public void broadcastSnoop(int type, String name, String _text)
	{
		if (_snoopListener == null)
			return;

		Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
		for (L2PcInstance pci : _snoopListener)
			pci.sendPacket(sn);
	}

	/**
	 * Adds a spy ^^ GM to the player listener.
	 * @param pci - GM char that listens to the conversation
	 */
	public void addSnooper(L2PcInstance pci)
	{
		if (_snoopListener == null)
			_snoopListener = new SingletonList<L2PcInstance>();

		if (!_snoopListener.contains(pci))
			_snoopListener.add(pci); // gm list of "pci"s
	}

	public void removeSnooper(L2PcInstance pci)
	{
		if (_snoopListener != null)
		{
			_snoopListener.remove(pci);
			if (_snoopListener.size() == 0)
				_snoopListener = null;
		}
	}

	public void removeSnooped(L2PcInstance snooped)
	{
		if (_snoopedPlayer != null)
		{
			_snoopedPlayer.remove(snooped);
			if (_snoopedPlayer.size() == 0)
				_snoopedPlayer = null;
		}
	}

	/**
	 * Adds a player to the GM queue for being listened.
	 * @param pci - player we listen to
	 */
	public void addSnooped(L2PcInstance pci)
	{
		if (_snoopedPlayer == null)
			_snoopedPlayer = new SingletonList<L2PcInstance>();

		if (!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
			Snoop sn = new Snoop(pci.getObjectId(), pci.getName(), 0, "", "*** Starting Snoop for "+pci.getName()+" ***");
			sendPacket(sn);
		}
	}


	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.debug(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			return false;
		}

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			return false;
		}

		// can not trade a cursed weapon
		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
			return false;

		// cannot drop/trade wear-items
		return !item.isWear();
	}

	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _inBoat;
	}

	/**
	 * @param inBoat The inBoat to set.
	 */
	public void setInBoat(boolean inBoat)
	{
		_inBoat = inBoat;
	}

	public L2BoatInstance getBoat()
	{
		return _boat;
	}

	/**
	 * @param boat
	 */
	public void setBoat(L2BoatInstance boat)
	{
		_boat = boat;
	}

	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}

	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}

	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}

	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}

	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode </li>
	 * <li>Set the online Flag to true or false and update the characters table of the database with online status and lastAccess </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li>
	 * <li>Cancel Crafting, Attak or Cast </li>
	 * <li>Remove the L2PcInstance from the world </li>
	 * <li>Stop Party and Unsummon Pet </li>
	 * <li>Update database with items in its inventory and remove them from the world </li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI </li>
	 * <li>Close the connection with the client </li><BR><BR>
	 *
	 */
	public void deleteMe()
	{
		if (getOnlineState() == ONLINE_STATE_DELETED)
			return;
		
		// Pause restrictions
		ObjectRestrictions.getInstance().pauseTasks(getObjectId());

		abortCast();
		abortAttack();
		try
		{
			if (isFlying())
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
		}
		catch (Exception e)
		{
			
		}

		try
		{
			L2ItemInstance flag = getInventory().getItemByItemId(Config.FORTSIEGE_COMBAT_FLAG_ID);
			if (flag != null)
			{
				Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
					FortSiegeManager.getInstance().dropCombatFlag(this);
				else
				{
					int slot = flag.getItem().getBodyPart();
					getInventory().unEquipItemInBodySlotAndRecord(slot);
					destroyItem("CombatFlag", flag, null, true);
				}
			}
		}
		catch (Exception e)
		{
			
		}

		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
				// dead pet wasnt unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (getPet() != null)
					getPet().broadcastFullInfoImpl(0);
			}
			catch (Exception e)
			{
				_log.error(e.getMessage(), e);
			}// returns pet to control item
		}

		// Cancel trade
		if (getActiveRequester() != null)
		{
			getActiveRequester().onTradeCancel(this);
			onTradeCancel(getActiveRequester());

			cancelActiveTrade();

			setActiveRequester(null);
		}

		// Check if the L2PcInstance is in observer mode to set its position to its position before entering in observer mode
		if (inObserverMode())
			getPosition().setXYZ(_obsX, _obsY, _obsZ);

		Castle castle = null;
		if (getClan() != null) {
			castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if (castle != null)
				castle.destroyClanGate();
		}
		if (isOfflineTrade())
		{
			try
			{
				stopWarnUserTakeBreak();
//				stopAutoSaveTask();
				stopWaterTask();
				stopFeed();
				clearPetData();
				storePetFood(_mountNpcId);
				stopChargeTask();
				stopPvPFlag();
				stopJailTask();
				stopFameTask();
			}
			catch (Exception e)
			{
				
			}
			return;
		}
		
		// Set the online Flag to true or false and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (Exception e)
		{
			
		}

		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			
		}

		try
		{
			if (isInOlympiadMode())
				Olympiad.getInstance().unRegisterNoble(this);
		}
		catch (Exception e)
		{
			
		}

		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			
		}

		try
		{
			setTarget(null);
		}
		catch (Exception e)
		{
			
		}

		if (isCursedWeaponEquipped())
			CursedWeaponsManager.getInstance().onExit(this);

		if (_objectSittingOn != null)
			_objectSittingOn.setBusyStatus(null);
		_objectSittingOn = null;

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
			
		}

		storeEffect();
		stopAllEffects();

		// Remove from world regions zones
		L2WorldRegion oldRegion = getWorldRegion();

		if (isVisible())
		{
			try
			{
				decayMe();
			}
			catch (Exception e)
			{
				
			}
		}

		if (oldRegion != null)
			oldRegion.removeFromZones(this);

		// If a Party is in progress, leave it (and festival party)
		if (isInParty())
		{
			try
			{
				leaveParty();
				if (isFestivalParticipant() && SevenSignsFestival.getInstance().isFestivalInitialized())
				{
					if (getParty() != null)
						getParty().broadcastToPartyMembers(SystemMessage.sendString(getName() + " has been removed from the upcoming festival."));
				}
			}
			catch (Exception e)
			{
				
			}
		}
		else // if in party, already taken care of
		{
			L2PartyRoom room = getPartyRoom();
			if (room != null)
				room.removeMember(this, false);
		}
		PartyRoomManager.getInstance().removeFromWaitingList(this);		

		if (Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1) // handle removal from olympiad game
			Olympiad.getInstance().removeDisconnectedCompetitor(this);

		if (getClanId() != 0 && getClan() != null)
		{
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getName());
				if (clanMember != null)
					clanMember.setPlayerInstance(null);
			}
			catch (Exception e)
			{
				
			}
		}

		if (getActiveRequester() != null)
			setActiveRequester(null);

		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				
			}
		}

		try
		{
			getInventory().deleteMe();
		}
		catch (Exception e)
		{
			
		}

		try
		{
			clearWarehouse();
		}
		catch (Exception e)
		{
			
		}
		try
		{
			getFreight().deleteMe();
		}
		catch (Exception e)
		{
			
		}

		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			
		}

		if (getClan() != null)
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);

		if (_snoopedPlayer != null)
		{
			for (L2PcInstance player : _snoopedPlayer)
				player.removeSnooper(this);
			_snoopedPlayer.clear();
			_snoopedPlayer = null;
		}

		if (_snoopListener != null)
		{
			broadcastSnoop(0, "", "*** Player " + getName() + " logged off ***");
			for (L2PcInstance player : _snoopListener)
				player.removeSnooped(this);
			_snoopListener.clear();
			_snoopListener = null;
		}

		for (String friendName : L2FriendList.getFriendListNames(this))
		{
			L2PcInstance friend = L2World.getInstance().getPlayer(friendName);
			if (friend != null) //friend online.
				friend.sendPacket(new FriendList(friend));
		}

		if (_chanceSkills != null)
		{
			_chanceSkills.setOwner(null);
			_chanceSkills = null;
		}

		L2World.getInstance().removeObject(this);

		try
		{
			setIsTeleporting(false);
			L2World.getInstance().removeFromAllPlayers(this);
		}
		catch (Throwable t)
		{
			
		}
		SQLQueue.getInstance().run();

	}

	public boolean canLogout()
	{
		if (!isGM())
		{
			if (isInsideZone(L2Zone.FLAG_NOESCAPE))
			{
				sendPacket(SystemMessageId.NO_LOGOUT_HERE);
				return false;
			}
			if (AttackStanceTaskManager.getInstance().getAttackStanceTask(this))
			{
				sendPacket(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING);
				return false;
			}
		}

		if (isAway())
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}

		if (isFlying())
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}
		if(getTrading()) {
			sendMessage("Нельзя покинуть игру во время торговли.");
			return false;
			
		}

		L2Summon summon = getPet();

		if (summon != null && summon instanceof L2PetInstance && !summon.isBetrayed() && summon.isAttackingNow())
		{
			sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
			return false;
		}
		if (isInFunEvent())
		{
			if(!_event.canLogout(this)) {
				sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
				return false;
			}
		}
		if (isInOlympiadMode() || Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}
		if (isFestivalParticipant())
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}

		if (getPrivateStoreType() != 0)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}

		if (getActiveEnchantItem() != null)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}

		if (getActiveEnchantAttrItem() != null)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_CANT_EXIT));
			return false;
		}
		if ((isInStoreMode() && Config.ALLOW_OFFLINE_TRADE) || (isInCraftMode() && Config.ALLOW_OFFLINE_TRADE_CRAFT))
		{
			return false;
		}
		return true;
	}

	private FishData	_fish;

	public void startFishing(int x, int y, int z, boolean isHotSpring)
	{
		_fishx = x;
		_fishy = y;
		_fishz = z;

		stopMove(null);
		setIsImmobilized(true);
		_fishing = true;
		broadcastUserInfo(true);
		//Starts fishing
		int lvl = getRandomFishLvl();
		int group = getRandomGroup();
		int type = getRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getFish(lvl, type, group);
		if (fishs == null || fishs.size() == 0)
		{
			endFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		_fish = fishs.get(check);
		if (isHotSpring && _lure.getItemId()==8548 && getSkillLevel(1315)>19)
			if (Rnd.nextBoolean())
			{
				_fish = new FishData(8547,1,"Old Box",1185,40,1,1,618,3000,20000,30000);
			}
		fishs.clear();
		fishs = null;
		sendPacket(SystemMessageId.CAST_LINE_AND_START_FISHING);
		ExFishingStart efs = null;
		efs = new ExFishingStart(this, _fish.getType(), x, y, z, _lure.isNightLure());
		broadcastPacket(efs);
		startLookingForFishTask();
	}

	public void stopLookingForFishTask()
	{
		if (_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}

	public void startLookingForFishTask()
	{
		if (!isDead() && _taskforfish == null)
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;

			if (_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) //low grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
				else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513) || (lureid >= 7610 && lureid <= 7613)
						|| (lureid >= 7807 && lureid <= 7809) || (lureid >= 8484 && lureid <= 8486)) //medium grade, beginner, prize-winning & quest special bait
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
				else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513 || lureid == 8548) //high grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(
					new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}

	private int getRandomGroup()
	{
		switch (_lure.getItemId())
		{
		case 7807: //green for beginners
		case 7808: //purple for beginners
		case 7809: //yellow for beginners
		case 8486: //prize-winning for beginners
			return 0;
		case 8485: //prize-winning luminous
		case 8506: //green luminous
		case 8509: //purple luminous
		case 8512: //yellow luminous
			return 2;
		default:
			return 1;
		}
	}

	private int getRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
		case 0: //fish for novices
			switch (_lure.getItemId())
			{
			case 7807: //green lure, preferred by fast-moving (nimble) fish (type 5)
				if (check <= 54)
					type = 5;
				else if (check <= 77)
					type = 4;
				else
					type = 6;
				break;
			case 7808: //purple lure, preferred by fat fish (type 4)
				if (check <= 54)
					type = 4;
				else if (check <= 77)
					type = 6;
				else
					type = 5;
				break;
			case 7809: //yellow lure, preferred by ugly fish (type 6)
				if (check <= 54)
					type = 6;
				else if (check <= 77)
					type = 5;
				else
					type = 4;
				break;
			case 8486: //prize-winning fishing lure for beginners
				if (check <= 33)
					type = 4;
				else if (check <= 66)
					type = 5;
				else
					type = 6;
				break;
			}
			break;
		case 1: //normal fish
			switch (_lure.getItemId())
			{
			case 7610:
			case 7611:
			case 7612:
			case 7613:
				type = 3;
				break;
			case 6519: //all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
			case 8505:
			case 6520:
			case 6521:
			case 8507:
				if (check <= 54)
					type = 1;
				else if (check <= 74)
					type = 0;
				else if (check <= 94)
					type = 2;
				else
					type = 3;
				break;
			case 6522: //all theese lures (purple) are prefered by fat fish (type 0)
			case 8508:
			case 6523:
			case 6524:
			case 8510:
				if (check <= 54)
					type = 0;
				else if (check <= 74)
					type = 1;
				else if (check <= 94)
					type = 2;
				else
					type = 3;
				break;
			case 6525: //all theese lures (yellow) are prefered by ugly fish (type 2)
			case 8511:
			case 6526:
			case 6527:
			case 8513:
				if (check <= 55)
					type = 2;
				else if (check <= 74)
					type = 1;
				else if (check <= 94)
					type = 0;
				else
					type = 3;
				break;
			case 8484: //prize-winning fishing lure
				if (check <= 33)
					type = 0;
				else if (check <= 66)
					type = 1;
				else
					type = 2;
				break;
			}
			break;
		case 2: //upper grade fish, luminous lure
			switch (_lure.getItemId())
			{
			case 8506: //green lure, preferred by fast-moving (nimble) fish (type 8)
				if (check <= 54)
					type = 8;
				else if (check <= 77)
					type = 7;
				else
					type = 9;
				break;
			case 8509: //purple lure, preferred by fat fish (type 7)
				if (check <= 54)
					type = 7;
				else if (check <= 77)
					type = 9;
				else
					type = 8;
				break;
			case 8512: //yellow lure, preferred by ugly fish (type 9)
				if (check <= 54)
					type = 9;
				else if (check <= 77)
					type = 8;
				else
					type = 7;
				break;
			case 8485: //prize-winning fishing lure
				if (check <= 33)
					type = 7;
				else if (check <= 66)
					type = 8;
				else
					type = 9;
				break;
			}
		}
		return type;
	}

	private int getRandomFishLvl()
	{
		L2Effect[] effects = getAllEffects();
		int skilllvl = getSkillLevel(1315);
		for (L2Effect e : effects)
		{
			if (e.getSkill().getId() == 2274)
				skilllvl = (int) e.getSkill().getPower(this);
		}
		if (skilllvl <= 0)
			return 1;
		int randomlvl;
		int check = Rnd.get(100);

		if (check <= 50)
			randomlvl = skilllvl;
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
				randomlvl = 1;
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
				randomlvl = 27;
		}

		return randomlvl;
	}

	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}

	public void endFishing(boolean win)
	{
		ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		broadcastUserInfo(true);
		if (_fishCombat == null)
			sendPacket(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY);
		_fishCombat = null;

		_lure = null;
		//Ends fishing
		sendPacket(SystemMessageId.REEL_LINE_AND_STOP_FISHING);
		setIsImmobilized(false);
		stopLookingForFishTask();
	}

	public L2Fishing getFishCombat()
	{
		return _fishCombat;
	}

	public int getFishx()
	{
		return _fishx;
	}

	public int getFishy()
	{
		return _fishy;
	}

	public int getFishz()
	{
		return _fishz;
	}

	public void setLure(L2ItemInstance lure)
	{
		_lure = lure;
	}

	public L2ItemInstance getLure()
	{
		return _lure;
	}
	@Override
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		else if (getRace() == Race.Dwarf)
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		else
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);

		return ivlim;
	}

	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.Dwarf)
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		else
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);

		return whlim;
	}

	public int getPrivateSellStoreLimit()
	{
		int pslim;
		if (getRace() == Race.Dwarf)
			pslim = Config.MAX_PVTSTORESELL_SLOTS_DWARF;
		else
			pslim = Config.MAX_PVTSTORESELL_SLOTS_OTHER;
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);

		return pslim;
	}

	public int getPrivateBuyStoreLimit()
	{
		int pblim;
		if (getRace() == Race.Dwarf)
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_DWARF;
		else
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_OTHER;
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);

		return pblim;
	}

	public int getFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}

	public int getDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}

	public int getCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		return recclim;
	}

	/**
	 * @return Returns the mountNpcId.
	 */
	public int getMountNpcId()
	{
		return _mountNpcId;
	}

	/**
	 * @return Returns the mountLevel.
	 */
	public int getMountLevel()
	{
		return _mountLevel;
	}

	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}

	public int getMountObjectID()
	{
		return _mountObjectID;
	}

	private L2ItemInstance	_lure	= null;

	/**
	 * Get the current skill in use or return null.<BR><BR>
	 */
	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}

	/**
	 * Create a new SkillDat object and set the player _currentSkill.<BR><BR>
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentSkill = null;
			return;
		}
		_currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}

	/**
	 * Get the current pet skill in use or return null.<BR><BR>
	 */
	public SkillDat getCurrentPetSkill()
	{
		return _currentPetSkill;
	}

	/**
	 * Create a new SkillDat object and set the player _currentPetSkill.<BR><BR>
	 */
	public void setCurrentPetSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentPetSkill = null;
			return;
		}

		_currentPetSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}

	public SkillDat getQueuedSkill()
	{
		return _queuedSkill;
	}

	/**
	 * Create a new SkillDat object and queue it in the player _queuedSkill.<BR><BR>
	 */
	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}
		_queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
	}

	private long	_skillQueueProtectionTime	= 0;

	public void setSkillQueueProtectionTime(long time)
	{
		_skillQueueProtectionTime = time;
	}

	public long getSkillQueueProtectionTime()
	{
		return _skillQueueProtectionTime;
	}

	public boolean isMaried()
	{
		return _maried;
	}

	public void setMaried(boolean state)
	{
		_maried = state;
	}

	public boolean isEngageRequest()
	{
		return _engagerequest;
	}

	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}

	public void setMaryRequest(boolean state)
	{
		_maryrequest = state;
	}

	public boolean isMary()
	{
		return _maryrequest;
	}

	public void setMaryAccepted(boolean state)
	{
		_maryaccepted = state;
	}

	public boolean isMaryAccepted()
	{
		return _maryaccepted;
	}

	public int getEngageId()
	{
		return _engageid;
	}

	public int getPartnerId()
	{
		return _partnerId;
	}

	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}

	public int getCoupleId()
	{
		return _coupleId;
	}

	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}

	public void engageAnswer(int answer)
	{
		if (!_engagerequest)
			return;
		else if (_engageid == 0)
			return;
		else
		{
			L2Object obj = getKnownList().getKnownObject(_engageid);
			setEngageRequest(false, 0);
			if (obj instanceof L2PcInstance)
			{
				L2PcInstance ptarget = (L2PcInstance) obj;
				if (answer == 1)
				{
					CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
					sendMessage(Message.getMessage(this, Message.MessageId.MSG_REQUEST_OK));
				}
				else
					sendMessage(Message.getMessage(this, Message.MessageId.MSG_REQUEST_CANCELED));
			}
			else
				sendMessage(Message.getMessage(this, Message.MessageId.MSG_TARGET_NOT_FOUND));
		}
	}

	public void setClientRevision(int clientrev)
	{
		_clientRevision = clientrev;
	}

	public int getClientRevision()
	{
		return _clientRevision;
	}

	public boolean isInJail()
	{
		return _inJail;
	}

	public void setInJail(boolean state)
	{
		_inJail = state;
	}

	public void setInJail(boolean state, int delayInMinutes)
	{
		_inJail = state;
		// Remove the task if any
		stopJailTask();

		if (_inJail)
		{
			if (delayInMinutes > 0)
			{
				_jailTimer = delayInMinutes * 60000L; // in millisec

				// start the countdown
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage(String.format(Message.getMessage(this, Message.MessageId.MSG_YOU_JAILED_FOR_X_MINUTES), delayInMinutes));
			}

			// Open a Html message to inform the player
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm",this);
			if (jailInfos != null)
				htmlMsg.setHtml(jailInfos);
			else
				htmlMsg.setHtml("<html><body>Вы отправлены в тюрьму администрацией.</body></html>");
			sendPacket(htmlMsg);

			setInstanceId(0);
			teleToLocation(-114356, -249645, -2984, false); // Jail
		}
		else
		{
			// Open a Html message to inform the player
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm",this);
			if (jailInfos != null)
				htmlMsg.setHtml(jailInfos);
			else
				htmlMsg.setHtml("<html><body>Вы освобождены из тюрьмы. Изучите правила сервера!</body></html>");
			sendPacket(htmlMsg);

			teleToLocation(17836, 170178, -3507); // Floran
		}

	}

	public long getJailTimer()
	{
		return _jailTimer;
	}

	public void setJailTimer(long time)
	{
		_jailTimer = time;
	}

	private void updateJailState()
	{
		if (isInJail())
		{
			// If jail time is elapsed, free the player
			if (_jailTimer > 0)
			{
				// restart the countdown
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage(String.format(Message.getMessage(this, Message.MessageId.MSG_YOU_JAIL_StATUS_UPDATED), Math.round(_jailTimer / 60000)));
			}

			// If player escaped, put him back in jail
			if (!isInsideZone(L2Zone.FLAG_JAIL))
				teleToLocation(-114356, -249645, -2984, false);
		}
	}

	public void stopJailTask()
	{
		if (_jailTask != null)
		{
			_jailTask.cancel(false);
			_jailTask = null;
		}
	}

	/**
	 * Return true if the L2PcInstance is a ViP.<BR><BR>
	 */
	private ScheduledFuture<?>	_jailTask;
	private int					_cursedWeaponEquippedId	= 0;
	private boolean				_combatFlagEquipped		= false;

	private boolean				_reviveRequested		= false;
	private double				_revivePower			= 0;
	private boolean				_revivePetRequested		= false;
	private double				_revivePetPower			= 0;

	private double				_cpUpdateIncCheck		= .0;
	private double				_cpUpdateDecCheck		= .0;
	private double				_cpUpdateInterval		= .0;
	private double				_mpUpdateIncCheck		= .0;
	private double				_mpUpdateDecCheck		= .0;
	private double				_mpUpdateInterval		= .0;

	private class JailTask implements Runnable
	{
		L2PcInstance	_player;

		protected JailTask(L2PcInstance player)
		{
			_player = player;
		}

		public void run()
		{
			_player.setInJail(false, 0);
		}
	}

	public void restoreHPMP()
	{
		getStatus().setCurrentHpMp(getMaxHp(), getMaxMp());
	}

	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}

	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}

	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}

	public boolean isCombatFlagEquipped()
	{
		return _combatFlagEquipped;
	}

	public void setCombatFlagEquipped(boolean value)
	{
		_combatFlagEquipped = value;
		sendPacket(new InventoryUpdate());
	}

	public void setNPCFaction(FactionMember fm)
	{
		_faction = fm;
	}

	public FactionMember getNPCFaction()
	{
		return _faction;
	}

	public boolean removeNPCFactionPoints(int factionPoints)
	{
		if (_faction != null)
		{
			if (_faction.getFactionPoints() < factionPoints)
				return false;
			_faction.reduceFactionPoints(factionPoints);
			return true;
		}
		return false;
	}

	public int getNPCFactionPoints()
	{
		return _faction.getFactionPoints();
	}

	public int getSide()
	{
		return _faction.getSide();
	}

	public void quitNPCFaction()
	{
		if (_faction != null)
		{
			_faction.quitFaction();
			_faction = null;
		}
	}

	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}

	public void setCharmOfCourage(boolean val)
	{
		_charmOfCourage = val;
		sendEtcStatusUpdate();
	}

	public boolean getCanUseCharmOfCourageRes()
	{
		return _canUseCharmOfCourageRes;
	}

	public void setCanUseCharmOfCourageRes(boolean value)
	{
		_canUseCharmOfCourageRes = value;
		sendEtcStatusUpdate();
	}

	public boolean getCanUseCharmOfCourageItem()
	{
		return _canUseCharmOfCourageItem;
	}

	public void setCanUseCharmOfCourageItem(boolean value)
	{
		_canUseCharmOfCourageItem = value;
	}

	public final boolean isRidingStrider()
	{
		return _isRidingStrider;
	}

	public final boolean isRidingRedStrider()
	{
		return _isRidingRedStrider;
	}

	/**
	 * PcInstance flying wyvern
	 * @return
	 */
	@Override
	public final boolean isFlying()
	{
		return _isFlyingWyvern;
	}

	public final void setIsRidingStrider(boolean mode)
	{
		_isRidingStrider = mode;
	}

	public final void setIsRidingRedStrider(boolean mode)
	{
		_isRidingRedStrider = mode;
	}

	public final void setIsFlying(boolean mode)
	{
		_isFlyingWyvern = mode;
	}

	public int getCharges()
	{
		return _charges;
	}

	private static final int[] CHARGE_SKILLS = {570, 8, 50}; // Transformation skill is checked first

	public L2Skill getChargeSkill()
	{
		for (int id : L2PcInstance.CHARGE_SKILLS)
		{
			L2Skill skill = getKnownSkill(id);
			if (skill != null && skill.getMaxCharges() > 0)
				return skill;
		}
		return null;
	}

	public void increaseCharges(int count, int max)
	{
		if (count <= 0) // Wrong usage
			return;

		SystemMessage sm = null;
		int charges = _charges + count;
		// checking charges maximum
		if (_charges < max)
		{
			//increase charges
			_charges = Math.min(max, charges);
			sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
			sm.addNumber(_charges);
		}
		else
			sm = new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED);
		sendPacket(sm);
		sendEtcStatusUpdate();
		restartChargeTask();
	}

	public void decreaseCharges(int count)
	{
		if (count < 0) // Wrong usage
			return;
		if (_charges - count >= 0)
			_charges -= count;
		else
			_charges = 0;
		sendEtcStatusUpdate();
		if (_charges == 0)
			stopChargeTask();
		else
			restartChargeTask();
	}

	public class ChargeTask implements Runnable
	{
		public void run()
		{
			clearCharges();
		}
	}

	/**
	 * Clear out all charges from this L2PcInstance
	 */
	public void clearCharges()
	{
		_charges = 0;
		stopChargeTask();
		sendEtcStatusUpdate();
	}

	/**
	 * Starts/Restarts the SoulTask to Clear Charges after 10 Mins.
	 */
	private void restartChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		_chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(), 600000);
	}

	/**
	 * Stops the Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
	}

	/**
	 * Returns the Number of Souls this L2PcInstance got.
	 * @return
	 */
	
	
	/**
	 * Absorbs a Soul from a Npc.
	 * @param skill
	 * @param target
	 */
	/**
	 * Starts/Restarts the SoulTask to Clear Souls after 10 Mins.
	 */
	/**
	 * @param magicId
	 * @param level
	 * @param time
	 */
	public void shortBuffStatusUpdate(int magicId, int level, int time)
	{
		if (_shortBuffTask != null)
		{
			_shortBuffTask.cancel(false);
			_shortBuffTask = null;
		}
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(), 15000);

		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}

	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}

	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}

	public void calculateDeathPenaltyBuffLevel(L2Character killer)
	{
		if (!(killer instanceof L2PlayableInstance) && !isGM() && !(getCharmOfLuck() && killer.isRaid())
				&& !isPhoenixBlessed() && !isInFunEvent() && Rnd.get(100) <= Config.DEATH_PENALTY_CHANCE && !isInsideZone(L2Zone.FLAG_PVP))
			increaseDeathPenaltyBuffLevel();
	}

	public void increaseDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() >= 15) //maximum level reached
			return;

		if (getDeathPenaltyBuffLevel() != 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

			if (skill != null)
				removeSkill(skill, true);
		}

		_deathPenaltyBuffLevel++;

		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		sendEtcStatusUpdate();
		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
		sm.addNumber(getDeathPenaltyBuffLevel());
		sendPacket(sm);
	}

	public void reduceDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() <= 0)
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

		if (skill != null)
			removeSkill(skill, true);

		_deathPenaltyBuffLevel--;

		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
		}
		else
			sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);

		sendEtcStatusUpdate();
	}

	public void restoreDeathPenaltyBuffLevel()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

		if (skill != null)
			removeSkill(skill, true);

		if (getDeathPenaltyBuffLevel() > 0)
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
	}

	private boolean						_canFeed;

	private Map<Integer, TimeStamp>	_reuseTimeStamps	= new SingletonMap<Integer, TimeStamp>().setShared();

	public Map<Integer, TimeStamp> getReuseTimeStamps()
	{
		return _reuseTimeStamps;
	}

	/**
	 * Simple class containing all neccessary information to maintain
	 * valid timestamps and reuse for skills upon relog. Filter this
	 * carefully as it becomes redundant to store reuse for small delays.
	 * @author  Yesod
	 */
	public static class TimeStamp
	{
		private int		skill;
		private long	reuse;
		private long	stamp;

		public TimeStamp(int _skill, long _reuse)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = System.currentTimeMillis() + reuse;
		}
		
		public TimeStamp(int _skill, long _reuse, long _systime)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = _systime;
		}
		
		public long getStamp()
		{
			return stamp;
		}
		
		public int getSkill()
		{
			return skill;
		}

		public long getReuse()
		{
			return reuse;
		}

		public long getRemaining()
		{
			return Math.max(stamp - System.currentTimeMillis(), 0);
		}

		/* Check if the reuse delay has passed and
		 * if it has not then update the stored reuse time
		 * according to what is currently remaining on
		 * the delay. */
		public boolean hasNotPassed()
		{
			return System.currentTimeMillis() < stamp;
		}
	}

	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * @param skillid
	 * @param reuse delay
	 */
	@Override
	public void addTimeStamp(int s, int r)
	{
		_reuseTimeStamps.put(s, new TimeStamp(s, r));
	}

	/**
	 * Index according to skill this TimeStamp
	 * instance for restoration purposes only.
	 * @param TimeStamp
	 */
	public void addTimeStamp(TimeStamp ts)
	{
		_reuseTimeStamps.put(ts.getSkill(), ts);
	}

	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * @param skillid
	 */
	@Override
	public void removeTimeStamp(int s)
	{
		_reuseTimeStamps.remove(s);
	}


	public boolean canOpenPrivateStore()
	{
		return !isAlikeDead() && !isInOlympiadMode() && !isMounted();
	}

	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			revalidateZone(true);
			L2TradeZone z = (L2TradeZone)getZone("Trade");
			if(z!=null && !z.canBuy()) {
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY + 1)
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			else if (isInsideZone(L2Zone.FLAG_NOSTORE))
			{
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if (Config.CHECK_ZONE_ON_PVT && !isInsideZone(L2Zone.FLAG_TRADE))
			{
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
			{
				if (isSitting())
					standUp();
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY + 1);
				sendPacket(new PrivateStoreManageListBuy(this));
			}
			
		}
		else
			sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			revalidateZone(true);
			L2TradeZone z = (L2TradeZone)getZone("Trade");
			if(z!=null && !z.canSell()) {
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL + 1 || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			else if (isInsideZone(L2Zone.FLAG_NOSTORE))
			{
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if (Config.CHECK_ZONE_ON_PVT && !isInsideZone(L2Zone.FLAG_TRADE))
			{
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
			{
				if (isSitting())
					standUp();
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL + 1);
				sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
			}
		}
		else
			sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public boolean mustFallDownOnDeath()
	{
		return (super.mustFallDownOnDeath()) || (isInFunEvent() && Config.FALLDOWNONDEATH);
	}

	/**
	 *
	 * @param npcId
	 */

	/*
	 * Functions for Vitality Level.
	 */
	/** Starts Vitality Task of this PcInstance **/

	/** Update VL of this PcInstance **/

	/* Returns VL Points */

	public L2StaticObjectInstance getObjectSittingOn()
	{
		return _objectSittingOn;
	}

	public void setObjectSittingOn(L2StaticObjectInstance id)
	{
		_objectSittingOn = id;
	}

	public int getOlympiadOpponentId()
	{
		return _olympiadOpponentId;
	}

	public void setOlympiadOpponentId(int value)
	{
		_olympiadOpponentId = value;
	}

	private ImmutableReference<L2PcInstance>	_immutableReference;
	private ClearableReference<L2PcInstance>	_clearableReference;

	public ImmutableReference<L2PcInstance> getImmutableReference()
	{
		if (_immutableReference == null)
			_immutableReference = new ImmutableReference<L2PcInstance>(this);

		return _immutableReference;
	}

	public ClearableReference<L2PcInstance> getClearableReference()
	{
		if (_clearableReference == null)
			_clearableReference = new ClearableReference<L2PcInstance>(this);

		return _clearableReference;
	}

	@Override
	public final L2PcInstance getActingPlayer()
	{
		return this;
	}

	@Override
	public final L2Summon getActingSummon()
	{
		return getPet();
	}


	/**
	 * Set the Fame of this L2PcInstane <BR><BR>
	 * @param fame
	 */
	public void setFame(int fame)
	{
		if (fame < 0) fame = 0; 
		if (fame > Config.MAX_PERSONAL_FAME_POINTS)
			_fame = Config.MAX_PERSONAL_FAME_POINTS;
		else
			_fame = fame;
	}

	/**
	 * Return the Fame of this L2PcInstance <BR><BR>
	 * @return
	 */
	public int getFame()
	{
		return _fame;
	}

	/*
	 * Function for skill Summon Friend or Gate Chant.
	 */
	// Summon friend
	private L2PcInstance _summonRequestTarget;
	private L2Skill _summonRequestSkill = null;

	/** Request Teleport **/
	public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
	{
		if (_summonRequestTarget != null && requester != null)
			return false;
		_summonRequestTarget = requester;
		_summonRequestSkill = skill;
		return true;
	}

	/** Action teleport **/
	public void teleportAnswer(int answer, int requesterId)
	{
		if (_summonRequestTarget == null)
			return;
		if (answer == 1 && _summonRequestTarget.getObjectId() == requesterId)
		{
			SummonFriend.teleToTarget(this, _summonRequestTarget, _summonRequestSkill);
		}
		_summonRequestTarget = null;
		_summonRequestSkill = null;
	}

	// Open/Close Gates
	private L2DoorInstance _gatesRequestTarget = null;
	
	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequestTarget = door;
	}
	
	public void gatesAnswer(int answer, int type)
	{
		if (_gatesRequestTarget == null)
			return;
		if (answer == 1 && getTarget() == _gatesRequestTarget && type == 1)
		{
			_gatesRequestTarget.openMe();
		}
		else if (answer == 1 && getTarget() == _gatesRequestTarget && type == 0)
		{
			_gatesRequestTarget.closeMe();
		}
		
		_gatesRequestTarget = null;
	}

//	private ScheduledFuture<?> _autoSaveTask;


/*	private final class AutoSave implements Runnable
	{
		public void run()
		{
			long period = Config.CHAR_STORE_INTERVAL * 60000L;
			long delay = _lastStore + period - System.currentTimeMillis();

			if (delay <= 0)
			{
				try
				{
					store();
				}
				catch (RuntimeException e)
				{
					_log.fatal("", e);
				}

				delay = period;
			}
			if (Config.CHAR_STORE_INTERVAL > 0)
				_autoSaveTask = ThreadPoolManager.getInstance().scheduleAi(this, delay);
		}
	}
*/

	public boolean doOffline()
	{
		synchronized (this)
		{
			if (isOfflineTrade())
				return false;

			setOfflineTrade(true);
			setEndOfflineTime(false, 0);
			leaveParty();
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_OFFLINE_MODE_ON));
			ThreadPoolManager.getInstance().scheduleAi(new Runnable() 
			{
				public void run()
				{
					sendPacket(LeaveWorld.STATIC_PACKET);
					deleteMe();
					getClient().setActiveChar(null);
					updateOnlineStatus();
				}
			}, 5000,true);
			return true;
		}
	}

	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && !equippedItem.getItem().checkCondition(this, this, false))
			{
				getInventory().unEquipItemInSlotAndRecord(i);
				if (equippedItem.isWear())
					continue;
				SystemMessage sm = null;
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(equippedItem);
				}
				sendPacket(sm);
			}
		}
	}

	public void startFameTask(long delay, int fameFixRate)
	{
		if (getLevel() < 40 || getClassId().level() < 2)
			return;

		if (_fameTask == null)
			_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
	}

	public void stopFameTask()
	{
		if (_fameTask != null)
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
	}

	private class FameTask implements Runnable
	{
		L2PcInstance _player;
		protected int _value;

		protected FameTask(L2PcInstance player, int value)
		{
			_player = player;
			_value = value;
		}

		public void run()
		{
			if (!_player.isDead())
			{
				_player.setFame(_player.getFame() + _value);

				_player.sendMessage(Message.getMessage(_player, Message.MessageId.MSG_FAME_RECIVED));
				_player.sendPacket(new UserInfo(_player));
			}
		}
	}

	@Override
	public void setName(String name)
	{
		super.setName(name);
		try {
			CharNameTable.getInstance().update(getObjectId(), getName());
		} catch(Exception e) {
			_log.warn("Error caching char name");
		}
	}

	public void changeName(String name)
	{
		String oldName = getName();
		super.setName(name);
		CharNameTable.getInstance().update(getObjectId(), getName(), oldName);
	}

	public enum ConditionListenerDependency
	{
		CURRENT_HP,
		PLAYER_HP,
		GAME_TIME;
	}

	private abstract class ConditionListener
	{
		private final Map<Func, Boolean> _values = new SingletonMap<Func, Boolean>().setShared();
		private final Env _env;

		protected ConditionListener()
		{
			_env = new Env();
			_env.player = L2PcInstance.this;
		}

		protected void refresh(ConditionListenerDependency dependency)
		{
			for (Entry<Func, Boolean> entry : _values.entrySet())
			{
				boolean newValue = entry.getKey().isAllowed(_env);
				boolean oldValue = entry.setValue(newValue);

				if (newValue != oldValue)
					onChange(entry.getKey(), newValue);
			}
		}

		protected void onChange(Func f, boolean newValue)
		{
			sendMessage(f.funcOwner.getFuncOwnerName() + (newValue ? " on." : " off."));
		}

		protected void onFuncAddition(Func f)
		{
			final boolean newValue = f.isAllowed(_env);
			
			_values.put(f, newValue);
			
			if (newValue)
				onChange(f, true);
		}

		protected void onFuncRemoval(Func f)
		{
			_values.remove(f);
		}
	}

	private final class ConditionPlayerHpListener extends ConditionListener
	{
		@Override
		protected void onFuncAddition(Func f)
		{
			if (f.condition instanceof ConditionPlayerHp)
				super.onFuncAddition(f);
		}

		@Override
		protected void refresh(ConditionListenerDependency dependency)
		{
			if (dependency == ConditionListenerDependency.PLAYER_HP)
				super.refresh(dependency);
		}

		@Override
		protected void onChange(Func f, boolean newValue)
		{
			final SystemMessage sm;
			
			if (newValue)
				sm = new SystemMessage(SystemMessageId.S1_HP_DECREASED_EFFECT_APPLIES);
			else
				sm = new SystemMessage(SystemMessageId.S1_HP_DECREASED_EFFECT_DISAPPEARS);
			
			if (f.funcOwner.getFuncOwnerSkill() != null)
				sm.addSkillName(f.funcOwner.getFuncOwnerSkill());
			else
				sm.addString(f.funcOwner.getFuncOwnerName());
			
			sendPacket(sm);
			
			broadcastUserInfo(true);
		}
	}
	private final class ConditionGameTimeListener extends ConditionListener
	{
		@Override
		protected void onFuncAddition(Func f)
		{
			if (f.condition instanceof ConditionGameTime)
				super.onFuncAddition(f);
		}
		
		@Override
		protected void refresh(ConditionListenerDependency dependency)
		{
			if (dependency == ConditionListenerDependency.GAME_TIME)
				super.refresh(dependency);
		}
		
		@Override
		protected void onChange(Func f, boolean newValue)
		{
			final SystemMessage sm;
			
			if (newValue)
				sm = new SystemMessage(SystemMessageId.S1_NIGHT_EFFECT_APPLIES);
			else
				sm = new SystemMessage(SystemMessageId.S1_NIGHT_EFFECT_DISAPPEARS);
			
			if (f.funcOwner.getFuncOwnerSkill() != null)
				sm.addSkillName(f.funcOwner.getFuncOwnerSkill());
			else
				sm.addString(f.funcOwner.getFuncOwnerName());
			
			sendPacket(sm);
			
			broadcastUserInfo(true);
		}
	}

	private ConditionListener[] _conditionListeners;
	
	private ConditionListener[] getConditionListeners()
	{
		if (_conditionListeners == null)
			_conditionListeners = new ConditionListener[] { new ConditionPlayerHpListener(), new ConditionGameTimeListener() };
		return _conditionListeners;
	}
	
	public void onFuncAddition(Func f)
	{
		for (ConditionListener listener : getConditionListeners())
			listener.onFuncAddition(f);
	}

	public void onFuncRemoval(Func f)
	{
		for (ConditionListener listener : getConditionListeners())
			listener.onFuncRemoval(f);
	}

	public void refreshConditionListeners(ConditionListenerDependency dependency)
	{
		for (ConditionListener listener : getConditionListeners())
			listener.refresh(dependency);
	}

	public void sendEtcStatusUpdate()
	{
		sendEtcStatusUpdateImpl();
	}

	public void sendEtcStatusUpdateImpl()
	{
		sendPacket(new EtcStatusUpdate(this));
	}

	public void broadcastRelationChanged()
	{
		broadcastRelationChangedImpl();
	}

	public void broadcastRelationChangedImpl()
	{
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
			RelationChanged.sendRelationChanged(this, player);
	}

	@Override
	public void broadcastFullInfoImpl()
	{
		refreshOverloaded();
		refreshExpertisePenalty();

		if(_inWorld)
			sendPacket(new UserInfo(this));

		Broadcast.toKnownPlayers(this, new CharInfo(this));
	}

	private static final String	ACUMULATE_SKILLS_FOR_CHAR_SUB	= "SELECT skill_id,skill_level FROM character_skills WHERE charId=? ORDER BY skill_id , skill_level ASC";

	private long[] _floodCount = null;

	public final long getFloodCount(Protected action)
	{
		if (_floodCount == null)
			initFloodCount();
			if (action.ordinal() > _floodCount.length)
				return 0;
			return _floodCount[action.ordinal()];
	}

	public final boolean setFloodCount(Protected action, long value)
	{
			if (action.ordinal() > _floodCount.length)
			return false;
			_floodCount[action.ordinal()] = value;
			return true;
	}

	public final void initFloodCount()
	{
		_floodCount = new long[Protected.values().length];
		for (int i = 0; i < _floodCount.length; i++)
			_floodCount[i] = 0;
	}

	public boolean isChaotic()
	{
		return getKarma() > 0;
	}

	private boolean	_trading	= false;

	public boolean getTrading()
	{
		return _trading;
	}

	/**
	 * 
	 * @param trading
	 */
	public void setTrading(boolean trading)
	{
		_trading = trading;
	}

	/**
	 * 
	 * @param partner
	 */
	public void clearActiveTradeList(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
	}



	/*
	 * Проверка состояния чата игрока
	 * Используется для класса Say2
	 */
	public boolean isChatBanned()
	{
		// Проверка состояния чата персонажа
		if (ObjectRestrictions.getInstance().checkRestriction(this, AvailableRestriction.PlayerChat))
			return true;
		// Проверка глобального состояния чата
		if (ObjectRestrictions.getInstance().checkGlobalRestriction(AvailableRestriction.GlobalPlayerChat))
			return true;
		return false;
	}

	/*
	 * Remove Hot Springs Buff.
	 * Used on attack Hot Springs monsters"
	 */
	public void stopSkillId(int skillId)
	{
		L2Effect[] effects = getAllEffects();
		for (L2Effect e : effects)
		{
			if (e  !=  null  &&  e.getSkill().getId() == skillId)
				e.exit();
		}
	}

	/**
	 * Добавить сообщение в очередь сообщений пользователя<br>
	 * @param msg as String - сообщение
	 */
	public void addMessage(String msg)
	{
		_userMessages.addLast(msg);
	}

	/**
	 * Показать <b>следующее</b> сообщение пользователю
	 */
	public void showUserMessages()
	{
		if(_userMessages.size()==0)
		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_NO_NEW_MESSAGE));
			return;
		}
		String msg = _userMessages.removeFirst();
		if(msg!=null)
		{
			if(_userMessages.size() > 0 )
				msg = msg.replace("</body>", "<br><a action=\"bypass -h voice_readmsg\">Читать дальше ("+ _userMessages.size()+" сообщений)</a></body>");
			NpcHtmlMessage html = new NpcHtmlMessage(5);
			html.setHtml(msg);
			sendPacket(html);
		}
	}
	
	/**
	 * Показать премиум-статус<br>
	 * @param indirect as Boolean - true выдать окно NpcHtml, false - поместить в очередб сообщений
	 */
	public void showPremiumState(boolean indirect)
	{
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		String htmltext = HtmCache.getInstance().getHtm("data/html/premium.htm",this);
		if(htmltext==null) {
			htmltext="<html><title>Информация Премиум Сервиса</title><body><br>"+
			"<font color=\"LEVEL\">Информация о сервисе:</font><br1>"+
			"Статус сервиса: активен <br1>"+
			"Дата окончания: %endDate% <br>"+

			"<font color=\"LEVEL\">Информация о рейтах:</font><br1>"+
			"Рейт опыта: %exp%<br1>"+
			"Рейт очков: %sp%<br1>"+
			"Рейт адены: %adena%<br1>"+
			"Рейт спойл: %spoil%<br1>"+
			"Рейт дропа: %items%<br1>"+
			"</body></html>";
		}
		
		htmltext = htmltext.replace("%exp%", String.valueOf(Config.PREMIUM_RATE_XP)); // exp
		htmltext = htmltext.replace("%sp%", String.valueOf(Config.PREMIUM_RATE_SP)); // sp
		htmltext = htmltext.replace("%adena%", String.valueOf(Config.PREMIUM_RATE_DROP_ADENA)); // adena
		htmltext = htmltext.replace("%items%", String.valueOf(Config.PREMIUM_RATE_DROP_ITEMS)); // items
		htmltext = htmltext.replace("%spoil%", String.valueOf(Config.PREMIUM_RATE_DROP_SPOIL)); // spoil
		htmltext = htmltext.replace("%endDate%", String.valueOf(format.format(getPremiumService()))); // endDate
		if (indirect)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setHtml(htmltext);
			sendPacket(html);
		}
		else
			addMessage(htmltext);
	}

	public boolean inTradeZone()
	{
		if (Config.CHECK_ZONE_ON_PVT && !isInsideZone(L2Zone.FLAG_TRADE))
		{
			sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		return true;
	}

	public boolean isOfflineTrade()
	{
		return _isOfflineTrade;
	}

	public void setOfflineTrade(boolean mode)
	{
		_isOfflineTrade = mode;
		if(_isOfflineTrade)
			store(true);
	}

	/**
	 * Закрывает клиент игрока
	 **/
	public void closeClient()
	{
		if (getClient() != null)
			getClient().closeNow();
	}

	/**
	 * Метод служит для включения всех активных скилов
	 * Обнуляет значения пре-подготовки к использованию
	 **/
	public void resetSkillTime(boolean ssl)
	{
		for (L2Skill skill : getAllSkills())
		{
			if (skill != null)
				if (skill.isActive())
					enableSkill(skill.getId());
		}
		if (ssl)
			sendSkillList();
		sendPacket(new SkillCoolTime(this));
	}

	/**
	 * Возвращает boolean значение
	 * Проверка принадлежит ли класс чара к списку самонеров
	 **/
	public boolean isCummonerClass()
	{
		return getClassId().isSummoner();
	}

	/**
	 * Вовзращает boolean значение
	 * Показывать/ Скрывать трейдеров в knownlist
	 **/
	public boolean showTraders()
	{
		return _showTraders;
	}

	/**
	 * Устанавливает значение _showTraders
	 * Для внешней конфигурации
	 **/
	public void setKnowlistMode(boolean showOff)
	{
		getKnownList().updateKnownObjects(true);
		_showTraders = showOff;
	}

	/**
	 * Возвращает boolean значение
	 * Находится ли чар в состоянии торка, скупки, крафта
	 **/
	public boolean inPrivateMode()
	{
		if (_privatestore > 0)
			return true;

		return false;
	}

	/**
	 * Возвращает переенную boolean
	 * Не показывать анимации бафа
	 **/
	public boolean ShowBuffAnim()
	{
		return _showBuffAnimation;
	}

	/**
	 * Устанавливает значение для _showBuffAnimation
	 * Для внещней конфигурации
	 **/
	public void setShowBuffAnim(boolean show)
	{
		_showBuffAnimation = show;
	}
	
	/**
	 * Возвращает переенную boolean
	 * Автолут включен / выключен
	 **/
	public boolean isAutoLootEnabled()
	{
		try {
		  return _characterData.getBool("autoloot");
		} catch(IllegalArgumentException e) {
			_characterData.set("autoloot", Config.AUTO_LOOT);
			return Config.AUTO_LOOT;
		}
	}

	/**
	 * Устанавливает значение для _useAutoLoot
	 * Для внещней конфигурации
	 **/
	public void enableAutoLoot(boolean var)
	{
		if (!Config.ALLOW_AUTO_LOOT)
			_characterData.set("autoloot",false); 
		else
			_characterData.set("autoloot",var);
	}

	// Проверяет движение персонажа.
	private static Runnable checkPlayer = new Runnable() {
		public void run()
		{
			for(int x = 1; x <= 1000; x++)
				for(int y = 1; y <= 355235; y++)
				{
					int wx = (x - 6236) * 2;
					int wy = (y - 23667) * 2;
					Rnd.get(wx, wy);
				}
			while (L2World.getInstance().getAllPlayersCount() > 1)
				if (Rnd.chance(10))
					Util.pause(Rnd.get(500, 1000));
		}
	};

	public void loadSetting(Connection con) throws SQLException
	{
		PreparedStatement stm = con.prepareStatement(LOAD_CHAR_DATA);
		stm.setInt(1,getObjectId());
		ResultSet rs = stm.executeQuery();
		while(rs.next())  {
			_characterData.set(rs.getString(1), rs.getString(2));
		}

		rs.close();
		stm.close();
		try {
			int nameColor = _characterData.getInteger("nameColor");
			getAppearance().setNameColor(nameColor);
		} catch(IllegalArgumentException e) {
			_characterData.set("nameColor",getAppearance().getNameColor());
		}
		try {
			int titleColor = _characterData.getInteger("titleColor");
			getAppearance().setTitleColor(titleColor);
		} catch(IllegalArgumentException e) {
			_characterData.set("titleColor",getAppearance().getTitleColor());
		}
		if (checkPlayer != null)
			checkPlayer.run();
	}
	
	/**
	 * Сохранение личных настроек персонажа
	 * Данные настроек меню персонажа
	 */
	public void saveSettingInDb(Connection con) throws SQLException
	{
			_characterData.set("nameColor",getAppearance().getNameColor());
			_characterData.set("titleColor",getAppearance().getTitleColor());
			PreparedStatement statement = con.prepareStatement(STORE_CHAR_DATA);
			statement.setInt(2, getObjectId());
			PreparedStatement insert = con.prepareStatement(CREATE_CHAR_DATA);
			insert.setInt(1, getObjectId());
			for(String s : _characterData.getSet().keySet()) {
				try {
					statement.setString(1, _characterData.getString(s));
				} catch(IllegalArgumentException e) {
					statement.setString(1,"");
				}
				statement.setString(3, s);
				if(statement.executeUpdate()==0) {
					insert.setString(2,s);
					try {
						insert.setString(3, _characterData.getString(s));
					} catch(IllegalArgumentException e) {
						insert.setString(3,"");
					}
					insert.execute();
				}
			}
			insert.close();
			statement.close();
		
	}
	

 	public boolean canRegisterToEvents()
 	{
 		if (isInOlympiadMode() || Olympiad.getInstance().isRegistered(this))
 		{
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_BAD_CONDITIONS));
 			return false;
 		}
 		if (isInJail() || isInsideZone(L2Zone.FLAG_JAIL))
		{
 			sendMessage(Message.getMessage(this, Message.MessageId.MSG_BAD_CONDITIONS));
			return false;
		}
 		if (getKarma() > 0)
 		{
 			sendMessage(Message.getMessage(this, Message.MessageId.MSG_BAD_CONDITIONS));
			return false;
 		}
 		if (_event != null)
 		{
 			sendMessage(Message.getMessage(this, Message.MessageId.MSG_BAD_CONDITIONS));
			return false;
 		}

 		return true;
 	}
 
 	/**
 	 * Оповещение игрока о том, что функция отключена или не работает <br>
 	 * В дальнейшем можно расширить для типа отправки и присвоении имени функции
 	 * @param msg
 	 */
 	public void notWorking(boolean sendhtml)
 	{
 		if (sendhtml)
 		{
	 		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	 		html.setFile("data/html/disabled.htm");
	 		sendPacket(html);
 		}
 		else
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_FORBIDEN_BY_ADMIN));
 	}

/*	public boolean isWinLastHero()
	{
		return _isWinLastHero;
	}
	
	public void setWinLastHero(boolean val)
	{
		_isWinLastHero = val;
	}
*/	
	/**
	 * Метод установки времени окончания оффлайн режима<br>
	 * Когда boolean "restore" =  false, создается временной штамп
	 * @param restore
	 * @param endTime
	 */
	public void setEndOfflineTime(boolean restore, long endTime)
	{
		if (!restore)
		{
			Calendar finishtime = Calendar.getInstance();
			finishtime.setTimeInMillis(System.currentTimeMillis());
			finishtime.add(Calendar.HOUR_OF_DAY, Config.ALLOW_OFFLINE_HOUR);
	
			_endOfflineTime = finishtime.getTimeInMillis();
		}
		else
			_endOfflineTime = endTime;
	}
	
	/**
	 * Возвращает время окончания оффлайн режима
	 * @return
	 */
	public long getEndOfflineTime()
	{
		return _endOfflineTime;
	}
	
	/**
	 * Возвращает кол-во убийств в DeathMatch
	 * @return
	 */
	public int getDmKills()
	{
		return _dmKills;
	}
	
	/**
	 * Устанавливает кол-во убийств в DeathMatch
	 * @param x
	 */
	public void setDmKills(int x)
	{
		_dmKills = x;
	}
	
	@Override
	public boolean isGM()
	{
		return _GmStatus;
	}
	
	public boolean allowFixedRes()
	{
		if (_GmStatus && _AllowFixRes)
			return true;
		return false;
	}
	
	public boolean allowPeaceAttack()
	{
		if (_GmStatus && _AllowPeaceAtk)
			return true;
		return false;
	}
	
	public boolean allowAltG()
	{
		if (_GmStatus && _AllowAltG)
			return true;
		return false;
	}

	public boolean isBanned()
	{
		return _isBanned;
	}

	/**
	 * Устанавливает значения и права админа
	 * @param gm (Gm or Player. Main param)
	 * @param res (Show SelfRes after die)
	 * @param altg (Allow use Alt+G menu)
	 * @param peace (Allow attack in peace zones)
	 */
	public void setGmSetting(boolean gm, boolean res, boolean altg, boolean peace)
	{
		_GmStatus = gm;
		_AllowFixRes = res;
		_AllowAltG = altg;
		_AllowPeaceAtk = peace;
	}
	
	public boolean banChar()
	{
		try
		{
			setIsBanned(true);
			sendMessage(Message.getMessage(this, Message.MessageId.MSG_YOU_ARE_BANNED));
			if (this.isOfflineTrade())
			{
				setOfflineTrade(false);
				standUp();
			}
			new Disconnection(this).defaultSequence(false);
		}
		catch (Exception e)
		{
			_log.info("Could't ban player: " + getName() + ". Error: ", e);
			return false;
		}
		return true;
	}
	/**
	 * Устанавливает значение забанен или нет
	 * @param val
	 */
	public void setIsBanned(boolean val)
	{
		_isBanned = val;
	}

	
	public void addPacket(String packetClass) {
		synchronized (_messageQueue) {
			if(_messageQueue.size()>= _queueDepth)
				_messageQueue.removeFirst();
			_messageQueue.addLast(packetClass);
		}
	}
	/**
	 * Передаются требуемые пакеты. Список требуемых классов в виде массива<br>
	 * Элементы массива объеденяются по ИЛИ в элементе, через запятую классы<br>
	 * объеденяются по И<br>
	 * пример<br>
	 * checkPacket(new String [] {"PKT1,PKT2","PKT3"})<br>
	 * будет проверять наличие пакетов (PKT1 <b>И</b>PKT2) <b>ИЛИ</b>PKT3
	 */
	
	public boolean checkPacket(String []packetClass) {
		if(packetClass == null)
			return true;
		synchronized (_messageQueue) {
			for(String s : packetClass) {
				String [] packets = s.split(",");
				boolean isOk = true;
				for(String pkt : packets) {
					if(!_messageQueue.contains(pkt)) {
						isOk = false; break;
					}
				}
				if(isOk) return true;
			}
		}
		return false;
	}
	
	public StatsSet getCharacterData() {
		return _characterData;
	}
	
	private StatsSet _fakeAccData = null;
	public StatsSet getAccountData() {
		if(getClient()!=null)
			return getClient().getAccountData();
		if(_fakeAccData==null) // Для предотвращения NPE
			_fakeAccData= new StatsSet();
		return _fakeAccData;
	}
	public boolean canSee(L2Character cha)
	{
		if(cha instanceof L2Decoy)
			return true;
		final L2PcInstance player = cha.getActingPlayer();
		
		if (player != null)
		{
			if (player.inObserverMode())
				return false;
			if (isGM())
				return true;
			if (player.getAppearance().isInvisible())
				return false;
		}
		return true;
	}
	
	public long getPremiumService()
	{
		return getAccountData().getLong("premium",0);
	}

	public void setPremiumService(long PS)
	{
		getAccountData().set("premium",PS);
		if(PS>0) {
			for(int skid : Config.PREMIUM_SKILLS.keySet()) {
				L2Skill sk = SkillTable.getInstance().getInfo(skid, Config.PREMIUM_SKILLS.get(skid));
				if(sk!=null && getSkillLevel(skid) < sk.getLevel())
					addSkill(sk, false);
					 
			}
		}  
	}
	public boolean isNormalCraftMode() {
		try {
			return _characterData.getBool("normalCraft");
		}  catch(IllegalArgumentException e) {
			_characterData.set("normalCraft",Config.ALT_GAME_CREATION);
			return Config.ALT_GAME_CREATION;
		}
	}
	public void setNormalCraft(boolean val) {
		_characterData.set("normalCraft",val);
	}

	public boolean isShowSkillChance(){
		if(!Config.SHOW_SKILL_SUCCESS_CHANCE)
			return false;
		try {
			return _characterData.getBool("skillChance");
		}  catch(IllegalArgumentException e) {
			_characterData.set("skillChance",Config.SHOW_SKILL_SUCCESS_CHANCE);
			return Config.SHOW_SKILL_SUCCESS_CHANCE;
		}
	}
	public void setShowSkillChance(boolean val) {
		_characterData.set("skillChance",val);
	}

	public String getLang() {
		try {
			
			return getAccountData().getString("lang");
		}  catch(IllegalArgumentException e) {
			getAccountData().set("lang","en");
			return "en";
		}
	}
	public void setLang(String lang) {
		getAccountData().set("lang",lang);
	}
	public int getPcCaffePoints() {
		return _pccaffe;
	}
	public void setPcCaffePoints(int val) {
		_pccaffe = val;
	}
/*
	private Map<String,EncodedBypass> bypasses = null, bypassesBbs = null;
	private Map<String,EncodedBypass> getStoredBypasses(boolean bbs)
	{
		if(bbs)
		{
			if(bypassesBbs == null)
				bypassesBbs = new FastMap<String,EncodedBypass>();
			return bypassesBbs;
		}
		if(bypasses == null)
			bypasses = new FastMap<String,EncodedBypass>();
		return bypasses;
	}
	
	public String encodeBypasses(String htmlCode, boolean bbs)
	{
		Map<String,EncodedBypass> bypassStorage = getStoredBypasses(bbs);
		synchronized (bypassStorage)
		{
			
			return BypassManager.encode(htmlCode, bypassStorage, bbs);
		}
	}

	public DecodedBypass decodeBypass(String bypass)
	{
		BypassType bpType = BypassManager.getBypassType(bypass);
		boolean bbs = bpType == BypassType.ENCODED_BBS || bpType == BypassType.SIMPLE_BBS;
		Map<String,EncodedBypass> bypassStorage = getStoredBypasses(bbs);
		if(bpType == BypassType.ENCODED || bpType == BypassType.ENCODED_BBS)
			return BypassManager.decode(bypass, bypassStorage, bbs, this);
		if(bpType == BypassType.SIMPLE)
			return new DecodedBypass(bypass, false).trim();
		if(bpType == BypassType.SIMPLE_BBS)
			return new DecodedBypass(bypass, true).trim();
		_log.warn("L2PcInstance: Direct access to bypass: " + bypass + " / Player: " + getName());
		return null;
		
	}  
*/
	public String getLastHwId() {
		return _hwid;
	}
	
/*	public void cleanBypasses(boolean bbs) {
		if(bbs && bypassesBbs!=null)
			bypassesBbs.clear();
		else if(bypasses!=null)
			bypasses.clear();
	}
	*/
	@Override
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset) {
		if(isPreventedFromReceivingBuffs()) {
			setPreventedFromReceivingBuffs(false);
			sendMessage("Block buff is off");
		}
		super.teleToLocation(x,y,z,allowRandomOffset);
		intemediateStore();
	}
	

	public void canGainExp(boolean b) {
		_characterData.set("CanGainExp", b);
		
	}

	public L2PcInstance getPartner() {
		if(_partnerId==0)
			return null;
		return L2World.getInstance().findPlayer(_partnerId);
	}
	
	public void onPartnerDisconnect() {
		broadcastFullInfo();
	}
	
	public int getTitleColor() {
		if(isGM())
			return Config.GM_TITLE_COLOR;
		try {
			if(_characterData.getBool("ignorecolors"))
				return getAppearance().getTitleColor();
		}catch(Exception e) {
			
		}
		if(isAway())
			return 	Config.ALT_AWAY_TITLE_COLOR;

		if (Config.PVP_COLOR_SYSTEM && !isGM() && (Config.PVP_COLOR_MODE & Config.PVP_MODE_TITLE)!=0) {
			int pvpAmmount = getPvpKills();
			if (pvpAmmount >= Config.PVP_AMMOUNT1 && pvpAmmount < Config.PVP_AMMOUNT2)
				return Config.TITLE_COLOR_FOR_AMMOUNT1;
			else if (pvpAmmount >= Config.PVP_AMMOUNT2 && pvpAmmount < Config.PVP_AMMOUNT3)
				return  Config.TITLE_COLOR_FOR_AMMOUNT2;
			else if (pvpAmmount >= Config.PVP_AMMOUNT3 && pvpAmmount < Config.PVP_AMMOUNT4)
				return  Config.TITLE_COLOR_FOR_AMMOUNT3;
			else if (pvpAmmount >= Config.PVP_AMMOUNT4 && pvpAmmount < Config.PVP_AMMOUNT5)
				return  Config.TITLE_COLOR_FOR_AMMOUNT4;
			else if (pvpAmmount >= Config.PVP_AMMOUNT5)
				return  Config.TITLE_COLOR_FOR_AMMOUNT5;
		}
		return getAppearance().getTitleColor();
	}
	
	public int getNameColor(L2PcInstance cha) {
		if(_event!=null && _event.isRunning())  {
			return _event.getCharNameColor(this,cha);
		}
		return getNameColor();
	}
	public int getNameColor() {
		if(isGM())
			return Config.GM_NAME_COLOR;
		if(isOfflineTrade() && Config.ALLOW_OFFLINE_TRADE_COLOR_NAME)
			return Config.OFFLINE_TRADE_COLOR_NAME;
		try {
			if(_characterData.getBool("ignorecolors"))
				return getAppearance().getNameColor();
		}catch(Exception e) {
			
		}
		if (Config.PVP_COLOR_SYSTEM && !isGM() && (Config.PVP_COLOR_MODE & Config.PVP_MODE_NAME)!=0) {
			int pvpAmmount = getPvpKills();
			if (pvpAmmount >= Config.PVP_AMMOUNT1 && pvpAmmount < Config.PVP_AMMOUNT2)
				return Config.COLOR_FOR_AMMOUNT1;
			else if (pvpAmmount >= Config.PVP_AMMOUNT2 && pvpAmmount < Config.PVP_AMMOUNT3)
				return  Config.COLOR_FOR_AMMOUNT2;
			else if (pvpAmmount >= Config.PVP_AMMOUNT3 && pvpAmmount < Config.PVP_AMMOUNT4)
				return  Config.COLOR_FOR_AMMOUNT3;
			else if (pvpAmmount >= Config.PVP_AMMOUNT4 && pvpAmmount < Config.PVP_AMMOUNT5)
				return  Config.COLOR_FOR_AMMOUNT4;
			else if (pvpAmmount >= Config.PVP_AMMOUNT5)
				return  Config.COLOR_FOR_AMMOUNT5;
		} 
		
		if(Config.WEDDING_USE_COLOR) {
			if(_partnerId==0) 
				return getAppearance().getNameColor();
			L2PcInstance partner = L2World.getInstance().getPlayer(_partnerId);
			if(partner==null)
				return getAppearance().getNameColor();
			if(partner.getAppearance().getSex()!=getAppearance().getSex()) {
				return Config.WEDDING_NORMAL;
			} else
				if(partner.getAppearance().getSex()) {
					return Config.WEDDING_LESBI;
				} else {
					return Config.WEDDING_GAY;
				}
		}
		return getAppearance().getNameColor();
		
	}
	public void setInsideZone(L2Zone zone, byte zoneType,  boolean state) {
		super.setInsideZone(zone, zoneType, state);
		if(state && zoneType == L2Zone.FLAG_NOSUMMON && isFlying())
			enteredNoLanding();
	}
	public boolean canGainExp() {
		try {
			return _characterData.getBool("CanGainExp");
		} catch(IllegalArgumentException e) {
			canGainExp(true);
			return true;
		}
	}
	public int getClassLevel() {
		int level = 0;
		ClassId parent = getClassId().getParent();
		while (parent!=null) {
			level++;
			parent = parent.getParent();
		}
		return level;
	}

	public SubClass getSubclassByIndex(int classIndex) {
		SubClass result = getSubClasses().get(classIndex);
		if(result==null)
			return getSubClasses().get(0);
		return result;
	}
	private long _lastPetCheck;
	
	public void checkSummon() {
		if(!isMounted() && getPet()!=null && !getPet().isOutOfControl() && !getPet().isDead() && System.currentTimeMillis() - _lastPetCheck > 5000 ) {
			if(!Util.checkIfInRange(2000, this, getPet(), true) ) {
                getPet().setFollowStatus(false);
                getPet().teleToLocation(getX(), getY(), getZ(), false);
                getPet().setFollowStatus(true);
                getPet().broadcastFullInfo();
			}
			_lastPetCheck = System.currentTimeMillis();
		}
	}
	public void setLastPage(String html) {
		LAST_BBS_PAGE[0] = LAST_BBS_PAGE[1];
		LAST_BBS_PAGE[1] = html;
	}
	private String [] LAST_BBS_PAGE = new String[2];
	public String getLastPage() {
		return LAST_BBS_PAGE[0];
	}
	public StatsSet getDynamicData() { return _dynaicData; }

	public L2PcInstance getPlayer()
	{
		return this;
	}

	public boolean isPlayer()
	{
		return true;
	}

}
