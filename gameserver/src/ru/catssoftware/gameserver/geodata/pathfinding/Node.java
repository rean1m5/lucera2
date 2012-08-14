package ru.catssoftware.gameserver.geodata.pathfinding;

/**
 * @author -Nemesiss-
 */
public class Node
{
	private final AbstractNodeLoc _loc;
	private final int _neighborsIdx;
	private Node[] _neighbors;
	private Node _parent;
	private short _cost;

	public Node(AbstractNodeLoc Loc, int Neighbors_idx)
	{
		_loc = Loc;
		_neighborsIdx = Neighbors_idx;
	}

	public void setParent(Node p)
	{
		_parent = p;
	}

	public void setCost(int cost)
	{
		_cost = (short)cost;
	}

	public void attachNeighbors()
	{
		if(_loc == null)
			_neighbors = null;
		else
			_neighbors = PathFinding.getInstance().readNeighbors(this, _neighborsIdx);
	}

	public Node[] getNeighbors()
	{
		return _neighbors;
	}

	public Node getParent()
	{
		return _parent;
	}

	public AbstractNodeLoc getLoc()
	{
		return _loc;
	}

	public float getCost()
	{
		return _cost;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0)
	{
		if(!(arg0 instanceof Node))
			return false;
		Node n = (Node)arg0;

		return _loc.getX() == n.getLoc().getX() && _loc.getY() == n.getLoc().getY() && _loc.getZ() == n.getLoc().getZ();
	}
}