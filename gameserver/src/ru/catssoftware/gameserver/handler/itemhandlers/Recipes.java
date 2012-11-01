package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.RecipeController;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2RecipeList;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class Recipes implements IItemHandler
{
	private final int[]	ITEM_IDS;

	public Recipes()
	{
		ITEM_IDS = RecipeController.getInstance().getAllItemIds();
	}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2RecipeList rp = RecipeController.getInstance().getRecipeByItemId(item.getItemId());
		if (activeChar.hasRecipeList(rp.getId()))
			activeChar.sendPacket(SystemMessageId.RECIPE_ALREADY_REGISTERED);
		else
		{
			if (rp.isDwarvenRecipe())
			{
				if (activeChar.hasDwarvenCraft())
				{
					if (rp.getLevel() > activeChar.getDwarvenCraft())
						activeChar.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
					else if (activeChar.getDwarvenRecipeBook().length >= activeChar.getDwarfRecipeLimit())
					{
						//Up to $s1 recipes can be registered.
						SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER);
						sm.addNumber(activeChar.getDwarfRecipeLimit());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.registerDwarvenRecipeList(rp);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RECIPE_ADD_DWARV),item.getItemName()));
					}
				}
				else
					activeChar.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
			}
			else
			{
				if (activeChar.hasCommonCraft())
				{
					if (rp.getLevel() > activeChar.getCommonCraft())
						activeChar.sendPacket(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
					else if (activeChar.getCommonRecipeBook().length >= activeChar.getCommonRecipeLimit())
					{
						//Up to $s1 recipes can be registered.
						SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER);
						sm.addNumber(activeChar.getCommonRecipeLimit());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.registerCommonRecipeList(rp);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_RECIPE_ADD_COMMON),item.getItemName()));
					}
				}
				else
					activeChar.sendPacket(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
			}
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}