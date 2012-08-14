package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExPutIntensiveResultForVariationMake;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.item.L2Item;

/**
 * Fromat(ch) dd
 * @author  -Wooden-
 */
public class RequestConfirmRefinerItem extends L2GameClientPacket
{
	private static final String	_C__D0_2A_REQUESTCONFIRMREFINERITEM	= "[C] D0:2A RequestConfirmRefinerItem";

	private static final int	GEMSTONE_D							= 2130;
	private static final int	GEMSTONE_C							= 2131;

	private int					_targetItemObjId;
	private int					_refinerItemObjId;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
			return;

		L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		if (refinerItem == null)
			return;

		int itemGrade = targetItem.getItem().getItemGrade();
		int refinerItemId = refinerItem.getItem().getItemId();

		// is the item a life stone?
		if (!refinerItem.getItem().isLifeStone() && targetItem.getItem().getType2()==L2Item.TYPE2_WEAPON)
		{
			activeChar.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}
		if (!refinerItem.getItem().isAccLifeStone() && targetItem.getItem().getType2()==L2Item.TYPE2_ACCESSORY)
		{
			activeChar.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		int gemstoneCount = 0;
		int gemstoneItemId = 0;
		SystemMessage sm = new SystemMessage(SystemMessageId.REQUIRES_S1_S2);
		if (targetItem.getItem().getType2()==L2Item.TYPE2_WEAPON)
		{
			switch (itemGrade)
			{
				case L2Item.CRYSTAL_C:
					gemstoneCount = 20;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				case L2Item.CRYSTAL_B:
					gemstoneCount = 30;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				case L2Item.CRYSTAL_A:
					gemstoneCount = 20;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
				case L2Item.CRYSTAL_S:
					gemstoneCount = 25;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
			}
		}
		else
		{
			switch (itemGrade)
			{
				case L2Item.CRYSTAL_C:
					gemstoneCount = 200;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				case L2Item.CRYSTAL_B:
					gemstoneCount = 300;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				case L2Item.CRYSTAL_A:
					gemstoneCount = 200;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
				case L2Item.CRYSTAL_S:
					gemstoneCount = 250;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
			}
		}

		activeChar.sendPacket(new ExPutIntensiveResultForVariationMake(_refinerItemObjId, refinerItemId, gemstoneItemId, gemstoneCount));
		activeChar.sendPacket(sm);
	}

	@Override
	public String getType()
	{
		return _C__D0_2A_REQUESTCONFIRMREFINERITEM;
	}
}