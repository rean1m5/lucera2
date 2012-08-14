package ru.catssoftware.gameserver.handler.usercommandhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.GameTimeController;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IUserCommandHandler;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SetupGauge;
import ru.catssoftware.gameserver.util.Broadcast;

public class Escape implements IUserCommandHandler
{
	private static final int[]	COMMAND_IDS	= { 52 };

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (activeChar.isInsideZone(L2Zone.FLAG_NOESCAPE))
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return false;
		}
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		// Check to see if the current player is in TvT , CTF or ViP events.
		if (activeChar.isInFunEvent())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
			return false;
		}
		// Check to see if the player is in a festival.
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
			return false;
		}
		// Check to see if player is in jail
		if (activeChar.isInJail())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
			return false;
		}
		if (activeChar.inObserverMode())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
			return false;
		}
		// Check to see if player is in a duel
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_CANNOT_ESCAPE));
			return false;
		}
		if (activeChar.isCastingNow() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead())
			return false;

		int unstuckTimer = (activeChar.isGM() ? 1000 : Config.UNSTUCK_INTERVAL * 1000);

		activeChar.forceIsCasting(GameTimeController.getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
		L2Skill GM_escape = SkillTable.getInstance().getInfo(2100, 1); // 1 second escape
		L2Skill escape = SkillTable.getInstance().getInfo(2099, 1); // 5 minutes escape
		if (activeChar.isGM())
		{
			if (GM_escape != null)
			{
				activeChar.doCast(GM_escape);
				return true;
			}
			activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_USE_ESCAPE), "1 sec."));
		}
		else if (Config.UNSTUCK_INTERVAL == 300 && escape  != null)
		{
			activeChar.doCast(escape);
			return true;
		}
		else
		{
			if (Config.UNSTUCK_INTERVAL > 100)
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_USE_ESCAPE), unstuckTimer / 60000 + " m."));
			else
				activeChar.sendMessage(String.format(Message.getMessage(activeChar, Message.MessageId.MSG_YOU_USE_ESCAPE), unstuckTimer / 1000 + " s."));
		}

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		//SoE Animation section
		activeChar.disableAllSkills();
		MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 1050, 1, unstuckTimer, 0, false);
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000);
		SetupGauge sg = new SetupGauge(0, unstuckTimer);
		activeChar.sendPacket(sg);
		//End SoE Animation section
		EscapeFinalizer ef = new EscapeFinalizer(activeChar);
		// continue execution later
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));

		return true;
	}

	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance	_activeChar;

		EscapeFinalizer(L2PcInstance activeChar)
		{
			_activeChar = activeChar;
		}

		public void run()
		{
			if (_activeChar.isDead())
				return;

			QuestState stq = _activeChar.getQuestState("144_InjuredDragon");
			if (stq != null)
			{
				stq.takeItems(13032,-999);
				stq.takeItems(13052,-1);
				stq.takeItems(13053,-1);
				stq.takeItems(13054,-1);
				stq.takeItems(13046,-1);
				stq.takeItems(13047,-1);
			}

			_activeChar.setIsIn7sDungeon(false);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			_activeChar.setInstanceId(0);
			try
			{
				_activeChar.teleToLocation(TeleportWhereType.Town);
			}
			catch (Exception e)
			{
				_log.error(e.getMessage(), e);
			}
		}
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}