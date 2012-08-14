package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.datatables.GmListTable;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2ControllableMobInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.model.base.Experience;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.PetInfo;
import ru.catssoftware.gameserver.taskmanager.DecayTaskManager;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Item;

public class gmcommands extends gmHandler
{
	private static final String[] commands =
	{
		"gmliston",
		"gmlistoff",
		"silence",
		"diet",
		"tradeoff",
		"summon_npc",
		"summon_item",
		"unsummon",
		"itemcreate",
		"create_item",
		"clearpc",
		"create_adena",
		"summon",
		"ride_wyvern",
		"ride_strider",
		"ride_wolf",
		"unride_wyvern",
		"unride_strider",
		"unride_wolf",
		"ride_horse",
		"unride",
		"res",
		"gmchat",
		"snoop",
		"unsnoop"
	};
	
	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];
		
		if (command.equals("gmliston"))
		{
			GmListTable.getInstance().setGmOption(admin, false);
			admin.sendMessage("Вы зарегистрировались в gmList");
			return;
		}
		else if (command.equals("gmlistoff"))
		{
			GmListTable.getInstance().setGmOption(admin, true);
			admin.sendMessage("Вы отменили регистрацию в gmList");
			return;
		}
		else if (command.equals("silence"))
		{
			if (admin.getMessageRefusal())
			{
				admin.setMessageRefusal(false);
				admin.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				admin.setMessageRefusal(true);
				admin.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
			return;
		}
		else if (command.equals("diet"))
		{
			try
			{
				if (params[1].equalsIgnoreCase("on"))
				{
					admin.setDietMode(true);
					admin.sendMessage("Лимит веса отключен");
				}
				else if (params[1].equalsIgnoreCase("off"))
				{
					admin.setDietMode(false);
					admin.sendMessage("Лимит веса включен");
				}
			}
			catch (Exception e)
			{
				if (admin.getDietMode())
				{
					admin.setDietMode(false);
					admin.sendMessage("Лимит веса включен");
				}
				else
				{
					admin.setDietMode(true);
					admin.sendMessage("Лимит веса отключен");
				}
			}
			finally
			{
				admin.refreshOverloaded();
			}
			return;
		}
		else if (command.equals("tradeoff"))
		{
			try
			{
				if (params[1].equalsIgnoreCase("on"))
				{
					admin.setTradeRefusal(true);
					admin.sendMessage("Трейд выключен");
				}
				else if (params[1].equalsIgnoreCase("off"))
				{
					admin.setTradeRefusal(false);
					admin.sendMessage("Трейд включен");
				}
			}
			catch (Exception ex)
			{
				if (admin.getTradeRefusal())
				{
					admin.setTradeRefusal(false);
					admin.sendMessage("Трейд выключен");
				}
				else
				{
					admin.setTradeRefusal(true);
					admin.sendMessage("Трейд включен");
				}
			}
			return;
		}
		else if (command.equals("summon_npc"))
		{
			try
			{
				int npcId = Integer.parseInt(params[1]);
				if (npcId != 0)
					adminSummon(admin, npcId);
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //summon_npc <npcid>");
			}
			return;
		}
		else if (command.equals("summon_item"))
		{
			try
			{
				int id = Integer.parseInt(params[1]);
				if (admin.addItem("GM", id, 1, admin, true, true) == null)
					admin.sendMessage("Указанная вещь не может быть создана");
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //summon_item <itemid>");
			}
			return;
		}
		else if (command.equals("unsummon"))
		{
			if (admin.getPet() != null)
				admin.getPet().unSummon(admin);
		}
		if (command.equals("itemcreate"))
		{
			methods.showSubMenuPage(admin, "itemcreation_menu.htm");
		}
		else if (command.startsWith("create_adena"))
		{
			try
			{
				int numval = Integer.parseInt(params[1]);

				L2Object target = admin.getTarget();
				L2PcInstance player = null;

				if (target instanceof L2PcInstance)
					player = (L2PcInstance) target;
				else
					player = admin;

				player.getInventory().addItem("admin_create_adena", 57, numval, admin, admin);
				player.sendMessage(admin.getName() + " добавил Вам " + numval + " Аден в инвентарь");
				admin.sendMessage("Вы добавили " + numval + " Аден игроку " + player.getName());
			}
			catch (Exception e)
			{
				admin.sendMessage("Невозможно создать адену");
			}
		}
		else if (command.startsWith("create_item") || command.startsWith("summon"))
		{
			try
			{
				int idval = Integer.parseInt(params[1]);
				int numval = Integer.parseInt(params[2]);
				L2PcInstance player = admin;
				if (params.length == 4) {
					if (admin.getTarget() instanceof L2PcInstance)
						player = (L2PcInstance)admin.getTarget();
				}
				createItem(player, idval, numval);
			}
			catch (Exception e)
			{
			}
			methods.showSubMenuPage(admin, "itemcreation_menu.htm");
		}
		else if (command.equals("clearpc"))
			removeAllItems(admin);
		else if (command.startsWith("ride"))
		{
			int _petRideId = 0;

			if (admin.isMounted() || admin.getPet() != null)
			{
				admin.sendMessage("Вы уже управляете питомцем");
				return;
			}
			if (command.equals("ride_wyvern"))
				_petRideId = 12621;
			else if (command.equals("ride_strider"))
				_petRideId = 12526;
			else if (command.equals("ride_wolf"))
				_petRideId = 16041;
			else if (command.equals("ride_horse"))
				_petRideId = 13130;
			else
			{
				admin.sendMessage("Command '" + command + "' not recognized");
				return;
			}
			admin.mount(_petRideId, 0, false);
			return;
		}
		else if (command.startsWith("unride"))
		{
			admin.dismount();
			return;
		}
		else if (command.equals("res"))
		{
			if (params.length > 1)
			{
				try
				{
					int radius = Integer.parseInt(params[1]);

					for (L2Character knownChar : admin.getKnownList().getKnownCharactersInRadius(radius))
					{
						if (knownChar==null || knownChar instanceof L2ControllableMobInstance)
							continue;
						doResurrect(knownChar, admin);
					}
					admin.sendMessage("Воскрешение завершено");
				}
				catch (Exception e)
				{
					admin.sendMessage("Используйте: //res [radius]");
					return;
				}
			}
			else
			{
				L2Object obj = admin.getTarget();
				if (obj == null)
					obj = admin;
				
				if (obj instanceof L2Character && !(obj instanceof L2ControllableMobInstance))
				{
					doResurrect(((L2Character)obj), admin);
					admin.sendMessage("Воскрешение завершено");
				}
				else
					admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
		}
		else if (command.startsWith("gmchat"))
		{
			try
			{
				String text = "";
				for (int x=1;x<params.length;x++)
					text += (" " + params[x]);
	
				UseGmChat(text, admin);
			}
			catch (Exception e)
			{
			}
			methods.showSubMenuPage(admin, "gmmenu.htm");
			return;
		}
		else if (command.startsWith("snoop"))
		{
			if (params.length > 1)
				snoop(params[1], admin);
			else
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		else if (command.startsWith("unsnoop"))
		{
			if (params.length > 1)
				unSnoop(params[1], admin);
			else
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
	}
	
	private void doResurrect(L2Character targetChar, L2PcInstance activeChar)
	{
		if (!targetChar.isDead())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (targetChar != null && targetChar instanceof L2PcInstance)
			((L2PcInstance) targetChar).restoreExp(100.0);
		else
			DecayTaskManager.getInstance().cancelDecayTask(targetChar);

		targetChar.doRevive();
	}

	private void createItem(L2PcInstance admin, int id, int num)
	{
		L2Item template = ItemTable.getInstance().getTemplate(id);
		if (template == null)
		{
			admin.sendMessage("Указанный итем не существует");
			return;
		}
		if (num > 20)
		{
			if (!template.isStackable())
			{
				admin.sendMessage("Указанный итем не стэкуется. Создание в таком количестве не возможно");
				return;
			}
		}
		admin.sendMessage("Создано " + num + " " + template.getName() + " (" + id + ") в Ваш инвентарь");
		admin.getInventory().addItem("admin_create", id, num, admin, admin);
		admin.sendPacket(new ItemList(admin, false));
	}

	private void removeAllItems(L2PcInstance admin)
	{
		for (L2ItemInstance item : admin.getInventory().getItems())
		{
			if (item.getLocation() == L2ItemInstance.ItemLocation.INVENTORY)
				admin.getInventory().destroyItem("Destroy", item.getObjectId(), item.getCount(), admin, null);
		}
		admin.sendPacket(new ItemList(admin, false));
	}

	public void adminSummon(L2PcInstance admin, int npcId)
	{
		if (admin.getPet() != null)
		{
			admin.sendPacket(SystemMessageId.S2_S1);
			admin.getPet().unSummon(admin);
		}

		L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(npcId);
		L2SummonInstance summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, admin, null);

		summon.setTitle(admin.getName());
		summon.setExpPenalty(0);
		if (summon.getLevel() >= Experience.LEVEL.length)
			summon.getStat().setExp(Experience.LEVEL[Experience.LEVEL.length - 1]);
		else
			summon.getStat().setExp(Experience.LEVEL[(summon.getLevel() % Experience.LEVEL.length)]);

		summon.getStat().setExp(0);
		summon.getStatus().setCurrentHp(summon.getMaxHp());
		summon.getStatus().setCurrentMp(summon.getMaxMp());
		summon.setHeading(admin.getHeading());
		summon.setRunning();
		admin.setPet(summon);
		L2World.getInstance().storeObject(summon);
		summon.spawnMe(admin.getX() + 50, admin.getY() + 100, admin.getZ());
		summon.setFollowStatus(true);
		summon.setShowSummonAnimation(false);
		admin.sendPacket(new PetInfo(summon, 0));
	}
	
	private void UseGmChat(String text, L2PcInstance activeChar)
	{
		try
		{
			CreatureSay cs = new CreatureSay(activeChar.getObjectId(), SystemChatChannelId.Chat_Alliance, activeChar.getName(), text);
			GmListTable.broadcastToGMs(cs);
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}

	private void snoop(String name, L2PcInstance admin)
	{
		if (name == null)
			return;

		L2PcInstance player = L2World.getInstance().getPlayer(name);
		if (player == null)
		{
			admin.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			return;
		}
		if (player == admin)
		{
			admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (player.isGM())
		{
			admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
			
		player.addSnooper(admin);
		admin.addSnooped(player);
	}

	private void unSnoop(String command, L2PcInstance admin)
	{
		L2Object target = admin.getTarget();
		if (target == null || !(target instanceof L2PcInstance))
		{
			admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		player.removeSnooper(admin);
		admin.removeSnooped(player);
		admin.sendMessage("Snoop режим остановлен");
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}