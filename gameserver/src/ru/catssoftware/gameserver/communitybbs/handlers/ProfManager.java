package ru.catssoftware.gameserver.communitybbs.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.communitybbs.IBBSHandler;
import ru.catssoftware.gameserver.datatables.CharTemplateTable;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import javolution.text.TextBuilder;

public class ProfManager implements IBBSHandler {

	private static final String NOT_ALLOWED = "<html><body><br><br><center>Класс Мастер в данный момент недоступен</center></body></html>"; 

	@Override
	public String[] getCommands() {
		return new String [] {"change_class"};
	}
	

	@Override
	public String handleCommand(L2PcInstance player,
			String command, String args) {
		if (player.getGameEvent()!=null || player.isInCombat() || Olympiad.getInstance().isRegistered(player) ||
				Olympiad.getInstance().isRegisteredInComp(player)) {
			return NOT_ALLOWED;
		}
		
		if(args==null || args.length()==0) {
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body><center><br><br><br><font color=\"006600\">Класс Мастер:</font><br>");
			ClassId classId = player.getClassId();
			int level = player.getLevel();
			int jobLevel = classId.level();
			int newJobLevel = jobLevel + 1;

			if ((((level >= 20 && jobLevel == 0) || (level >= 40 && jobLevel == 1) || (level >= 76 && jobLevel == 2)) && Config.CLASS_MASTER_SETTINGS.isAllowed(newJobLevel)))
			{
				if (((level >= 20 && jobLevel == 0) || (level >= 40 && jobLevel == 1) || (level >= 76 && jobLevel == 2)) && Config.CLASS_MASTER_SETTINGS.isAllowed(newJobLevel))
				{
					sb.append("Список доступных классов:<br>");

					for (ClassId child : ClassId.values())
					{
						if (child.childOf(classId) && child.level() == newJobLevel)
							sb.append("<br><a action=\"bypass _bbschange_class " + (child.getId()) + "\"> " + CharTemplateTable.getClassNameById(child.getId()) + "</a>");
					}

					if (Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel) != null && !Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).isEmpty())
					{
						sb.append("<br><br>Список необходимых предметов:");
						sb.append("<table width=270>");
						for (Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keySet())
						{
							int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
							sb.append("<tr><td><font color=\"LEVEL\">" + _count + "</font></td><td>" + ItemTable.getInstance().getTemplate(_itemId).getName() + "</td></tr>");
						}
						sb.append("</table>");
					}
				}

				sb.append("<br>");
			}
			else
			{
				switch (jobLevel)
				{
					case 0:
						if (Config.CLASS_MASTER_SETTINGS.isAllowed(1))
							sb.append("Вернитесь, когда достигните 20 уровня.<br>");
						else if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
							sb.append("Вернитесь после получения 1 профессии.<br>");
						else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
							sb.append("Вернитесь после получения 2 профессии.<br>");
						else
							sb.append("Вы не межете изменить вашу профессию.<br>");
						break;
					case 1:
						if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
							sb.append("Вернитесь, когда достигните 40 уровня.<br>");
						else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
							sb.append("Вернитесь после получения 2 профессии.<br>");
						else
							sb.append("Вы не межете изменить вашу профессию.<br>");
						break;
					case 2:
						if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
							sb.append("Вернитесь, когда достигните 76 уровня.<br>");
						else
							sb.append("Вы не межете изменить вашу профессию.<br>");
						break;
					case 3:
						sb.append("Больше нет доступных профессий.<br>");
						break;
				}
				sb.append("<br>");
			}
			sb.append("</center></body></html>");
			return sb.toString();
			
		} else {
			int val = Integer.parseInt(args);

			ClassId classId = player.getClassId();
			ClassId newClassId = ClassId.values()[val];

			int level = player.getLevel();
			int jobLevel = classId.level();
			int newJobLevel = newClassId.level();

			if (!Config.CLASS_MASTER_SETTINGS.isAllowed(newJobLevel))
				return NOT_ALLOWED;;
			if (!newClassId.childOf(classId))
				return NOT_ALLOWED;;
			if (newJobLevel != jobLevel + 1)
				return NOT_ALLOWED;;
			if (level < 20 && newJobLevel > 1)
				return NOT_ALLOWED;;
			if (level < 40 && newJobLevel > 2)
				return NOT_ALLOWED;;
			if (level < 76 && newJobLevel > 3)
				return NOT_ALLOWED;;

			// Weight/Inventory check 
			if(!Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).isEmpty()) 
			{ 
				if (player.getWeightPenalty() >= 3 || (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize())) 
				{ 
					player.sendPacket(new SystemMessage(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT)); 
					return NOT_ALLOWED;
				} 
			} 
			
			for (Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keySet())
			{
				int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
				if (player.getInventory().getInventoryItemCount(_itemId, -1) < _count)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return NOT_ALLOWED;
				}
			}
			for (Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).keySet())
			{
				int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel).get(_itemId);
				player.destroyItemByItemId("ClassMaster", _itemId, _count, player, true);
			}
			for (Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).keySet())
			{
				int _count = Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).get(_itemId);
				player.addItem("ClassMaster", _itemId, _count, player, true);
			}

			changeClass(player, val);

			player.rewardSkills();

			if (newJobLevel == 3)
				player.sendPacket(SystemMessageId.THIRD_CLASS_TRANSFER);
			else
				player.sendPacket(SystemMessageId.CLASS_TRANSFER);

			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("<br><br><center>");
			sb.append("Поздравляю, Вы получили класс: <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getClassId().getId()) + "</font>.");
			sb.append("</center></body></html>");
			player.refreshOverloaded();
			player.refreshExpertisePenalty();
			return sb.toString();
		}
	}
	
	private void changeClass(L2PcInstance player, int val)
	{
		player.setClassId(val);

		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());

		player.broadcastUserInfo(true);
		player.broadcastClassIcon();
	}

}
