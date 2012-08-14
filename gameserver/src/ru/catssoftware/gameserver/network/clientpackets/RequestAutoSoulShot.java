package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExAutoSoulShot;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;

public class RequestAutoSoulShot extends L2GameClientPacket
{
	private static final String	_C__CF_REQUESTAUTOSOULSHOT	= "[C] CF RequestAutoSoulShot";

	private int					_itemId, _type;

	@Override
	protected void readImpl()
	{
		_itemId = readD();
		_type = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;
		if (activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null && !activeChar.isDead())
		{
			L2ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);

			if (item != null)
			{
				if (_type == 1)
				{
					if (_itemId < 6535 || _itemId > 6540)
					{
						if (_itemId == 6645 || _itemId == 6646 || _itemId == 6647)
						{
							activeChar.addAutoSoulShot(_itemId);
							ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
							activeChar.sendPacket(atk);

							SystemMessage sm = new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
							sm.addString(item.getItemName());
							activeChar.sendPacket(sm);
							sm = null;

							activeChar.rechargeAutoSoulShot(true, true, true, true);
						}
						else
						{
							int shotType = item.getItem().getCrystalType();

							if (_itemId >= 3947 && _itemId <= 3952 && activeChar.isInOlympiadMode())
							{
								activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
								return;
							}

							if (activeChar.getActiveWeaponItem() != activeChar.getFistsWeaponItem()	&& shotType == activeChar.getActiveWeaponItem().getCrystalType())
							{
								activeChar.addAutoSoulShot(_itemId);
								ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
								activeChar.sendPacket(atk);
							}
							else
							{
								if ((_itemId >= 2509 && _itemId <= 2514) || (_itemId >= 3947 && _itemId <= 3952) || _itemId == 5790)
									activeChar.sendPacket(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH);
								else
									activeChar.sendPacket(SystemMessageId.SOULSHOTS_GRADE_MISMATCH);

								activeChar.addAutoSoulShot(_itemId);
								ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
								activeChar.sendPacket(atk);
							}

							SystemMessage sm = new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
							sm.addString(item.getItemName());
							activeChar.sendPacket(sm);
							sm = null;
							activeChar.rechargeAutoSoulShot(true, true, false, true);
						}
					}
				}
				else if (_type == 0)
				{
					activeChar.removeAutoSoulShot(_itemId);
					ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
					activeChar.sendPacket(atk);

					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(item.getItemName());
					activeChar.sendPacket(sm);
					sm = null;
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__CF_REQUESTAUTOSOULSHOT;
	}
}