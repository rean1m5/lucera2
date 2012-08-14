package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.BanditStrongholdSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.WildBeastFarmSiege;
import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2SiegeClan;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.model.entity.Siege;

public class Die extends L2GameServerPacket
{
	private static final String	_S__00_DIE	= "[S] 00 Die [dddddddd]";
	private int					_charObjId;
	private boolean				_fallDown;
	private boolean				_sweepable;
	private boolean				_inFunEvent			= false;
	private L2Character			_activeChar;
	private int					_showVillage;
	private int					_showClanhall;
	private int					_showCastle;
	private int					_showFlag;
	private int					_fixedres = 0;

	/**
	 * @param _characters
	 */
	public Die(L2Character cha)
	{
		_activeChar = cha;
		L2Clan clan = null;
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			clan = player.getClan();
			if (player.isInFunEvent())
				_inFunEvent = true;
			if (player.getGameEvent()!=null)
				_inFunEvent = !player.getGameEvent().canTeleportOnDie(player);
			_fixedres = player.isGM()?1:0;
		}
		_charObjId = cha.getObjectId();
		_fallDown = cha.mustFallDownOnDeath();
		if (cha instanceof L2Attackable)
			_sweepable = ((L2Attackable) cha).isSweepActive();
		if (clan != null)
		{
			_showClanhall = clan.getHasHideout() <= 0 ? 0 : 1;
			_showCastle = clan.getHasCastle() <= 0 ? 0 : 1;
			
			L2SiegeClan siegeClan = null;
			boolean isInDefense = false;
			Siege siege = SiegeManager.getInstance().getSiege(_activeChar);
			if (siege != null && siege.getIsInProgress())
			{
				siegeClan = siege.getAttackerClan(clan);
				if (siegeClan == null && siege.checkIsDefender(clan))
					isInDefense = true;
			}
			else
			{
				FortSiege fsiege = FortSiegeManager.getInstance().getSiege(_activeChar);
				if (fsiege != null && fsiege.getIsInProgress())
				{
					siegeClan = fsiege.getAttackerClan(clan);
					if (siegeClan == null && fsiege.checkIsDefender(clan))
						isInDefense = true;
				}
			}
			_showFlag = (siegeClan == null || isInDefense || siegeClan.getFlag().size() <= 0) ? 0 : 1;
			if (BanditStrongholdSiege.getInstance().getIsInProgress())
			{
				if (BanditStrongholdSiege.getInstance().isPlayerRegister(clan,_activeChar.getName()))
					_showFlag=1;
			}
			if (WildBeastFarmSiege.getInstance().getIsInProgress())
			{
				if (WildBeastFarmSiege.getInstance().isPlayerRegister(clan,_activeChar.getName()))
					_showFlag=1;
			}
		}
		_showVillage = 1;
	}

	@Override
	protected final void writeImpl()
	{
		if (!_fallDown)
			return;

		writeC(0x6);
		writeD(_charObjId);
		writeD(_inFunEvent ? 0x00 : _showVillage);
		writeD(_inFunEvent ? 0x00 : _showClanhall);
		writeD(_inFunEvent ? 0x00 : _showCastle);
		writeD(_showFlag);
		writeD(_sweepable ? 0x01 : 0x00); // sweepable  (blue glow)
		writeD(_inFunEvent ? 0x00 :_fixedres );
	}

	@Override
	public String getType()
	{
		return _S__00_DIE;
	}
}
