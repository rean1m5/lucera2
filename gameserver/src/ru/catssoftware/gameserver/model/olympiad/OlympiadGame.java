package ru.catssoftware.gameserver.model.olympiad;

import javolution.util.FastList;
import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.Message.MessageId;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension.Action;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.HeroSkillTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2CubicInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.olympiad.Olympiad.COMP_TYPE;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.skills.l2skills.L2SkillSummon;
import ru.catssoftware.util.StatsSet;

import java.util.Map;
import java.util.logging.Logger;


public class OlympiadGame
{
	protected static final Logger		_log			= Logger.getLogger(OlympiadGame.class.getName());
	protected COMP_TYPE					_type;
	protected boolean					_aborted;
	protected boolean					_gamestarted;
	protected boolean					_playerOneDisconnected;
	protected boolean					_playerTwoDisconnected;
	protected boolean					_playerOneDefaulted;
	protected boolean					_playerTwoDefaulted;
	protected String					_playerOneName;
	protected String					_playerTwoName;
	protected int						_playerOneID	= 0;
	protected int						_playerTwoID	= 0;
	protected static final int			OLY_MANAGER		= 31688;
	private static final String			POINTS			= "olympiad_points";
	private static final String			COMP_DONE		= "competitions_done";
	private static final String			COMP_WON		= "competitions_won";
	private static final String			COMP_LOST		= "competitions_lost";
	private static final String			COMP_DRAWN		= "competitions_drawn";
	protected static boolean			_battleStarted;
	public int							_damageP1		= 0;
	public int							_damageP2		= 0;
	public L2PcInstance					_playerOne;
	public L2PcInstance					_playerTwo;
	protected FastList<L2PcInstance>	_players;
	private int[]						_stadiumPort;
	private int							x1, y1, z1, x2, y2, z2;
	public int							_stadiumID;
	private SystemMessage				_sm;
	private SystemMessage				_sm2;
	private SystemMessage				_sm3;
	private L2PcInstance				_winner;

	protected OlympiadGame(int id, COMP_TYPE type, FastList<L2PcInstance> list)
	{
		_aborted = false;
		_gamestarted = false;
		_stadiumID = id;
		_playerOneDisconnected = false;
		_playerTwoDisconnected = false;
		_type = type;
		_stadiumPort = OlympiadManager.STADIUMS[id].getCoordinates();


		if (list != null)
		{
			_players = list;
			_playerOne = list.get(0);
			_playerTwo = list.get(1);

			try
			{
				_playerOneName = _playerOne.getName();
				_playerTwoName = _playerTwo.getName();
				_playerOne.setOlympiadGameId(id);
				_playerTwo.setOlympiadGameId(id);
				_playerOneID = _playerOne.getObjectId();
				_playerTwoID = _playerTwo.getObjectId();

				if (!Config.ALT_OLY_SAME_IP)
				{
					String _playerOneIp = _playerOne.getClient().getHostAddress();
					String _playerTwoIp = _playerTwo.getClient().getHostAddress();
					if (_playerOneIp.equals(_playerTwoIp))
					{
						String classed = "no";
						switch (_type)
						{
							case CLASSED:
								classed = "yes";
								break;
						}
						Olympiad.logResult(_playerOneName, _playerTwoName, 0D, 0D, 0, 0, "same ip", 0, classed);
						
						_playerOne.sendMessage(Message.getMessage(_playerOne, MessageId.MSG_OLY_SAME_IP));
						_playerTwo.sendMessage(Message.getMessage(_playerTwo, MessageId.MSG_OLY_SAME_IP));
						_aborted = true;
						clearPlayers();
					}
				}
			}
			catch (Exception e)
			{
				_aborted = true;
				clearPlayers();
			}
		}
		else
		{
			_aborted = true;
			clearPlayers();
			return;
		}
	}

	public boolean isAborted()
	{
		return _aborted;
	}

	protected void clearPlayers()
	{
		_playerOne = null;
		_playerTwo = null;
		_players = null;
		_playerOneName = "";
		_playerTwoName = "";
		_playerOneID = 0;
		_playerTwoID = 0;
	}

	protected void handleDisconnect(L2PcInstance player)
	{
		if (_gamestarted)
		{
			if (player == _playerOne)
				_playerOneDisconnected = true;
			else if (player == _playerTwo)
				_playerTwoDisconnected = true;
		}
	}

	protected void removals()
	{
		if (_aborted)
			return;

		if (_playerOne == null || _playerTwo == null)
			return;

		if (_playerOneDisconnected || _playerTwoDisconnected)
			return;

		for (L2PcInstance player : _players)
		{
			try
			{
				player.getStatus().restoreHpMp();
				// Remove Buffs
				player.stopAllEffects();

				// Remove Clan Skills
				if (player.getClan() != null)
				{
					for (L2Skill skill : player.getClan().getAllSkills())
						player.removeSkill(skill, false);
					if (player.getClan().getHasCastle() > 0)
						CastleManager.getInstance().getCastleByOwner(player.getClan()).removeResidentialSkills(player);
					if (player.getClan().getHasFort() > 0)
						FortManager.getInstance().getFortByOwner(player.getClan()).removeResidentialSkills(player);
					
				}
				// Abort casting if player casting
				if (player.isCastingNow())
					player.abortCast();

				// Force the character to be visible
				player.getAppearance().setVisible();

				// Remove Hero Skills
				if (player.isHero())
				{
					for (L2Skill skill : HeroSkillTable.getInstance().getHeroSkills())
						player.removeSkill(skill, false);
				}

				// Heal Player fully

				// Remove Summon's Buffs
				if (player.getPet() != null)
				{
					L2Summon summon = player.getPet();
					//summon.unSummon(player);
					summon.stopAllEffects();
				}

				// Remove player from his party
				if (player.getParty() != null)
				{
					L2Party party = player.getParty();
					party.removePartyMember(player);
				}

				player.checkItemRestriction();

				// Remove shot automation
				Map<Integer, Integer> activeSoulShots = player.getAutoSoulShot();
				for (int itemId : activeSoulShots.values())
				{
					player.removeAutoSoulShot(itemId);
					ExAutoSoulShot atk = new ExAutoSoulShot(itemId, 0);
					player.sendPacket(atk);
				}

				// Discharge any active shots
				L2ItemInstance weap = player.getActiveWeaponInstance();
				if (weap != null)
				{
					weap.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,false);
					weap.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}

				player.sendSkillList();

				// Remove invalid cubics
				if (Config.ALT_OLY_REMOVE_CUBICS && player.getCubics() != null)
				{
					FastList<Integer> allowedList = new FastList<Integer>();

					for (L2Skill skill : player.getAllSkills())
					{
						if (skill.isCubic() && skill instanceof L2SkillSummon)
						{
							int npcId = ((L2SkillSummon) skill).getNpcId();
							if (npcId != 0)
								allowedList.add(npcId);
						}
					}
					for (L2CubicInstance cubic : player.getCubics().values())
					{
						if (!allowedList.contains((cubic.getId())))
						{
							cubic.stopAction();
							player.delCubic(cubic.getId());
						}
					}
				}
				
			}
			catch (Exception e)
			{
			}
		}
	}

	protected void checkPet()
	{
		if (_playerOne == null || _playerTwo == null)
			return;

		for (L2PcInstance player : _players)
		{
			try
			{
				// Remove Summon's Buffs
				if (player.getPet() != null)
				{
					L2Summon summon = player.getPet();

					if (summon instanceof L2PetInstance)
						summon.unSummon(player);
				}
			}
			catch (Exception e)
			{
			}
		}
	}

	protected boolean portPlayersToArena()
	{
		
		boolean _playerOneCrash = (_playerOne == null || _playerOneDisconnected);
		boolean _playerTwoCrash = (_playerTwo == null || _playerTwoDisconnected);

		if (_playerOneCrash || _playerTwoCrash || _aborted)
		{
			_playerOne = null;
			_playerTwo = null;
			_aborted = true;
			return false;
		}

		try
		{

			if (_playerOne.getObservMode() == 1)
				_playerOne.leaveObserverMode();
			else if (_playerOne.getObservMode() == 2)
				_playerOne.leaveOlympiadObserverMode();

			if (_playerTwo.getObservMode() == 1)
				_playerTwo.leaveObserverMode();
			else if (_playerTwo.getObservMode() == 2)
				_playerTwo.leaveOlympiadObserverMode();

			x1 = _playerOne.getX();
			y1 = _playerOne.getY();
			z1 = _playerOne.getZ();
			x2 = _playerTwo.getX();
			y2 = _playerTwo.getY();
			z2 = _playerTwo.getZ();

			_playerOne.abortAttack();
			_playerOne.abortCast();
			_playerOne.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			
			_playerTwo.abortAttack();
			_playerTwo.abortCast();
			_playerTwo.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			
			if (_playerOne.isSitting())
				_playerOne.standUp();

			if (_playerTwo.isSitting())
				_playerTwo.standUp();

			_playerOne.setTarget(null);
			_playerTwo.setTarget(null);
			if(_playerOne.getPet()!=null)
				_playerOne.getPet().stopAllEffects();
			if(_playerTwo.getPet()!=null)
				_playerTwo.getPet().stopAllEffects();
			
			if(_playerOne.getParty()!=null)
				_playerOne.getParty().removePartyMember(_playerOne);
			if(_playerTwo.getParty()!=null)
				_playerTwo.getParty().removePartyMember(_playerTwo);
			_playerOne.stopAllEffects();
			_playerTwo.stopAllEffects();
			_gamestarted = true;
			_playerOne.teleToLocation(_stadiumPort[0] + 800, _stadiumPort[1], _stadiumPort[2], false);
			_playerTwo.teleToLocation(_stadiumPort[0] - 800, _stadiumPort[1], _stadiumPort[2], false);
			_playerOne.getStatus().restoreHpMp();
			_playerTwo.getStatus().restoreHpMp();
			_playerOne.sendPacket(new ExOlympiadMode(2));
			_playerTwo.sendPacket(new ExOlympiadMode(2));
			_playerOne.setIsInOlympiadMode(true);
			_playerOne.setIsOlympiadStart(false);
			_playerOne.setOlympiadSide(1);
			_playerTwo.setIsInOlympiadMode(true);
			_playerTwo.setIsOlympiadStart(false);
			_playerTwo.setOlympiadSide(2);
			OlympiadManager.enchantUpdate(_playerOne);
			OlympiadManager.enchantUpdate(_playerTwo);
			OlympiadManager.checkRestrictItem(_playerOne);
			OlympiadManager.checkRestrictItem(_playerTwo);
			_playerOne.broadcastFullInfo();
			_playerTwo.broadcastFullInfo();

		}
		catch (NullPointerException e)
		{
			return false;
		}
		return true;
	}

	protected void sendMessageToPlayers(boolean toBattleBegin, int nsecond)
	{
		if (!toBattleBegin)
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
		else
			_sm = new SystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S);

		_sm.addNumber(nsecond);
		try
		{
			for (L2PcInstance player : _players) {
				if(player.getClient()==null || player.getClient().isDisconnected())
					handleDisconnect(player);
				player.sendPacket(_sm);
			}
		}
		catch (Exception e)
		{
		}
	}

	protected void cleanEffects()
	{
		if (_playerOne == null || _playerTwo == null)
			return;

		if (_playerOneDisconnected || _playerTwoDisconnected)
			return;

		for (L2PcInstance player : _players)
		{
			try
			{
				player.stopAllEffects();
				player.clearCharges();

				if (player.getPet() != null)
				{
					L2Summon summon = player.getPet();
					summon.stopAllEffects();
				}
			}
			catch (Exception e)
			{
			}
		}
	}

	protected void portPlayersBack()
	{
		if (_playerOne != null) {
			_playerOne.teleToLocation(x1, y1, z1, true);
			GameExtensionManager.getInstance().handleAction(_playerOne, Action.PC_OLY_BATTLE_FINISHED,_winner);
		}
		
		if (_playerTwo != null) {
			_playerTwo.teleToLocation(x2, y2, z2, true);
			GameExtensionManager.getInstance().handleAction(_playerTwo, Action.PC_OLY_BATTLE_FINISHED,_winner);
		}

		if(Config.ALT_OLY_RESET_SKILL_TIME) {
			_playerOne.resetSkillTime(true);
			_playerTwo.resetSkillTime(true);
		}
	}

	protected void PlayersStatusBack()
	{
		for (L2PcInstance player : _players)
		{
			try
			{
				if (player.isDead() == true)
					player.setIsDead(false);


				player.setIsPendingRevive(false);
				player.getStatus().setCurrentCp(player.getMaxCp());
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());
				player.setIsInOlympiadMode(false);
				OlympiadManager.enchantUpdate(player);
				player.setIsOlympiadStart(false);
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.sendPacket(new ExOlympiadMode(0));
				player.getStatus().startHpMpRegeneration();

				// Add Clan Skills
				if (player.getClan() != null)
				{
					for (L2Skill skill : player.getClan().getAllSkills())
					{
						if (skill.getMinPledgeClass() <= player.getPledgeClass())
							player.addSkill(skill, false);
					}
					if (player.getClan().getHasCastle() > 0)
						CastleManager.getInstance().getCastleByOwner(player.getClan()).giveResidentialSkills(player);
					if (player.getClan().getHasFort() > 0)
						FortManager.getInstance().getFortByOwner(player.getClan()).giveResidentialSkills(player);
				}

				// Add Hero Skills
				if (player.isHero())
				{
					for (L2Skill skill : HeroSkillTable.getInstance().getHeroSkills())
						player.addSkill(skill, false);
				}
				player.sendSkillList();
			}
			catch (Exception e)
			{
			}
		}
	}

	protected boolean haveWinner()
	{
		if (_aborted || _playerOne == null || _playerTwo == null || _playerOneDisconnected || _playerTwoDisconnected)
			return true;

		double playerOneHp = 0;

		try
		{
			if (_playerOne != null && _playerOne.getOlympiadGameId() != -1)
				playerOneHp = _playerOne.getStatus().getCurrentHp();
		}
		catch (Exception e)
		{
			playerOneHp = 0;
		}

		double playerTwoHp = 0;
		try
		{
			if (_playerTwo != null && _playerTwo.getOlympiadGameId() != -1)
				playerTwoHp = _playerTwo.getStatus().getCurrentHp();
		}
		catch (Exception e)
		{
			playerTwoHp = 0;
		}
		if (playerTwoHp <= 0 || playerOneHp <= 0)
			return true;

		return false;
	}

	protected void validateWinner()
	{
		if (_aborted)
			return;

		boolean _pOneCrash = (_playerOne == null || _playerOneDisconnected);
		boolean _pTwoCrash = (_playerTwo == null || _playerTwoDisconnected);

		int _div;
		int _gpreward;

		String classed;
		switch (_type)
		{
			case NON_CLASSED:
				_div = 5;
				_gpreward = Config.ALT_OLY_NONCLASSED_RITEM_C;
				classed = "no";
				break;
			default:
				_div = 3;
				_gpreward = Config.ALT_OLY_CLASSED_RITEM_C;
				classed = "yes";
				break;
		}

		StatsSet playerOneStat = Olympiad.getNobleStats(_playerOneID);
		StatsSet playerTwoStat = Olympiad.getNobleStats(_playerTwoID);

		int playerOnePlayed = playerOneStat.getInteger(COMP_DONE);
		int playerTwoPlayed = playerTwoStat.getInteger(COMP_DONE);
		int playerOneWon = playerOneStat.getInteger(COMP_WON);
		int playerTwoWon = playerTwoStat.getInteger(COMP_WON);
		int playerOneLost = playerOneStat.getInteger(COMP_LOST);
		int playerTwoLost = playerTwoStat.getInteger(COMP_LOST);
		int playerOneDrawn = playerOneStat.getInteger(COMP_DRAWN);
		int playerTwoDrawn = playerTwoStat.getInteger(COMP_DRAWN);
		int playerOnePoints = playerOneStat.getInteger(POINTS);
		int playerTwoPoints = playerTwoStat.getInteger(POINTS);
		int pointDiff = Math.min(playerOnePoints, playerTwoPoints) / _div;

		// Check for if a player defaulted before battle started
		if (_playerOneDefaulted || _playerTwoDefaulted)
		{
			if (_playerOneDefaulted)
			{
				int lostPoints = playerOnePoints / 3;
				playerOneStat.set(POINTS, playerOnePoints - lostPoints);
				Olympiad.updateNobleStats(_playerOneID, playerOneStat);
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
				sm.addString(_playerOneName);
				sm.addNumber(lostPoints);
				broadcastMessage(sm, false);
				Olympiad.logResult(_playerOneName, _playerTwoName, 0D, 0D, 0, 0, _playerOneName + " default", lostPoints, classed);
			}
			if (_playerTwoDefaulted)
			{
				int lostPoints = playerTwoPoints / 3;
				playerTwoStat.set(POINTS, playerTwoPoints - lostPoints);
				Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
				sm.addString(_playerTwoName);
				sm.addNumber(lostPoints);
				broadcastMessage(sm, false);
				Olympiad.logResult(_playerOneName, _playerTwoName, 0D, 0D, 0, 0, _playerTwoName + " default", lostPoints, classed);
			}
			return;
		}

		// Create results for players if a player crashed
		if (_pOneCrash || _pTwoCrash)
		{
			if (_pOneCrash && !_pTwoCrash)
			{
				try
				{
					playerOneStat.set(POINTS, playerOnePoints - pointDiff);
					playerOneStat.set(COMP_LOST, playerOneLost + 1);
					Olympiad.logResult(_playerOneName, _playerTwoName, 0D, 0D, 0, 0, _playerOneName + " crash", pointDiff, classed);
					playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
					playerTwoStat.set(COMP_WON, playerTwoWon + 1);
					_sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					_sm.addString(_playerTwoName);
					broadcastMessage(_sm, true);
					_sm2.addString(_playerTwoName);
					_sm2.addNumber(pointDiff);
					broadcastMessage(_sm2, false);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_pTwoCrash && !_pOneCrash)
			{
				try
				{
					playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
					playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
					Olympiad.logResult(_playerOneName, _playerTwoName, 0D, 0D, 0, 0, _playerTwoName + " crash", pointDiff, classed);
					playerOneStat.set(POINTS, playerOnePoints + pointDiff);
					playerOneStat.set(COMP_WON, playerOneWon + 1);
					_sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					_sm.addString(_playerOneName);
					broadcastMessage(_sm, true);
					_sm2.addString(_playerOneName);
					_sm2.addNumber(pointDiff);
					broadcastMessage(_sm2, false);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_pOneCrash && _pTwoCrash)
			{
				try
				{
					playerOneStat.set(POINTS, playerOnePoints - pointDiff);
					playerOneStat.set(COMP_LOST, playerOneLost + 1);

					playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
					playerTwoStat.set(COMP_LOST, playerTwoLost + 1);

					Olympiad.logResult(_playerOneName, _playerTwoName, 0D, 0D, 0, 0, "both crash", pointDiff, classed);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
			playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
			Olympiad.updateNobleStats(_playerOneID, playerOneStat);
			Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);
			return;
		}

		double playerOneHp = 0;
		if (!_playerOne.isDead())
			playerOneHp = _playerOne.getStatus().getCurrentHp() + _playerOne.getStatus().getCurrentCp();

		double playerTwoHp = 0;
		if (!_playerTwo.isDead())
			playerTwoHp = _playerTwo.getStatus().getCurrentHp() + _playerTwo.getStatus().getCurrentCp();

		_sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
		_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
		_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);

		// if players crashed, search if they've relogged
		_playerOne = L2World.getInstance().getPlayer(_playerOneName);
		_players.set(0, _playerOne);
		_playerTwo = L2World.getInstance().getPlayer(_playerTwoName);
		_players.set(1, _playerTwo);

		String winner = "draw";

		if (_playerOne == null && _playerTwo == null)
		{
			playerOneStat.set(COMP_DRAWN, playerOneDrawn + 1);
			playerTwoStat.set(COMP_DRAWN, playerTwoDrawn + 1);
			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
			broadcastMessage(_sm, true);
		}
		else if (_playerTwo == null || _playerTwo.isOnline() == 0 || (playerTwoHp == 0 && playerOneHp != 0) || (_damageP1 > _damageP2 && playerTwoHp != 0 && playerOneHp != 0))
		{
			playerOneStat.set(POINTS, playerOnePoints + pointDiff);
			playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
			playerOneStat.set(COMP_WON, playerOneWon + 1);
			playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
			_winner = _playerOne;
			_sm.addString(_playerOneName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerOneName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerTwoName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, false);
			winner = _playerOneName + " won";

			try
			{
				L2ItemInstance item = _playerOne.getInventory().addItem("Olympiad", Config.ALT_OLY_BATTLE_REWARD_ITEM, _gpreward, _playerOne, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerOne.sendPacket(iu);

				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addNumber(_gpreward);
				_playerOne.sendPacket(sm);
			}
			catch (Exception e)
			{
			}
		}
		else if (_playerOne == null || _playerOne.isOnline() == 0 || (playerOneHp == 0 && playerTwoHp != 0) || (_damageP2 > _damageP1 && playerOneHp != 0 && playerTwoHp != 0))
		{
			playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
			playerOneStat.set(POINTS, playerOnePoints - pointDiff);
			playerTwoStat.set(COMP_WON, playerTwoWon + 1);
			playerOneStat.set(COMP_LOST, playerOneLost + 1);
			_winner = _playerTwo;
			_sm.addString(_playerTwoName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerTwoName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerOneName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, false);
			winner = _playerTwoName + " won";

			try
			{
				L2ItemInstance item = _playerTwo.getInventory().addItem("Olympiad", Config.ALT_OLY_BATTLE_REWARD_ITEM, _gpreward, _playerTwo, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerTwo.sendPacket(iu);

				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addNumber(_gpreward);
				_playerTwo.sendPacket(sm);
			}
			catch (Exception e)
			{
			}
		}
		else
		{
			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
			broadcastMessage(_sm, true);
			if(Config.ALT_OLY_REMOVE_POINTS_ON_TIE) {
				int pointOneDiff = playerOnePoints / 5;
				int pointTwoDiff = playerTwoPoints / 5;
				playerOneStat.set(POINTS, playerOnePoints - pointOneDiff);
				playerTwoStat.set(POINTS, playerTwoPoints - pointTwoDiff);
				playerOneStat.set(COMP_DRAWN, playerOneDrawn + 1);
				playerTwoStat.set(COMP_DRAWN, playerTwoDrawn + 1);
				_sm2 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
				_sm2.addString(_playerOneName);
				_sm2.addNumber(pointOneDiff);
				broadcastMessage(_sm2, false);
				_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
				_sm3.addString(_playerTwoName);
				_sm3.addNumber(pointTwoDiff);
				broadcastMessage(_sm3, false);
			}
		}

		playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
		playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);

		Olympiad.updateNobleStats(_playerOneID, playerOneStat);
		Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);

		Olympiad.logResult(_playerOneName, _playerTwoName, playerOneHp, playerTwoHp, _damageP1, _damageP2, winner, pointDiff, classed);

		for (int i = 40; i > 10; i -= 10)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
			_sm.addNumber(i);
			broadcastMessage(_sm, false);
			try
			{
				Thread.sleep(10000);
			}
			catch (InterruptedException e)
			{
			}
			if (i == 20)
			{
				_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
				_sm.addNumber(10);
				broadcastMessage(_sm, false);
				try
				{
					Thread.sleep(5000);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		for (int i = 5; i > 0; i--)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
			_sm.addNumber(i);
			broadcastMessage(_sm, false);
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	protected boolean makeCompetitionStart()
	{
		if (_aborted)
			return false;

		_sm = new SystemMessage(SystemMessageId.STARTS_THE_GAME);
		broadcastMessage(_sm, true);
		try
		{
			for (L2PcInstance player : _players)
				player.setIsOlympiadStart(true);
		}
		catch (Exception e)
		{
			_aborted = true;
			return false;
		}

		return true;
	}

	protected void addDamage(L2PcInstance player, int damage)
	{
		if (_playerOne == null || _playerTwo == null)
			return;

		if (player == _playerOne)
			_damageP1 += damage;
		else if (player == _playerTwo)
			_damageP2 += damage;
	}

	protected String getTitle()
	{
		String msg = "";
		if (_battleStarted)
			msg=Message.getMessage(null, MessageId.MSG_OLY_INGAME);
		else
			msg=Message.getMessage(null, MessageId.MSG_OLY_WAIT);
		msg += _playerOneName + " : " + _playerTwoName;
		return msg;
	}

	protected L2PcInstance[] getPlayers()
	{
		L2PcInstance[] players = new L2PcInstance[2];

		if (_playerOne == null || _playerTwo == null)
			return null;

		players[0] = _playerOne;
		players[1] = _playerTwo;

		return players;
	}

	private void broadcastMessage(SystemMessage sm, boolean toAll)
	{
		try
		{
			_playerOne.sendPacket(sm);
			_playerTwo.sendPacket(sm);
		}
		catch (Exception e)
		{
		}

		if (toAll && OlympiadManager.STADIUMS[_stadiumID].getSpectators() != null)
		{
			try
			{
			for (L2PcInstance spec : OlympiadManager.STADIUMS[_stadiumID].getSpectators())
			{

				try {
					spec.sendPacket(sm);
				}
				catch (NullPointerException e)
				{
				}
			}
			} catch(Exception e) { }
		}
	}

	protected void additions()
	{
		for(L2PcInstance player : _players)
		{
			try
			{
				L2Skill skill;
				SystemMessage sm;

				skill = SkillTable.getInstance().getInfo(1204, 1);
				skill.getEffects(player, player);
				sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				sm.addSkillName(1204);
				player.sendPacket(sm);

				if(!player.isMageClass())
				{
					skill = SkillTable.getInstance().getInfo(1086, 1);
					skill.getEffects(player, player);
					sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(1086);
					player.sendPacket(sm);
				}
				else
				{
					skill = SkillTable.getInstance().getInfo(1085, 1);
					skill.getEffects(player, player);
					sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(1085);
					player.sendPacket(sm);
				}
				player.getStatus().setCurrentCp(player.getMaxCp());
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());

				L2Summon summon = player.getSummon();
				if (summon != null)
				{
					summon.getStatus().setCurrentCp(summon.getMaxCp());
					summon.getStatus().setCurrentHp(summon.getMaxHp());
					summon.getStatus().setCurrentMp(summon.getMaxMp());
				}
			}
			catch(Exception e)
			{
				//null
			}
		}
	}
}

/**
 * @author ascharot
 */
class OlympiadGameTask implements Runnable
{
	protected static final Logger	_log			= Logger.getLogger(OlympiadGameTask.class.getName());
	public OlympiadGame				_game			= null;
	protected static final long		BATTLE_PERIOD	= Config.ALT_OLY_BATTLE;								// 6 mins

	private boolean					_terminated		= false;
	private boolean					_started		= false;

	public boolean isTerminated()
	{
		return _terminated || _game._aborted;
	}

	public boolean isStarted()
	{
		return _started;
	}

	public OlympiadGameTask(OlympiadGame game)
	{
		_game = game;
	}

	protected boolean checkBattleStatus()
	{
		boolean _pOneCrash = (_game._playerOne == null || _game._playerOneDisconnected);
		boolean _pTwoCrash = (_game._playerTwo == null || _game._playerTwoDisconnected);
		if (_pOneCrash || _pTwoCrash || _game._aborted)
			return false;

		return true;
	}

	@SuppressWarnings("static-access")
	protected boolean checkDefaulted()
	{
		_game._playerOne = L2World.getInstance().getPlayer(_game._playerOneName);
		_game._players.set(0, _game._playerOne);
		_game._playerTwo = L2World.getInstance().getPlayer(_game._playerTwoName);
		_game._players.set(1, _game._playerTwo);
		for (int i = 0; i < 2; i++)
		{
			boolean defaulted = false;
			L2PcInstance player = _game._players.get(i);
			if (player != null)
				player.setOlympiadGameId(_game._stadiumID);
			L2PcInstance otherPlayer = _game._players.get(i ^ 1);
			SystemMessage sm = null;

			if (player == null)
				defaulted = true;
			else if (player.isDead())
			{
				sm = new SystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_OLYMPIAD_WHILE_DEAD);
				sm.addPcName(player);
				defaulted = true;
			}
			else if (player.isSubClassActive())
			{
				sm = new SystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_WHILE_CHANGED_TO_SUB_CLASS);
				sm.addPcName(player);
				defaulted = true;
			}
			else if (player.isCursedWeaponEquipped())
			{
				sm = new SystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1);
				sm.addItemName(player.getCursedWeaponEquippedId());
				defaulted = true;
			}
			else if (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize())
			{
				sm = new SystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_INVENTORY_SLOT_EXCEEDS_80_PERCENT);
				sm.addPcName(player);
				defaulted = true;
			}

			if (defaulted)
			{
				if (player != null)
					player.sendPacket(sm);
				if (otherPlayer != null)
					otherPlayer.sendPacket(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
				if (i == 0)
					_game._playerOneDefaulted = true;
				else
					_game._playerTwoDefaulted = true;
			}
		}
		return _game._playerOneDefaulted || _game._playerTwoDefaulted;
	}

	public void run()
	{
		_started = true;
		if (_game != null)
		{
			if (_game._playerOne == null || _game._playerTwo == null)
				return;

			if (teleportCountdown())
				runGame();

			_terminated = true;
			_game.validateWinner();
			_game.cleanEffects();
			_game.PlayersStatusBack();

			if (_game._gamestarted)
			{
				_game._gamestarted = false;
				try
				{
					_game.portPlayersBack();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			if (OlympiadManager.STADIUMS[_game._stadiumID].getSpectators() != null)
			{
				for (L2PcInstance spec : OlympiadManager.STADIUMS[_game._stadiumID].getSpectators())
					spec.sendPacket(new ExOlympiadMatchEnd());
			}

			_game.clearPlayers();
			OlympiadManager.getInstance().removeGame(_game);
			_game = null;
		}
	}

	private boolean runGame()
	{
		// Checking for opponents and teleporting to arena
		if (checkDefaulted())
			return false;

		_game.portPlayersToArena();
		_game.removals();

		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
		}

		synchronized (this)
		{
			if (!OlympiadGame._battleStarted)
				OlympiadGame._battleStarted = true;
		}

		for (int i = 60; i > 10; i -= 10)
		{
			_game.sendMessageToPlayers(true, i);
			try
			{
				Thread.sleep(10000);
			}
			catch (InterruptedException e)
			{
			}
			if (i == 20)
			{
				_game._damageP1 = 0;
				_game._damageP2 = 0;
				_game.checkPet();
				_game.sendMessageToPlayers(true, 10);
				try
				{
					Thread.sleep(5000);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		_game.additions();
		for (int i = 5; i > 0; i--)
		{
			_game.sendMessageToPlayers(true, i);
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
			}
		}

		if (!checkBattleStatus())
			return false;

		_game._playerOne.sendPacket(new ExOlympiadUserInfo(_game._playerTwo, 1));
		_game._playerTwo.sendPacket(new ExOlympiadUserInfo(_game._playerOne, 1));
		_game._playerOne.updateEffectIcons();
		_game._playerTwo.updateEffectIcons();

		if (OlympiadManager.STADIUMS[_game._stadiumID].getSpectators() != null)
		{
			for (L2PcInstance spec : OlympiadManager.STADIUMS[_game._stadiumID].getSpectators())
			{
				spec.sendPacket(new ExOlympiadUserInfo(_game._playerOne));
				spec.sendPacket(new ExOlympiadUserInfo(_game._playerTwo));
			}
		}

		if (!_game.makeCompetitionStart())
			return false;

		// Wait 3 mins (Battle)
		for (int i = 0; i < BATTLE_PERIOD; i += 10000)
		{
			try
			{
				Thread.sleep(10000);
				if (_game.haveWinner())
					break;
			}
			catch (InterruptedException e)
			{
			}
		}
		return checkBattleStatus();
	}

	private boolean teleportCountdown()
	{
		// Waiting for teleport to arena
		for (int i = 45; i > 10; i -= 5)
		{
			switch (i)
			{
				case 45:
				case 30:
				case 15:
					_game.sendMessageToPlayers(false, i);
					break;
			}
			try
			{
				Thread.sleep(5000);
			}
			catch (InterruptedException e)
			{
				return false;
			}
		}
		for (int i = 5; i > 0; i--)
		{
			_game.sendMessageToPlayers(false, i);
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				return false;
			}
		}

		return true;
	}
}
