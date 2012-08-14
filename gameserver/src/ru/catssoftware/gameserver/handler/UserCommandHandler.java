package ru.catssoftware.gameserver.handler;

import java.lang.reflect.Constructor;

import javolution.util.FastMap;




import ru.catssoftware.util.JarUtils;

public class UserCommandHandler
{

	private static UserCommandHandler				_instance;

	private FastMap<Integer, IUserCommandHandler>	_datatable;

	public static UserCommandHandler getInstance()
	{
		if (_instance == null)
			_instance = new UserCommandHandler();
		return _instance;
	}

	private UserCommandHandler()
	{
		_datatable = new FastMap<Integer, IUserCommandHandler>();
		try {
			for(String handler : JarUtils.enumClasses("ru.catssoftware.gameserver.handler.usercommandhandlers")) try {
				Class<?> _handler = Class.forName(handler);
				if(_handler!=null && IUserCommandHandler.class.isAssignableFrom(_handler)) {
					Constructor<?> ctor = _handler.getConstructor();
					if(ctor!=null) 
						registerUserCommandHandler((IUserCommandHandler)ctor.newInstance());
				}
			} catch(Exception e) {
				continue;
			}
		} catch(Exception e) {
		
		}
		
	}

	public void registerUserCommandHandler(IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		for (int element : ids)
		{
			_datatable.put(element, handler);
		}
	}

	public IUserCommandHandler getUserCommandHandler(int userCommand)
	{
		return _datatable.get(Integer.valueOf(userCommand));
	}

	public int size()
	{
		return _datatable.size();
	}
}