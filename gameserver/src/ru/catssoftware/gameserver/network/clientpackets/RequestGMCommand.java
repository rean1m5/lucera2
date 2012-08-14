package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.GMHennaInfo;
import ru.catssoftware.gameserver.network.serverpackets.GMViewItemList;
import ru.catssoftware.gameserver.network.serverpackets.GMViewPledgeInfo;
import ru.catssoftware.gameserver.network.serverpackets.GMViewQuestInfo;
import ru.catssoftware.gameserver.network.serverpackets.GMViewSkillInfo;
import ru.catssoftware.gameserver.network.serverpackets.GMViewWarehouseWithdrawList;
import ru.catssoftware.gameserver.network.serverpackets.GMViewCharacterInfo;

public class RequestGMCommand extends L2GameClientPacket
{
	private String				_targetName;
	private int					_command;

	@Override
	protected void readImpl()
	{
		_targetName = readS();
		_command = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = L2World.getInstance().getPlayer(_targetName);
		L2PcInstance activeChar = getClient().getActiveChar();

		if (player  == null || activeChar == null || !activeChar.allowAltG())
			return;

		switch (_command)
		{
			case 1: // player status
			{
				sendPacket(new GMViewCharacterInfo(player));
				break;
			}
			case 2: // player clan
			{
				if (player.getClan() != null)
					sendPacket(new GMViewPledgeInfo(player.getClan(), player));
				else
					activeChar.sendMessage(player.getName() + " не в клане.");
				break;
			}
			case 3: // player skills
			{
				sendPacket(new GMViewSkillInfo(player));
				break;
			}
			case 4: // player quests
			{
				sendPacket(new GMViewQuestInfo(player));
				break;
			}
			case 5: // player inventory
			{
				sendPacket(new GMViewItemList(player));
				sendPacket(new GMHennaInfo(player));
				break;
			}
			case 6: // player warehouse
			{
				sendPacket(new GMViewWarehouseWithdrawList(player));
				break;
			}
		}
	}

	@Override
	public String getType()
	{
		return "[C] 6e RequestGMCommand";
	}
}