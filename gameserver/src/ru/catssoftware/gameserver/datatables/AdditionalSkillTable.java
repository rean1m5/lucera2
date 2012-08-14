package ru.catssoftware.gameserver.datatables;

import javolution.util.FastList;


import org.apache.log4j.Logger;



/**
 * @author M-095
 * @for L2CatsSoftware
 */

public class AdditionalSkillTable
{
	private static Logger							_log				= Logger.getLogger(AdditionalSkillTable.class);
	private static FastList<Integer>			_skillList			= new FastList<Integer>();
	private static AdditionalSkillTable			_instance			= null;
	// Список ID скилов принадлежащих к сабовым скилам
	// Список скилов исключений
	private static int[]						_exludeSkills		=
	{
		194, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609,
		610, 617, 618, 619, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674
	};

	public static AdditionalSkillTable getInstance()
	{
		if (_instance == null)
			_instance = new AdditionalSkillTable();
		return _instance;
	}

	private AdditionalSkillTable()
	{
		for (int skillId : _exludeSkills)
				_skillList.add(skillId);

		_log.info("ExtraSkillTable: Loaded  " + _skillList.size() + " skills.");
	}

	public boolean isExSkill(int skillId)
	{
		return (_skillList.contains(skillId));
	}
}