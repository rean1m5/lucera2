package ru.catssoftware.gameserver;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.Inventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.*;
import ru.catssoftware.gameserver.skills.Stats;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.random.Rnd;
import ru.catssoftware.util.StatsSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class RecipeController
{
	private final static Logger									_log			= Logger.getLogger(RecipeController.class.getName());

	private static RecipeController								_instance;
	private Map<Integer, L2RecipeList>							_lists;
	protected static final Map<L2PcInstance, RecipeItemMaker>	_activeMakers	= Collections.synchronizedMap(new WeakHashMap<L2PcInstance, RecipeItemMaker>());
	private static final String									RECIPES_FILE	= "recipes.xml";

	public static RecipeController getInstance()
	{
		return _instance == null ? _instance = new RecipeController() : _instance;
	}

	public RecipeController()
	{
		_lists = new FastMap<Integer, L2RecipeList>();

		try
		{
			loadFromXML();
			_log.info("RecipeController: Loaded " + _lists.size() + " recipes.");
		}
		catch (Exception e)
		{
			_log.fatal("Failed loading recipe list", e);
		}
	}

	public int getRecipesCount()
	{
		return _lists.size();
	}

	public L2RecipeList getRecipeList(int listId)
	{
		return _lists.get(listId);
	}

	public L2RecipeList getRecipeByItemId(int itemId)
	{
		for (L2RecipeList find : _lists.values())
		{
			if (find.getRecipeId() == itemId)
				return find;
		}
		return null;
	}

	public int[] getAllItemIds()
	{
		int[] idList = new int[_lists.size()];
		int i = 0;
		for (L2RecipeList rec : _lists.values())
			idList[i++] = rec.getRecipeId();

		return idList;
	}

	public synchronized void requestBookOpen(L2PcInstance player, boolean isDwarvenCraft)
	{
		RecipeItemMaker maker = null;
		if (player.isNormalCraftMode())
			maker = _activeMakers.get(player);

		if (maker == null)
		{
			RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
			response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
			player.sendPacket(response);
			return;
		}

		player.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
	}

	public synchronized void requestMakeItemAbort(L2PcInstance player)
	{
		_activeMakers.remove(player); // TODO:  anything else here?
	}

	public synchronized void requestManufactureItem(L2PcInstance manufacturer, int recipeListId, L2PcInstance player)
	{
		L2RecipeList recipeList = getValidRecipeList(player, recipeListId);

		if (recipeList == null)
			return;

		List<L2RecipeList> dwarfRecipes = Arrays.asList(manufacturer.getDwarvenRecipeBook());
		List<L2RecipeList> commonRecipes = Arrays.asList(manufacturer.getCommonRecipeBook());

		if (!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
			return;
		}

		RecipeItemMaker maker;

		if (manufacturer.isNormalCraftMode() && (maker = _activeMakers.get(manufacturer)) != null) // check if busy
		{
			player.sendMessage("Manufacturer is busy, please try later.");
			return;
		}

		maker = new RecipeItemMaker(manufacturer, recipeList, player);
		if (maker._isValid)
		{
			if (manufacturer.isNormalCraftMode())
			{
				_activeMakers.put(manufacturer, maker);
				ThreadPoolManager.getInstance().scheduleGeneral(maker, 100);
			}
			else
				maker.run();
		}
	}

	public synchronized void requestMakeItem(L2PcInstance player, int recipeListId)
	{
		if (player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_CRAFT_DURING_COMBAT);
			return;
		}

		L2RecipeList recipeList = getValidRecipeList(player, recipeListId);

		if (recipeList == null)
			return;

		List<L2RecipeList> dwarfRecipes = Arrays.asList(player.getDwarvenRecipeBook());
		List<L2RecipeList> commonRecipes = Arrays.asList(player.getCommonRecipeBook());

		if (!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
			return;
		}

		RecipeItemMaker maker;

		// check if already busy (possible in alt mode only)
		if (player.isNormalCraftMode() && ((maker = _activeMakers.get(player)) != null))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1);
			sm.addItemName(recipeList.getItemId());
			sm.addString("You are busy creating");
			player.sendPacket(sm);
			return;
		}

		maker = new RecipeItemMaker(player, recipeList, player);
		if (maker._isValid)
		{
			if (player.isNormalCraftMode())
			{
				_activeMakers.put(player, maker);
				ThreadPoolManager.getInstance().scheduleGeneral(maker, 100);
			}
			else
				maker.run();
		}
	}

	private void loadFromXML() throws SAXException, IOException, ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(Config.DATAPACK_ROOT, "data/" + RECIPES_FILE);
		if (file.exists())
		{
			Document doc = factory.newDocumentBuilder().parse(file);
			List<L2RecipeInstance> recipePartList = new FastList<L2RecipeInstance>();
			List<L2RecipeStatInstance> recipeStatUseList = new FastList<L2RecipeStatInstance>();
			List<L2RecipeStatInstance> recipeAltStatChangeList = new FastList<L2RecipeStatInstance>();

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					recipesFile: for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							recipePartList.clear();
							recipeStatUseList.clear();
							recipeAltStatChangeList.clear();
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							int id = -1;
							StatsSet set = new StatsSet();

							att = attrs.getNamedItem("id");
							if (att == null)
							{
								_log.fatal("Missing id for recipe item, skipping");
								continue;
							}
							id = Integer.parseInt(att.getNodeValue());
							set.set("id", id);

							att = attrs.getNamedItem("recipeId");
							if (att == null)
							{
								_log.fatal("Missing recipeId for recipe item id: " + id + ", skipping");
								continue;
							}
							set.set("recipeId", Integer.parseInt(att.getNodeValue()));

							att = attrs.getNamedItem("name");
							if (att == null)
							{
								_log.fatal("Missing name for recipe item id: " + id + ", skipping");
								continue;
							}
							set.set("recipeName", att.getNodeValue());

							att = attrs.getNamedItem("craftLevel");
							if (att == null)
							{
								_log.fatal("Missing level for recipe item id: " + id + ", skipping");
								continue;
							}
							set.set("craftLevel", Integer.parseInt(att.getNodeValue()));

							att = attrs.getNamedItem("type");
							if (att == null)
							{
								_log.fatal("Missing type for recipe item id: " + id + ", skipping");
								continue;
							}
							set.set("isDwarvenRecipe", att.getNodeValue().equalsIgnoreCase("dwarven"));

							att = attrs.getNamedItem("successRate");
							if (att == null)
							{
								_log.fatal("Missing successRate for recipe item id: " + id + ", skipping");
								continue;
							}
							set.set("successRate", Integer.parseInt(att.getNodeValue()));

							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if ("statUse".equalsIgnoreCase(c.getNodeName()))
								{
									String statName = c.getAttributes().getNamedItem("name").getNodeValue();
									int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());
									try
									{
										recipeStatUseList.add(new L2RecipeStatInstance(statName, value));
									}
									catch (Exception e)
									{
										_log.fatal("Error in StatUse parameter for recipe item id: " + id + ", skipping");
										continue recipesFile;
									}
								}
								else if ("altStatChange".equalsIgnoreCase(c.getNodeName()))
								{
									String statName = c.getAttributes().getNamedItem("name").getNodeValue();
									int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());
									try
									{
										recipeAltStatChangeList.add(new L2RecipeStatInstance(statName, value));
									}
									catch (Exception e)
									{
										_log.fatal("Error in AltStatChange parameter for recipe item id: " + id + ", skipping");
										continue recipesFile;
									}
								}
								else if ("ingredient".equalsIgnoreCase(c.getNodeName()))
								{
									int ingId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
									int ingCount = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
									recipePartList.add(new L2RecipeInstance(ingId, ingCount));
								}
								else if ("production".equalsIgnoreCase(c.getNodeName()))
								{
									set.set("itemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
									set.set("count", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
								}
							}

							L2RecipeList recipeList = new L2RecipeList(set);
							for (L2RecipeInstance recipePart : recipePartList)
								recipeList.addRecipe(recipePart);
							for (L2RecipeStatInstance recipeStatUse : recipeStatUseList)
								recipeList.addStatUse(recipeStatUse);
							for (L2RecipeStatInstance recipeAltStatChange : recipeAltStatChangeList)
								recipeList.addAltStatChange(recipeAltStatChange);

							_lists.put(id, recipeList);
						}
					}
				}
			}
		}
		else
			_log.fatal("Recipes file (" + file.getAbsolutePath() + ") doesnt exists.");
	}

	private class RecipeItemMaker implements Runnable
	{
		protected boolean				_isValid;
		protected List<TempItem>		_items	= null;
		protected final L2RecipeList	_recipeList;
		protected final L2PcInstance	_player;			// "crafter"
		protected final L2PcInstance	_target;			// "customer"
		protected final L2Skill			_skill;
		protected final int				_skillId;
		protected final int				_skillLevel;
		protected int					_creationPasses = 1;
		protected int					_itemGrab;
		protected int					_exp = -1;
		protected int					_sp = -1;
		protected int					_price;
		protected int					_totalItems;
		@SuppressWarnings("unused")
		protected int					_materialsRefPrice;
		protected int					_delay;

		public RecipeItemMaker(L2PcInstance pPlayer, L2RecipeList pRecipeList, L2PcInstance pTarget)
		{
			_player = pPlayer;
			_target = pTarget;
			_recipeList = pRecipeList;

			_isValid = false;
			_skillId = _recipeList.isDwarvenRecipe() ? L2Skill.SKILL_CREATE_DWARVEN : L2Skill.SKILL_CREATE_COMMON;
			_skillLevel = _player.getSkillLevel(_skillId);
			_skill = _player.getKnownSkill(_skillId);

			_player.isInCraftMode(true);

			if (_player.isAlikeDead())
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if (_target.isAlikeDead())
			{
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if (_target.isProcessingTransaction())
			{
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if (_player.isProcessingTransaction())
			{
				if (_player != _target)
					_target.sendMessage("Manufacturer " + _player.getName() + " is busy.");

				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			// validate recipe list
			if (_recipeList.getRecipes().length == 0)
			{
				_player.sendMessage("No such recipe");
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			// validate skill level
			if (_recipeList.getLevel() > _skillLevel)
			{
				_player.sendMessage("Need skill level " + _recipeList.getLevel());
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			// check that customer can afford to pay for creation services
			if (_player != _target)
			{
				for (L2ManufactureItem temp : _player.getCreateList().getList())
				{
					if (temp.getRecipeId() == _recipeList.getId()) // find recipe for item we want manufactured
					{
						_price = temp.getCost();
						if (_target.getAdena() < _price) // check price
						{
							_target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
							abort();
							return;
						}
						break;
					}
				}
			}

			// make temporary items
			if ((_items = listItems(false)) == null)
			{
				abort();
				return;
			}

			// calculate reference price
			for (TempItem i : _items)
			{
				_materialsRefPrice += i.getReferencePrice() * i.getQuantity();
				_totalItems += i.getQuantity();
			}

			// initial statUse checks
			if (!calculateStatUse(false, false))
			{
				abort();
				return;
			}

			// initial AltStatChange checks
			if (_player.isNormalCraftMode())
				calculateAltStatChange();

			updateMakeInfo(true);
			updateCurMp();
			updateCurLoad();

			_player.isInCraftMode(false);
			_isValid = true;
		}

		@Override
		public void run()
		{
			if (!Config.IS_CRAFTING_ENABLED)
			{
				_target.sendMessage("Создание вещей отключено.");
				abort();
				return;
			}

			if (_player == null || _target == null)
			{
				_log.warn("player or target == null (disconnected?), aborting" + _target + _player);
				abort();
				return;
			}

			if (_player.isOnline() == 0 || _target.isOnline() == 0)
			{
				_log.warn("player or target is not online, aborting " + _target + _player);
				abort();
				return;
			}

			if (_player.isNormalCraftMode() && _activeMakers.get(_player) == null)
			{
				if (_target != _player)
				{
					_target.sendMessage("Мануфактура прервана.");
					_player.sendMessage("Мануфактура прервана.");
				}
				else
					_player.sendMessage("Создание вещи прервано.");

				abort();
				return;
			}

			if (_player.isNormalCraftMode()  && !_items.isEmpty())
			{
				if (!calculateStatUse(true, true))
					return; // check stat use
				updateCurMp(); // update craft window mp bar

				grabSomeItems(); // grab (equip) some more items with a nice msg to player

				// if still not empty, schedule another pass
				if (!_items.isEmpty())
				{
					// divided by RATE_CONSUMABLES_COST to remove craft time increase on higher consumables rates
					_delay = (int) (Config.ALT_GAME_CREATION_SPEED * _player.getStat().getMReuseRate(_skill) * GameTimeController.TICKS_PER_SECOND / Config.RATE_CONSUMABLE_COST)
							* GameTimeController.MILLIS_IN_TICK;

					// FIXME: please fix this packet to show crafting animation (somebody)
					MagicSkillUse msk = new MagicSkillUse(_player, _skillId, _skillLevel, _delay, 0, false);
					_player.broadcastPacket(msk);

					_player.sendPacket(new SetupGauge(0, _delay));
					ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
				}
				else
				{
					// for alt mode, sleep delay msec before finishing
					_player.sendPacket(new SetupGauge(0, _delay));

					ThreadPoolManager.getInstance().schedule(new Runnable() {
						@Override
						public void run()
						{
							finishCrafting();
						}
					}, _delay);

				}
			} // for old craft mode just finish
			else
				finishCrafting();
		}

		private void finishCrafting()
		{
			if (!Config.ALT_GAME_CREATION)
				calculateStatUse(false, true);

			// first take adena for manufacture
			if ((_target != _player) && _price > 0) // customer must pay for services
			{
				// attempt to pay for item
				L2ItemInstance adenatransfer = _target.transferItem("PayManufacture", _target.getInventory().getAdenaInstance().getObjectId(), _price, _player
						.getInventory(), _player);

				if (adenatransfer == null)
				{
					_target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
					abort();
					return;
				}
			}

			boolean success = false;
			if ((_items = listItems(true)) == null) // this line actually takes materials from inventory
			{ // handle possible cheaters here
				// (they click craft then try to get rid of items in order to get free craft)
			}
			else if (_recipeList.getSuccessRate() == 100 || Rnd.get(100) < _recipeList.getSuccessRate())
			{
				rewardPlayer(_price); // and immediately puts created item in its place
				success = true;
			}
			else
			{
				if (_target != _player)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.CREATION_OF_S2_FOR_S1_AT_S3_ADENA_FAILED);
					msg.addString(_target.getName());
					msg.addItemName(_recipeList.getItemId());
					msg.addNumber(_price);
					_player.sendPacket(msg);
					msg = new SystemMessage(SystemMessageId.S1_FAILED_TO_CREATE_S2_FOR_S3_ADENA);
					msg.addString(_player.getName());
					msg.addItemName(_recipeList.getItemId());
					msg.addNumber(_price);
					_target.sendPacket(msg);
				}
				else
					_target.sendPacket(SystemMessageId.ITEM_MIXING_FAILED);
			}
			// update load and mana bar of craft window
			updateCurMp();
			updateCurLoad();
			_activeMakers.remove(_player);
			_player.isInCraftMode(false);
			_target.sendPacket(new ItemList(_target, false));
			updateMakeInfo(success);
		}

		private void updateMakeInfo(boolean success)
		{
			if (_target == _player)
				_target.sendPacket(new RecipeItemMakeInfo(_recipeList.getId(), _target, success));
			else
				_target.sendPacket(new RecipeShopItemInfo(_player, _recipeList.getId()));
		}

		private void updateCurLoad()
		{
			StatusUpdate su = new StatusUpdate(_target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, _target.getCurrentLoad());
			_target.sendPacket(su);
		}

		private void updateCurMp()
		{
			StatusUpdate su = new StatusUpdate(_target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_MP, (int) _target.getStatus().getCurrentMp());
			_target.sendPacket(su);
		}

		private void grabSomeItems()
		{
			int grabItems = _itemGrab;
			while (grabItems > 0 && !_items.isEmpty())
			{
				TempItem item = _items.get(0);

				int count = item.getQuantity();
				if (count >= grabItems)
					count = grabItems;

				item.setQuantity(item.getQuantity() - count);
				if (item.getQuantity() <= 0)
					_items.remove(0);
				else
					_items.set(0, item);

				grabItems -= count;

				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED); // you equipped ...
				sm.addNumber(count);
				sm.addItemName(item.getItemId());
				_player.sendPacket(sm);
				if (_target != _player)
					_target.sendMessage("Manufacturer " + _player.getName() + " used " + count + " " + item.getItemName());
			}
		}

		// AltStatChange parameters make their effect here
		private void calculateAltStatChange()
		{
			_itemGrab = _skillLevel;

			for (L2RecipeStatInstance altStatChange : _recipeList.getAltStatChange())
			{
				if (altStatChange.getType() == L2RecipeStatInstance.statType.XP)
					_exp = altStatChange.getValue();
				else if (altStatChange.getType() == L2RecipeStatInstance.statType.SP)
					_sp = altStatChange.getValue();
				else if (altStatChange.getType() == L2RecipeStatInstance.statType.GIM)
					_itemGrab *= altStatChange.getValue();
			}
			// determine number of creation passes needed
			_creationPasses = (_totalItems / _itemGrab) + ((_totalItems % _itemGrab) != 0 ? 1 : 0);
			if (_creationPasses < 1)
				_creationPasses = 1;
		}

		// StatUse
		private boolean calculateStatUse(boolean isWait, boolean isReduce)
		{
			boolean ret = true;
			for (L2RecipeStatInstance statUse : _recipeList.getStatUse())
			{
				double modifiedValue = statUse.getValue() / _creationPasses;
				if (statUse.getType() == L2RecipeStatInstance.statType.HP)
				{
					// we do not want to kill the player, so its CurrentHP must be greater than the reduce value
					if (_player.getStatus().getCurrentHp() <= modifiedValue)
					{
						// rest (wait for HP)
						if (Config.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(0, _delay));
							ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
						}
						else // no rest - report no hp
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
							abort();
						}
						ret = false;
					}
					else if (isReduce)
						_player.reduceCurrentHp(modifiedValue, _player, true, false, null);
				}
				else if (statUse.getType() == L2RecipeStatInstance.statType.MP)
				{
					if (_player.getStatus().getCurrentMp() < modifiedValue)
					{
						// rest (wait for MP)
						if (Config.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(0, _delay));
							ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
						}
						else // no rest - report no mana
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							abort();
						}
						ret = false;
					}
					else if (isReduce)
						_player.reduceCurrentMp(modifiedValue);
				}
				else
				{
					// there is an unknown StatUse value
					_target.sendMessage("Ошибка рецепта, сообщите администратору.");
					ret = false;
					abort();
				}
			}
			return ret;
		}

		private List<TempItem> listItems(boolean remove)
		{
			L2RecipeInstance[] recipes = _recipeList.getRecipes();
			Inventory inv = _target.getInventory();
			List<TempItem> materials = new FastList<TempItem>();

			for (L2RecipeInstance recipe : recipes)
			{
				int quantity = _recipeList.isConsumable() ? (int) (recipe.getQuantity() * Config.RATE_CONSUMABLE_COST) : recipe.getQuantity();

				if (quantity > 0)
				{
					L2ItemInstance item = inv.getItemByItemId(recipe.getItemId());
					int itemQuantityAmount = item == null ? 0 : item.getCount();
	
					// check materials
					if (itemQuantityAmount < quantity)
					{
						SystemMessage msg = new SystemMessage(SystemMessageId.MISSING_S2_S1_TO_CREATE);
						msg.addItemName(recipe.getItemId());
						msg.addNumber(quantity - itemQuantityAmount);
						_target.sendPacket(msg);

						abort();
						return null;
					}

					// make new temporary object, just for counting purposes

					TempItem temp = new TempItem(item, quantity);
					materials.add(temp);
				}
			}

			if (remove)
			{
				for (TempItem tmp : materials)
				{
					inv.destroyItemByItemId("Manufacture", tmp.getItemId(), tmp.getQuantity(), _target, _player);
					SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					msg.addItemName(tmp.getItemId());
					msg.addNumber(tmp.getQuantity());
					_target.sendPacket(msg);
				}
			}
			return materials;
		}

		private void abort()
		{
			updateMakeInfo(false);
			_player.isInCraftMode(false);
			_activeMakers.remove(_player);
		}

		/**
		 * FIXME: This class should be in some other file, but I don't know where
		 *
		 * Class explanation:
		 * For item counting or checking purposes. When you don't want to modify inventory
		 * class contains itemId, quantity, ownerId, referencePrice, but not objectId
		 */
		private class TempItem
		{ // no object id stored, this will be only "list" of items with it's owner
			private int		_itemId;
			private int		_quantity;
			private int		_ownerId;
			private int		_referencePrice;
			private String	_itemName;

			/**
			 * @param item
			 * @param quantity of that item
			 */
			public TempItem(L2ItemInstance item, int quantity)
			{
				super();
				_itemId = item.getItemId();
				_quantity = quantity;
				_ownerId = item.getOwnerId();
				_itemName = item.getItem().getName();
				_referencePrice = item.getReferencePrice();
			}

			/**
			 * @return Returns the quantity.
			 */
			public int getQuantity()
			{
				return _quantity;
			}

			/**
			 * @param quantity The quantity to set.
			 */
			public void setQuantity(int quantity)
			{
				_quantity = quantity;
			}

			public int getReferencePrice()
			{
				return _referencePrice;
			}

			/**
			 * @return Returns the itemId.
			 */
			public int getItemId()
			{
				return _itemId;
			}

			@SuppressWarnings("unused")
			public int getOwnerId()
			{
				return _ownerId;
			}

			/**
			 * @return Returns the itemName.
			 */
			public String getItemName()
			{
				return _itemName;
			}
		}

		private void rewardPlayer(int price)
		{
			int itemId = _recipeList.getItemId();
			int itemCount = _recipeList.getCount();

			L2Item template = ItemTable.getInstance().getTemplate(itemId);

			_target.getInventory().addItem("Manufacture", itemId, itemCount, _target, _player);

			SystemMessage msg;
			if (_target != _player)
			{
				// inform manufacturer of earned profit
				if (itemCount == 1)
				{
					msg = new SystemMessage(SystemMessageId.S2_CREATED_FOR_S1_FOR_S3_ADENA);
					msg.addString(_target.getName());
					msg.addItemName(itemId);
					msg.addNumber(price);
					_player.sendPacket(msg);
					msg = new SystemMessage(SystemMessageId.S1_CREATED_S2_FOR_S3_ADENA);
					msg.addString(_player.getName());
					msg.addItemName(itemId);
					msg.addNumber(price);
					_target.sendPacket(msg);
				}
				else
				{
					msg = new SystemMessage(SystemMessageId.S2_S3_S_CREATED_FOR_S1_FOR_S4_ADENA);
					msg.addString(_target.getName());
					msg.addNumber(itemCount);
					msg.addItemName(itemId);
					msg.addNumber(price);
					_player.sendPacket(msg);
					msg = new SystemMessage(SystemMessageId.S1_CREATED_S2_S3_S_FOR_S4_ADENA);
					msg.addString(_player.getName());
					msg.addNumber(itemCount);
					msg.addItemName(itemId);
					msg.addNumber(price);
					_target.sendPacket(msg);
				}
			}

			// inform customer of earned item

			if (itemCount > 1)
			{
				msg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				msg.addItemName(itemId);
				msg.addNumber(itemCount);
				_target.sendPacket(msg);
			}
			else
			{
				msg = new SystemMessage(SystemMessageId.EARNED_S1);
				msg.addItemName(itemId);
				_target.sendPacket(msg);
			}

			if (Config.ALT_GAME_CREATION)
			{
				int recipeLevel = _recipeList.getLevel();
				if (_exp < 0)
				{
					_exp = template.getReferencePrice() * itemCount;
					_exp /= recipeLevel;
				}
				if (_sp < 0)
					_sp = _exp / 10;
				// one variation

				// exp -= materialsRefPrice;   // mat. ref. price is not accurate so other method is better

				if (_exp < 0)
					_exp = 0;
				if (_sp < 0)
					_sp = 0;

				for (int i = _skillLevel; i > recipeLevel; i--)
				{
					_exp /= 4;
					_sp /= 4;
				}

				// Added multiplication of Creation speed with XP/SP gain
				// slower crafting -> more XP,  faster crafting -> less XP
				// you can use ALT_GAME_CREATION_XP_RATE/SP to
				// modify XP/SP gained (default = 1)

				_player.addExpAndSp((int) _player.calcStat(Stats.EXPSP_RATE, _exp * Config.ALT_GAME_CREATION_XP_RATE * Config.ALT_GAME_CREATION_SPEED, null,
						null), (int) _player.calcStat(Stats.EXPSP_RATE, _sp * Config.ALT_GAME_CREATION_SP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null));
			}
			updateMakeInfo(true); // success
		}
	}

	private L2RecipeList getValidRecipeList(L2PcInstance player, int id)
	{
		L2RecipeList recipeList = getRecipeList(id);

		if ((recipeList == null) || (recipeList.getRecipes().length == 0))
		{
			player.sendMessage("Нет рецепта для ID: " + id);
			player.isInCraftMode(false);
			return null;
		}
		return recipeList;
	}
}