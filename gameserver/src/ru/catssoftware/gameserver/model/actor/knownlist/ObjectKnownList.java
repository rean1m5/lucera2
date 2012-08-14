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
package ru.catssoftware.gameserver.model.actor.knownlist;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;

public abstract class ObjectKnownList
{
	private static final ObjectKnownList _instance = new ObjectKnownList() {
		@Override
		public boolean addKnownObject(L2Object object, L2Character dropper)
		{
			return false;
		}
		
		@Override
		public void removeAllKnownObjects()
		{
		}
		
		@Override
		public boolean removeKnownObject(L2Object object)
		{
			return false;
		}
		
		@Override
		public void tryAddObjects(L2Object[][] surroundingObjects)
		{
		}
		
		@Override
		public void tryRemoveObject(L2Object obj, boolean forced)
		{
		}
		
		@Override
		public void tryRemoveObjects(boolean forced)
		{
		}
	};
	
	public static ObjectKnownList getInstance()
	{
		return _instance;
	}
	
	ObjectKnownList()
	{
	}
	
	public final boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}
	
	public abstract boolean addKnownObject(L2Object object, L2Character dropper);
	
	public abstract void removeAllKnownObjects();
	
	public abstract boolean removeKnownObject(L2Object object);
	
	public abstract void tryAddObjects(L2Object[][] surroundingObjects);
	
	public abstract void tryRemoveObjects(boolean forced);
	
	public abstract void tryRemoveObject(L2Object obj, boolean forced);
}
