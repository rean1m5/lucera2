package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.datatables.TeleportLocationTable;
import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.gmaccess.gmCache;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.instancemanager.ZoneManager;
import ru.catssoftware.gameserver.model.L2Multisell;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;


public class reload extends gmHandler
{
	private static final String[] commands =
	{
		"reload",
		"reload_menu",
		"config_reload",
		"config"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;
		String command = params[0];
/*		if(L2World.getInstance().getAllPlayers().size()>5) {
			NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setHtml("<html><body><center>ПЕРЕЗАГРУЗКА СЕРВЕРА - это отладочная функция<br>ЗАПРЕЩНО делать перезагрузку при онлайне больше 5!</center></body></html>");
			admin.sendPacket(msg);
			return;
		}
*/
		if (command.equals("config"))
		{
			sendConfigReloadPage(admin);
			return;
		}
		else if (command.equals("config_reload"))
		{
			String type = "";
			if (params.length > 1)
				type = params[1];
			else
			{
				sendConfigReloadPage(admin);
				return;
			}

			try
			{
				if (type.equals("rates"))
				{
					Config.loadRatesConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Rates Configs Reloaded.");
					return;
				}
				else if (type.equals("enchant"))
				{
					Config.loadEnchantConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Enchant Configs Reloaded.");
					return;
				}
				else if (type.equals("geodata"))
				{
					Config.loadGeoConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Geodata config reloaded");
					return;
				}
				else if (type.equals("events"))
				{
					Config.loadFunEventsConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Events config reloaded");
					
				}
				else if (type.equals("pvp"))
				{
					Config.loadPvpConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("PvP Config Configs Reloaded");
					return;
				}
				else if (type.equals("options"))
				{
					Config.loadOptionsConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Options Configs Reloaded");
					return;
				}
				else if (type.equals("other"))
				{
					Config.loadOtherConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Other Config Configs Reloaded.");
					return;
				}
				else if (type.equals("alt"))
				{
					Config.loadAltConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Alternative Configs Reloaded");
					return;
				}
				else if (type.equals("clans"))
				{
					Config.loadClansConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Clans Configs Reloaded");
					return;
				}
				else if (type.equals("champions"))
				{
					Config.loadChampionsConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Champions Configs Reloaded");
					return;
				}
				else if (type.equals("lottery"))
				{
					Config.loadLotteryConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Lottery Configs Reloaded");
					return;
				}
				else if (type.equals("areas"))
				{
					Config.loadAreasConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Four Sepulchers Configs Reloaded");
					return;
				}
				else if (type.equals("entities"))
				{
					Config.loadEntitiesConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("all entitites Configs Reloaded.");
					return;
				}
				else if (type.equals("sevensigns"))
				{
					Config.loadSevenSignsConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Seven Signs Configs Reloaded.");
					return;
				}
				else if (type.equals("gmaccess"))
				{
					Config.loadGmAccess();
					sendConfigReloadPage(admin);
					admin.sendMessage("GMAccess Configs Reloaded.");
					return;
				}
				else if (type.equals("chatfilter"))
				{
					Config.unallocateFilterBuffer();
					Config.loadChatConfig();
					Config.loadFilter();
					sendConfigReloadPage(admin);
					admin.sendMessage("Chat Filter Reloaded.");
					return;
				}
				else if (type.equals("classmaster"))
				{
					Config.loadClassMasterConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Classmaster Configs Reloaded.");
					return;
				}
				else if (type.equals("all"))
				{
					Config.unallocateFilterBuffer();
					Config.loadAll();
					sendConfigReloadPage(admin);
					admin.sendMessage("All Configs Reloaded.");
					return;
				}
				else if (type.equals("fortsiege"))
				{
					Config.loadFortSiegeConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("FortSiege config reloaded");
					return;
				}
				else if (type.equals("siege"))
				{
					Config.loadSiegeConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Siege config reloaded");
					return;
				}
				else if (type.equals("wedding"))
				{
					Config.loadWeddingConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Wedding config reloaded");
					return;
				}
				else if (type.equals("service"))
				{
					Config.loadServicesConfig();
					sendConfigReloadPage(admin);
					admin.sendMessage("Service config reloaded");
					return;
				}
				else
				{
					Config.reload(type);
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //reload <type>");
				return;
			}
			return;
		}
		else if (command.equals("reload_menu"))
			sendReloadPage(admin);
		else if (command.startsWith("reload"))
		{
			String type = "";
			if (params.length > 1)
				type = params[1];
			else
			{
				sendReloadPage(admin);
				return;
			}

			try
			{
				if (type.equals("multisell"))
				{
					L2Multisell.getInstance().reload();
					sendReloadPage(admin);
					admin.sendMessage("Multisell Reload Complete.");
					return;
				}
				else if (type.startsWith("door"))
				{
					DoorTable.getInstance().reloadAll();
					sendReloadPage(admin);
					admin.sendMessage("Doors reloaded");
					return;
				}
				else if (type.startsWith("teleport"))
				{
					TeleportLocationTable.getInstance().reloadAll();
					sendReloadPage(admin);
					admin.sendMessage("All Teleport Tables Reloaded.");
					return;
				}
				else if (type.startsWith("skill"))
				{
					SkillTable.reload();
					sendReloadPage(admin);
					admin.sendMessage("Skills reload Complete.");
					return;
				}
				else if (type.equals("npcs"))
				{
					NpcTable.getInstance().cleanUp();
					NpcTable.getInstance().reloadAll();
					sendReloadPage(admin);
					admin.sendMessage("NPCs Reload Complete.");
					return;
				}
				else if (type.startsWith("html"))
				{
					HtmCache.getInstance().load();
					for(Quest q : QuestManager.getInstance().getAllManagedScripts()) 
						q.loadHTML();
					sendReloadPage(admin);
					admin.sendMessage(HtmCache.getInstance().toString());
					return;
				}
				else if (type.startsWith("item"))
				{
					ItemTable.reload();
					sendReloadPage(admin);
					admin.sendMessage("Item Templates Reload Complete.");
					return;
				}
				else if (type.startsWith("tradelist"))
				{
					TradeListTable.getInstance().reloadAll();
					sendReloadPage(admin);
					admin.sendMessage("Buylists Reload Complete.");
					return;
				}
				else if (type.startsWith("zone"))
				{
					ZoneManager.getInstance().reload();
					sendReloadPage(admin);
					admin.sendMessage("Zones Reload Complete.");
					return;
				}
				else if (type.equals("spawnlist"))
				{
					SpawnTable.getInstance().reloadAll();
					sendReloadPage(admin);
					admin.sendMessage("Spawns Reload Complete.");
					return;
				}
				else if (type.startsWith("gmcache"))
				{
					gmCache.getInstance().loadAccess(true);
					sendReloadPage(admin);
					admin.sendMessage("GmCache Reload Complete.");
					return;
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте:  //reload <type>");
				return;
			}
		}
	}

	private void sendReloadPage(L2PcInstance activeChar)
	{
		methods.showSubMenuPage(activeChar, "reload_menu.htm");
	}

	private void sendConfigReloadPage(L2PcInstance activeChar)
	{
		methods.showMenuPage(activeChar, "config.htm");
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}