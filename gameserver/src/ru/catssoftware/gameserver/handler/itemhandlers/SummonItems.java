package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.xml.SummonItemsData;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Spawn;
import ru.catssoftware.gameserver.model.L2SummonItem;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillLaunched;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.PetItemList;
import ru.catssoftware.gameserver.network.serverpackets.SetupGauge;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Broadcast;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

/**
* @author FBIagent
*/
public class SummonItems implements IItemHandler
{
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;

		if (!FloodProtector.tryPerformAction(activeChar, Protected.ITEMPETSUMMON))
			return;

		if (RainbowSpringSiege.getInstance().isPlayerInArena(activeChar))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return;
		}

		if (activeChar.inObserverMode())
			return;

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		if (activeChar.isAllSkillsDisabled() || activeChar.isCastingNow())
			return;

		L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(item.getItemId());

		if ((activeChar.getPet() != null || activeChar.isMounted()) && sitem.isPetSummon())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
			return;
		}

		if (activeChar.isAttackingNow())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT);
			return;
		}

		if (activeChar.isCursedWeaponEquipped() && sitem.isPetSummon())
		{
			activeChar.sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
			return;
		}

		int npcID = sitem.getNpcId();

		if (npcID == 0)
			return;

		L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);

		if (npcTemplate == null)
			return;

		activeChar.stopMove(null, false);

		switch (sitem.getType())
		{
		case 0: // static summons (like christmas tree)
			try
			{
				L2Spawn spawn = new L2Spawn(npcTemplate);

				spawn.setId(IdFactory.getInstance().getNextId());
				spawn.setLocx(activeChar.getX());
				spawn.setLocy(activeChar.getY());
				spawn.setLocz(activeChar.getZ());
				L2World.getInstance().storeObject(spawn.spawnOne(true));
				activeChar.destroyItem("Summon", item.getObjectId(), 1, null, false);
				activeChar.sendMessage("Created " + npcTemplate.getName() + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
			}
			catch (Exception e)
			{
				activeChar.sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			}

			break;
		case 1: // pet summons
			Broadcast.toSelfAndKnownPlayers(activeChar, new MagicSkillUse(activeChar, activeChar, 2046, 1, 5000, 0, false));
			activeChar.sendPacket(new SetupGauge(0, 5000));
			activeChar.setSummonning(true);
			activeChar.sendPacket(SystemMessageId.SUMMON_A_PET);
			activeChar.setIsCastingNow(true);

			ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFinalizer(activeChar, npcTemplate, item), 5000);
			break;
		case 2: // wyvern
			activeChar.mount(sitem.getNpcId(), item.getObjectId(), true);
			break;
		}
	}

	static class PetSummonFeedWait implements Runnable
	{
		private L2PcInstance	_activeChar;
		private L2PetInstance	_petSummon;

		PetSummonFeedWait(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}

		public void run()
		{
			try
			{
				if (_petSummon.getCurrentFed() <= 0)
					_petSummon.unSummon(_activeChar);
				else
					_petSummon.startFeed();
			}
			catch (Throwable e)
			{
			}
		}
	}

	static class PetSummonFinalizer implements Runnable
	{
		private L2PcInstance	_activeChar;
		private L2ItemInstance	_item;
		private L2NpcTemplate	_npcTemplate;

		PetSummonFinalizer(L2PcInstance activeChar, L2NpcTemplate npcTemplate, L2ItemInstance item)
		{
			_activeChar = activeChar;
			_npcTemplate = npcTemplate;
			_item = item;
		}

		public void run()
		{
			try
			{
				_activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1, false));
				if (!_activeChar.isSummoning())
					return;
				_activeChar.setIsCastingNow(false);
				L2PetInstance petSummon = L2PetInstance.spawnPet(_npcTemplate, _activeChar, _item);

				if (petSummon == null)
					return;

				petSummon.setTitle(_activeChar.getName());

				if (!petSummon.isRespawned())
				{
					petSummon.getStatus().setCurrentHp(petSummon.getMaxHp());
					petSummon.getStatus().setCurrentMp(petSummon.getMaxMp());
					petSummon.getStat().setExp(petSummon.getExpForThisLevel());
					petSummon.setCurrentFed(petSummon.getMaxFed());
				}

				petSummon.setRunning();

				if (!petSummon.isRespawned())
					petSummon.store();

				_activeChar.setPet(petSummon);

				L2World.getInstance().storeObject(petSummon);
				petSummon.spawnMe(_activeChar.getX() + 50, _activeChar.getY() + 100, _activeChar.getZ());
				petSummon.startFeed();
				_item.setEnchantLevel(petSummon.getLevel());

				if (petSummon.getCurrentFed() <= 0)
					ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFeedWait(_activeChar, petSummon), 60000);
				else
					petSummon.startFeed();

				petSummon.setFollowStatus(true);
				petSummon.setShowSummonAnimation(false); // shouldn't be this always true?
				int weaponId = petSummon.getWeapon();
				int armorId = petSummon.getArmor();
				int jewelId = petSummon.getJewel();
				if (weaponId > 0 && petSummon.getOwner().getInventory().getItemByItemId(weaponId)!= null)
				{
					L2ItemInstance item = petSummon.getOwner().getInventory().getItemByItemId(weaponId);
					L2ItemInstance newItem = petSummon.getOwner().transferItem("Transfer", item.getObjectId(), 1, petSummon.getInventory(), petSummon); 
					if (newItem == null)
					{
						_log.warn("Invalid item transfer request: " + petSummon.getName() + "(pet) --> " + petSummon.getOwner().getName());
						petSummon.setWeapon(0);
					}
					else
						petSummon.getInventory().equipItem(newItem);
				}
				else
					petSummon.setWeapon(0);

				if (armorId > 0 && petSummon.getOwner().getInventory().getItemByItemId(armorId)!= null)
				{
					L2ItemInstance item = petSummon.getOwner().getInventory().getItemByItemId(armorId);
					L2ItemInstance newItem = petSummon.getOwner().transferItem("Transfer", item.getObjectId(), 1, petSummon.getInventory(), petSummon); 
					if (newItem == null)
					{
						_log.warn("Invalid item transfer request: " + petSummon.getName() + "(pet) --> " + petSummon.getOwner().getName());
						petSummon.setArmor(0);
					}
					else
						petSummon.getInventory().equipItem(newItem);
				}
				else
					petSummon.setArmor(0);

				if (jewelId > 0 && petSummon.getOwner().getInventory().getItemByItemId(jewelId)!= null)
				{
					L2ItemInstance item = petSummon.getOwner().getInventory().getItemByItemId(jewelId);
					L2ItemInstance newItem = petSummon.getOwner().transferItem("Transfer", item.getObjectId(), 1, petSummon.getInventory(), petSummon); 
					if (newItem == null)
					{
						_log.warn("Invalid item transfer request: " + petSummon.getName() + "(pet) --> " + petSummon.getOwner().getName());
						petSummon.setJewel(0);
					}
					else
						petSummon.getInventory().equipItem(newItem);
				}
				else
					petSummon.setJewel(0);

				petSummon.getOwner().sendPacket(new PetItemList(petSummon));
				petSummon.broadcastStatusUpdate();
			}
			catch (Throwable e)
			{
			}
		}
	}

	public int[] getItemIds()
	{
		return SummonItemsData.getInstance().itemIDs();
	}
}