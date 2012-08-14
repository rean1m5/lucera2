package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2ManufactureItem;
import ru.catssoftware.gameserver.model.L2ManufactureList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.RecipeShopMsg;

public class RequestRecipeShopListSet extends L2GameClientPacket
{
	private static final String	_C__B2_RequestRecipeShopListSet	= "[C] b2 RequestRecipeShopListSet";

	private int					_count;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_count = readD();
		if (_count < 0 || _count * 8 > getByteBuffer().remaining() || _count > Config.MAX_ITEM_IN_PACKET)
				_count = 0;
		_items = new int[_count * 2];
		for (int x = 0; x < _count; x++)
		{
			int recipeID = readD(); _items[(x * 2)] = recipeID;
			int cost = readD(); _items[x*2 + 1] = cost;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (player.isOfflineTrade())
			return;

		if (player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_CRAFT_DURING_COMBAT);
			return;
		}
		player.stopMove();
		if (player.isInsideZone(L2Zone.FLAG_NOSTORE))
		{
			player.sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_count>20)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_count == 0)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			player.broadcastUserInfo();
			player.standUp();
		}
		else
		{
			L2ManufactureList createList = new L2ManufactureList();

			for (int x = 0; x < _count; x++)
			{
				int recipeID = _items[(x * 2)];
				int cost = _items[x * 2 + 1];
				createList.add(new L2ManufactureItem(recipeID, cost));
			}
			createList.setStoreName(player.getCreateList() != null ? player.getCreateList().getStoreName() : "");
			player.setCreateList(createList);

			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_MANUFACTURE);
			player.sitDown();
			player.broadcastUserInfo();
			player.broadcastPacket(new RecipeShopMsg(player));
		}
	}

	@Override
	public String getType()
	{
		return _C__B2_RequestRecipeShopListSet;
	}
}