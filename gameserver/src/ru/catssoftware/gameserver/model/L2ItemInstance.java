package ru.catssoftware.gameserver.model;

import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.instancemanager.ItemsOnGroundManager;
import ru.catssoftware.gameserver.instancemanager.MercTicketManager;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;
import ru.catssoftware.gameserver.taskmanager.SQLQueue;
import ru.catssoftware.gameserver.templates.item.*;
import ru.catssoftware.sql.SQLQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;


public final class L2ItemInstance extends L2Object implements FuncOwner
{
	protected static final Logger	_log		= Logger.getLogger(L2ItemInstance.class.getName());
	private static final Logger	_logItems	= Logger.getLogger("item");

	public static enum ItemLocation
	{
		VOID, INVENTORY, PAPERDOLL, WAREHOUSE, CLANWH, PET, PET_EQUIP, LEASE, FREIGHT, NPC
	}

	private static final int	MANA_CONSUMPTION_RATE		= 60000;
	private ScheduledFuture<?>	itemLootShedule				= null;
	private int					_chargedSoulshot			= CHARGED_NONE;
	private int					_chargedSpiritshot			= CHARGED_NONE;
	private boolean				_chargedFishtshot			= false;
	private boolean				_consumingMana				= false;
	private boolean				_decrease					= false;
	private L2Augmentation		_augmentation				= null;
	private int					_mana						= -1;
	public static final int		CHARGED_NONE				= 0;
	public static final int		CHARGED_SOULSHOT			= 1;
	public static final int		CHARGED_SPIRITSHOT			= 1;
	public static final int		CHARGED_BLESSED_SOULSHOT	= 2;
	public static final int		CHARGED_BLESSED_SPIRITSHOT	= 2;
	public static final int		UNCHANGED					= 0;
	public static final int		ADDED						= 1;
	public static final int		MODIFIED					= 2;
	public static final int		REMOVED						= 3;
	private int					_lastChange					= 2;
	private int					_ownerId;
	private int					_count;
	private int					_initCount;
	private int					_time;
	private int					_type1;
	private int					_type2;
	private long				_dropTime;
	private int					_locData;
	private int					_enchantLevel;
	private int					_priceSell;
	private int					_priceBuy;
	private final int			_itemId;
	private final int			_itemDisplayId;
	private final L2Item		_item;
	private ItemLocation		_loc;
	private boolean				_wear;
	private boolean				_protected;
	private boolean				_existsInDb;
	private boolean				_storedInDb;
	public int					engraver;
	private int					_engraverId;
	private int					_rewardId;
	private long 				_rewardTime;
	private String				_itemData = "";	

	private String				_process = "";
	private int					_creator;
	private long				_creationTime;
	public int getEngraver()
	{
		return _engraverId;
	}

	public void engrave()
	{
		_engraverId = _ownerId;
	}
	public void clearEngravement()
	{
		if(_engraverId==_ownerId)
			_engraverId = 0;
	}

	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 *
	 * @param objectId :
	 *            int designating the ID of the object in the world
	 * @param itemId :
	 *            int designating the ID of the item
	 * @param itemDisplayId :
	 *            int designating the ID of the item to the client
	 */
	public L2ItemInstance(int objectId, int itemId, int itemDisplayId)
	{
		super(objectId);
		getKnownList();
		_itemId = itemId;
		_itemDisplayId = itemDisplayId;
		_item = ItemTable.getInstance().getTemplate(itemId);
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
	}

	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 *
	 * @param objectId :
	 *            int designating the ID of the object in the world
	 * @param itemId :
	 *            int designating the ID of the item
	 */
	public L2ItemInstance(int objectId, int itemId)
	{
		super(objectId);
		getKnownList();
		_itemId = itemId;
		_item = ItemTable.getInstance().getTemplate(itemId);
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		_itemDisplayId = _item.getItemDisplayId();
		setCount(1);
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
	}

	/**
	 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
	 *
	 * @param objectId :
	 *            int designating the ID of the object in the world
	 * @param item :
	 *            L2Item containing informations of the item
	 */
	public L2ItemInstance(int objectId, L2Item item)
	{
		super(objectId);
		getKnownList();
		_itemId = item.getItemId();
		_itemDisplayId = item.getItemDisplayId();
		_item = item;
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_mana = _item.getDuration();
	}

	/**
	 * Sets the ownerID of the item
	 *
	 * @param process :
	 *            String Identifier of process triggering this action
	 * @param owner_id :
	 *            int designating the ID of the owner
	 * @param creator :
	 *            L2PcInstance Player requesting the item creation
	 * @param reference :
	 *            L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void setOwnerId(String process, int owner_id, L2PcInstance creator, L2Object reference)
	{
		if(GameExtensionManager.getInstance().handleAction(this, Action.ITEM_SETOWNER, process, owner_id,_ownerId)!=null)
			return;
		
		_process = process;
		if(_ownerId==0) {
			_creationTime = System.currentTimeMillis()/1000;
			if(reference!=null)
				if(reference instanceof L2NpcInstance)
					_creator = ((L2NpcInstance)reference).getNpcId();
				else 
					_creator =  reference.getObjectId();
			else
				_creator = creator==null?0:creator.getObjectId();
		}
		setOwnerId(owner_id);
		
		if (Config.LOG_ITEMS && !Config.IGNORE_LOG.contains(process.toUpperCase()))
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CHANGE:" + process);
			param.add(this);
			param.add(creator);
			param.add(reference);
			_logItems.info(param);
		}
	}

	/**
	 * Sets the ownerID of the item
	 *
	 * @param owner_id :
	 *            int designating the ID of the owner
	 */
	protected void setOwnerId(int owner_id)
	{
		if (owner_id == _ownerId)
			return;
		if(GameExtensionManager.getInstance().handleAction(this, Action.ITEM_SETOWNER, owner_id)!=null)
			return;
		_ownerId = owner_id;
		_storedInDb = false;
	}

	/**
	 * Returns the ownerID of the item
	 *
	 * @return int : ownerID of the item
	 */
	public int getOwnerId()
	{
		return _ownerId;
	}

	/**
	 * Sets the location of the item
	 *
	 * @param loc :
	 *            ItemLocation (enumeration)
	 */
	public void setLocation(ItemLocation loc)
	{
		setLocation(loc, 0);
	}

	/**
	 * Sets the location of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param loc :
	 *            ItemLocation (enumeration)
	 * @param loc_data :
	 *            int designating the slot where the item is stored or the village for freights
	 */
	public void setLocation(ItemLocation loc, int loc_data)
	{
		if (loc == _loc && loc_data == _locData)
			return;
		_loc = loc;
		_locData = loc_data;
		_storedInDb = false;
	}

	public ItemLocation getLocation()
	{
		return _loc;
	}

	/**
	* Sets the quantity of the item.<BR><BR>
	* @param count the new count to set
	*/
	public void setCount(int count)
	{
		if (getCount() == count)
			return;

		_count = count >= -1 ? count : 0;
		_storedInDb = false;
	}

	/**
	* @return Returns the count.
	*/
	public int getCount()
	{
		return _count;
	}

	// No logging (function designed for shots only)
	public void changeCountWithoutTrace(int count, L2PcInstance creator, L2Object reference)
	{
		changeCount(null, count, creator, reference);
	}

	/**
	 * Sets the quantity of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param process :
	 *            String Identifier of process triggering this action
	 * @param count :
	 *            int
	 * @param creator :
	 *            L2PcInstance Player requesting the item creation
	 * @param reference :
	 *            L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void changeCount(String process, int count, L2PcInstance creator, L2Object reference)
	{
		if (count == 0)
			return;

		if (count > 0 && getCount() > Integer.MAX_VALUE - count)
			setCount(Integer.MAX_VALUE);
		else
			setCount(getCount() + count);

		if (getCount() < 0)
			setCount(0);

		_storedInDb = false;

		if (Config.LOG_ITEMS && process != null && !Config.IGNORE_LOG.contains(process.toUpperCase()))
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CHANGE:" + process);
			param.add(this);
			param.add(creator);
			param.add(reference);
			_logItems.info(param);
		}
	}

	/**
	 * Returns if item is equipable
	 *
	 * @return boolean
	 */
	public boolean isEquipable()
	{
		return !(_item.getBodyPart() == 0 || _item instanceof L2EtcItem);
	}

	/**
	 * Returns if item is equipped
	 *
	 * @return boolean
	 */
	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
	}

	/**
	 * Returns the slot where the item is stored
	 *
	 * @return int
	 */
	public int getLocationSlot()
	{
		if (Config.ASSERT)
			assert _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP || _loc == ItemLocation.FREIGHT || _loc == ItemLocation.INVENTORY;

		return _locData;
	}

	/**
	 * Returns the characteristics of the item
	 *
	 * @return L2Item
	 */

	public L2Item getItem()
	{
		return _item;
	}

	public int getCustomType1()
	{
		return _type1;
	}

	public int getCustomType2()
	{
		return _type2;
	}

	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}

	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}

	public void setDropTime(long time)
	{
		_dropTime = time;
	}

	public long getDropTime()
	{
		return _dropTime;
	}

	public boolean isWear()
	{
		return _wear;
	}

	public void setWear(boolean newwear)
	{
		_wear = newwear;
	}

	/**
	 * Returns the type of item
	 *
	 * @return Enum
	 */
	public AbstractL2ItemType getItemType()
	{
		return _item.getItemType();
	}

	/**
	 * Returns the ID of the item
	 *
	 * @return int
	 */
	public int getItemId()
	{
		return _itemId;
	}

	public int getItemDisplayId()
	{
		return _itemDisplayId;
	}

	/**
	 * Returns true if item is an EtcItem
	 *
	 * @return boolean
	 */
	public boolean isEtcItem()
	{
		return (_item instanceof L2EtcItem);
	}

	/**
	 * Returns true if item is a Weapon/Shield
	 *
	 * @return boolean
	 */
	public boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}

	/**
	 * Returns true if item is an Armor
	 *
	 * @return boolean
	 */
	public boolean isArmor()
	{
		return (_item instanceof L2Armor);
	}

	/**
	 * Returns the characteristics of the L2EtcItem
	 *
	 * @return L2EtcItem
	 */
	public L2EtcItem getEtcItem()
	{
		if (_item instanceof L2EtcItem)
			return (L2EtcItem) _item;

		return null;
	}

	/**
	 * Returns the characteristics of the L2Weapon
	 *
	 * @return L2Weapon
	 */
	public L2Weapon getWeaponItem()
	{
		if (_item instanceof L2Weapon)
			return (L2Weapon) _item;

		return null;
	}

	/**
	 * Returns the characteristics of the L2Armor
	 *
	 * @return L2Armor
	 */
	public L2Armor getArmorItem()
	{
		if (_item instanceof L2Armor)
			return (L2Armor) _item;

		return null;
	}

	/**
	 * Returns the quantity of crystals for crystallization
	 *
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _item.getCrystalCount(_enchantLevel);
	}

	/**
	 * Returns the reference price of the item
	 *
	 * @return int
	 */
	public int getReferencePrice()
	{
		return _item.getReferencePrice();
	}

	/**
	 * Returns the name of the item
	 *
	 * @return String
	 */
	public String getItemName()
	{
		return _item.getName();
	}

	/**
	 * Returns the price of the item for selling
	 *
	 * @return int
	 */
	public int getPriceToSell()
	{
		return (isConsumable() ? (int) (_priceSell * Config.RATE_CONSUMABLE_COST) : _priceSell);
	}

	/**
	 * Sets the price of the item for selling <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param price :
	 *            int designating the price
	 */
	public void setPriceToSell(int price)
	{
		_priceSell = price;
		_storedInDb = false;
	}

	/**
	 * Returns the price of the item for buying
	 *
	 * @return int
	 */
	public int getPriceToBuy()
	{
		return (isConsumable() ? (int) (_priceBuy * Config.RATE_CONSUMABLE_COST) : _priceBuy);
	}

	/**
	 * Sets the price of the item for buying <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param price :
	 *            int
	 */
	public void setPriceToBuy(int price)
	{
		_priceBuy = price;
		_storedInDb = false;
	}

	/**
	 * Returns the last change of the item
	 *
	 * @return int
	 */
	public int getLastChange()
	{
		return _lastChange;
	}

	/**
	 * Sets the last change of the item
	 *
	 * @param lastChange :
	 *            int
	 */
	public void setLastChange(int lastChange)
	{
		_lastChange = lastChange;
	}

	/**
	 * Returns if item is stackable
	 *
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _item.isStackable();
	}

	/**
	 * Returns if item is dropable
	 *
	 * @return boolean
	 */
	public boolean isDropable()
	{
		return !isAugmented() && _item.isDropable();
	}

	/**
	 * Returns if item is destroyable
	 *
	 * @return boolean
	 */
	public boolean isDestroyable()
	{
		return _item.isDestroyable();
	}

	/**
	 * Returns if item is tradeable
	 *
	 * @return boolean
	 */
	public boolean isTradeable()
	{
		return !isAugmented() && _item.isTradeable() && !isBounded();
	}

	
	/**
	 * Returns if item is sellable
	 * @return boolean
	 */
	public boolean isSellable()
	{
		return !isAugmented() && _item.isSellable();
	}

	/**
	 * Returns if item is consumable
	 *
	 * @return boolean
	 */
	public boolean isConsumable()
	{
		return _item.isConsumable();
	}

	/**
	 * Returns if item is a heroitem
	 *
	 * @return boolean
	 */
	public boolean isHeroItem()
	{
		return _item.isHeroItem();
	}

	public boolean isBounded() {
		if(_itemData!=null)
			return _itemData.contains("b");
		return false;
	}
	public void bind() {
		if(isBounded())
			return;
		L2PcInstance pc = L2World.getInstance().getPlayer(getOwnerId());
		if(pc!=null ) {
			if(!_itemData.isEmpty())
				_itemData+=";";
			_itemData+="b";
		}
	}
	/**
	 * Returns if item is available for manipulation
	 *
	 * @return boolean
	 */
	public boolean isAvailable(L2PcInstance player, boolean allowAdena)
	{
		return ((!isEquipped()) // Not equipped
				&& (getItem().getType2() != 3) // Not Quest Item
				&& (getItem().getType2() != 4 || getItem().getType1() != 1) // Not Money or Shield Armor
				&& (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
				&& (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
				&& (allowAdena || getItemId() != 57) // Not adena
				&& (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId())
				&& (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != getItemId())
				&& (isTradeable()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.catssoftware.gameserver.model.L2Object#onAction(ru.catssoftware.gameserver.model.L2PcInstance) also check constraints: only soloing castle owners may pick
	 *      up mercenary tickets of their castle
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		int _castleId = MercTicketManager.getInstance().getTicketCastleId(_itemId);

		if (_castleId > 0)
		{
			boolean privMatch = (player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES;
			boolean castleMatch = ((player.getClan() != null) && player.getClan().getHasCastle() == _castleId);
			if (privMatch && castleMatch)
			{
				if (player.isInParty())
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_YOU_CANT_PICKUP_MERCH_IN_PARTY));
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				}
				else
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			}
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else {
			if(getOwnerId()==0) {
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
			}
			else 
				player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	/**
	 * Returns the level of enchantment of the item
	 *
	 * @return int
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	/**
	 * Sets the level of enchantment of the item
	 *
	 * @param enchantLevel
	 */
	public void setEnchantLevel(int enchantLevel)
	{
		if (_enchantLevel == enchantLevel)
			return;
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}

	/**
	 * Returns the physical defense of the item
	 *
	 * @return int
	 */
	public int getPDef()
	{
		if (_item instanceof L2Armor)
			return ((L2Armor) _item).getPDef();
		return 0;
	}

	/**
	 * Returns whether this item is augmented or not
	 *
	 * @return true if augmented
	 */
	public boolean isAugmented()
	{
		return _augmentation != null;
	}

	/**
	 * Returns the augmentation object for this item
	 *
	 * @return augmentation
	 */
	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}

	/**
	 * Sets a new augmentation
	 *
	 * @param augmentation
	 * @return return true if sucessfull
	 */
	public boolean setAugmentation(L2Augmentation augmentation)
	{
		// there shall be no previous augmentation..
		if (_augmentation != null)
			return false;
		_augmentation = augmentation;
		updateItemAttributes();
		return true;
	}

	/**
	 * Remove the augmentation
	 */
	private SQLQuery _removeAqugmentationQueue = new SQLQuery() {

		@Override
		public void execute(Connection con) {
			try {
			PreparedStatement statement = null;
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?"); // Remove the entry since the item also has no elemental enchant

			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();
			} catch(SQLException e) { }
		}
		
	};
	public void removeAugmentation()
	{
		if(_augmentation!=null) try {
			
			Connection con = L2DatabaseFactory.getInstance().getConnection();
			_removeAqugmentationQueue.execute(con);
			con.close();
			_augmentation = null;
		} catch(SQLException e) {}
	}

	/**
	 * Restore the augmentation from DB
	 */
	public void restoreAttributes()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT augAttributes,augSkillId,augSkillLevel FROM item_attributes WHERE itemId=?");
			statement.setInt(1, getObjectId());
			ResultSet rs = statement.executeQuery();
			rs = statement.executeQuery();
			if (rs.next())
			{
				int aug_attributes = rs.getInt(1);
				int aug_skillId = rs.getInt(2);
				int aug_skillLevel = rs.getInt(3);
				if (aug_attributes != -1 && aug_skillId != -1 && aug_skillLevel != -1)
					_augmentation = new L2Augmentation(aug_attributes, aug_skillId, aug_skillLevel);
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fatal("Could not restore augmentation and elemental data for item " + getObjectId() + " from DB: "+e.getMessage(), e);
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

	public void updateItemAttributes()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?,?,?)");
			statement.setInt(1, getObjectId());
			if (_augmentation == null)
			{
				statement.setInt(2, -1);
				statement.setInt(3, -1);
				statement.setInt(4, -1);
			}
			else
			{
				statement.setInt(2, _augmentation.getAttributes());
				if(_augmentation.getSkill() == null)
				{
					statement.setInt(3, 0);
					statement.setInt(4, 0);
				}
				else
				{
					statement.setInt(3, _augmentation.getSkill().getId());
					statement.setInt(4, _augmentation.getSkill().getLevel());
				}
			}
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fatal("Could not remove elemental enchant for item: "+getObjectId()+" from DB:", e);
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
				e.printStackTrace();
			}
		}
	}


	/**
	 * Used to decrease mana (mana means life time for shadow items)
	 */
	public class ScheduleConsumeManaTask implements Runnable
	{
		private L2ItemInstance	_shadowItem;

		public ScheduleConsumeManaTask(L2ItemInstance item)
		{
			_shadowItem = item;
		}

		public void run()
		{
			try
			{
				// decrease mana
				if (_shadowItem != null)
					_shadowItem.decreaseMana(true);
			}
			catch (Exception e)
			{
				_log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Returns true if this item is a shadow item Shadow items have a limited life-time
	 *
	 * @return
	 */
	public boolean isShadowItem()
	{
		return (_mana >= 0);
	}

	/**
	 * Sets the mana for this shadow item <b>NOTE</b>: does not send an inventory update packet
	 *
	 * @param mana
	 */
	public void setMana(int mana)
	{
		_mana = mana;
	}

	/**
	 * Returns the remaining mana of this shadow item
	 *
	 * @return lifeTime
	 */
	public int getMana()
	{
		return _mana;
	}

	/**
	 * Decreases the mana of this shadow item, sends a inventory update schedules a new consumption task if non is running optionally one could force a new task
	 *
	 * @param resetConsumingMana
	 *            a new consumption task if item is equipped
	 */
	public void decreaseMana(boolean resetConsumingMana)
	{
		if (!isShadowItem())
			return;

		if (_mana > 0)
			_mana--;

		if (_storedInDb)
			_storedInDb = false;
		if (resetConsumingMana)
			_consumingMana = false;

		L2PcInstance player = L2World.getInstance().getPlayer(getOwnerId());
		if (player != null && !player.isDead())
		{
			SystemMessage sm;
			switch (_mana)
			{
			case 10:
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
				sm.addString(getItemName());
				player.sendPacket(sm);
				break;
			case 5:
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
				sm.addString(getItemName());
				player.sendPacket(sm);
				break;
			case 1:
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
				sm.addString(getItemName());
				player.sendPacket(sm);
				break;
			}

			if (_mana == 0) // The life time has expired
			{
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
				sm.addString(getItemName());
				player.sendPacket(sm);

				// unequip
				if (isEquipped())
				{
					L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
					InventoryUpdate iu = new InventoryUpdate();
					for (L2ItemInstance element : unequiped)
					{
						player.checkSSMatch(null, element);
						iu.addModifiedItem(element);
					}
					player.sendPacket(iu);
				}

				if (getLocation() != ItemLocation.WAREHOUSE)
				{
					// destroy
					player.getInventory().destroyItem("L2ItemInstance", this, player, null);

					// send update
					InventoryUpdate iu = new InventoryUpdate();
					iu.addRemovedItem(this);
					player.sendPacket(iu);

					StatusUpdate su = new StatusUpdate(player.getObjectId());
					su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
					player.sendPacket(su);
				}
				else
					player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);

				// delete from world
				L2World.getInstance().removeObject(this);
			}
			else
			{
				// Reschedule if still equipped
				if (!_consumingMana && isEquipped())
					scheduleConsumeManaTask();
				if (getLocation() != ItemLocation.WAREHOUSE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
				}
			}
		}
	}

	private void scheduleConsumeManaTask()
	{
		_consumingMana = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
	}

	/**
	 * Returns the type of charge with SoulShot of the item.
	 *
	 * @return int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public int getChargedSoulshot()
	{
		return _chargedSoulshot;
	}

	/**
	 * Returns the type of charge with SpiritShot of the item
	 *
	 * @return int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public int getChargedSpiritshot()
	{
		return _chargedSpiritshot;
	}

	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}

	/**
	 * Sets the type of charge with SoulShot of the item
	 *
	 * @param type :
	 *            int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public void setChargedSoulshot(int type, boolean isSkill)
	{
		_chargedSoulshot = type;
		L2PcInstance owner=L2World.getInstance().getPlayer(getOwnerId());
		L2PetInstance petowner=L2World.getInstance().getPet(getOwnerId());
		if (owner!=null)
			if ((type==CHARGED_NONE)&&(isEquipped())&&(owner.getAutoSoulShot().size()>0))
			{
				if (isSkill)
					owner.rechargeAutoSoulShot(true, false, false, true);
				else
					owner.rechargeAutoSoulShot(true, false, false, false);
			}
		if (petowner!=null)
			if ((type==L2ItemInstance.CHARGED_NONE))
				petowner.getOwner().rechargeAutoSoulShot(true, true, true, false);		
	}

	/**
	 * Sets the type of charge with SpiritShot of the item
	 *
	 * @param type :
	 *            int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public void setChargedSpiritshot(int type)
	{
		_chargedSpiritshot = type;
		L2PcInstance owner=L2World.getInstance().getPlayer(getOwnerId());
		L2PetInstance petowner=L2World.getInstance().getPet(getOwnerId());		
		if (owner!=null)
			if ((type==CHARGED_NONE)&&(isEquipped())&&(owner.getAutoSoulShot().size()>0))
			{
					owner.rechargeAutoSoulShot(true, true, false, true);
			}
		if (petowner!=null)
			if ((type==L2ItemInstance.CHARGED_NONE))
				petowner.getOwner().rechargeAutoSoulShot(true, true, true, false);		
	}

	public void setChargedFishshot(boolean type)
	{
		_chargedFishtshot = type;
	}

	/**
	 * This function basically returns a set of functions from L2Armor/L2Weapon, but may add additional functions, if this particular item instance is
	 * enhanched for a particular player.
	 *
	 * @param player :
	 *            L2Character designating the player
	 * @return Func[]
	 */
	public Func[] getStatFuncs(L2Character player)
	{
		if (getItem() instanceof L2Equip)
			return ((L2Equip) getItem()).getStatFuncs(this, player);
		return L2Equip.EMPTY_FUNC_SET;
	}

	/**
	 * Updates database.<BR>
	 * Не актуален, обновление предметов осуществялется ТОЛЬКО автоматически 
	 */
	
	public void updateDatabase()
	{
		updateDatabase(false);
	}

	/**
	* Updates the database.<BR>
	*
	* @param force if the update should necessarilly be done.
	*/
	public void updateDatabase(boolean force)
	{
		if (getUpdateMode(force) != UpdateMode.NONE)
			SQLQueue.getInstance().add(UPDATE_DATABASE_QUERY);
	}

	private final SQLQuery UPDATE_DATABASE_QUERY = new SQLQuery()
	{
		public void execute(Connection con)
		{
			switch (getUpdateMode(true))
			{
			case INSERT:
				insertIntoDb(con);
				break;
			case UPDATE:
				updateInDb(con);
				break;
			case REMOVE:
				removeFromDb(con);
				break;
			}
		}
	};

	private UpdateMode getUpdateMode(boolean force)
	{
		if (_wear)
			return UpdateMode.NONE;

		boolean shouldBeInDb = true;
		shouldBeInDb &= (_ownerId != 0);
		shouldBeInDb &= (_loc != ItemLocation.VOID);
		shouldBeInDb &= (_count != 0 || _loc == ItemLocation.LEASE);

		if (_existsInDb)
		{
			if (!shouldBeInDb)
				return UpdateMode.REMOVE;
			else if (!Config.LAZY_ITEMS_UPDATE || force)
				return UpdateMode.UPDATE;
		}
		else
		{
			if (shouldBeInDb && _loc != ItemLocation.NPC)
				return UpdateMode.INSERT;
		}

		return UpdateMode.NONE;
	}

	private static enum UpdateMode
	{
		INSERT,
		UPDATE,
		REMOVE,
		NONE
	}

	/**
	 * Returns a L2ItemInstance stored in database from its objectID
	 *
	 * @param ownerId :
	 *            int designating the objectID of the item
	 * @param rs
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(int ownerId, ResultSet rs)
	{
		L2ItemInstance inst = null;

		int objectId, item_id, count, loc_data, enchant_level, custom_type1, custom_type2, manaLeft;
		String data;
		ItemLocation loc;
		try
		{
			objectId = rs.getInt("object_id");
			item_id = rs.getInt("item_id");
			count = rs.getInt("count");
			loc = ItemLocation.valueOf(rs.getString("loc"));
			loc_data = rs.getInt("loc_data");
			enchant_level = rs.getInt("enchant_level");
			custom_type1 = rs.getInt("custom_type1");
			custom_type2 = rs.getInt("custom_type2");
			manaLeft = rs.getInt("mana_left");
			data = rs.getString("data");
		}
		catch (Exception e)
		{
			_log.fatal("Could not restore an item owned by " + ownerId + " from DB:" + e.getMessage(), e);
			return null;
		}
		L2Item item = ItemTable.getInstance().getTemplate(item_id);
		if (item == null)
		{
			_log.fatal("Item item_id=" + item_id + " not known, object_id=" + objectId);
			return null;
		}
		inst = new L2ItemInstance(objectId, item);
		inst._ownerId = ownerId;
		inst.setCount(count);
		inst._enchantLevel = enchant_level;
		inst._type1 = custom_type1;
		inst._type2 = custom_type2;
		inst._loc = loc;
		inst._locData = loc_data;
		inst._existsInDb = true;
		inst._storedInDb = true;
		inst._itemData = data;

		// Setup life time for shadow weapons
		inst._mana = manaLeft;

		// consume 1 mana
		if (inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
			inst.decreaseMana(false);

		// if mana left is 0 delete this item
		if (inst._mana == 0)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				inst.removeFromDb(con);
			}
			catch (SQLException e)
			{
				_log.warn("", e);
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
			return null;
		}
		else if (inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
			inst.scheduleConsumeManaTask();

		//load augmentation and elemental enchant
		if (inst.isEquipable())
			inst.restoreAttributes();

		return inst;
	}

	/**
	 * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion </li>
	 * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
	 * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li>
	 * <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li> _worldRegion == null <I>(L2Object is invisible at the beginning)</I></li>
	 * <BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Drop item</li>
	 * <li> Call Pet</li>
	 * <BR>
	 */
	public final void dropMe(L2Character dropper, int x, int y, int z)
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() == null;

		if (Config.GEODATA && dropper != null)
		{
			Location dropDest = GeoData.getInstance().moveCheck(dropper.getX(), dropper.getY(), dropper.getZ(), x, y, z, dropper.getInstanceId());
			x = dropDest.getX();
			y = dropDest.getY();
			z = dropDest.getZ();
		}

		if (dropper != null)
			setInstanceId(dropper.getInstanceId()); // Inherit instancezone when dropped in visible world
		else
			setInstanceId(0); // No dropper? Make it a global item...

		synchronized (this)
		{
			getPosition().setXYZ(x, y, z);
		}

		// Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion
		getPosition().getWorldRegion().addVisibleObject(this);
		setOwnerId(0);
		setDropTime(System.currentTimeMillis());

		// this can synchronize on others instancies, so it's out of
		// synchronized, to avoid deadlocks
		// Add the L2ItemInstance dropped in the world as a visible object
		L2World.getInstance().addVisibleObject(this, dropper);
		updateDatabase();
		if (Config.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance().save(this);
	}

	/**
	 * Update the database with values of the item
	 */
	private void updateInDb(Connection con)
	{
		if (_storedInDb)
			return;

		try
		{
			PreparedStatement statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,data=? WHERE object_id = ?");
			statement.setInt(1, _ownerId);
			statement.setInt(2, getCount());
			statement.setString(3, _loc.name());
			statement.setInt(4, _locData);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, getCustomType1());
			statement.setInt(7, getCustomType2());
			statement.setInt(8, getMana());
			statement.setString(9, _itemData);
			statement.setInt(10, getObjectId());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			statement.close();
		}
		catch (Exception e)
		{
			_log.fatal("Could not update item " + getObjectId(), e);
		}
	}

	/**
	 * Insert the item in database
	 */
	private void insertIntoDb(Connection con)
	{
		try
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,process,creator_id,first_owner_id,creation_time,data) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _ownerId);
			statement.setInt(2, _itemId);
			statement.setInt(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, getObjectId());
			statement.setInt(8, _type1);
			statement.setInt(9, _type2);
			statement.setInt(10, getMana());
			statement.setString(11, _process);
			statement.setInt(12, _creator);
			statement.setInt(13,_ownerId);
			statement.setLong(14, _creationTime);
			statement.setString(15, _itemData);
			
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			statement.close();
		}
		catch (Exception e)
		{
			_log.fatal("Could not insert item " + this);
			if(_ownerId!=0) {
				L2Character owner = L2World.getInstance().findCharacter(_ownerId);
				if(owner!=null)
					owner.getInventory().destroyItem("fail", this,null, null);
			}
		}
	}

	/**
	 * Delete item from database
	 */
	private void removeFromDb(Connection con)
	{
		_augmentation = null;
		if(!_existsInDb)
			return;
		try
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE object_id=?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			_existsInDb = false;
			_storedInDb = false;
			statement.close();

			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fatal("Could not delete item " + getObjectId(), e);
		}
	}

	/**
	 * Returns the item in String format
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		StringBuffer output = new StringBuffer();
		output.append("item " + getObjectId() + ": ");
		if (getEnchantLevel() > 0)
			output.append("+" + getEnchantLevel() + " ");
		output.append(getItem().getName());
		output.append("(" + getCount() + ")");
		output.append(" owner: "+getOwnerId()+", last process: "+_process);
		if(_creator!=0)
			output.append(", creator "+_creator);
		return output.toString();
	}

	public void resetOwnerTimer()
	{
		if (itemLootShedule != null)
			itemLootShedule.cancel(true);
		itemLootShedule = null;
	}

	public void setItemLootShedule(ScheduledFuture<?> sf)
	{
		itemLootShedule = sf;
	}

	public ScheduledFuture<?> getItemLootShedule()
	{
		return itemLootShedule;
	}

	public void setProtected(boolean is_protected)
	{
		_protected = is_protected;
	}

	public boolean isProtected()
	{
		return _protected;
	}

	public boolean isNightLure()
	{
		return ((_itemId >= 8505 && _itemId <= 8513) || _itemId == 8485);
	}

	public void setCountDecrease(boolean decrease)
	{
		_decrease = decrease;
	}

	public boolean getCountDecrease()
	{
		return _decrease;
	}

	public void setInitCount(int InitCount)
	{
		_initCount = InitCount;
	}

	public int getInitCount()
	{
		return _initCount;
	}

	public void restoreInitCount()
	{
		if (_decrease)
			setCount(_initCount);
	}

	public void setTime(int time)
	{
		_time = time > 0 ? time : 0;
	}

	public int getTime()
	{
		return _time;
	}

	public boolean isOlyRestrictedItem()
	{
		return (Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId) );
	}

	@Override
	public final String getFuncOwnerName()
	{
		return getItem().getFuncOwnerName();
	}

	@Override
	public final L2Skill getFuncOwnerSkill()
	{
		return getItem().getFuncOwnerSkill();
	}

	//L2EMU_ADD
	public boolean isCrystalScroll()
	{
		return ((_itemId >= 957 && _itemId <= 958) || //Crystal Scroll (Grade D)
				(_itemId >= 953 && _itemId <= 954) || //Crystal Scroll (Grade C)
				(_itemId >= 949 && _itemId <= 950) || //Crystal Scroll (Grade B)
				(_itemId >= 731 && _itemId <= 732) || //Crystal Scroll (Grade A)
				(_itemId >= 961 && _itemId <= 962)); //Crystal Scroll (Grade S)
	}

	public boolean isBracelet()
	{
		return ((_itemId >= 9589 && _itemId <= 9592) || (_itemId >= 10209 && _itemId <= 10210));
	}
	//L2EMU_ADD


	/**
	 * Remove a L2ItemInstance from the world and send server->client GetItem packets.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client Packet GetItem to player that pick up and its _knowPlayers member </li>
	 * <li>Remove the L2Object from the world</li>
	 * <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li> this instanceof L2ItemInstance</li>
	 * <li> _worldRegion != null <I>(L2Object is visible at the beginning)</I></li>
	 * <BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Do Pickup Item : PCInstance and Pet</li>
	 * <BR>
	 * <BR>
	 * 
	 * @param player Player that pick up the item
	 */
	public final void pickupMe(L2Character player) // NOTE: Should move this function into L2ItemInstance because it does not apply to L2Character
	{
		//if (Config.ASSERT)
		//	assert this instanceof L2ItemInstance;
		//if (Config.ASSERT)
		//	assert getPosition().getWorldRegion() != null;
		_rewardId = 0;
		L2WorldRegion oldregion = getPosition().getWorldRegion();
		resetOwnerTimer();
		// Create a server->client GetItem packet to pick up the L2ItemInstance
		player.broadcastPacket(new GetItem(this, player.getObjectId()));
		player.broadcastPacket(new DeleteObject(this));
		getPosition().clearWorldRegion();
		
		// if this item is a mercenary ticket, remove the spawns!
		ItemsOnGroundManager.getInstance().removeObject(this);
		
		final int itemId = getItemId();
		
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) > 0)
		{
			MercTicketManager.getInstance().removeTicket(this);
		}
		else if (itemId == 57 || itemId == 6353)
		{
			L2PcInstance pc = player.getActingPlayer();
			if (pc != null)
			{
				QuestState qs = pc.getQuestState("255_Tutorial");
				if (qs != null)
					qs.getQuest().notifyEvent("CE" + itemId + "", null, pc);
			}
		}
		
		// this can synchronize on others instancies, so it's out of
		// synchronized, to avoid deadlocks
		// Remove the L2ItemInstance from the world
		L2World.getInstance().removeVisibleObject(this, oldregion);
		setOwnerId(player.getObjectId());
	}
	
	public void setRewardId(int rewarder) {
		_rewardId = rewarder;
		_rewardTime = System.currentTimeMillis();
		
	}
	public boolean canPickup(L2PcInstance player) {
		if(_ownerId!=0)
			return false;
		if(_rewardId!=0 && _rewardId!=player.getObjectId()) {
			if(player.getParty()!=null)
				for(L2PcInstance pc : player.getParty().getPartyMembers() )
					if(_rewardId == pc.getObjectId()) {
						_rewardId = 0;
						return true;
					}
			if(System.currentTimeMillis() - _rewardTime < 15000) {
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return false;
			}
		} 
		_rewardId = 0;
		return true;
		
	}
	public void setData(String val) {
		_itemData = val;
		_storedInDb = false;
	}
	public String getData() { return _itemData; }
}
