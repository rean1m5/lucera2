package ru.catssoftware.gameserver.util;

import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.tools.random.Rnd;

public class RndCoord {
	public static Location coordsRandomize(L2Object obj, int min, int max)
	{
		return obj.getLoc().rnd(min, max, false);
	}

	public static Location coordsRandomize(L2Object obj, int radius)
	{
		return coordsRandomize(obj, 0, radius);
	}

	public static Location coordsRandomize(int x, int y, int z, int heading, int radius_min, int radius_max)
	{
		if(radius_max == 0 || radius_max < radius_min)
			return new Location(x, y, z, heading);
		int radius = Rnd.get(radius_min, radius_max);
		double angle = Rnd.nextDouble() * 2 * Math.PI;
		return new Location((int) (x + radius * Math.cos(angle)), (int) (y + radius * Math.sin(angle)), z, heading);
	}

	public static Location coordsRandomize(Location pos, int radius_min, int radius_max)
	{
		return coordsRandomize(pos.getX(), pos.getY(), pos.getZ(), 0, radius_min, radius_max);
	}

}
