package ru.catssoftware.gameserver.gmaccess.handlers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.quest.State;
import ru.catssoftware.gameserver.network.serverpackets.ExShowQuestMark;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.QuestList;

import javolution.text.TextBuilder;


public class questedit extends gmHandler
{
	private static final String[] commands =
	{ 
		"charquestmenu",
		"setcharquest"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		L2PcInstance target = null;
		L2Object targetObject = null;
		String[] val = new String[4];
		val[0] = null;

		if (params.length > 1)
		{
			target = L2World.getInstance().getPlayer(params[1]);
			if (params.length > 2)
			{
				if (params[2].equals("0")) { val[0] = "var"; val[1] = "Start"; }
				if (params[2].equals("1")) { val[0] = "var"; val[1] = "Started"; }
				if (params[2].equals("2")) { val[0] = "var"; val[1] = "Completed"; }
				if (params[2].equals("3")) { val[0] = "full"; }
				if (params[2].indexOf("_") != -1) { val[0] = "name"; val[1] = params[2]; }
				if (params.length > 3)
				{
					if (params[3].equals("custom")) 
						val[0] = "custom"; val[1] = params[2];
				}
			}
		}
		else
		{
			targetObject = admin.getTarget();

			if (targetObject != null && targetObject.isPlayer())
				target = (L2PcInstance) targetObject;
		}

		if (target == null)
			return;

		if (command.equals("charquestmenu"))
		{
			if (val[0] != null)
			{
				try 
				{ 
					showquestmenu(target,admin,val);
				}
				catch (Exception e)
				{
				}
			}
			else 
				showfirstquestmenu(target,admin);
		}
		else if (command.equals("setcharquest"))
		{
			if (params.length >= 5)
			{
				val[0] = params[2];
				val[1] = params[3];
				val[2] = params[4];
				if (params.length == 6)
					val[3] = params[5];
				try 
				{
					setquestvar(target,admin,val);
				}
				catch (Exception e)
				{
				}
			}
			else
				return;
		}
	}

	private void showfirstquestmenu(L2PcInstance target, L2PcInstance actor)
	{
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		int ID = target.getObjectId();

		replyMSG.append("Quest Menu for <font color=\"LEVEL\">"+target.getName()+"</font> (ID:"+ID+")<br><center>");
		replyMSG.append("<table width=250><tr><td><button value=\"CREATED\" action=\"bypass -h admin_charquestmenu "+target.getName()+" 0\" width=85 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"STARTED\" action=\"bypass -h admin_charquestmenu "+target.getName()+" 1\" width=85 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"COMPLETED\" action=\"bypass -h admin_charquestmenu "+target.getName()+" 2\" width=85 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
		replyMSG.append("<tr><td><br><button value=\"All\" action=\"bypass -h admin_charquestmenu "+target.getName()+" 3\" width=85 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
		replyMSG.append("<tr><td><br><br>Manual Edit by Quest number:<br></td></tr>");
		replyMSG.append("<tr><td><edit var=\"qn\" width=50 height=17><br><button value=\"Edit\" action=\"bypass -h admin_charquestmenu "+target.getName()+" $qn custom\" width=50 height=17 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
		replyMSG.append("</table></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		actor.sendPacket(adminReply);
	}

	private void showquestmenu(L2PcInstance target, L2PcInstance actor, String[] val) throws SQLException
	{
		try
		{
			Connection con = null;
			ResultSet rs;
			PreparedStatement req;
			int ID = target.getObjectId();

			TextBuilder replyMSG = new TextBuilder("<html><body>");
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			con = L2DatabaseFactory.getInstance().getConnection(con);

			if (val[0].equals("full"))
			{
				replyMSG.append("<table width=250><tr><td>Full Quest List for <font color=\"LEVEL\">"+target.getName()+"</font> (ID:"+ID+")</td></tr>");
				req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId='" + ID +"' ORDER by name");
				req.execute();
				rs = req.getResultSet();
				while(rs.next()) replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">"+rs.getString(1)+"</a></td></tr>");
				replyMSG.append("</table></body></html>");
				con.close();
			}
			else if (val[0].equals("name"))
			{
				String[] states = {"CREATED","STARTED","COMPLETED"};
				String state = states[target.getQuestState(val[1]).getState()];
				replyMSG.append("Character: <font color=\"LEVEL\">"+target.getName()+"</font><br>Quest: <font color=\"LEVEL\">"+val[1]+"</font><br>State: <font color=\"LEVEL\">"+state+"</font><br><br>");
				replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
				req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId='" + ID +"' and name='"+val[1]+"'");
				req.execute();
				rs = req.getResultSet();
				while(rs.next()){
					String var_name = rs.getString(1);
					if (var_name.equals("<state>"))
						continue;
					else 
						replyMSG.append("<tr><td>"+var_name+"</td><td>"+rs.getString(2)+"</td><td><edit var=\"var"+var_name+"\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+val[1]+" "+var_name+" $var"+var_name+"\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+val[1]+" "+var_name+" delete\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
				}
				replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
				replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+val[1]+" state COMLETED 1\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
				replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+val[1]+" state COMLETED 0\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
				replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+val[1]+" state DELETE\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
				replyMSG.append("</center></body></html>");
				con.close();
			}
			else if (val[0].equals("var"))
			{
				replyMSG.append("Character: <font color=\"LEVEL\">"+target.getName()+"</font><br>Quests with state: <font color=\"LEVEL\">"+val[1]+"</font><br>");
				replyMSG.append("<table width=250>");
				req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId='" + ID +"' and var='<state>' and value='"+val[1]+"'");
				req.execute();
				rs = req.getResultSet();
				while(rs.next()) replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1)+"</a></td></tr>");
				replyMSG.append("</table></body></html>");
				con.close();
			}
			else if (val[0].equals("custom"))
			{
				boolean exqdb = true;
				boolean exqch = true;
				int qnumber = Integer.parseInt(val[1]);
				String state = null;
				String qname = null;
				QuestState qs = null;
				String[] states = {"CREATED","STARTED","COMPLETED"};

				Quest quest = QuestManager.getInstance().getQuest(qnumber);

				if (quest != null)
				{
				qname = quest.getName();
				qs = target.getQuestState(qname);
				}
				else
				{ 
					exqdb = false;
				}

				if (qs != null)
				{
					state = states[qs.getState()];
				}
				else { exqch = false; state = "N/A"; }

				if(exqdb)
				{
					if(exqch) {
						replyMSG.append("Character: <font color=\"LEVEL\">"+target.getName()+"</font><br>Quest: <font color=\"LEVEL\">"+qname+"</font><br>State: <font color=\"LEVEL\">"+state+"</font><br><br>");
						replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
						req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId='" + ID +"' and name='"+qname+"'");
						req.execute();
						rs = req.getResultSet();
						while(rs.next())
						{
							String var_name = rs.getString(1);
							if (var_name.equals("<state>")) 
								continue;
							else
								replyMSG.append("<tr><td>"+var_name+"</td><td>"+rs.getString(2)+"</td><td><edit var=\"var"+var_name+"\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qname+" "+var_name+" $var"+var_name+"\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qname+" "+var_name+" delete\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
						}
						replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
						replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qname+" state COMLETED 1\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
						replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qname+" state COMLETED 0\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
						replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qname+" state DELETE\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
						replyMSG.append("</center></body></html>");
						con.close();
					}
					else
					{
						replyMSG.append("Character: <font color=\"LEVEL\">"+target.getName()+"</font><br>Quest: <font color=\"LEVEL\">"+qname+"</font><br>State: <font color=\"LEVEL\">"+state+"</font><br><br>");
						replyMSG.append("<center>Start this Quest for player:<br>");
						replyMSG.append("<button value=\"Create Quest\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qnumber+" state CREATE\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br><br>");
						replyMSG.append("<font color=\"ee0000\">Only for Unrepeateble quests:</font><br>");
						replyMSG.append("<button value=\"Create & Complete\" action=\"bypass -h admin_setcharquest "+target.getName()+" "+qnumber+" state CC\" width=130 height=20 back=\"sek.cbui94\" fore=\"sek.cbui94\"><br><br>");
						replyMSG.append("</center></body></html>");
					}
				}
				else
				{
					replyMSG.append("<center><font color=\"ee0000\">Quest with number </font><font color=\"LEVEL\">"+qnumber+"</font><font color=\"ee0000\"> doesn't exist!</font></center></body></html>");
				}
			}
			adminReply.setHtml(replyMSG.toString());
			actor.sendPacket(adminReply);
		}
		catch (Exception e)
		{
			actor.sendMessage("Error!");
		}
	}

	private void setquestvar(L2PcInstance target, L2PcInstance actor, String[] val)
	{
		QuestState qs = target.getQuestState(val[0]);
		String[] outval = new String[3];

		if(val[1].equals("state"))
		{
			if(val[2].equals("COMLETED"))
			{
				qs.exitQuest((val[3].equals("1")) ? true : false);
			}
			else if(val[2].equals("DELETE"))
			{
				qs.getQuest();
				Quest.deleteQuestInDb(qs);
				target.sendPacket(new QuestList(target));
				target.sendPacket(new ExShowQuestMark(qs.getQuest().getQuestIntId()));
			}
			else if(val[2].equals("CREATE"))
			{
				qs = QuestManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
				qs.setState(State.STARTED);
				qs.set("cond", "1");
				target.sendPacket(new QuestList(target));
				target.sendPacket(new ExShowQuestMark(qs.getQuest().getQuestIntId()));
				val[0] = qs.getQuest().getName();
			}
			else if(val[2].equals("CC"))
			{
				qs = QuestManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
				qs.exitQuest(false);
				target.sendPacket(new QuestList(target));
				target.sendPacket(new ExShowQuestMark(qs.getQuest().getQuestIntId()));
				val[0] = qs.getQuest().getName();
			}
		}
		else 
		{
			if(val[2].equals("delete"))
				qs.unset(val[1]);
			else
				qs.set(val[1], val[2]);
			target.sendPacket(new QuestList(target));
			target.sendPacket(new ExShowQuestMark(qs.getQuest().getQuestIntId()));
		}
		actor.sendMessage("Квесты персонажа успешно изменены");
		outval[0] = "name";
		outval[1] = val[0];
		try
		{
			showquestmenu(target,actor,outval);
		}
		catch(Exception e)
		{
		};
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}