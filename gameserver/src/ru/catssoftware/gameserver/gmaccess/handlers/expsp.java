package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author m095
 * @version 1.0
 */

public class expsp extends gmHandler
{
	private static final String[] commands =
	{
		"add_exp_sp_to_character",
		"add_exp_sp",
		"remove_exp_sp"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		if (command.equals("add_exp_sp"))
		{
			try
			{
				long exp = Long.parseLong(params[1]);
				long sp =  Long.parseLong(params[2]);
				if (!adminAddExpSp(admin, exp, sp))
					admin.sendMessage("Используйте: //add_exp_sp exp sp");
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //add_exp_sp exp sp");
			}
		}
		else if (command.equals("remove_exp_sp"))
		{
			try
			{
				long exp = Long.parseLong(params[1]);
				long sp =  Long.parseLong(params[2]);
				if (!adminRemoveExpSP(admin, exp, sp))
					admin.sendMessage("Используйте: //remove_exp_sp exp sp");
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //remove_exp_sp exp sp");
			}
		}
		addExpSp(admin);
	}

	private void addExpSp(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		if (!(target.isPlayer()))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		L2PcInstance player = (L2PcInstance) target;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/menus/submenus/expsp_menu.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		activeChar.sendPacket(adminReply);
	}

	private boolean adminAddExpSp(L2PcInstance activeChar, long exp, long sp)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		if (!(target.isPlayer()))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		
		L2PcInstance player = (L2PcInstance) target;
		if (exp != 0 || sp != 0)
		{
			player.sendMessage("Gm добавил Вам " + exp + " xp and " + sp + " sp");
			player.addExpAndSp(exp, (int) sp);
			activeChar.sendMessage("Добавлено " + exp + " xp and " + sp + " sp игроку " + player.getName());
		}
		return true;
	}

	private boolean adminRemoveExpSP(L2PcInstance activeChar, long exp, long sp)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		if (!(target.isPlayer()))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		
		L2PcInstance player = (L2PcInstance) target;
		if (exp != 0 || sp != 0)
		{
			player.sendMessage("Gm удалил Вам " + exp + " xp and " + sp + " sp");
			player.removeExpAndSp(exp, (int) sp);
			activeChar.sendMessage("Удалено " + exp + " xp and " + sp + " sp игроку " + player.getName());
		}
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}