package ru.catssoftware.gameserver.network.serverpackets;
//TODO: Rebuild!
import ru.catssoftware.gameserver.datatables.ClanTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.SystemMessageId;
import javolution.text.TextBuilder;


public class AllyInfo extends L2GameServerPacket
{
	private static final String _S__B5_ALLYINFO = "[S] b5 AllyInfo";
	private L2PcInstance _cha;
	
	public AllyInfo(L2PcInstance cha)
	{
		_cha = cha;
	}
	
	@Override
	public void runImpl(final L2GameClient client, final L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;
		
		if (activeChar.getAllyId() == 0)
		{
			_cha.sendPacket(SystemMessageId.NO_CURRENT_ALLIANCES);
			return;
		}

		//======<AllyInfo>======
		SystemMessage sm = null;
		_cha.sendPacket(SystemMessageId.ALLIANCE_INFO_HEAD);
		//======<Ally Name>======
		sm = new SystemMessage(SystemMessageId.ALLIANCE_NAME_S1);
		sm.addString(_cha.getClan().getAllyName());
		_cha.sendPacket(sm);
		int online = 0;
		int count = 0;
		int clancount = 0;
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == _cha.getAllyId())
			{
				clancount++;
				online += clan.getOnlineMembers(0).length;
				count += clan.getMembers().length;
			}
		}
		//Connection
		sm = new SystemMessage(SystemMessageId.CONNECTION_S1_TOTAL_S2);
		sm.addNumber(online);
		sm.addNumber(count);
		_cha.sendPacket(sm);
		L2Clan leaderclan = ClanTable.getInstance().getClan(_cha.getAllyId());
		sm = new SystemMessage(SystemMessageId.ALLIANCE_LEADER_S2_OF_S1);
		sm.addString(leaderclan.getName());
		sm.addString(leaderclan.getLeaderName());
		_cha.sendPacket(sm);
		//clan count
		sm = new SystemMessage(SystemMessageId.ALLIANCE_CLAN_TOTAL_S1);
		sm.addNumber(clancount);
		_cha.sendPacket(sm);
		//clan information
		_cha.sendPacket(SystemMessageId.CLAN_INFO_HEAD);
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == _cha.getAllyId())
			{
				//clan name
				sm = new SystemMessage(SystemMessageId.CLAN_INFO_NAME_S1);
				sm.addString(clan.getName());
				_cha.sendPacket(sm);
				//clan leader name
				sm = new SystemMessage(SystemMessageId.CLAN_INFO_LEADER_S1);
				sm.addString(clan.getLeaderName());
				_cha.sendPacket(sm);
				//clan level
				sm = new SystemMessage(SystemMessageId.CLAN_INFO_LEVEL_S1);
				sm.addNumber(clan.getLevel());
				_cha.sendPacket(sm);
				//---------
				_cha.sendPacket(SystemMessageId.CLAN_INFO_SEPARATOR);
			}
		}
		//=========================
		_cha.sendPacket(SystemMessageId.CLAN_INFO_FOOT);
		NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		TextBuilder replyMSG = new TextBuilder("<html><title>Информация альянса</title><body>");
		replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		for (L2Clan clan : ClanTable.getInstance().getClans())
			if (clan.getAllyId() == _cha.getAllyId())
			{
				replyMSG
					.append("<br><center><button value=\"")
					.append(clan.getName())
					.append("\" action=\"bypass -h show_clan_info ")
					.append(clan.getName())
					.append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center><br>");
			}
		replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		_cha.sendPacket(adminReply);
	}

	@Override
	public String getType()
	{
		return _S__B5_ALLYINFO;
	}
}