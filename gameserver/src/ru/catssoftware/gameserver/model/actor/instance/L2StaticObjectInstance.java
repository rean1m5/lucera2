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
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2CharacterAI;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.knownlist.StaticObjectKnownList;
import ru.catssoftware.gameserver.model.actor.stat.StaticObjStat;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.templates.chars.L2CharTemplate;
import ru.catssoftware.gameserver.templates.item.L2Weapon;


/**
 * @author godson
 */
public class L2StaticObjectInstance extends L2Character
{
	public static final int INTERACTION_DISTANCE = 150;

	private int _staticObjectId;
	private int _meshIndex = 0; // 0 - static objects, alternate static objects
	private int _type = -1; // 0 - Map signs, 1 - Throne , 2 - Arena signs
	private int _x;
	private int _y;
	private String _texture;

	private L2PcInstance actualPersonToSitOn = null;

	/** This class may be created only by L2Character and only for AI */
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		@Override
		public L2StaticObjectInstance getActor()
		{
			return L2StaticObjectInstance.this;
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
		public void stopMove(Location pos)
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
		return null;
	}

	/**
	 * @return Returns the StaticObjectId.
	 */
	public int getStaticObjectId()
	{
		return _staticObjectId;
	}

	public L2StaticObjectInstance(int objectId, L2CharTemplate template, int staticId)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();
		_staticObjectId = staticId;
	}

	@Override
	public final StaticObjectKnownList getKnownList()
	{
		if (_knownList == null)
			_knownList = new StaticObjectKnownList(this);

		return (StaticObjectKnownList) _knownList;
	}

	@Override
	public final StaticObjStat getStat()
	{
		if (_stat == null)
			_stat = new StaticObjStat(this);

		return (StaticObjStat) _stat;
	}

	public boolean isBusy()
	{
		return (actualPersonToSitOn != null);
	}

	public void setBusyStatus(L2PcInstance actualPersonToSitOn)
	{
		this.actualPersonToSitOn = actualPersonToSitOn;
	}

	public int getType()
	{
		return _type;
	}
	
	public void setType(int type)
	{
		_type = type;
	}

	public void setMap(String texture, int x, int y)
	{
		_texture = "town_map." + texture;
		_x = x;
		_y = y;
	}

	private int getMapX()
	{
		return _x;
	}

	private int getMapY()
	{
		return _y;
	}

	@Override
	public final int getLevel()
	{
		return 1;
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

	/**
	 * This is called when a player interacts with this NPC
	 * @param player
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (_type < 0)
			_log.info("L2StaticObjectInstance: StaticObject with invalid type! StaticObjectId: " + getStaticObjectId());
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!player.isInsideRadius(this, INTERACTION_DISTANCE, false, false))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);

				// Send a Server->Client packet ActionFailed (target is out of interaction range) to the L2PcInstance player
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				if (_type == 2)
				{
					String filename = "data/html/signboard.htm";
					String content = HtmCache.getInstance().getHtm(filename,player);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

					if (content == null)
						html.setHtml("<html><body>Signboard is missing:<br>" + filename + "</body></html>");
					else
						html.setHtml(content);

					player.sendPacket(html);
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
				else if (_type == 0)
				{
					player.sendPacket(new ShowTownMap(_texture, getMapX(), getMapY()));
					// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
		}
	}

	@Override
	public void onActionShift(L2PcInstance player)
	{
		if (player == null)
			return;

		if (player.isGM())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel());
			player.sendPacket(my);

			StaticObject su = new StaticObject(this);

			player.sendPacket(su);

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder html1 = new TextBuilder("<html><body><table border=0>");
			html1.append("<tr><td>S.Y.L. Says:</td></tr>");
			html1.append("<tr><td>X: " + getX() + "</td></tr>");
			html1.append("<tr><td>Y: " + getY() + "</td></tr>");
			html1.append("<tr><td>Z: " + getZ() + "</td></tr>");
			html1.append("<tr><td>Object ID: " + getObjectId() + "</td></tr>");
			html1.append("<tr><td>Static Object ID: " + getStaticObjectId() + "</td></tr>");
			html1.append("<tr><td>Mesh Index: " + getMeshIndex() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");

			html1.append("<tr><td>Class: " + getClass().getName() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			html1.append("</table></body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else
		{
			// ATTACK the mob without moving?
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Tries to use the StaticObjectInstance as a throne with the given player to assign to
	 * 
	 * @param player The actual person who wants to sit on the throne
	 * @return Sitting was possible or not
	 */
	public boolean useThrone(L2PcInstance player)
	{
		// This check is added if char that sits on the chair had
		// a critical game error and throne wasn't release in a
		// clean way to avoid that "isBusy" will be true all the
		// way until server restarts.
		if (actualPersonToSitOn != null && L2World.getInstance().getPlayer( // If the actual user isn't
				actualPersonToSitOn.getObjectId()) == null) // found in the world anymore
			setBusyStatus(null); // release me

		if (player.getTarget() != this || // Player's target isn't me or
			getType() != 1 || // I'm no throne or
			isBusy()) // I'm already in use
			return false;

		if (player.getClan() == null || // Player has no clan or
			CastleManager.getInstance().getCastle(this) == null || // I got no castle assigned or
			CastleManager.getInstance().getCastleById(player.getClan().getHasCastle()) == null) // Player's clan has no castle
			return false;

		if (!player.isInsideRadius(this, // Player is not in radius
			INTERACTION_DISTANCE, false, false)) // to interact with me
			return false;

		if (CastleManager.getInstance().getCastle(this) != // Player's clan castle isn't
		CastleManager.getInstance().getCastleById( // the same as mine
			player.getClan().getHasCastle()))
			return false;

		if (Config.ONLY_CLANLEADER_CAN_SIT_ON_THRONE && // Only clan leader can use throne is set and
			player.getObjectId() != player.getClan().getLeaderId()) // Player is not the clan leader
			return false;

		setBusyStatus(player);
		player.setObjectSittingOn(this);

		ChairSit cs = new ChairSit(player, getStaticObjectId());
		player.sitDown();
		player.broadcastPacket(cs);

		return true;
	}

	public void setMeshIndex(int meshIndex)
	{
		_meshIndex = meshIndex;
		broadcastPacket(new StaticObject(this));
	}

	public int getMeshIndex()
	{
		return _meshIndex;
	}

	@Override
	public void broadcastFullInfoImpl()
	{
	}
}