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
package ru.catssoftware.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author NB4L1
 */
public abstract class SingletonCollection<E> implements Collection<E>
{
	protected abstract Collection<E> get(boolean init);

	public final boolean add(E e)
	{
		return get(true).add(e);
	}

	public final boolean addAll(Collection<? extends E> c)
	{
		return get(true).addAll(c);
	}

	public final void clear()
	{
		get(false).clear();
	}

	public final boolean contains(Object o)
	{
		return get(false).contains(o);
	}

	public final boolean containsAll(Collection<?> c)
	{
		return get(false).containsAll(c);
	}

	public final boolean isEmpty()
	{
		return get(false).isEmpty();
	}

	public final Iterator<E> iterator()
	{
		return get(false).iterator();
	}

	public final boolean remove(Object o)
	{
		return get(false).remove(o);
	}

	public final boolean removeAll(Collection<?> c)
	{
		return get(false).removeAll(c);
	}

	public final boolean retainAll(Collection<?> c)
	{
		return get(false).retainAll(c);
	}

	public final int size()
	{
		return get(false).size();
	}

	public final Object[] toArray()
	{
		return get(false).toArray();
	}

	public final <T> T[] toArray(T[] a)
	{
		return get(false).toArray(a);
	}
}