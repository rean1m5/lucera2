package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.model.L2Character;

public class L2FortZone extends EntityZone
{
	@Override
	protected void register()
	{
		_entity = FortManager.getInstance().getFortById(_fortId);
		if (_entity != null)
		{
			// Forts: One zone for multiple purposes (could expand this later and add defender spawn areas)
			_entity.registerZone(this);
			_entity.registerHeadquartersZone(this);
		}
		else
			_log.warn("Invalid fortId: " + _fortId);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		super.onEnter(character);
		character.setInsideZone(this,FLAG_FORT, true);
	}

	@Override
	protected void onExit(L2Character character)
	{
		super.onExit(character);
		character.setInsideZone(this,FLAG_FORT, false);
	}
}