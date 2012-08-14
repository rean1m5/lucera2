package ru.catssoftware.gameserver.datatables.xml;


import org.w3c.dom.Node;

import ru.catssoftware.gameserver.datatables.ClassTreeTable;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.util.StatsSet;

public class NpcLikePcTemplate 
{
	private int [] _inventory = new int[Inventory.PAPERDOLL_TOTALSLOTS];
	private int [] _enchant = new int[Inventory.PAPERDOLL_TOTALSLOTS];
	private StatsSet _stats = new StatsSet();
	private Race _race = Race.Human;
	private ClassId _class = ClassId.fighter;
	public NpcLikePcTemplate(Node node) {
		_race = Race.valueOf(node.getAttributes().getNamedItem("race").getNodeValue());
		if(node.getAttributes().getNamedItem("class")!=null)
			_class = ClassTreeTable.getInstance().getClassId(node.getAttributes().getNamedItem("class").getNodeValue());
		for(Node n = node.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("equip")) 
				loadInventory(n);
			else if(n.getNodeName().equals("set")) {
				_stats.set(n.getAttributes().getNamedItem("name").getNodeValue(), n.getAttributes().getNamedItem("value").getNodeValue());
			}
			
		}
	}
	private void loadInventory(Node node) {
		for(Node n = node.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("item")) try {
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				L2Item item = ItemTable.getInstance().getTemplate(id);
				int slot = getSlotForItem(item);
				_inventory[slot] = item.getItemId();
				if(n.getAttributes().getNamedItem("enchant")!=null)
					_enchant[slot] = Integer.parseInt(n.getAttributes().getNamedItem("enchant").getNodeValue());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	public int [] getInventory() {
		return _inventory;
	}
	public int [] getEnchant() {
		return _enchant;
	}
	
	public ClassId getClassId() {
		return _class;
	}
	public StatsSet getStats() {
		return _stats;
	}
	public Race getRace() {
		return _race;
	}
	public static int getSlotForItem(L2Item item) {
		int pdollSlot =-1;
		switch (item.getBodyPart())
		{
			case L2Item.SLOT_L_EAR:
				pdollSlot = Inventory.PAPERDOLL_LEAR;
				break;
			case L2Item.SLOT_R_EAR:
				pdollSlot = Inventory.PAPERDOLL_REAR;
				break;
			case L2Item.SLOT_NECK:
				pdollSlot = Inventory.PAPERDOLL_NECK;
				break;
			case L2Item.SLOT_R_FINGER:
				pdollSlot = Inventory.PAPERDOLL_RFINGER;
				break;
			case L2Item.SLOT_L_FINGER:
				pdollSlot = Inventory.PAPERDOLL_LFINGER;
				break;
			case L2Item.SLOT_HAIR:
				pdollSlot = Inventory.PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_FACE:
				pdollSlot = Inventory.PAPERDOLL_FACE;
				break;
			case L2Item.SLOT_HAIRALL:
				pdollSlot = Inventory.PAPERDOLL_HAIRALL;
				break;
			case L2Item.SLOT_HEAD:
				pdollSlot = Inventory.PAPERDOLL_HEAD;
				break;
			case L2Item.SLOT_R_HAND:
				pdollSlot = Inventory.PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_HAND:
				pdollSlot = Inventory.PAPERDOLL_LHAND;
				break;
			case L2Item.SLOT_GLOVES:
				pdollSlot = Inventory.PAPERDOLL_GLOVES;
				break;
			case L2Item.SLOT_LEGS:
				pdollSlot = Inventory.PAPERDOLL_LEGS;
				break;
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_ALLDRESS:
				pdollSlot = Inventory.PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_BACK:
				pdollSlot = Inventory.PAPERDOLL_BACK;
				break;
			case L2Item.SLOT_FEET:
				pdollSlot = Inventory.PAPERDOLL_FEET;
				break;
			case L2Item.SLOT_UNDERWEAR:
				pdollSlot = Inventory.PAPERDOLL_UNDER;
				break;
			case L2Item.SLOT_LR_HAND:
				pdollSlot = Inventory.PAPERDOLL_LRHAND;
				break;
		}
		return  pdollSlot;
		
	}
	
}
