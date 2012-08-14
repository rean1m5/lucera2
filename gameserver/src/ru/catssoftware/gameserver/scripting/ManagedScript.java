/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.scripting;

import ru.catssoftware.gameserver.cache.HtmCache;

import java.io.File;

/**
 * Abstract class for classes that are meant to be implemented by scripts.<BR>
 *
 * @author  KenM
 */
public abstract class ManagedScript
{
	private String	_scriptFile;
	private long	_lastLoadTime;
	private boolean	_isActive;

	public ManagedScript()
	{
		_scriptFile = L2ScriptEngineManager.getInstance().getCurrentLoadingScript();
		this.setLastLoadTime(System.currentTimeMillis());
		init_LoadGlobalData();
	}
	public void loadHTML() {
		if(_scriptFile!=null)
			HtmCache.getInstance().scanDir(new File(_scriptFile).getParentFile());
		
	}
	
	protected void init_LoadGlobalData()
	{
		loadHTML();
	}

	/**
	 * Attempts to reload this script and to refresh the necessary bindings with it ScriptControler.<BR>
	 * Subclasses of this class should override this method to properly refresh their bindings when necessary.
	 *
	 * @return true if and only if the scrip was reloaded, false otherwise.
	 */
	public boolean reload()
	{
		try
		{
			if(getScriptFile()!=null)
				L2ScriptEngineManager.getInstance().executeScript(new File(getScriptFile()));
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public abstract boolean unload();

	public void setActive(boolean status)
	{
		_isActive = status;
	}

	public boolean isActive()
	{
		return _isActive;
	}

	/**
	 * @return Returns the scriptFile.
	 */
	public String getScriptFile()
	{
		return _scriptFile;
	}

	/**
	 * @param lastLoadTime The lastLoadTime to set.
	 */
	protected void setLastLoadTime(long lastLoadTime)
	{
		_lastLoadTime = lastLoadTime;
	}

	/**
	 * @return Returns the lastLoadTime.
	 */
	protected long getLastLoadTime()
	{
		return _lastLoadTime;
	}

	public abstract String getScriptName();

	public abstract ScriptManager<?> getScriptManager();
}