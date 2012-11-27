package ru.catssoftware.gameserver.model;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.xml.AugmentationData;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.SkillCoolTime;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.funcs.FuncAdd;
import ru.catssoftware.gameserver.skills.funcs.FuncOwner;
import javolution.util.FastList;


public final class L2Augmentation
{
	private int						_effectsId	= 0;
	private AugmentationStatBoni	_boni		= null;
	private L2Skill					_skill		= null;

	public L2Augmentation(int effects, L2Skill skill)
	{
		_effectsId = effects;
		_boni = new AugmentationStatBoni(_effectsId);
		_skill = skill;
	}

	public L2Augmentation(int effects, int skill, int skillLevel)
	{
		this(effects, SkillTable.getInstance().getInfo(skill, skillLevel));
	}

	public final class AugmentationStatBoni implements FuncOwner
	{
		private Stats	_stats[];
		private float	_values[];
		private boolean	_active;

		public AugmentationStatBoni(int augmentationId)
		{
			_active = false;
			FastList<AugmentationData.AugStat> as = AugmentationData.getInstance().getAugStatsById(augmentationId);

			_stats = new Stats[as.size()];
			_values = new float[as.size()];

			int i = 0;
			for (AugmentationData.AugStat aStat : as)
			{
				_stats[i] = aStat.getStat();
				_values[i] = aStat.getValue();
				i++;
			}
		}

		public void applyBonus(L2PcInstance player)
		{
			// make sure the bonuses are not applied twice..
			if (_active)
				return;

			for (int i = 0; i < _stats.length; i++)
				player.addStatFunc(new FuncAdd(_stats[i], 0x40, this, _values[i], null));

			_active = true;
		}

		public void removeBonus(L2PcInstance player)
		{
			// make sure the bonuses are not removed twice
			if (!_active)
				return;

			player.removeStatsOwner(this);

			_active = false;
		}

		public String getFuncOwnerName()
		{
			return null;
		}

		public L2Skill getFuncOwnerSkill()
		{
			return null;
		}
	}

	public int getAttributes()
	{
		return _effectsId;
	}

	/**
	 * Get the augmentation "id" used in serverpackets.
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	/**
	 * Applies the bonuses to the player.
	 * @param player
	 */
	public void applyBonus(L2PcInstance player)
	{
		_boni.applyBonus(player);

		boolean updateTimeStamp = false;
		// add the skill if any
		if (_skill != null)
		{
			player.addSkill(_skill);
			if (_skill.isActive())
			{
				if (!player.isSkillDisabled(_skill))
				{
					int equipDelay = _skill.getEquipDelay();
					if (equipDelay > 0)
					{
						player.disableSkill(_skill, equipDelay);
						updateTimeStamp = true;
					}
				}
			}
			player.sendSkillList();
			if (updateTimeStamp)
				player.sendPacket(new SkillCoolTime(player));
		}
	}

	/**
	 * Удаление бонуса аугументации игрока
	 * Исправлено для CatsSoftware
	 * Активные скилы не отменяют эффекта
	 * @param player
	 */
	public void removeBonus(L2PcInstance player)
	{
		// Удаление бонуса
		_boni.removeBonus(player);
		// Удаление скила
		if (_skill != null)
		{
			if(_skill.isPassive())
				player.removeSkill(_skill,false,true);
			else
			// Эфекты активного скила не отменяются
				if (!Config.CANCEL_AUGUMENTATION_EFFECT)
					player.removeSkill(_skill, false,false);
				else
					player.removeSkill(_skill,false,true);
			player.sendSkillList();
		}
	}
}