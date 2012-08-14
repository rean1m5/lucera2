package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.Message;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Clan;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.zone.L2Zone;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class ClanGate implements ISkillHandler
{
	private static final L2SkillType[] CG_SKILLS = { L2SkillType.CLAN_GATE };

	@Override
	public L2SkillType[] getSkillIds()
	{
		return CG_SKILLS;
	}

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		L2PcInstance player = null;
		if (activeChar instanceof L2PcInstance)
			player = (L2PcInstance) activeChar;
		else
			return;

		if (player.isInFunEvent() || player.isInsideZone(L2Zone.FLAG_NOSUMMON) || player.isInsideZone(L2Zone.FLAG_NOLANDING) || player.isInOlympiadMode())
		{
			player.sendMessage(Message.getMessage(player, Message.MessageId.MSG_CANNOT_OPEN_PORTAL));
			return;
		}

		L2Clan clan = player.getClan();
		if (clan != null)
		{
			if(CastleManager.getInstance().getCastleByOwner(clan) != null)
			{
				Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
				if (player.isCastleLord(castle.getCastleId()))
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new RemoveClanGate(castle.getCastleId()), skill.getTotalLifeTime());
					castle.createClanGate(activeChar.getX(), activeChar.getY(), activeChar.getZ() + 20);
					player.getClan().broadcastToOnlineMembers(new SystemMessage(SystemMessageId.COURT_MAGICIAN_CREATED_PORTAL));
					skill.getEffects(activeChar, activeChar);
				}
			}
		}
		L2Effect effect = player.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
			effect.exit();
		skill.getEffectsSelf(player);
	}

	private class RemoveClanGate implements Runnable
	{
		private final int castle;

		private RemoveClanGate(int castle)
		{
			this.castle = castle;
		}

		@Override
		public void run()
		{
			CastleManager.getInstance().getCastleById(castle).destroyClanGate();
		}
	}
}
