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
package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.cache.HTMParser;
import ru.catssoftware.gameserver.cache.HtmCache;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import javolution.util.FastMap;

/**
 *
 * the HTML parser in the client knowns these standard and non-standard tags and attributes
 * VOLUMN
 * UNKNOWN
 * UL
 * U
 * TT
 * TR
 * TITLE
 * TEXTCODE
 * TEXTAREA
 * TD
 * TABLE
 * SUP
 * SUB
 * STRIKE
 * SPIN
 * SELECT
 * RIGHT
 * PRE
 * P
 * OPTION
 * OL
 * MULTIEDIT
 * LI
 * LEFT
 * INPUT
 * IMG
 * I
 * HTML
 * H7
 * H6
 * H5
 * H4
 * H3
 * H2
 * H1
 * FONT
 * EXTEND
 * EDIT
 * COMMENT
 * COMBOBOX
 * CENTER
 * BUTTON
 * BR
 * BODY
 * BAR
 * ADDRESS
 * A
 * SEL
 * LIST
 * VAR
 * FORE
 * READONL
 * ROWS
 * VALIGN
 * FIXWIDTH
 * BORDERCOLORLI
 * BORDERCOLORDA
 * BORDERCOLOR
 * BORDER
 * BGCOLOR
 * BACKGROUND
 * ALIGN
 * VALU
 * READONLY
 * MULTIPLE
 * SELECTED
 * TYP
 * TYPE
 * MAXLENGTH
 * CHECKED
 * SRC
 * Y
 * X
 * QUERYDELAY
 * NOSCROLLBAR
 * IMGSRC
 * B
 * FG
 * SIZE
 * FACE
 * COLOR
 * DEFFON
 * DEFFIXEDFONT
 * WIDTH
 * VALUE
 * TOOLTIP
 * NAME
 * MIN
 * MAX
 * HEIGHT
 * DISABLED
 * ALIGN
 * MSG
 * LINK
 * HREF
 * ACTION
 *
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public final class NpcHtmlMessage extends L2GameServerPacket
{
	private static final String _S__1B_NPCHTMLMESSAGE = "[S] 0f NpcHtmlMessage";

	// d S
	// d is usually 0, S is the html text starting with <html> and ending with </html>

	private int		_npcObjId;
	private String 			_fileName = null;
	private String 			_html;
	private L2Object			_npc = null;
	private FastMap<String , String > _replacement = new FastMap<String, String>();
	
	public NpcHtmlMessage(int npcObjId, int itemId)
	{
		_npcObjId = npcObjId;
	}

	public NpcHtmlMessage(int npcObjId, String text)
	{
		_npcObjId = npcObjId;

		setHtml(text);
	}

	public NpcHtmlMessage(int npcObjId)
	{
		_npcObjId = npcObjId;
	}

	@Override
	public void runImpl(L2GameClient client, L2PcInstance activeChar)
	{
	}

	public void setHtml(CharSequence text)
	{
		_html = text.toString();
	}

	public void setHtml(StringBuilder text)
	{
		_html = text.toString();
	}

	public void setFile(String path)
	{
		_fileName = path;
	}

	public void replace(String pattern, String value)
	{
		_replacement.put(pattern, value);
	}

	public void replace(String pattern, long value)
	{
		replace(pattern, String.valueOf(value));
	}

	public void replace(String pattern, double value)
	{
		replace(pattern, String.valueOf(value));
	}

	public void replace(String pattern, Object value)
	{
		replace(pattern, String.valueOf(value));
	}

	@Override
	protected void writeImpl()
	{
		if(_fileName!=null && _html==null) 
			_html = HtmCache.getInstance().getHtm(_fileName,getClient().getActiveChar());
		if(_html==null)
			return;
		writeC(0x0f);
		writeD(_npcObjId);
		if(_replacement.size()>0 ) {
			for(String s: _replacement.keySet()) 
				_html = _html.replace(s, _replacement.get(s));
			_replacement.clear();
		}
		String html = HTMParser.parseHTM(_html, getClient().getActiveChar(),_npc );
		if (html.length() > 8192)
		{
			writeS("<html><body><br>Sorry, the HTML is too long!</body></html>");
			_log.warn("The HTML is too long! This will crash the client!");
		}
		else
			writeS(html);
		writeD(0);
	}

	@Override
	public String getType()
	{
		return _S__1B_NPCHTMLMESSAGE;
	}
}