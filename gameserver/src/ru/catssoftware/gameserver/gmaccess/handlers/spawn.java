package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.DayNightSpawnManager;
import ru.catssoftware.gameserver.instancemanager.RaidBossSpawnManager;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.actor.instance.L2MonsterInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;

public class spawn extends gmHandler
{
	private static final String[] commands =
	{
		"spawn_menu",
		"spawn",
		"cspawn",
		"otspawn",
		"spawn_once",
		"spawnday",
		"spawnnight",
		"returntospawn",
		"loc"
		
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		String cmd = params[0];

		if (cmd.equals("spawn_menu"))
		{
			methods.showSubMenuPage(admin, "spawn_menu.htm");
			return;
		}
		else if (cmd.equals("spawn") || cmd.equals("cspawn") || cmd.equals("otspawn") || cmd.equals("spawn_once"))
		{
			/* Определение параметров спавна */
			boolean custom = cmd.equals("cspawn");
			boolean respawn = !cmd.equals("spawn_once");
			boolean storeInDb = !cmd.equals("otspawn") && respawn;

			/* Проверка длинны массива */
			if (params.length >= 2)
			{
				/* Временные переменные */
				int npcId = 0, count = 1, radius = 300;
				String name = params[1];
				
				try
				{
					try
					{
						npcId = Integer.parseInt(name);
					}
					catch (NumberFormatException e)
					{
					}
					try
					{
						if (params.length > 2)
						{
							count = Integer.parseInt(params[2]);
							if (params.length > 3)
								radius = Integer.parseInt(params[3]);
						}
					}
					catch (NumberFormatException e)
					{
					}
	
					/* Пытаемся заспавнить NPC */
					if (npcId > 0)
					{
						spawnNpc(admin, npcId, count, radius, storeInDb, respawn, custom);
						methods.showSubMenuPage(admin, "spawn_menu.htm");
					}
					else if (name.length() > 0)
					{
						spawnNpc(admin, name, count, radius, storeInDb, respawn, custom);
						methods.showSubMenuPage(admin, "spawn_menu.htm");
					}
					else
					{
						methods.showSubMenuPage(admin, "spawn_menu.htm");
						admin.sendMessage("Используйте1: //" + cmd + " [id] [count] [radius]");
						return;
					}
				}
				catch (Exception e)
				{
					methods.showSubMenuPage(admin, "spawn_menu.htm");
					admin.sendMessage("Используйте2: //" + cmd + " [id] [count] [radius]");
					return;
				}
			}
			else
			{
				methods.showSubMenuPage(admin, "spawn_menu.htm");
				admin.sendMessage("Используйте3: //" + cmd + " [id] [count] [radius]");
				return;
			}
			return;
		}
		else if (cmd.equals("spawnday"))
		{
			DayNightSpawnManager.getInstance().spawnDayCreatures();
			methods.showSubMenuPage(admin, "spawn_menu.htm");
			admin.sendMessage("Все дневные NPC заспавнены");
			return;
		}
		else if (cmd.equals("returntospawn")) {
			if(admin.getTarget()!=null)  {
				if(admin.getTarget() instanceof L2NpcInstance) {
					L2NpcInstance npc = (L2NpcInstance)admin.getTarget();
					if(npc.getSpawn()!=null) {
						npc.teleToLocation(npc.getSpawn().getLocx(),npc.getSpawn().getLocy(),npc.getSpawn().getLocz());
						if(npc instanceof L2MonsterInstance) {
							L2MonsterInstance monster = (L2MonsterInstance)npc;
							if(monster.hasMinions()) 
								monster.callMinions(true);
							
						}
					}
				}
			}
		}
		else if (cmd.equals("spawnnight"))
		{
			DayNightSpawnManager.getInstance().spawnNightCreatures();
			methods.showSubMenuPage(admin, "spawn_menu.htm");
			admin.sendMessage("Все ночные NPC заспавнены");
			return;
		}
		else if (cmd.equals("loc"))
		{
			String send = "["
					+ admin.getLoc().getX() + ", "
					+ admin.getLoc().getY() + ", "
					+ admin.getLoc().getZ() + ", "
					+ admin.getLoc().getHeading() + "]";
			admin.sendMessage("Location [x,y,z,h]: " + send);
			_log.info("Location [" + admin.getName() + "]: " + send);
		}
	}

	/**
	 * Основной метод спавна NPC
	 * @param activeChar
	 * @param npcId
	 * @param count
	 * @param radius
	 * @param saveInDb
	 * @param respawn
	 * @param custom
	 */
	private void spawnNpc(L2PcInstance activeChar, int npcId, int count, int radius, boolean saveInDb, boolean respawn, boolean custom)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		
		if (template == null)
		{
			activeChar.sendMessage("NpcID " + npcId + " не найден");
			return;
		}

		if(template.getType().equalsIgnoreCase("L2GrandBoss") && !Config.DEVELOPER) {
			activeChar.sendMessage("Нельзя спавнить Гранд Боссов");
			return;
		}
		try
		{
			for (int i = 0; i < count; i++)
			{
				int x = target.getX();
				int y = target.getY();
				int z = target.getZ();
				int heading = activeChar.getHeading();

				if (radius > 0 && count > 1)
				{
					int signX = (Rnd.nextInt(2) == 0) ? -1 : 1;
					int signY = (Rnd.nextInt(2) == 0) ? -1 : 1;
					int randX = Rnd.nextInt(radius);
					int randY = Rnd.nextInt(radius);
					int randH = Rnd.nextInt(0xFFFF);

					x = x + signX * randX;
					y = y + signY * randY;
					heading = randH;
				}

				L2Spawn spawn = new L2Spawn(template);

				if (custom)
					spawn.setCustom();

				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z);
				spawn.setAmount(1);
				spawn.setHeading(heading);
				spawn.setRespawnDelay(Config.STANDARD_RESPAWN_DELAY);
				spawn.setInstanceId(activeChar.getInstanceId());

				if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()) && respawn && !Config.ALT_DEV_NO_SPAWNS && spawn.getInstanceId() == 0)
					activeChar.sendMessage("Вы не можете вызвать " + template.getName() + ". Уже вызван");
				else
				{
					if (saveInDb && !Config.ALT_DEV_NO_SPAWNS && spawn.getInstanceId() == 0)
					{
						if (RaidBossSpawnManager.getInstance().getValidTemplate(spawn.getNpcId()) != null)
						{
							spawn.setRespawnMinDelay(43200);
							spawn.setRespawnMaxDelay(129600);
							RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0, template.getBaseHpMax(), template.getBaseMpMax(), true);
						}
						else
							SpawnTable.getInstance().addNewSpawn(spawn, respawn);
					}
					else
						spawn.spawnOne(true);
					
					spawn.init();
					
					if (!respawn)
						spawn.stopRespawn();

					activeChar.sendMessage("Вызван " + template.getName() + " по координатам " + target.getX() + " " + target.getY() + " " + target.getZ() + ".");
				}
			}
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * Второй метод спавна с ипользованием основного. Спавн по имени
	 * @param activeChar
	 * @param npcName
	 * @param count
	 * @param radius
	 * @param saveInDb
	 * @param respawn
	 * @param custom
	 */
	private void spawnNpc(L2PcInstance activeChar, String npcName, int count, int radius, boolean saveInDb, boolean respawn, boolean custom)
	{
		int npcId = 0;
		
		for (L2NpcTemplate t : NpcTable.getInstance().getAllTemplates().values())
		{
			if (t.getName().equalsIgnoreCase(npcName.replace("_", " ")))
			{
				npcId = t.getNpcId();
				break;
			}
		}

		if (npcId > 0)
			spawnNpc(activeChar, npcId, count, radius, saveInDb, respawn, custom);
		else
			activeChar.sendMessage("NpcID " + npcId + " не найден");
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}