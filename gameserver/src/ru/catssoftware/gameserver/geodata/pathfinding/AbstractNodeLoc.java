package ru.catssoftware.gameserver.geodata.pathfinding;

/**
 * @author -Nemesiss-
 */
public abstract class AbstractNodeLoc
{
	public abstract int getX();
	public abstract int getY();
	public abstract short getZ();
	public abstract void setZ(short z);
	public abstract int getNodeX();
	public abstract int getNodeY();
}