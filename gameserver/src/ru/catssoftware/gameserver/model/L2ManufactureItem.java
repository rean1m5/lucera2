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
package ru.catssoftware.gameserver.model;

import ru.catssoftware.gameserver.RecipeController;

public class L2ManufactureItem
{
	private int		_recipeId;
	private int		_cost;
	private boolean	_isDwarven;

	public L2ManufactureItem(int recipeId, int cost)
	{
		_recipeId = recipeId;
		_cost = cost;

		_isDwarven = RecipeController.getInstance().getRecipeList(_recipeId).isDwarvenRecipe();
	}

	public int getRecipeId()
	{
		return _recipeId;
	}

	public int getCost()
	{
		return _cost;
	}

	public boolean isDwarven()
	{
		return _isDwarven;
	}
}