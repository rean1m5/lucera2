package ru.catssoftware.gameserver.model.zone;

import ru.catssoftware.gameserver.instancemanager.FourSepulchersManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.AntharasManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.BaiumManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.BossLair;
import ru.catssoftware.gameserver.instancemanager.grandbosses.FrintezzaManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.QueenAntManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.SailrenManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.ValakasManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.VanHalterManager;
import ru.catssoftware.gameserver.instancemanager.grandbosses.ZakenManager;
import ru.catssoftware.gameserver.instancemanager.lastimperialtomb.LastImperialTombManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;

public class L2BossZone extends L2DefaultZone
{
	public BossLair _lair;
	@Override
	protected void register()
	{
		if(_boss==null)
			_boss=Boss.NONE;
		switch (_boss)
		{
			case ANTHARAS:
				_lair = AntharasManager.getInstance();
				break;
			case BAIUM:
				_lair = BaiumManager.getInstance();
				break;
			case FRINTEZZA:
				_lair = FrintezzaManager.getInstance();
				break;
			case SAILREN:
				_lair = SailrenManager.getInstance();
				break;
			case VALAKAS:
				_lair = ValakasManager.getInstance();
				break;
			case VANHALTER:
				_lair = VanHalterManager.getInstance();
				break;
			case QUEENANT:
				_lair = QueenAntManager.getInstance();
			case FOURSEPULCHERS:
				_lair = FourSepulchersManager.getInstance().findMausoleum(getId());
				break;
			case LASTIMPERIALTOMB:
				_lair = LastImperialTombManager.getInstance();
				break;
			case ZAKEN:
				_lair = ZakenManager.getInstance();
				break;
				
		}
		if(_lair!=null)
			_lair.registerZone(this);
	}

	public BossLair getLair() {
		return _lair;
	}
	public Boss getBoss() {
		return _boss;
	}
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(this,FLAG_NOSUMMON, true);
		super.onEnter(character);
		if(_lair!=null)
			_lair.onEnter(character);
		if(_boss==Boss.QUEENANT && character instanceof L2PlayableInstance) {
			if(!character.getActingPlayer().isGM() && character.getActingPlayer().getLevel() > QueenAntManager.SAFE_LEVEL )
				character.teleToLocation(TeleportWhereType.Town);
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(this,FLAG_NOSUMMON, false);
		super.onExit(character);
		if(_lair!=null)
			_lair.onExit(character);
		
	}
}
