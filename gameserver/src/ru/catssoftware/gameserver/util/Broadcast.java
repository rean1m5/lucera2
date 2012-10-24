package ru.catssoftware.gameserver.util;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;

public final class Broadcast
{
	private Broadcast()
	{
	}

	public static void toKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		for (L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if (player == null || player.isOfflineTrade())
				continue;

			player.sendPacket(mov);
		}
	}

	public static void toAllPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius) {
		if (radius < 0)
			radius = 1500;

		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			if(player.isInsideRadius(character, radius, false, true))
				player.sendPacket(mov);
				
		
	}
	public static void toKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius)
	{
		if (radius < 0)
			radius = 1500;

		for (L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if (player == null)
				continue;

			if (character.isInsideRadius(player, radius, false, false))
				player.sendPacket(mov);
		}
	}

	public static void toSelfAndKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		if (character.isPlayer())
			character.sendPacket(mov);

		toKnownPlayers(character, mov);
	}

	public static void toSelfAndKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, long radius)
	{
		if (radius < 0)
			radius = 360000;

		if (character instanceof L2PcInstance)
			character.sendPacket(mov);

		for (L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if (player != null && character.getDistanceSq(player) <= radius)
				player.sendPacket(mov);
		}
	}

	public static void toAllOnlinePlayers(L2GameServerPacket mov)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player == null)
				continue;

			player.sendPacket(mov);
		}
	}
	
	public static void announceToOnlinePlayers(String text)
	{
		CreatureSay cs = new CreatureSay(0, SystemChatChannelId.Chat_Announce, "", text);
		toAllOnlinePlayers(cs);
	}
}