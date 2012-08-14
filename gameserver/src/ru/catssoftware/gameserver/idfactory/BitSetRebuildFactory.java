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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javolution.util.FastList;


import org.apache.log4j.Logger;


import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.tools.util.PrimeFinder;

/*
 * author: evill33t
 */
public class BitSetRebuildFactory extends IdFactory
{
	private final static Logger	_log	= Logger.getLogger(BitSetRebuildFactory.class.getName());

	private BitSet				_freeIds;
	private AtomicInteger		_freeIdCount;
	private AtomicInteger		_nextFreeId;

	protected BitSetRebuildFactory()
	{
		super();
		initialize();
	}

	public synchronized void initialize()
	{
		_log.info("starting db rebuild, good luck");
		_log.info("this will take a while, dont kill the process or power off youre machine!");
		try
		{
			_freeIds = new BitSet(PrimeFinder.nextPrime(100000));
			_freeIds.clear();
			_freeIdCount = new AtomicInteger(FREE_OBJECT_ID_SIZE);
			List<Integer> used_ids = new FastList<Integer>();
			// first get all used ids
			for (int usedObjectId : extractUsedObjectIDTable())
				used_ids.add(usedObjectId);

			_nextFreeId = new AtomicInteger(_freeIds.nextClearBit(0));

			Connection con = null;
			con = L2DatabaseFactory.getInstance().getConnection(con);
			int nextid;
			int changedids = 0;
			// now loop through all already used oids and assign a new clean one
			for (int i : extractUsedObjectIDTable())
			{
				for (;;) //danger ;)
				{
					nextid = getNextId();
					if (!used_ids.contains(nextid))
						break;
				}
				for (String update : ID_UPDATES)
				{
					PreparedStatement ps = con.prepareStatement(update);
					ps.setInt(1, nextid);
					ps.setInt(2, i);
					ps.execute();
					ps.close();
					changedids++;
				}
			}
			_log.info("database rebuild done, changed " + changedids + " ids, set idfactory config to BitSet! ^o^/");
			System.exit(0);
		}
		catch (Exception e)
		{
			_log.fatal("could not rebuild database! :", e);
			System.exit(0);
		}
	}

	@Override
	public synchronized void releaseId(int objectID)
	{
		if ((objectID - FIRST_OID) > -1)
		{
			_freeIds.clear(objectID - FIRST_OID);
			_freeIdCount.incrementAndGet();
		}
		else
			_log.warn("BitSet ID Factory: release objectID " + objectID + " failed (< " + FIRST_OID + ")");
	}

	@Override
	public synchronized int getNextId()
	{
		int newID = _nextFreeId.get();
		_freeIds.set(newID);
		_freeIdCount.decrementAndGet();

		int nextFree = _freeIds.nextClearBit(newID);

		if (nextFree < 0)
		{
			nextFree = _freeIds.nextClearBit(0);
		}
		if (nextFree < 0)
		{
			if (_freeIds.size() < FREE_OBJECT_ID_SIZE)
			{
				increaseBitSetCapacity();
			}
			else
			{
				throw new IndexOutOfBoundsException("Ran out of valid Id's.");
			}
		}

		_nextFreeId.set(nextFree);

		return newID + FIRST_OID;
	}

	@Override
	public synchronized int size()
	{
		return _freeIdCount.get();
	}

	protected synchronized int usedIdCount()
	{
		return (size() - FIRST_OID);
	}

	protected synchronized boolean reachingBitSetCapacity()
	{
		return PrimeFinder.nextPrime(usedIdCount() * 11 / 10) > _freeIds.size();
	}

	protected synchronized void increaseBitSetCapacity()
	{
		BitSet newBitSet = new BitSet(PrimeFinder.nextPrime(usedIdCount() * 11 / 10));
		newBitSet.or(_freeIds);
		_freeIds = newBitSet;
	}

	@Override
	public int getCurrentId()
	{
		return 0;
	}
}