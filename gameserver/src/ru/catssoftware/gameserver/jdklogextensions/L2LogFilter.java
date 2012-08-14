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

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * @author NB4L1
 */
public abstract class L2LogFilter implements Filter
{
	@Override
	public boolean isLoggable(LogRecord record)
	{
		return getLoggerName().equals(record.getLoggerName());
	}
	
	protected abstract String getLoggerName();
}
