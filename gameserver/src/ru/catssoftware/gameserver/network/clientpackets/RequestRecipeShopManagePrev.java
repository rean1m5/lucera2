package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.RecipeShopSellList;

public class RequestRecipeShopManagePrev extends L2GameClientPacket
{
	private static final String	_C__B7_RequestRecipeShopPrev	= "[C] b7 RequestRecipeShopPrev";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null || player.getTarget() == null)
			return;

		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (player.isAlikeDead())
		{
			ActionFailed();
			return;
		}

		if (!(player.getTarget().isPlayer()))
			return;
		L2PcInstance target = (L2PcInstance) player.getTarget();
		player.sendPacket(new RecipeShopSellList(player, target));
	}

	@Override
	public String getType()
	{
		return _C__B7_RequestRecipeShopPrev;
	}
}
