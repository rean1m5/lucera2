package ru.catssoftware.gameserver.network.clientpackets;

import java.util.Map;
import java.util.logging.Logger;

import ru.catssoftware.gameserver.instancemanager.RaidPointsManager;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExGetBossRecord;


public class RequestGetBossRecord extends L2GameClientPacket
{
	private static final String		 _C__D0_18_REQUESTGETBOSSRECORD	= "[C] D0:18 RequestGetBossRecord";
	protected static final Logger	_log							= Logger.getLogger(RequestGetBossRecord.class.getName());
	private int						_bossId;

	@Override
	protected void readImpl()
	{
		_bossId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if (_bossId != 0)
		{
			_log.info("C5: RequestGetBossRecord: d: "+_bossId+" ActiveChar: "+activeChar);
		}
		int points = RaidPointsManager.getPointsByOwnerId(activeChar.getObjectId());
		int ranking = RaidPointsManager.calculateRanking(activeChar.getObjectId());

		Map<Integer, Integer> list = RaidPointsManager.getList(activeChar);
		activeChar.sendPacket(new ExGetBossRecord(ranking, points, list));
	}

	@Override
	public String getType()
	{
		return _C__D0_18_REQUESTGETBOSSRECORD;
	}
}