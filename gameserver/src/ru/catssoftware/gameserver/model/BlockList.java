package ru.catssoftware.gameserver.model;

import java.util.Set;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.instancemanager.BlockListManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;


public final class BlockList
{
	private final L2PcInstance			_owner;
	private final Set<String>			_set;
	private boolean						_blockingAll = false;

	public BlockList(L2PcInstance owner)
	{
		_owner = owner;
		_set = BlockListManager.getInstance().getBlockList(_owner.getObjectId());
	}

	public void add(String name)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(name);
		if (player == null)
		{
			_owner.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}
		
		if (player.isGM())
		{
			_owner.sendPacket(SystemMessageId.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_GM);
			return;
		}
		
		if (_set.add(player.getName()))
		{
			_owner.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST).addPcName(player));
			
			player.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addPcName(_owner));
			
			BlockListManager.getInstance().insert(_owner, player);
		}
		else
			_owner.sendMessage(String.format(Message.getMessage(_owner, Message.MessageId.MSG_ALREADY_IGNORED),player.getName()));
	}

	public void remove(String name)
	{
		if (_set.remove(name))
		{
			_owner.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST).addString(name));
			
			BlockListManager.getInstance().remove(_owner, name);
		}
		else
			_owner.sendMessage(String.format(Message.getMessage(_owner, Message.MessageId.MSG_NOT_IN_IGNOR_LIST),name));
	}

	private boolean contains(L2PcInstance player)
	{
		if (player == null || player.isGM())
			return false;
		
		return _blockingAll || _set.contains(player.getName());
	}

	public static boolean isBlocked(L2PcInstance listOwner, L2PcInstance player)
	{
		return listOwner.getBlockList().contains(player);
	}

	public void setBlockingAll(boolean blockingAll)
	{
		_blockingAll = blockingAll;
		
		if (_blockingAll)
			_owner.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
		else
			_owner.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
	}

	public void sendListToOwner()
	{
		_owner.sendPacket(SystemMessageId.BLOCK_LIST_HEADER);
		for (String name : _set)
			_owner.sendMessage(name);
	}
}