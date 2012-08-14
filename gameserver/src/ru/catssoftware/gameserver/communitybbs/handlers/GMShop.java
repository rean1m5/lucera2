package ru.catssoftware.gameserver.communitybbs.handlers;

import javolution.util.FastMap;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.communitybbs.CommunityBoard;
import ru.catssoftware.gameserver.communitybbs.IBBSHandler;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.model.L2Multisell;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.serverpackets.HideBoard;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * 
 * @author Azagthtot
 * Обработчик команд gmshop-а<br>
 * Поддерживаемые команды:<br>
 * _bbsgmshop Link <i>страница</i> - выдать html относительно CommunityBoard/gmshop<br>
 * _bbsgmshop multisell	<i>ID</id> - выдать Multisell<br>
 * _bbsnpc <i>NpcId</i> - показать (в отдельном окне) диалог для указаного NPC<br>
 * _bbsQuest <i>Имя [событие]</i> - вывести диалоги квеста в панели BBS<br>
 *  Если <i>событие</i> не указно, вызывается метод onTalk квеста, при этом npc = null<br>
 *  если указано, то OnEvent с <u>корректным</u> QuestState
 */
public class GMShop implements IBBSHandler {

	
	public static Integer GMSHOP_SIGNATURE = 100067856;
	private Map<Integer, L2NpcInstance> _npcs;
	public GMShop() {
		_npcs = new FastMap<Integer, L2NpcInstance>();
	}

	private static String [] _commands = { "gmshop","npc","Quest" }; 
	@Override
	public String[] getCommands()
	{
		return _commands;
	}
	public static boolean checkItemCondition(L2PcInstance player)
	{
		
		boolean ok=true;
		if ((player.getGameEvent()!=null && player.getGameEvent().isRunning()))
			ok=false;
		if (player.isInOlympiadMode())
			ok=false;
		if (player.isInCombat())
			ok=false;
		if (!ok)
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
		return ok;	
	}
	public static boolean checkMagicCondition(L2PcInstance player)
	{
		boolean ok=true;
		if(player.getPrivateStoreType()!=L2PcInstance.STORE_PRIVATE_NONE && Config.BBS_RESTRICTIONS.contains("TRADE"))
			ok = false;
		if ((player.getGameEvent()!=null && player.getGameEvent().isRunning()) && Config.BBS_RESTRICTIONS.contains("EVENT"))
			ok=false;
		if (player.isInJail() && Config.BBS_RESTRICTIONS.contains("JAIL"))
			ok=false;
		if (player.getOlympiadGameId() >= 0 && Config.BBS_RESTRICTIONS.contains("OLY"))
			ok=false;
		if (player.isInCombat() && Config.BBS_RESTRICTIONS.contains("COMBAT"))
			ok=false;
		if(player.getKarma()>0 && Config.BBS_RESTRICTIONS.contains("KARMA"))
			ok = false;
		if(player.getPvpFlag()>0 && Config.BBS_RESTRICTIONS.contains("PVP"))
			ok = false;
		if(player.isInsideZone(L2Zone.FLAG_SIEGE) && Config.BBS_RESTRICTIONS.contains("SIEGE"))
			ok = false;
		if(player.isInsideZone(L2Zone.FLAG_NOSUMMON) && Config.BBS_RESTRICTIONS.contains("RB"))
			ok = false;
		if(player.isInsideZone(L2Zone.FLAG_PVP)&& Config.BBS_RESTRICTIONS.contains("ARENA"))
			ok = false;
		if (!ok)
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NOT_ALLOWED_AT_THE_MOMENT));
		return ok;		
	}

	@Override
	public String handleCommand(L2PcInstance activeChar, String command, String params)
	{
			if(command.equals("gmshop"))
			{
				String cmd = params;
				if(params.contains(" "))
				{
					cmd = params.substring(0,params.indexOf(" "));
					params = params.substring(params.indexOf(" ")+1);
				}
				else 
					params = "";
				if(cmd.equals("Link"))
				{
					return "gmshop/"+params;
				}
				else if(cmd.equals("multisell"))
				{
					
					int val = Integer.parseInt(params);
					activeChar.sendPacket(new HideBoard());
					L2Multisell.getInstance().separateAndSend(val, activeChar, false, 0);
					activeChar._bbsMultisell = val;
				}
				else if(cmd.equals("exc_multisell"))
				{
					int val = Integer.parseInt(params);
					activeChar.sendPacket(new HideBoard());
					L2Multisell.getInstance().separateAndSend(val, activeChar, true, 0);
					activeChar._bbsMultisell = val;
				}
				
			} 
			else if (command.equals("Quest"))
			{
				String qn = params;
				if(params.contains(" "))
				{
					qn = params.substring(0,params.indexOf(" "));
					params = params.substring(params.indexOf(" ")+1);
				}
				else 
					params = "";
				Quest q = QuestManager.getInstance().getQuest(qn);
				if(q!=null)
				{
					String result  = null;
					QuestState qs = activeChar.getQuestState(qn);
					if(qs==null)
						qs = q.newQuestState(activeChar);
					if (params.length()==0)
						result = q.onTalk(null, activeChar);
					else {
						result = q.onEvent(params, qs,true);
						if(result==null)
							result = q.onAdvEvent(params, null, activeChar);
						
					}
					if(result!=null) {
						if(result.endsWith(".htm"))
							result = q.showHtmlFile(activeChar, result,true);
					}
					return CommunityBoard.parseOldFormat(result, activeChar);
				}
			}
			else if (command.equals("npc"))
			{
				int npcId = 0;
				try
				{
					if (params.contains(" "))
					{
						npcId = Integer.parseInt(params.substring(0,params.indexOf(" ")));
						params = params.substring(params.indexOf(" ")+1);
					}
					else
					{
						npcId = Integer.parseInt(params);
						params = "";
					}
				}
				catch(Exception e)
				{
					npcId = 0;
				}
				if (npcId==0)
					return null;
				L2NpcInstance npc = _npcs.get(npcId);
				if(npc==null)
				{
					try
					{
						L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
						if(t==null)
							return null;
						Class<?> clazz = Class.forName("ru.catssoftware.gameserver.model.actor.instance."+t.getType()+"Instance");
						if(clazz==null)
							return null;
						Constructor<?> c = clazz.getConstructor(int.class,L2NpcTemplate.class);
						if(c==null)
							return null;
						npc = (L2NpcInstance)c.newInstance(IdFactory.getInstance().getNextId(),t);
						_npcs.put(npcId, npc);
					}
					catch(Exception e)
					{
						e.printStackTrace();
						return null;
					}
				}
				if(npc==null) 
					return null;
				activeChar.setTarget(npc);
				npc.onBypassFeedback(activeChar, params);
			}
			return null;
		
	}

}
