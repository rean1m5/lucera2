package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.util.Util;

public class RequestGetItemFromPet extends L2GameClientPacket
{
	private static final String	REQUESTGETITEMFROMPET__C__8C	= "[C] 8C RequestGetItemFromPet";
	private int					_objectId;
	private int					_amount;
	@SuppressWarnings("unused")
	private int					_unknown;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_amount = readD();
		_unknown = readD();// = 0 for most trades
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null || !(player.getPet() instanceof L2PetInstance))
			return;
		L2PetInstance pet = (L2PetInstance) player.getPet();

		if (_amount < 0)
		{
			Util.handleIllegalPlayerAction(player, "[RequestGetItemFromPet] count < 0! ban! oid: " + _objectId + " owner: " + player.getName(), Config.DEFAULT_PUNISH);
			return;
		}
		else if (_amount == 0)
			return;

		if (pet.transferItem("Transfer", _objectId, _amount, player.getInventory(), player, pet) == null)
		{
			_log.info("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
		}
	}

	@Override
	public String getType()
	{
		return REQUESTGETITEMFROMPET__C__8C;
	}
}
