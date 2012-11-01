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
package ru.catssoftware.gameserver.templates.item;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.conditions.Condition;
import ru.catssoftware.gameserver.skills.funcs.Func;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;
import ru.catssoftware.gameserver.skills.funcs.FuncTemplate;
import ru.catssoftware.util.ArrayUtils;
import ru.catssoftware.util.LinkedBunch;
import ru.catssoftware.util.StatsSet;

import java.util.Arrays;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI>
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public abstract class L2Item implements FuncOwner
{
	public static final int				TYPE1_WEAPON_RING_EARRING_NECKLACE	= 0;
	public static final int				TYPE1_SHIELD_ARMOR					= 1;
	public static final int				TYPE1_ITEM_QUESTITEM_ADENA			= 4;

	public static final int				TYPE2_WEAPON						= 0;
	public static final int				TYPE2_SHIELD_ARMOR					= 1;
	public static final int				TYPE2_ACCESSORY						= 2;
	public static final int				TYPE2_QUEST							= 3;
	public static final int				TYPE2_MONEY							= 4;
	public static final int				TYPE2_OTHER							= 5;
	public static final int				TYPE2_PET_WOLF						= 6;
	public static final int				TYPE2_PET_HATCHLING					= 7;
	public static final int				TYPE2_PET_STRIDER					= 8;
	public static final int				TYPE2_PET_BABY						= 9;
	public static final int				TYPE2_PET_EVOLVEDWOLF				= 10;

	public static final int				SLOT_NONE							= 0x0000;
	public static final int				SLOT_UNDERWEAR						= 0x0001;
	public static final int				SLOT_R_EAR							= 0x0002;
	public static final int				SLOT_L_EAR							= 0x0004;
	public static final int				SLOT_LR_EAR							= 0x00006;
	public static final int				SLOT_NECK							= 0x0008;
	public static final int				SLOT_R_FINGER						= 0x0010;
	public static final int				SLOT_L_FINGER						= 0x0020;
	public static final int				SLOT_LR_FINGER						= 0x0030;
	public static final int				SLOT_HEAD							= 0x0040;
	public static final int				SLOT_R_HAND							= 0x0080;
	public static final int				SLOT_L_HAND							= 0x0100;
	public static final int				SLOT_GLOVES							= 0x0200;
	public static final int				SLOT_CHEST							= 0x0400;
	public static final int				SLOT_LEGS							= 0x0800;
	public static final int				SLOT_FEET							= 0x1000;
	public static final int				SLOT_BACK							= 0x2000;
	public static final int				SLOT_LR_HAND						= 0x4000;
	public static final int				SLOT_FULL_ARMOR						= 0x8000;
	public static final int				SLOT_HAIR							= 0x010000;
	public static final int				SLOT_ALLDRESS						= 0x020000;
	public static final int				SLOT_FACE							= 0x040000;
	public static final int				SLOT_HAIRALL						= 0x080000;
	public static final int				SLOT_R_BRACELET						= 0x100000;
	public static final int				SLOT_L_BRACELET						= 0x200000;
	public static final int				SLOT_DECO							= 0x400000;
	public static final int 			SLOT_BELT 							= 0x10000000;	
	public static final int				SLOT_WOLF							= -100;
	public static final int				SLOT_HATCHLING						= -101;
	public static final int				SLOT_STRIDER						= -102;
	public static final int				SLOT_BABYPET						= -103;
	public static final int				SLOT_GREATWOLF						= -104;

	public static final int				MATERIAL_STEEL						= 0x00;									// ??
	public static final int				MATERIAL_FINE_STEEL					= 0x01;									// ??
	public static final int				MATERIAL_BLOOD_STEEL				= 0x02;									// ??
	public static final int				MATERIAL_BRONZE						= 0x03;									// ??
	public static final int				MATERIAL_SILVER						= 0x04;									// ??
	public static final int				MATERIAL_GOLD						= 0x05;									// ??
	public static final int				MATERIAL_MITHRIL					= 0x06;									// ??
	public static final int				MATERIAL_ORIHARUKON					= 0x07;									// ??
	public static final int				MATERIAL_PAPER						= 0x08;									// ??
	public static final int				MATERIAL_WOOD						= 0x09;									// ??
	public static final int				MATERIAL_CLOTH						= 0x0a;									// ??
	public static final int				MATERIAL_LEATHER					= 0x0b;									// ??
	public static final int				MATERIAL_BONE						= 0x0c;									// ??
	public static final int				MATERIAL_HORN						= 0x0d;									// ??
	public static final int				MATERIAL_DAMASCUS					= 0x0e;									// ??
	public static final int				MATERIAL_ADAMANTAITE				= 0x0f;									// ??
	public static final int				MATERIAL_CHRYSOLITE					= 0x10;									// ??
	public static final int				MATERIAL_CRYSTAL					= 0x11;									// ??
	public static final int				MATERIAL_LIQUID						= 0x12;									// ??
	public static final int				MATERIAL_SCALE_OF_DRAGON			= 0x13;									// ??
	public static final int				MATERIAL_DYESTUFF					= 0x14;									// ??
	public static final int				MATERIAL_COBWEB						= 0x15;									// ??
	public static final int				MATERIAL_SEED						= 0x15;									// ??

	public static final int				CRYSTAL_NONE						= 0x00;									// ??
	public static final int				CRYSTAL_D							= 0x01;									// ??
	public static final int				CRYSTAL_C							= 0x02;									// ??
	public static final int				CRYSTAL_B							= 0x03;									// ??
	public static final int				CRYSTAL_A							= 0x04;									// ??
	public static final int				CRYSTAL_S							= 0x05;									// ??

	private static final int[]			crystalItemId						=
																			{ 0, 1458, 1459, 1460, 1461, 1462 };
	private static final int[]			crystalEnchantBonusArmor			=
																			{ 0, 11, 6, 11, 19, 25 };
	private static final int[]			crystalEnchantBonusWeapon			=
																			{ 0, 90, 45, 67, 144, 250 };

	private final int					_itemId;
	private final int					_itemDisplayId;
	private final String				_name;
	private final int					_type1;																		// needed for item list (inventory)
	private final int					_type2;																		// different lists for armor, weapon, etc
	private final int					_weight;
	private final boolean				_crystallizable;
	private final boolean				_stackable;
	private final int					_materialType;
	private final int					_crystalType;																	// default to none-grade
	private final int					_duration;
	private final int					_lifetime;		
	private final int					_bodyPart;
	private final int					_referencePrice;
	private final int					_crystalCount;
	private final boolean				_sellable;
	private final boolean				_dropable;
	private final boolean				_destroyable;
	private final boolean				_tradeable;
	private final boolean				_isCommonItem;

	protected final AbstractL2ItemType	_type;

	private FuncTemplate[] _funcTemplates;
	private Condition[] _preConditions = Condition.EMPTY_ARRAY;

	/**
	 * Constructor of the L2Item that fill class variables.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>type</LI>
	 * <LI>_itemId</LI>
	 * <LI>_name</LI>
	 * <LI>_type1 & _type2</LI>
	 * <LI>_weight</LI>
	 * <LI>_crystallizable</LI>
	 * <LI>_stackable</LI>
	 * <LI>_materialType & _crystalType & _crystlaCount</LI>
	 * <LI>_duration</LI>
	 * <LI>_bodypart</LI>
	 * <LI>_referencePrice</LI>
	 * <LI>_sellable</LI>
	 * @param type : Enum designating the type of the item
	 * @param set : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected L2Item(AbstractL2ItemType type, StatsSet set)
	{
		_type = type;
		_itemId = set.getInteger("item_id");
		_itemDisplayId = set.getInteger("item_display_id");
		_name = set.getString("name");
		_type1 = set.getInteger("type1"); // needed for item list (inventory)
		_type2 = set.getInteger("type2"); // different lists for armor, weapon, etc
		_weight = set.getInteger("weight");
		_crystallizable = set.getBool("crystallizable");
		_stackable = set.getBool("stackable", false);
		_materialType = set.getInteger("material");
		_crystalType = set.getInteger("crystal_type", CRYSTAL_NONE); // default to none-grade
		_duration = set.getInteger("duration");
		_bodyPart = set.getInteger("bodypart");
		_referencePrice = set.getInteger("price");
		_lifetime = set.getInteger("lifetime");
		_crystalCount = set.getInteger("crystal_count", 0);
		_sellable = set.getBool("sellable", true);
		_dropable = set.getBool("dropable", true);
		_destroyable = set.getBool("destroyable", true);
		_tradeable = set.getBool("tradeable", true);
		_isCommonItem = _name.startsWith("Common Item") || _name.startsWith("Standard Item");
	}

	public Condition[] getConditions() { return _preConditions; }
	/**
	 * Returns the itemType.
	 * @return AbstractL2ItemType
	 */
	public AbstractL2ItemType getItemType()
	{
		return _type;
	}

	/**
	 * Returns the duration of the item
	 * @return int
	 */
	public final int getDuration()
	{
		return _duration;
	}

	public final int getLifetime() {
		return _lifetime;
	}
	/**
	 * Returns the ID of the iden
	 * @return int
	 */
	public final int getItemId()
	{
		return _itemId;
	}

	public final int getItemDisplayId()
	{
		return _itemDisplayId;
	}

	public abstract int getItemMask();

	/**
	 * Return the type of material of the item
	 * @return int
	 */
	public final int getMaterialType()
	{
		return _materialType;
	}

	/**
	 * Returns the type 2 of the item
	 * @return int
	 */
	public final int getType2()
	{
		return _type2;
	}

	/**
	 * Returns the weight of the item
	 * @return int
	 */
	public final int getWeight()
	{
		return _weight;
	}

	/**
	 * Returns if the item is crystallizable
	 * @return boolean
	 */
	public final boolean isCrystallizable()
	{
		return _crystallizable;
	}

	/**
	 * Return the type of crystal if item is crystallizable
	 * @return int
	 */
	public final int getCrystalType()
	{
		return _crystalType;
	}

	/**
	 * Return the type of crystal if item is crystallizable
	 * @return int
	 */
	public final int getCrystalItemId()
	{
		return crystalItemId[_crystalType];
	}

	/**
	 * Returns the grade of the item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * In fact, this fucntion returns the type of crystal of the item.
	 * @return int
	 */
	public final int getItemGrade()
	{
		return getCrystalType();
	}

	/**
	 * Returns the quantity of crystals for crystallization
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}

	/**
	 * Returns the quantity of crystals for crystallization on specific enchant level
	 * @return int
	 */
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
			case TYPE2_SHIELD_ARMOR:
			case TYPE2_ACCESSORY:
				return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * (3 * enchantLevel - 6);
			case TYPE2_WEAPON:
				return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * (2 * enchantLevel - 3);
			default:
				return _crystalCount;
			}
		}
		else if (enchantLevel > 0)
		{
			switch (_type2)
			{
			case TYPE2_SHIELD_ARMOR:
			case TYPE2_ACCESSORY:
				return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * enchantLevel;
			case TYPE2_WEAPON:
				return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * enchantLevel;
			default:
				return _crystalCount;
			}
		}
		else
			return _crystalCount;
	}

	/**
	 * Returns the name of the item
	 * @return String
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * Return the part of the body used with the item.
	 * @return int
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}

	/**
	 * Returns the type 1 of the item
	 * @return int
	 */
	public final int getType1()
	{
		return _type1;
	}

	/**
	 * Returns if the item is stackable
	 * @return boolean
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}

	/**
	 * Returns if the item is consumable
	 * @return boolean
	 */
	public boolean isConsumable()
	{
		return false;
	}

	/**
	 * Returns if the item is a heroitem
	 * @return boolean
	 */
	public boolean isHeroItem()
	{
		return ((_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842);
	}

	public boolean isCommonItem()
	{
		return _isCommonItem;
	}

	public boolean isLifeStone()
	{
		return !(_itemId < 8723 || (_itemId > 8762 && _itemId < 9573) || (_itemId > 9576 && _itemId < 10483) || _itemId > 10486);
	}
	public boolean isAccLifeStone()
	{
		return !(_itemId < 12754 || (_itemId > 12763 && _itemId < 12821) || _itemId > 12822);
	}
	public boolean isEquipable()
	{
		return getBodyPart() != 0 && !(getItemType() instanceof L2EtcItemType);
	}

	/**
	 * Returns the price of reference of the item
	 * @return int
	 */
	public final int getReferencePrice()
	{
		return (isConsumable() ? (int) (_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice);
	}

	/**
	 * Returns if the item can be sold
	 * @return boolean
	 */
	public final boolean isSellable()
	{
		return _sellable;
	}

	/**
	 * Returns if the item can dropped
	 * @return boolean
	 */
	public final boolean isDropable()
	{
		return _dropable;
	}

	/**
	 * Returns if the item can destroy
	 * @return boolean
	 */
	public final boolean isDestroyable()
	{
		return _destroyable;
	}

	/**
	 * Returns if the item can add to trade
	 * @return boolean
	 */
	public final boolean isTradeable()
	{
		return _tradeable;
	}

	/**
	 * Returns if item is for hatchling
	 * @return boolean
	 */
	public boolean isForHatchling()
	{
		return (_type2 == TYPE2_PET_HATCHLING);
	}

	/**
	 * Returns if item is for strider
	 * @return boolean
	 */
	public boolean isForStrider()
	{
		return (_type2 == TYPE2_PET_STRIDER);
	}

	/**
	 * Returns if item is for wolf
	 * @return boolean
	 */
	public boolean isForWolf()
	{
		return (_type2 == TYPE2_PET_WOLF);
	}

	/**
	 * Returns if item is for Great wolf
	 * @return boolean
	 */
	public boolean isForEvolvedWolf()
	{
		return (_type2 == TYPE2_PET_EVOLVEDWOLF);
	}

	/**
	 * Returns if item is for wolf
	 * @return boolean
	 */
	public boolean isForBabyPet()
	{
		return (_type2 == TYPE2_PET_BABY);
	}

	/**
	 * Returns array of Func objects containing the list of functions used by the item
	 * @param instance : L2ItemInstance pointing out the item
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	public FuncTemplate [] getFuncTemplates() {
		return _funcTemplates;
	}
	public  void addFuncTemplate(FuncTemplate f) {
		_funcTemplates = ArrayUtils.add(_funcTemplates, f);
	}
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates == null)
			return Func.EMPTY_ARRAY;

		LinkedBunch<Func> funcs = new LinkedBunch<Func>();
		for (FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.target = player;
			env.item = instance;
			Func f = t.getFunc(env, instance);
			if (f != null)
				funcs.add(f);
		}

		if (funcs.size() == 0)
			return Func.EMPTY_ARRAY;

		return funcs.moveToArray(new Func[funcs.size()]);
	}

	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		// If _functTemplates is empty, create it and add the FuncTemplate f in it
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{ f };
		}
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public final void attach(Condition c)
	{
		if (c == null || ArrayUtils.contains(_preConditions, c))
			return;

		_preConditions = Arrays.copyOf(_preConditions, _preConditions.length + 1);
		_preConditions[_preConditions.length - 1] = c;
	}

	public boolean checkCondition(L2Character activeChar, L2Object target, boolean sendMessage)
	{
		if (activeChar.isPlayer())
		{
			if (((L2PcInstance) activeChar).isGM() && !Config.GM_ITEM_RESTRICTION)
				return true;
		}

		for (Condition preCondition : _preConditions)
		{
			Env env = new Env();
			env.player = activeChar;
			if (target instanceof L2Character)
				env.target = (L2Character) target;

			if (!preCondition.test(env))
			{
				if (activeChar instanceof L2SummonInstance)
				{
					((L2SummonInstance) activeChar).getOwner().sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
					return false;
				}
				else if (sendMessage && activeChar.isPlayer())
					preCondition.sendMessage(activeChar.getPlayer(), this);
				return false;
			}
		}
		return true;
	}

	/**
	* Returns the name of the item
	* @return String
	*/
	@Override
	public String toString()
	{
		return _name;
	}

	public final String getFuncOwnerName()
	{
		return getName();
	}

	public final L2Skill getFuncOwnerSkill()
	{
		return null;
	}
}