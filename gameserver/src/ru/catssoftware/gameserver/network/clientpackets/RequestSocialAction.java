package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class RequestSocialAction extends L2GameClientPacket
{
	private static final String	_C__1B_REQUESTSOCIALACTION	= "[C] 1B RequestSocialAction";

	private int					_actionId;

	@Override
	protected void readImpl()
	{
		_actionId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.isFishing())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			return;
		}

		if (_actionId < 2 || _actionId > 14)
		{
			Util.handleIllegalPlayerAction(activeChar, "Игрок " + activeChar.getName() + "пытался использовать запрещенное действие.",
				Config.DEFAULT_PUNISH);
			return;
		}

		if (activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null && !activeChar.isAlikeDead() && !activeChar.isCastingNow() && !activeChar.isCastingSimultaneouslyNow()
				&& (!activeChar.isAllSkillsDisabled() || activeChar.isInDuel()) && activeChar.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE
				&& FloodProtector.tryPerformAction(activeChar, Protected.SOCIAL))
		{
			activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), _actionId));
		}
	}

	@Override
	public String getType()
	{
		return _C__1B_REQUESTSOCIALACTION;
	}
}