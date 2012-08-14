/*
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
package ru.catssoftware.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import ru.catssoftware.lang.L2Entity;


/**
 * @author NB4L1
 */
public final class L2ReadWriteCollection<T extends L2Entity> extends L2Collection<T>
{
	private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.ReadLock _read = _lock.readLock();
	private final ReentrantReadWriteLock.WriteLock _write = _lock.writeLock();

	@Override
	public int size()
	{
		_read.lock();
		try
		{
			return super.size();
		}
		finally
		{
			_read.unlock();
		}
	}

	@Override
	public boolean isEmpty()
	{
		_read.lock();
		try
		{
			return super.isEmpty();
		}
		finally
		{
			_read.unlock();
		}
	}

	@Override
	public boolean contains(T obj)
	{
		_read.lock();
		try
		{
			return super.contains(obj);
		}
		finally
		{
			_read.unlock();
		}
	}

	@Override
	public T get(Integer id)
	{
		_read.lock();
		try
		{
			return super.get(id);
		}
		finally
		{
			_read.unlock();
		}
	}

	@Override
	public void add(T obj)
	{
		_write.lock();
		try
		{
			super.add(obj);
		}
		finally
		{
			_write.unlock();
		}
	}

	@Override
	public void remove(T obj)
	{
		_write.lock();
		try
		{
			super.remove(obj);
		}
		finally
		{
			_write.unlock();
		}
	}

	@Override
	public void clear()
	{
		_write.lock();
		try
		{
			super.clear();
		}
		finally
		{
			_write.unlock();
		}
	}

	@Override
	public T[] toArray(T[] array)
	{
		_read.lock();
		try
		{
			return super.toArray(array);
		}
		finally
		{
			_read.unlock();
		}
	}
}