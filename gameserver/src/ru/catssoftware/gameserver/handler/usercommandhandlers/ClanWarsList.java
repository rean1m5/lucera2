package ru.catssoftware.gameserver.handler.usercommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;


public class ClanWarsList implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 88, 89, 90 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0] && id != COMMAND_IDS[1] && id != COMMAND_IDS[2])
			return false;

		L2Clan clan = activeChar.getClan();
		if (clan == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
			return false;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement;
			if (id == 88)
			{
				//attack list
				activeChar.sendPacket(SystemMessageId.CLANS_YOU_DECLARED_WAR_ON);
				statement = con.prepareStatement("SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan1=? AND clan_id=clan2 AND clan2 NOT IN (SELECT clan1 FROM clan_wars WHERE clan2=?)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, clan.getClanId());
			}
			else if (id == 89)
			{
				//under attack list
				activeChar.sendPacket(SystemMessageId.CLANS_THAT_HAVE_DECLARED_WAR_ON_YOU);
				statement = con
						.prepareStatement("SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan2=? AND clan_id=clan1 AND clan1 NOT IN (SELECT clan2 FROM clan_wars WHERE clan1=?)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, clan.getClanId());
			}
			else // id = 90
			{
				//war list
				activeChar.sendPacket(SystemMessageId.WAR_LIST);
				statement = con.prepareStatement("SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan1=? AND clan_id=clan2 AND clan2 IN (SELECT clan1 FROM clan_wars WHERE clan2=?)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, clan.getClanId());
			}
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				SystemMessage sm = null;
				String clanName = rset.getString("clan_name");
				int ally_id = rset.getInt("ally_id");
				if (ally_id > 0)
				{
					//target with ally
					sm = new SystemMessage(SystemMessageId.S1_S2_ALLIANCE);
					sm.addString(clanName);
					sm.addString(rset.getString("ally_name"));
				}
				else
				{
					//target without ally
					sm = new SystemMessage(SystemMessageId.S1_NO_ALLI_EXISTS);
					sm.addString(clanName);
				}
				activeChar.sendPacket(sm);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
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

		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}