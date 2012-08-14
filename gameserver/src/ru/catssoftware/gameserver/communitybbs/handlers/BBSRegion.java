package ru.catssoftware.gameserver.communitybbs.handlers;

import java.util.Collection;
import java.util.List;

import javolution.util.FastList;

import ru.catssoftware.gameserver.cache.HTMParser.HTMLTable;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.communitybbs.CommunityBoard;
import ru.catssoftware.gameserver.communitybbs.IBBSHandler;
import ru.catssoftware.gameserver.instancemanager.MapRegionManager;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegion;
import ru.catssoftware.gameserver.model.mapregion.L2MapRegionRestart;
import ru.catssoftware.gameserver.model.zone.L2Zone;

public class BBSRegion implements IBBSHandler {

	private static String [] COMMANDS = { "loc" };
	private static final String REGIONBase = CommunityBoard.HTMBase+"region/";
	@Override
	public String[] getCommands() {
		return COMMANDS;
	}

	@Override
	public String handleCommand(L2PcInstance activeChar, String command,
			String params) {
		String [] paramArray = params.split(" ");
		int onlineStart = 0;
		if(paramArray.length!=0) {
			if(paramArray[0].equals("u")) {
				return showUser(Integer.valueOf(paramArray[1]),activeChar);
			}
			if(paramArray[0].equals("t")) {
				return showTraders(paramArray);
			}
			if(paramArray[0].equals("o")) 
				onlineStart = Integer.parseInt(paramArray[1]);
		}
		String html = HtmCache.getInstance().getHtm(REGIONBase+"index.htm", activeChar);
		
		String next = "";
		HTMLTable online = new HTMLTable(5);
		Collection<L2PcInstance> oc = L2World.getInstance().getAllPlayers();
		L2PcInstance [] onliners = oc.toArray(new L2PcInstance[oc.size()]);
		int offliners = 0;
		for(int i=onlineStart;i<onliners.length;i++) {
			if (i>onlineStart+30) {
				next="<a action=\"bypass _bbsloc o "+i+"\">Дальше</a>";
				break;
			}
			L2PcInstance pc = onliners[i];
			String td  = "<a action=\"bypass _bbsloc u "+pc.getObjectId()+"\">"+pc.getName()+"</a>";
			if(pc.isOfflineTrade()) {
				td = String.format("<font color=%06x>%s</font>", pc.getNameColor(),td);
				offliners++;
			}
			online.add(td);
		}
		String players = "<table width=500>"+online.getTable()+"</table><br><center>";
		if(onlineStart>0)
			players+="<a action=\"bypass _bbsloc o "+(onlineStart-30)+"\">Назад</a> ";
		if(!next.isEmpty())
			players+=next;
		players+="</center>";
		List<String> visited = new FastList<String>();
		String actions = "";
		for(L2Zone z : activeChar.getZones()) {
			if(visited.contains(z.getTypeName()))
					continue;
			visited.add(z.getTypeName());
			if(HtmCache.getInstance().pathExists(REGIONBase+"actions/"+z.getTypeName()+".htm"))
				actions+=HtmCache.getInstance().getHtm(REGIONBase+"actions/"+z.getTypeName()+".htm", activeChar);
		}
		if(visited.isEmpty() && HtmCache.getInstance().pathExists(REGIONBase+"actions/NoZone.htm"))
			actions+=HtmCache.getInstance().getHtm(REGIONBase+"actions/NoZone.htm",activeChar);
		html = html.replace("%actions%",actions);
		html = html.replace("%castle%",findCastle(activeChar));
		html = html.replace("%online%",String.valueOf(oc.size()));
		html = html.replace("%offliners%",String.valueOf(offliners));
		html = html.replace("%players%", players);
		return html;
	}
	private String findCastle(L2PcInstance pc) {
		String castle = "Unknown";
		L2MapRegion region = MapRegionManager.getInstance().getRegion(pc);
		if (region != null)
		{
			int restartId = region.getRestartId();
			L2MapRegionRestart restart = MapRegionManager.getInstance().getRestartLocation(restartId);
			if(restart!=null && restart.getCastle()!=null)
				castle = restart.getCastle().getName();
		}
		return castle;
	}
	
	private String showUser(int userId,L2PcInstance activeChar) {
		L2PcInstance pc = L2World.getInstance().getPlayer(userId);
		if(pc==null)
			return HtmCache.getInstance().getHtm(REGIONBase+"user-not-found.htm",activeChar);
		String html = HtmCache.getInstance().getHtm(REGIONBase+"user.htm",activeChar);
		html = html.replace("%objectId%", String.valueOf(userId));
		html = html.replace("%race%",pc.getRace().name());
		html = html.replace("%username%",pc.getName());
		String className = pc.getClassId().name();
		className = className.substring(0,1).toUpperCase()+className.substring(1);
		
		html = html.replace("%class%",className);
		html = html.replace("%castle%",findCastle(pc));
		return html;
		
	}
	
	private String showTraders(String...params) {
		return null;
	}
}
