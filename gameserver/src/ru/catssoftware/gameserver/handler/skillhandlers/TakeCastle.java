package ru.catssoftware.gameserver.handler.skillhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.handler.ISkillHandler;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.FortManager;
import ru.catssoftware.gameserver.instancemanager.FortSiegeManager;
import ru.catssoftware.gameserver.instancemanager.SiegeManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2ArtefactInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Castle;
import ru.catssoftware.gameserver.model.entity.Fort;
import ru.catssoftware.gameserver.model.entity.FortSiege;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;

public class TakeCastle implements ISkillHandler
{
	private static final L2SkillType[]	SKILL_IDS	= { L2SkillType.TAKECASTLE };

	public void useSkill(L2Character activeChar, L2Skill skill, L2Character... targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		if(player.getClan()==null)
			return;
		Fort fort = FortManager.getInstance().getFort(player);
		if(fort!=null && fort.getFortId()>0)  
			if(fort.getSiege().getIsInProgress()) 
				if(fort.getSiege().getAttackerClan(player.getClan())!=null) 
					if(player.getActiveWeaponItem()!=null && player.getActiveWeaponItem().getItemId()==Config.FORTSIEGE_COMBAT_FLAG_ID) {
						player.getInventory().destroyItemByItemId("endSiege", Config.FORTSIEGE_COMBAT_FLAG_ID, 1, player, null);
						fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.S1).addString("Попытка захвата"), player.getClan().getName());
						fort.endOfSiege(player.getClan());
					}
		
		if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId())
			return;

		Castle castle = CastleManager.getInstance().getCastle(player);
		if (castle == null || !SiegeManager.getInstance().checkIfOkToCastSealOfRule(player, castle, true))
			return;

		if (targets.length > 0 && targets[0] instanceof L2ArtefactInstance)
			castle.engrave(player.getClan(), targets[0].getObjectId());
	}

	public static boolean checkIfOkToCastSealOfRule(L2Character activeChar, boolean isCheckOnly)
	{
		
		if(activeChar.getActiveWeaponItem()!=null && activeChar.getActiveWeaponItem().getItemId()==Config.FORTSIEGE_COMBAT_FLAG_ID) {
			FortSiege siege  = FortSiegeManager.getInstance().getSiege(activeChar);
			if(siege!=null && siege.getAttackerClan(((L2PcInstance)activeChar).getClan())!=null) {
				if(activeChar.getTarget() instanceof L2ArtefactInstance) {
					L2ArtefactInstance flag = (L2ArtefactInstance)activeChar.getTarget();
					if (flag.getFortId() == siege.getFort().getFortId())
						return true;
				}
			}
		}
		return SiegeManager.getInstance().checkIfOkToCastSealOfRule(activeChar, CastleManager.getInstance().getCastle(activeChar), isCheckOnly);
	}

	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}