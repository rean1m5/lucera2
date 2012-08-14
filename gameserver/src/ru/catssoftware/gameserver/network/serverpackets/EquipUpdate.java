package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.templates.item.L2Item;

public class EquipUpdate extends L2GameServerPacket
{
	private static final String	_S__4B_EQUIPUPDATE	= "[S] 4b EquipUpdate [ddd]";
	private L2ItemInstance		_item;
	private int					_change;

	public EquipUpdate(L2ItemInstance item, int change)
	{
		_item = item;
		_change = change;
	}

	@Override
	protected final void writeImpl()
	{
		int bodypart = 0;
		writeC(0x4b);
		writeD(_change);
		writeD(_item.getObjectId());
		switch(_item.getItem().getBodyPart())
		{
			case L2Item.SLOT_L_EAR:
				bodypart = 0x01;
				break;
			case L2Item.SLOT_R_EAR:
				bodypart = 0x02;
				break;
			case L2Item.SLOT_NECK:
				bodypart = 0x03;
				break;
			case L2Item.SLOT_R_FINGER:
				bodypart = 0x04;
				break;
			case L2Item.SLOT_L_FINGER:
				bodypart = 0x05;
				break;
			case L2Item.SLOT_HEAD:
				bodypart = 0x06;
				break;
			case L2Item.SLOT_R_HAND:
				bodypart = 0x07;
				break;
			case L2Item.SLOT_L_HAND:
				bodypart = 0x08;
				break;
			case L2Item.SLOT_GLOVES:
				bodypart = 0x09;
				break;
			case L2Item.SLOT_CHEST:
				bodypart = 0x0a;
				break;
			case L2Item.SLOT_LEGS:
				bodypart = 0x0b;
				break;
			case L2Item.SLOT_FEET:
				bodypart = 0x0c;
				break;
			case L2Item.SLOT_BACK:
				bodypart = 0x0d;
				break;
			case L2Item.SLOT_LR_HAND:
				bodypart = 0x0e;
				break;
			case L2Item.SLOT_HAIR:
				bodypart = 0x0f;
				break;
		}

		writeD(bodypart);
	}

	@Override
	public String getType()
	{
		return _S__4B_EQUIPUPDATE;
	}
}
