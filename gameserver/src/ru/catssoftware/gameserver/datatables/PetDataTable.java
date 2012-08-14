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
package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.model.L2PetData;

public class PetDataTable
{
	private final static Logger										_log						= Logger.getLogger(PetDataTable.class.getName());
	private static PetDataTable										_instance;

	public final static int											PET_WOLF_ID					= 12077;

	public final static int											HATCHLING_WIND_ID			= 12311;
	public final static int											HATCHLING_STAR_ID			= 12312;
	public final static int											HATCHLING_TWILIGHT_ID		= 12313;

	public final static int											STRIDER_WIND_ID				= 12526;
	public final static int											STRIDER_STAR_ID				= 12527;
	public final static int											STRIDER_TWILIGHT_ID			= 12528;

	public final static int											RED_STRIDER_WIND_ID			= 16038;
	public final static int											RED_STRIDER_STAR_ID			= 16039;
	public final static int											RED_STRIDER_TWILIGHT_ID		= 16040;

	public final static int											WYVERN_ID					= 12621;

	public final static int											BABY_BUFFALO_ID				= 12780;
	public final static int											BABY_KOOKABURRA_ID			= 12781;
	public final static int											BABY_COUGAR_ID				= 12782;

	public final static int											IMPROVED_BABY_BUFFALO_ID	= 16034;
	public final static int											IMPROVED_BABY_KOOKABURRA_ID	= 16035;
	public final static int											IMPROVED_BABY_COUGAR_ID		= 16036;

	public final static int											SIN_EATER_ID				= 12564;

	public final static int											BLACK_WOLF_ID				= 16030;
	public final static int											WGREAT_WOLF_ID				= 16037;
	public final static int											GREAT_WOLF_ID				= 16025;
	public final static int											FENRIR_WOLF_ID				= 16041;
	public final static int											WFENRIR_WOLF_ID				= 16042;
	public final static int											PURPLE_HORSE_ID				= 13130;

	private static FastMap<Integer, FastMap<Integer, L2PetData>>	petTable;

	public final static int[]										EMPTY_INT					= { 0 };

	public static PetDataTable getInstance()
	{
		if (_instance == null)
			_instance = new PetDataTable();

		return _instance;
	}

	private PetDataTable()
	{
		petTable = new FastMap<Integer, FastMap<Integer, L2PetData>>();
	}

	public void loadPetsData()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT typeID, level, expMax, hpMax, mpMax, patk, pdef, matk, mdef, acc, evasion, crit, speed, atk_speed, cast_speed, feedMax, feedbattle, feednormal, loadMax, hpregen, mpregen, owner_exp_taken FROM pets_stats");
			ResultSet rset = statement.executeQuery();

			int petId, petLevel;

			while (rset.next())
			{
				petId = rset.getInt("typeID");
				petLevel = rset.getInt("level");

				//build the petdata for this level
				L2PetData petData = new L2PetData();
				petData.setPetID(petId);
				petData.setPetLevel(petLevel);
				petData.setPetMaxExp(rset.getLong("expMax"));
				petData.setPetMaxHP(rset.getInt("hpMax"));
				petData.setPetMaxMP(rset.getInt("mpMax"));
				petData.setPetPAtk(rset.getInt("patk"));
				petData.setPetPDef(rset.getInt("pdef"));
				petData.setPetMAtk(rset.getInt("matk"));
				petData.setPetMDef(rset.getInt("mdef"));
				petData.setPetAccuracy(rset.getInt("acc"));
				petData.setPetEvasion(rset.getInt("evasion"));
				petData.setPetCritical(rset.getInt("crit"));
				petData.setPetSpeed(rset.getInt("speed"));
				petData.setPetAtkSpeed(rset.getInt("atk_speed"));
				petData.setPetCastSpeed(rset.getInt("cast_speed"));
				petData.setPetMaxFeed(rset.getInt("feedMax"));
				petData.setPetFeedNormal(rset.getInt("feednormal"));
				petData.setPetFeedBattle(rset.getInt("feedbattle"));
				petData.setPetMaxLoad(rset.getInt("loadMax"));
				petData.setPetRegenHP(rset.getInt("hpregen"));
				petData.setPetRegenMP(rset.getInt("mpregen"));
				petData.setOwnerExpTaken(rset.getFloat("owner_exp_taken"));

				// if its the first data for this petid, we initialize its level FastMap
				if (!petTable.containsKey(petId))
					petTable.put(petId, new FastMap<Integer, L2PetData>());

				petTable.get(petId).put(petLevel, petData);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not load pets stats: " + e);
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

		_log.info("PetDataTable: loaded " + petTable.size() + " pets.");
	}

	public void addPetData(L2PetData petData)
	{
		FastMap<Integer, L2PetData> h = petTable.get(petData.getPetID());

		if (h == null)
		{
			FastMap<Integer, L2PetData> statTable = new FastMap<Integer, L2PetData>();
			statTable.put(petData.getPetLevel(), petData);
			petTable.put(petData.getPetID(), statTable);
			return;
		}

		h.put(petData.getPetLevel(), petData);
	}

	public void addPetData(L2PetData[] petLevelsList)
	{
		for (int i = 0; i < petLevelsList.length; i++)
			addPetData(petLevelsList[i]);
	}

	public L2PetData getPetData(int petID, int petLevel)
	{
		try
		{
			return petTable.get(petID).get(petLevel);
		}
		catch (NullPointerException npe)
		{
		return null;
		}
	}

	public static int getPetIdByItemId(int itemId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getControlItemId() == itemId)
				return pet.getNpcId();
		}

		return 0;
	}

	public static int getItemIdByPetId(int npcId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getNpcId() == npcId)
				return pet.getControlItemId();
		}

		return 0;
	}

	public static int[] getFoodItemId(int npcId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getNpcId() == npcId)
				return pet.getFoodIds();
		}

		return EMPTY_INT;
	}

	public static boolean isPet(int npcId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getNpcId() == npcId)
				return true;
		}

		return false;
	}

	public static boolean isPetFood(int npcId, int itemId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getNpcId() == npcId)
			{
				for (int id : pet.getFoodIds())
				{
					if (id == itemId)
						return true;
				}
			}
		}

		return false;
	}

	public static boolean isPetFood(int itemId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			for (int id : pet.getFoodIds())
			{
				if (id == itemId)
					return true;
			}
		}

		return false;
	}

	public static boolean isPetItem(int itemId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getControlItemId() == itemId)
				return true;
		}

		return false;
	}

	public static boolean isMountable(int npcId)
	{
		for (L2Pet pet : L2Pet.values())
		{
			if (pet.getNpcId() == npcId)
				return pet.isMountable();
		}

		return false;
	}

	public static boolean isWolf(int npcId)
	{
		return (PET_WOLF_ID == npcId);
	}

	public static boolean isHatchling(int npcId)
	{
		return (HATCHLING_WIND_ID == npcId || HATCHLING_STAR_ID == npcId || HATCHLING_TWILIGHT_ID == npcId);
	}

	public static boolean isStrider(int npcId)
	{
		switch (npcId)
		{
		case STRIDER_WIND_ID:
		case STRIDER_STAR_ID:
		case STRIDER_TWILIGHT_ID:
		case RED_STRIDER_WIND_ID:
		case RED_STRIDER_STAR_ID:
		case RED_STRIDER_TWILIGHT_ID:
			return true;
		}

		return false;
	}

	public static boolean isWyvern(int npcId)
	{
		return (WYVERN_ID == npcId);
	}

	public static boolean isEvolvedWolf(int npcId)
	{
		switch (npcId)
		{
		case GREAT_WOLF_ID:
		case WGREAT_WOLF_ID:
		case BLACK_WOLF_ID:
		case FENRIR_WOLF_ID:
		case WFENRIR_WOLF_ID:
			return true;
		}

		return false;
	}

	public static boolean isBaby(int npcId)
	{
		return (BABY_BUFFALO_ID == npcId || BABY_KOOKABURRA_ID == npcId || BABY_COUGAR_ID == npcId);
	}

	public static boolean isImprovedBaby(int npcId)
	{
		return (IMPROVED_BABY_BUFFALO_ID == npcId || IMPROVED_BABY_KOOKABURRA_ID == npcId || IMPROVED_BABY_COUGAR_ID == npcId);
	}

	public static boolean isSinEater(int npcId)
	{
		return npcId == SIN_EATER_ID;
	}

	public static boolean isHorse(int npcId)
	{
		return npcId == PURPLE_HORSE_ID;
	}

	/**
	 * This class describes basic pet info
	 * NPC template id, control item id, food item id and can be pet mounted
	 */
	private static enum L2Pet
	{
		WOLF(PET_WOLF_ID, 2375, new int[] {2515}, false),

		HATCHLING_WIND(HATCHLING_WIND_ID, 3500, new int[] {4038}, false),
		HATCHLING_STAR(HATCHLING_STAR_ID, 3501, new int[] {4038}, false),
		HATCHLING_TWILIGHT(HATCHLING_TWILIGHT_ID, 3502, new int[] {4038}, false),

		STRIDER_WIND(STRIDER_WIND_ID, 4422, new int[] {5168,5169}, true),
		STRIDER_STAR(STRIDER_STAR_ID, 4423, new int[] {5168,5169}, true),
		STRIDER_TWILIGHT(STRIDER_TWILIGHT_ID, 4424, new int[] {5168,5169}, true),


		WYVERN(WYVERN_ID, 5249, new int[] {6316}, true),

		GREAT_WOLF(GREAT_WOLF_ID, 10163, new int[] {9668}, true),
/*		WGREAT_WOLF(WGREAT_WOLF_ID, 10307, new int[] {9668}, true),
		BLACK_WOLF(BLACK_WOLF_ID, 9882, new int[] {9668}, true),
		FENRIR_WOLF(FENRIR_WOLF_ID, 10426, new int[] {9668}, true),
		WFENRIR_WOLF(WFENRIR_WOLF_ID, 10611, new int[] {9668}, true),
*/
		BABY_BUFFALO(BABY_BUFFALO_ID, 6648, new int[] {7582}, false),
		BABY_KOOKABURRA(BABY_KOOKABURRA_ID, 6649, new int[] {7582}, false),
		BABY_COUGAR(BABY_COUGAR_ID, 6650, new int[] {7582}, false),


		SIN_EATER(SIN_EATER_ID, 4425, new int[] {2515}, false);

		private final int		_npcId;
		private final int		_controlItemId;
		private final int[]		_foodIds;
		private final boolean	_isMountabe;

		private L2Pet(int npcId, int controlItemId, int[] foodIds, boolean isMountabe)
		{
			_npcId = npcId;
			_controlItemId = controlItemId;
			_foodIds = foodIds;
			_isMountabe = isMountabe;
		}

		public int getNpcId()
		{
			return _npcId;
		}

		public int getControlItemId()
		{
			return _controlItemId;
		}

		public int[] getFoodIds()
		{
			return _foodIds;
		}

		public boolean isMountable()
		{
			return _isMountabe;
		}
	}
}