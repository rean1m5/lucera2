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
public class AccountWrongPasswordException extends Exception
{
	/**
	 *
	 */
	private static final long	serialVersionUID	= -9080179050086340310L;

	/**
	 * Default constructor
	 */
	public AccountWrongPasswordException()
	{
		super();
	}

	/**
	 * constructor with reason
	 */
	public AccountWrongPasswordException(String reason)
	{
		super("Wrong password for user " + reason);
	}

	/**
	 * Copy constructor
	 */
	public AccountWrongPasswordException(Throwable e)
	{
		super(e);
	}

	/**
	 * Copy constructor
	 */
	public AccountWrongPasswordException(String reason, Throwable e)
	{
		super("Wrong password for user " + reason, e);
	}

}
