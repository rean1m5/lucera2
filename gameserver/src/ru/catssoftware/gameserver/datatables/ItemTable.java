package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.xml.ExtractableItemsData;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.items.model.Item;
import ru.catssoftware.gameserver.model.L2Boss;
import ru.catssoftware.gameserver.model.L2CommandChannel;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.L2ItemInstance.ItemLocation;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.skills.SkillsEngine;
import ru.catssoftware.gameserver.templates.item.L2ArmorType;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.util.L2Collections;
import ru.catssoftware.util.StatsSet;


public final class ItemTable
{
	private static final Logger _log = Logger.getLogger(ItemTable.class);
	private static final Logger _logItems = Logger.getLogger("item");
	
	private static final Map<String, Integer> _materials = new HashMap<String, Integer>();
	private static final Map<String, Integer> _crystalTypes = new HashMap<String, Integer>();
	private static final Map<String, L2WeaponType> _weaponTypes = new HashMap<String, L2WeaponType>();
	private static final Map<String, L2ArmorType> _armorTypes = new HashMap<String, L2ArmorType>();
	private static final Map<String, Integer> _slots = new HashMap<String, Integer>();

	static
	{
		_materials.put("paper", L2Item.MATERIAL_PAPER);
		_materials.put("wood", L2Item.MATERIAL_WOOD);
		_materials.put("liquid", L2Item.MATERIAL_LIQUID);
		_materials.put("cloth", L2Item.MATERIAL_CLOTH);
		_materials.put("leather", L2Item.MATERIAL_LEATHER);
		_materials.put("horn", L2Item.MATERIAL_HORN);
		_materials.put("bone", L2Item.MATERIAL_BONE);
		_materials.put("bronze", L2Item.MATERIAL_BRONZE);
		_materials.put("fine_steel", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("cotton", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("mithril", L2Item.MATERIAL_MITHRIL);
		_materials.put("silver", L2Item.MATERIAL_SILVER);
		_materials.put("gold", L2Item.MATERIAL_GOLD);
		_materials.put("adamantaite", L2Item.MATERIAL_ADAMANTAITE);
		_materials.put("steel", L2Item.MATERIAL_STEEL);
		_materials.put("oriharukon", L2Item.MATERIAL_ORIHARUKON);
		_materials.put("blood_steel", L2Item.MATERIAL_BLOOD_STEEL);
		_materials.put("crystal", L2Item.MATERIAL_CRYSTAL);
		_materials.put("damascus", L2Item.MATERIAL_DAMASCUS);
		_materials.put("chrysolite", L2Item.MATERIAL_CHRYSOLITE);
		_materials.put("scale_of_dragon", L2Item.MATERIAL_SCALE_OF_DRAGON);
		_materials.put("dyestuff", L2Item.MATERIAL_DYESTUFF);
		_materials.put("cobweb", L2Item.MATERIAL_COBWEB);
		_materials.put("seed", L2Item.MATERIAL_SEED);

		_crystalTypes.put("s", L2Item.CRYSTAL_S);
		_crystalTypes.put("a", L2Item.CRYSTAL_A);
		_crystalTypes.put("b", L2Item.CRYSTAL_B);
		_crystalTypes.put("c", L2Item.CRYSTAL_C);
		_crystalTypes.put("d", L2Item.CRYSTAL_D);
		_crystalTypes.put("none", L2Item.CRYSTAL_NONE);

		_weaponTypes.put("blunt", L2WeaponType.BLUNT);
		_weaponTypes.put("bow", L2WeaponType.BOW);
		_weaponTypes.put("dagger", L2WeaponType.DAGGER);
		_weaponTypes.put("dual", L2WeaponType.DUAL);
		_weaponTypes.put("dualfist", L2WeaponType.DUALFIST);
		_weaponTypes.put("etc", L2WeaponType.ETC);
		_weaponTypes.put("fist", L2WeaponType.FIST);
		_weaponTypes.put("shield", L2WeaponType.NONE);
		_weaponTypes.put("none", L2WeaponType.NONE); // these are shields !
		_weaponTypes.put("pole", L2WeaponType.POLE);
		_weaponTypes.put("sword", L2WeaponType.SWORD);
		_weaponTypes.put("bigsword", L2WeaponType.BIGSWORD); //Two-Handed Swords
		_weaponTypes.put("pet", L2WeaponType.PET); //Pet Weapon
		_weaponTypes.put("rod", L2WeaponType.ROD); //Fishing Rods
		_weaponTypes.put("bigblunt", L2WeaponType.BIGBLUNT); //Two handed blunt

		_armorTypes.put("none", L2ArmorType.NONE);
		_armorTypes.put("light", L2ArmorType.LIGHT);
		_armorTypes.put("heavy", L2ArmorType.HEAVY);
		_armorTypes.put("magic", L2ArmorType.MAGIC);
		_armorTypes.put("pet", L2ArmorType.PET);

		_slots.put("shirt", L2Item.SLOT_UNDERWEAR);
		_slots.put("lbracelet", L2Item.SLOT_L_BRACELET);
		_slots.put("rbracelet", L2Item.SLOT_R_BRACELET);
		_slots.put("talisman", L2Item.SLOT_DECO);
		_slots.put("chest", L2Item.SLOT_CHEST);
		_slots.put("fullarmor", L2Item.SLOT_FULL_ARMOR);
		_slots.put("head", L2Item.SLOT_HEAD);
		_slots.put("hair", L2Item.SLOT_HAIR);
		_slots.put("face", L2Item.SLOT_FACE);
		_slots.put("dhair", L2Item.SLOT_HAIRALL);
		_slots.put("hairall", L2Item.SLOT_HAIRALL);
		_slots.put("underwear", L2Item.SLOT_UNDERWEAR);
		_slots.put("back", L2Item.SLOT_BACK);
		_slots.put("neck", L2Item.SLOT_NECK);
		_slots.put("legs", L2Item.SLOT_LEGS);
		_slots.put("feet", L2Item.SLOT_FEET);
		_slots.put("gloves", L2Item.SLOT_GLOVES);
		_slots.put("chest,legs", L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		_slots.put("belt", L2Item.SLOT_BELT);		
		_slots.put("rhand", L2Item.SLOT_R_HAND);
		_slots.put("lhand", L2Item.SLOT_L_HAND);
		_slots.put("lrhand", L2Item.SLOT_LR_HAND);
		_slots.put("rear,lear", L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		_slots.put("rfinger,lfinger", L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		_slots.put("wolf", L2Item.SLOT_WOLF);
		_slots.put("greatwolf", L2Item.SLOT_GREATWOLF);
		_slots.put("hatchling", L2Item.SLOT_HATCHLING);
		_slots.put("strider", L2Item.SLOT_STRIDER);
		_slots.put("babypet", L2Item.SLOT_BABYPET);
		_slots.put("greatwolf", L2Item.SLOT_GREATWOLF);
		_slots.put("none", L2Item.SLOT_NONE);
	}


	/** Table of SQL request in order to obtain items from tables [etcitem], [armor], [weapon] */
	private static final String[] SQL_ITEM_SELECTS = {
		"SELECT item_id, name, crystallizable, item_type, weight, consume_type, material, crystal_type,"
			+ " duration, lifetime, price, crystal_count, sellable, dropable, destroyable, tradeable, skill FROM etcitem",
		
		"SELECT item_id, name, bodypart, crystallizable, armor_type, weight,"
			+ " material, crystal_type, avoid_modify, duration, lifetime, p_def, m_def, mp_bonus,"
			+ " price, crystal_count, sellable, dropable, destroyable, tradeable, skills_item FROM armor",
		
		"SELECT item_id, name, bodypart, crystallizable, weight, soulshots, spiritshots,"
			+ " material, crystal_type, p_dam, rnd_dam, weaponType, critical, hit_modify, avoid_modify,"
			+ " shield_def, shield_def_rate, atk_speed, mp_consume, m_dam, duration, lifetime, price, crystal_count,"
			+ " sellable,  dropable, destroyable, tradeable, skills_item, skills_enchant4,"
			+ " skills_onCast, skills_onCrit, change_weaponId FROM weapon" };
	
	private static final String[] SQL_CUSTOM_ITEM_SELECTS = {
		"SELECT item_id, item_display_id, name, crystallizable, item_type, weight, consume_type, material, crystal_type,"
			+ " duration, lifetime, price, crystal_count, sellable, dropable, destroyable, tradeable, skill FROM custom_etcitem",
		
		"SELECT item_id, item_display_id, name, bodypart, crystallizable, armor_type, weight,"
			+ " material, crystal_type, avoid_modify, duration, lifetime, p_def, m_def, mp_bonus,"
			+ " price, crystal_count, sellable, dropable, destroyable, tradeable, skills_item FROM custom_armor",
		
		"SELECT item_id, item_display_id, name, bodypart, crystallizable, weight, soulshots, spiritshots,"
			+ " material, crystal_type, p_dam, rnd_dam, weaponType, critical, hit_modify, avoid_modify,"
			+ " shield_def, shield_def_rate, atk_speed, mp_consume, m_dam, duration, lifetime, price, crystal_count,"
			+ " sellable,  dropable, destroyable, tradeable, skills_item, skills_enchant4,"
			+ " skills_onCast, skills_onCrit, change_weaponId FROM custom_weapon" };
	
	private static ItemTable _instance;
	public static ItemTable getInstance()
	{
		if (_instance == null)
			_instance = new ItemTable();

		return _instance;
	}

	public static void reload()
	{
		_instance = new ItemTable();
		ExtractableItemsData.reload();
	}
	public Collection <Integer> getAllWeaponsId()
	{
		return weaponData.keySet();
	}
	public Collection <Integer> getAllArmorsId()
	{
		return armorData.keySet();
	}
	
	private final L2Item[] _allTemplates;
	private final Map<Integer, Item> armorData = new HashMap<Integer, Item>();
	private final Map<Integer, Item> weaponData = new HashMap<Integer, Item>();

	private ItemTable()
	{
		final Map<Integer, Item> itemData = new HashMap<Integer, Item>();

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			_log.info("Items: Cleanup items table");
			PreparedStatement stm = con.prepareStatement("delete from items where first_owner_id=0 OR first_owner_id is null");
			int rows = stm.executeUpdate(); 
			if(rows>0) 
				_log.warn("Items: Deleted "+rows+" incorrect created items");
			stm.close(); 
			for (String selectQuery : SQL_ITEM_SELECTS)
			{
				PreparedStatement statement = con.prepareStatement(selectQuery);
				ResultSet rset = statement.executeQuery();

				while (rset.next())
				{
					if (selectQuery.endsWith("etcitem"))
					{
						Item newItem = readItem(rset, false);
						itemData.put(newItem.id, newItem);
					}
					else if (selectQuery.endsWith("armor"))
					{
						Item newItem = readArmor(rset, false);
						armorData.put(newItem.id, newItem);
					}
					else if (selectQuery.endsWith("weapon"))
					{
						Item newItem = readWeapon(rset, false);
						weaponData.put(newItem.id, newItem);
					}
				}
				rset.close();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.warn("data error on item: ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}

		con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			for (String selectQuery : SQL_CUSTOM_ITEM_SELECTS)
			{
				PreparedStatement statement = con.prepareStatement(selectQuery);
				ResultSet rset = statement.executeQuery();

				while (rset.next())
				{
					if (selectQuery.endsWith("etcitem"))
					{
						Item newItem = readItem(rset, true);
						itemData.put(newItem.id, newItem);
					}
					else if (selectQuery.endsWith("armor"))
					{
						Item newItem = readArmor(rset, true);
						armorData.put(newItem.id, newItem);
					}
					else if (selectQuery.endsWith("weapon"))
					{
						Item newItem = readWeapon(rset, true);
						weaponData.put(newItem.id, newItem);
					}
				}
				rset.close();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.warn("data error on custom item: ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}

		final List<L2Item> etcItems = SkillsEngine.loadItems(itemData);
		_log.info("ItemTable: Loaded " + etcItems.size() + "/" + etcItems.size() + " Items.");
		
		final List<L2Item> armors = SkillsEngine.loadArmors(armorData);
		_log.info("ItemTable: Loaded " + armors.size() + "/" + armorData.size() + " Armors.");
		
		final List<L2Item> weapons = SkillsEngine.loadWeapons(weaponData);
		_log.info("ItemTable: Loaded " + weapons.size() + "/" + weaponData.size() + " Weapons.");
		
		int highestId = 0;
		for (L2Item item : L2Collections.concatenatedIterable(armors, etcItems, weapons))
			if (highestId < item.getItemId())
				highestId = item.getItemId();
		
		_allTemplates = new L2Item[highestId + 1];
		
		for (L2Item item : L2Collections.concatenatedIterable(armors, etcItems, weapons))
		{
			L2Item old = _allTemplates[item.getItemId()];
			
			_allTemplates[item.getItemId()] = item;
			
			if (old != null)
				_log.fatal("ItemTable: ID: " + old.getItemId() + " (" + old.getItemType() + " replaced with " + item.getItemType() + ")");
		}
	}
	/**
	 * Returns object Item from the record of the database
	 * 
	 * @param rset : ResultSet designating a record of the [weapon] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readWeapon(ResultSet rset, boolean custom) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.type = _weaponTypes.get(rset.getString("weaponType"));
		item.id = rset.getInt("item_id");
		item.displayid = custom ? rset.getInt("item_display_id") : item.id;
		item.name = rset.getString("name");

		item.set.set("item_id", item.id);
		item.set.set("item_display_id", item.displayid);
		item.set.set("name", item.name);

		// lets see if this is a shield
		if (item.type == L2WeaponType.NONE)
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
			item.set.set("type2", L2Item.TYPE2_SHIELD_ARMOR);
		}
		else
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			item.set.set("type2", L2Item.TYPE2_WEAPON);
		}
		item.set.set("bodypart", _slots.get(rset.getString("bodypart")));
		item.set.set("material", _materials.get(rset.getString("material")));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("weight", rset.getInt("weight"));
		item.set.set("soulshots", rset.getInt("soulshots"));
		item.set.set("spiritshots", rset.getInt("spiritshots"));
		item.set.set("p_dam", rset.getInt("p_dam"));
		item.set.set("rnd_dam", rset.getInt("rnd_dam"));
		item.set.set("critical", rset.getInt("critical"));
		item.set.set("hit_modify", rset.getDouble("hit_modify"));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("shield_def", rset.getInt("shield_def"));
		item.set.set("shield_def_rate", rset.getInt("shield_def_rate"));
		item.set.set("atk_speed", rset.getInt("atk_speed"));
		item.set.set("mp_consume", rset.getInt("mp_consume"));
		item.set.set("m_dam", rset.getInt("m_dam"));
		item.set.set("duration", rset.getInt("duration"));
		item.set.set("lifetime",rset.getInt("lifetime"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("dropable", Boolean.valueOf(rset.getString("dropable")));
		item.set.set("destroyable", Boolean.valueOf(rset.getString("destroyable")));
		item.set.set("tradeable", Boolean.valueOf(rset.getString("tradeable")));

		item.set.set("skills_item", rset.getString("skills_item"));
		item.set.set("skills_enchant4", rset.getString("skills_enchant4"));
		item.set.set("skills_onCast", rset.getString("skills_onCast"));
		item.set.set("skills_onCrit", rset.getString("skills_onCrit"));
		item.set.set("change_weaponId", rset.getInt("change_weaponId"));

		if (item.type == L2WeaponType.PET)
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			if (item.set.getInteger("bodypart") == L2Item.SLOT_WOLF)
				item.set.set("type2", L2Item.TYPE2_PET_WOLF);
			else if (item.set.getInteger("bodypart") == L2Item.SLOT_GREATWOLF)
				item.set.set("type2", L2Item.TYPE2_PET_EVOLVEDWOLF);
			else if (item.set.getInteger("bodypart") == L2Item.SLOT_HATCHLING)
				item.set.set("type2", L2Item.TYPE2_PET_HATCHLING);
			else if (item.set.getInteger("bodypart") == L2Item.SLOT_BABYPET)
				item.set.set("type2", L2Item.TYPE2_PET_BABY);
			else
				item.set.set("type2", L2Item.TYPE2_PET_STRIDER);

			item.set.set("bodypart", L2Item.SLOT_R_HAND);
		}

		return item;
	}

	/**
	 * Returns object Item from the record of the database
	 * @param rset : ResultSet designating a record of the [armor] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readArmor(ResultSet rset, boolean custom) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.type = _armorTypes.get(rset.getString("armor_type"));
		item.id = rset.getInt("item_id");
		item.displayid = custom ? rset.getInt("item_display_id") : item.id;
		item.name = rset.getString("name");

		item.set.set("item_id", item.id);
		item.set.set("item_display_id", item.displayid);
		item.set.set("name", item.name);
		int bodypart = _slots.get(rset.getString("bodypart"));
		item.set.set("bodypart", bodypart);
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("dropable", Boolean.valueOf(rset.getString("dropable")));
		item.set.set("destroyable", Boolean.valueOf(rset.getString("destroyable")));
		item.set.set("tradeable", Boolean.valueOf(rset.getString("tradeable")));

		item.set.set("skills_item", rset.getString("skills_item"));

		if (bodypart == L2Item.SLOT_NECK || bodypart == L2Item.SLOT_HAIR || bodypart == L2Item.SLOT_FACE
			|| bodypart == L2Item.SLOT_HAIRALL || (bodypart & L2Item.SLOT_L_EAR) != 0
			|| (bodypart & L2Item.SLOT_L_FINGER) != 0
			|| (bodypart & L2Item.SLOT_R_BRACELET) != 0
			|| (bodypart & L2Item.SLOT_L_BRACELET) != 0 )
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			item.set.set("type2", L2Item.TYPE2_ACCESSORY);
		}
		else
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
			item.set.set("type2", L2Item.TYPE2_SHIELD_ARMOR);
		}

		item.set.set("weight", rset.getInt("weight"));
		item.set.set("material", _materials.get(rset.getString("material")));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("duration", rset.getInt("duration"));
		item.set.set("lifetime",rset.getInt("lifetime"));
		item.set.set("p_def", rset.getInt("p_def"));
		item.set.set("m_def", rset.getInt("m_def"));
		item.set.set("mp_bonus", rset.getInt("mp_bonus"));
		item.set.set("price", rset.getInt("price"));

		if (item.type == L2ArmorType.PET)
		{
			if (bodypart == L2Item.SLOT_NECK)
			{
				item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
				item.set.set("type2", L2Item.TYPE2_ACCESSORY);
				item.set.set("bodypart", L2Item.SLOT_NECK);
			}
			else
			{
				item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
				switch (item.set.getInteger("bodypart"))
				{
				case L2Item.SLOT_WOLF:
					item.set.set("type2", L2Item.TYPE2_PET_WOLF);
					break;
				case L2Item.SLOT_GREATWOLF:
					item.set.set("type2", L2Item.TYPE2_PET_EVOLVEDWOLF);
					break;
				case L2Item.SLOT_HATCHLING:
					item.set.set("type2", L2Item.TYPE2_PET_HATCHLING);
					break;
				case L2Item.SLOT_BABYPET:
					item.set.set("type2", L2Item.TYPE2_PET_BABY);
					break;
				default:
					item.set.set("type2", L2Item.TYPE2_PET_STRIDER);
					break;
				}
				item.set.set("bodypart", L2Item.SLOT_CHEST);
			}
		}

		return item;
	}

	/**
	 * Returns object Item from the record of the database
	 * @param rset : ResultSet designating a record of the [etcitem] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readItem(ResultSet rset, boolean custom) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.id = rset.getInt("item_id");
		item.displayid = custom ? rset.getInt("item_display_id") : item.id;

		item.set.set("item_id", item.id);
		item.set.set("item_display_id", item.displayid);
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("type1", L2Item.TYPE1_ITEM_QUESTITEM_ADENA);
		item.set.set("type2", L2Item.TYPE2_OTHER);
		item.set.set("bodypart", 0);
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("dropable", Boolean.valueOf(rset.getString("dropable")));
		item.set.set("destroyable", Boolean.valueOf(rset.getString("destroyable")));
		item.set.set("tradeable", Boolean.valueOf(rset.getString("tradeable")));
		item.set.set("skill", rset.getString("skill"));

		String itemType = rset.getString("item_type");
		if (itemType.equals("none"))
			item.type = L2EtcItemType.OTHER; // only for default
		else if (itemType.equals("castle_guard"))
			item.type = L2EtcItemType.SCROLL; // dummy
		else if (itemType.equals("material"))
			item.type = L2EtcItemType.MATERIAL;
		else if (itemType.equals("pet_collar"))
			item.type = L2EtcItemType.PET_COLLAR;
		else if (itemType.equals("potion"))
			item.type = L2EtcItemType.POTION;
		else if (itemType.equals("recipe"))
			item.type = L2EtcItemType.RECEIPE;
		else if (itemType.equals("scroll"))
			item.type = L2EtcItemType.SCROLL;
		else if (itemType.equals("seed"))
			item.type = L2EtcItemType.SEED;
		else if (itemType.equals("shot"))
			item.type = L2EtcItemType.SHOT;
		else if (itemType.equals("spellbook"))
			item.type = L2EtcItemType.SPELLBOOK; // Spellbook, Amulet, Blueprint
		else if (itemType.equals("herb"))
			item.type = L2EtcItemType.HERB;
		else if (itemType.equals("arrow"))
		{
			item.type = L2EtcItemType.ARROW;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if (itemType.equals("bolt"))
		{
			item.type = L2EtcItemType.BOLT;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if (itemType.equals("quest"))
		{
			item.type = L2EtcItemType.QUEST;
			item.set.set("type2", L2Item.TYPE2_QUEST);
		}
		else if (itemType.equals("lure"))
		{
			item.type = L2EtcItemType.OTHER;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else
		{
			_log.debug("unknown etcitem type:" + itemType);
			item.type = L2EtcItemType.OTHER;
		}

		String consume = rset.getString("consume_type");
		if (consume.equals("asset"))
		{
			item.type = L2EtcItemType.MONEY;
			item.set.set("stackable", true);
			item.set.set("type2", L2Item.TYPE2_MONEY);
		}
		else if (consume.equals("stackable"))
		{
			item.set.set("stackable", true);
		}
		else
		{
			item.set.set("stackable", false);
		}

		int material = _materials.get(rset.getString("material"));
		item.set.set("material", material);

		int crystal = _crystalTypes.get(rset.getString("crystal_type"));
		item.set.set("crystal_type", crystal);

		int weight = rset.getInt("weight");
		item.set.set("weight", weight);
		item.name = rset.getString("name");
		item.set.set("name", item.name);

		item.set.set("duration", rset.getInt("duration"));
		item.set.set("lifetime",rset.getInt("lifetime"));
		item.set.set("price", rset.getInt("price"));

		return item;
	}

	/**

	 * Returns the item corresponding to the item ID
	 * @param id : int designating the item
	 * @return L2Item
	 */
	public L2Item getTemplate(int id)
	{
		if (id >= _allTemplates.length)
			return null;
		return _allTemplates[id];
	}
	
	public String getItemName(int id) {
		L2Item item = getTemplate(id);
		if(item==null)
			return "NoName";
		else 
			return  item.getName();
	}
	/**
	 * Create the L2ItemInstance corresponding to the Item Identifier and quantitiy add logs the activity.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create and Init the L2ItemInstance corresponding to the Item Identifier and quantity </li>
	 * <li>Add the L2ItemInstance object to _allObjects of L2world </li>
	 * <li>Logs Item creation according to log settings</li>
	 * <BR>
	 * <BR>
	 * 
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be created
	 * @param count : int Quantity of items to be created for stackable items
	 * @param actor : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in
	 *            transformation
	 * @return L2ItemInstance corresponding to the new item
	 */
	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		// Create and Init the L2ItemInstance corresponding to the Item Identifier
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		if(GameExtensionManager.getInstance().handleAction(item, Action.ITEM_CREATE, process,actor,reference)!=null)
			return null;
		if (process.equalsIgnoreCase("loot") &&  !actor.isAutoLootEnabled())
		{
			long delay;
			// if in CommandChannel and was killing a World/RaidBoss
			if (reference instanceof L2Boss)
			{
				L2CommandChannel commandChannel = ((L2Boss)reference).getFirstCommandChannelAttacked();
				
				if (commandChannel != null && commandChannel.meetRaidWarCondition(reference))
				{
					item.setOwnerId(process,commandChannel.getChannelLeader().getObjectId(),actor,reference);
					delay = 300000;
				}
				else
				{
					item.setOwnerId(process,actor.getObjectId(),actor,reference);
					delay = 15000;
				}
			}
			else
			{
				item.setOwnerId(process,actor.getObjectId(),actor,reference);
				delay = 15000;
			}
			
			item.setItemLootShedule(ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), delay));
		}

		// Add the L2ItemInstance object to _allObjects of L2world
		L2World.getInstance().storeObject(item);

		// Set Item parameters
		if (item.isStackable() && count > 1)
			item.setCount(count);

		if (Config.LOG_ITEMS && !process.equals("Reset") && !Config.IGNORE_LOG.contains(process.toUpperCase()))
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CREATE:" + process);
			param.add(item);
			param.add(actor);
			param.add(reference);
			_logItems.info(param);
		}
		return item;
	}

	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor)
	{
		return createItem(process, itemId, count, actor, null);
	}

	/**
	 * Returns a dummy (fr = factice) item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * Dummy item is created by setting the ID of the object in the world at null value
	 * @param itemId : int designating the item
	 * @return L2ItemInstance designating the dummy item created
	 */
	
	public L2ItemInstance createDummyItem(int itemId)
	{
		L2Item item = getTemplate(itemId);
		if (item == null)
			return null;
		L2ItemInstance temp = new L2ItemInstance(0, item);
		try
		{
			temp = new L2ItemInstance(0, itemId, itemId);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			// this can happen if the item templates were not initialized
		}
		return temp;
	}

	/**
	 * Destroys the L2ItemInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Sets L2ItemInstance parameters to be unusable </li>
	 * <li>Removes the L2ItemInstance object to _allObjects of L2world </li>
	 * <li>Logs Item delettion according to log settings</li><BR><BR>
	 *
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be created
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		synchronized (item)
		{
			item.setCount(0);
			item.setOwnerId("destroy",0,null,null);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);

			L2World.getInstance().removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());

			if (process!=null && Config.LOG_ITEMS && !Config.IGNORE_LOG.contains(process.toUpperCase()))
			{
				List<Object> param = new ArrayList<Object>();
				param.add("DELETE:" + process);
				param.add(item);
				param.add(actor);
				param.add(reference);
				_logItems.info(param);
			}

			// if it's a pet control item, delete the pet as well
			if (PetDataTable.isPetItem(item.getItemId()))
			{
				Connection con = null;
				try
				{
					// Delete the pet in db
					con = L2DatabaseFactory.getInstance().getConnection(con);
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
					statement.setInt(1, item.getObjectId());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warn("could not delete pet objectid:", e);
				}
				finally
				{
					L2DatabaseFactory.close(con);
				}
			}

			// delete augmentation data
			item.removeAugmentation();
		}
	}
	
	private static final class ResetOwner implements Runnable
	{
		private final L2ItemInstance _item;
		
		private ResetOwner(L2ItemInstance item)
		{
			_item = item;
		}
		
		public void run()
		{
			_item.setOwnerId("reset",0,null,null);
			_item.setItemLootShedule(null);
		}
	}

	public L2Item [] getAllItems() {
		// TODO Auto-generated method stub
		return _allTemplates;
	}
}
