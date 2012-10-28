package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.BanditStrongholdSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.WildBeastFarmSiege;
import ru.catssoftware.gameserver.model.L2SiegeClan;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.lang.RunnableImpl;


public class RequestRestartPoint extends L2GameClientPacket
{
	private static final String	_C__6d_REQUESTRESTARTPOINT	= "[C] 6d RequestRestartPoint";
	protected int				_requestedPointType;
	protected boolean			_continuation;

	@Override
	protected void readImpl()
	{
		_requestedPointType = readD();
	}

	private class DeathTask extends RunnableImpl
	{
		private final L2PcInstance	activeChar;

		public DeathTask(L2PcInstance _activeChar)
		{
			activeChar = _activeChar;
		}

		@Override
		public void runImpl()
		{
			try
			{
				Location loc = null;
				Siege siege = null;
				FortSiege fsiege = null;

				if (activeChar.isInJail())
					_requestedPointType = 27;
				else if (activeChar.isFestivalParticipant())
					_requestedPointType = 5;
				if(activeChar.getGameEvent() !=null && activeChar.getGameEvent().isRunning()) {
					if(activeChar.getGameEvent().requestRevive(activeChar, _requestedPointType))
						return;
				}
				switch (_requestedPointType)
				{
					case 1: // to clanhall
						if (activeChar.getClan() == null || activeChar.getClan().getHasHideout() == 0)
							return;
						Fort fort = FortManager.getInstance().getFortByOwner(activeChar.getClan());
						if (fort != null) {
							if(fort.getFunction(Fort.FUNC_RESTORE_EXP) != null) 
								activeChar.restoreExp(fort.getFunction(Fort.FUNC_RESTORE_EXP).getLvl());
							loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Fortress);
						} else {
							loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.ClanHall);

							if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null && ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
								activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
						}
						break;
					case 2: // to castle
						siege = SiegeManager.getInstance().getSiege(activeChar);
						if (siege != null && siege.getIsInProgress())
						{
							if (siege.checkIsDefender(activeChar.getClan()))
								loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Castle);
							else if (siege.checkIsAttacker(activeChar.getClan()))
								loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
							else
								return;
						}
						else
						{
							if (activeChar.getClan() == null || activeChar.getClan().getHasCastle() == 0)
								return;
							loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Castle);
						}
						Castle castle = CastleManager.getInstance().getCastleByOwner(activeChar.getClan());
						if (castle != null && castle.getFunction(Castle.FUNC_RESTORE_EXP) != null)
							activeChar.restoreExp(CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getFunction(Castle.FUNC_RESTORE_EXP).getLvl());
						break;
/*					case 3: // to Fortress
						fsiege = FortSiegeManager.getInstance().getSiege(activeChar);
						if (fsiege != null && fsiege.getIsInProgress())
						{
							if (fsiege.checkIsAttacker(activeChar.getClan()))
								loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
							else
								return;
						}
						else
						{
							if (activeChar.getClan() == null || activeChar.getClan().getHasFort() == 0)
								return;
							loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Fortress);
						}
						Fort fort = FortManager.getInstance().getFortByOwner(activeChar.getClan());
						if (fort != null && fort.getFunction(Fort.FUNC_RESTORE_EXP) != null)
							activeChar.restoreExp(fort.getFunction(Fort.FUNC_RESTORE_EXP).getLvl());
						break; */
					case 3: // to siege HQ
						L2SiegeClan siegeClan = null;
						siege = SiegeManager.getInstance().getSiege(activeChar);

						fsiege = FortSiegeManager.getInstance().getSiege(activeChar);

						if (fsiege == null && siege != null && siege.getIsInProgress())
							siegeClan = siege.getAttackerClan(activeChar.getClan());
						else if (siege == null && fsiege != null && fsiege.getIsInProgress())
							siegeClan = fsiege.getAttackerClan(activeChar.getClan());
						if (!BanditStrongholdSiege.getInstance().isPlayerRegister(activeChar.getClan(),activeChar.getName()) &&
								!WildBeastFarmSiege.getInstance().isPlayerRegister(activeChar.getClan(),activeChar.getName()))
							if (siegeClan == null || siegeClan.getFlag().size() == 0)
								return;
						loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.SiegeFlag);
						break;
					case 4: // Fixed or Player is a festival participant
						if (!activeChar.isGM() && !activeChar.isFestivalParticipant())
							return;
						if (activeChar.isGM())
							activeChar.restoreExp(100.0);
						loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ()); // spawn them where they died
						break;
					case 27: // to jail
						if (!activeChar.isInJail())
							return;
						loc = new Location(-114356, -249645, -2984);
						break;
					default:
						if (activeChar.isInsideZone(L2Zone.FLAG_JAIL) || activeChar.isInsideZone(L2Zone.FLAG_NOESCAPE))
							loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ());
						else if (activeChar.getInstanceId()!=0){
							Instance instanceObj = InstanceManager.getInstance().getInstance(activeChar.getInstanceId());
							if (instanceObj != null)
								loc = instanceObj.getTpLoc(activeChar);
							else
								loc  = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
						}
						else
							loc = MapRegionManager.getInstance().getTeleToLocation(activeChar, TeleportWhereType.Town);
						break;
				}
				// Teleport and revive
				activeChar.setInstanceId(0);
				activeChar.setIsPendingRevive(true);
				activeChar.teleToLocation(loc, true);
				if(activeChar.getPet()!=null) {
					L2Summon pet = activeChar.getPet();
					pet.abortAttack();
					pet.abortCast();
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					pet.teleToLocation(loc,false);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		if (activeChar.isFakeDeath())
		{
			activeChar.stopFakeDeath(null);
			return;
		}
		else if (!activeChar.isDead())
			return;

		Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if (castle != null && castle.getSiege().getIsInProgress())
		{
			if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
			{
				int restartTime = castle.getSiege().getAttackerRespawnDelay();
				ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), restartTime);
				if (restartTime > 0)
					activeChar.sendMessage("Вы будете перемещены через " + restartTime / 1000 + " секунд.");
				return;
			}
		}
		new DeathTask(activeChar).run();
	}

	@Override
	public String getType()
	{
		return _C__6d_REQUESTRESTARTPOINT;
	}
}