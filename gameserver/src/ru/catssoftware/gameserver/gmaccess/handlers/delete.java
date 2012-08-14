package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.RaidBossSpawnManager;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;

public class delete extends gmHandler
{
	private static final String[] commands =
	{
		"delete",
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String command = params[0];

		if (command.equals("delete"))
		{
			handleDelete(admin);
			return;
		}
	}

	private void handleDelete(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if ((obj != null) && (obj instanceof L2NpcInstance))
		{
			L2NpcInstance target = (L2NpcInstance) obj;
			target.deleteMe();

			L2Spawn spawn = target.getSpawn();
			if (spawn != null)
			{
				spawn.stopRespawn();

				if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
					RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
				else
					SpawnTable.getInstance().deleteSpawn(spawn, true);
			}
			activeChar.sendMessage(target.getName() + " удален");
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}