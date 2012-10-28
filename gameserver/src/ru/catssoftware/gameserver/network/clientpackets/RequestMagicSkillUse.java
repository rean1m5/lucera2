package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.restriction.AvailableRestriction;
import ru.catssoftware.gameserver.model.restriction.ObjectRestrictions;
import ru.catssoftware.gameserver.templates.skills.L2SkillType;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class RequestMagicSkillUse extends L2GameClientPacket
{
	private static final String	_C__2F_REQUESTMAGICSKILLUSE	= "[C] 2F RequestMagicSkillUse";
	private int					_magicId;
	private boolean				_ctrlPressed;
	private boolean				_shiftPressed;

	@Override
	protected void readImpl()
	{
		_magicId = readD(); // Identifier of the used skill
		_ctrlPressed = readD() != 0; // true if it's a ForceAttack : Ctrl pressed
		_shiftPressed = readC() != 0; // true if Shift pressed
	}

	@Override
	protected void runImpl()
	{
		//Get the current L2PcInstance of the player
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		if (ObjectRestrictions.getInstance().checkRestriction(activeChar, AvailableRestriction.PlayerCast))
			return;


		int level = activeChar.getSkillLevel(_magicId);
		if (level <= 0)
		{
			ActionFailed();
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);

		// Check the validity of the skill
		if (skill != null && skill.getSkillType() != L2SkillType.NOTDONE)
		{
			if(Config.DISABLE_SKILLS_ON_LEVEL_LOST > 0) {
				if(skill.getMagicLevel()-activeChar.getLevel()>=Config.DISABLE_SKILLS_ON_LEVEL_LOST) {
					ActionFailed();
					return;
				}
			}

			if(activeChar._lastSkill==skill.getId() && Config.SKILL_DELAY>0) {
				if(!FloodProtector.tryPerformAction(activeChar, Protected.CASTSKILL)) {
					ActionFailed();
					return;
				}
					
			}
			activeChar._lastSkill = skill.getId();   
			// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
			if (skill.getSkillType() == L2SkillType.RECALL && !Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
				return;
			// players mounted on pets cannot use any toggle skills
			if (skill.isToggle() && activeChar.isMounted())
				return;
			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
		else
		{
			ActionFailed();
			if (skill != null && skill.getSkillType() == L2SkillType.NOTDONE)
				activeChar.sendMessage("Скил нереализован.");
		}
	}

	@Override
	public String getType()
	{
		return _C__2F_REQUESTMAGICSKILLUSE;
	}
}