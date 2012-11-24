package ru.catssoftware.gameserver.network.clientpackets;



import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.Message;
import ru.catssoftware.Message.MessageId;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension;
import ru.catssoftware.gameserver.Announcements;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.datatables.GmListTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.gmaccess.gmController;
import ru.catssoftware.gameserver.instancemanager.*;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.DevastatedCastleSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.FortressOfDeadSiege;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.*;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.model.entity.events.GameEventManager;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.olympiad.Olympiad;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.model.zone.L2BossZone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.util.FloodProtector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EnterWorld extends L2GameClientPacket
{
	private static final String		_C__03_ENTERWORLD	= "[C] 03 EnterWorld";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
		{
			getClient().closeNow();
			return;
		}


		L2PcInstance player = L2World.getInstance().getPlayer(activeChar.getName());
		if (player != null)
		{
			_log.warn("EnterWorld failed! duplicate caracter");
			getClient().closeNow();
			return;
		}
		
		// restore instance
		if (Config.RESTORE_PLAYER_INSTANCE)
			activeChar.setInstanceId(InstanceManager.getInstance().getPlayerInstance(activeChar.getObjectId()));
		else
		{
			int instanceId = InstanceManager.getInstance().getPlayerInstance(activeChar.getObjectId());
			if (instanceId > 0)
				InstanceManager.getInstance().getInstance(instanceId).removePlayer(activeChar.getObjectId());
		}

		FloodProtector.registerNewPlayer(activeChar);

		if (gmController.getInstance().checkPrivs(activeChar))
		{

			int objId = activeChar.getObjectId();
			if (Config.SHOW_GM_LOGIN)
				Announcements.getInstance().announceToAll("Администратор: " + activeChar.getName() + " входит в игру.");
			if (Config.GM_STARTUP_INVULNERABLE && gmController.getInstance().hasAccess("invul", objId))
				activeChar.setIsInvul(true);
			if (Config.GM_STARTUP_INVISIBLE && gmController.getInstance().hasAccess("invis", objId))
				activeChar.getAppearance().setInvisible();
			if (Config.GM_STARTUP_SILENCE && gmController.getInstance().hasAccess("silence", objId))
				activeChar.setMessageRefusal(true);
			if (Config.GM_STARTUP_AUTO_LIST && gmController.getInstance().hasAccess("gmliston", objId))
				GmListTable.getInstance().addGm(activeChar, false);
			else
				GmListTable.getInstance().addGm(activeChar, true);
		}

		
		try {
			activeChar.getStat().resetModifiers();
			sendPacket(new UserInfo(activeChar, true));
		} catch(Exception e) { }
		
		
		
		if (activeChar.getRace().ordinal() == 2)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
			if (skill != null && activeChar.getSkillLevel(294) == 1)
			{
				if (GameTimeController.getInstance().isNowNight())
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_NIGHT_EFFECT_APPLIES);
					sm.addSkillName(294);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_NIGHT_EFFECT_DISAPPEARS);
					sm.addSkillName(294);
					sendPacket(sm);
				}
			}
		} 


		// Restore character's siege state
		if (activeChar.getClan() != null)
		{
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
					continue;
				if (siege.checkIsAttacker(activeChar.getClan()))
				{
					L2SiegeStatus.getInstance().addStatus(activeChar.getClanId(), activeChar.getObjectId());
					activeChar.setSiegeState((byte) 1);
				}
					
				else if (siege.checkIsDefender(activeChar.getClan()))
				{
					L2SiegeStatus.getInstance().addStatus(activeChar.getClanId(), activeChar.getObjectId());
					activeChar.setSiegeState((byte) 2);
				}
			}

			for (FortSiege fsiege : FortSiegeManager.getInstance().getSieges())
			{
				if (!fsiege.getIsInProgress())
					continue;
				if (fsiege.checkIsAttacker(activeChar.getClan()))
					activeChar.setSiegeState((byte) 1);
				else if (fsiege.checkIsDefender(activeChar.getClan()))
					activeChar.setSiegeState((byte) 2);
			}
			if(DevastatedCastleSiege.getInstance().getIsInProgress())
				if(DevastatedCastleSiege.getInstance().checkIsRegistered(activeChar.getClan()))
					activeChar.setSiegeState((byte) 1);
			if(FortressOfDeadSiege.getInstance().getIsInProgress())
				if(FortressOfDeadSiege.getInstance().checkIsRegistered(activeChar.getClan()))
					activeChar.setSiegeState((byte) 1);
		}

		// Check hero status (1 - Olympiad, 2 - Service)
		if (Hero.getInstance().getHeroes() != null && Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
			activeChar.setHero(true);
		else
			activeChar.restoreHeroServiceData(activeChar);

		if(!activeChar.isHero() && Config.ALT_STRICT_HERO_SYSTEM) {
			for(L2ItemInstance item : activeChar.getInventory().getItems())
				if(item.isHeroItem())
					activeChar.destroyItem("RemoveHero", item, null, false);
		}
		//Updating Seal of Strife Buff/Debuff 
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			int owner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE);
			if (owner != SevenSigns.CABAL_NULL)
			{
				int cabal = SevenSigns.getInstance().getPlayerCabal(activeChar);
				if (cabal == owner)
					activeChar.addSkill(SkillTable.getInstance().getInfo(5074,1), false);
				else if (cabal != SevenSigns.CABAL_NULL)
					activeChar.addSkill(SkillTable.getInstance().getInfo(5075,1), false);
			}
		}

		if (activeChar.getClan() != null && activeChar.getClan().getHasFort() > 0)
			FortManager.getInstance().getFortByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
		if (activeChar.getClan() != null && activeChar.getClan().getHasCastle() > 0)
			CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);

		// Send Macro List
		activeChar.getMacroses().sendUpdate();

		// Send Item List
		sendPacket(new ItemList(activeChar, false));

		// Send gg check (even if we are not going to check for reply)

		// Send Shortcuts
		sendPacket(new ShortCutInit(activeChar));

		activeChar.sendSkillList();

		activeChar.sendPacket(new HennaInfo(activeChar));

		Quest.playerEnter(activeChar);
		activeChar.sendPacket(new QuestList(activeChar));
		loadTutorial(activeChar);

		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			activeChar.setProtection(true);

		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());

		activeChar.getKnownList().updateKnownObjects();

		activeChar.updateEffectIcons();
		activeChar.sendEtcStatusUpdate();

		//Expand Skill
		ExStorageMaxCount esmc = new ExStorageMaxCount(activeChar);
		activeChar.sendPacket(esmc);

		FriendList fl = new FriendList(activeChar);
		sendPacket(fl);

		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);


		// Resume paused restrictions
		ObjectRestrictions.getInstance().resumeTasks(activeChar.getObjectId());

		// check Lucky (Done in RewardSkills())
		/*if (activeChar.getLevel() > 9)
		{
			for (L2Skill skill : activeChar.getAllSkills())
			{
				if (skill != null && skill.getId() == 194)
					activeChar.removeSkill(194);
			}
		}*/

		// check player skills
		if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
			activeChar.checkAllowedSkills();

		// check for academy
		activeChar.academyCheck(activeChar.getClassId().getId());

		// check for crowns
		try {
		CrownManager.getInstance().checkCrowns(activeChar);
		} catch(Exception e) {}

		if (Config.ANNOUNCE_7S_AT_START_UP)
			SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);

		Announcements.getInstance().showAnnouncements(activeChar);

		if (Config.ONLINE_PLAYERS_AT_STARTUP)
			activeChar.sendMessage("Текущий онлайн: " + L2World.getInstance().getAllPlayers().size());

		PetitionManager.getInstance().checkPetitionMessages(activeChar);

		if (activeChar.getClanId() != 0 && activeChar.getClan() != null)
		{
			PledgeShowMemberListAll psmla = new PledgeShowMemberListAll(activeChar.getClan(), activeChar);
			sendPacket(psmla);
			PledgeStatusChanged psc = new PledgeStatusChanged(activeChar.getClan());
			sendPacket(psc);
		}

		if (activeChar.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to others
			Die d = new Die(activeChar);
			sendPacket(d);
		}

		// engage and notify Partner
		if (Config.ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar);

			// Check if player is maried and remove if necessary Cupid's Bow
			if (!activeChar.isMaried())
			{
				L2ItemInstance item = activeChar.getInventory().getItemByItemId(9140);
				// Remove Cupid's Bow
				if (item != null)
				{
					activeChar.destroyItem("Removing Cupid's Bow", item, activeChar, true);
					// Logger it
					_log.info("Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " got Cupid's Bow removed.");
				}
			}
		}

		notifyFriends(activeChar);
		notifyClanMembers(activeChar);
		notifySponsorOrApprentice(activeChar);
		activeChar.onPlayerEnter();
		sendPacket(new SkillCoolTime(activeChar));

		if (Olympiad.getInstance().playerInStadia(activeChar))
		{
			activeChar.doRevive();
			activeChar.teleToLocation(TeleportWhereType.Town);
			activeChar.sendMessage("Вы перемещены в ближайший город, т.к. находились в зоне Олимпиады.");
		}

		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), true)) // Exclude waiting room
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);

		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
			activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);

		if (activeChar.getClan() != null)
		{
			//Sets the apropriate Pledge Class for the clannie (e.g. Viscount, Count, Baron, Marquiz)
			activeChar.setPledgeClass(L2ClanMember.getCurrentPledgeClass(activeChar));

			// Add message if clanHall not paid. Possibly this is custom...
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
					activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_TOMORROW);
			}
		}

		updateShortCuts(activeChar);

		// remove combat flag before teleporting
		L2ItemInstance flag = activeChar.getInventory().getItemByItemId(Config.FORTSIEGE_COMBAT_FLAG_ID);
		if (flag != null)
		{
			Fort fort = FortManager.getInstance().getFort(activeChar);
			if (fort != null)
				FortSiegeManager.getInstance().dropCombatFlag(activeChar);
			else
			{
				int slot = flag.getItem().getBodyPart();
				activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
				activeChar.destroyItem("CombatFlag", flag, null, true);
			}
		}

		if (!activeChar.isGM() && activeChar.getSiegeState() < 2 && SiegeManager.getInstance().checkIfInZone(activeChar))
		{
			// Attacker or spectator logging in to a siege zone. Actually should be checked for inside castle only?
			activeChar.teleToLocation(TeleportWhereType.Town);
		}

		
		GameEvent evt = GameEventManager.getInstance().participantOf(activeChar);
		
		if(evt!=null) {
			activeChar.setGameEvent(evt);
			activeChar.getGameEvent().onLogin(activeChar);
		}

		activeChar.regiveTemporarySkills();
		activeChar.sendPacket(ExBasicActionList.DEFAULT_ACTION_LIST);

		if (activeChar.isCursedWeaponEquipped())
			CursedWeaponsManager.getInstance().getCursedWeapon(activeChar.getCursedWeaponEquippedId()).cursedOnLogin(activeChar);

		if (Config.ALLOW_SERVER_WELCOME_MESSAGE)
			activeChar.sendPacket(new ExShowScreenMessage(Config.SERVER_WELCOME_MESSAGE, 10000));

		if (Config.SHOW_HTML_WELCOME)
		{
			String msg = HtmCache.getInstance().getHtm("data/html/welcome.htm",activeChar);
			if(msg!=null)
				activeChar.addMessage(msg.replace("%name%", activeChar.getName()));
		}

		getClient().checkKeyProtection();

		// Clan privilegies fix
		if (activeChar.getClan() != null)
		{
			int rank = activeChar.getPledgeRank();

			if (activeChar.getClan().getLeaderId() != activeChar.getObjectId())
			{
				if (rank == 0)
				{
					activeChar.setPledgeRank(5);
					activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(5));
				}
				else
				{
					activeChar.setPledgeRank(rank);
					activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(rank));
				}
			}
			else
			{
				activeChar.setPledgeRank(1);
				activeChar.setClanPrivileges(L2Clan.CP_ALL);
			}
		}
		else
		{
			activeChar.setClanPrivileges(L2Clan.CP_NOTHING);
		}

		// Check premium state
		if (activeChar.getPremiumService() > 0)
		{
			
			if(System.currentTimeMillis()>activeChar.getPremiumService())
				activeChar.setPremiumService(0);
			else  {
				activeChar.setPremiumService(activeChar.getPremiumService());
				if (Config.SHOW_PREMIUM_STATE_ON_ENTER) 
					activeChar.showPremiumState(false);
			}
		}
		L2BossZone zone = (L2BossZone)activeChar.getZone("Boss");
		if(zone!=null && zone.getLair()!=null && !activeChar.isGM())
			activeChar.teleToLocation(TeleportWhereType.Town);

		// Send event Message
		EventMessage(activeChar);
		if(activeChar.getGameEvent()!=null && activeChar.getGameEvent().isState(GameEvent.State.STATE_ACTIVE))
			activeChar.addMessage("<html><body><br>Не забудьте, вы участник эвента <font color=\"LEVEL\">"+activeChar.getGameEvent().getName()+"</font>!</body></html>");
		giveItems(activeChar);
		try {
			announce(activeChar);
		} catch(Exception e) {}
		if (activeChar.getStatus().getCurrentHp() < 0.5) // is dead
			activeChar.setIsDead(true);
		
		activeChar.showUserMessages();
		GameExtensionManager.getInstance().handleAction(activeChar, ObjectExtension.Action.CHAR_ENTERWORLD);
		activeChar.actionFail();
		
	}

	private void giveItems(L2PcInstance pc) {
		Connection con = null;
		try {
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("select * from character_items where owner_id=?");
			stm.setInt(1, pc.getObjectId());
			ResultSet rs = stm.executeQuery();
			while(rs.next()) {
				L2ItemInstance item = pc.getInventory().addItem("external", rs.getInt("item_id"), rs.getInt("count"), pc,null);
				if(item!=null && !item.isStackable() && rs.getInt("enchant_level")>0)
					item.setEnchantLevel(rs.getInt("enchant_level"));
			}
			rs.close();
			stm.close();
			stm = con.prepareStatement("delete from character_items where owner_id=?");
			stm.setInt(1, pc.getObjectId());
			stm.execute();
			stm.close();
		} catch(SQLException e) {
		}
		finally {
			if(con!=null)
				try { con.close(); } catch(SQLException e) { }
		}
	}
	private void engage(L2PcInstance cha)
	{
		int _chaid = cha.getObjectId();

		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if (cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid)
			{
				if (cl.getMaried())
					cha.setMaried(true);

				cha.setCoupleId(cl.getId());

				if (cl.getPlayer1Id() == _chaid)
					cha.setPartnerId(cl.getPlayer2Id());
				else
					cha.setPartnerId(cl.getPlayer1Id());
			}
		}
	}

	private void notifyPartner(L2PcInstance cha)
	{
		if (cha.getPartnerId() != 0)
		{
			L2PcInstance partner = L2World.getInstance().getPlayer(cha.getPartnerId());
			if (partner != null) {
				partner.sendMessage("Ваш партнер " + cha.getName() + " входит в мир.");
			}
		}
	}

	private void notifyFriends(L2PcInstance cha)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
		sm.addString(cha.getName());

		for (String friendName : L2FriendList.getFriendListNames(cha))
		{
			L2PcInstance friend = L2World.getInstance().getPlayer(friendName);
			if (friend != null) //friend logged in.
			{
				friend.sendPacket(new FriendList(friend));
				friend.sendPacket(sm);
			}
		}
		sm = null;
	}

	private void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			L2ClanMember clanmember = clan.getClanMember(activeChar.getObjectId());
			if (clanmember != null)
			{
				clanmember.setPlayerInstance(activeChar);
				SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
				msg.addString(activeChar.getName());
				clan.broadcastToOtherOnlineMembers(msg, activeChar);
				msg = null;
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);

				if (clan.isNoticeEnabled() && !clan.getNotice().isEmpty())
					activeChar.addMessage("<html><title>Clan Message</title><body><br><center><font color=\"CCAA00\">" + activeChar.getClan().getName() + "</font> <font color=\"6655FF\">Clan Alert Message</font></center><br>" + "<img src=\"L2UI.SquareWhite\" width=270 height=1><br>" + activeChar.getClan().getNotice() + "</body></html>");
			}
		}
	}

	private void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			L2PcInstance sponsor = L2World.getInstance().getPlayer(activeChar.getSponsor());

			if (sponsor != null)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				sponsor.sendPacket(msg);
			}
		}
		else if (activeChar.getApprentice() != 0)
		{
			L2PcInstance apprentice = L2World.getInstance().getPlayer(activeChar.getApprentice());

			if (apprentice != null)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				apprentice.sendPacket(msg);
			}
		}
	}

	private void updateShortCuts(L2PcInstance activeChar)
	{
		L2ShortCut[] allShortCuts = activeChar.getAllShortCuts();

		for (L2ShortCut sc : allShortCuts)
			activeChar.sendPacket(new ShortCutRegister(sc));
	}

	private void loadTutorial(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent("UC", null, player);
	}

	private void EventMessage(L2PcInstance player)
	{
		if (Config.L2DAY_SPAWN)
		{
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_DAY1));
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_DAY2));
		}
		if (Config.CRISTMAS_SPAWN)
		{
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_CHRISTMAS1));
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_CHRISTMAS2));
		}
		if (Config.MEDAL_SPAWN)
		{
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_MEDALS1));
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_MEDALS2));
		}
		if (Config.STAR_SPAWN)
		{
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_STAR1));
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_STAR2));
		}
		if (Config.BIGSQUASH_SPAWN)
		{
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_SQUASH1));
			player.sendMessage(Message.getMessage(player, MessageId.MSG_EVT_SQUASH2));
		}
	}

	private void announce(L2PcInstance pc) throws Exception {
		String msg = "Player "+pc.getName()+" entered into world!";
		if(Config.WORLD_ANNOUNCES.contains(pc.getName())) 
			Announcements.getInstance().announceToAll(msg);
		if(Config.CLAN_ANNOUNCE.contains(pc.getName()) && pc.getClan()!=null)
			Announcements.getInstance().announceToClan(pc.getClan(), msg);
		if(Config.ALLY_ANOUNCE.contains(pc.getName()) && pc.getClan()!=null)
			Announcements.getInstance().announceToAlly(pc.getClan(), msg);
		
		if(pc.getClan()!=null && pc.getClan().getLeaderId() == pc.getObjectId() ) {
			msg = "Leader of "+pc.getClan().getName()+" entered into world!";
			if(Config.WORLD_ANNOUNCES.contains("LEADER"))
				Announcements.getInstance().announceToAll(msg);
			if(Config.CLAN_ANNOUNCE.contains("LEADER"))
				Announcements.getInstance().announceToClan(pc.getClan(), msg);
			if(Config.ALLY_ANOUNCE.contains("LEADER"))
				Announcements.getInstance().announceToAlly(pc.getClan(), msg);
			if(pc.getClan().getHasCastle()>0) {
				msg = "Owner of  "+CastleManager.getInstance().getCastleById(pc.getClan().getHasCastle()).getName() +" entered into world!";
				if(Config.WORLD_ANNOUNCES.contains("LORD"))
					Announcements.getInstance().announceToAll(msg);
				if(Config.CLAN_ANNOUNCE.contains("LORD"))
					Announcements.getInstance().announceToClan(pc.getClan(), msg);
				if(Config.ALLY_ANOUNCE.contains("LORD"))
					Announcements.getInstance().announceToAlly(pc.getClan(), msg);
			}
			if(pc.getClan().getHasFort()>0 || (pc.getClan().getHasHideout() == 21 ||
					pc.getClan().getHasHideout() == 34 || pc.getClan().getHasHideout() == 35 ||
					pc.getClan().getHasHideout() == 63  || pc.getClan().getHasHideout() == 64)
					) {
				msg = "Owner of  "+ClanHallManager.getInstance().getClanHallByOwner(pc.getClan()).getName() +" entered into world!";
				if(Config.WORLD_ANNOUNCES.contains("DUKE"))
					Announcements.getInstance().announceToAll(msg);
				if(Config.CLAN_ANNOUNCE.contains("DUKE"))
					Announcements.getInstance().announceToClan(pc.getClan(), msg);
				if(Config.ALLY_ANOUNCE.contains("DUKE"))
					Announcements.getInstance().announceToAlly(pc.getClan(), msg);
			}
			
		}
	}
	@Override
	public String getType()
	{
		return _C__03_ENTERWORLD;
	}
	
}
