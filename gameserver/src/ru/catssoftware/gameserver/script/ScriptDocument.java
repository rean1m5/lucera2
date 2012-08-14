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
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ScriptDocument
{
	private final static Logger	_log		= Logger.getLogger(ScriptDocument.class);
	private Document			_document	= null;
	private String				_name		= null;

	public ScriptDocument(String name, InputStream input)
	{
		if (input == null)
			return;
		_name = name;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			_document = builder.parse(input);
		}
		catch (SAXException sxe)
		{
			// Error generated during parsing)
			_log.error("Invalid document " + name + ". Error = " + sxe.getMessage());
		}
		catch (ParserConfigurationException pce)
		{
			// Parser with specified options can't be built
			_log.error(pce.getMessage(), pce);
		}
		catch (IOException ioe)
		{
			// I/O error
			_log.error(ioe.getMessage(), ioe);
		}
	}

	public Document getDocument()
	{
		return _document;
	}

	/**
	 * @return Returns the _name.
	 */
	public String getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		return _name;
	}
}