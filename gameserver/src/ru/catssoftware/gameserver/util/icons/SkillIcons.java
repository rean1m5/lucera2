package ru.catssoftware.gameserver.util.icons;

/*
 * @author Ro0TT
 * @date 12.01.2012
 */

import org.apache.log4j.Logger;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Skill;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

public class SkillIcons
{
	private static SkillIcons _instance = null;

	protected static Logger _log = Logger.getLogger(SkillIcons.class);

	public static SkillIcons getInstance()
	{
		if (_instance==null)
			_instance = new SkillIcons();
		return _instance;
	}

	private HashMap<L2Skill, String> _skills = new HashMap<L2Skill, String>();

	public String getIcon(L2Skill skill)
	{
		String icon = "Icon.skill0000";
		if (skill!=null && _skills.containsKey(skill))
			icon = _skills.get(skill);
		return icon;
	}

	public String getIcon(int skillId, int skillLevel)
	{

		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		return getIcon(skill);
	}

	public SkillIcons()
	{
		_instance = this;
		java.sql.Connection con = null;
		PreparedStatement statement;
		ResultSet rset;
		int skillId = 0, skillLevel = 0;
		_skills = new HashMap<L2Skill, String>();

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT `skillId`, `skillLevel`, `icon` FROM `skill_icons`");
			rset = statement.executeQuery();
			
			String icon;
			
			while(rset.next())
			{
				skillId = rset.getInt("skillId");				
				skillLevel = rset.getInt("skillLevel");
				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
				
				icon = rset.getString("icon");
				_skills.put(skill, icon);
			}
			
			_log.info("Loaded " + _skills.size() + " Skill Icons.");
		}
		catch(final Exception e)
		{
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
}
