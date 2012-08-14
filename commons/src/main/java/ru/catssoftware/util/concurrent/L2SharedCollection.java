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

import java.util.Iterator;

import ru.catssoftware.lang.L2Entity;


/**
 * @author NB4L1
 */
public final class L2SharedCollection<T extends L2Entity> extends L2Collection<T> implements Iterable<T>, Executor<T>
{
	@Override
	public Iterator<T> iterator()
	{
		return super.iterator();
	}

	@Override
	public void executeForEach(Executable<T> executable)
	{
		super.executeForEach(executable);
	}
}