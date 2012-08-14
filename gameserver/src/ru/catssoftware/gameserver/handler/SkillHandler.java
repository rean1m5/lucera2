package ru.catssoftware.gameserver.handler;

import java.lang.reflect.Constructor;
import java.util.Map;

import javolution.util.FastMap;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.util.HandlerRegistry;
import ru.catssoftware.util.JarUtils;


public final class SkillHandler extends HandlerRegistry<L2SkillType, ISkillHandler> implements ISkillHandler
{
	private static SkillHandler _instance;
	
	public static SkillHandler getInstance()
	{
		if (_instance == null)
			_instance = new SkillHandler();
		
		return _instance;
	}

	private SkillHandler()
	{
		try {
			for(String handler : JarUtils.enumClasses("ru.catssoftware.gameserver.handler.skillhandlers")) try {
				Class<?> _handler = Class.forName(handler);
				if(_handler!=null && ISkillHandler.class.isAssignableFrom(_handler)) {
					Constructor<?> ctor = _handler.getConstructor();
					if(ctor!=null) 
						registerSkillHandler((ISkillHandler)ctor.newInstance());
				}
			} catch(Exception e) {
				continue;
			}
		} catch(Exception e) {
		
		}
		
		HandlerRegistry._log.info("SkillHandler: Loaded " + size() + " handlers.");
	}

	private Map<Integer, ICustomSkillHandler> _customSkills = new FastMap<Integer, ICustomSkillHandler>();
	public void registerSkillHandler(ISkillHandler handler)
	{
		registerAll(handler, handler.getSkillIds());
	}

	public void registerCustomSkill(ICustomSkillHandler handler) {
		for(int i : handler.getSkills())
			_customSkills.put(i, handler);
	}
	public void handleCustomSkill(L2Skill skill,L2Character caster, L2Character...targets) {
		ICustomSkillHandler h = _customSkills.get(skill.getId());
		if(h!=null)
			h.useSkill(caster, skill, targets);
	}
	public ISkillHandler getSkillHandler(L2SkillType skillType)
	{
		ISkillHandler handler = get(skillType);
		
		return handler == null ? this : handler;
	}

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		skill.useSkill(activeChar, targets);
	}
	
	public L2SkillType[] getSkillIds()
	{
		return null;
	}
}
