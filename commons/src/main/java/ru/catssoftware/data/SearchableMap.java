package ru.catssoftware.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SearchableMap<K,V> extends Searchable<V>  implements Map<K,V>{
	private HashMap<K, V> _inner = new HashMap<K, V>();
	@Override
	public Iterator<V> iterator() {
		return _inner.values().iterator();
	}

	@Override
	public int size() {
		return _inner.size();
	}

	@Override
	public boolean isEmpty() {
		return _inner.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return _inner.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return _inner.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return _inner.get(key);
	}

	@Override
	public V put(K key, V value) {
		return _inner.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return _inner.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		_inner.putAll(m);
		
	}

	@Override
	public void clear() {
		_inner.clear();
	}

	@Override
	public Set<K> keySet() {
		return _inner.keySet();
	}

	@Override
	public Collection<V> values() {
		return _inner.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return _inner.entrySet();
	}

}
