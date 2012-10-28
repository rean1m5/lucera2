package ru.catssoftware.gameserver.util.actions;

import javolution.util.FastList;
import ru.catssoftware.extension.GameExtensionManager;
import ru.catssoftware.extension.ObjectExtension;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

import java.util.List;

/*
 * @author Ro0TT
 * @date 07.01.2012
 */

public class PlayerActions extends ObjectExtension
{
	public static List<IOnLogin> _iOnLogin = new FastList<IOnLogin>();
	public static List<ILogOut> _iLogOut = new FastList<ILogOut>();
	public static List<IChangeSubClass> _iChangeSubCLass = new FastList<IChangeSubClass>();
	public static List<IOnKill> _iOnKill = new FastList<IOnKill>();
	
	public static PlayerActions _instance;
	public static PlayerActions getInstance()
	{
		if (_instance==null)
		{
			_instance = new PlayerActions();
			GameExtensionManager.getInstance().registerExtension(_instance);
		}
		return _instance;
	}
	
	@Override
	public Class<?>[] forClasses()
	{
		return new Class<?>[]{ L2PcInstance.class };
	}

	@Override
	public Object hanlde(Object o, Action action, Object... os)
	{
		if (o instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) o;
			if (action.equals(Action.CHAR_ENTERWORLD))
				for(IOnLogin script : _iOnLogin)
					script.intoTheGame(player);
			else if (action.equals(Action.PC_CHANGE_CLASS))
				for(IChangeSubClass script : _iChangeSubCLass)
					script.changeSubClass(player, (Integer) os[0]);
			else if (action.equals(Action.NPC_ONACTION))
				for(ILogOut script : _iLogOut)
					script.outTheGame(player);
			else if (action.equals(Action.CHAR_DIE))
				for (IOnKill script : _iOnKill)
					script.onKill(os.length == 0 || os[0] == null ? player : (L2Character) os[0], player);
		}
		return null;
	}
	
	public void addScript(Object script)
	{
		if (script instanceof IOnLogin)
			_iOnLogin.add((IOnLogin) script);

		if (script instanceof IChangeSubClass)
			_iChangeSubCLass.add((IChangeSubClass) script);
		
		if (script instanceof ILogOut)
			_iLogOut.add((ILogOut) script);

		if (script instanceof IOnKill)
			_iOnKill.add((IOnKill) script);
	}
}