package ru.catssoftware.gameserver.communitybbs.handlers;


// import ru.catssoftware.Config;
import ru.catssoftware.gameserver.communitybbs.IBBSHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
/**
 * 
 * @author Azagthtot
 * Обработчик _bbsgetfav для поддержки gmshop в BBS
 */
public class BBSFavorites implements IBBSHandler {


	private String [] _commands = {"getfav"};
	@Override
	public String[] getCommands() {
		return _commands;
	}

	@Override
	public String handleCommand(L2PcInstance activeChar, String command,
			String params) {
			if(params!=null && params.length()>0) {
				String args[] = params.split(" ");
				if(args[0].equals("Link"))
					return "gmshop/"+args[1];
			}
			return "gmshop/main.htm";
	}

}
