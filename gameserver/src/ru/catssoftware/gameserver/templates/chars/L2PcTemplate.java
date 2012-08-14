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
package ru.catssoftware.gameserver.templates.chars;

import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.util.StatsSet;

import javolution.util.FastList;


/**
 * Base template for all type of playable characters
 * Override {@link ru.catssoftware.gameserver.templates.chars.L2CharTemplate} to add some properties
 * specific to pc.
 *
 * <br/>
 * <br/>
 * <font color="red">
 * <b>Property don't change in the time, this is just a template, not the currents status
 * of characters !</b>
 * </font>
 */
public class L2PcTemplate extends L2CharTemplate
{
	/** The Class object of the L2PcInstance */
	private ClassId			classId;

	private Race			race;
	private String			className;

	private int				spawnX;
	private int				spawnY;
	private int				spawnZ;

	private int				classBaseLevel;
	private float			lvlHpAdd;
	private float			lvlHpMod;
	private float			lvlCpAdd;
	private float			lvlCpMod;
	private float			lvlMpAdd;
	private float			lvlMpMod;

	private List<PcTemplateItem> _items = new FastList<PcTemplateItem>();

	public L2PcTemplate(StatsSet set)
	{
		super(set);
		classId = ClassId.values()[set.getInteger("classId")];
		race = Race.values()[set.getInteger("raceId")];
		className = set.getString("className");

		spawnX = set.getInteger("spawnX");
		spawnY = set.getInteger("spawnY");
		spawnZ = set.getInteger("spawnZ");

		classBaseLevel = set.getInteger("classBaseLevel");
		lvlHpAdd = set.getFloat("lvlHpAdd");
		lvlHpMod = set.getFloat("lvlHpMod");
		lvlCpAdd = set.getFloat("lvlCpAdd");
		lvlCpMod = set.getFloat("lvlCpMod");
		lvlMpAdd = set.getFloat("lvlMpAdd");
		lvlMpMod = set.getFloat("lvlMpMod");
	}

	/**
	 * adds starter equipment
	 * @param i
	 */
	public void addItem(int itemId, int amount, boolean equipped)
	{
		_items.add(new PcTemplateItem(itemId, amount, equipped));
	}

	/**
	 *
	 * @return itemIds of all the starter equipment
	 */
	public List<PcTemplateItem> getItems()
	{
		return _items;
	}

	/**
	 * @return the classBaseLevel
	 */
	public int getClassBaseLevel()
	{
		return classBaseLevel;
	}

	/**
	 * @param classBaseLevel the classBaseLevel to set
	 */
	public void setClassBaseLevel(int _classBaseLevel)
	{
		classBaseLevel = _classBaseLevel;
	}

	/**
	 * @return the classId
	 */
	public ClassId getClassId()
	{
		return classId;
	}

	/**
	 * @param classId the classId to set
	 */
	public void setClassId(ClassId _classId)
	{
		classId = _classId;
	}

	/**
	 * @return the className
	 */
	public String getClassName()
	{
		return className;
	}

	/**
	 * @param className the className to set
	 */
	public void setClassName(String _className)
	{
		className = _className;
	}

	/**
	 * @return the lvlCpAdd
	 */
	public float getLvlCpAdd()
	{
		return lvlCpAdd;
	}

	/**
	 * @param lvlCpAdd the lvlCpAdd to set
	 */
	public void setLvlCpAdd(float _lvlCpAdd)
	{
		lvlCpAdd = _lvlCpAdd;
	}

	/**
	 * @return the lvlCpMod
	 */
	public float getLvlCpMod()
	{
		return lvlCpMod;
	}

	/**
	 * @param lvlCpMod the lvlCpMod to set
	 */
	public void setLvlCpMod(float _lvlCpMod)
	{
		lvlCpMod = _lvlCpMod;
	}

	/**
	 * @return the lvlHpAdd
	 */
	public float getLvlHpAdd()
	{
		return lvlHpAdd;
	}

	/**
	 * @param lvlHpAdd the lvlHpAdd to set
	 */
	public void setLvlHpAdd(float _lvlHpAdd)
	{
		lvlHpAdd = _lvlHpAdd;
	}

	/**
	 * @return the lvlHpMod
	 */
	public float getLvlHpMod()
	{
		return lvlHpMod;
	}

	/**
	 * @param lvlHpMod the lvlHpMod to set
	 */
	public void setLvlHpMod(float _lvlHpMod)
	{
		lvlHpMod = _lvlHpMod;
	}

	/**
	 * @return the lvlMpAdd
	 */
	public float getLvlMpAdd()
	{
		return lvlMpAdd;
	}

	/**
	 * @param lvlMpAdd the lvlMpAdd to set
	 */
	public void setLvlMpAdd(float _lvlMpAdd)
	{
		lvlMpAdd = _lvlMpAdd;
	}

	/**
	 * @return the lvlMpMod
	 */
	public float getLvlMpMod()
	{
		return lvlMpMod;
	}

	/**
	 * @param lvlMpMod the lvlMpMod to set
	 */
	public void setLvlMpMod(float _lvlMpMod)
	{
		lvlMpMod = _lvlMpMod;
	}

	/**
	 * @return the race
	 */
	public Race getRace()
	{
		return race;
	}

	/**
	 * @param race the race to set
	 */
	public void setRace(Race _race)
	{
		race = _race;
	}

	/**
	 * @return the spawnX
	 */
	public int getSpawnX()
	{
		return spawnX;
	}

	/**
	 * @param spawnX the spawnX to set
	 */
	public void setSpawnX(int _spawnX)
	{
		spawnX = _spawnX;
	}

	/**
	 * @return the spawnY
	 */
	public int getSpawnY()
	{
		return spawnY;
	}

	/**
	 * @param spawnY the spawnY to set
	 */
	public void setSpawnY(int _spawnY)
	{
		spawnY = _spawnY;
	}

	/**
	 * @return the spawnZ
	 */
	public int getSpawnZ()
	{
		return spawnZ;
	}

	/**
	 * @param spawnZ the spawnZ to set
	 */
	public void setSpawnZ(int _spawnZ)
	{
		spawnZ = _spawnZ;
	}

	public int getBaseFallSafeHeight(boolean female)
	{
		if (classId.getRace() == Race.Darkelf || classId.getRace() == Race.Elf)
			return (classId.isMage()) ? ((female) ? 330 : 300) : ((female) ? 380 : 350);

		else if (classId.getRace() == Race.Dwarf)
			return ((female) ? 200 : 180);

		else if (classId.getRace() == Race.Human)
			return (classId.isMage()) ? ((female) ? 220 : 200) : ((female) ? 270 : 250);

		else if (classId.getRace() == Race.Orc)
			return (classId.isMage()) ? ((female) ? 280 : 250) : ((female) ? 220 : 200);

		return Config.ALT_MINIMUM_FALL_HEIGHT;

		/**
		  	Dark Elf Fighter F 380
			Dark Elf Fighter M 350
			Dark Elf Mystic F 330
			Dark Elf Mystic M 300
			Dwarf Fighter F 200
			Dwarf Fighter M 180
			Elf Fighter F 380
			Elf Fighter M 350
			Elf Mystic F 330
			Elf Mystic M 300
			Human Fighter F 270
			Human Fighter M 250
			Human Mystic F 220
			Human Mystic M 200
			Orc Fighter F 220
			Orc Fighter M 200
			Orc Mystic F 280
			Orc Mystic M 250
		 */
	}

	public static final class PcTemplateItem
	{
		private final int _itemId;
		private final int _amount;
		private final boolean _equipped;

		/**
		 * @param itemId
		 * @param amount
		 * @param equipped
		 */
		public PcTemplateItem(int itemId, int amount, boolean equipped)
		{
			_itemId = itemId;
			_amount = amount;
			_equipped = equipped;
		}

		/**
		 * @return Returns the itemId.
		 */
		public int getItemId()
		{
			return _itemId;
		}

		/**
		 * @return Returns the amount.
		 */
		public int getAmount()
		{
			return _amount;
		}

		/**
		 * @return Returns the if the item should be equipped after char creation.
		 */
		public boolean isEquipped()
		{
			return _equipped;
		}
	}
}