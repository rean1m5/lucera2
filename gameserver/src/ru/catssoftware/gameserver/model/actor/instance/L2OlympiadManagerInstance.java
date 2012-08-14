package ru.catssoftware.gameserver.model.actor.instance;


import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.util.FastList;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2Multisell;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.olympiad.OlympiadManager;
import ru.catssoftware.gameserver.network.serverpackets.ExHeroList;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

/**
 * @Olympiad Npc's Instance
 * @author godson
 */

public class L2OlympiadManagerInstance extends L2FolkInstance
{
	private final static Logger	_log		= Logger.getLogger(L2OlympiadManagerInstance.class.getName());
	private static final int	GATE_PASS	= 6651;

	public L2OlympiadManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("OlympiadDesc"))
		{
			int val = Integer.parseInt(command.substring(13, 14));
			String suffix = command.substring(14);
			showChatWindow(player, val, suffix);
		}
		else if (command.startsWith("OlympiadNoble"))
		{
			if (!player.isNoble() || player.getClassId().level() < 3)
				return;

			int val = Integer.parseInt(command.substring(14));
			NpcHtmlMessage reply;
			TextBuilder replyMSG;
			switch (val)
			{
				case 1:
					Olympiad.getInstance().unRegisterNoble(player);
					break;
				case 2:
					int classed = 0;
					int nonClassed = 0;
					int[] array = Olympiad.getInstance().getWaitingList();

					if (array != null)
					{
						classed = array[0];
						nonClassed = array[1];
					}
					reply = new NpcHtmlMessage(getObjectId());
					reply.setFile("data/html/olympiad/await.htm");
					reply.replace("%classed%", String.valueOf(classed));
					reply.replace("%nonClassesd%",String.valueOf(nonClassed));
					reply.replace("%objectId%",String.valueOf(getObjectId()));
					player.sendPacket(reply);
					break;
				case 3:
					int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					if (points >= 0)
					{
						reply = new NpcHtmlMessage(getObjectId());
						replyMSG = new TextBuilder("<html><body>");
						replyMSG.append("There are " + points + " Grand Olympiad " + "points granted for this event.<br><br>" + "<a action=\"bypass -h npc_"
								+ getObjectId() + "_OlympiadDesc 2a\">Назад</a>");
						replyMSG.append("</body></html>");
						reply.setHtml(replyMSG.toString());
						player.sendPacket(reply);
					}
					break;
				case 4:
					Olympiad.getInstance().registerNoble(player, false);
					break;
				case 5:
					Olympiad.getInstance().registerNoble(player, true);
					break;
				case 6:
					int passes = Olympiad.getInstance().getNoblessePasses(player.getObjectId());
					if (passes > 0)
						player.addItem("Olympiad", GATE_PASS, passes, player, true, true);
					else
					{
						reply = new NpcHtmlMessage(getObjectId());
						replyMSG = new TextBuilder("<html><body>");
						replyMSG.append("Менеджер Великой Олимпиады :<br>" + "Извините, у Вас недостаточно очков для обмена на Noblesse Gate Pass. Попробуйте в следующий раз.<br>"
								+ "<a action=\"bypass -h npc_" + getObjectId() + "_OlympiadDesc 4a\">Назад</a>");
						replyMSG.append("</body></html>");
						reply.setHtml(replyMSG.toString());
						player.sendPacket(reply);
					}
					break;
				case 7:
					L2Multisell.getInstance().separateAndSend(102, player, false, getCastle().getTaxRate());
					break;
				default:
					_log.warn("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else if (command.startsWith("Olympiad"))
		{
			if(player.getGameEvent()!=null) {
				player.sendMessage("Вы не можете наблюдать, если зарегистрированы на эвент");
				return;
			}
			int val = Integer.parseInt(command.substring(9, 10));
			NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
			TextBuilder replyMSG = new TextBuilder("<html><body><br>");

			switch (val)
			{
				case 1:
					FastMap<Integer, String> matches = OlympiadManager.getInstance().getAllTitles();
					replyMSG.append("Наблюдение за Великой Олимпиадой<br>" +
							"Предупреждение: Вы не сможете наблюдать за Олимпиадой, если у Вас вызван питомец или слуга.<br><br>");

					for (int i = 0; i < Olympiad.getStadiumCount(); i++)
					{
						int arenaID = i + 1;
						String title = "";
						if (matches.containsKey(i))
							title = matches.get(i);
						else
							title = "Подготовка";
						replyMSG.append("<a action=\"bypass -h npc_"+getObjectId()+"_Olympiad 3_" + i + "\">" + "Арена-" + arenaID + "&nbsp;&nbsp;&nbsp;" + title + "</a><br>");
					}
					replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>");
					replyMSG.append("<table width=270 border=0 cellpadding=0 cellspacing=0>");
					replyMSG.append("<tr><td width=90 height=20 align=center>");
					replyMSG.append("<button value=\"Назад\" action=\"bypass -h npc_"+getObjectId()+"_Chat 0\" width=80 height=27 back=\"sek.cbui94\" fore=\"L2UI_CT1.Button_DF\">");
					replyMSG.append("</td></tr></table></body></html>");

					reply.setHtml(replyMSG.toString());
					player.sendPacket(reply);
					break;
				case 2:
					// for example >> Olympiad 1_88
					int classId = Integer.parseInt(command.substring(11));
					if (classId >= 88 && classId <= 118)
					{
						replyMSG.append("<center>Ранги Великой Олимпиады");
						replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>");

						FastList<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
						if (!names.isEmpty())
						{
							replyMSG.append("<table width=270 border=0 bgcolor=\"000000\">");
							int index = 1;
							for (String name : names)
							{
								replyMSG.append("<tr>");
								replyMSG.append("<td align=\"left\">" + index++ + "</td>");
								replyMSG.append("<td align=\"right\">" + name + "</td>");
								replyMSG.append("</tr>");
							}
							replyMSG.append("</table>");
						}
						replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>");
						replyMSG.append("<button value=\"Назад\" action=\"bypass -h npc_" + getObjectId() + "_Chat 0\" back=\"sek.cbui94\" fore=\"L2UI_CT1.Button_DF\" width=80 height=27>");
						replyMSG.append("</center>");
						replyMSG.append("</body></html>");
						reply.setHtml(replyMSG.toString());
						player.sendPacket(reply);
					}
					break;
				case 3:
					int id = Integer.parseInt(command.substring(11));
					Olympiad.addSpectator(id, player, true);
					break;
				case 4:
					player.sendPacket(new ExHeroList());
					break;
				default:
					_log.warn("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	private void showChatWindow(L2PcInstance player, int val, String suffix)
	{
		String filename = Olympiad.OLYMPIAD_HTML_PATH;

		filename += "noble_desc" + val;
		filename += (suffix != null) ? suffix + ".htm" : ".htm";

		if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm"))
			filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
		showChatWindow(player, filename);
	}
}