package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.instancemanager.CursedWeaponsManager;
import ru.catssoftware.gameserver.model.L2Decoy;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class CharInfo extends L2GameServerPacket
{
	private static final String	_S__31_CHARINFO	= "[S] 31 CharInfo [dddddsddd dddddddddddd dddddddd hhhh d hhhhhhhhhhhh d hhhh hhhhhhhhhhhhhhhh dddddd dddddddd ffff ddd s ddddd ccccccc h c d c h ddd cc d ccc ddddddddddd]";
	private L2PcInstance		_activeChar;
	private Inventory			_inv;
	private int					_x;
	private int					_y;
	private int					_z;
	private	int					_objectId;
	private int					_heading;
	private int					_mAtkSpd;
	private int					_pAtkSpd;
	private int					_runSpd;
	private int					_walkSpd;
	private int					_swimRunSpd;
	private int					_swimWalkSpd;
	private int					_flRunSpd;
	private int					_flWalkSpd;
	private int					_flyRunSpd;
	private int					_flyWalkSpd;
	private float				_moveMultiplier;
	private float				_attackSpeedMultiplier;
	private boolean				_isDecoy;
	private L2Decoy				_decoy;

	public CharInfo(L2PcInstance cha)
	{
		_activeChar = cha;
		_isDecoy = false;
		_objectId = _activeChar.getObjectId();
		_inv = _activeChar.getInventory();
		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_moveMultiplier = _activeChar.getStat().getMovementSpeedMultiplier();
		_attackSpeedMultiplier = _activeChar.getStat().getAttackSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getStat().getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
		_isDecoy = false;
		_decoy =null;
	}

	public CharInfo(L2Decoy decoy) {
		this(decoy.getOwner());
		_objectId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
		_isDecoy = true;
		_decoy = decoy;
	}

	@Override
	public void runImpl(L2GameClient client, L2PcInstance attacker)
	{
		RelationChanged.sendRelationChanged(_activeChar, attacker);
	}

	@Override
	protected final void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
		{
			boolean gmSeeInvis = _isDecoy;
			if(_activeChar.getAppearance().isInvisible() && !_isDecoy)
			{
				L2PcInstance tmp = getClient().getActiveChar();
				if(tmp != null && tmp.isGM())
				{
					gmSeeInvis = true;
				}
				else
					return;
			}

			if(_activeChar.getPoly().isMorphed())
			{
				L2NpcTemplate template = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());

				if(template != null)
				{
					writeC(0x16);
					writeD(_activeChar.getObjectId());
					writeD(_activeChar.getPoly().getPolyId() + 1000000); // npctype id
					writeD(_activeChar.getKarma() > 0 ? 1 : 0);
					writeD(_x);
					writeD(_y);
					writeD(_z);
					writeD(_heading);
					writeD(0x00);
					writeD(_mAtkSpd);
					writeD(_pAtkSpd);
					writeD(_runSpd);
					writeD(_walkSpd);
					writeD(_swimRunSpd/*0x32*/); // swimspeed
					writeD(_swimWalkSpd/*0x32*/); // swimspeed
					writeD(_flRunSpd);
					writeD(_flWalkSpd);
					writeD(_flyRunSpd);
					writeD(_flyWalkSpd);
					writeF(_moveMultiplier);
					writeF(_attackSpeedMultiplier);
					writeF(template.getCollisionRadius());
					writeF(template.getCollisionHeight());
					writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND)); // right hand weapon
					writeD(0);
					writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND)); // left hand weapon
					writeC(1); // name above char 1=true ... ??
					writeC(_activeChar.isRunning() ? 1 : 0);
					writeC(_activeChar.isInCombat() ? 1 : 0);
					writeC(_activeChar.isAlikeDead() ? 1 : 0);

					if(gmSeeInvis)
					{
						writeC(0);
						writeS("Invisible");
					}
					else
					{
						writeC(_activeChar.getAppearance().isInvisible() ? 1 : 0); // invisible ?? 0=false  1=true   2=summoned (only works if model has a summon animation)

						if(_activeChar.isInFunEvent())
							writeS(_activeChar.getGameEvent().getTitle(_activeChar, activeChar));
						else
							writeS(_activeChar.getTitle());
					}
					
					if(_activeChar.isInFunEvent())
						writeS(_activeChar.getGameEvent().getName(_activeChar, activeChar));
					else
						writeS(_activeChar.getName());

					writeD(0);
					writeD(0);
					writeD(0000); // hmm karma ??

					if(gmSeeInvis)
					{
						writeD((_activeChar.getAbnormalEffect()));
					}
					else
					{
						writeD(_activeChar.getAbnormalEffect()); // C2
					}

					writeD(0); // C2
					writeD(0); // C2
					writeD(0); // C2
					writeD(0); // C2
					writeC(0); // C2
				}
				else
				{
					_log.warn("Character " + _activeChar.getName() + " (" + _activeChar.getObjectId() + ") morphed in a Npc (" + _activeChar.getPoly().getPolyId() + ") w/o template.");
				}
			}
			else
			{
				writeC(0x03);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(_objectId);
				
				if(_activeChar.isInFunEvent())
					writeS(_activeChar.getGameEvent().getName(_activeChar, activeChar));
				else
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

				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE));

				// c6 new h's
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);
				writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
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
				writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);

				writeD(_activeChar.getPvpFlag());
				writeD(_activeChar.getKarma());

				writeD(_mAtkSpd);
				writeD(_pAtkSpd);

				writeD(_activeChar.getPvpFlag());
				writeD(_activeChar.getKarma());

				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_swimRunSpd/*0x32*/); // swimspeed
				writeD(_swimWalkSpd/*0x32*/); // swimspeed
				writeD(_flRunSpd);
				writeD(_flWalkSpd);
				writeD(_flyRunSpd);
				writeD(_flyWalkSpd);
				writeF(_activeChar.getStat().getMovementSpeedMultiplier()); // _activeChar.getProperMultiplier()
				writeF(_activeChar.getStat().getAttackSpeedMultiplier()); // _activeChar.getAttackSpeedMultiplier()
				writeF(_activeChar.getBaseTemplate().getdCollisionRadius());
				writeF(_activeChar.getBaseTemplate().getdCollisionHeight());

				writeD(_activeChar.getAppearance().getHairStyle());
				writeD(_activeChar.getAppearance().getHairColor());
				writeD(_activeChar.getAppearance().getFace());

				if(gmSeeInvis && !_isDecoy)
				{
					writeS("Invisible");
				}
				else
				{
					if(_activeChar.isInFunEvent())
						writeS(_activeChar.getGameEvent().getTitle(_activeChar, activeChar));
					else
						writeS(_activeChar.getTitle());
				}

				writeD(_activeChar.getClanId());
				writeD(_activeChar.getClanCrestId());
				writeD(_activeChar.getAllyId());
				writeD(_activeChar.getAllyCrestId());
				// In UserInfo leader rights and siege flags, but here found nothing??
				// Therefore RelationChanged packet with that info is required
				writeD(0);
				if(_isDecoy) {
					writeC(_decoy.isSitting() ? 0 : 1); // standing = 1  sitting = 0
					writeC(0); // running = 1   walking = 0
					writeC(0);
					writeC(0);
					writeC(0);
				} else {
					writeC(_activeChar.isSitting() ? 0 : 1); // standing = 1  sitting = 0
					writeC(_activeChar.isRunning() ? 1 : 0); // running = 1   walking = 0
					writeC(_activeChar.isInCombat() ? 1 : 0);
					writeC(_activeChar.isAlikeDead() ? 1 : 0);
					if(gmSeeInvis)
					{
						writeC(0);
					}
					else
					{
						writeC(_activeChar.getAppearance().isInvisible() ? 1 : 0); // invisible = 1  visible =0
					}
				}
				writeC(_activeChar.getMountType()); // 1 on strider   2 on wyvern   0 no mount
				writeC(_activeChar.getPrivateStoreType()); //  1 - sellshop

				writeH(_activeChar.getCubics().size());
				for(int id : _activeChar.getCubics().keySet())
				{
					writeH(id);
				}

				writeC(0x00); // find party members

				if(gmSeeInvis)
				{
					writeD(_activeChar.getAbnormalEffect());
				}
				else
				{
					writeD(_activeChar.getAbnormalEffect());
				}

				writeC(_activeChar.getRecomLeft()); //Changed by Thorgrim
				writeH(_activeChar.getRecomHave()); //Blue value for name (0 = white, 255 = pure blue)
				writeD(_activeChar.getClassId().getId());

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
				writeC(_activeChar.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
				writeC(_activeChar.isHero()?0x01:0x00); // Hero Aura

				writeC(_activeChar.isFishing() ? 1 : 0); //0x01: Fishing Mode (Cant be undone by setting back to 0)
				writeD(_activeChar.getFishx());
				writeD(_activeChar.getFishy());
				writeD(_activeChar.getFishz());
				if(_activeChar.isInFunEvent())
					writeD(_activeChar.getGameEvent().getCharNameColor(_activeChar, activeChar));
				else	
					writeD(_activeChar.getNameColor());

				writeD(0x00); // isRunning() as in UserInfo?

				writeD(_activeChar.getPledgeClass());
				writeD(0x00); // ??
				if(_activeChar.isInFunEvent())
					writeD(_activeChar.getGameEvent().getCharTitleColor(_activeChar, activeChar));
				else	
					writeD(_activeChar.getTitleColor());

				//writeD(0x00); // ??

				if(_activeChar.isCursedWeaponEquipped())
				{
					writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()));
				}
				else
				{
					writeD(0x00);
				}
			}
		}
	}

	@Override
	public boolean canBroadcast(L2PcInstance activeChar)
	{
		if (_isDecoy)
			return true;
		
		if (activeChar == _activeChar || activeChar == null || !activeChar.canSee(_activeChar))
			return false;

		return true;
	}

	@Override
	public String getType()
	{
		return _S__31_CHARINFO;
	}
}