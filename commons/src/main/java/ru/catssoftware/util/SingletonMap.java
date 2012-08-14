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
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

/**
 * @author NB4L1
 */
public final class SingletonMap<K, V> implements Map<K, V>
{
	private FastMap<K, V> _map;
	private boolean _shared = false;

	private Map<K, V> get(boolean init)
	{
		if (_map == null)
		{
			if (init)
				_map = new FastMap<K, V>().setShared(_shared);
			else
				return L2Collections.emptyMap();
		}

		return _map;
	}

	public SingletonMap<K, V> setShared()
	{
		_shared = true;
		
		if (_map != null)
			_map.setShared(true);

		return this;
	}

	public void clear()
	{
		get(false).clear();
	}

	public boolean containsKey(Object key)
	{
		return get(false).containsKey(key);
	}

	public boolean containsValue(Object value)
	{
		return get(false).containsValue(value);
	}

	public Set<Entry<K, V>> entrySet()
	{
		return get(false).entrySet();
	}

	public V get(Object key)
	{
		return get(false).get(key);
	}

	public boolean isEmpty()
	{
		return get(false).isEmpty();
	}
	
	public Set<K> keySet()
	{
		return get(false).keySet();
	}

	public V put(K key, V value)
	{
		return get(true).put(key, value);
	}

	public void putAll(Map<? extends K, ? extends V> m)
	{
		get(true).putAll(m);
	}

	public V remove(Object key)
	{
		return get(false).remove(key);
	}

	public int size()
	{
		return get(false).size();
	}

	public Collection<V> values()
	{
		return get(false).values();
	}
}