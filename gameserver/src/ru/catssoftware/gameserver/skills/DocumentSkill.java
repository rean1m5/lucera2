package ru.catssoftware.gameserver.skills;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.conditions.Condition;
import ru.catssoftware.gameserver.skills.effects.EffectTemplate;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.util.ObjectPool;
import ru.catssoftware.util.StatsSet;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mkizub
 */
final class DocumentSkill extends DocumentBase
{
	private static final class StatsSetPool extends ObjectPool<StatsSet>
	{
		private StatsSetPool()
		{
			super(false);
		}
		
		@Override
		protected StatsSet create()
		{
			return new StatsSet();
		}
		
		@Override
		protected void reset(StatsSet set)
		{
			set.getSet().clear();
		}
		
		private StatsSet getSkillSet(int level)
		{
			StatsSet set = get();
			
			set.set("skill_id", _currentSkillId);
			set.set("level", level);
			set.set("name", _currentSkillName);
			
			return set;
		}
	}
	
	private static final StatsSetPool STATS_SET_POOL = new StatsSetPool();
	
	private static final String[] VALID_NODE_NAMES =
	{
		"set", "for", "cond",
		"enchant1", "enchant1for", "enchant1cond",
		"enchant2", "enchant2for", "enchant2cond",
	};
	
	private static int _currentSkillId;
	private static int _currentSkillLevel;
	private static String _currentSkillName;
	
	private static final Map<String, String[]> _tables = new ConcurrentHashMap<String, String[]>();
	
	private static final List<StatsSet> _sets = Collections.synchronizedList(new ArrayList<StatsSet>());
	private static final List<StatsSet> _enchsets1 = Collections.synchronizedList(new ArrayList<StatsSet>());
	private static final List<StatsSet> _enchsets2 = Collections.synchronizedList(new ArrayList<StatsSet>());

	
	private static final List<L2Skill> _skills = new ArrayList<L2Skill>();
	
	private final List<L2Skill> _skillsInFile = new ArrayList<L2Skill>();
	
	DocumentSkill(File file)
	{
		super(file);
	}
	
	List<L2Skill> getSkills()
	{
		return _skillsInFile;
	}
	
	@Override
	String getTableValue(String value, Object template)
	{
		if (template instanceof Integer)
			return _tables.get(value)[(Integer)template - 1];
		else
			return _tables.get(value)[_currentSkillLevel];
	}
	
	@Override
	String getDefaultNodeName()
	{
		return "skill";
	}
	
	@Override
	void parseDefaultNode(Node n)
	{
		try
		{
			parseSkill(n);
			
			_skillsInFile.addAll(_skills);
		}
		catch (Exception e)
		{
			_log.warn("Error while parsing skill id " + _currentSkillId + ", level " + (_currentSkillLevel + 1), e);
		}
		finally
		{
			_currentSkillId = 0;
			_currentSkillLevel = 0;
			_currentSkillName = null;
			
			_tables.clear();
			
			clear(_sets);
			clear(_enchsets1);
			clear(_enchsets2);
			
			_skills.clear();
		}
	}
	
	private void clear(List<StatsSet> statsSets)
	{
		for (StatsSet set : statsSets)
			STATS_SET_POOL.store(set);
		
		statsSets.clear();
	}
	
	private void parseSkill(Node n) throws Exception
	{
		final NamedNodeMap attrs = n.getAttributes();
		
		_currentSkillId = Integer.decode(attrs.getNamedItem("id").getNodeValue());
		_currentSkillName = attrs.getNamedItem("name").getNodeValue();
		
		final int levels = getLevel(attrs, "levels", null);
		final int enchantLevels1 = getLevel(attrs, "enchantLevels1", 0);
		final int enchantLevels2 = getLevel(attrs, "enchantLevels2", 0);
		
		final Node first = n.getFirstChild();
		
		node_loop: for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
				
				if (name.charAt(0) != '#')
					throw new IllegalStateException("Table name must start with '#'!");
				
				StringTokenizer st = new StringTokenizer(n.getFirstChild().getNodeValue());
				
				String[] table = new String[st.countTokens()];
				
				for (int i = 0; i < table.length; i++)
					table[i] = st.nextToken();
				
				_tables.put(name, table);
			}
			else if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				final String name = n.getNodeName();
				
				for (String validName : VALID_NODE_NAMES)
					if (validName.equals(name))
						continue node_loop;
				
				throw new IllegalStateException("Invalid tag <" + n.getNodeName() + ">");
			}
		}
		
		parseBeanSets(first, levels, 1, _sets, "set");
		parseBeanSets(first, enchantLevels1, 101, _enchsets1, "enchant1");
		parseBeanSets(first, enchantLevels2, 141, _enchsets2, "enchant2");
		
		makeSkills(_sets);
		makeSkills(_enchsets1);
		makeSkills(_enchsets2);
		
		int startLvl = 0;
		
		attach(first, startLvl += 0, levels, "cond", "for");
		attach(first, startLvl += levels, enchantLevels1, "enchant1cond", "enchant1for");
		attach(first, startLvl += enchantLevels1, enchantLevels2, "enchant2cond", "enchant2for");

	}
	
	private int getLevel(NamedNodeMap attrs, String nodeName, Integer defaultValue)
	{
		if (attrs.getNamedItem(nodeName) != null)
			return Integer.decode(attrs.getNamedItem(nodeName).getNodeValue());
		
		return defaultValue.intValue();
	}
	
	private void parseBeanSets(Node first, int length, int startLvl, List<StatsSet> statsSets, String setName)
	{
		for (int i = 0; i < length; i++)
		{
			StatsSet set = STATS_SET_POOL.getSkillSet(i + startLvl);
			
			statsSets.add(set);
			
			if (startLvl >= 100)
			{
				for (Node n = first; n != null; n = n.getNextSibling())
				{
					if ("set".equalsIgnoreCase(n.getNodeName()))
						parseBeanSet(n, set, _sets.size());
				}
			}
			
			for (Node n = first; n != null; n = n.getNextSibling())
			{
				if (setName.equalsIgnoreCase(n.getNodeName()))
					parseBeanSet(n, set, i + 1);
			}
		}
	}
	
	private void parseBeanSet(Node n, StatsSet set, int level)
	{
		String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		
		set.set(name, getValue(value, level));
	}
	
	private void makeSkills(List<StatsSet> statsSets) throws Exception
	{
		for (StatsSet set : statsSets)
			_skills.add(set.getEnum("skillType", L2SkillType.class).makeSkill(set));
	}
	
	private void attach(final Node first, final int startLvl, final int length, String condName, String forName)
	{
		for (int i = 0; i < length; i++)
		{
			final L2Skill skill = _skills.get(startLvl + i);
			
			_currentSkillLevel = i;
			
			boolean found = false;
			for (Node n = first; n != null; n = n.getNextSibling())
			{
				if (condName.equalsIgnoreCase(n.getNodeName()))
				{
					found = true;
					skill.attach(parseConditionWithMessage(n, skill));
				}
				else if (forName.equalsIgnoreCase(n.getNodeName()))
				{
					found = true;
					parseTemplate(n, skill);
				}
			}
			
			if (!found && startLvl > 0)
			{
				_currentSkillLevel = _sets.size() - 1;
				
				for (Node n = first; n != null; n = n.getNextSibling())
				{
					if ("cond".equalsIgnoreCase(n.getNodeName()))
					{
						skill.attach(parseConditionWithMessage(n, skill));
					}
					else if ("for".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, skill);
					}
				}
			}
		}
	}
	
	@Override
	void parseTemplateNode(Node n, Object template, Condition condition)
	{
		if ("effect".equalsIgnoreCase(n.getNodeName()))
			attachEffect(n, template, condition);
		else
			super.parseTemplateNode(n, template, condition);
	}
	
	final void attachEffect(Node n, Object template, Condition attachCond)
	{
		if (!(template instanceof L2Skill))
			throw new IllegalStateException("Attaching an effect to a non-L2Skill template");
		
		final L2Skill skill = (L2Skill)template;
		final NamedNodeMap attrs = n.getAttributes();
		
		final String name = attrs.getNamedItem("name").getNodeValue();
		
		int count = 1;
		if (attrs.getNamedItem("count") != null)
			count = Integer.decode(getValue(attrs.getNamedItem("count").getNodeValue(), template));
		
		count = Math.max(1, count);
		
		int time = Integer.decode(getValue(attrs.getNamedItem("time").getNodeValue(), template)) * skill.getTimeMulti();
		
		if (Config.ENABLE_MODIFY_SKILL_DURATION)
		{
			if (Config.SKILL_DURATION_LIST.containsKey(skill.getId()))
				if (skill.getLevel() < 100)
					time = Config.SKILL_DURATION_LIST.get(skill.getId());
				else if ((skill.getLevel() >= 100) && (skill.getLevel() < 140))
					time += Config.SKILL_DURATION_LIST.get(skill.getId());
				else if (skill.getLevel() > 140)
					time = Config.SKILL_DURATION_LIST.get(skill.getId());
		}

		boolean self = false;
		if (attrs.getNamedItem("self") != null)
			self = (Integer.decode(getValue(attrs.getNamedItem("self").getNodeValue(), template)) == 1);
		
		boolean showIcon = true;
		if (attrs.getNamedItem("noicon") != null)
			showIcon = !(Integer.decode(getValue(attrs.getNamedItem("noicon").getNodeValue(), template)) == 1);
		
		final String lambda = getLambda(n, template);
		
		int abnormal = 0;
		if (attrs.getNamedItem("abnormal") != null)
		{
			String abn = attrs.getNamedItem("abnormal").getNodeValue();
			
			if (abn.equalsIgnoreCase("bleeding"))
				abnormal |= AbnormalEffect.BLEEDING.getMask();
			else if (abn.equalsIgnoreCase("poison"))
				abnormal |= AbnormalEffect.POISON.getMask();
			else if (abn.equalsIgnoreCase("redcircle"))
				abnormal |= AbnormalEffect.REDCIRCLE.getMask();
			else if (abn.equalsIgnoreCase("ice"))
				abnormal |= AbnormalEffect.ICE.getMask();
			else if (abn.equalsIgnoreCase("wind"))
				abnormal |= AbnormalEffect.WIND.getMask();
			else if (abn.equalsIgnoreCase("flame"))
				abnormal |= AbnormalEffect.FLAME.getMask();
			else if (abn.equalsIgnoreCase("stun"))
				abnormal |= AbnormalEffect.STUN.getMask();
			else if (abn.equalsIgnoreCase("mute"))
				abnormal |= AbnormalEffect.MUTED.getMask();
			else if (abn.equalsIgnoreCase("root"))
				abnormal |= AbnormalEffect.ROOT.getMask();
			else if (abn.equalsIgnoreCase("bighead"))
				abnormal |= AbnormalEffect.BIG_HEAD.getMask();
			else if (abn.equalsIgnoreCase("stealth"))
				abnormal |= AbnormalEffect.STEALTH.getMask();
			else if (abn.equalsIgnoreCase("earthquake"))
				abnormal |= AbnormalEffect.EARTHQUAKE.getMask();
			else if (abn.equalsIgnoreCase("invul"))
				abnormal |= AbnormalEffect.INVULNERABLE.getMask();
			else
				throw new IllegalStateException("Invalid abnormal value: '" + abn + "'!");
		}
		
		final String stackType;
		if (attrs.getNamedItem("stackType") != null)
			stackType = attrs.getNamedItem("stackType").getNodeValue();
		else
			stackType = skill.generateUniqueStackType();
		
		float stackOrder = 0;
		if (attrs.getNamedItem("stackOrder") != null)
			stackOrder = Float.parseFloat(getValue(attrs.getNamedItem("stackOrder").getNodeValue(), template));

		int trigId = 0;
		if (attrs.getNamedItem("triggeredId") != null)
			trigId = Integer.parseInt(getValue(attrs.getNamedItem("triggeredId").getNodeValue(), template));

		int trigLvl = 1;
		if (attrs.getNamedItem("triggeredLevel") != null)
			trigLvl = Integer.parseInt(getValue(attrs.getNamedItem("triggeredLevel").getNodeValue(), template));

		int skillId = skill.getId();
		if (attrs.getNamedItem("iconId") != null)
			skillId = Integer.parseInt(attrs.getNamedItem("iconId").getNodeValue());
		
		EffectTemplate effectTemplate = new EffectTemplate(attachCond, name, Double.parseDouble(lambda), count, time, abnormal, stackType,
			stackOrder, showIcon, trigId, trigLvl,skillId);
				
		parseTemplate(n, effectTemplate);
		
		if (!self)
			skill.attach(effectTemplate);
		else
			skill.attachSelf(effectTemplate);
	}
}
