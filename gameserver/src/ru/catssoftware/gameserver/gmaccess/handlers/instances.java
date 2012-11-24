package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class instances extends gmHandler
{
	private static final String[] commands =
	{
			"setinstance",
			"createinstance",
			"destroyinstance",
			"listinstances",
			"zonelist"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		if (command.equals("zonelist")) {
			for(L2Zone z : admin.getZones()) {
				admin.sendMessage("Zone "+z.getName()+": "+z.getClass().getSimpleName());
			}
		}
		else if (command.startsWith("createinstance"))
		{
			if (params.length < 2)
				admin.sendMessage("Формат: //createinstance <id> <templatefile>");
			else
			{
				try
				{
					int id = Integer.parseInt(params[1]);
					if (InstanceManager.getInstance().createInstanceFromTemplate(id, params[2]) && id < 300000)
					{
						admin.sendMessage("Инстанс создан");
						return;
					}

					admin.sendMessage("Невозможно создать инстанс");
					return;
				}
				catch (Exception e)
				{
					admin.sendMessage("Ошибка загрузки: " + params[2]);
					return;
				}
			}
		}
		else if (command.startsWith("listinstances"))
		{
			for (Instance temp : InstanceManager.getInstance().getInstances().values())
				admin.sendMessage("Id: " + temp.getId() + " Name: " + temp.getName());
		}
		else if (command.startsWith("setinstance"))
		{
			try
			{
				int val = Integer.parseInt(params[1]);
				if (InstanceManager.getInstance().getInstance(val) == null)
				{
					admin.sendMessage("Инстанс " + val + " не существует");
					return;
				}

				L2Object target = admin.getTarget();
				if (target == null || target instanceof L2Summon)
				{
					admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				target.setInstanceId(val);
				if (target.isPlayer())
				{
					L2PcInstance player = (L2PcInstance) target;
					player.sendMessage("GM отправил Вас в инстанс:" + val);
					InstanceManager.getInstance().getInstance(val).addPlayer(player.getObjectId());
					player.teleToLocation(player.getX(), player.getY(), player.getZ());
					L2Summon pet = player.getPet();
					if (pet != null)
					{
						pet.setInstanceId(val);
						pet.teleToLocation(pet.getX(), pet.getY(), pet.getZ());
						player.sendMessage("GM отправил Вашего питомца " + pet.getName() + " в инстанс:" + val);
					}
				}
				admin.sendMessage("Игрок " + target.getName() + " отправлен в инстанс " + target.getInstanceId());
				return;
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //setinstance id");
			}
		}
		else if (command.startsWith("destroyinstance"))
		{
			try
			{
				int val = Integer.parseInt(params[1]);
				InstanceManager.getInstance().destroyInstance(val);
				admin.sendMessage("Инстанс удален");
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //destroyinstance id");
			}
		}
		return;
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}