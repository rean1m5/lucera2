package ru.catssoftware.protection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.gmaccess.gmCache;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.util.L2Utils;

public class GatsGuardHandler extends gmHandler {

	private static String [] _COMMANDS = {
		"hwid",
		"hwidban",
		"hwidunban",
		"hwidlist",
		"hwidbanned"
	};
	@Override
	public String[] getCommandList() {
		return _COMMANDS;
	}

	@Override
	public void runCommand(L2PcInstance admin, String... params) {
		String command = params[0];
		if(command.equals("hwid")) {
			String html = "<html><title>CatsGuard</title><body><center><br>";
			html+="<edit var=\"char_name\" width=110 height=15><br>";
			html+="<table width=200><tr>";
			html+="<td><button action=\"bypass -h admin_hwidban $char_name\" value=\"Бан\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></td>";
			html+="<td><button action=\"bypass -h admin_hwidunban $char_name\" value=\"Анбан\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></td>";
			html+="</tr><tr><td><button action=\"bypass -h admin_hwidbanned\" value=\"Забаненные\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></td>";
			html+="<td><button action=\"bypass -h admin_hwidlist\" value=\"Игроки\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></td>";
			html +="</tr></table>";
			html +="Справка по коммандам<br>";
			html +="//hwidban [имя_игрока] - забанить игрока<br1>";
			html +="//hwidunban имя_игрока - разбанить игрока игрока<br1>";
			html +="//hwidbanned - список забаненых<br1>";
			html +="//hwidlist - список игроков онлайн с HWID<br1>";
			html +="</center></body></html>";
			NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setHtml(html);
			admin.sendPacket(msg);
		}
		else if (command.equals("hwidban")) {
			String hwid=null;
			if(params.length==1) {
				if(admin.getTarget() instanceof L2PcInstance) 
					hwid = ((L2PcInstance)admin.getTarget()).getHWid();
			} 
			else {
				hwid = L2Utils.getHwId(params[1]);
			}
			if(hwid!=null) {
				try {
//					Integer.parseInt(hwid,16);
					CatsGuard.getInstance().ban(hwid);
					for(L2PcInstance pc : L2World.getInstance().getAllPlayers()) {
						if(pc.getHWid()!=null && pc.getHWid().equals(hwid) && !gmCache.getInstance().isGm(pc.getObjectId()))
							new Disconnection(pc).defaultSequence(false);
					}
					admin.sendMessage("HWID "+hwid+" забанен");
				} catch(NumberFormatException e) {
					admin.sendMessage("Нет такого игрока "+params[1]);
				}
				
			}
			runCommand(admin, new String[] {"hwidbanned"});
		}
		else if (command.equals("hwidlist")) {
			int start =0;
			int ncount = 0;
			String table = "";
			if(params.length==2)
				start = Integer.parseInt(params[1]);
			boolean endReached = true;
			String html = "<html><title>CatsGuard</title><body><center>Игроки онлайн (всего "+L2World.getInstance().getAllPlayersCount()+")<br><table width=220>";
			for(L2PcInstance pc : L2World.getInstance().getAllPlayers()) {
				if(pc.isOfflineTrade()) continue;
				if(++ncount<start) continue;
				table+="<tr><td><font color=\"LEVEL\">"+pc.getHWid()+"</font></td><td>"+pc.getName()+"</td><td><a action=\"bypass -h admin_hwidban "+pc.getName()+"\">Бан</td></tr>";
				if(table.length()>7000) {
					endReached = false;
					break;
				}
			}
			html+=table;
			html+="</table>";
			if(!endReached)
				html+="<a action=\"bypass -h admin_hwidlist "+ncount+"\">Дальше</a><br>";
			html+="<button action=\"bypass -h admin_hwid\" value=\"Назад\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\">"; 
			html+="</center></body></html>";
			NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setHtml(html);
			admin.sendPacket(msg);
		}
		else if (command.equals("hwidbanned")) {
			try {
				Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement stm = con.prepareStatement("select h.hwid, ch.account_name, ch.char_name from banned_hwid h "+
															 "inner join characters ch on ch.hwid = h.hwid");
				ResultSet rs = stm.executeQuery();
				int start = 0;
				int ncount = 0;
				if(params.length==2)
					start = Integer.parseInt(params[1]);
				String html = "<html><title>CatsGuard</title><body><center>Забаненные<br><table width=220>";
				String table = "";
				boolean endReached = true;
				while(rs.next()) {
					if(++ncount < start) continue;
					table+="<tr><td><font color=\"LEVEL\">"+rs.getString(1)+"</font></td><td>"+rs.getString(2)+"</td><td>"+rs.getString(3)+
					       "</td><td><a action=\"bypass -h admin_hwidunban "+rs.getString(1)+"\">Анбан</a></td></tr>";
					if(table.length()>7000) {
						endReached = false;
						break;
					}
				}
				html+=table;
				html+="</table>";
				if(!endReached)
					html+="<a action=\"bypass -h admin_hwidbanned "+ncount+"\">Дальше</a><br>";
				html+="<button action=\"bypass -h admin_hwid\" value=\"Назад\" width=74 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\">"; 
				html+="</center></body></html>";
				
				rs.close();
				stm.close();
				con.close();
				NpcHtmlMessage msg = new NpcHtmlMessage(0);
				msg.setHtml(html);
				admin.sendPacket(msg);
			} catch(SQLException e) {
			
			}
		}
		else if (command.equals("hwidunban")) {
			if(params.length==1)
				admin.sendMessage("Использование //hwidunban hwid|персонаж");
			else {
				String hwid = L2Utils.getHwId(params[1]);
				if(hwid!=null) {
					CatsGuard.getInstance().unban(hwid);
					admin.sendMessage("Игрок "+params[1]+" разбанен");
				} else {
					CatsGuard.getInstance().unban(params[1]);
					admin.sendMessage("HWID "+params[1]+" разбанен");
				}
			}
			runCommand(admin, new String[] {"hwidbanned"});
		}

	}

}
