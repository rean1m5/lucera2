package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ManufactureList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.zone.L2TradeZone;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.RecipeShopManageList;

public class RequestRecipeShopManageList extends L2GameClientPacket
{
	private static final String	_C__B0_RequestRecipeShopManageList	= "[C] b0 RequestRecipeShopManageList";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (player.isAlikeDead())
		{
			ActionFailed();
			return;
		}
		player.revalidateZone(true);
		L2TradeZone z = (L2TradeZone)player.getZone("Trade");
		if(z!=null && !z.canCrfat()) {
			sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isInsideZone(L2Zone.FLAG_NOSTORE))
		{
			player.sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);
			ActionFailed();
			return;
		}

		if (player.getPrivateStoreType() != 0)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			player.broadcastUserInfo(true);
			if (player.isSitting())
				player.standUp();
		}

		if (player.getCreateList() == null)
		{
			player.setCreateList(new L2ManufactureList());
		}

		player.sendPacket(new RecipeShopManageList(player, true));
	}

	@Override
	public String getType()
	{
		return _C__B0_RequestRecipeShopManageList;
	}
}
