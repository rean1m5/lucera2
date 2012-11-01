package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.ai.L2SiegeGuardAI;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2SiegeClan;
import ru.catssoftware.gameserver.model.L2SiegeGuard;
import ru.catssoftware.gameserver.model.actor.knownlist.SiegeGuardKnownList;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2SiegeGuardInstance extends L2SiegeGuard
{
	public L2SiegeGuardInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // inits the knownlist
	}

	@Override
	public SiegeGuardKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new SiegeGuardKnownList(this);
		return (SiegeGuardKnownList) _knownList;
	}

	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai;
		if (ai == null)
		{
			synchronized(this)
			{
				if (_ai == null)
					_ai = new L2SiegeGuardAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// Summons and traps are attackable, too
		L2PcInstance player = attacker.getPlayer();
		if (player == null)
			return false;
		if (player.getClan() == null)
			return true;
		if (DevastatedCastleSiege.getInstance().checkIsRegistered(player.getClan()) && DevastatedCastleSiege.getInstance().getIsInProgress())
			return true;
		if (FortressOfDeadSiege.getInstance().checkIsRegistered(player.getClan()) && FortressOfDeadSiege.getInstance().getIsInProgress())
			return true;
		boolean isCastle = (getCastle() != null && getCastle().getSiege().getIsInProgress() && !getCastle().getSiege().checkIsDefender(player.getClan()));
		return isCastle;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	public void returnHome()
	{
		if (getStat().getWalkSpeed() <= 0)
			return;

		if (getSpawn() != null && !isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), 40, false))
		{
			setisReturningToSpawnPoint(true);
			clearAggroList();
			if (hasAI())
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
		}
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		if (Config.SIEGE_ONLY_REGISTERED)
		{
			boolean opp = false;
			Siege siege = SiegeManager.getInstance().getSiege(player);
			FortSiege fortSiege = FortSiegeManager.getInstance().getSiege(player);
			L2Clan oppClan = player.getClan();
			//Castle Sieges
			if (siege != null && siege.getIsInProgress() && oppClan != null)
			{
				for (L2SiegeClan clan : siege.getAttackerClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == oppClan || cl.getAllyId() == player.getAllyId())
					{
						opp = true;
						break;
					}
				}
				for (L2SiegeClan clan : siege.getDefenderClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == oppClan || cl.getAllyId() == player.getAllyId())
					{
						opp = true;
						break;
					}
				}
			}
			//Fort Sieges
			else if (fortSiege != null && fortSiege.getIsInProgress() && oppClan != null)
			{
				for (L2SiegeClan clan : fortSiege.getAttackerClans())
				{
					L2Clan cl = ClanTable.getInstance().getClan(clan.getClanId());

					if (cl == oppClan || cl.getAllyId() == player.getAllyId())
					{
						opp = true;
						break;
					}
				}
			}
			else if (oppClan != null && DevastatedCastleSiege.getInstance().checkIsRegistered(oppClan) && DevastatedCastleSiege.getInstance().getIsInProgress())
				opp = true;
			else if (oppClan != null && FortressOfDeadSiege.getInstance().checkIsRegistered(oppClan) && FortressOfDeadSiege.getInstance().getIsInProgress())
				opp = true;

			if (!opp)
				return;
		}

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
			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			boolean AutoAT = isAutoAttackable(player);
			if (AutoAT && !isAlikeDead())
			{
				if (Math.abs(player.getZ() - getZ()) < 600) // this max heigth difference might need some tweaking
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				else
					player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			if (!AutoAT)
			{
				if (!canInteract(player))
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				else
					showChatWindow(player, 0);
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	@Override
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if (attacker == null)
			return;

		if (!(attacker instanceof L2SiegeGuardInstance))
			super.addDamageHate(attacker, damage, aggro);
	}
}