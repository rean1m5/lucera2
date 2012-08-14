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
package ru.catssoftware.gameserver.model.actor.instance;

import java.util.Calendar;
import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.SevenSigns;
import ru.catssoftware.gameserver.SevenSignsFestival;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Party;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.util.StatsSet;

import javolution.text.TextBuilder;

/**
 * Festival of Darkness Guide (Seven Signs)
 *
 * @author Tempy
 */
public final class L2FestivalGuideInstance extends L2FolkInstance
{
	protected int	_festivalType;
	protected int	_festivalOracle;
	protected int	_blueStonesNeeded;
	protected int	_greenStonesNeeded;
	protected int	_redStonesNeeded;

	/**
	 * @param template
	 */
	public L2FestivalGuideInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		switch (getNpcId())
		{
		case 31127:
		case 31132:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
			_festivalOracle = SevenSigns.CABAL_DAWN;
			_blueStonesNeeded = 900;
			_greenStonesNeeded = 540;
			_redStonesNeeded = 270;
			break;
		case 31128:
		case 31133:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
			_festivalOracle = SevenSigns.CABAL_DAWN;
			_blueStonesNeeded = 1500;
			_greenStonesNeeded = 900;
			_redStonesNeeded = 450;
			break;
		case 31129:
		case 31134:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
			_festivalOracle = SevenSigns.CABAL_DAWN;
			_blueStonesNeeded = 3000;
			_greenStonesNeeded = 1800;
			_redStonesNeeded = 900;
			break;
		case 31130:
		case 31135:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
			_festivalOracle = SevenSigns.CABAL_DAWN;
			_blueStonesNeeded = 4500;
			_greenStonesNeeded = 2700;
			_redStonesNeeded = 1350;
			break;
		case 31131:
		case 31136:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
			_festivalOracle = SevenSigns.CABAL_DAWN;
			_blueStonesNeeded = 6000;
			_greenStonesNeeded = 3600;
			_redStonesNeeded = 1800;
			break;

		case 31137:
		case 31142:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
			_festivalOracle = SevenSigns.CABAL_DUSK;
			_blueStonesNeeded = 900;
			_greenStonesNeeded = 540;
			_redStonesNeeded = 270;
			break;
		case 31138:
		case 31143:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
			_festivalOracle = SevenSigns.CABAL_DUSK;
			_blueStonesNeeded = 1500;
			_greenStonesNeeded = 900;
			_redStonesNeeded = 450;
			break;
		case 31139:
		case 31144:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
			_festivalOracle = SevenSigns.CABAL_DUSK;
			_blueStonesNeeded = 3000;
			_greenStonesNeeded = 1800;
			_redStonesNeeded = 900;
			break;
		case 31140:
		case 31145:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
			_festivalOracle = SevenSigns.CABAL_DUSK;
			_blueStonesNeeded = 4500;
			_greenStonesNeeded = 2700;
			_redStonesNeeded = 1350;
			break;
		case 31141:
		case 31146:
			_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
			_festivalOracle = SevenSigns.CABAL_DUSK;
			_blueStonesNeeded = 6000;
			_greenStonesNeeded = 3600;
			_redStonesNeeded = 1800;
			break;
		}
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("FestivalDesc"))
		{
			int val = Integer.parseInt(command.substring(13));
			showChatWindow(player, val, null, true);
		}
		else if(command.startsWith("ReturnFromRift")) {
			QuestState st = player.getQuestState("Quest 1103_OracleTeleport");
			if(st!=null)
				st.getQuest().onAdvEvent("Return", this, player);
			else
				player.teleToLocation(147468,27073,-2203,true);
			
		}
		else if (command.startsWith("Festival"))
		{
			L2Party playerParty = player.getParty();
			int val = Integer.parseInt(command.substring(9, 10));

			switch (val)
			{
			case 1: // Become a Participant
				// Check if the festival period is active, if not then don't allow registration.
				if (SevenSigns.getInstance().isSealValidationPeriod())
				{
					showChatWindow(player, 2, "a", false);
					return;
				}

				// Check if a festival is in progress, then don't allow registration yet.
				if (SevenSignsFestival.getInstance().isFestivalInitialized())
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_REGISTER_WHILE_EVENT));
					return;
				}

				// Check if the player is in a formed party already.
				if (playerParty == null)
				{
					showChatWindow(player, 2, "b", false);
					return;
				}

				// Check if the player is the party leader.
				if (!playerParty.isLeader(player))
				{
					showChatWindow(player, 2, "c", false);
					return;
				}

				// Check to see if the party has at least 5 members.
				if (playerParty.getMemberCount() < Config.ALT_FESTIVAL_MIN_PLAYER)
				{
					showChatWindow(player, 2, "b", false);
					return;
				}

				// Check if all the party members are in the required level range.
				if (playerParty.getLevel() > SevenSignsFestival.getMaxLevelForFestival(_festivalType))
				{
					showChatWindow(player, 2, "d", false);
					return;
				}

				/*
				 * Check to see if the player has already signed up,
				 * if they are then update the participant list providing all the
				 * required criteria has been met.
				 */
				if (player.isFestivalParticipant())
				{
					SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
					showChatWindow(player, 2, "f", false);
					return;
				}

				showChatWindow(player, 1, null, false);
				break;
			case 2: // Festival 2 xxxx
				int stoneType = Integer.parseInt(command.substring(11));
				int stonesNeeded = 0;

				switch (stoneType)
				{
				case SevenSigns.SEAL_STONE_BLUE_ID:
					stonesNeeded = _blueStonesNeeded;
					break;
				case SevenSigns.SEAL_STONE_GREEN_ID:
					stonesNeeded = _greenStonesNeeded;
					break;
				case SevenSigns.SEAL_STONE_RED_ID:
					stonesNeeded = _redStonesNeeded;
					break;
				}

				if (!player.destroyItemByItemId("SevenSigns", stoneType, stonesNeeded, this, true))
					return;

				SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
				SevenSignsFestival.getInstance().addAccumulatedBonus(_festivalType, stoneType, stonesNeeded);

				showChatWindow(player, 2, "e", false);
				break;
			case 3: // Score Registration
				// Check if the festival period is active, if not then don't register the score.
				if (SevenSigns.getInstance().isSealValidationPeriod())
				{
					showChatWindow(player, 3, "a", false);
					return;
				}

				// Check if a festival is in progress, if it is don't register the score.
				if (SevenSignsFestival.getInstance().isFestivalInProgress())
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_REGISTER_WHILE_EVENT));
					return;
				}

				// Check if the player is in a party.
				if (playerParty == null)
				{
					showChatWindow(player, 3, "b", false);
					return;
				}

				List<L2PcInstance> prevParticipants = SevenSignsFestival.getInstance().getPreviousParticipants(_festivalOracle, _festivalType);

				// Check if there are any past participants.
				if (prevParticipants == null)
					return;

				// Check if this player was among the past set of participants for this festival.
				if (!prevParticipants.contains(player))
				{
					showChatWindow(player, 3, "b", false);
					return;
				}

				// Check if this player was the party leader in the festival.
				if (player.getObjectId() != prevParticipants.get(0).getObjectId())
				{
					showChatWindow(player, 3, "b", false);
					return;
				}

				L2ItemInstance bloodOfferings = player.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);
				int offeringCount = 0;

				// Check if the player collected any blood offerings during the festival.
				if (bloodOfferings == null)
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_BLOOD_OFFERINGS));
					return;
				}

				offeringCount = bloodOfferings.getCount();

				int offeringScore = offeringCount * SevenSignsFestival.FESTIVAL_OFFERING_VALUE;
				boolean isHighestScore = SevenSignsFestival.getInstance().setFinalScore(player, _festivalOracle, _festivalType, offeringScore);

				player.destroyItem("SevenSigns", bloodOfferings, this, false);

				// Send message that the contribution score has increased.
				SystemMessage sm = new SystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED_BY_S1);
				sm.addNumber(offeringScore);
				player.sendPacket(sm);

				if (isHighestScore)
					showChatWindow(player, 3, "c", false);
				else
					showChatWindow(player, 3, "d", false);
				break;
			case 4: // Current High Scores
				TextBuilder strBuffer = new TextBuilder("<html><body>Гид Фестиваля:<br>Это лучший результат недели, для Фестиваля Заката ");

				final StatsSet dawnData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, _festivalType);
				final StatsSet duskData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, _festivalType);
				final StatsSet overallData = SevenSignsFestival.getInstance().getOverallHighestScoreData(_festivalType);

				final int dawnScore = dawnData.getInteger("score");
				final int duskScore = duskData.getInteger("score");
				int overallScore = 0;

				// If no data is returned, assume there is no record, or all scores are 0.
				if (overallData != null)
					overallScore = overallData.getInteger("score");

				strBuffer.append(SevenSignsFestival.getFestivalNameText(_festivalType) + ".<br>");

				if (dawnScore > 0)
					strBuffer.append("Рассвет: " + calculateDate(dawnData.getString("date")) + ". Очков " + dawnScore + "<br>" + dawnData.getString("members")
							+ "<br>");
				else
					strBuffer.append("Рассвет: Еще не учавствовал. Очки 0<br>");

				if (duskScore > 0)
					strBuffer.append("Закат: " + calculateDate(duskData.getString("date")) + ". Очки " + duskScore + "<br>" + duskData.getString("members")
							+ "<br>");
				else
					strBuffer.append("Закат: Еще не учавствовал. Очки 0<br>");

				if (overallScore > 0)
				{
					String cabalStr = "Дети Заката";

					if (overallData != null)
					{
						if (overallData.getString("cabal").equals("dawn"))
							cabalStr = "Дети Рассвета";

						strBuffer.append("Подробности лучшего результата: " + calculateDate(overallData.getString("date")) + ". Очков " + overallScore
								+ "<br>сторона: " + cabalStr + "<br>" + overallData.getString("members") + "<br>");
					}
				}
				else
					strBuffer.append("Подробности лучшего результата: Еще не определены. Очков 0<br>");

				strBuffer.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Назад</a></body></html>");

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setHtml(strBuffer.toString());
				player.sendPacket(html);
				break;
			case 8: // Increase the Festival Challenge
				if (playerParty == null)
					return;

				if (!SevenSignsFestival.getInstance().isFestivalInProgress())
					return;

				if (!playerParty.isLeader(player))
				{
					showChatWindow(player, 8, "a", false);
					break;
				}

				if (SevenSignsFestival.getInstance().increaseChallenge(_festivalOracle, _festivalType))
					showChatWindow(player, 8, "b", false);
				else
					showChatWindow(player, 8, "c", false);
				break;
			case 9: // Leave the Festival
				if (playerParty == null)
					return;

				/**
				 * If the player is the party leader, remove all participants from the festival
				 * (i.e. set the party to null, when updating the participant list)
				 * otherwise just remove this player from the "arena", and also remove them from the party.
				 */
				boolean isLeader = playerParty.isLeader(player);

				if (isLeader)
				{
					SevenSignsFestival.getInstance().updateParticipants(player, null);
				}

				if (playerParty.getMemberCount() > Config.ALT_FESTIVAL_MIN_PLAYER)
				{
					SevenSignsFestival.getInstance().updateParticipants(player, playerParty);
					playerParty.removePartyMember(player);
				}
				else
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CANT_LEAVE_FESTIVAL));
				break;
			case 0: // Distribute Accumulated Bonus
				if (!SevenSigns.getInstance().isSealValidationPeriod())
				{
					player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_NO_PRISE_TIL_EVENT_ACTIVE));
					return;
				}

				if (SevenSignsFestival.getInstance().distribAccumulatedBonus(player) > 0)
					showChatWindow(player, 0, "a", false);
				else
					showChatWindow(player, 0, "b", false);
				break;
			default:
				showChatWindow(player, val, null, false);
			}
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}

	private void showChatWindow(L2PcInstance player, int val, String suffix, boolean isDescription)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH + "festival/";
		filename += (isDescription) ? "desc_" : "festival_";
		filename += (suffix != null) ? val + suffix + ".htm" : val + ".htm";

		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalType%", SevenSignsFestival.getFestivalNameText(_festivalType));
		html.replace("%cycleMins%", String.valueOf(SevenSignsFestival.getInstance().getMinsToNextCycle()));
		if (!isDescription && "2b".equals(val + suffix))
			html.replace("%minFestivalPartyMembers%", String.valueOf(Config.ALT_FESTIVAL_MIN_PLAYER));

		// If the stats or bonus table is required, construct them.
		if (val == 5)
			html.replace("%statsTable%", getStatsTable());
		else if (val == 6)
			html.replace("%bonusTable%", getBonusTable());

		//festival's fee
		else if (val == 1)
		{
			html.replace("%blueStoneNeeded%", String.valueOf(_blueStonesNeeded));
			html.replace("%greenStoneNeeded%", String.valueOf(_greenStonesNeeded));
			html.replace("%redStoneNeeded%", String.valueOf(_redStonesNeeded));
		}

		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private final String getStatsTable()
	{
		TextBuilder tableHtml = new TextBuilder();

		// Get the scores for each of the festival level ranges (types).
		for (int i = 0; i < 5; i++)
		{
			int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
			int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
			String festivalName = SevenSignsFestival.getFestivalNameText(i);
			String winningCabal = "Дети Заката";

			if (dawnScore > duskScore)
				winningCabal = "Дети Рассвета";
			else if (dawnScore == duskScore)
				winningCabal = "Нет";

			tableHtml.append("<tr><td width=\"100\" align=\"center\">" + festivalName + "</td><td align=\"center\" width=\"35\">" + duskScore
					+ "</td><td align=\"center\" width=\"35\">" + dawnScore + "</td><td align=\"center\" width=\"130\">" + winningCabal + "</td></tr>");
		}

		return tableHtml.toString();
	}

	private final String getBonusTable()
	{
		TextBuilder tableHtml = new TextBuilder();

		// Get the accumulated scores for each of the festival level ranges (types).
		for (int i = 0; i < 5; i++)
		{
			int accumScore = SevenSignsFestival.getInstance().getAccumulatedBonus(i);
			String festivalName = SevenSignsFestival.getFestivalNameText(i);

			tableHtml.append("<tr><td align=\"center\" width=\"150\">" + festivalName + "</td><td align=\"center\" width=\"150\">" + accumScore + "</td></tr>");
		}

		return tableHtml.toString();
	}

	private final String calculateDate(String milliFromEpoch)
	{
		long numMillis = Long.valueOf(milliFromEpoch);
		Calendar calCalc = Calendar.getInstance();

		calCalc.setTimeInMillis(numMillis);

		return calCalc.get(Calendar.YEAR) + "/" + calCalc.get(Calendar.MONTH) + "/" + calCalc.get(Calendar.DAY_OF_MONTH);
	}
}
