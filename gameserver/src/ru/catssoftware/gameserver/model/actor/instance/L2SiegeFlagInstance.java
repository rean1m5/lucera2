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
package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2SiegeClan;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2SiegeFlagInstance extends L2NpcInstance
{
	private L2Clan _clan;
	private L2PcInstance _player;
	private Siege _siege;
	private FortSiege _fortSiege;
	private boolean _isAdvanced;
	private boolean _canTalk;
	private boolean _autoSet;

	public L2SiegeFlagInstance(L2PcInstance player, int objectId, L2NpcTemplate template, boolean advanced, boolean autoSet,L2Clan clan)
	{
		super(objectId, template);

		_isAdvanced = advanced;
		_player = player;
		if (autoSet==true)
		{
			_siege = null;
			_fortSiege = null;
			_clan = clan;
		}
		else
		{
			_siege = SiegeManager.getInstance().getSiege(_player);
			_fortSiege = FortSiegeManager.getInstance().getSiege(_player);
			_clan = player.getClan();
		}
		_canTalk = true;
		_autoSet =autoSet;
		
		if (autoSet == false)
		{
			if (_clan == null || (_siege == null && _fortSiege == null))
				deleteMe();
			else if (_siege != null && _fortSiege == null)
			{
				L2SiegeClan sc = _siege.getAttackerClan(_player.getClan());
				if (sc == null)
					deleteMe();
				else
					sc.addFlag(this);
			}
			else if (_siege == null && _fortSiege != null)
			{
				L2SiegeClan sc = _fortSiege.getAttackerClan(_player.getClan());
				if (sc == null)
					deleteMe();
				else
					sc.addFlag(this);
			}
		}
	}

	@Override
	public boolean isAttackable()
	{
		if (_autoSet == true)
			return false;
		return true;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker) 
	{
		if(attacker instanceof L2PcInstance) {
			L2PcInstance pc = (L2PcInstance)attacker;
			return _clan != pc.getClan();
		}
		return true;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		if (_siege != null)
		{
			L2SiegeClan sc = _siege.getAttackerClan(_player.getClan());
			if (sc != null)
				sc.removeFlag(this);
		}
		else if (_fortSiege != null)
		{
			L2SiegeClan sc = _fortSiege.getAttackerClan(_player.getClan());
			if (sc != null)
				sc.removeFlag(this);
		}

		return true;
	}

	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (_autoSet==true)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if(!_player.canBeTargetedByAtSiege(player) && Config.SIEGE_ONLY_REGISTERED)
			return;

		if (player == null || !canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int)getStatus().getCurrentHp() );
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp() );
			player.sendPacket(su);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			else
				player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		// Advanced Headquarters have double HP.
		if(_isAdvanced)
			 damage /= 2;

		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);

		if (canTalk())
		{
			if (getCastle() != null && getCastle().getSiege().getIsInProgress())
			{
				if (_clan != null)
				{
					// send warning to owners of headquarters that theirs base is under attack
					_clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.BASE_UNDER_ATTACK));
					setCanTalk(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTalkTask(), 20000);
				}
			}
			else if (getFort() != null && getFort().getSiege().getIsInProgress())
			{
				if (_clan != null)
				{
					// send warning to owners of headquarters that theirs base is under attack
					_clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.BASE_UNDER_ATTACK));
					setCanTalk(false);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTalkTask(), 20000);
				}
			}
		}
	}

	private class ScheduleTalkTask implements Runnable
	{
		public void run()
		{
			setCanTalk(true);
		}
	}

	void setCanTalk(boolean val)
	{
		_canTalk = val;
	}

	private boolean canTalk()
	{
		return _canTalk;
	}
}