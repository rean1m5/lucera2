package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class UserInfo extends L2GameServerPacket
{
	private static final String	_S__04_USERINFO	= "[S] 04 UserInfo";
	private L2PcInstance		_activeChar;
	private int					_runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd, _relation;
	private float				_moveMultiplier;
	private boolean				_first;

	public UserInfo(L2PcInstance cha)
	{
		this(cha, false);
	}

	public UserInfo(L2PcInstance cha, boolean first)
	{

		_activeChar = cha;
		if(cha==null)
			return;
		
		_moveMultiplier = _activeChar.getStat().getMovementSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getStat().getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
		_relation = _activeChar.isClanLeader() ? 0x40 : 0;
		if (_activeChar.getSiegeState() == 1)
			_relation |= 0x180;
		if (_activeChar.getSiegeState() == 2)
			_relation |= 0x80;
		_first = first;
/*		if(_first || _activeChar._inWorld) {
		Throwable t = new Throwable();
		t.fillInStackTrace();
		System.out.println("UI Created ");
		for(StackTraceElement e : t.getStackTrace() )
		 if(!e.getClassName().startsWith("ru.c"))
			 break;
		 else 
			 System.out.println(e.getClassName().replace("ru.catssoftware", "")+": "+e.getMethodName());
		} */
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{

		if(_activeChar==null)
			return;
		if(_activeChar!=client.getActiveChar())
			return;
		if (!_first && client.getActiveChar() != null)
			if ((activeChar._inWorld == false) && (client.getActiveChar() == activeChar))
				return;
//		System.out.println("UI Sent! ");
		writeC(0x04);

		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getName());
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.getAppearance().getSex() ? 1 : 0);

		if(_activeChar.getClassIndex() == 0)
		{
			writeD(_activeChar.getClassId().getId());
		}
		else
		{
			writeD(_activeChar.getBaseClass());
		}

		writeD(_activeChar.getLevel());
		writeQ(_activeChar.getExp());
		writeD(_activeChar.getStat().getSTR());
		writeD(_activeChar.getStat().getDEX());
		writeD(_activeChar.getStat().getCON());
		writeD(_activeChar.getStat().getINT());
		writeD(_activeChar.getStat().getWIT());
		writeD(_activeChar.getStat().getMEN());
		writeD(_activeChar.getMaxHp());
		writeD((int) _activeChar.getCurrentHp());
		writeD(_activeChar.getMaxMp());
		writeD((int) _activeChar.getCurrentMp());
		writeD(_activeChar.getSp());
		writeD(_activeChar.getCurrentLoad());
		writeD(_activeChar.getMaxLoad());
		
		writeD(0x28); // FIXME: ╨┤╨░╨╗╤М╨╜╨╛╤Б╤В╤М ╨╛╤А╤Г╨╢╨╕╤П (PTS: nWeaponRange)
		
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIRALL));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));

		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));

		// ╨Э╨Р╨з╨Р╨Ы╨Ю ╨Р╨г╨У╨Ь╨Х╨Э╨в╨Р╨ж╨Ш╨Ш
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		// ╨Ъ╨Ю╨Э╨Х╨ж ╨Р╨г╨У╨Ь╨Х╨Э╨в╨Р╨ж╨Ш╨Ш

		writeD(_activeChar.getPAtk(null));
		writeD(_activeChar.getPAtkSpd());
		writeD(_activeChar.getPDef(null));
		writeD(_activeChar.getEvasionRate(null));
		writeD(_activeChar.getAccuracy());
		writeD(_activeChar.getCriticalHit(null, null));
		writeD(_activeChar.getMAtk(null, null));

		writeD(_activeChar.getMAtkSpd());
		writeD(_activeChar.getPAtkSpd());

		writeD(_activeChar.getMDef(null, null));

		writeD(_activeChar.getPvpFlag()); // 0-non-pvp  1-pvp = violett name
		writeD(_activeChar.getKarma());

		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd); // swimspeed
		writeD(_swimWalkSpd); // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(_moveMultiplier);
		writeF(_activeChar.getStat().getAttackSpeedMultiplier());

		L2Summon pet = _activeChar.getPet();
		if(_activeChar.getMountType() != 0 && pet != null)
		{
			writeF(pet.getTemplate().getCollisionRadius());
			writeF(pet.getTemplate().getCollisionHeight());
		}
		else
		{
			writeF(_activeChar.getBaseTemplate().getCollisionRadius());
			writeF(_activeChar.getBaseTemplate().getCollisionHeight());
		}

		writeD(_activeChar.getAppearance().getHairStyle());
		writeD(_activeChar.getAppearance().getHairColor());
		writeD(_activeChar.getAppearance().getFace());
		writeD(_activeChar.isGM() ? 1 : 0); // builder level

		String title = _activeChar.getTitle();
		if(_activeChar.getAppearance().isInvisible() && _activeChar.isGM())
		{
			title = "Invisible";
		}
		if(_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate polyObj = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			if(polyObj != null)
			{
				title += " - " + polyObj.getName();
			}
		}
		if(_activeChar.isInFunEvent())
			writeS(_activeChar.getGameEvent().getTitle(_activeChar, _activeChar));
		else
			writeS(title);

		writeD(_activeChar.getClanId());
		writeD(_activeChar.getClanCrestId());
		writeD(_activeChar.getAllyId());
		writeD(_activeChar.getAllyCrestId()); // ally crest id
		// 0x40 leader rights
		// siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
		writeD(_relation);
		writeC(_activeChar.getMountType()); // mount type
		writeC(_activeChar.getPrivateStoreType());
		writeC(_activeChar.hasDwarvenCraft() ? 1 : 0);
		writeD(_activeChar.getPkKills());
		writeD(_activeChar.getPvpKills());

		writeH(_activeChar.getCubics().size());
		for(int id : _activeChar.getCubics().keySet())
		{
			writeH(id);
		}

		writeC(0x00); //FIXME: ╨┐╨╛╨╕╤Б╨║ ╨┐╨░╤А╤В╨╕╨╕ (1-╨╕╤Й╨╡╨╝ ╨┐╨░╤А╤В╨╕╤О)

		writeD(_activeChar.getAbnormalEffect());
		writeC(0x00); //unk

		writeD(_activeChar.getClanPrivileges());

		writeH(_activeChar.getRecomLeft()); //c2  recommendations remaining
		writeH(_activeChar.getRecomHave()); //c2  recommendations received
		writeD(0x00); // FIXME: MOUNT NPC ID
		writeH(_activeChar.getInventoryLimit());

		writeD(_activeChar.getClassId().getId());
		writeD(0x00); // FIXME: special effects? circles around player...
		writeD(_activeChar.getMaxCp());
		writeD((int) _activeChar.getCurrentCp());
		writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect(false));

		if(_activeChar.getTeam() == 1)
		{
			writeC(0x01); //team circle around feet 1= Blue, 2 = red
		}
		else if(_activeChar.getTeam() == 2)
		{
			writeC(0x02); //team circle around feet 1= Blue, 2 = red
		}
		else
		{
			writeC(0x00); //team circle around feet 1= Blue, 2 = red
		}

		writeD(_activeChar.getClanCrestLargeId());
		writeC(_activeChar.isNoble() ? 1 : 0); //0x01: symbol on char menu ctrl+I
		writeC(_activeChar.isHero()? 1 : 0); //0x01: Hero Aura

		writeC(_activeChar.isFishing() ? 1 : 0); //Fishing Mode
		writeD(_activeChar.getFishx()); //fishing x
		writeD(_activeChar.getFishy()); //fishing y
		writeD(_activeChar.getFishz()); //fishing z
		if(_activeChar.isInFunEvent())
			writeD(_activeChar.getGameEvent().getCharNameColor(_activeChar, _activeChar));
		else
			writeD(_activeChar.getNameColor());

		writeC(_activeChar.isRunning() ? 0x01 : 0x00); //changes the Speed display on Status Window

		writeD(_activeChar.getPledgeClass()); //changes the text above CP on Status Window
		writeD(_activeChar.getPledgeClass()); // TODO: PLEDGE TYPE
		
		if(_activeChar.isInFunEvent())
			writeD(_activeChar.getGameEvent().getCharTitleColor(_activeChar, _activeChar));
		else
			writeD(_activeChar.getTitleColor());

		if(_activeChar.isCursedWeaponEquipped())
		{
			writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()));
		}
		else
		{
			writeD(0x00);
		}
		
	}


	@Override
	public String getType()
	{
		return _S__04_USERINFO;
	}
}