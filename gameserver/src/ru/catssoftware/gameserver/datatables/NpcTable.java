package ru.catssoftware.gameserver.datatables;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.instancemanager.FactionManager;
import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.model.L2DropCategory;
import ru.catssoftware.gameserver.model.L2DropData;
import ru.catssoftware.gameserver.model.L2MinionData;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.entity.faction.Faction;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.util.StatsSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class NpcTable
{
	private final static Logger			_log			= Logger.getLogger(NpcTable.class.getName());

	private static NpcTable				_instance;

	private Map<Integer, L2NpcTemplate>	_npcs;
	private boolean						_initialized	= false;

	public static NpcTable getInstance()
	{
		if (_instance == null)
			_instance = new NpcTable();

		return _instance;
	}

	private NpcTable()
	{
		_npcs = new FastMap<Integer, L2NpcTemplate>();
		restoreNpcData();
	}

	private void restoreNpcData()
	{
		Connection con = null;

		try
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(
				"id",
				"idTemplate",
				"name",
				"serverSideName",
				"title",
				"serverSideTitle",
				"race",
				"collision_radius",
				"collision_height",
				"level",
				"sex",
				"type",
				"attackrange",
				"hp",
				"mp",
				"hpreg",
				"mpreg",
				"str",
				"con",
				"dex",
				"int",
				"wit",
				"men",
				"exp",
				"sp",
				"patk",
				"pdef",
				"matk",
				"mdef",
				"atkspd",
				"aggro",
				"matkspd",
				"rhand",
				"lhand",
				"armor",
				"walkspd",
				"runspd",
				"faction_id",
				"faction_range",
				"isUndead",
				"absorb_level",
				"absorb_type",
				"ss",
				"bss",
				"ss_rate",
				"AI",
				"drop_herbs") + " FROM npc");
				ResultSet npcdata = statement.executeQuery();

				fillNpcTable(npcdata);
				npcdata.close();
				statement.close();
				_log.info("NpcTable: Loaded " + _npcs.size() + " Npc Templates.");
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error creating NPC table: ", e);
			}

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement;
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(
				"id",
				"idTemplate",
				"name",
				"serverSideName",
				"title",
				"serverSideTitle",
				"race",
				"collision_radius",
				"collision_height",
				"level",
				"sex",
				"type",
				"attackrange",
				"hp",
				"mp",
				"hpreg",
				"mpreg",
				"str",
				"con",
				"dex",
				"int",
				"wit",
				"men",
				"exp",
				"sp",
				"patk",
				"pdef",
				"matk",
				"mdef",
				"atkspd",
				"aggro",
				"matkspd",
				"rhand",
				"lhand",
				"armor",
				"walkspd",
				"runspd",
				"faction_id",
				"faction_range",
				"isUndead",
				"absorb_level",
				"absorb_type",
				"ss",
				"bss",
				"ss_rate",
				"AI",
				"drop_herbs") + " FROM custom_npc");
				ResultSet npcdata = statement.executeQuery();
				int npc_count = _npcs.size();
				fillNpcTable(npcdata);
				npcdata.close();
				statement.close();
				if (_npcs.size() > npc_count)
					_log.info("NpcTable: Loaded " + (_npcs.size() - npc_count) + " Custom Npc Templates.");
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error creating custom NPC table: ", e);
			}

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills");
				ResultSet npcskills = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				L2Skill npcSkill = null;

				while (npcskills.next())
				{
					int mobId = npcskills.getInt("npcid");
					npcDat = _npcs.get(mobId);

					if (npcDat == null)
						continue;

					int skillId = npcskills.getInt("skillid");
					int level = npcskills.getInt("level");

					if (skillId == 4416)
					{
						npcDat.setRace(level);
						continue;
					}

					npcSkill = SkillTable.getInstance().getInfo(skillId, level);

					if (npcSkill == null)
						continue;

					npcDat.addSkill(npcSkill);
				}

				npcskills.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error reading NPC skills table: ", e);
			}

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("SELECT npcid, skillid, level FROM custom_npcskills");
				ResultSet npcskills = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				L2Skill npcSkill = null;

				while (npcskills.next())
				{
					int mobId = npcskills.getInt("npcid");
					npcDat = _npcs.get(mobId);

					if (npcDat == null)
						continue;

					int skillId = npcskills.getInt("skillid");
					int level = npcskills.getInt("level");

					if (skillId == 4416)
					{
						npcDat.setRace(level);
						continue;
					}

					npcSkill = SkillTable.getInstance().getInfo(skillId, level);

					if (npcSkill == null)
						continue;
					npcDat.addSkill(npcSkill);
				}

				npcskills.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error reading custom NPC skills table: ", e);
			}

			try
			{
				PreparedStatement statement2 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString("mobId", "itemId", "min", "max", "category", "chance") + " FROM droplist ORDER BY mobId, chance DESC");
				ResultSet dropData = statement2.executeQuery();
				L2DropData dropDat = null;
				L2NpcTemplate npcDat = null;

				while (dropData.next())
				{
					int mobId = dropData.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if (npcDat == null)
					{
						_log.fatal("NPCTable: Drop data for undefined NPC. npcId: " + mobId);
						continue;
					}
					dropDat = new L2DropData();

					dropDat.setItemId(dropData.getInt("itemId"));
					dropDat.setMinDrop(dropData.getInt("min"));
					dropDat.setMaxDrop(dropData.getInt("max"));
					dropDat.setChance(dropData.getInt("chance"));

					int category = dropData.getInt("category");

					npcDat.addDropData(dropDat, category);
				}

				dropData.close();
				statement2.close();
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error reading NPC drop data: ", e);
			}

			try
			{
				PreparedStatement statement2 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString("mobId", "itemId", "min", "max", "category", "chance") + " FROM custom_droplist ORDER BY mobId, chance DESC");
				ResultSet dropData = statement2.executeQuery();
				L2DropData dropDat = null;
				L2NpcTemplate npcDat = null;

				while (dropData.next())
				{
					int mobId = dropData.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if (npcDat == null)
					{
						_log.fatal("NPCTable: Custom drop data for undefined NPC. npcId: " + mobId);
						continue;
					}
					dropDat = new L2DropData();

					dropDat.setItemId(dropData.getInt("itemId"));
					dropDat.setMinDrop(dropData.getInt("min"));
					dropDat.setMaxDrop(dropData.getInt("max"));
					dropDat.setChance(dropData.getInt("chance"));

					int category = dropData.getInt("category");

					npcDat.addDropData(dropDat, category);
				}

				dropData.close();
				statement2.close();
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error reading custom NPC drop data: ", e);
			}

			try
			{
				PreparedStatement statement3 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString("npc_id", "class_id") + " FROM skill_learn");
				ResultSet learndata = statement3.executeQuery();

				while (learndata.next())
				{
					int npcId = learndata.getInt("npc_id");
					int classId = learndata.getInt("class_id");
					L2NpcTemplate npc = getTemplate(npcId);

					if (npc == null)
					{
						_log.warn("NPCTable: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
						continue;
					}

					npc.addTeachInfo(ClassId.values()[classId]);
				}

				learndata.close();
				statement3.close();
			}
			catch (Exception e)
			{
				_log.fatal("NPCTable: Error reading NPC trainer data: ", e);
			}

			try
			{
				PreparedStatement statement4 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString("boss_id", "minion_id", "amount_min", "amount_max") + " FROM minions");
				ResultSet minionData = statement4.executeQuery();
				L2MinionData minionDat = null;
				L2NpcTemplate npcDat = null;
				int cnt = 0;

				while (minionData.next())
				{
					int raidId = minionData.getInt("boss_id");
					npcDat = _npcs.get(raidId);
					if (npcDat == null)
					{
						_log.warn("Minion references undefined boss NPC. Boss NpcId: "+raidId);
						continue;
					}
					minionDat = new L2MinionData();
					minionDat.setMinionId(minionData.getInt("minion_id"));
					minionDat.setAmountMin(minionData.getInt("amount_min"));
					minionDat.setAmountMax(minionData.getInt("amount_max"));
					npcDat.addRaidData(minionDat);
					cnt++;
				}

				minionData.close();
				statement4.close();
				_log.info("NpcTable: Loaded " + cnt + " Minions.");
			}
			catch (Exception e)
			{
				_log.fatal("Error loading minion data: ", e);
			}
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

		_initialized = true;
	}

	private boolean fillNpcTable(ResultSet NpcData) throws Exception
	{
		boolean loaded = false;
		while (NpcData.next())
		{
			StatsSet npcDat = new StatsSet();
			int id = NpcData.getInt("id");

			if (Config.ASSERT)
				assert id < 1000000;

			npcDat.set("npcId", id);
			npcDat.set("idTemplate", NpcData.getInt("idTemplate"));
			int level = NpcData.getInt("level");
			npcDat.set("level", level);
			npcDat.set("race", NpcData.getInt("race"));

			npcDat.set("baseShldDef", 0);
			npcDat.set("baseShldRate", 0);
			npcDat.set("baseCritRate", 38);

			npcDat.set("name", NpcData.getString("name"));
			npcDat.set("serverSideName", NpcData.getBoolean("serverSideName"));
			npcDat.set("title", NpcData.getString("title"));
			npcDat.set("serverSideTitle", NpcData.getBoolean("serverSideTitle"));
			npcDat.set("collision_radius", NpcData.getDouble("collision_radius"));
			npcDat.set("collision_height", NpcData.getDouble("collision_height"));
			npcDat.set("fcollision_radius", NpcData.getDouble("collision_radius"));
			npcDat.set("fcollision_height", NpcData.getDouble("collision_height"));
			npcDat.set("sex", NpcData.getString("sex"));
			if (!Config.ALLOW_NPC_WALKERS && NpcData.getString("type").equalsIgnoreCase("L2NpcWalker"))
				npcDat.set("type", "L2Npc");
			else
				npcDat.set("type", NpcData.getString("type"));
			npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
			npcDat.set("rewardExp", NpcData.getInt("exp"));
			npcDat.set("rewardSp", NpcData.getInt("sp"));
			npcDat.set("basePAtkSpd", NpcData.getInt("atkspd"));
			npcDat.set("baseMAtkSpd", NpcData.getInt("matkspd"));
			npcDat.set("aggroRange", NpcData.getInt("aggro"));
			npcDat.set("rhand", NpcData.getInt("rhand"));
			npcDat.set("lhand", NpcData.getInt("lhand"));
			npcDat.set("armor", NpcData.getInt("armor"));
			npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
			npcDat.set("baseRunSpd", NpcData.getInt("runspd"));

			// constants, until we have stats in DB
			npcDat.set("baseSTR", NpcData.getInt("str"));
			npcDat.set("baseCON", NpcData.getInt("con"));
			npcDat.set("baseDEX", NpcData.getInt("dex"));
			npcDat.set("baseINT", NpcData.getInt("int"));
			npcDat.set("baseWIT", NpcData.getInt("wit"));
			npcDat.set("baseMEN", NpcData.getInt("men"));

			npcDat.set("baseHpMax", NpcData.getInt("hp"));
			npcDat.set("baseCpMax", 0);
			npcDat.set("baseMpMax", NpcData.getInt("mp"));
			npcDat.set("baseHpReg", NpcData.getFloat("hpreg") > 0 ? NpcData.getFloat("hpreg") : 1.5 + ((level - 1) / 10.0));
			npcDat.set("baseMpReg", NpcData.getFloat("mpreg") > 0 ? NpcData.getFloat("mpreg") : 0.9 + 0.3 * ((level - 1) / 10.0));
			npcDat.set("basePAtk", NpcData.getInt("patk"));
			npcDat.set("basePDef", NpcData.getInt("pdef"));
			npcDat.set("baseMAtk", NpcData.getInt("matk"));
			npcDat.set("baseMDef", NpcData.getInt("mdef"));

			npcDat.set("factionId", NpcData.getString("faction_id"));
			npcDat.set("factionRange", NpcData.getInt("faction_range"));

			npcDat.set("isUndead", NpcData.getString("isUndead"));

			npcDat.set("absorb_level", NpcData.getString("absorb_level"));
			npcDat.set("absorb_type", NpcData.getString("absorb_type"));

			npcDat.set("ss", NpcData.getInt("ss"));
			npcDat.set("bss", NpcData.getInt("bss"));
			npcDat.set("ssRate", NpcData.getInt("ss_rate"));

			npcDat.set("AI", NpcData.getString("AI"));
			npcDat.set("drop_herbs", Boolean.valueOf(NpcData.getString("drop_herbs")));

			if (Config.FACTION_ENABLED)
			{
				Faction faction;
				for (int i = 0; i < FactionManager.getInstance().getFactions().size(); i++)
				{
					faction = FactionManager.getInstance().getFactions().get(i);
					if (faction.getNpcList().contains(id))
					{
						npcDat.set("NPCFaction", faction.getId());
						npcDat.set("NPCFactionName", faction.getName());
					}
				}
			}

			L2NpcTemplate template = new L2NpcTemplate(npcDat);
			template.addVulnerability(Stats.BOW_WPN_VULN, 1);
			template.addVulnerability(Stats.CROSSBOW_WPN_VULN, 1);
			template.addVulnerability(Stats.BLUNT_WPN_VULN, 1);
			template.addVulnerability(Stats.DAGGER_WPN_VULN, 1);

			_npcs.put(id, template);

			loaded = true;
		}
		return loaded;
	}

	public boolean reloadNpc(int id)
	{
		Connection con = null;
		boolean loaded = false;
		try
		{
			// save a copy of the old data
			L2NpcTemplate old = getTemplate(id);
			FastMap<Integer, L2Skill> skills = new FastMap<Integer, L2Skill>();

			if (old != null && old.getSkills() != null)
				skills.putAll(old.getSkills());

			FastList<L2DropCategory> categories = new FastList<L2DropCategory>();

			if (old != null && old.getDropData() != null)
				categories.addAll(old.getDropData());

			FastList<ClassId> classIds = new FastList<ClassId>();

			if (old != null && old.getTeachInfo() != null)
				classIds.addAll(old.getTeachInfo());

			FastList<L2MinionData> minions = new FastList<L2MinionData>();

			if (old != null && old.getMinionData() != null)
				minions.addAll(old.getMinionData());

			// reload the NPC base data
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement st = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(
			"id",
			"idTemplate",
			"name",
			"serverSideName",
			"title",
			"serverSideTitle",
			"collision_radius",
			"collision_height",
			"level",
			"race",
			"sex",
			"type",
			"attackrange",
			"hp",
			"mp",
			"hpreg",
			"mpreg",
			"str",
			"con",
			"dex",
			"int",
			"wit",
			"men",
			"exp",
			"sp",
			"patk",
			"pdef",
			"matk",
			"mdef",
			"atkspd",
			"aggro",
			"matkspd",
			"rhand",
			"lhand",
			"armor",
			"walkspd",
			"runspd",
			"faction_id",
			"faction_range",
			"isUndead",
			"absorb_level",
			"absorb_type",
			"ss",
			"bss",
			"ss_rate",
			"AI",
			"drop_herbs") + " FROM npc WHERE id=?");
			st.setInt(1, id);
			ResultSet rs = st.executeQuery();
			loaded = fillNpcTable(rs);
			rs.close();
			st.close();

			if (!loaded)
			{
				st = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(
				"id",
				"idTemplate",
				"name",
				"serverSideName",
				"title",
				"serverSideTitle",
				"collision_radius",
				"collision_height",
				"level",
				"sex",
				"race",
				"type",
				"attackrange",
				"hp",
				"mp",
				"hpreg",
				"mpreg",
				"str",
				"con",
				"dex",
				"int",
				"wit",
				"men",
				"exp",
				"sp",
				"patk",
				"pdef",
				"matk",
				"mdef",
				"atkspd",
				"aggro",
				"matkspd",
				"rhand",
				"lhand",
				"armor",
				"walkspd",
				"runspd",
				"faction_id",
				"faction_range",
				"isUndead",
				"absorb_level",
				"absorb_type",
				"ss",
				"bss",
				"ss_rate",
				"AI",
				"drop_herbs") + " FROM custom_npc WHERE id=?");
				st.setInt(1, id);
				rs = st.executeQuery();
				loaded = fillNpcTable(rs);
				rs.close();
				st.close();
			}

			// restore additional data from saved copy
			L2NpcTemplate created = getTemplate(id);

			for (L2Skill skill : skills.values())
				created.addSkill(skill);

			for (ClassId classId : classIds)
				created.addTeachInfo(classId);

			for (L2MinionData minion : minions)
				created.addRaidData(minion);
		}
		catch (Exception e)
		{
			_log.warn("NPCTable: Could not reload data for NPC " + id + ": " + e, e);
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

		return loaded;
	}

	// just wrapper
	public void reloadAll()
	{
		restoreNpcData();
		QuestManager.getInstance().reloadAllQuests();
	}

	public void cleanUp()
	{
		_npcs.clear();
	}

	public void saveNpc(StatsSet npc)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			Map<String, Object> set = npc.getSet();

			String name = "";
			String values = "";

			for (Object obj : set.keySet())
			{
				name = (String) obj;

				if (name.equalsIgnoreCase("npcId"))
					continue;

				if (values != "")
					values += ", ";

				values += name + " = '" + set.get(name) + "'";
			}

			String query = "UPDATE npc SET " + values + " WHERE id = ?";
			String query_custom = "UPDATE custom_npc SET " + values + " WHERE id = ?";

			try
			{
				PreparedStatement statement = con.prepareStatement(query);
				statement.setInt(1, npc.getInteger("npcId"));
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
			}

			try
			{
				PreparedStatement statement = con.prepareStatement(query_custom);
				statement.setInt(1, npc.getInteger("npcId"));
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
			}
		}
		catch (Exception e)
		{
			_log.warn("NPCTable: Could not store new NPC data in database: ", e);
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

	public boolean isInitialized()
	{
		return _initialized;
	}

	public void replaceTemplate(L2NpcTemplate npc)
	{
		_npcs.put(npc.getNpcId(), npc);
	}

	public L2NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}

	public Map<Integer, L2NpcTemplate> getAllTemplates()
	{
		return _npcs;
	}

	public L2NpcTemplate getTemplateByName(String name)
	{
		for (L2NpcTemplate npcTemplate : _npcs.values())
		{
			if (npcTemplate.getName().equalsIgnoreCase(name))
				return npcTemplate;
		}
		return null;
	}

	public L2NpcTemplate[] getAllOfLevel(int lvl)
	{
		FastList<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		for (L2NpcTemplate t : _npcs.values())
		{
			if (t.getLevel() == lvl)
				list.add(t);
		}
		return list.toArray(new L2NpcTemplate[list.size()]);
	}

	public L2NpcTemplate[] getAllMonstersOfLevel(int lvl)
	{
		FastList<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		for (L2NpcTemplate t : _npcs.values())
		{
			if (t.getLevel() == lvl && "L2Monster".equals(t.getType()))
				list.add(t);
		}
		return list.toArray(new L2NpcTemplate[list.size()]);
	}

	public L2NpcTemplate[] getAllNpcStartingWith(String letter)
	{
		FastList<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		for (L2NpcTemplate t : _npcs.values())
		{
			if (t.getName().startsWith(letter) && "L2Npc".equals(t.getType()))
				list.add(t);
		}
		return list.toArray(new L2NpcTemplate[list.size()]);
	}

	public L2NpcTemplate[] getAllNpcOfClassType(String classType)
	{
		FastList<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		for (L2NpcTemplate t : _npcs.values())
		{
			if (t.getType().equalsIgnoreCase(classType))
				list.add(t);
		}
		return list.toArray(new L2NpcTemplate[list.size()]);
	}

	public Set<Integer> getAllNpcOfL2jClass(Class<?> clazz)
	{
		return null;
	}

	public Set<Integer> getAllNpcOfAiType(String aiType)
	{
		return null;
	}
}