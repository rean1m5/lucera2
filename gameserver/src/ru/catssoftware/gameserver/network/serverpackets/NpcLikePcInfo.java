package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.actor.instance.L2NpcLikePcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;

public class NpcLikePcInfo extends L2GameServerPacket {
	private L2NpcLikePcInstance _activeChar;
	public NpcLikePcInfo(L2NpcLikePcInstance owner) {
		_activeChar = owner;
	}
	@Override
	protected void writeImpl()  {
		int _mAtkSpd = _activeChar.getMAtkSpd();
		int _pAtkSpd = _activeChar.getPAtkSpd();
		double _moveMultiplier = _activeChar.getStat().getMovementSpeedMultiplier();
		int _runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		int _walkSpd = (int) (_activeChar.getStat().getWalkSpeed() / _moveMultiplier);

		writeC(0x03);
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getName());
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.isFemale() ? 1 : 0);
		writeD(_activeChar.getClassId().getId());

		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
		writeD(0);
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
		writeD(_activeChar.getPaperdollItemId(Inventory.PAPERDOLL_FACE));

		// c6 new h's
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(0);
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
		writeD(0);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);

		writeD(0);
		writeD(0);

		writeD(_mAtkSpd);
		writeD(_pAtkSpd);

		writeD(0);
		writeD(0);

		for(int i=0;i<4;i++) {
			writeD(_runSpd);
			writeD(_walkSpd);
		}
		writeF(_activeChar.getStat().getMovementSpeedMultiplier()); // _activeChar.getProperMultiplier()
		writeF(_activeChar.getStat().getAttackSpeedMultiplier()); // _activeChar.getAttackSpeedMultiplier()
		writeF(_activeChar.getCollisionRadius());
		writeF(_activeChar.getCollisionHeight());

		writeD(_activeChar.getHairStyle());
		writeD(_activeChar.getHairColor());
		writeD(_activeChar.getFaceType());

		writeS(_activeChar.getTitle());
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
		// In UserInfo leader rights and siege flags, but here found nothing??
		// Therefore RelationChanged packet with that info is required
		writeD(0);
		writeC(1); // standing = 1  sitting = 0
		writeC(_activeChar.isRunning() ? 1 : 0); // running = 1   walking = 0
		writeC(_activeChar.isInCombat() ? 1 : 0);
		writeC(_activeChar.isAlikeDead() ? 1 : 0);
		writeC(0);
		writeC(0); // 1 on strider   2 on wyvern   0 no mount
		writeC(0); //  1 - sellshop

		writeH(0);
		writeC(0x00); // find party members
		writeD(_activeChar.getAbnormalEffect());

		writeC(0); //Changed by Thorgrim
		writeH(0); //Blue value for name (0 = white, 255 = pure blue)
		writeD(_activeChar.getClassId().getId());

		writeD(_activeChar.getMaxCp());
		writeD((int) _activeChar.getCurrentCp());
		writeC(_activeChar.getEnchantEffect());

		writeC(0x00); //team circle around feet 1= Blue, 2 = red

		writeD(0);
		writeC(0); // Symbol on char menu ctrl+I
		writeC(0x00); // Hero Aura

		writeC(0); //0x01: Fishing Mode (Cant be undone by setting back to 0)
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(_activeChar.getNameColor());

		writeD(0x00); // isRunning() as in UserInfo?

		writeD(0);
		writeD(0x00); // ??
		writeD(_activeChar.getTitleColor());

		//writeD(0x00); // ??

			writeD(0x00);
	}
}
