package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.gmaccess.gmController;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.events.GameEvent;
import ru.catssoftware.gameserver.model.entity.events.GameEventManager;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;

public class events extends gmHandler
{
	private static final String[] commands =
	{
		"events"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		if (params[0].equals("events"))
		{
			if (admin!=null)
			{
				if(params.length==3)
				{
					GameEvent evt = GameEventManager.getInstance().findEvent(params[1]);
					if(evt!=null)
					{
						if(params[2].equals("start"))
						{
							if(evt.isState(GameEvent.State.STATE_OFFLINE))
								evt.start();
						}
						else if(params[2].equals("stop"))
						{
							if(!evt.isState(GameEvent.State.STATE_OFFLINE))
								evt.finish();
						}
					}
				}
				
				NpcHtmlMessage msg = new NpcHtmlMessage(1);
				msg.setFile("data/html/admin/menus/events.htm");
				String html = "";
	
				for(GameEvent evt : GameEventManager.getInstance().getAllEvents())
				{
					html+="<tr><td>";
	
					if(gmController.getInstance().cmdExists(evt.getName())) 
						html+="<a action=\"bypass admin_"+evt.getName()+"\">"+evt.getName()+"</a>";
					else
						html+=evt.getName();
	
					html+="</td><td><font color=\"LEVEL\">";
	
					if(evt.isState(GameEvent.State.STATE_OFFLINE))
						html+="остановлен";
					else if(evt.isState(GameEvent.State.STATE_ACTIVE))
						html+="регистрация";
					else if(evt.isRunning())
						html+="игра запущена";
	
					html+="</font></td><td>";
	
					html+="<button width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\" action=\"bypass -h admin_events ";
					html+=(evt.getName()+" "+(evt.isState(GameEvent.State.STATE_ACTIVE)?"start":"stop"));
					html+="\" value=\"";
					html+=((evt.isState(GameEvent.State.STATE_ACTIVE)?"СТАРТ":"СТОП")+"\"></td>");
					html+="</tr>";
				}
				msg.replace("%events%", html);
				admin.sendPacket(msg);
			}
		}
		return;
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}