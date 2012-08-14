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

import javolution.text.TextBuilder;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.ai.L2DoorAI;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.knownlist.DoorKnownList;
import ru.catssoftware.gameserver.model.actor.stat.DoorStat;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.entity.Siege;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.gameserver.templates.item.L2Weapon;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;



/**
 * This class ...
 * 
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2DoorInstance extends L2Character
{
	/** The castle index in the array of L2Castle this L2DoorInstance belongs to */
	private int					_castleIndex		= -2;
	private Castle				_castle;
	/** The fort index in the array of L2Fort this L2DoorInstance belongs to */
	private int					_fortId				= -2;
	private Fort				_fort;

	private L2MapRegion			_mapRegion			= null;

	protected final int			_doorId;
	protected final String		_name;
	private boolean				_open;
	private boolean				_isCommanderDoor;
	private boolean				_unlockable;
	public boolean				_isCHDoor = false;

	// when door is closed, the dimensions are
	private int					_rangeXMin			= 0;
	private int					_rangeYMin			= 0;
	private int					_rangeZMin			= 0;
	private int					_rangeXMax			= 0;
	private int					_rangeYMax			= 0;
	private int					_rangeZMax			= 0;

	// these variables assist in see-through calculation only
	private int					_A					= 0;
	private int					_B					= 0;
	private int					_C					= 0;
	private int					_D					= 0;

	private ClanHall			_clanHall;

	protected int				_autoActionDelay	= -1;
	private ScheduledFuture<?>	_autoActionTask;

	/** This class may be created only by L2Character and only for AI */
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		@Override
		public L2DoorInstance getActor()
		{
			return L2DoorInstance.this;
		}

		@Override
		public void moveTo(int x, int y, int z, int offset)
		{
		}

		@Override
		public boolean moveTo(int x, int y, int z)
		{
			return false;
		}

		@Override
		public void stopMove(L2CharPosition pos)
		{
		}

		@Override
		public void doAttack(L2Character target)
		{
		}

		@Override
		public void doCast(L2Skill skill)
		{
		}
	}

	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2DoorAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	class CloseTask implements Runnable
	{
		public void run()
		{
			try
			{
				onClose();
			}
			catch (Exception e)
			{
				_log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Manages the auto open and closing of a door.
	 */
	class AutoOpenClose implements Runnable
	{
		public void run()
		{
			try
			{
				if (!getOpen())
				{
					openMe();
				}
				else
				{
					closeMe();
				}
			}
			catch (Exception e)
			{
				_log.warn("Could not auto open/close door ID " + _doorId + " (" + _name + ")");
				e.printStackTrace();
			}
		}
	}

	public L2DoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		_doorId = doorId;
		_name = name;
		_unlockable = unlockable;
	}

	@Override
	public final DoorKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new DoorKnownList(this);

		return (DoorKnownList) _knownList;
	}

	@Override
	public DoorStat getStat()
	{
		if (_stat == null)
			_stat = new DoorStat(this);

		return (DoorStat) _stat;
	}
	public void setCHDoor(boolean par)
	{
		_isCHDoor = par;
	}
	public final boolean isUnlockable()
	{
		return _unlockable;
	}

	@Override
	public final int getLevel()
	{
		return 1;
	}

	/**
	 * @return Returns the doorId.
	 */
	public int getDoorId()
	{
		return _doorId;
	}

	/**
	 * @return Returns the open.
	 */
	public boolean getOpen()
	{
		return _open;
	}

	/**
	 * @param open
	 *            The open to set.
	 */
	public void setOpen(boolean open)
	{
		_open = open;
	}

	/**
	 * @param val Used for Fortresses to determine if doors can be attacked during siege or not
	 */
	public void setIsCommanderDoor(boolean val)
	{
		_isCommanderDoor = val;
	}

	/**
	 * @return Doors that cannot be attacked during siege
	 * these doors will be auto opened if u take control of all commanders buildings
	 */
	public boolean getIsCommanderDoor()
	{
		return _isCommanderDoor;
	}

	/**
	 * Sets the delay in milliseconds for automatic opening/closing of this door
	 * instance. <BR>
	 * <B>Note:</B> A value of -1 cancels the auto open/close task.
	 * 
	 * @param actionDelay
	 *            actionDelay
	 */
	public void setAutoActionDelay(int actionDelay)
	{
		if (_autoActionDelay == actionDelay)
			return;

		if (actionDelay > -1)
		{
			AutoOpenClose ao = new AutoOpenClose();
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(ao, actionDelay, actionDelay);
		}
		else
		{
			if (_autoActionTask != null)
				_autoActionTask.cancel(false);
		}

		_autoActionDelay = actionDelay;
	}

	public int getDamage()
	{
		int dmg = 6 - (int) Math.ceil(getStatus().getCurrentHp() / getMaxHp() * 6);
		if (dmg > 6)
			return 6;
		if (dmg < 0)
			return 0;
		return dmg;
	}

	public final Castle getCastle()
	{
		if (_castle == null)
		{
			Castle castle = null;

			if (_castleIndex < 0)
			{
				castle = CastleManager.getInstance().getCastle(this);
				if (castle != null)
					_castleIndex = castle.getCastleId();
			}
			if (_castleIndex > 0)
				castle = CastleManager.getInstance().getCastleById(_castleIndex);
			_castle = castle;
		}
		return _castle;
	}

	public final Fort getFort()
	{
		if (_fort == null)
		{
			Fort fort = null;

			if (_fortId < 0)
			{
				fort = FortManager.getInstance().getFort(this);
				if (fort != null)
					_fortId = fort.getCastleId();
			}
			if (_fortId > 0)
				fort = FortManager.getInstance().getFortById(_fortId);
			_fort = fort;
		}
		return _fort;
	}

	public void setClanHall(ClanHall clanHall)
	{
		_clanHall = clanHall;
	}

	public ClanHall getClanHall()
	{
		return _clanHall;
	}

	public boolean isEnemy()
	{
		if (getCastle() != null && getCastle().getSiege().getIsInProgress())
			return true;
		if (getFort() != null && getFort().getSiege().getIsInProgress() && !getIsCommanderDoor())
			return true;
		if (_isCHDoor && (DevastatedCastleSiege.getInstance().getIsInProgress()
				||FortressOfDeadSiege.getInstance().getIsInProgress()))
			return true;
		return false;
    }

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (isUnlockable() && getFort() == null)
			return true;

		// Doors can't be attacked by NPCs
		if (!(attacker instanceof L2PlayableInstance))
			return false;

		// Attackable only during siege by everyone (not owner)
		boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
		boolean isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress() && !getIsCommanderDoor());

		if (_isCHDoor && (DevastatedCastleSiege.getInstance().getIsInProgress()
				||FortressOfDeadSiege.getInstance().getIsInProgress()))
			return true;
		else if(getClanHall()!=null)
			return false;
		if (isFort)
		{
			if (attacker instanceof L2SummonInstance)
			{
				L2Clan clan = ((L2SummonInstance)attacker).getOwner().getClan();
				if (clan != null && clan == getFort().getOwnerClan())
					return false;
			}
			else if (attacker instanceof L2PcInstance)
			{
				L2Clan clan = ((L2PcInstance)attacker).getClan();
				if (clan != null && clan == getFort().getOwnerClan())
					return false;
			}
		}
		else if (isCastle)
		{
			if (attacker instanceof L2SummonInstance)
			{
				L2Clan clan = ((L2SummonInstance)attacker).getOwner().getClan();
				if (clan != null && clan.getClanId() == getCastle().getOwnerId())
					return false;
			}
			else if (attacker instanceof L2PcInstance)
			{
				L2Clan clan = ((L2PcInstance)attacker).getClan();
				if (clan != null && clan.getClanId() == getCastle().getOwnerId())
					return false;
			}
		}

		return (isCastle || isFort);
	}

	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}

	public int getDistanceToWatchObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
			return 0;
		return 3000;
	}

	/**
	 * Return the distance after which the object must be remove from
	 * _knownObject according to the type of the object.<BR>
	 * <BR>
	 * 
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li> object is a L2PcInstance : 4000</li>
	 * <li> object is not a L2PcInstance : 0 </li>
	 * <BR>
	 * <BR>
	 * 
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
			return 0;

		return 4000;
	}

	/**
	 * Return null.<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (player == null)
			return;

		if (Config.SIEGE_ONLY_REGISTERED)
		{
			boolean opp = false;
			Siege siege = SiegeManager.getInstance().getSiege(player);
			L2Clan oppClan = player.getClan();
			if (siege != null && siege.getIsInProgress())
			{
				if (oppClan != null)
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
			}
			else if (_isCHDoor && (DevastatedCastleSiege.getInstance().getIsInProgress()
					||FortressOfDeadSiege.getInstance().getIsInProgress()))
			{
				opp = true;
			}
			else
				opp = true;

			if (!opp)
				return;
		}

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance
			// player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			player.sendPacket(new DoorStatusUpdate(this));

			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (isAutoAttackable(player))
			{
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth
				// difference might
				// need some
				// tweaking
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else if (player.getClan() != null && getClanHall() != null && player.getClanId() == getClanHall().getOwnerId())
			{
				if (!isInsideRadius(player, L2NpcInstance.INTERACTION_DISTANCE, false, false))
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				else
				{
					player.gatesRequest(this);
					if (!getOpen())
						player.sendPacket(new ConfirmDlg(1140));
					else
						player.sendPacket(new ConfirmDlg(1141));
				}
			}
			else if (player.getClan() != null && getFort() != null && player.getClanId() == getFort().getOwnerId() && isUnlockable() && !getFort().getSiege().getIsInProgress())
			{
				if (!isInsideRadius(player, L2NpcInstance.INTERACTION_DISTANCE, false, false))
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				else
				{
					player.gatesRequest(this);
					if (!getOpen())
						player.sendPacket(new ConfirmDlg(1140));
					else
						player.sendPacket(new ConfirmDlg(1141));
				}
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to
		// avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onActionShift(L2PcInstance player)
	{
		if (player.isGM())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			player.sendPacket(new DoorStatusUpdate(this));

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder html1 = new TextBuilder("<html><body><table border=0>");
			html1.append("<tr><td>S.Y.L. Says:</td></tr>");
			html1.append("<tr><td>Current HP  " + getStatus().getCurrentHp() + "</td></tr>");
			html1.append("<tr><td>Max HP      " + getMaxHp() + "</td></tr>");
			html1.append("<tr><td>Max X       " + getXMax() + "</td></tr>");
			html1.append("<tr><td>Max Y       " + getYMax() + "</td></tr>");
			html1.append("<tr><td>Max Z       " + getZMax() + "</td></tr>");
			html1.append("<tr><td>Min X       " + getXMin() + "</td></tr>");
			html1.append("<tr><td>Min Y       " + getYMin() + "</td></tr>");
			html1.append("<tr><td>Min Z       " + getZMin() + "</td></tr>");
			html1.append("<tr><td>Object ID:  " + getObjectId() + "</td></tr>");
			html1.append("<tr><td>Door ID: <br>" + getDoorId() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			html1.append("<tr><td><br></td></tr>");

			html1.append("<tr><td>Class: " + getClass().getName() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			html1.append("</table>");

			html1.append("<table><tr>");
			html1.append("<td><button value=\"Open\" action=\"bypass -h admin_open " + getDoorId()
					+ "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
			html1.append("<td><button value=\"Close\" action=\"bypass -h admin_close " + getDoorId()
					+ "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
			html1.append("<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
			html1.append("<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
			html1.append("</tr></table></body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else
		{
			// ATTACK the mob without moving?
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public final void broadcastStatusUpdateImpl()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		if (knownPlayers == null || knownPlayers.isEmpty())
			return;

		DoorInfo su = new DoorInfo(this, false);
		DoorStatusUpdate dsu = new DoorStatusUpdate(this);

		for (L2PcInstance player : knownPlayers)
		{
			if ((getCastle() != null && getCastle().getCastleId() > 0) || (getFort() != null && getFort().getFortId() > 0 && !getIsCommanderDoor()))
				su = new DoorInfo(this, true);
			
			player.sendPacket(su);
			player.sendPacket(dsu);
		}
	}

	public void onOpen()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), 60000);
	}

	public void onClose()
	{
		closeMe();
	}

	public final void closeMe()
	{
		setOpen(false);
		broadcastFullInfo();
	}

	public final void openMe()
	{
		setOpen(true);
		broadcastFullInfo();
	}

	@Override
	public String toString()
	{
		return "door " + _doorId;
	}

	public int getXMin()
	{
		return _rangeXMin;
	}

	public int getYMin()
	{
		return _rangeYMin;
	}

	public int getZMin()
	{
		return _rangeZMin;
	}

	public int getXMax()
	{
		return _rangeXMax;
	}

	public int getYMax()
	{
		return _rangeYMax;
	}

	public int getZMax()
	{
		return _rangeZMax;
	}

	public void setRange(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
	{
		_rangeXMin = xMin;
		_rangeYMin = yMin;
		_rangeZMin = zMin;

		_rangeXMax = xMax;
		_rangeYMax = yMax;
		_rangeZMax = zMax;

		_A = _rangeYMax * (_rangeZMax - _rangeZMin) + _rangeYMin * (_rangeZMin - _rangeZMax);
		_B = _rangeZMin * (_rangeXMax - _rangeXMin) + _rangeZMax * (_rangeXMin - _rangeXMax);
		_C = _rangeXMin * (_rangeYMax - _rangeYMin) + _rangeXMin * (_rangeYMin - _rangeYMax);
		_D = -1
				* (_rangeXMin * (_rangeYMax * _rangeZMax - _rangeYMin * _rangeZMax) + _rangeXMax * (_rangeYMin * _rangeZMin - _rangeYMin * _rangeZMax) + _rangeXMin
						* (_rangeYMin * _rangeZMax - _rangeYMax * _rangeZMin));
	}

	public String getDoorName()
	{
		return _name;
	}

	public L2MapRegion getMapRegion()
	{
		return _mapRegion;
	}

	public void setMapRegion(L2MapRegion region)
	{
		_mapRegion = region;
	}

	public int getA()
	{
		return _A;
	}

	public int getB()
	{
		return _B;
	}

	public int getC()
	{
		return _C;
	}

	public int getD()
	{
		return _D;
	}

	@Override
	public void broadcastFullInfoImpl()
	{
		broadcastPacket(new StaticObject(this));
		broadcastPacket(new DoorStatusUpdate(this));
	}

	public boolean isEnemyOf(L2PcInstance activeChar) {
		return false;
	}
	@Override
	public boolean doDie(L2Character killer) {
		if(super.doDie(killer)) {
			if(getCastle() != null && getCastle().getSiege().getIsInProgress()) {
				SystemMessage msg = new SystemMessage(SystemMessageId.CASTLE_GATE_BROKEN_DOWN);
				getCastle().getSiege().announceToParticipants(msg);
			}
			return true;
		}
		return false;
	}

	@Override
	public L2DoorInstance getDoor()
	{
		return this;
	}

	@Override
	public boolean isDoor()
	{
		return true;
	}
}