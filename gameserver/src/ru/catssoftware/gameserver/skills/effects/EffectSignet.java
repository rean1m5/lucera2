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
package ru.catssoftware.gameserver.skills.effects;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2EffectPointInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.skills.Env;
import ru.catssoftware.gameserver.skills.l2skills.L2SkillSignet;
import ru.catssoftware.gameserver.skills.l2skills.L2SkillSignetCasttime;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;

/**
 * @authors Forsaiken, Sami
 */
public final class EffectSignet extends L2Effect
{
	private L2Skill					_skill;
	private L2EffectPointInstance	_actor;

	public EffectSignet(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SIGNET_EFFECT;
	}

	@Override
	public boolean onStart()
	{
		if (getSkill() instanceof L2SkillSignet)
			_skill = SkillTable.getInstance().getInfo(((L2SkillSignet) getSkill()).effectId, getLevel());
		else if (getSkill() instanceof L2SkillSignetCasttime)
			_skill = SkillTable.getInstance().getInfo(((L2SkillSignetCasttime) getSkill()).effectId, getLevel());
		_actor = (L2EffectPointInstance) getEffected();
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		if (_skill == null)
			return true;

		int mpConsume = _skill.getMpConsume();

		if (mpConsume > getEffector().getStatus().getCurrentMp())
		{
			getEffector().sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
			return false;
		}

		getEffector().reduceCurrentMp(mpConsume);

		for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if (cha == null)
				continue;
			_skill.getEffects(_actor, cha);
			// there doesn't seem to be a visible effect with MagicSkillLaunched packet...
			_actor.broadcastPacket(new MagicSkillUse(_actor, cha, _skill.getId(), _skill.getLevel(), 0, 0, false));
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if (_actor != null)
		{
			_actor.deleteMe();
		}
	}
}