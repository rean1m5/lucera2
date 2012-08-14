package ru.catssoftware.gameserver.model.quest.pack.ai;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.InventoryUpdate;

public class VarkaKetraAlly extends L2AttackableAIScript
{
	public VarkaKetraAlly()
	{
		super(-1, "Varka Ketra Ally", "ai");
		int[] mobs =
		{
			// ketra mobs
			21324, 21325, 21327, 21328, 21329, 21331, 21332,
			21334, 21335, 21336, 21338, 21339, 21340, 21342,
			21343, 21344, 21345, 21346, 21347, 21348, 21349,
			// varka mobs
			21350, 21351, 21353, 21354, 21355, 21357, 21358,
			21360, 21361, 21362, 21364, 21365, 21366, 21368,
			21369, 21370, 21371, 21372, 21373, 21374, 21375
		};

		this.registerMobs(mobs);
	}

	protected static final int[] ketraAllyMark = { 7211, 7212, 7213, 7214, 7215 };

	protected static final int[] varkaAllyMark = { 7225, 7224, 7223, 7222, 7221 };

	public String onAttack (L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if ((attacker.getAllianceWithVarkaKetra() != 0) && (attacker.isAlliedWithKetra() && npc.getFactionId() == "ketra") || (attacker.isAlliedWithVarka() && npc.getFactionId() == "varka"))
		{
			L2Skill skill = SkillTable.getInstance().getInfo(4578, 1);
			if (skill != null)
			{
				npc.setTarget(attacker);
				npc.doCast(skill);
			}
		}

		return super.onAttack (npc, attacker, damage, isPet);
	}

	public String onKill (L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (killer.getParty() != null)
		{
			for (L2PcInstance member : killer.getParty().getPartyMembers())
				if(member.isInsideRadius(killer, 700, false, false))
					decreaseAlly(npc, member);
		}
		else
			decreaseAlly(npc, killer);

		return super.onKill(npc, killer, isPet);
	}

	private void decreaseAlly (L2NpcInstance npc, L2PcInstance player)
	{
		if (player.getAllianceWithVarkaKetra() != 0)
		{
			L2ItemInstance mark = null;

			if (player.isAlliedWithKetra() && npc.getFactionId() == "ketra")
			{
				L2ItemInstance varkasBadgeSoldier = player.getInventory().getItemByItemId(7216);
				L2ItemInstance varkasBadgeOfficer = player.getInventory().getItemByItemId(7217);
				L2ItemInstance varkasBadgeCaptain = player.getInventory().getItemByItemId(7218);
				L2ItemInstance valorTotem = player.getInventory().getItemByItemId(7219);
				L2ItemInstance wisdomTotem = player.getInventory().getItemByItemId(7220);

				int varkasBadgeSoldierCount = varkasBadgeSoldier == null ? 0 : varkasBadgeSoldier.getCount();
				int varkasBadgeOfficerCount = varkasBadgeOfficer == null ? 0 : varkasBadgeOfficer.getCount();
				int varkasBadgeCaptainCount = varkasBadgeCaptain == null ? 0 : varkasBadgeCaptain.getCount();
				int valorTotemCount = valorTotem == null ? 0 : valorTotem.getCount();
				int wisdomTotemCount = wisdomTotem == null ? 0 : wisdomTotem.getCount();

				if (varkasBadgeSoldierCount > 0)
					player.destroyItemByItemId("ketrasBadgeSoldier", 7216, varkasBadgeSoldierCount, player, true);
				if (varkasBadgeOfficerCount > 0)
					player.destroyItemByItemId("ketrasBadgeOfficer", 7217, varkasBadgeOfficerCount, player, true);
				if (varkasBadgeCaptainCount > 0)
					player.destroyItemByItemId("ketrasBadgeCaptain", 7218, varkasBadgeCaptainCount, player, true);
				if (valorTotemCount > 0)
					player.destroyItemByItemId("valorTotem", 7219, valorTotemCount, player, true);
				if (wisdomTotemCount > 0)
					player.destroyItemByItemId("wisdomTotem", 7220, wisdomTotemCount, player, true);

				player.destroyItemByItemId("Mark", ketraAllyMark[player.getAllianceWithVarkaKetra() - 1], 1, player, true);
				player.setAllianceWithVarkaKetra(player.getAllianceWithVarkaKetra() - 1);
				if (player.getAllianceWithVarkaKetra() != 0)
					mark = player.getInventory().addItem("Mark", ketraAllyMark[player.getAllianceWithVarkaKetra() - 1], 1, player, player);
			}

			if (player.isAlliedWithVarka() && npc.getFactionId() == "varka")
			{
				L2ItemInstance ketrasBadgeSoldier = player.getInventory().getItemByItemId(7226);
				L2ItemInstance ketrasBadgeOfficer = player.getInventory().getItemByItemId(7227);
				L2ItemInstance ketrasBadgeCaptain = player.getInventory().getItemByItemId(7228);
				L2ItemInstance featherValor = player.getInventory().getItemByItemId(7229);
				L2ItemInstance featherWisdom = player.getInventory().getItemByItemId(7230);

				int ketrasBadgeSoldierCount = ketrasBadgeSoldier == null ? 0 : ketrasBadgeSoldier.getCount();
				int ketrasBadgeOfficerCount = ketrasBadgeOfficer == null ? 0 : ketrasBadgeOfficer.getCount();
				int ketrasBadgeCaptainCount = ketrasBadgeCaptain == null ? 0 : ketrasBadgeCaptain.getCount();
				int featherValorCount = featherValor == null ? 0 : featherValor.getCount();
				int featherWisdomCount = featherWisdom == null ? 0 : featherWisdom.getCount();

				if (ketrasBadgeSoldierCount > 0)
					player.destroyItemByItemId("ketrasBadgeSoldier", 7226, ketrasBadgeSoldierCount, player, true);
				if (ketrasBadgeOfficerCount > 0)
					player.destroyItemByItemId("ketrasBadgeOfficer", 7227, ketrasBadgeOfficerCount, player, true);
				if (ketrasBadgeCaptainCount > 0)
					player.destroyItemByItemId("ketrasBadgeCaptain", 7228, ketrasBadgeCaptainCount, player, true);
				if (featherValorCount > 0)
					player.destroyItemByItemId("featherValor", 7229, featherValorCount, player, true);
				if (featherWisdomCount > 0)
					player.destroyItemByItemId("featherWisdom", 7230, featherWisdomCount, player, true);

				player.destroyItemByItemId("Mark", varkaAllyMark[player.getAllianceWithVarkaKetra() + 5], 1, player, true);
				player.setAllianceWithVarkaKetra(player.getAllianceWithVarkaKetra() + 1);
				if (player.getAllianceWithVarkaKetra() != 0)
					mark = player.getInventory().addItem("Mark", varkaAllyMark[player.getAllianceWithVarkaKetra() + 5], 1, player, player);
			}

			InventoryUpdate u = new InventoryUpdate();
			u.addNewItem(mark);
			player.sendPacket(u);
		}
	}
}