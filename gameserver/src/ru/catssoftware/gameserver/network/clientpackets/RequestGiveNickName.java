package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;


public class RequestGiveNickName extends L2GameClientPacket
{
	private static final String	_C__55_REQUESTGIVENICKNAME	= "[C] 55 RequestGiveNickName";
	private String				_target, _title;

	@Override
	protected void readImpl()
	{
		_target = readS();
		_title = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		//Can the player change/give a title?
		if ((activeChar.isNoble() || activeChar.isGM()) && activeChar.getTarget() == activeChar)
		{
			if (!Config.TITLE_PATTERN.matcher(_title).matches())
			{
				activeChar.sendMessage("Неверный титул, попробуйте заново.");
			}
			else
			{
				activeChar.setTitle(_title);
				SystemMessage sm = new SystemMessage(SystemMessageId.TITLE_CHANGED);
				activeChar.sendPacket(sm);
				activeChar.broadcastTitleInfo();
				sm = null;
			}
		}
		else if (activeChar.getClan() != null && (activeChar.getClanPrivileges() & L2Clan.CP_CL_GIVE_TITLE) == L2Clan.CP_CL_GIVE_TITLE)
		{
			if (activeChar.getClan().getLevel() < 3)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE);
				activeChar.sendPacket(sm);
				sm = null;
				return;
			}

			L2ClanMember member1 = activeChar.getClan().getClanMember(_target);
			if (member1 != null)
			{
				L2PcInstance member = member1.getPlayerInstance();
				//is target from the same clan?
				if (member != null)
				{
					if (!Config.TITLE_PATTERN.matcher(_title).matches())
					{
						activeChar.sendMessage("Неверный титул, попробуйте заново.");
					}
					else
					{
						member.setTitle(_title);
						SystemMessage sm = new SystemMessage(SystemMessageId.TITLE_CHANGED);
						member.sendPacket(sm);
						sm = null;
						member.broadcastTitleInfo();

						if (member != activeChar)
						{
							sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_TITLE_CHANGED_TO_S2);
							sm.addString(member.getName());
							sm.addString(member.getTitle());
							member.sendPacket(sm);
							sm = null;
						}
					}
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
					activeChar.sendPacket(sm);
					sm = null;
				}
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
				activeChar.sendPacket(sm);
				sm = null;
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__55_REQUESTGIVENICKNAME;
	}
}
