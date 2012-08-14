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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * @author NB4L1
 */
public class HandlerRegistry<K, V>
{
	protected static final Logger _log = Logger.getLogger(HandlerRegistry.class);

	private final HashMap<K, V> _map = new HashMap<K, V>();

	public final void register(K key, V handler)
	{
		V old = _map.put(key, handler);

		if (old != null)
			_log.warn(getClass().getSimpleName() + ": Replaced type(" + key + "), " + old + " -> " + handler + ".");
	}

	public final void registerAll(V handler, K... keys)
	{
		for (K key : keys)
			register(key, handler);
	}

	public final V get(K key)
	{
		return _map.get(key);
	}

	public final int size()
	{
		return _map.size();
	}

	public Map<K, V> getHandlers()
	{
		return Collections.unmodifiableMap(_map);
	}
}