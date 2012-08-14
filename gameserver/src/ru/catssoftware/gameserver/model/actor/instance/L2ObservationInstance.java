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

import java.util.StringTokenizer;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Util;


/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 *
 */
public final class L2ObservationInstance extends L2FolkInstance
{
	private static final int OBSERVE_SPECIAL_COST = 500;
	private static final int OBSERVE_COST = 80;

	private static final int[][] ALLOWED_PLACES = {
		new int[] { 148416, 46724, -3000 },
		new int[] { 149500, 46724, -3000 },
		new int[] { 150511, 46724, -3000 }
	};

	private static final int[][] ALLOWED_SPECIAL_PLACES = {
		new int[] { -18347, 114000, -2360 },
		new int[] { -18347, 113255, -2447 },
		new int[] { 22321, 155785, -2604 },
		new int[] { 22321, 156492, -2627 },
		new int[] { 112000, 144864, -2445 },
		new int[] { 112657, 144864, -2525 },
		new int[] { 116260, 244600, -775 },
		new int[] { 116260, 245264, -721 },
		new int[] { 78100, 36950, -2242 },
		new int[] { 78744, 36950, -2244 },
		new int[] { 147457, 9601, -233 },
		new int[] { 147457, 8720, -252 },
		new int[] { 147542, -43543, -1328 },
		new int[] { 147465, -45259, -1328 },
		new int[] { 20598, -49113, -300 },
		new int[] { 18702, -49150, -600 },
		new int[] { 77541, -147447, 353 },
		new int[] { 77541, -149245, 353 },

		new int[] { -80210, 87400, -4800 },
		new int[] { -77200, 88500, -4800 },
		new int[] { -75320, 87135, -4800 },
		new int[] { -76840, 85770, -4800 },
		new int[] { -79950, 85165, -4800 },
		new int[] { -79185, 112725, -4300 },
		new int[] { -76175, 113330, -4300 },
		new int[] { -74305, 111965, -4300 },
		new int[] { -75915, 110600, -4300 },
		new int[] { -78930, 110005, -4300 },
	};
	/**
	 * @param template
	 */
	public L2ObservationInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(player.getGameEvent()!=null) {
			player.sendMessage("Вы не можете наблюдать, если зарегистрированы на эвент");
			return;
		}
		if (command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken(); // Bypass cost

			if (SiegeManager.getInstance().checkIfInZone(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())))
				doObserve(player, val, true);
			else
				player.sendPacket(SystemMessageId.ONLY_VIEW_SIEGE);
		}
		else if (command.startsWith("observeOracle"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken(); // Bypass cost

			doObserve(player, val, true);
		}
		else if (command.startsWith("observe"))
			doObserve(player, command.substring(8), false);
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			showMessageWindow(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}

	public void showMessageWindow(L2PcInstance player, int val)
	{
		String filename = "";

		if (isInsideRadius(-79884, 86529, 50, true) || isInsideRadius(-78858, 111358, 50, true) || isInsideRadius(-76973, 87136, 50, true) || isInsideRadius(-75850, 111968, 50, true))
		{
			if (val == 0)
				filename = "data/html/observation/" + getNpcId() + "-cata.htm";
			else
				filename = "data/html/observation/" + getNpcId() + "-cata-" + val + ".htm";
		}
		else
		{
			if (val == 0)
				filename = "data/html/observation/" + getNpcId() + ".htm";
			else
				filename = "data/html/observation/" + getNpcId() + "-" + val + ".htm";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	private void doObserve(L2PcInstance player, String val, boolean special)
	{
		if(player.isCastingNow() || player.isInCombat() || player.isImmobilized())
			return;
		StringTokenizer st = new StringTokenizer(val);
		st.nextToken(); // this may be fake
		int cost = (special ? OBSERVE_SPECIAL_COST : OBSERVE_COST);
		// The coordinates may also be fake
		int x = Integer.parseInt(st.nextToken());
		int y = Integer.parseInt(st.nextToken());
		int z = Integer.parseInt(st.nextToken());
		if (!canObserve(x, y, z, special))
		{
			Util.handleIllegalPlayerAction(player, "Framed observation request!", Config.DEFAULT_PUNISH);
			return;
		}
		if (player.reduceAdena("Broadcast", cost, this, true))
		{
			// enter mode
			player.enterObserverMode(x, y, z);
			player.sendPacket(new ItemList(player, false));
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;
		if (this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showMessageWindow(player, 0);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	private final boolean canObserve(int x, int y, int z, boolean special)
	{
		int[][] allowed = (special ? ALLOWED_SPECIAL_PLACES : ALLOWED_PLACES);
		for (int i = 0; i < allowed.length; i++)
		{
			int[] coords = allowed[i];
			if (coords[0] == x && coords[1] == y && coords[2] == z)
				return true;
		}
		return false;
	}	
}