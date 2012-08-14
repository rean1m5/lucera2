package ru.catssoftware.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.BanditStrongholdSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortResistSiegeManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.WildBeastFarmSiege;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2ClanMember;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
/**
*
* @author MHard L2CatsSoftware
*/

public class L2ClanHallSiegeInfInstance extends L2NpcInstance
{
	public L2ClanHallSiegeInfInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance
			// player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the
			// L2NpcInstance
			if (!canInteract(player))
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
			{
				if (getNpcId()==35420)
					showSiegeInfoWindow(player,1);
				else if (getNpcId()==35639)
					showSiegeInfoWindow(player,2);
				else
					showMessageWindow(player,0);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to
		// avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	@Override	
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			showMessageWindow(player, val);
		}
		else if (command.startsWith("Quest"))
		{
			String quest = "";
			try
			{
				quest = command.substring(5).trim();
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}

			if (quest.length() == 0)
				showQuestWindow(player);
			else
				showQuestWindow(player, quest);
		}
		else if (command.startsWith("Registration"))
		{
			L2Clan playerClan=player.getClan();
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			String str;
			str = "<html><body>Вестник!<br>";

			switch(getTemplate().getNpcId())
			{
			case 35437:
				if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
				{
					showMessageWindow(player,1);
					return;
				}
				if (BanditStrongholdSiege.getInstance().clanhall.getOwnerClan()==playerClan)
				{
					str += "Ваш Клан уже зарегестрирован на осаду, что вы еще хотите от меня?<br>";
					str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Добавить/удалить участника осады</a><br>";
				}
				else
				{
					if (BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
					{
						str += "Ваш Клан уже зарегестрирован на осаду, что вы еще хотите от меня?<br>";
						str += "<a action=\"bypass -h npc_%objectId%_UnRegister\">Отменить регистрацию</a><br>";
						str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Добавить/удалить участника осады</a><br>";
					}
					else
					{
						int res=BanditStrongholdSiege.getInstance().registerClanOnSiege(player,playerClan);
						if (res==0)
						{
							str += "Ваш клан : <font color=\"LEVEL\">"+player.getClan().getName()+"</font>, успешно зарегистрирован на осаду Холл Клана.<br>";
							str += "Теперь Вам необходимо выбрать не более 18 игоков, которые примут участие в осаде, из членов вашего клана.<br>";
							str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Выбрать участников осады</a><br>";
						}
						else if (res==1)
						{
							str += "Вы не прошли испытание и не получили Право на участие в осаде Крепости Разбойников<br>";
							str += "Возвращайтесь когда все будет готово.";
						}
						else if (res==2)
						{
							str += "К сожалению вы опоздали. Пять лидеров кланов уже подали заявки на регистрацию.<br>";
							str += "В следующий раз будьте более разторопны.";
						}
					}
				}
				break;
			case 35627:
				if(!WildBeastFarmSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
				{
					showMessageWindow(player,1);
					return;
				}
				if (WildBeastFarmSiege.getInstance().clanhall.getOwnerClan()==playerClan)
				{
					str += "Ваш Клан уже зарегестрирован на осаду, что вы еще хотите от меня?<br>";
					str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Добавить/удалить участника осады</a><br>";
				}
				else
				{
					if (WildBeastFarmSiege.getInstance().isClanOnSiege(playerClan))
					{
						str += "Ваш Клан уже зарегестрирован на осаду, что вы еще хотите от меня?<br>";
						str += "<a action=\"bypass -h npc_%objectId%_UnRegister\">Отменить регистрацию</a><br>";
						str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Добавить/удалить участника осады</a><br>";
					}
					else
					{
						int res=WildBeastFarmSiege.getInstance().registerClanOnSiege(player,playerClan);
						if (res==0)
						{
							str += "Ваш клан : <font color=\"LEVEL\">"+player.getClan().getName()+"</font>, успешно зарегистрирован на осаду Холл Клана.<br>";
							str += "Теперь Вам необходимо выбрать не более 18 игоков, которые примут участие в осаде, из членов вашего клана.<br>";
							str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Выбрать участников осады</a><br>";
						}
						else if (res==1)
						{
							str += "Вы не прошли испытание и не получили Право на участие в осаде Крепости Разбойников<br>";
							str += "Возвращайтесь когда все будет готово.";
						}
						else if (res==2)
						{
							str += "К сожалению вы опоздали. Пять лидеров кланов уже подали заявки на регистрацию.<br>";
							str += "В следующий раз будьте более разторопны.";
						}
					}
				}
				break;
			case 35604:
				if(!RainbowSpringSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,6);
					return;
				}
				if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
				{
					showMessageWindow(player,4);
					return;
				}
				if (RainbowSpringSiege.getInstance()._clanhall.getOwnerClan()==playerClan)
				{
					str += "Ваш Клан уже зарегестрирован на осаду, что вы еще хотите от меня?<br>";
				}
				else if (RainbowSpringSiege.getInstance().isClanOnSiege(playerClan))
				{
					str += "Ваш Клан уже подал заявку на участие в соревновании за обладание Холл Кланом, что вы еще хотите от меня?<br>";
				}
				else
				{
					int res=RainbowSpringSiege.getInstance().registerClanOnSiege(player,playerClan);
					if (res>0)
						str += "Ваша заявка на участие в соревновании за обладание Холл Кланом принята, вы внесли <font color=\"LEVEL\">"+res+" Свидетельство Участия в Войне за Холл Клана Горячего Источника</font>.<br>";
					else
						str += "Для подачи заявки на участие в соревновании за обладание Холл Кланом, необходимо добыть как можно больше <font color=\"LEVEL\">Свидетельств Участия в Войне за Холл Клана Горячего Источника</font>.<br>";						
				}
				break;
			}
			str += "</body></html>";
			html.setHtml(str);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else if (command.startsWith("UnRegister"))
		{
			L2Clan playerClan=player.getClan();
			NpcHtmlMessage html;
			String str;
			if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
			{
				_log.info("Player "+player.getName()+" use packet hack, try unregister clan.");
				return;
			}
			switch(getTemplate().getNpcId())
			{
			case 35437:
				if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				html = new NpcHtmlMessage(getObjectId());
				if (BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
				{
					if (BanditStrongholdSiege.getInstance().unRegisterClan(playerClan))
					{
						str = "<html><body>Вестник!<br>";
						str += "Ваш клан : <font color=\"LEVEL\">"+player.getClan().getName()+"</font>, успешно снят с регистрации на осаду Холл Клана.<br>";
						str += "</body></html>";
						html.setHtml(str);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
				}
				else
					_log.info("Player "+player.getName()+" use packet hack, try unregister clan.");
				break;
			case 35627:
				if(!WildBeastFarmSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				html = new NpcHtmlMessage(getObjectId());
				if (WildBeastFarmSiege.getInstance().isClanOnSiege(playerClan))
				{
					if (WildBeastFarmSiege.getInstance().unRegisterClan(playerClan))
					{
						str = "<html><body>Вестник!<br>";
						str += "Ваш клан : <font color=\"LEVEL\">"+player.getClan().getName()+"</font>, успешно снят с регистрации на осаду Холл Клана.<br>";
						str += "</body></html>";
						html.setHtml(str);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
				}
				else
					_log.info("Player "+player.getName()+" use packet hack, try unregister clan.");
				break;
			case 35604:
				if(!RainbowSpringSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,6);
					return;
				}
				html = new NpcHtmlMessage(getObjectId());
				if (RainbowSpringSiege.getInstance().isClanOnSiege(playerClan))
				{
					if (RainbowSpringSiege.getInstance().unRegisterClan(player))
					{
						str = "<html><body>Вестник!<br>";
						str += "Ваш клан : <font color=\"LEVEL\">"+player.getClan().getName()+"</font>, успешно снят с регистрации на осаду Холл Клана.<br>";
						str += "</body></html>";
						html.setHtml(str);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
				}
				break;
			}
		}
		else if (command.startsWith("PlayerList"))
		{
			L2Clan playerClan=player.getClan();
			if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
			{
				return;
			}
			switch(getTemplate().getNpcId())
			{
			case 35437:
				if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				if (BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
				{
					showPlayersList(playerClan,player);
				}
				break;
			case 35627:
				if(!WildBeastFarmSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				if (WildBeastFarmSiege.getInstance().isClanOnSiege(playerClan))
				{
					showPlayersList(playerClan,player);
				}
				break;
			}
		}
		else if (command.startsWith("addPlayer"))
		{
			L2Clan playerClan=player.getClan();
			if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
			{
				return;
			}
			String val = command.substring(10);
			if (playerClan.getClanMember(val)==null)
				return;

			switch(getTemplate().getNpcId())
			{
			case 35437:
				if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}			
				BanditStrongholdSiege.getInstance().addPlayer(playerClan,val);
				if (BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
				{
					showPlayersList(playerClan,player);
				}
				break;
			case 35627:
				if(!WildBeastFarmSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}			
				WildBeastFarmSiege.getInstance().addPlayer(playerClan,val);
				if (WildBeastFarmSiege.getInstance().isClanOnSiege(playerClan))
				{
					showPlayersList(playerClan,player);
				}
				break;
			}
		}
		else if (command.startsWith("removePlayer"))
		{
			L2Clan playerClan=player.getClan();
			if ((playerClan==null)||(playerClan.getLeaderName()!=player.getName())||(playerClan.getLevel()<4))
			{
				return;
			}
			String 	val = command.substring(13);
			switch(getTemplate().getNpcId())
			{
			case 35437:
				if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				if (playerClan.getClanMember(val)!=null)
				{
					BanditStrongholdSiege.getInstance().removePlayer(playerClan,val);
				}
				if (BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
				{
					showPlayersList(playerClan,player);
				}
				break;
			case 35627:
				if(!WildBeastFarmSiege.getInstance().isRegistrationPeriod())
				{
					showMessageWindow(player,3);
					return;
				}
				if (playerClan.getClanMember(val)!=null)
				{
					WildBeastFarmSiege.getInstance().removePlayer(playerClan,val);
				}
				if (WildBeastFarmSiege.getInstance().isClanOnSiege(playerClan))
				{
					showPlayersList(playerClan,player);
				}
				break;
			}
		}
	}
	public void showPlayersList(L2Clan playerClan,L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String str;
		str = "<html><body>Вестник!<br>";
		str += "Ваш клан : <font color=\"LEVEL\">"+player.getClan().getName()+"</font>. выберите участников для осады.<br><br>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=170 align=center>Зарегестрированные</td><td width=110 align=center>действие</td></tr></table>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0>";
		if(getTemplate().getNpcId() == 35437)
			for (String temp : BanditStrongholdSiege.getInstance().getRegisteredPlayers(playerClan))
			{
				str += "<tr><td width=170>"+temp+"</td><td width=110 align=center><a action=\"bypass -h npc_%objectId%_removePlayer "+temp+"\"> Удалить</a></td></tr>";			
			}
		else if (getTemplate().getNpcId() == 35627)
			for (String temp : WildBeastFarmSiege.getInstance().getRegisteredPlayers(playerClan))
			{
				str += "<tr><td width=170>"+temp+"</td><td width=110 align=center><a action=\"bypass -h npc_%objectId%_removePlayer "+temp+"\"> Удалить</a></td></tr>";			
			}
		str += "</table>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=170 align=center>Члены Клана</td><td width=110 align=center>действие</td></tr></table>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0>";
		for (L2ClanMember temp : playerClan.getMembers())
		{
			if (getTemplate().getNpcId() == 35437 && !BanditStrongholdSiege.getInstance().getRegisteredPlayers(playerClan).contains(temp.getName()))
			{
				str += "<tr><td width=170>"+temp.getName()+"</td><td width=110 align=center><a action=\"bypass -h npc_%objectId%_addPlayer "+temp.getName()+"\"> Добавить</a></td></tr>";
			}
			if (getTemplate().getNpcId() == 35627 && !WildBeastFarmSiege.getInstance().getRegisteredPlayers(playerClan).contains(temp.getName()))
			{
				str += "<tr><td width=170>"+temp.getName()+"</td><td width=110 align=center><a action=\"bypass -h npc_%objectId%_addPlayer "+temp.getName()+"\"> Добавить</a></td></tr>";
			}
		}
		str += "</table>";
		str += "</body></html>";
		html.setHtml(str);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	public void showMessageWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		long startSiege=0;
		int npcId = getTemplate().getNpcId();
		String filename;
		if (val==0)
			filename = "data/html/default/" + npcId + ".htm";
		else
			filename = "data/html/default/" + npcId +"-"+val+ ".htm";
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		if (npcId == 35382)
		{
			startSiege=FortResistSiegeManager.getInstance().getSiegeDate().getTimeInMillis();
		}
		else if (npcId == 35437 || npcId == 35627 || npcId == 35604)
		{
			ClanHall clanhall = null;
			String clans = "";
			clans += "<table width=280 border=0>";
			int clanCount=0;

			switch(npcId)
			{
			case 35437:
				clanhall = ClanHallManager.getInstance().getClanHallById(35);
				startSiege=BanditStrongholdSiege.getInstance().getSiegeDate().getTimeInMillis();
				for (String a : BanditStrongholdSiege.getInstance().getRegisteredClans())
				{
					clanCount++;
					clans+="<tr><td><font color=\"LEVEL\">"+a+"</font>  (Количество :"+BanditStrongholdSiege.getInstance().getPlayersCount(a)+"чел.)</td></tr>";
				}
				break;
			case 35627:
				clanhall = ClanHallManager.getInstance().getClanHallById(63);
				startSiege=WildBeastFarmSiege.getInstance().getSiegeDate().getTimeInMillis();
				for (String a : WildBeastFarmSiege.getInstance().getRegisteredClans())
				{
					clanCount++;
					clans+="<tr><td><font color=\"LEVEL\">"+a+"</font>  (Количество :"+BanditStrongholdSiege.getInstance().getPlayersCount(a)+"чел.)</td></tr>";
				}
				break;
			case 35604:
				clanhall = ClanHallManager.getInstance().getClanHallById(62);
				startSiege=RainbowSpringSiege.getInstance().getSiegeDate().getTimeInMillis();
				break;
			}
			while (clanCount<5)
			{
				clans+="<tr><td><font color=\"LEVEL\">**Не зарегистрирован**</font>  (Количество : чел.)</td></tr>";
				clanCount++;
			}
			clans+= "</table>";
			html.replace("%clan%", String.valueOf(clans));
			L2Clan clan = clanhall.getOwnerClan();
			String clanName;
			if (clan==null)
				clanName="НПЦ";
			else
				clanName=clan.getName();
			html.replace("%clanname%", String.valueOf(clanName));
		}
		
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		html.replace("%SiegeDate%", String.valueOf(format.format(startSiege)));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	public void showSiegeInfoWindow(L2PcInstance player,int index)
	{
		if (validateCondition(index))
		{
			if (index==1)
				DevastatedCastleSiege.getInstance().listRegisterClan(player);
			else
				FortressOfDeadSiege.getInstance().listRegisterClan(player);
		}
		else
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/siege/" + getTemplate().getNpcId() + "-busy.htm");
			html.replace("%castlename%", getCastle().getName());
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	private boolean validateCondition(int index)
	{
		if (index==1)
			return !DevastatedCastleSiege.getInstance().getIsInProgress();
		else
			return !FortressOfDeadSiege.getInstance().getIsInProgress();
	}
}