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
package ru.catssoftware.gameserver.idfactory;


/**
 * This class is used to implement a basic Id factory based on increment
 * Don't use it in a real environnement !!!
 *
 */
public class IncrementIDFactory extends IdFactory
{
	private int	_curId;

	protected IncrementIDFactory()
	{
		super();
		try {
			int ids[] = extractUsedObjectIDTable();
			_curId = ids[ids.length-1];
			_curId++;
		} catch(Exception e) {
			_curId = FIRST_OID;
		}
		_log.info("IdFactory: Next objectId is "+_curId);
		_initialized = true;
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.idfactory.IdFactory#getNextId()
	 */
	@Override
	public synchronized int getNextId()
	{
		return _curId++;
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.idfactory.IdFactory#releaseId(int)
	 */
	@Override
	public synchronized void releaseId(int id)
	{
		// do nothing
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.idfactory.IdFactory#size()
	 */
	@Override
	public int size()
	{
		return LAST_OID - _curId;
	}

	/**
	 * This class is for test purpose, we don't need to clean up the db
	 * @see ru.catssoftware.gameserver.idfactory.IdFactory#cleanUpDB()
	 */
	@Override
	protected void cleanUpDB()
	{
		// Do nothing
	}

	/**
	 * This class is for test purpose, we don't need to set all character offline
	 * @see ru.catssoftware.gameserver.idfactory.IdFactory#setAllCharacterOffline()
	 */
	@Override
	protected void setAllCharacterOffline()
	{
		// Do nothing
	}

	@Override
	public int getCurrentId()
	{
		return _curId;
	}
}