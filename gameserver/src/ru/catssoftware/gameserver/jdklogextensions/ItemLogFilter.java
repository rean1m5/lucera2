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
package ru.catssoftware.gameserver.jdklogextensions;

import java.util.EnumSet;
import java.util.logging.LogRecord;

import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.templates.item.AbstractL2ItemType;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;


/**
 * @author Advi
 */
public final class ItemLogFilter extends L2LogFilter
{
	private static final String EXCLUDED_PROCESSES = "Consume";
	
	private static final EnumSet<L2EtcItemType> EXCLUDED_ITEM_TYPES = EnumSet.of(L2EtcItemType.SHOT,
		L2EtcItemType.ARROW, L2EtcItemType.BOLT, L2EtcItemType.HERB);
	
	@Override
	public boolean isLoggable(LogRecord record)
	{
		if (!super.isLoggable(record))
			return false;
		
		if (record.getParameters() != null)
		{ 
			AbstractL2ItemType type = ((L2ItemInstance)record.getParameters()[0]).getItemType();
			if (EXCLUDED_ITEM_TYPES.contains(type))
				return false;
		}
		
		String[] messageList = record.getMessage().split(":");
		if (messageList.length >= 2 && EXCLUDED_PROCESSES.contains(messageList[1]))
			return false;
		
		return true;
	}
	
	@Override
	protected String getLoggerName()
	{
		return "item";
	}
}
