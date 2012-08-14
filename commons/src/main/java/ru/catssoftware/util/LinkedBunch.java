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

import java.lang.reflect.Array;
import java.util.List;


import ru.catssoftware.util.L2Collections.Filter;


/**
 * <p>
 * Default implementation of {@link Bunch<E>}. It uses nodes to store objects, so no array reallocation needed. The
 * created nodes are reused so garbage is reduced heavily.
 * </p>
 * 
 * @author NB4L1
 */
@SuppressWarnings("unchecked")
public final class LinkedBunch<E> extends AbstractNode implements Bunch<E>
{

	private static final class Node extends AbstractNode
	{
		private static final ObjectPool<Node> POOL = new ObjectPool<Node>() {
			@Override
			protected Node create()
			{
				return new Node();
			}
		};

		private static <E> Node newInstance(final LinkedBunch<E> bunch, final Object value)
		{
			final Node node = POOL.get();

			node.setPrevious(bunch._last);
			node.setValue(value);
			node.setNext(null);

			if (bunch._last != null)
				bunch._last.setNext(node);
			bunch._last = node;
			bunch._size++;

			return node;
		}

		private static <E> void recycle(final LinkedBunch<E> bunch, final Node node)
		{
			bunch._size--;
			if (bunch._last == node)
				bunch._last = bunch._last.getPrevious();

			if (node.getPrevious() != null)
				node.getPrevious().setNext(node.getNext());

			node.setValue(null);

			if (node.getNext() != null)
				node.getNext().setPrevious(node.getPrevious());

			POOL.store(node);
		}

		private AbstractNode _previous;
		private Object _value;

		@Override
		AbstractNode getPrevious()
		{
			return _previous;
		}

		@Override
		void setPrevious(AbstractNode previous)
		{
			_previous = previous;
		}

		@Override
		Object getValue()
		{
			return _value;
		}

		@Override
		void setValue(Object value)
		{
			_value = value;
		}
	}

	private AbstractNode _last = this;
	private int _size = 0;

	private E valueOf(AbstractNode node)
	{
		return (E)node.getValue();
	}

	private AbstractNode delete(AbstractNode node)
	{
		AbstractNode previous = node.getPrevious();

		Node.recycle(this, (Node)node);

		return previous;
	}

	private Node getNode(int index)
	{
		if (index < 0 || size() <= index)
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());

		int i = 0;
		for (AbstractNode node = this; (node = node.getNext()) != null; i++)
		{
			if (i == index)
				return (Node)node;
		}

		return null;
	}

	public int size()
	{
		return _size;
	}

	public LinkedBunch<E> add(E value)
	{
		if (value == null)
		{
			return this;
		}

		Node.newInstance(this, value);
		return this;
	}

	public LinkedBunch<E> remove(E value)
	{
		for (AbstractNode node = this; (node = node.getNext()) != null;)
		{
			if (equals(value, valueOf(node)))
				node = delete(node);
		}

		return this;
	}

	public void clear()
	{
		for (AbstractNode node = this; (node = node.getNext()) != null;)
			node = delete(node);
	}

	public boolean isEmpty()
	{
		return size() == 0;
	}

	public E get(int index)
	{
		return valueOf(getNode(index));
	}

	public E set(int index, E value)
	{
		if (value == null)
			throw new NullPointerException();

		final Node node = getNode(index);
		final E old = valueOf(node);

		node.setValue(value);

		return old;
	}

	public E remove(int index)
	{
		final Node node = getNode(index);
		final E value = valueOf(node);

		delete(node);

		return value;
	}

	public boolean contains(E value)
	{
		for (AbstractNode node = this; (node = node.getNext()) != null;)
		{
			if (equals(value, valueOf(node)))
				return true;
		}

		return false;
	}

	public LinkedBunch<E> addAll(LinkedBunch<? extends E> b)
	{
		if (b != null)
		{
			for (AbstractNode node = b; (node = node.getNext()) != null;)
				add(valueOf(node));
		}

		return this;
	}

	public LinkedBunch<E> addAll(Iterable<? extends E> c)
	{
		if (c != null)
		{
			for (E e : c)
				add(e);
		}

		return this;
	}

	public LinkedBunch<E> addAll(E[] array)
	{
		if (array != null)
		{
			for (E e : array)
				add(e);
		}

		return this;
	}

	public Object[] moveToArray()
	{
		return moveToArray(new Object[size()]);
	}

	public <T> T[] moveToArray(T[] array)
	{
		if (array.length != size())
			array = (T[])Array.newInstance(array.getClass().getComponentType(), size());

		int i = 0;
		for (AbstractNode node = this; (node = node.getNext()) != null && i < array.length;)
		{
			array[i++] = (T)valueOf(node);

			node = delete(node);
		}

		clear();

		return array;
	}

	public <T> T[] moveToArray(Class<T> clazz)
	{
		return moveToArray((T[])Array.newInstance(clazz, size()));
	}

	public List<E> moveToList(List<E> list)
	{
		for (AbstractNode node = this; (node = node.getNext()) != null;)
		{
			list.add(valueOf(node));

			node = delete(node);
		}

		clear();

		return list;
	}

	private boolean equals(E o1, E o2)
	{
		return o1 == null ? o2 == null : o1 == o2 || o1.equals(o2);
	}

	public LinkedBunch<E> cleanByFilter(Filter<E> filter)
	{
		for (AbstractNode node = this; (node = node.getNext()) != null;)
		{
			if (!filter.accept(valueOf(node)))
				node = delete(node);
		}

		return this;
	}
}

abstract class AbstractNode
{
	AbstractNode getPrevious()
	{
		throw new UnsupportedOperationException();
	}

	void setPrevious(AbstractNode previous)
	{
		throw new UnsupportedOperationException();
	}

	Object getValue()
	{
		throw new UnsupportedOperationException();
	}

	void setValue(Object value)
	{
		throw new UnsupportedOperationException();
	}

	private AbstractNode _next;

	final AbstractNode getNext()
	{
		return _next;
	}

	final void setNext(AbstractNode next)
	{
		_next = next;
	}
}