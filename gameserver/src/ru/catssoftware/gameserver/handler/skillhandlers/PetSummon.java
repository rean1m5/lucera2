package ru.catssoftware.gameserver.handler.skillhandlers;

/*
 * @author Ro0TT
 * @date 25.11.2012
 */

import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.xml.SummonItemsData;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.serverpackets.PetItemList;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class PetSummon implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.PET_SUMMON };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets) 
	{
		L2PcInstance player = activeChar.getPlayer();

		if (player == null)
		{
			Thread.dumpStack();
			return;
		}

		try
		{
			if (!player.isSummoning())
			{
				Thread.dumpStack();
				return;
			}

			L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(player.getSummonItem().getItemId());

			L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(sitem.getNpcId());
			L2PetInstance petSummon = L2PetInstance.spawnPet(npcTemplate, player, player.getSummonItem());


			if (petSummon == null)
				return;

			petSummon.setTitle(player.getName());

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

			player.setPet(petSummon);

			L2World.getInstance().storeObject(petSummon);
			petSummon.spawnMe();
			petSummon.startFeed();
			player.getSummonItem().setEnchantLevel(petSummon.getLevel());

			if (petSummon.getCurrentFed() <= 0)
				ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFeedWait(player, petSummon), 60000);
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
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally {
			player.setSummonning(null);
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

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS; 
	}
}
