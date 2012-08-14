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
package ru.catssoftware.gameserver.script;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javolution.util.FastList;

import org.apache.log4j.Logger;


/**
 * ScriptPackage is able to read a zip file and extract a list of ScriptDocument from it
 * If the zip file doesn't contain any xml file, it is considered as an empty package.
 * Only xml file are read !
 * If a xml file is found but the ScriptDocument can't be extracted (the xml is not a valid script for sure)
 * we just log the error but the treatment continue.
 *
 * @author Luis Arias
 */
public class ScriptPackage
{
	private final static Logger		_log	= Logger.getLogger(ScriptPackage.class);

	private List<ScriptDocument>	_scriptFiles;
	private String					_name;

	public ScriptPackage(ZipFile pack)
	{
		_scriptFiles = new FastList<ScriptDocument>();
		_name = pack.getName();
		addFiles(pack);
	}

	/**
	 * @return Returns the scriptFiles.
	 */
	public List<ScriptDocument> getScriptFiles()
	{
		return _scriptFiles;
	}

	/**
	 * @param scriptFiles The scriptFiles to set.
	 */
	private void addFiles(ZipFile pack)
	{
		for (Enumeration<? extends ZipEntry> e = pack.entries(); e.hasMoreElements();)
		{
			ZipEntry entry = e.nextElement();
			if (entry.getName().endsWith(".xml"))
			{
				try
				{
					ScriptDocument newScript = new ScriptDocument(entry.getName(), pack.getInputStream(entry));
					_scriptFiles.add(newScript);
				}
				catch (IOException e1)
				{
					_log.error(e1.getMessage(), e1);
				}
			}
			else if (!entry.isDirectory())
			{
				// ignore it
			}
		}
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		if (getScriptFiles().isEmpty())
			return "Empty script Package.";

		String out = "Package Name: " + getName() + "\n";

		if (!getScriptFiles().isEmpty())
		{
			out += "Xml Script Files...\n";
			for (ScriptDocument script : getScriptFiles())
				out += script.getName() + "\n";
		}
		return out;
	}
}