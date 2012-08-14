package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.util.Util;
import ru.catssoftware.tools.geometry.Point3D;

public final class RequestExMagicSkillUseGround extends L2GameClientPacket
{
	private static final String	_C__D0_2F_REQUESTEXMAGICSKILLUSEGROUND	= "[C] D0:2F RequestExMagicSkillUseGround";

	private int					_x, _y, _z, _skillId;
	private boolean				_ctrlPressed, _shiftPressed;

	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_skillId = readD();
		_ctrlPressed = readD() != 0;
		_shiftPressed = readC() != 0;
	}

	@Override
	protected void runImpl()
	{
		// Get the current L2PcInstance of the player
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		// Get the level of the used skill
		int level = activeChar.getSkillLevel(_skillId);
		if (level <= 0)
		{
			ActionFailed();
			return;
		}

		// Get the L2Skill template corresponding to the skillID received from the client
		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);

		// Check the validity of the skill
		if (skill != null)
		{
			activeChar.setCurrentSkillWorldPosition(new Point3D(_x, _y, _z));

			// normally magicskilluse packet turns char client side but for these skills, it doesn't (even with correct target)
			activeChar.setHeading(Util.calculateHeadingFrom(activeChar.getX(), activeChar.getY(), _x, _y));
			activeChar.broadcastPacket(new ValidateLocation(activeChar));
			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
		else
			ActionFailed();
	}

	@Override
	public String getType()
	{
		return _C__D0_2F_REQUESTEXMAGICSKILLUSEGROUND;
	}
}
