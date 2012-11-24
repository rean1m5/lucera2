package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.communitybbs.CommunityBoard;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.gmaccess.gmController;
import ru.catssoftware.gameserver.handler.*;
import ru.catssoftware.gameserver.model.L2CharPosition;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.model.entity.events.GameEventManager;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.network.InvalidPacketException;
import ru.catssoftware.gameserver.network.serverpackets.GMViewPledgeInfo;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import ru.catssoftware.gameserver.model.BypassManager.DecodedBypass;


public class RequestBypassToServer extends L2GameClientPacket
{
	private String				_command = null;

	@Override
	protected void readImpl()
	{
		_command = readS();
/*		if(!bp.isEmpty() && getClient().getActiveChar()!=null) {
			DecodedBypass bypass = getClient().getActiveChar().decodeBypass(bp);
			if(bypass!=null)
				_command = bypass.bypass;
		}
*/
	}

	@Override
	protected void runImpl() throws InvalidPacketException
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		if(activeChar.isDead() && !_command.startsWith("voice_access") && !getClient().checkKeyProtection()) {
			ActionFailed();
			return;
		}



		activeChar._bbsMultisell = 0;

		if(_command==null)
			return;

		if (_command.startsWith("admin_"))
		{
			if (activeChar.isGM())
			{
				gmController.getInstance().useCommand(activeChar, _command.substring(6).split(" "));
			}
			else
				ActionFailed();
		}
		else if (_command.startsWith("item_")) {
			Pattern p = Pattern.compile("item_(\\d+) ?(.?+)");
			Matcher m = p.matcher(_command);
			if(m.find()) {
				int objId = Integer.parseInt(m.group(1));
				if(m.groupCount()>1)
					_command = m.group(2); 
				else 
					_command =  null;
				L2ItemInstance item = activeChar.getInventory().getItemByObjectId(objId);
				if(item==null)
					return;
				if(_command==null) {
					IItemHandler h = ItemHandler.getInstance().getItemHandler(item.getItemId());
					if(h==null)
						return;
					h.useItem(activeChar, item);
				} else {
					IExItemHandler handler = ItemHandler.getInstance().getExHandler(item.getItemId());
					if(handler==null)
						return;
					handler.useItem(activeChar, item, _command.split(" "));
				}
			}
		}
		else if (_command.startsWith("voice_"))
		{
			String command;
			String params="";
			
			// ------------------------------ Parse command --------------------------------
			if (_command.contains(" "))
			{
				command = _command.substring(6, _command.indexOf(" "));
				params = _command.substring(_command.indexOf(" ")+1);
			}
			else
				command = _command.substring(6);
			
			// ------------------------------ Execute command ------------------------------
			IVoicedCommandHandler vc = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);

			if (vc == null)
				return;

			vc.useVoicedCommand(command, activeChar, params);
		}
		// L2CatsSoftware: Bypass for ctf commands
		else if (_command.startsWith("event")) {
			String eventName = _command.substring(6);
			int i = eventName.indexOf(" ");
			String cmd = "";
			String param = "";
			if(i!=-1) {
				cmd = eventName.substring(i+1);
				eventName = eventName.substring(0,i);
			}
			i = cmd.indexOf(" ");
			if(i!=-1) {
				param = cmd.substring(i+1);
				cmd = cmd.substring(0,i);
			}
			GameEvent evt = GameEventManager.getInstance().findEvent(eventName);
			if(evt!=null)
				evt.onCommand(activeChar, cmd, param);
		}
		else if (_command.equals("come_here") && activeChar.isGM())
			comeHere(activeChar);
		else if (_command.startsWith("show_clan_info "))
			activeChar.sendPacket(new GMViewPledgeInfo(ClanTable.getInstance().getClanByName(_command.substring(15)), activeChar));
		else if (_command.startsWith("player_help "))
			playerHelp(activeChar, _command.substring(12));
		else if (_command.startsWith("npc_"))
		{
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);

			try
			{
				L2Object object = null;
				int objectId = Integer.parseInt(id);

				if (activeChar.getTargetId() == objectId)
					object = activeChar.getTarget();
				if (object == null)
					object = L2World.getInstance().findObject(objectId);

				if (object instanceof L2NpcInstance && endOfId > 0 && activeChar.isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false))
				{
					try
					{
						((L2NpcInstance) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
					catch (NoSuchElementException nsee)
					{
						activeChar.sendMessage("Неверно задан аргумент");
					}

				}
				ActionFailed();
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Draw a Symbol
		else if (_command.equals("menu_select?ask=-16&reply=1"))
		{

			L2Object object = activeChar.getTarget();
			if (object instanceof L2NpcInstance)
				((L2NpcInstance) object).onBypassFeedback(activeChar, _command);
		}
		else if (_command.equals("menu_select?ask=-16&reply=2"))
		{
			L2Object object = activeChar.getTarget();
			if (object instanceof L2NpcInstance)
				((L2NpcInstance) object).onBypassFeedback(activeChar, _command);
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			L2Object object = activeChar.getTarget();
			if (object instanceof L2NpcInstance)
				((L2NpcInstance) object).onBypassFeedback(activeChar, _command);
		}
		else if (_command.startsWith("bbs_"))
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		else if (_command.startsWith("_bbs"))
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		else if (_command.startsWith("_maillist_0_1_0_"))
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		else if (_command.startsWith("Quest "))
		{

			String p = _command.substring(6).trim();
			int idx = p.indexOf(' ');
			if (idx < 0)
				activeChar.processQuestEvent(p, "");
			else
				activeChar.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
		}
		else if (_command.startsWith("OlympiadArenaChange"))
			Olympiad.bypassChangeArena(_command, activeChar);
	}

	/**
	 * @param client
	 */
	private void comeHere(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if (obj instanceof L2NpcInstance)
		{
			L2NpcInstance temp = (L2NpcInstance) obj;
			temp.setTarget(activeChar);
			temp.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 0));
		}
	}

	private void playerHelp(L2PcInstance activeChar, String path)
	{
		if (path.contains(".."))
			return;

		StringTokenizer st = new StringTokenizer(path);
		String[] cmd = st.nextToken().split("#");

		if (cmd.length > 1)
		{
			int itemId = Integer.parseInt(cmd[1]);
			String filename = "data/html/help/" + cmd[0];
			NpcHtmlMessage html = new NpcHtmlMessage(1, itemId);
			html.setFile(filename);
			activeChar.sendPacket(html);
		}
		else
		{
			String filename = "data/html/help/" + path;
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(filename);
			activeChar.sendPacket(html);
		}
	}

	@Override
	public String getType()
	{
		return "[C] 21 RequestBypassToServer";
	}
}
