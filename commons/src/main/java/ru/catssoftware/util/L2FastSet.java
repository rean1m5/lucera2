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
package ru.catssoftware.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;

import javolution.util.FastCollection;
import javolution.util.FastMap;
import javolution.util.FastCollection.Record;
import javolution.util.FastMap.Entry;

/**
 * @author NB4L1
 */
@SuppressWarnings("unchecked")
public final class L2FastSet<E> implements Set<E>
{
	private static final Object NULL = new Object();

	private final FastMap<E, Object> _map;

	public L2FastSet()
	{
		_map = new FastMap<E, Object>();
	}

	public L2FastSet(int capacity)
	{
		_map = new FastMap<E, Object>(capacity);
	}

	public L2FastSet(Set<? extends E> elements)
	{
		_map = new FastMap<E, Object>(elements.size());

		addAll(elements);
	}

	public L2FastSet<E> setShared(boolean isShared)
	{
		_map.setShared(isShared);
		return this;
	}

	public boolean isShared()
	{
		return _map.isShared();
	}

	public Record head()
	{
		return _map.head();
	}

	public Record tail()
	{
		return _map.tail();
	}

	public E valueOf(Record record)
	{
		return ((FastMap.Entry<E, Object>)record).getKey();
	}

	public void delete(Record record)
	{
		_map.remove(((FastMap.Entry<E, Object>)record).getKey());
	}

	public E getFirst()
	{
		final Entry<E, Object> first = _map.head().getNext();
		if (first == _map.tail())
			return null;

		return first.getKey();
	}

	public E getLast()
	{
		final Entry<E, Object> last = _map.tail().getPrevious();
		if (last == _map.head())
			return null;

		return last.getKey();
	}

	public E removeFirst()
	{
		final Entry<E, Object> first = _map.head().getNext();
		if (first == _map.tail())
			return null;

		final E value = first.getKey();
		_map.remove(value);
		return value;
	}

	public E removeLast()
	{
		final Entry<E, Object> last = _map.tail().getPrevious();
		if (last == _map.head())
			return null;

		final E value = last.getKey();
		_map.remove(value);
		return value;
	}

	public boolean add(E value)
	{
		return _map.put(value, NULL) == null;
	}

	public boolean addAll(Collection<? extends E> c)
	{
		if (c instanceof RandomAccess && c instanceof List<?>)
			return addAll((List<? extends E>)c);

		if (c instanceof FastCollection<?>)
			return addAll((FastCollection<? extends E>)c);

		boolean modified = false;

		for (E e : c)
		{
			if (add(e))
				modified = true;
		}

		return modified;
	}

	private boolean addAll(FastCollection<? extends E> c)
	{
		boolean modified = false;

		for (Record r = c.head(), end = c.tail(); (r = r.getNext()) != end;)
		{
			if (add(c.valueOf(r)))
				modified = true;
		}

		return modified;
	}

	private boolean addAll(List<? extends E> c)
	{
		boolean modified = false;

		for (int i = 0, size = c.size(); i < size;)
		{
			if (add(c.get(i++)))
				modified = true;
		}

		return modified;
	}

	public void clear()
	{
		_map.clear();
	}

	public boolean contains(Object o)
	{
		return _map.containsKey(o);
	}

	public boolean containsAll(Collection<?> c)
	{
		if (c instanceof FastCollection<?>)
			return containsAll((FastCollection<? extends E>)c);

		for (Object obj : c)
		{
			if (!contains(obj))
				return false;
		}

		return true;
	}

	private boolean containsAll(FastCollection<? extends E> c)
	{
		for (Record r = c.head(), end = c.tail(); (r = r.getNext()) != end;)
		{
			if (!contains(c.valueOf(r)))
				return false;
		}

		return true;
	}

	public boolean isEmpty()
	{
		return _map.isEmpty();
	}

	public Iterator<E> iterator()
	{
		return _map.keySet().iterator();
	}

	public boolean remove(Object o)
	{
		return _map.remove(o) != null;
	}

	public boolean removeAll(Collection<?> c)
	{
		boolean modified = false;

		for (Record head = head(), r = tail().getPrevious(), previous; r != head; r = previous)
		{
			previous = r.getPrevious();
			if (c.contains(valueOf(r)))
			{
				delete(r);
				modified = true;
			}
		}

		return modified;
	}

	public boolean retainAll(Collection<?> c)
	{
		boolean modified = false;

		for (Record head = head(), r = tail().getPrevious(), previous; r != head; r = previous)
		{
			previous = r.getPrevious();
			if (!c.contains(valueOf(r)))
			{
				delete(r);
				modified = true;
			}
		}

		return modified;
	}

	public int size()
	{
		return _map.size();
	}

	public Object[] toArray()
	{
		return toArray(new Object[size()]);
	}

	public <T> T[] toArray(T[] array)
	{
		int size = size();

		if (array.length != size)
			array = (T[])Array.newInstance(array.getClass().getComponentType(), size);

		if (size == 0 && array.length == 0)
			return array;

		int i = 0;
		for (Record r = head(), end = tail(); (r = r.getNext()) != end;)
			array[i++] = (T)valueOf(r);

		return array;
	}
}