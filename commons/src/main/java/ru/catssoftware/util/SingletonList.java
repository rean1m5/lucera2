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
import java.util.List;
import java.util.ListIterator;

import javolution.util.FastList;

/**
 * @author NB4L1
 */
public final class SingletonList<E> extends SingletonCollection<E> implements List<E>
{
	private List<E> _list;

	@Override
	protected List<E> get(boolean init)
	{
		if (_list == null)
		{
			if (init)
				_list = FastList.newInstance();
			else
				return L2Collections.emptyList();
		}

		return _list;
	}

	public void add(int index, E element)
	{
		get(true).add(index, element);
	}

	public boolean addAll(int index, Collection<? extends E> c)
	{
		return get(true).addAll(index, c);
	}

	public E get(int index)
	{
		return get(false).get(index);
	}

	public int indexOf(Object o)
	{
		return get(false).indexOf(o);
	}

	public int lastIndexOf(Object o)
	{
		return get(false).lastIndexOf(o);
	}

	public ListIterator<E> listIterator()
	{
		return get(false).listIterator();
	}

	public ListIterator<E> listIterator(int index)
	{
		return get(false).listIterator(index);
	}

	public E remove(int index)
	{
		return get(false).remove(index);
	}

	public E set(int index, E element)
	{
		return get(false).set(index, element);
	}

	public List<E> subList(int fromIndex, int toIndex)
	{
		return get(false).subList(fromIndex, toIndex);
	}
}