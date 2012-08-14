package ru.catssoftware.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.cache.CrestCache;
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;


public class RequestSetAllyCrest extends L2GameClientPacket
{
	private static final String	_C__87_REQUESTSETALLYCREST	= "[C] 87 RequestSetAllyCrest";
	private int					_length;
	private byte[]				_data;

	@Override
	protected void readImpl()
	{
		_length = readD();
		_data = new byte[_length];
		readB(_data);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (_length < 0)
		{
			activeChar.sendMessage("Ошибка передачи файла.");
			return;
		}

		if (_length > 192)
		{
			activeChar.sendMessage("Значок слишком большой. Макс: 192.");
			return;
		}

		if (activeChar.getAllyId() != 0)
		{
			L2Clan leaderclan = ClanTable.getInstance().getClan(activeChar.getAllyId());

			if (activeChar.getClanId() != leaderclan.getClanId() || !activeChar.isClanLeader())
			{
				activeChar.sendPacket(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
				return;
			}

			CrestCache crestCache = CrestCache.getInstance();

			int newId = IdFactory.getInstance().getNextId();

			if (!crestCache.saveAllyCrest(newId, _data))
			{
				_log.info("Error loading crest of ally:" + leaderclan.getAllyName());
				return;
			}
			if (leaderclan.getAllyCrestId() != 0)
			{
				crestCache.removeAllyCrest(leaderclan.getAllyCrestId());
			}

			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?");
				statement.setInt(1, newId);
				statement.setInt(2, leaderclan.getAllyId());
				statement.executeUpdate();
				statement.close();
			}
			catch (SQLException e)
			{
				_log.warn("could not update the ally crest id:" + e.getMessage());
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
			for (L2Clan clan : ClanTable.getInstance().getClans())
			{
				if (clan.getAllyId() == activeChar.getAllyId())
				{
					clan.setAllyCrestId(newId);
					for (L2PcInstance member : clan.getOnlineMembers(0))
						member.broadcastUserInfo();
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__87_REQUESTSETALLYCREST;
	}
}
