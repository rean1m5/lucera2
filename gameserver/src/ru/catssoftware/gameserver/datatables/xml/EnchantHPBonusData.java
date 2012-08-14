package ru.catssoftware.gameserver.datatables.xml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.skills.funcs.FuncTemplate;
import ru.catssoftware.gameserver.templates.item.L2Item;

import javolution.util.FastMap;

public class EnchantHPBonusData
{
	private static final Logger _log = Logger.getLogger(EnchantHPBonusData.class);

	private final FastMap<Integer,Integer[]> _singleArmorHPBonus = new FastMap<Integer, Integer[]>();
	private final FastMap<Integer,Integer[]> _fullArmorHPBonus = new FastMap<Integer, Integer[]>();
	private static EnchantHPBonusData _instance;
	
	public static final EnchantHPBonusData getInstance()
	{
		if (_instance == null)
			_instance = new EnchantHPBonusData();

		return _instance;
	}

	public EnchantHPBonusData()
	{
		if(!Config.ALT_ENCHANT_HP_BONUS)
			return;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(Config.DATAPACK_ROOT, "data/enchantHPBonus.xml");
		Document doc = null;

		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch(SAXException e)
			{
				e.printStackTrace();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			catch(ParserConfigurationException e)
			{
				e.printStackTrace();
			}

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("enchantHP".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							Integer grade;
							boolean fullArmor;

							att = attrs.getNamedItem("grade");
							if (att == null)
							{
								_log.warn("[EnchantHPBonusData] Missing grade, skipping");
								continue;
							}
							grade = Integer.parseInt(att.getNodeValue());

							att = attrs.getNamedItem("fullArmor");
							if (att == null)
							{
								_log.warn("[EnchantHPBonusData] Missing fullArmor, skipping");
								continue;
							}
							fullArmor = Boolean.valueOf(att.getNodeValue());

							att = attrs.getNamedItem("values");
							if (att == null)
							{
								_log.warn("[EnchantHPBonusData] Missing bonus id: "+grade+", skipping");
								continue;
							}
							StringTokenizer st = new StringTokenizer(att.getNodeValue(), ",");
							int tokenCount = st.countTokens();
							Integer[] bonus = new Integer[tokenCount];
							for (int i=0; i<tokenCount; i++)
							{
								Integer value = Integer.decode(st.nextToken().trim());
								if (value == null)
								{
									_log.warn("[EnchantHPBonusData] Bad Hp value!! grade: "+grade + " FullArmor? "+ fullArmor + " token: "+ i);
									value = 0;
								}
								bonus[i] = value;
							}
							if (fullArmor)
								_fullArmorHPBonus.put(grade, bonus);
							else
								_singleArmorHPBonus.put(grade, bonus);
						}
					}
				}
			}
			if (_fullArmorHPBonus.isEmpty() && _singleArmorHPBonus.isEmpty())
				return;

			Collection<Integer> itemIds = ItemTable.getInstance().getAllArmorsId();
			int count = 0;

			for (Integer itemId: itemIds)
			{
				L2Item item = ItemTable.getInstance().getTemplate(itemId);
				if (item != null && item.getCrystalType() != L2Item.CRYSTAL_NONE)
				{
					switch(item.getBodyPart())
					{
						case L2Item.SLOT_CHEST:
						case L2Item.SLOT_FEET:
						case L2Item.SLOT_GLOVES:
						case L2Item.SLOT_HEAD:
						case L2Item.SLOT_LEGS:
						case L2Item.SLOT_BACK:
						case L2Item.SLOT_FULL_ARMOR:
						case L2Item.SLOT_UNDERWEAR:
						case L2Item.SLOT_L_HAND:
							count++;
							FuncTemplate ft = new FuncTemplate(null, null, "Enchant", Stats.MAX_HP, 0x60, "0");
							item.attach(ft);
							break;
					}
				}
			}

			// shields in the weapons table
			itemIds = ItemTable.getInstance().getAllWeaponsId();
			for (Integer itemId: itemIds)
			{
				L2Item item = ItemTable.getInstance().getTemplate(itemId);
				if (item != null && item.getCrystalType() != L2Item.CRYSTAL_NONE)
				{
					switch(item.getBodyPart())
					{
						case L2Item.SLOT_L_HAND:
							count++;
							FuncTemplate ft = new FuncTemplate(null, null, "Enchant", Stats.MAX_HP, 0x60, "0");
							item.attach(ft);
							break;
					}
				}
			}
			_log.info("Enchant HP Bonus registered for " + count + " items!");
		}
	}

	public final int getHPBonus(L2Item item,int enchantLevel)
	{
		final Integer[] values;
		int itemGrade=item.getItemGrade();
		if (itemGrade==0x06)
			itemGrade=0x05;
			
		if (item.getBodyPart() == L2Item.SLOT_FULL_ARMOR)
			values = _fullArmorHPBonus.get(itemGrade);
		else
			values = _singleArmorHPBonus.get(itemGrade);

		if (values == null || values.length == 0)
			return 0;
			
		return values[Math.min(enchantLevel, values.length) - 1];
	}
}
