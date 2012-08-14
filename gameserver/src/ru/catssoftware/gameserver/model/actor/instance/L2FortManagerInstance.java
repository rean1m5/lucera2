package ru.catssoftware.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.TeleportLocationTable;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2TeleportLocation;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.network.serverpackets.WareHouseDepositList;
import ru.catssoftware.gameserver.network.serverpackets.WareHouseWithdrawalList;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;


/**
 * Fortress Foreman implementation used for:
 * Area Teleports, Support Magic, Clan Warehouse, Exp Loss Reduction
 */
public class L2FortManagerInstance extends L2MerchantInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;

	public L2FortManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.sendPacket(html);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		player.setLastFolkNPC(this);

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the zL2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to
		// avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		int condition = validateCondition(player);

		// BypassValidation Exploit plug.
		if (player.getLastFolkNPC().getObjectId() != this.getObjectId())
			return;

		if (condition <= COND_ALL_FALSE || condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;
		else if (condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command

			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}

			if (actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

				if ((player.getClanPrivileges() & L2Clan.CP_CS_DISMISS) == L2Clan.CP_CS_DISMISS)
				{
					if (val.isEmpty())
					{
						html.setFile("data/html/fortress/foreman-expel.htm");
					}
					else
					{
						getFort().banishForeigners(); // Move non-clan members off fortress area
						html.setFile("data/html/fortress/foreman-expeled.htm");
					}
				}
				else
				{
					html.setFile("data/html/fortress/foreman-noprivs.htm");
				}

				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("manage_vault"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE)
				{
					if (val.equalsIgnoreCase("deposit"))
						showVaultWindowDeposit(player);
					else if (val.equalsIgnoreCase("withdraw"))
						showVaultWindowWithdraw(player);
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile("data/html/fortress/foreman-vault.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/fortress/foreman-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("receive_report"))
			{
				SimpleDateFormat format2 = new SimpleDateFormat("HH");
				SimpleDateFormat format3 = new SimpleDateFormat("mm");

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					
				if (getFort().getFortState() == 1)
					html.setFile("data/html/fortress/foreman-report-independant.htm");
				else if (getFort().getFortState() == 2)
					html.setFile("data/html/fortress/foreman-report-contracted.htm");
				else
					html.setFile("data/html/fortress/foreman-report-unknown.htm");

				html.replace("%hr%", format2.format(getFort().getOwnedTime()));
				html.replace("%min%", format3.format(getFort().getOwnedTime()));
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("operate_door")) // Door
			// Control
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
				{
					if (!val.isEmpty())
					{
						boolean open = (Integer.parseInt(val) == 1);
						while (st.hasMoreTokens())
							getFort().openCloseDoor(Integer.parseInt(st.nextToken()), open);
					}

					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/fortress/" + getTemplate().getNpcId() + "-d.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					return;
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/fortress/foreman-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
			}
			else if (actualCommand.equalsIgnoreCase("manage_functions"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/fortress/foreman-manage.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("functions"))
			{
				if (val.equalsIgnoreCase("tele"))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					if (getFort().getFunction(Fort.FUNC_TELEPORT) == null)
						html.setFile("data/html/fortress/foreman-nac.htm");
					else
						html.setFile("data/html/fortress/"+ getNpcId()+ "-t" + getFort().getFunction(Fort.FUNC_TELEPORT).getLvl()+ ".htm");
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("support"))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					if (getFort().getFunction(Fort.FUNC_SUPPORT) == null)
						html.setFile("data/html/fortress/foreman-nac.htm");
					else
					{
						html.setFile("data/html/fortress/support" + getFort().getFunction(Fort.FUNC_SUPPORT).getLvl()+ ".htm");
						html.replace("%mp%", String.valueOf((int)getStatus().getCurrentMp()));
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("back"))
					showMessageWindow(player);
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/fortress/foreman-functions.htm");
					if (getFort().getFunction(Fort.FUNC_RESTORE_EXP) != null)
						html.replace("%xp_regen%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLvl()));
					else
						html.replace("%xp_regen%", "0");
					if (getFort().getFunction(Fort.FUNC_RESTORE_HP) != null)
						html.replace("%hp_regen%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_HP).getLvl()));
					else
						html.replace("%hp_regen%", "0");
					if (getFort().getFunction(Fort.FUNC_RESTORE_MP) != null)
						html.replace("%mp_regen%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_MP).getLvl()));
					else
						html.replace("%mp_regen%", "0");
					sendHtmlMessage(player, html);
				}
			}
			else if (actualCommand.equalsIgnoreCase("manage"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_SET_FUNCTIONS) == L2Clan.CP_CS_SET_FUNCTIONS)
				{
					if (val.equalsIgnoreCase("recovery"))
					{
						if (st.countTokens() >= 1)
						{
							if (getFort().getOwnerClan() == null)
							{
								player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_RIGHTS_TO_CONTROLL_FORT));
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("hp_cancel"))
							{
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "recovery hp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("mp_cancel"))
							{
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "recovery mp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("exp_cancel"))
							{
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "recovery exp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_hp"))
							{
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-apply.htm");
								html.replace("%name%", "(Устройство Восстановления HP)");
								int percent = Integer.valueOf(val);
								int cost;
								switch (percent)
								{
									case 300:
										cost = Config.FORT_HPREG1_FEE;
										break;
									default: // 400
										cost = Config.FORT_HPREG2_FEE;
										break;
								}

								html.replace("%cost%", String.valueOf(cost) + "</font>Аден /" + String.valueOf(Config.FORT_HPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней</font>)");
								html.replace("%use%", "Предоставляет дополнительное восстановление HP участникам клана в крепости.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "Recovery hp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_mp"))
							{
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-apply.htm");
								html.replace("%name%", "(Восстановление MP)");
								int percent = Integer.valueOf(val);
								int cost;
								switch (percent)
								{
									case 40:
										cost = Config.FORT_MPREG1_FEE;
										break;
									default: // 50
										cost = Config.FORT_MPREG2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Аден /" + String.valueOf(Config.FORT_MPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней</font>)");
								html.replace("%use%", "Предоставляет дополнительное восстановление MP участникам клана в крепости.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "Recovery mp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_exp"))
							{
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-apply.htm");
								html.replace("%name%", "(Устройство Восстановления EXP)");
								int percent = Integer.valueOf(val);
								int cost;
								switch (percent)
								{
									case 45:
										cost = Config.FORT_EXPREG1_FEE;
										break;
									default: // 50
										cost = Config.FORT_EXPREG2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Аден /" + String.valueOf(Config.FORT_EXPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней</font>)");
								html.replace("%use%", "Восстанавливает Exp любого участника клана, который возрожден в крепости.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "Recovery exp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("hp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile("data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_RESTORE_HP) != null)
									{
										if (getFort().getFunction(Fort.FUNC_RESTORE_HP).getLvl() == Integer.valueOf(val))
										{
											html.setFile("data/html/fortress/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									int percent = Integer.valueOf(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											html.setFile("data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 300:
											fee = Config.FORT_HPREG1_FEE;
											break;
										default: // 400
											fee = Config.FORT_HPREG2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_RESTORE_HP, percent, fee, Config.FORT_HPREG_FEE_RATIO, (getFort().getFunction(Fort.FUNC_RESTORE_HP) == null)))
									{
										html.setFile("data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("mp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile("data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_RESTORE_MP) != null)
									{
										if (getFort().getFunction(Fort.FUNC_RESTORE_MP).getLvl() == Integer.valueOf(val))
										{
											html.setFile("data/html/fortress/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									int percent = Integer.valueOf(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											html.setFile("data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 40:
											fee = Config.FORT_MPREG1_FEE;
											break;
										default: // 50
											fee = Config.FORT_MPREG2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_RESTORE_MP, percent, fee, Config.FORT_MPREG_FEE_RATIO, (getFort().getFunction(Fort.FUNC_RESTORE_MP) == null)))
									{
										html.setFile("data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("exp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile("data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_RESTORE_EXP) != null)
									{
										if (getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLvl() == Integer.valueOf(val))
										{
											html.setFile("data/html/fortress/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									int percent = Integer.valueOf(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											html.setFile("data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 45:
											fee = Config.FORT_EXPREG1_FEE;
											break;
										default: // 50
											fee = Config.FORT_EXPREG2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_RESTORE_EXP, percent, fee, Config.FORT_EXPREG_FEE_RATIO, (getFort().getFunction(Fort.FUNC_RESTORE_EXP) == null)))
									{
										html.setFile("data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
						}
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/fortress/edit_recovery.htm");
						String hp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 400\">400%</a>]";
						String exp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
						String mp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 50\">50%</a>]";
						if (getFort().getFunction(Fort.FUNC_RESTORE_HP) != null)
						{
							html.replace("%hp_recovery%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_HP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_HP).getLease()) + "</font>Аден /" + String.valueOf(Config.FORT_HPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней)");
							html.replace("%hp_period%", "Дата взымания арендной платы за функцию " + format.format(getFort().getFunction(Fort.FUNC_RESTORE_HP).getEndTime()));
							html.replace("%change_hp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Отключить</a>]" + hp);
						}
						else
						{
							html.replace("%hp_recovery%", "нет");
							html.replace("%hp_period%", "нет");
							html.replace("%change_hp%", hp);
						}
						if (getFort().getFunction(Fort.FUNC_RESTORE_EXP) != null)
						{
							html.replace("%exp_recovery%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLease()) + "</font>Аден /" + String.valueOf(Config.FORT_EXPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней)");
							html.replace("%exp_period%", "Дата взымания арендной платы за функцию " + format.format(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getEndTime()));
							html.replace("%change_exp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Отключить</a>]" + exp);
						}
						else
						{
							html.replace("%exp_recovery%", "нет");
							html.replace("%exp_period%", "нет");
							html.replace("%change_exp%", exp);
						}
						if (getFort().getFunction(Fort.FUNC_RESTORE_MP) != null)
						{
							html.replace("%mp_recovery%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_MP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_MP).getLease()) + "</font>Аден /" + String.valueOf(Config.FORT_MPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней)");
							html.replace("%mp_period%", "Дата взымания арендной платы за функцию " + format.format(getFort().getFunction(Fort.FUNC_RESTORE_MP).getEndTime()));
							html.replace("%change_mp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Отключить</a>]" + mp);
						}
						else
						{
							html.replace("%mp_recovery%", "нет");
							html.replace("%mp_period%", "нет");
							html.replace("%change_mp%", mp);
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("other"))
					{
						if (st.countTokens() >= 1)
						{
							if (getFort().getOwnerClan() == null)
							{
								player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_RIGHTS_TO_CONTROLL_FORT));
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("tele_cancel"))
							{
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "other tele 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("support_cancel"))
							{
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "other support 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_support"))
							{
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-apply.htm");
								html.replace("%name%", "Знаки отличия (Дополнительное Волшебство)");
								int stage = Integer.valueOf(val);
								int cost;
								switch (stage)
								{
									case 1:
										cost = Config.FORT_SUPPORT1_FEE;
										break;
									default:
										cost = Config.FORT_SUPPORT2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Аден /" + String.valueOf(Config.FORT_SUPPORT_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней</font>)");
								html.replace("%use%", "Позволяет использовать вспомагательную магию.");
								html.replace("%apply%", "other support " + String.valueOf(stage));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_tele"))
							{
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/fortress/functions-apply.htm");
								html.replace("%name%", "Зеркало (Устройство Телепортации)");
								int stage = Integer.valueOf(val);
								int cost;
								switch (stage)
								{
									case 1:
										cost = Config.FORT_TELE1_FEE;
										break;
									default:
										cost = Config.FORT_TELE2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Аден /" + String.valueOf(Config.FORT_TELE_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней</font>)");
								html.replace("%use%", "Телепортирует участников клана в форте к цели <font color=\"00FFFF\">Стадия " + String.valueOf(stage) + "</font> - го уровня");
								html.replace("%apply%", "other tele "+ String.valueOf(stage));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("tele"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile("data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_TELEPORT) != null)
									{
										if (getFort().getFunction(Fort.FUNC_TELEPORT).getLvl() == Integer.valueOf(val))
										{
											html.setFile("data/html/fortress/functions-used.htm");
											html.replace("%val%", "Стадия "	+ String.valueOf(val));
											sendHtmlMessage(player, html);
											return;
										}
									}
									int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											html.setFile("data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 1:
											fee = Config.FORT_TELE1_FEE;
											break;
										default:
											fee = Config.FORT_TELE2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_TELEPORT, lvl, fee, Config.FORT_TELE_FEE_RATIO, (getFort().getFunction(Fort.FUNC_TELEPORT) == null)))
									{
										html.setFile("data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("support"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									NpcHtmlMessage html = new NpcHtmlMessage(1);
									html.setFile("data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_SUPPORT) != null)
									{
										if (getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() == Integer.valueOf(val))
										{
											html.setFile("data/html/fortress/functions-used.htm");
											html.replace("%val%", "Стадия " + String.valueOf(val));
											sendHtmlMessage(player, html);
											return;
										}
									}
									int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											html.setFile("data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 1:
											fee = Config.FORT_SUPPORT1_FEE;
											break;
										default:
											fee = Config.FORT_SUPPORT2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_SUPPORT, lvl, fee, Config.FORT_SUPPORT_FEE_RATIO, (getFort().getFunction(Fort.FUNC_SUPPORT) == null)))
									{
										html.setFile("data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									else
										sendHtmlMessage(player, html);
								}
								return;
							}
						}
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/fortress/edit_other.htm");
						String tele = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Ур. 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Ур. 2</a>]";
						String support = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Ур. 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Ур. 2</a>]";
						if (getFort().getFunction(Fort.FUNC_TELEPORT) != null)
						{
							html.replace("%tele%", "Стадия " + String.valueOf(getFort().getFunction(Fort.FUNC_TELEPORT).getLvl()) + "</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_TELEPORT).getLease()) + "</font>Аден /" + String.valueOf(Config.FORT_TELE_FEE_RATIO / 1000 / 60 / 60 / 24) + " Дней)");
							html.replace("%tele_period%", "Дата взымания арендной платы за функцию " + format.format(getFort().getFunction(Fort.FUNC_TELEPORT).getEndTime()));
							html.replace("%change_tele%", "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Отключить</a>]" + tele);
						}
						else
						{
							html.replace("%tele%", "нет");
							html.replace("%tele_period%", "нет");
							html.replace("%change_tele%", tele);
						}
						if (getFort().getFunction(Fort.FUNC_SUPPORT) != null)
						{
							html.replace("%support%", "Stage "
									+ String.valueOf(getFort().getFunction(Fort.FUNC_SUPPORT).getLvl())
									+ "</font> (<font color=\"FFAABB\">"
									+ String.valueOf(getFort().getFunction(Fort.FUNC_SUPPORT).getLease())
									+ "</font>Adena /"
									+ String.valueOf(Config.FORT_SUPPORT_FEE_RATIO
											/ 1000 / 60 / 60 / 24) + " Дней)");
							html.replace("%support_period%", "Дата взымания арендной платы за функцию "
									+ format.format(getFort().getFunction(Fort.FUNC_SUPPORT).getEndTime()));
							html.replace("%change_support%", "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Отключить</a>]"
									+ support);
						}
						else
						{
							html.replace("%support%", "нет");
							html.replace("%support_period%", "нет");
							html.replace("%change_support%", support);
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("back"))
						showMessageWindow(player);
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/fortress/manage.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/fortress/foreman-noprivs.htm");
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support"))
			{
				setTarget(player);
				L2Skill skill;
				if (val.isEmpty())
					return;

				try
				{
					int skill_id = Integer.parseInt(val);
					try
					{
						if (getFort().getFunction(Fort.FUNC_SUPPORT) == null)
							return;
						if (getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() == 0)
							return;
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						int skill_lvl = 0;
						if (st.countTokens() >= 1)
							skill_lvl = Integer.parseInt(st.nextToken());
						skill = SkillTable.getInstance().getInfo(skill_id, skill_lvl);
						if (skill.getSkillType() == L2SkillType.SUMMON)
							player.doCast(skill);
						else
						{
							if (!((skill.getMpConsume() + skill.getMpInitialConsume()) > getStatus().getCurrentMp()))
								this.doCast(skill);
							else
							{
								html.setFile("data/html/fortress/support-no_mana.htm");
								html.replace("%mp%", String.valueOf((int)getStatus().getCurrentMp()));
								sendHtmlMessage(player, html);
								return;
							}
						}
						html.setFile("data/html/fortress/support-done.htm");
						html.replace("%mp%", String.valueOf((int)getStatus().getCurrentMp()));
						sendHtmlMessage(player, html);
					}
					catch (Exception e)
					{
						player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_WRONG_SKILL_LEVEL_CONTACT_ADMINISTRATOR));
					}
				}
				catch (Exception e)
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_WRONG_SKILL_LEVEL_CONTACT_ADMINISTRATOR));
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support_back"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				if (getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() == 0)
					return;
				html.setFile("data/html/fortress/support"
						+ getFort().getFunction(Fort.FUNC_SUPPORT).getLvl()	+ ".htm");
				html.replace("%mp%", String.valueOf((int)getStatus().getCurrentMp()));
				sendHtmlMessage(player, html);
			}
			else if (actualCommand.equalsIgnoreCase("goto"))
			{
				int whereTo = Integer.parseInt(val);
				doTeleport(player, whereTo);
				return;
			}
			super.onBypassFeedback(player, command);
		}
	}

	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/fortress/foreman-no.htm";

		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/fortress/foreman-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER) // Clan owns Fortress
				filename = "data/html/fortress/foreman.htm"; // Owner message window
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	private void doTeleport(L2PcInstance player, int val)
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			if (player.reduceAdena("Телепортировать", list.getPrice(), this, true))
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
		}
		else
			_log.info("No teleport destination with id:" + val);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showVaultWindowDeposit(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getClan().getWarehouse());
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.CLAN));
	}

	private void showVaultWindowWithdraw(L2PcInstance player)
	{
		if (player.isClanLeader()||((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
		}
		else
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/fortress/foreman-noprivs.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
	}

	protected int validateCondition(L2PcInstance player)
	{
		if (getFort() != null && getFort().getFortId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getFort().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if (getFort().getOwnerClan() == player.getClan()) // Clan owns fortress
					return COND_OWNER; // Owner
			}
		}
		return COND_ALL_FALSE;
	}
}
