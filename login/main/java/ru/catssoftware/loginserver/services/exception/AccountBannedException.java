/*
 * $HeadURL: $
 *
 * $Author: $
 * $Date: $
 * $Revision: $
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.catssoftware.loginserver.services.exception;

/**
 * Exception for exception during account modification
 *
 */
public class AccountBannedException extends Exception
{
	/**
	 *
	 */
	private static final long	serialVersionUID	= 2448159234999935143L;

	/**
	 * Default constructor
	 */
	public AccountBannedException()
	{
		super();
	}

	/**
	 * constructor with reason
	 */
	public AccountBannedException(String reason)
	{
		super("Account " + reason + " is banned");
	}

	/**
	 * Copy constructor
	 */
	public AccountBannedException(Throwable e)
	{
		super(e);
	}

	/**
	 * Copy constructor
	 */
	public AccountBannedException(String reason, Throwable e)
	{
		super("Account " + reason + " is banned", e);
	}

}
