package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;

public class enchant extends gmHandler
{
	private static final String[] commands =
	{ 
		"seteh",
		"setec",
		"seteg",
		"setel",
		"seteb",
		"setew",
		"setes",
		"setle",
		"setre",
		"setlf",
		"setrf",
		"seten",
		"setun",
		"setba",
		"enchant"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String command = params[0];

		if (command.equals("enchant"))
		{
			showMainPage(admin);
			return;
		}
		else
		{
			int armorType = -1;

			if (command.equals("seteh"))
				armorType = Inventory.PAPERDOLL_HEAD;
			else if (command.equals("setec"))
				armorType = Inventory.PAPERDOLL_CHEST;
			else if (command.equals("seteg"))
				armorType = Inventory.PAPERDOLL_GLOVES;
			else if (command.equals("seteb"))
				armorType = Inventory.PAPERDOLL_FEET;
			else if (command.equals("setel"))
				armorType = Inventory.PAPERDOLL_LEGS;
			else if (command.equals("setew"))
				armorType = Inventory.PAPERDOLL_RHAND;
			else if (command.equals("setes"))
				armorType = Inventory.PAPERDOLL_LHAND;
			else if (command.equals("setle"))
				armorType = Inventory.PAPERDOLL_LEAR;
			else if (command.equals("setre"))
				armorType = Inventory.PAPERDOLL_REAR;
			else if (command.equals("setlf"))
				armorType = Inventory.PAPERDOLL_LFINGER;
			else if (command.equals("setrf"))
				armorType = Inventory.PAPERDOLL_RFINGER;
			else if (command.equals("seten"))
				armorType = Inventory.PAPERDOLL_NECK;
			else if (command.equals("setun"))
				armorType = Inventory.PAPERDOLL_UNDER;

			if (armorType != -1)
			{
				try
				{
					int ench = Integer.parseInt(params[1]);
					if (ench < 0 || ench > Config.GM_MAX_ENCHANT) 
						admin.sendMessage("Вы не можете точить вещи выше " + Config.GM_MAX_ENCHANT);
					else
						setEnchant(admin, ench, armorType);
				}
				catch (StringIndexOutOfBoundsException e)
				{
					admin.sendMessage("Введите уровень заточки");
				}
				catch (NumberFormatException e)
				{
					admin.sendMessage("Введите уровень заточки");
				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					admin.sendMessage("Введите уровень заточки");
				}
			}
			showMainPage(admin);
		}
	}

	private void setEnchant(L2PcInstance activeChar, int ench, int armorType)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;
		L2PcInstance player = null;

		if (target.isPlayer())
			player = (L2PcInstance) target;
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		int curEnchant = 0;
		L2ItemInstance itemInstance = null;

		L2ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);
		if (parmorInstance != null && parmorInstance.getLocationSlot() == armorType)
			itemInstance = parmorInstance;
		else
		{
			parmorInstance = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
			if (parmorInstance != null && parmorInstance.getLocationSlot() == Inventory.PAPERDOLL_LRHAND)
				itemInstance = parmorInstance;
		}

		if (itemInstance != null)
		{
			curEnchant = itemInstance.getEnchantLevel();
			player.getInventory().unEquipItemInSlotAndRecord(armorType);
			itemInstance.setEnchantLevel(ench);
			player.getInventory().equipItemAndRecord(itemInstance);
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemInstance);
			player.sendPacket(iu);
			player.broadcastUserInfo(true);

			activeChar.sendMessage("Игроку " + player.getName() + " изменен уровень точки вещи " + itemInstance.getItem().getName() + " с " + curEnchant + " на " + ench + ".");
			if (activeChar != player)
				player.sendMessage("GM изменил уровень точки вещи " + itemInstance.getItem().getName() + " с " + curEnchant + " на " + ench + ".");
		}
	}

	public void showMainPage(L2PcInstance activeChar)
	{
		methods.showSubMenuPage(activeChar, "enchant_menu.htm");
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}