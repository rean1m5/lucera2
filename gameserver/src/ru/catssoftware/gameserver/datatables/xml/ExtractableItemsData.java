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

/**
 * @author FBIagent
 */

package ru.catssoftware.gameserver.datatables.xml;

import java.io.File;

import javolution.util.FastList;
import javolution.util.FastMap;


import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.items.model.L2ExtractableItem;
import ru.catssoftware.gameserver.items.model.L2ExtractableProductItem;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.util.ArrayUtils;

public class ExtractableItemsData extends XMLDocument
{
	private final static Logger					_log		= Logger.getLogger(ExtractableItemsData.class.getName());

	private FastMap<Integer, L2ExtractableItem>     _items  = new FastMap<Integer, L2ExtractableItem>();

	private static ExtractableItemsData			_instance	= null;

	public static ExtractableItemsData getInstance()
	{
		if (_instance == null)
			_instance = new ExtractableItemsData();

		return _instance;
	}
   public static void reload() {
      _instance = new ExtractableItemsData();
   }
	public ExtractableItemsData()
	{
		_items.clear();
		File f = new File(Config.DATAPACK_ROOT, "data/extractable_items.xml");
		try {
			load(f);
			_log.info("Extractable items data: Loaded " + _items.size() + " extractable items!");
		} catch(Exception e) {
			_log.warn("Extractable items data: Can not load '" + Config.DATAPACK_ROOT + "data/extractable_items.xml'",e);
			return;
		}
		
	}

	public L2ExtractableItem getExtractableItem(int itemID)
	{
		return _items.get(itemID);
	}

	public int[] itemIDs()
	{
		int size = _items.size();
		int[] result = new int[size];
		int i = 0;
		for (L2ExtractableItem ei : _items.values())
			result[i++] = ei.getItemId();

		return result;
	}
	@Override
	protected void parseDocument(Document doc) {
		for(Node n = doc.getFirstChild();n!=null; n=n.getNextSibling()) {
			if(n.getNodeName().equals("list")) {
				for(Node node = n.getFirstChild();node!=null; node = node.getNextSibling()) {
					if(node.getNodeName().equals("item")) try {
						int itemId = Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue());
						FastList<L2ExtractableProductItem> product_temp = new FastList<L2ExtractableProductItem>();
						for(Node product = node.getFirstChild(); product != null; product=product.getNextSibling()) {
							if(product.getNodeName().equals("product")) {
								int skillId = Integer.parseInt(product.getAttributes().getNamedItem("skillId").getNodeValue());
								int skillLevel = Integer.parseInt(product.getAttributes().getNamedItem("skillLevel").getNodeValue());
								int chance =  Integer.parseInt(product.getAttributes().getNamedItem("chance").getNodeValue());
								L2Skill sk = SkillTable.getInstance().getInfo(skillId, skillLevel);
								if(sk==null) {
									_log.warn("Extractable items data: error loading item "+itemId);
									break;
								}
								Integer[] production = {};
								Integer[] amount = {};
								for(Node pitem = product.getFirstChild(); pitem!=null;pitem=pitem.getNextSibling()) {
									if(pitem.getNodeName().equals("item")) {
										production = ArrayUtils.add(production, Integer.parseInt(pitem.getAttributes().getNamedItem("id").getNodeValue()));
										amount = ArrayUtils.add(amount, Integer.parseInt(pitem.getAttributes().getNamedItem("count").getNodeValue()));
									}
								}
								L2ExtractableProductItem productItem = new L2ExtractableProductItem(production, amount, chance, sk);
								product_temp.add(productItem);
							}
							
						}
						L2ExtractableItem product = new L2ExtractableItem(itemId, product_temp);
						_items.put(itemId, product);
					} catch(Exception e ) {
						_log.warn("Extractable items data: error loading item",e);
					}
				}
			}
		}
		
	}
}