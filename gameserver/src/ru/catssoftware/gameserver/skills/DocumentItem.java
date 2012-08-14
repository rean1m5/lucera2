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
package ru.catssoftware.gameserver.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import ru.catssoftware.gameserver.items.model.Item;
import ru.catssoftware.gameserver.skills.conditions.Condition;
import ru.catssoftware.gameserver.templates.item.L2Armor;
import ru.catssoftware.gameserver.templates.item.L2ArmorType;
import ru.catssoftware.gameserver.templates.item.L2EtcItem;
import ru.catssoftware.gameserver.templates.item.L2EtcItemType;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;


/**
 * @author mkizub
 */
final class DocumentItem extends DocumentBase
{
	private final Map<Integer, Item> _itemData;
	
	private final List<L2Item> _itemsInFile = new ArrayList<L2Item>();
	
	DocumentItem(Map<Integer, Item> itemData, File file)
	{
		super(file);
		
		_itemData = itemData;
	}
	
	List<L2Item> getItemList()
	{
		return _itemsInFile;
	}
	
	@Override
	String getDefaultNodeName()
	{
		return "item";
	}
	
	@Override
	void parseDefaultNode(Node n)
	{
		int itemId = 0;
		
		try
		{
			itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
			
			final Item currentItem = _itemData.get(itemId);
			
			if (currentItem == null) {
				return;
			}
			
			L2Item item;
			
			if (currentItem.type instanceof L2ArmorType)
				item = new L2Armor((L2ArmorType)currentItem.type, currentItem.set);
			
			else if (currentItem.type instanceof L2WeaponType)
				item = new L2Weapon((L2WeaponType)currentItem.type, currentItem.set);
			
			else if (currentItem.type instanceof L2EtcItemType)
				item = new L2EtcItem((L2EtcItemType)currentItem.type, currentItem.set);
			
			else
				throw new IllegalStateException("Unknown item type " + currentItem.type);
			
			for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("cond".equalsIgnoreCase(n.getNodeName()))
				{
					item.attach(parseConditionWithMessage(n, item));
				}
				else if ("for".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, item);
				}
				else if (n.getNodeType() == Node.ELEMENT_NODE)
				{
					throw new IllegalStateException("Invalid tag <" + n.getNodeName() + ">");
				}
			}
			
			_itemsInFile.add(item);
		}
		catch (RuntimeException e)
		{
			_log.warn("Error while parsing item id " + itemId + " in file " + _file, e);
		}
	}
	
	@Override
	void parseTemplateNode(Node n, Object template, Condition condition)
	{
		if ("enchant".equalsIgnoreCase(n.getNodeName()))
			attachFunc(n, template, "Enchant", condition);
		else
			super.parseTemplateNode(n, template, condition);
	}
}
