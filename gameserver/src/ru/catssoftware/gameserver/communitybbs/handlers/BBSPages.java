package ru.catssoftware.gameserver.communitybbs.handlers;

import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.communitybbs.CommunityBoard;
import ru.catssoftware.gameserver.communitybbs.IBBSHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

public class BBSPages implements IBBSHandler {

	private static String [] PAGES = { "top","home","show","back" };
	@Override
	public String[] getCommands() {
		return PAGES;
	}

	@Override
	public String handleCommand(L2PcInstance activeChar, String command,
			String params) {
		if(command.equals("back")) {
			return activeChar.getLastPage();
		}
		if(command.equals(PAGES[0]) || command.equals(PAGES[1]))
			params = "index.htm";
		return HtmCache.getInstance().getHtm(CommunityBoard.HTMBase+params, activeChar);
	}

}
