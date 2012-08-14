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
package ru.catssoftware.gameserver.datatables.xml;

import java.io.File;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;


import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Augmentation;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.tools.random.Rnd;

/**
 * This class manages the augmentation data and can also create new augmentations.
 *
 * @author  durgus
 * edited by Gigiikun
 */
public class AugmentationData
{
	private final static Logger		_log	= Logger.getLogger(AugmentationData.class.getName());

	private static AugmentationData	_instance;

	public static final AugmentationData getInstance()
	{
		if (_instance == null)
			_instance = new AugmentationData();

		return _instance;
	}

	// stats
	private static final int			STAT_START			= 1;
	private static final int			STAT_END			= 14560;
	private static final int			STAT_BLOCKSIZE		= 3640;
	private static final int			STAT_SUBBLOCKSIZE	= 91;

	// weapon skills
	private static final int SKILLS_BLOCKSIZE = 178;
	private static final int BLUE_START = 0;
	private static final int BLUE_END = 16;
	private static final int PURPLE_START = 17;
	private static final int PURPLE_END = 123;
	private static final int RED_START = 124;
	private static final int RED_END = 177;

	// basestats
	private static final int			BASESTAT_STR		= 16341;
	private static final int			BASESTAT_CON		= 16342;
	private static final int			BASESTAT_INT		= 16343;
	private static final int			BASESTAT_MEN		= 16344;

	private FastList<?>[]				_augmentationStats;
	private AugmentationSkill[]			_augmentationSkills;

	public AugmentationData()
	{
		_augmentationStats = new FastList[4];
		_augmentationStats[0] = new FastList<AugmentationStat>();
		_augmentationStats[1] = new FastList<AugmentationStat>();
		_augmentationStats[2] = new FastList<AugmentationStat>();
		_augmentationStats[3] = new FastList<AugmentationStat>();

		load();

		// Use size*4: since theres 4 blocks of stat-data with equivalent size
		_log.info("AugmentationData: Loaded: " + (_augmentationStats[0].size() * 4) + " augmentation stats.");
		_log.info("AugmentationData: Loaded: " + _augmentationSkills.length + " weapons skills.");
	}

	public class AugmentationSkill
	{
		private int	_skillId;
		private int	_maxSkillLevel;
		private int	_augmentationSkillId;

		public AugmentationSkill(int skillId, int maxSkillLevel, int augmentationSkillId)
		{
			_skillId = skillId;
			_maxSkillLevel = maxSkillLevel;
			_augmentationSkillId = augmentationSkillId;
		}

		public L2Skill getSkill()
		{
			return SkillTable.getInstance().getInfo(_skillId, _maxSkillLevel);
		}

		public int getAugmentationSkillId()
		{
			return _augmentationSkillId;
		}
	}

	public class AugmentationStat
	{
		private Stats	_stat;
		private int		_singleSize;
		private int		_combinedSize;
		private float	_singleValues[];
		private float	_combinedValues[];

		public AugmentationStat(Stats stat, float sValues[], float cValues[])
		{
			_stat = stat;
			_singleSize = sValues.length;
			_singleValues = sValues;
			_combinedSize = cValues.length;
			_combinedValues = cValues;
		}

		public int getSingleStatSize()
		{
			return _singleSize;
		}

		public int getCombinedStatSize()
		{
			return _combinedSize;
		}

		public float getSingleStatValue(int i)
		{
			if (i >= _singleSize || i < 0)
				return _singleValues[_singleSize - 1];
			return _singleValues[i];
		}

		public float getCombinedStatValue(int i)
		{
			if (i >= _combinedSize || i < 0)
				return _combinedValues[_combinedSize - 1];
			return _combinedValues[i];
		}

		public Stats getStat()
		{
			return _stat;
		}
	}

	@SuppressWarnings("unchecked")
	private final void load()
	{
		// Load the skillmap
		// Note: the skillmap data is only used when generating new augmentations
		// the client expects a different id in order to display the skill in the
		// items description...
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT, "data/stats/augmentation/augmentation_skillmap.xml");
			if (!file.exists())
			{
				if (_log.isDebugEnabled() || Config.DEBUG)
					_log.info("The augmentation skillmap file is missing.");
				return;
			}

			FastList<AugmentationSkill> list = new FastList<AugmentationSkill>();

			Document doc = factory.newDocumentBuilder().parse(file);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("augmentation".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int skillId = 0, skillLevel = 0, augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							// type of the skill is not needed anymore but I do not erase the code.
							// maybe someone can use it for something

							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("skillId".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("skillLevel".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillLevel = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
							}

							list.add(new AugmentationSkill(skillId, skillLevel, augmentationId));
						}
					}
				}
			}
			_augmentationSkills = list.toArray(new AugmentationSkill[list.size()]);
		}
		catch (Exception e)
		{
			_log.fatal("Error parsing augmentation_skillmap.xml.", e);
			return;
		}

		// Load the stats from xml
		for (int i = 1; i < 5; i++)
		{
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);

				File file = new File(Config.DATAPACK_ROOT, "data/stats/augmentation/augmentation_stats" + i + ".xml");
				if (!file.exists())
				{
					if (_log.isDebugEnabled() || Config.DEBUG)
						_log.info("The augmentation stat data file " + i + " is missing.");
					return;
				}

				Document doc = factory.newDocumentBuilder().parse(file);

				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								String statName = attrs.getNamedItem("name").getNodeValue();
								float soloValues[] = null, combinedValues[] = null;

								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("table".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String tableName = attrs.getNamedItem("name").getNodeValue();

										StringTokenizer data = new StringTokenizer(cd.getFirstChild().getNodeValue());
										FastList<Float> array = new FastList<Float>();
										while (data.hasMoreTokens())
											array.add(Float.parseFloat(data.nextToken()));

										if (tableName.equalsIgnoreCase("#soloValues"))
										{
											soloValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												soloValues[x++] = value;
										}
										else
										{
											combinedValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												combinedValues[x++] = value;
										}
									}
								}
								// store this stat
								((FastList<AugmentationStat>) _augmentationStats[(i - 1)]).add(new AugmentationStat(Stats.valueOfXml(statName), soloValues,
										combinedValues));
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.fatal("Error parsing augmentation_stats" + i + ".xml.", e);
				return;
			}
		}
	}

	/**
	 * Generate a new random augmentation
	 * @param lifeStoneLevel
	 * @param lifeStoneGrade
	 * @return L2Augmentation
	 */
	public L2Augmentation generateRandomAugmentation(int lifeStoneLevel, int lifeStoneGrade)
	{
		// Note that stat12 stands for stat 1 AND 2 (same for stat34 ;p )
		// this is because a value can contain up to 2 stat modifications
		// (there are two short values packed in one integer value, meaning 4 stat modifications at max)
		// for more info take a look at getAugStatsById(...)

		// Note: lifeStoneGrade: (0 means low grade, 3 top grade)
		// First: determine whether we will add a skill/baseStatModifier or not
		// because this determine which color could be the result

		int stat34 = 0;
		boolean generateSkill = false;
		int resultColor = 0;
		boolean generateGlow = false;
		//lifestonelevel is used for stat Id and skill level, but here the max level is 10
		if (lifeStoneLevel > 10)
			lifeStoneLevel = 10;
		switch (lifeStoneGrade)
		{
		case 0:
			generateSkill = Rnd.get(100) < Config.AUGMENTATION_NG_SKILL_CHANCE;
			generateGlow = Rnd.get(100) < Config.AUGMENTATION_NG_GLOW_CHANCE;
			break;
		case 1:
			generateSkill = Rnd.get(100) < Config.AUGMENTATION_MID_SKILL_CHANCE;
			generateGlow = Rnd.get(100) < Config.AUGMENTATION_MID_GLOW_CHANCE;
			break;
		case 2:
			generateSkill = Rnd.get(100) < Config.AUGMENTATION_HIGH_SKILL_CHANCE;
			generateGlow = Rnd.get(100) < Config.AUGMENTATION_HIGH_GLOW_CHANCE;
			break;
		case 3:
			generateSkill = Rnd.get(100) < Config.AUGMENTATION_TOP_SKILL_CHANCE;
			generateGlow = Rnd.get(100) < Config.AUGMENTATION_TOP_GLOW_CHANCE;
			break;
		}

		if (!generateSkill && Rnd.get(100) < Config.AUGMENTATION_BASESTAT_CHANCE)
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);

		// Second: decide which grade the augmentation result is going to have:
		// 0:yellow, 1:blue, 2:purple, 3:red
		// The chances used here are most likely custom,
		// whats known is: you cant have yellow with skill(or baseStatModifier)
		// noGrade stone can not have glow, mid only with skill, high has a chance(custom), top allways glow
		if (stat34 == 0 && !generateSkill)
		{
			resultColor = Rnd.get(0, 100);
			if (resultColor <= (15 * lifeStoneGrade) + 40)
				resultColor = 1;
			else
				resultColor = 0;
		}
		else
		{
			resultColor = Rnd.get(0, 100);
			if (resultColor <= (10 * lifeStoneGrade) + 5 || stat34 != 0)
				resultColor = 3;
			else if (resultColor <= (10 * lifeStoneGrade) + 10)
				resultColor = 1;
			else
				resultColor = 2;
		}

		// Third: Calculate the subblock offset for the choosen color,
		// and the level of the lifeStone
		// from large number of retail augmentations:
		// no skill part
		// Id for stat12:
		// A:1-910 B:911-1820 C:1821-2730 D:2731-3640 E:3641-4550 F:4551-5460 G:5461-6370 H:6371-7280
		// Id for stat34(this defines the color):
		// I:7281-8190(yellow) K:8191-9100(blue) L:10921-11830(yellow) M:11831-12740(blue)
		// you can combine I-K with A-D and L-M with E-H
		// using C-D or G-H Id you will get a glow effect
		// there seems no correlation in which grade use which Id except for the glowing restriction
		// skill part
		// Id for stat12:
		// same for no skill part
		// A same as E, B same as F, C same as G, D same as H
		// A - no glow, no grade LS
		// B - weak glow, mid grade LS?
		// C - glow, high grade LS?
		// D - strong glow, top grade LS?

		int stat12 = 0;
		// is neither a skill nor basestat used for stat34? then generate a normal stat
		if (stat34 == 0 && !generateSkill)
		{
			int temp = Rnd.get(2,3);
			int colorOffset = resultColor * (10 * STAT_SUBBLOCKSIZE) + temp * STAT_BLOCKSIZE + 1;
			int offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + colorOffset;

			stat34 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
			if (generateGlow && lifeStoneGrade >= 2)
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + (temp - 2) * STAT_BLOCKSIZE + lifeStoneGrade * (10 * STAT_SUBBLOCKSIZE) + 1;
			else
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + (temp - 2) * STAT_BLOCKSIZE + Rnd.get(0, 1) * (10 * STAT_SUBBLOCKSIZE) + 1;
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}
		else
		{
			int offset;
			if (!generateGlow)
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + 1;
			else
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + (lifeStoneGrade + resultColor) / 2 * (10 * STAT_SUBBLOCKSIZE) + 1;
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}

		// generate a skill if neccessary
		L2Skill skill = null;

		if (generateSkill)
		{
			int skillOffset = (lifeStoneLevel - 1) * SKILLS_BLOCKSIZE;
			AugmentationSkill temp = null;
			switch (resultColor)
			{
			case 1: // blue skill
			{
				temp = _augmentationSkills[skillOffset + Rnd.get(BLUE_START, BLUE_END)];
				skill = temp.getSkill();
				stat34 = temp.getAugmentationSkillId();
				break;
			}
			case 2: // purple skill
			{
				temp = _augmentationSkills[skillOffset + Rnd.get(PURPLE_START, PURPLE_END)];
				skill = temp.getSkill();
				stat34 = temp.getAugmentationSkillId();
				break;
			}
			case 3: // red skill
			{
				temp = _augmentationSkills[skillOffset + Rnd.get(RED_START, RED_END)];
				skill = temp.getSkill();
				stat34 = temp.getAugmentationSkillId();
				break;
			}
			}
		}
		

		if (_log.isDebugEnabled())
			_log.info("Augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; resultColor=" + resultColor + "; level=" + lifeStoneLevel + "; grade=" + lifeStoneGrade);

		return new L2Augmentation(((stat34 << 16) + stat12), skill);
	}

	public class AugStat
	{
		private Stats	_stat;
		private float	_value;

		public AugStat(Stats stat, float value)
		{
			_stat = stat;
			_value = value;
		}

		public Stats getStat()
		{
			return _stat;
		}

		public float getValue()
		{
			return _value;
		}
	}

	/**
	 * Returns the stat and basestat boni for a given augmentation id
	 * @param augmentationId
	 * @return
	 */
	public FastList<AugStat> getAugStatsById(int augmentationId)
	{
		FastList<AugStat> temp = new FastList<AugStat>();
		// An augmentation id contains 2 short vaues so we gotta seperate them here
		// both values contain a number from 1-16380, the first 14560 values are stats
		// the 14560 stats are devided into 4 blocks each holding 3640 values
		// each block contains 40 subblocks holding 91 stat values
		// the first 13 values are so called Solo-stats and they have the highest stat increase possible
		// after the 13 Solo-stats come 78 combined stats (thats every possible combination of the 13 solo stats)
		// the first 12 combined stats (14-26) is the stat 1 combined with stat 2-13
		// the next 11 combined stats then are stat 2 combined with stat 3-13 and so on...
		// to get the idea have a look @ optiondata_client-e.dat - thats where the data came from :)
		int stats[] = new int[2];
		stats[0] = 0x0000FFFF & augmentationId;
		stats[1] = (augmentationId >> 16);

		for (int i = 0; i < 2; i++)
		{
			// its a stat
			if (stats[i] >= STAT_START && stats[i] <= STAT_END)
			{
				int block = 0;
				while (stats[i] > STAT_BLOCKSIZE)
				{
					stats[i] -= STAT_BLOCKSIZE;
					block++;
				}

				int subblock = 0;
				while (stats[i] > STAT_SUBBLOCKSIZE)
				{
					stats[i] -= STAT_SUBBLOCKSIZE;
					subblock++;
				}

				if (stats[i] < 14) // solo stat
				{
					AugmentationStat as = ((AugmentationStat) _augmentationStats[block].get((stats[i] - 1)));
					temp.add(new AugStat(as.getStat(), as.getSingleStatValue(subblock)));
				}
				else
				// twin stat
				{
					stats[i] -= 13; // rescale to 0 (if first of first combined block)
					int x = 12; // next combi block has 12 stats
					int rescales = 0; // number of rescales done

					while (stats[i] > x)
					{
						stats[i] -= x;
						x--;
						rescales++;
					}
					// get first stat
					AugmentationStat as = ((AugmentationStat) _augmentationStats[block].get(rescales));
					if (rescales == 0)
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock)));
					else
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue((subblock * 2) + 1)));

					// get 2nd stat
					as = ((AugmentationStat) _augmentationStats[block].get(rescales + stats[i]));
					if (as.getStat() == Stats.CRITICAL_DAMAGE)
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock)));
					else
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock * 2)));
				}
			}
			// its a base stat
			else if (stats[i] >= BASESTAT_STR && stats[i] <= BASESTAT_MEN)
			{
				switch (stats[i])
				{
				case BASESTAT_STR:
					temp.add(new AugStat(Stats.STAT_STR, 1.0f));
					break;
				case BASESTAT_CON:
					temp.add(new AugStat(Stats.STAT_CON, 1.0f));
					break;
				case BASESTAT_INT:
					temp.add(new AugStat(Stats.STAT_INT, 1.0f));
					break;
				case BASESTAT_MEN:
					temp.add(new AugStat(Stats.STAT_MEN, 1.0f));
					break;
				}
			}
		}

		return temp;
	}
}