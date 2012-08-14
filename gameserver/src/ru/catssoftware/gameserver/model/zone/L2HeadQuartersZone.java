package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Character;

public class L2HeadQuartersZone extends EntityZone
{
	@Override
	protected void register()
	{
		if (_castleId > 0)
		{
			_entity = CastleManager.getInstance().getCastleById(_castleId);
			_entity.registerHeadquartersZone(this);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		super.onEnter(character);
	}

	@Override
	protected void onExit(L2Character character)
	{
		super.onExit(character);
	}
}