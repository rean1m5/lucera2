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

import ru.catssoftware.lang.L2Entity;

/**
 * @author NB4L1
 */
public final class L2SynchronizedCollection<T extends L2Entity> extends L2Collection<T>
{
	@Override
	public synchronized int size()
	{
		return super.size();
	}

	@Override
	public synchronized boolean isEmpty()
	{
		return super.isEmpty();
	}

	@Override
	public synchronized boolean contains(T obj)
	{
		return super.contains(obj);
	}

	@Override
	public synchronized T get(Integer id)
	{
		return super.get(id);
	}

	@Override
	public synchronized void add(T obj)
	{
		super.add(obj);
	}

	@Override
	public synchronized void remove(T obj)
	{
		super.remove(obj);
	}

	@Override
	public synchronized void clear()
	{
		super.clear();
	}

	@Override
	public synchronized T[] toArray(T[] array)
	{
		return super.toArray(array);
	}
}