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
public class AccountModificationException extends Exception
{
	/**
	 * Default serialVersion UID
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Default constructor
	 */
	public AccountModificationException()
	{
		super();
	}

	/**
	 * constructor with reason
	 */
	public AccountModificationException(String reason)
	{
		super(reason);
	}

	/**
	 * Copy constructor
	 */
	public AccountModificationException(Throwable e)
	{
		super(e);
	}

	/**
	 * Copy constructor
	 */
	public AccountModificationException(String reason, Throwable e)
	{
		super(reason, e);
	}

}
