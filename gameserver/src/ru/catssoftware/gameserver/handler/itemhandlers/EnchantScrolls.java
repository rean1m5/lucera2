package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.Shutdown;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.network.serverpackets.ChooseInventoryItem;

public class EnchantScrolls implements IItemHandler
{
	private static final int[]	ITEM_IDS	=
	{
		// a grade
		729, 730, 731, 732, 6569, 6570,
		// b grade
		947, 948, 949, 950, 6571, 6572,
		// c grade
		951, 952, 953, 954, 6573, 6574,
		// d grade
		955, 956, 957, 958, 6575, 6576,
		// s grade
		959, 960, 961, 962, 6577, 6578
	};
	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par){}
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isCastingNow())
			return;

		
		// Restrict enchant during restart/shutdown (because of an existing exploit)
		if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_ENCHANT && Shutdown.getCounterInstance() != null && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_ACTION_NOT_ALLOWED_DURING_SHUTDOWN));
			return;
		}
		
		activeChar.setActiveEnchantItem(item);

		int itemId = item.getItemId();

		if (Config.ALLOW_CRYSTAL_SCROLL && (itemId == 957 || itemId == 958 || itemId == 953 || itemId == 954 || // Crystal scrolls D and C Grades
				itemId == 949 || itemId == 950 || itemId == 731 || itemId == 732 || // Crystal scrolls B and A Grades
				itemId == 961 || itemId == 962))
			activeChar.sendPacket(new ChooseInventoryItem(itemId - 2));
		else
			activeChar.sendPacket(new ChooseInventoryItem(itemId));
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}