package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.instancemanager.games.fishingChampionship;

public class RequestExFishRanking extends L2GameClientPacket
{
	private static final String	_C__D0_1F_REQUESTEXFISHRANKING	= "[C] D0:1F RequestExFishRanking";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		fishingChampionship.getInstance().showMidResult(getClient().getActiveChar());
	}

	@Override
	public String getType()
	{
		return _C__D0_1F_REQUESTEXFISHRANKING;
	}
}
