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
package ru.catssoftware.gameserver.model.actor.reference;

import java.lang.ref.WeakReference;

/**
 * @author NB4L1
 */
public class ImmutableReference<T>
{
	protected final WeakReference<T>	_ref;
	private Class<?>					_refClass;
	private final String				_name;

	public ImmutableReference(T referent)
	{
		_ref = new WeakReference<T>(referent);
		_refClass = referent.getClass();
		_name = referent + " @ " + Integer.toHexString(referent.hashCode());
	}

	public T get()
	{
		return _ref.get();
	}

	public String getName()
	{
		return _name;
	}

	public Class<?> getReferentClass()
	{
		return _refClass;
	}
}
