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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.log4j.Logger;


/**
 * Class used to manipulate a range of date.
 * you can instantiate it with the static function parse or with a public constructor with two dates
 *
 * @author Luis Arias
 */
public class DateRange
{
	private final static Logger	_log	= Logger.getLogger(DateRange.class);

	private Date				_startDate, _endDate;

	/**
	 * Constructor
	 * @param from
	 * @param to
	 */
	public DateRange(Date from, Date to)
	{
		_startDate = from;
		_endDate = to;
	}

	/**
	 * Create a DateRange with a single String and a Date formatter
	 * The initial String is split on "-"
	 * @param dateRange
	 * @param format
	 * @return DateRange
	 */
	public static DateRange parse(String dateRange, DateFormat format)
	{
		String[] date = dateRange.split("-");
		if (date.length == 2)
		{
			try
			{
				Date start = format.parse(date[0]);
				Date end = format.parse(date[1]);

				return new DateRange(start, end);
			}
			catch (ParseException e)
			{
				_log.warn("Invalid Date Format for a dateRange.");
			}
		}
		return new DateRange(null, null);
	}

	/**
	 *
	 * @return true if date range are valid, false if the two bound interval are undefined (null)
	 */
	public boolean isValid()
	{
		return _startDate != null || _endDate != null;
	}

	/**
	 *
	 * @param date
	 * @return true if date is after startDate and before endDate
	 */
	public boolean isWithinRange(Date date)
	{
		return date.after(_startDate) && date.before(_endDate);
	}

	public Date getEndDate()
	{
		return _endDate;
	}

	public Date getStartDate()
	{
		return _startDate;
	}
}