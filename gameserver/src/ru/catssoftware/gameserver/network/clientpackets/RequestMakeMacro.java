package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Macro;
import ru.catssoftware.gameserver.model.L2Macro.L2MacroCmd;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

/*
 * Rework by L2CatsSoftware DevTeam
 * Update 24/06/09
 */

public class RequestMakeMacro extends L2GameClientPacket
{
	private L2Macro				_macro;
	private int					_commandsLenght			= 0;
	private String				_commandIndex			= null;

	private static final String		_C__C1_REQUESTMAKEMACRO		= "[C] C1 RequestMakeMacro";

	@Override
	protected void readImpl()
	{
		int _id = readD();
		String _name = readS();
		String _desc = readS();
		String _acronym = readS();
		int _icon = readC();
		int _count = readC();
		if (_count > 12)
			_count = 12;

		L2MacroCmd[] commands = new L2MacroCmd[_count];
		for (int i = 0; i < _count; i++)
		{
			int entry = readC();
			int type = readC();
			int d1 = readD();
			int d2 = readC();
			String command = readS();
				if (command == null)
					return;

			_commandIndex = command;
			_commandsLenght += command.length();

		commands[i] = new L2MacroCmd(entry, type, d1, d2, command);
		}
		_macro = new L2Macro(_id, _icon, _name, _desc, _acronym, commands);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (_commandsLenght > 255)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_MACRO);
			return;
		}
		if (activeChar.getMacroses().getAllMacroses().length > 48)
		{
			activeChar.sendPacket(SystemMessageId.YOU_MAY_CREATE_UP_TO_48_MACROS);
			return;
		}
		if (_macro.name.length() == 0)
		{
			activeChar.sendPacket(SystemMessageId.ENTER_THE_MACRO_NAME);
			return;
		}
		if (_macro.descr.length() > 32)
		{
			activeChar.sendPacket(SystemMessageId.MACRO_DESCRIPTION_MAX_32_CHARS);
			return;
		}
		if (Config.CHECK_PLAYER_MACRO && !activeChar.isGM())
		{
			if (isRestrictedCommand(_commandIndex))
			{
				activeChar.sendPacket(SystemMessageId.INVALID_MACRO);
				return;
			}
		}
		activeChar.registerMacro(_macro);
	}

	public boolean isRestrictedCommand(String cmd)
	{
		return (Config.LIST_MACRO_RESTRICTED_WORDS.contains(cmd));
	}

	@Override
	public String getType()
	{
		return _C__C1_REQUESTMAKEMACRO;
	}
}
