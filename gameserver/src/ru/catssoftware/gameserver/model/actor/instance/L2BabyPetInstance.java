package ru.catssoftware.gameserver.model.actor.instance;

import javolution.util.FastList;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.PetDataTable;
import ru.catssoftware.gameserver.datatables.PetSkillsTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

import java.util.concurrent.Future;


public class L2BabyPetInstance extends L2PetInstance
{
	private Future<?>		_healingTask;
	private Future<?>		_rechargeTask;
	private Future<?>		_buffTask;
	protected static int	_strongHeal = 4718;
	protected static int	_weakHeal = 4717;
	protected static int	_recharge = 5200;
	

	public L2BabyPetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner, control);

		int npcID = template.getIdTemplate();

		if (PetDataTable.isBaby(npcID))
		{
			_strongHeal = 4718;
			_weakHeal = 4717;
			// start the healing task
			_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 3000, 1000);
			_rechargeTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Recharge(this), 3000, 1000);
		}

		//improved baby pets
		else if (PetDataTable.isImprovedBaby(npcID))
		{
			_strongHeal = 5590;
			_weakHeal = 5195;

			// start the healing task
			_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 3000, 1000);
			_rechargeTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Recharge(this), 3000, 1000);
			
			// buff task
			_buffTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Buff(this), 5000, 4000);
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}

		if (_buffTask != null)
		{
			_buffTask.cancel(false);
			_buffTask = null;
		}
		
		if (_rechargeTask != null)
		{
			_rechargeTask.cancel(false);
			_rechargeTask = null;
		}		
		return true;
	}

	@Override
	public synchronized void deleteMe(L2PcInstance owner)
	{
		super.deleteMe(owner);
		if (_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}

		if (_buffTask != null)
		{
			_buffTask.cancel(false);
			_buffTask = null;
		}
		
		if (_rechargeTask != null)
		{
			_rechargeTask.cancel(false);
			_rechargeTask = null;
		}
	}

	@Override
	public synchronized void unSummon(L2PcInstance owner)
	{
		super.unSummon(owner);

		if (_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}

		if (_buffTask != null)
		{
			_buffTask.cancel(false);
			_buffTask = null;
		}
		
		if (_rechargeTask != null)
		{
			_rechargeTask.cancel(false);
			_rechargeTask = null;
		}		
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		if (PetDataTable.isBaby(getTemplate().getIdTemplate()) || PetDataTable.isImprovedBaby(getTemplate().getIdTemplate()))
		{
			if (_rechargeTask == null)
				_rechargeTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Recharge(this), 0, 1000);
		
			if (_healingTask == null)
				_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 0, 1000);		
		}
		//improved baby pets
		if (PetDataTable.isImprovedBaby(getTemplate().getIdTemplate()) && _buffTask == null)
				_buffTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Buff(this), 3000, 4000);
	}

	private class Heal implements Runnable
	{
		private L2BabyPetInstance	_baby;

		public Heal(L2BabyPetInstance baby)
		{
			_baby = baby;
		}

		public void run()
		{
			L2PcInstance owner = _baby.getOwner();
			if (!owner.isDead() && !_baby.isCastingNow() && !_baby.isOutOfControl())
			{
				boolean previousFollowStatus = _baby.getFollowStatus();

				L2Skill skill = null;
				if ((owner.getCurrentHp() / owner.getMaxHp() < 0.20) && Rnd.get(100) <= 75)
					skill = SkillTable.getInstance().getInfo(_strongHeal, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, _strongHeal));
				if (((owner.getCurrentHp() / owner.getMaxHp() < 0.8) && Rnd.get(100) <= 25))
					skill = SkillTable.getInstance().getInfo(_weakHeal, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, _weakHeal));
				if (skill != null && _baby.getStatus().getCurrentMp() >= skill.getMpConsume() && !_baby.isSkillDisabled(skill))
					_baby.useMagic(skill, false, false);
				if (previousFollowStatus != _baby.getFollowStatus())
					setFollowStatus(previousFollowStatus);
			}
		}
	}
	
	private class Recharge implements Runnable
	{
		private L2BabyPetInstance	_baby;

		public Recharge(L2BabyPetInstance baby)
		{
			_baby = baby;
		}

		public void run()
		{
			L2PcInstance owner = _baby.getOwner();
			if (!owner.isDead() && !_baby.isCastingNow() && !_baby.isOutOfControl())
			{
				boolean previousFollowStatus = _baby.getFollowStatus();

				L2Skill skill = null;
				if ((owner.getCurrentMp() / owner.getMaxMp() < 0.60))
					skill = SkillTable.getInstance().getInfo(_recharge, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, _recharge));
				
				if (skill != null && _baby.getStatus().getCurrentMp() >= skill.getMpConsume() && !_baby.isSkillDisabled(skill))
					_baby.useMagic(skill, false, false);
				
				if (previousFollowStatus != _baby.getFollowStatus())
					setFollowStatus(previousFollowStatus);
			}
		}
	}	

	private class Buff implements Runnable
	{
		private L2BabyPetInstance	_baby;

		public Buff(L2BabyPetInstance baby)
		{
			_baby = baby;
		}

		public void run()
		{
			L2PcInstance owner = _baby.getOwner();

			if (!owner.isDead() && !_baby.isCastingNow() && !_baby.isOutOfControl())
			{
				boolean previousFollowStatus = _baby.getFollowStatus();

				FastList<Integer> skillId = new FastList<Integer>();
				for (int id : PetSkillsTable.getInstance().getAvailableSkills(_baby))
				{
					boolean isEffected = false;

					//remove heal skills
					if (id == _strongHeal || id == _weakHeal)
						continue;

					int _lvl = PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, id);
					L2Skill skill = SkillTable.getInstance().getInfo(id, _lvl);
					if (skill == null || _lvl == 0 || skill.getEffectTempate() == null || _baby.isSkillDisabled(skill))
						continue;

					String stackType = skill.getEffectTempate()[0].stackType;
					float stackOrder = skill.getEffectTempate()[0].stackOrder;

					//remove effected - player already have
					L2Effect[] effects = owner.getAllEffects();
					for (L2Effect e : effects)
					{
						if (e.getStackType().equals(stackType) && e.getStackOrder() >= stackOrder)
						{
							isEffected = true;
							break;
						}

						if (e.getSkill().getId() == id)
						{
							//remaining time < 60 sec rebuff 
							if (e.getRemainingTaskTime() > 60)
								isEffected = true;
							break;
						}
					}
					if (!isEffected)
						skillId.add(id);
				}

				if (skillId.size() == 0)
					return;

				int randomSkill = skillId.get(Rnd.get(skillId.size()));
				L2Skill skill = null;
				skill = SkillTable.getInstance().getInfo(randomSkill, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, randomSkill));

				if (skill != null && _baby.getStatus().getCurrentMp() >= skill.getMpConsume() && !_baby.isSkillDisabled(skill))
				{
					_baby.useMagic(skill, false, false);
					SystemMessage msg = new SystemMessage(SystemMessageId.PET_USES_S1);
					msg.addSkillName(skill);
					owner.sendPacket(msg);
				}

				if (previousFollowStatus != _baby.getFollowStatus())
					setFollowStatus(previousFollowStatus);
			}
		}
	}
}