package ru.catssoftware.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastList;
import javolution.util.FastMap;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Skill;

/**
 * Warning: must be loaded after loading SkillTable
 *
 * @author  House
 */
public class ResidentialSkillTable
{
	protected static final Logger _log = Logger.getLogger(ResidentialSkillTable.class.getName());

	private static ResidentialSkillTable _instance = null;
	private static FastMap<Integer, FastList<L2Skill>> _list;

	ResidentialSkillTable()
	{
		load();
	}

	private void load()
	{
		_list = new FastMap<Integer, FastList<L2Skill>>();
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM skill_residential ORDER BY entityId");
			ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				int entityId = rs.getInt("entityId");
				int skillId = rs.getInt("skillId");
				int skillLvl = rs.getInt("skillLevel");

				L2Skill sk = SkillTable.getInstance().getInfo(skillId, skillLvl);

				if (sk == null)
				{
					_log.warn("ResidentialSkillTable: SkillTable has returned null for ID/level: " +skillId+"/"+skillLvl);
					continue;
				}
				if (!_list.containsKey(entityId))
				{
					FastList<L2Skill> aux = new FastList<L2Skill>();
					aux.add(sk);
					_list.put(entityId, aux);
				}
				else
					_list.get(entityId).add(sk);
			}
			statement.close();
			rs.close();
		}
		catch (Exception e)
		{
			_log.warn("ResidentialSkillTable: a problem occured while loading skills!");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch(Exception e)
			{

			}
			_log.info("ResidentialSkillTable: Loaded " + _list.size() + " skills.");
		}
	}

	public FastList<L2Skill> getSkills(int entityId)
	{
		if (_list.containsKey(entityId))
			return _list.get(entityId);
		else return null;
	}

	public static ResidentialSkillTable getInstance()
	{
		if (_instance == null) 
			_instance = new ResidentialSkillTable();
		return _instance;
	}
}