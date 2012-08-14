package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.MercTicketManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;

public class RequestPetGetItem extends L2GameClientPacket
{
	private static final String	_C__8f_REQUESTPETGETITEM	= "[C] 8F RequestPetGetItem";
	private int					_objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		player._bbsMultisell = 0;
		L2Object obj = player.getKnownList().getKnownObject(_objectId);

		if (obj == null)
			obj = L2World.getInstance().findObject(_objectId);

		if (!(obj instanceof L2ItemInstance))
			return;

		if (player.getPet() == null || player.getPet() instanceof L2SummonInstance)
		{
			ActionFailed();
			return;
		}
		
		L2ItemInstance item = (L2ItemInstance) obj;
		if (item == null)
			return;
		if (item.getItemId() == Config.FORTSIEGE_COMBAT_FLAG_ID)
		{
			player.sendMessage("Питомец не может получить Combat Flag.");
			return;
		}

		int castleId = MercTicketManager.getInstance().getTicketCastleId(item.getItemId());
		if (castleId > 0)
		{
			ActionFailed();
			return;
		}
		L2PetInstance pet = (L2PetInstance) player.getPet();
		if (pet.isDead() || pet.isOutOfControl())
		{
			ActionFailed();
			return;
		}
		pet.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, item);
	}

	@Override
	public String getType()
	{
		return _C__8f_REQUESTPETGETITEM;
	}
}