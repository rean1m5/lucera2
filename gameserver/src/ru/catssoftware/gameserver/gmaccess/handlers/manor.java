package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import javolution.text.TextBuilder;
import javolution.util.FastList;


public class manor extends gmHandler
{
	private static final String[] commands =
	{
			"manor",
			"manor_approve",
			"manor_setnext",
			"manor_reset",
			"manor_setmaintenance",
			"manor_save",
			"manor_disable"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		if (command.equals("manor"))
		{
			showMainPage(admin);
		}
		else if (command.equals("manor_setnext"))
		{
			CastleManorManager.getInstance().setNextPeriod();
			CastleManorManager.getInstance().setNewManorRefresh();
			CastleManorManager.getInstance().updateManorRefresh();
			admin.sendMessage("Manor System: set to next period");
			showMainPage(admin);
		}
		else if (command.equals("manor_approve"))
		{
			CastleManorManager.getInstance().approveNextPeriod();
			CastleManorManager.getInstance().setNewPeriodApprove();
			CastleManorManager.getInstance().updatePeriodApprove();
			admin.sendMessage("Manor System: next period approved");
			showMainPage(admin);
		}
		else if (command.equals("manor_reset"))
		{
			int castleId = 0;
			try
			{
				castleId = Integer.parseInt(params[1]);
			}
			catch (Exception e)
			{
			}

			if (castleId > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(castleId);
				castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
				castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
				castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
				castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
				if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					castle.saveCropData();
					castle.saveSeedData();
				}
				admin.sendMessage("Manor data for " + castle.getName() + " was nulled");
			}
			else
			{
				for (Castle castle : CastleManager.getInstance().getCastles().values())
				{
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
					if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
					{
						castle.saveCropData();
						castle.saveSeedData();
					}
				}
				admin.sendMessage("Manor data was nulled");
			}
			showMainPage(admin);
		}
		else if (command.equals("manor_setmaintenance"))
		{
			boolean mode = CastleManorManager.getInstance().isUnderMaintenance();
			CastleManorManager.getInstance().setUnderMaintenance(!mode);
			if (mode)
				admin.sendMessage("Manor System: not under maintenance");
			else
				admin.sendMessage("Manor System: under maintenance");
			showMainPage(admin);
		}
		else if (command.equals("manor_save"))
		{
			CastleManorManager.getInstance().saveData();
			admin.sendMessage("Manor System: all data saved");
			showMainPage(admin);
		}
		else if (command.equals("manor_disable"))
		{
			boolean mode = CastleManorManager.getInstance().isDisabled();
			CastleManorManager.getInstance().setDisabled(!mode);
			if (mode)
				admin.sendMessage("Manor System: enabled");
			else
				admin.sendMessage("Manor System: disabled");
			showMainPage(admin);
		}

		return;
	}

	private String formatTime(long millis)
	{
		String s = "";
		int secs = (int) millis / 1000;
		int mins = secs / 60;
		secs -= mins * 60;
		int hours = mins / 60;
		mins -= hours * 60;

		if (hours > 0)
			s += hours + ":";
		s += mins + ":";
		s += secs;
		return s;
	}

	private void showMainPage(L2PcInstance admin)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<center><font color=\"LEVEL\"> [Manor System] </font></center><br>");
		replyMSG.append("<table width=\"100%\"><tr><td>");
		replyMSG.append("Disabled: " + (CastleManorManager.getInstance().isDisabled() ? "yes" : "no") + "</td><td>");
		replyMSG.append("Under Maintenance: " + (CastleManorManager.getInstance().isUnderMaintenance() ? "yes" : "no") + "</td></tr><tr><td>");
		replyMSG.append("Time to refresh: " + formatTime(CastleManorManager.getInstance().getMillisToManorRefresh()) + "</td><td>");
		replyMSG.append("Time to approve: " + formatTime(CastleManorManager.getInstance().getMillisToNextPeriodApprove()) + "</td></tr>");
		replyMSG.append("</table>");

		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Set Next\" action=\"bypass -h admin_manor_setnext\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td>");
		replyMSG.append("<button value=\"Approve Next\" action=\"bypass -h admin_manor_approve\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr><tr><td>");
		replyMSG.append("<button value=\"" + (CastleManorManager.getInstance().isUnderMaintenance() ? "Set normal" : "Set mainteance")
				+ "\" action=\"bypass -h admin_manor_setmaintenance\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td>");
		replyMSG.append("<button value=\"" + (CastleManorManager.getInstance().isDisabled() ? "Enable" : "Disable")
				+ "\" action=\"bypass -h admin_manor_disable\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr><tr><td>");
		replyMSG.append("<button value=\"Refresh\" action=\"bypass -h admin_manor\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td>");
		replyMSG.append("<button value=\"Back\" action=\"bypass -h admin_admin\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
		replyMSG.append("</table></center>");

		replyMSG.append("<br><center>Castle Information:<table width=\"100%\">");
		replyMSG.append("<tr><td></td><td>Current Period</td><td>Next Period</td></tr>");

		for (Castle c : CastleManager.getInstance().getCastles().values())
		{
			replyMSG.append("<tr><td>" + c.getName() + "</td>" + "<td>" + c.getManorCost(CastleManorManager.PERIOD_CURRENT) + "a</td>" + "<td>"
					+ c.getManorCost(CastleManorManager.PERIOD_NEXT) + "a</td>" + "</tr>");
		}

		replyMSG.append("</table><br>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		admin.sendPacket(adminReply);
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}