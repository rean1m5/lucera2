package ru.catssoftware.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.GmListTable;
import ru.catssoftware.gameserver.datatables.TradeListTable;
import ru.catssoftware.gameserver.datatables.xml.MerchantPriceConfigTable;
import ru.catssoftware.gameserver.datatables.xml.MerchantPriceConfigTable.MerchantPriceConfig;
import ru.catssoftware.gameserver.model.L2Multisell;
import ru.catssoftware.gameserver.model.L2TradeList;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.BuyList;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SellList;
import ru.catssoftware.gameserver.network.serverpackets.SetupGauge;
import ru.catssoftware.gameserver.network.serverpackets.ShopPreviewList;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

import javolution.text.TextBuilder;

public class L2MerchantInstance extends L2FolkInstance
{
	private MerchantPriceConfig	_mpc;

	public L2MerchantInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected String getHtmlFolder() {
		return "merchant";
	}

	public MerchantPriceConfig getMpc()
	{
		if (_mpc == null)
		{
			_mpc = MerchantPriceConfigTable.getInstance().getMerchantPriceConfig(this);
		}
		return _mpc;
	}

	private final void showWearWindow(L2PcInstance player, int val)
	{
		player.tempInvetoryDisable();
		L2TradeList list = TradeListTable.getInstance().getBuyList(val);

		if (list != null)
		{
			ShopPreviewList bl = new ShopPreviewList(list, player.getAdena(), player.getExpertiseIndex());
			player.sendPacket(bl);
		}
		else
			player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	protected void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 1.;

		if (getIsInTown())
			taxRate = getCastle().getTaxRate();
		taxRate = taxRate + getMpc().getBaseTaxRate();

		player.tempInvetoryDisable();

		L2TradeList list = TradeListTable.getInstance().getBuyList(val);

		if (list != null && (list.getNpcId()==-1 || list.getNpcId() == getNpcId()) )
		{
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else if (customBl && !Config.LOAD_CUSTOM_MERCHANT_BUYLISTS)
		{
			GmListTable.broadcastMessageToGMs("Внимание! Включите использование Custom Buylists.");
			_log.info(player.getName() + " is Trying to Buy Custom Buylists, please enable the load of it.");
		}
		else
		{
			int listid = list!=null?list.getListId():0;
			_log.info("Warn! Hacker: " + player.getName() + " attempting to buy from GM shop ("+listid+")! < Ban him!");
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	protected final void showSellWindow(L2PcInstance player)
	{
		player.sendPacket(new SellList(player));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		if (actualCommand.equalsIgnoreCase("Buy") || actualCommand.equalsIgnoreCase("CBuy"))
		{
			if (actualCommand.equalsIgnoreCase("CBuy"))
				customBl = true;
			if (st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
		else if (actualCommand.equalsIgnoreCase("RentPet"))
		{
			if (Config.ALLOW_RENTPET)
			{
				if (st.countTokens() < 1)
				{
					showRentPetWindow(player);
				}
				else
				{
					int val = Integer.parseInt(st.nextToken());
					tryRentPet(player, val);
				}
			}
		}
		else if (actualCommand.equalsIgnoreCase("Wear") && Config.ALLOW_WEAR)
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Multisell"))
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().separateAndSend(val, player, false, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("Exc_Multisell"))
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().separateAndSend(val, player, true, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("BuyShadow"))
		{
			if (st.countTokens() < 1)
			{
				if (player.getLevel()<40 | player.getLevel()>=61)
					showShadowWindow(player);
			}
			if (player.getLevel()>=40 && player.getLevel()<52)
				L2Multisell.getInstance().separateAndSend(203, player, false, getCastle().getTaxRate());	
			else if (player.getLevel()>=52 && player.getLevel()<61)
				L2Multisell.getInstance().separateAndSend(204, player, false, getCastle().getTaxRate());
		}			
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public final void showShadowWindow(L2PcInstance player)
	{
		TextBuilder html2 = new TextBuilder("<html><body>Продавец Теневого Оружия:<br>");
		html2.append("<tr><td>Теневое оружие доступно с 40 по 61 уровень</td></tr>");
		html2.append("</body></html>");
		insertObjectIdAndShowChatWindow(player, html2.toString());
	}	
	
	public final void showRentPetWindow(L2PcInstance player)
	{
		if (!Config.LIST_PET_RENT_NPC.contains(getTemplate().getNpcId()))
			return;

		TextBuilder html1 = new TextBuilder("<html><body>Pet Manager:<br>");
		html1.append("You can rent a wyvern or strider for adena.<br>My prices:<br1>");
		html1.append("<table border=0><tr><td>Ride</td></tr>");
		html1.append("<tr><td>Wyvern</td><td>Strider</td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 1\">30 sec/1800 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 11\">30 sec/900 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 2\">1 min/7200 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 12\">1 min/3600 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 3\">10 min/720000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 13\">10 min/360000 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 4\">30 min/6480000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 14\">30 min/3240000 adena</a></td></tr>");
		html1.append("</table>");
		html1.append("</body></html>");

		insertObjectIdAndShowChatWindow(player, html1.toString());
	}	public void tryRentPet(L2PcInstance player, int val)
	{
		if (player == null || player.getPet() != null || player.isMounted() || player.isRentedPet() || player.isCursedWeaponEquipped())
			return;

		if (!player.disarmWeapons())
			return;

		int petId;
		double price = 1;
		int cost[] = { 1800, 7200, 720000, 6480000 };
		int ridetime[] = { 30, 60, 600, 1800 };

		if (val > 10)
		{
			petId = 12526;
			val -= 10;
			price /= 2;
		}
		else
			petId = 12621;

		if (val < 1 || val > 4)
			return;

		price *= cost[val - 1];
		int time = ridetime[val - 1];

		if (!player.reduceAdena("Rent", (int) price, player.getLastFolkNPC(), true))
			return;

		player.mount(petId, 0, false);
		SetupGauge sg = new SetupGauge(3, time * 1000);
		player.sendPacket(sg);
		player.startRentPet(time);
	}

	@Override
	public final void onActionShift(L2PcInstance player)
	{
		if (player.isGM())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder html1 = new TextBuilder("<html><body><table border=0>");
			html1.append("<tr><td>Current Target:</td></tr>");
			html1.append("<tr><td><br></td></tr>");

			html1.append("<tr><td>Object ID: " + getObjectId() + "</td></tr>");
			html1.append("<tr><td>Template ID: " + getTemplate().getNpcId() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");

			html1.append("<tr><td>HP: " + getStatus().getCurrentHp() + "</td></tr>");
			html1.append("<tr><td>MP: " + getStatus().getCurrentMp() + "</td></tr>");
			html1.append("<tr><td>Level: " + getLevel() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");

			String className = getClass().getName().substring(44);
			html1.append("<tr><td>Class: " + className + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");

			html1.append("</table><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc " + getTemplate().getNpcId()
					+ "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
			html1.append("<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
			html1.append("<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist " + getTemplate().getNpcId()
					+ "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
			html1.append("<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td></tr>");
			html1.append("</table>");

			if (player.isGM())
			{
				html1.append("<button value=\"View Shop\" action=\"bypass -h admin_showShop " + getTemplate().getNpcId()
						+ "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></br>");
				html1.append("<button value=\"View Custom Shop\" action=\"bypass -h admin_showCustomShop " + getTemplate().getNpcId()
						+ "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></br>");
				html1.append("<button value=\"Lease next week\" action=\"bypass -h npc_" + getObjectId()
						+ "_Lease\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
				html1.append("<button value=\"Abort current leasing\" action=\"bypass -h npc_" + getObjectId()
						+ "_Lease next\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
				html1.append("<button value=\"Manage items\" action=\"bypass -h npc_" + getObjectId()
						+ "_Lease manage\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
			}

			html1.append("</body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	boolean	customBl	= false;
}
