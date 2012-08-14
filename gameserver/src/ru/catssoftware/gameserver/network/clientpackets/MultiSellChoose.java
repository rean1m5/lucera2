package ru.catssoftware.gameserver.network.clientpackets;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.model.L2Augmentation;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Multisell;
import ru.catssoftware.gameserver.model.L2Multisell.MultiSellEntry;
import ru.catssoftware.gameserver.model.L2Multisell.MultiSellIngredient;
import ru.catssoftware.gameserver.model.L2Multisell.MultiSellListContainer;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.itemcontainer.PcInventory;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExPCCafePointInfo;
import ru.catssoftware.gameserver.network.serverpackets.ItemList;
import ru.catssoftware.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;
import ru.catssoftware.gameserver.templates.item.L2Armor;
import ru.catssoftware.gameserver.templates.item.L2Item;
import ru.catssoftware.gameserver.templates.item.L2Weapon;
import javolution.util.FastList;

public class MultiSellChoose extends L2GameClientPacket
{
	private static final String	_C__A7_MULTISELLCHOOSE	= "[C] A7 MultiSellChoose";
	private int					_listId, _entryId, _amount, _enchantment, _transactionTax;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readD();
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0; 
	}

	@Override
	public void runImpl()
	{
		if (_amount < 1 || _amount > 5000)
			return;
		
		MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);
		if (list == null)
			return;

		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if(player.isProcessingTransaction()) {
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}

		for (MultiSellEntry entry : list.getEntries())
		{
			if (entry.getEntryId() == _entryId)
			{
				doExchange(player, entry, list.getApplyTaxes(), list.getMaintainEnchantment(),list.getMaintainEnchantmentLvl(), _enchantment,_listId==player._bbsMultisell);
				return;
			}
		}
	}

	private void doExchange(L2PcInstance player, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment,int maintainEnchantmentLvl, int enchantment, boolean GMShop)
	{
		PcInventory inv = player.getInventory();
		L2NpcInstance merchant = (player.getTarget() instanceof L2NpcInstance) ? (L2NpcInstance) player.getTarget() : null;
		if (merchant == null && !GMShop)
			return;

		if(!GMShop && !merchant.isInsideRadius(player, 250, false, false))
			return; 
		
		MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment);
		int cnt  = 0;
		for(MultiSellIngredient e : entry.getProducts())
			if(!ItemTable.getInstance().getTemplate(e.getItemId()).isStackable())
				cnt += (e.getItemCount() * _amount);
		if(player.getInventory().getSize() +  cnt >= player.getInventoryLimit()) {
			player.sendPacket(SystemMessageId.SLOTS_FULL.getSystemMessage());
			return;
		}
		
		FastList<MultiSellIngredient> _ingredientsList = new FastList<MultiSellIngredient>();
		boolean newIng = true;
		for (MultiSellIngredient e : entry.getIngredients())
		{
			newIng = true;

			for (MultiSellIngredient ex : _ingredientsList)
			{
				if ((ex.getItemId() == e.getItemId()) && (ex.getEnchantmentLevel() == e.getEnchantmentLevel()))
				{
					if ((long) ex.getItemCount() + e.getItemCount() >= Integer.MAX_VALUE)
					{
						player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			if (newIng)
				_ingredientsList.add(L2Multisell.getInstance().new MultiSellIngredient(e));
		}
		for (MultiSellIngredient e : _ingredientsList)
		{
			
			if ((double) e.getItemCount() * _amount >= Integer.MAX_VALUE)
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				_ingredientsList.clear();
				_ingredientsList = null;
				return;
			}
			switch (e.getItemId())
			{
			case -200: // Clan Reputation Score
			{
				if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
					return;
				}
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
					return;
				}
				if (player.getClan().getReputationScore() < e.getItemCount() * _amount)
				{
					player.sendPacket(SystemMessageId.CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return;
				}
				break;
			}
			case -100:
				if(player.getPcCaffePoints() <  e.getItemCount() * _amount)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					return;
				}
				break;
			case -300: // Player Fame
			{
				if (player.getFame() < e.getItemCount() * _amount)
				{
					player.sendMessage("Недостаточно очков славы");
					return;
				}
				break;
			}
			default:
			{
				int enchantLvl = -1;
				if (maintainEnchantment)
					enchantLvl = e.getEnchantmentLevel();
				else if (e.getEnchantmentLevel()>0)
					enchantLvl = e.getEnchantmentLevel();
				
				
				if (inv.getInventoryItemCount(e.getItemId(), enchantLvl) < ((Config.ALT_BLACKSMITH_USE_RECIPES || !e
						.getMantainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					_ingredientsList.clear();
					_ingredientsList = null;
					return;
				}
				if (ItemTable.getInstance().createDummyItem(e.getItemId()).isStackable())
					_enchantment = 0;
				break;
			}
			}
		}

		_ingredientsList.clear();
		_ingredientsList = null;
		FastList<L2Augmentation> augmentation = new FastList<L2Augmentation>();
		/** All ok, remove items and add final product */

		for (MultiSellIngredient e : entry.getIngredients())
		{
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					int repCost = player.getClan().getReputationScore() - (e.getItemCount() * _amount);
					player.getClan().setReputationScore(repCost, true);
					SystemMessage smsg = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					smsg.addNumber(e.getItemCount() * _amount);
					player.sendPacket(smsg);
					player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
					break;
				}
				case -300: // Player Fame
				{
					int fameCost = player.getFame() - (e.getItemCount() * _amount);
					player.setFame(fameCost);
					player.sendPacket(new UserInfo(player));
					break;
				}
				case -100:
					player.setPcCaffePoints(player.getPcCaffePoints()-(e.getItemCount() * _amount));
					player.sendPacket(new SystemMessage(SystemMessageId.USING_S1_PCPOINT).addNumber(e.getItemCount() * _amount));
					player.sendPacket(new ExPCCafePointInfo(player,-(e.getItemCount() * _amount),true,24,true));
					break;
				default:
				{
					L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId());
					if (itemToTake == null)
						return;
					if (itemToTake.isWear())
						return;
					
					if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient())
					{
						if (itemToTake.isStackable())
						{
							if (!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
								return;
						}
						else
						{
							if (maintainEnchantment || e.getEnchantmentLevel()>0)
							{
								L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
								for (int i = 0; i < (e.getItemCount() * _amount); i++)
								{
									if (inventoryContents[i].isAugmented())
										augmentation.add(inventoryContents[i].getAugmentation());
									if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
										return;
								}
							}
							else
							{
								for (int i = 1; i <= (e.getItemCount() * _amount); i++)
								{
									L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId());

									itemToTake = inventoryContents[0];
									if (itemToTake.getEnchantLevel() > 0)
									{
										for (L2ItemInstance item : inventoryContents)
										{
											if (item.getEnchantLevel() < itemToTake.getEnchantLevel())
											{
												itemToTake = item;
												if (itemToTake.getEnchantLevel() == 0)
													break;
											}
										}
									}
									if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
										return;
								}
							}
						}
					}
					break;
				}
			}
		}

		for (MultiSellIngredient e : entry.getProducts())
		{
			if (ItemTable.getInstance().getTemplate(e.getItemId()).isStackable())
				inv.addItem("Multisell", e.getItemId(), (e.getItemCount() * _amount), player, player.getTarget());
			else
			{
				L2ItemInstance product = null;
				for (int i = 0; i < (e.getItemCount() * _amount); i++)
				{
					product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
					if (maintainEnchantment)
					{
						if (i < augmentation.size())
							product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill()));
					}
					product.setEnchantLevel(e.getEnchantmentLevel());					
				}
			}
			SystemMessage sm;

			if (e.getItemCount() * _amount > 1)
			{
				sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(e.getItemId());
				sm.addNumber(e.getItemCount() * _amount);
				player.sendPacket(sm);
				sm = null;
			}
			else
			{
				if (maintainEnchantment && e.getEnchantmentLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
					sm.addNumber(e.getEnchantmentLevel());
					sm.addItemName(e.getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.EARNED_S1);
					sm.addItemName(e.getItemId());
				}
				player.sendPacket(sm);
				sm = null;
			}
		}
		player.sendPacket(new ItemList(player, false));
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		su = null;

		// finally, give the tax to the castle...
		if (merchant!= null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
	}

	private MultiSellEntry prepareEntry(L2NpcInstance merchant, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel)
	{
		MultiSellEntry newEntry = L2Multisell.getInstance().new MultiSellEntry();
		if(templateEntry==null)
			return newEntry;
		newEntry.setEntryId(templateEntry.getEntryId());
		int totalAdenaCount = 0;
		boolean hasIngredient = false;

		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			if(ing==null)
				continue;
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);

			if (newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.;
				if (applyTaxes)
				{
					if (merchant != null && merchant.getIsInTown())
						taxRate = merchant.getCastle().getTaxRate();
				}

				_transactionTax = (int) Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue;
			}
			else if (ing.getItemId() == 57)
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue;
			}
			else if (newIngredient.getItemId() > 0)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					if (maintainEnchantment)
						newIngredient.setEnchantmentLevel(enchantLevel);
					else
						newIngredient.setEnchantmentLevel(ing.getEnchantmentLevel());
					hasIngredient = true;
				}
			}
			newEntry.addIngredient(newIngredient);
		}
		if (totalAdenaCount > 0)
			newEntry.addIngredient(L2Multisell.getInstance().new MultiSellIngredient(57, totalAdenaCount, false, false));

		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			if(ing==null)
				continue;

			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);

			if (hasIngredient)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					if (maintainEnchantment)
						newIngredient.setEnchantmentLevel(enchantLevel);
					else
						newIngredient.setEnchantmentLevel(ing.getEnchantmentLevel());
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}

	@Override
	public String getType()
	{
		return _C__A7_MULTISELLCHOOSE;
	}
}