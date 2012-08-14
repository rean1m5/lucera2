package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.RecipeController;
import ru.catssoftware.gameserver.model.L2RecipeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.RecipeBookItemList;

public class RequestRecipeBookDestroy extends L2GameClientPacket
{
	private static final String	_C__AC_REQUESTRECIPEBOOKDESTROY	= "[C] AD RequestRecipeBookDestroy";
	private int					_recipeId;

	@Override
	protected void readImpl()
	{
		_recipeId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar != null)
		{
			L2RecipeList rp = RecipeController.getInstance().getRecipeList(_recipeId);
			if (rp == null)
				return;

			activeChar.unregisterRecipeList(_recipeId);

			RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), activeChar.getMaxMp());
			if (rp.isDwarvenRecipe())
				response.addRecipes(activeChar.getDwarvenRecipeBook());
			else
				response.addRecipes(activeChar.getCommonRecipeBook());

			activeChar.sendPacket(response);
		}
	}

	@Override
	public String getType()
	{
		return _C__AC_REQUESTRECIPEBOOKDESTROY;
	}
}