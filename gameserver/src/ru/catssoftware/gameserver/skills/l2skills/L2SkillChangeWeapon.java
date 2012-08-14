package ru.catssoftware.gameserver.skills.l2skills;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.util.StatsSet;

/**
 * @author nBd
 */
public class L2SkillChangeWeapon extends L2Skill
{
	public L2SkillChangeWeapon(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character caster, L2Character... targets)
	{
		if (caster.isAlikeDead())
			return;

		if (!(caster instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) caster;

		if (player.isEnchanting())
			return;

		L2Weapon weaponItem = player.getActiveWeaponItem();
		if (weaponItem == null)
			return;

		L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
			wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);

		if (wpn != null)
		{
			if (wpn.isWear())
				return;

			if (wpn.isAugmented())
				return;

			int newItemId = 0;
			int enchantLevel = 0;

			if (weaponItem.getChangeWeaponId() != 0)
			{
				newItemId = weaponItem.getChangeWeaponId();
				enchantLevel = wpn.getEnchantLevel();

				if (newItemId == -1)
					return;

				L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance item: unequiped)
					iu.addModifiedItem(item);

				player.sendPacket(iu);

				if (unequiped.length > 0)
				{
					byte count = 0;

					for (L2ItemInstance item: unequiped)
					{
						if (!(item.getItem() instanceof L2Weapon))
						{
							count++;
							continue;
						}

						SystemMessage sm = null;
						if (item.getEnchantLevel() > 0)
						{
							sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
							sm.addNumber(item.getEnchantLevel());
							sm.addItemName(item);
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.S1_DISARMED);
							sm.addItemName(item);
						}
						player.sendPacket(sm);
					}

					if (count == unequiped.length)
						return;
				}
				else
				{
					return;
				}

				L2ItemInstance destroyItem = player.getInventory().destroyItem("ChangeWeapon", wpn, player, null);

				if (destroyItem == null)
					return;

				L2ItemInstance newItem = player.getInventory().addItem("ChangeWeapon", newItemId, 1, player, destroyItem);

				if (newItem == null)
					return;

				newItem.setEnchantLevel(enchantLevel);
				player.getInventory().equipItem(newItem);

				SystemMessage msg = null;

				if (newItem.getEnchantLevel() > 0)
				{
					msg = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
					msg.addNumber(newItem.getEnchantLevel());
					msg.addItemName(newItem);
				}
				else
				{
					msg = new SystemMessage(SystemMessageId.S1_EQUIPPED);
					msg.addItemName(newItem);
				}
				player.sendPacket(msg);

				InventoryUpdate u = new InventoryUpdate();
				u.addRemovedItem(destroyItem);
				u.addNewItem(newItem);
				player.sendPacket(u);

				player.broadcastUserInfo();
			}
		}
	}
}