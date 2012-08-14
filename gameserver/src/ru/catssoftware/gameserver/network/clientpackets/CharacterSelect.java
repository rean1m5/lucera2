package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.L2GameClient.GameClientState;
import ru.catssoftware.gameserver.network.serverpackets.CharSelected;
import ru.catssoftware.gameserver.network.serverpackets.SSQInfo;

public class CharacterSelect extends L2GameClientPacket
{
	private static final String	_C__0D_CHARACTERSELECT	= "[C] 0D CharacterSelect";

	// cd
	private int					_charSlot;
	@SuppressWarnings("unused")
	private int					_unk1;
	@SuppressWarnings("unused")
	private int					_unk2;
	@SuppressWarnings("unused")
	private int					_unk3;
	@SuppressWarnings("unused")
	private int					_unk4;

	@Override
	protected void readImpl()
	{
		_charSlot = readD();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readD();
	}

	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() != null)
			return;

		L2PcInstance cha = getClient().loadCharFromDisk(_charSlot);
		if (cha == null)
		{
			_log.fatal("Character could not be loaded (slot:"+_charSlot+")");
			return;
		}

		if (cha.isBanned())
		{
			new Disconnection(getClient(), cha).defaultSequence(false);
			return;
		}

		cha.setClient(getClient());
		getClient().setActiveChar(cha);
		getClient().setState(GameClientState.IN_GAME);
		sendPacket(new SSQInfo());
		sendPacket(new CharSelected(cha, getClient().getSessionId().playOkID1));
	}

	@Override
	public String getType()
	{
		return _C__0D_CHARACTERSELECT;
	}
}