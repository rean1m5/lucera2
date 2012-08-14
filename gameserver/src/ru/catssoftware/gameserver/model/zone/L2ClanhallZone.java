package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.ClanHallManager;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.BanditStrongholdSiege;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.WildBeastFarmSiege;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.ClanHall;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.AgitDecoInfo;

public class L2ClanhallZone extends L2DefaultZone
{
	protected ClanHall	_clanhall;

	@Override
	protected void register()
	{
		_clanhall = ClanHallManager.getInstance().getClanHallById(_clanhallId);
		_clanhall.registerZone(this);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (_clanhall.getId()==35&&BanditStrongholdSiege.getInstance().getIsInProgress())
		{
			character.setInsideZone(this,FLAG_PVP, true);
			if (character instanceof L2PcInstance)
				character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
		}
		if (_clanhall.getId()==63&&WildBeastFarmSiege.getInstance().getIsInProgress())
		{
			character.setInsideZone(this,FLAG_PVP, true);
			if (character instanceof L2PcInstance)
				character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
		}
		if (character instanceof L2PcInstance)
		{
			// Set as in clan hall
			character.setInsideZone(this,FLAG_CLANHALL, true);
			// Send decoration packet
			if (_clanhall.getOwnerId()>0)
			{
				AgitDecoInfo deco = new AgitDecoInfo(_clanhall);
				character.sendPacket(deco);
			}
		}
		super.onEnter(character);
	}

	public void updateSiegeStatus()
	{
		if (_clanhall.getId()==35&&BanditStrongholdSiege.getInstance().getIsInProgress())
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (Exception e)
				{
				}
			}
		}
		else if (_clanhall.getId()==63&&WildBeastFarmSiege.getInstance().getIsInProgress())
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (Exception e)
				{
				}
			}
		}
		else
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					character.setInsideZone(this,FLAG_PVP, false);

					if (character instanceof L2PcInstance)
						character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
				}
				catch (Exception e)
				{
				}
			}
		}
	}
	@Override
	protected void onExit(L2Character character)
	{
		if (_clanhall.getId()==35&&BanditStrongholdSiege.getInstance().getIsInProgress())
		{
			character.setInsideZone(this,FLAG_PVP, false);
			if (character instanceof L2PcInstance)
				character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
		}
		if (_clanhall.getId()==63&&WildBeastFarmSiege.getInstance().getIsInProgress())
		{
			character.setInsideZone(this,FLAG_PVP, false);
			if (character instanceof L2PcInstance)
				character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
		}
		if (character instanceof L2PcInstance)
			character.setInsideZone(this,FLAG_CLANHALL, false);
		super.onExit(character);
	}
}