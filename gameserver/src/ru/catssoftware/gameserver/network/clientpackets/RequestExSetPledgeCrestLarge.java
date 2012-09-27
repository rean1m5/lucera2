package ru.catssoftware.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.cache.CrestCache;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;


public class RequestExSetPledgeCrestLarge extends L2GameClientPacket
{
	private static final String	_C__D0_11_REQUESTEXSETPLEDGECRESTLARGE	= "[C] D0:11 RequestExSetPledgeCrestLarge";
	private int					_size;
	private byte[]				_data;

	@Override
	protected void readImpl()
	{
		_size = readD();
		if (_size > 2176)
			return;
		if (_size > 0)
		{
			_data = new byte[_size];
			readB(_data);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Clan clan = activeChar.getClan();
		if (clan == null)
			return;

		if (_data == null)
		{
			CrestCache.getInstance().removePledgeCrestLarge(clan.getCrestId());

			clan.setHasCrestLarge(false);
			activeChar.sendMessage("Успешно удалено.");

			for (L2PcInstance member : clan.getOnlineMembers(0))
				member.broadcastUserInfo(true);

			return;
		}

		if (_size > 2176)
		{
			activeChar.sendMessage("Файл должен быть не выше 2176 байт.");
			return;
		}

		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if (clan.getHasCastle() == 0 && clan.getHasHideout() == 0)
			{
				activeChar.sendMessage("Только кланы имеющие клан-холл могут поставить знак");
				return;
			}

			CrestCache crestCache = CrestCache.getInstance();

			int newId = IdFactory.getInstance().getNextId();

			if (!crestCache.savePledgeCrestLarge(newId, _data))
			{
				_log.info("Error loading large crest of clan:" + clan.getName());
				return;
			}

			if (clan.hasCrestLarge())
			{
				crestCache.removePledgeCrestLarge(clan.getCrestLargeId());
			}

			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
				statement.setInt(1, newId);
				statement.setInt(2, clan.getClanId());
				statement.executeUpdate();
				statement.close();
			}
			catch (SQLException e)
			{
				_log.warn("could not update the large crest id:" + e.getMessage());
			}
			finally
			{
				try
				{
					if (con != null)
						con.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}

			clan.setCrestLargeId(newId);
			clan.setHasCrestLarge(true);

			activeChar.sendPacket(SystemMessageId.CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED);

			for (L2PcInstance member : clan.getOnlineMembers(0))
				member.broadcastUserInfo(true);

		}
	}

	@Override
	public String getType()
	{
		return _C__D0_11_REQUESTEXSETPLEDGECRESTLARGE;
	}
}