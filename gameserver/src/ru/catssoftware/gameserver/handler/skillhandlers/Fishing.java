package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.geodata.GeoData;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.instancemanager.ZoneManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;

public class Fishing implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.FISHING };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		if (!Config.ALLOW_FISHING)
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_FISHING_IS_NOT_ALLOWED));
			return;
		}
		if (player.isFishing())
		{
			if (player.getFishCombat() != null)
				player.getFishCombat().doDie(false);
			else
				player.endFishing(false);
			//Cancels fishing
			player.sendPacket(SystemMessageId.FISHING_ATTEMPT_CANCELLED);
			return;
		}
		if (player.isInBoat())
		{
			//You can't fish while you are on boat
			player.sendPacket(SystemMessageId.CANNOT_FISH_ON_BOAT);
			return;
		}
		if (!player.isInsideZone(L2Zone.FLAG_FISHING) || player.isInsideZone(L2Zone.FLAG_PEACE))
		{
			//You can't fish here
			player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
			return;
		}
		if (player.isInsideZone(L2Zone.FLAG_WATER))
		{
			//You can't fish in water
			player.sendPacket(SystemMessageId.CANNOT_FISH_UNDER_WATER);
			return;
		}

		// calculate point of a float
		int d = Rnd.get(50) + 150;
		double angle = Util.convertHeadingToDegree(player.getHeading());
		double radian = Math.toRadians(angle);

		int dx = (int) (d * Math.cos(radian));
		int dy = (int) (d * Math.sin(radian));

		int x = activeChar.getX() + dx;
		int y = activeChar.getY() + dy;

		L2Zone water = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.Water, x, y);
		// float must be in water
		if (water == null)
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
			return;
		}
		boolean isHotSpringZone = false;
		if (water.getName().equalsIgnoreCase("24_14_water"))
			isHotSpringZone = true;

		int z = water.getMaxZ(x, y, activeChar.getZ());

		if (Config.GEODATA && !GeoData.getInstance().canSeeTarget(activeChar.getX(), activeChar.getY(), activeChar.getZ(), x, y, z)
				|| (!Config.GEODATA && (Util.calculateDistance(activeChar.getX(), activeChar.getY(), activeChar.getZ(), x, y, z, true) > d * 1.73)))
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
			return;
		}
		if (player.isInCraftMode() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_WHILE_USING_RECIPE_BOOK);
			return;
		}
		L2Weapon weaponItem = player.getActiveWeaponItem();
		if ((weaponItem == null || weaponItem.getItemType() != L2WeaponType.ROD))
		{
			//Fishing poles are not installed
			player.sendPacket(SystemMessageId.FISHING_POLE_NOT_EQUIPPED);
			return;
		}
		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (lure == null)
		{
			//Bait not equiped.
			player.sendPacket(SystemMessageId.BAIT_ON_HOOK_BEFORE_FISHING);
			return;
		}
		player.setLure(lure);
		L2ItemInstance lure2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		if (lure2 == null || lure2.getCount() < 1) //Not enough bait.
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_BAIT);
			player.sendPacket(new ItemList(player, false));
		}
		else
		//Has enough bait, consume 1 and update inventory. Start fishing follows.
		{
			lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(lure2);
			player.sendPacket(iu);
		}

		// client itself find z coord of a float
		player.startFishing(x, y, z + 10, isHotSpringZone);
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}