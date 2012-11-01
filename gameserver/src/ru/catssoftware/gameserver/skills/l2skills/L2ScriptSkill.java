package ru.catssoftware.gameserver.skills.l2skills;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.util.StatsSet;

public class L2ScriptSkill extends L2Skill {
	public static interface IScriptSkillHandler {
		public void useSkill(L2Skill skill, L2Character caster, L2Character... targets);
	}
	
	private String _className;
	private IScriptSkillHandler _handler;

	public L2ScriptSkill(StatsSet set)
	{
		super(set);
		_className = set.getString("className");
	}
	
	@Override
	public void useSkill(L2Character caster, L2Character... targets) {
		try {
			if(_handler==null)  {
				Class<?> skillClass = Class.forName(_className);
				if(skillClass!=null)
					_handler = (IScriptSkillHandler)(skillClass.getConstructor().newInstance());
			}
			_handler.useSkill(this, caster, targets);
		} catch(Exception e) {
			if(caster.isPlayer())
				caster.sendMessage(String.format(Message.getMessage(((L2PcInstance) caster), Message.MessageId.MSG_SKILL_NOT_IMPLEMENTED), getId()));
			_log.warn("Skill: Error while casting "+getId(),e);
		}
	}

}
