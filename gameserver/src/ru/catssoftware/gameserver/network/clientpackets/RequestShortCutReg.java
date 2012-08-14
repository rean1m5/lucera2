package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ShortCut;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ShortCutRegister;

public class RequestShortCutReg extends L2GameClientPacket
{
	private static final String	_C__33_REQUESTSHORTCUTREG	= "[C] 33 RequestShortCutReg";
	@SuppressWarnings("unused")
	private int					_type, _id, _slot, _page, _lvl, _characterType;

	@Override
	protected void readImpl()
	{
		_type = readD();
		int slot = readD();
		_id = readD();
		_characterType = readD();

		_slot = slot % 12;
		_page = slot / 12;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		switch (_type)
		{
		case 0x01: // item
		case 0x03: // action
		case 0x04: // macro
		case 0x05: // recipe
		{
			L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, -1, _characterType);
			sendPacket(new ShortCutRegister(sc));
			activeChar.registerShortCut(sc);
			break;
		}
		case 0x02: // skill
		{
			int _lvl = activeChar.getSkillLevel(_id);
			if(_lvl > 0)
			{
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id,  _lvl, _characterType);
				sendPacket(new ShortCutRegister(sc));
				activeChar.registerShortCut(sc);
			}
			break;
		}
		}
	}

	@Override
	public String getType()
	{
		return _C__33_REQUESTSHORTCUTREG;
	}
}