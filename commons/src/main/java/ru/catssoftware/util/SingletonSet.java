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

import java.util.Set;

/**
 * @author NB4L1
 */
public final class SingletonSet<E> extends SingletonCollection<E> implements Set<E>
{
	private L2FastSet<E> _set;
	private boolean _shared = false;

	@Override
	protected Set<E> get(boolean init)
	{
		if (_set == null)
		{
			if (init)
				_set = new L2FastSet<E>().setShared(_shared);
			else
				return L2Collections.emptySet();
		}

		return _set;
	}

	public SingletonSet<E> setShared()
	{
		_shared = true;

		if (_set != null)
			_set.setShared(true);

		return this;
	}
}