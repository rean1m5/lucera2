package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

public class EffectFusion extends L2Effect
{
	public int	_effect;
	public int	_maxEffect;
	L2Skill		skill;

	public EffectFusion(Env env, EffectTemplate template)
	{
		super(env, template);
		_effect = getSkill().getLevel();
		_maxEffect = 10;
	}

	@Override
	public boolean onActionTime()
	{
		return true;
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FUSION;
	}

	public void increaseEffect()
	{
		if (_effect < _maxEffect)
		{
			_effect++;
			updateBuff();
		}
	}

	public void decreaseForce()
	{
		_effect--;
		if (_effect < 1)
			exit();
		else
			updateBuff();
	}

	private void updateBuff()
	{
		// Удаляем предыдущий скил
		exit();
		// Собираем информацию скила
		if (getSkill() != null)
			skill = SkillTable.getInstance().getInfo(getSkill().getId(), _effect);
		// Вешаем новый скилл на цель
		if (skill != null)
			skill.getEffects(getEffector(), getEffected());
	}
}