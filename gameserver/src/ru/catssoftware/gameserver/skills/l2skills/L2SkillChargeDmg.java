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
package ru.catssoftware.gameserver.skills.l2skills;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.Formulas;
import ru.catssoftware.gameserver.templates.item.L2WeaponType;
import ru.catssoftware.util.StatsSet;

public class L2SkillChargeDmg extends L2Skill
{
	public L2SkillChargeDmg(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, L2Character... targets)
	{
		if (activeChar.isAlikeDead() || !(activeChar instanceof L2PcInstance))
			return;

		L2PcInstance player = (L2PcInstance) activeChar;

		double modifier = 0.8 + 0.201 * player.getCharges(); // thanks Diego Vargas of L2Guru: 70*((0.8+0.201*No.Charges) * (PATK+POWER)) / PDEF
		if (getConsumeCharges())
			player.decreaseCharges(getNeededCharges());

		for (L2Character target : targets)
		{
			L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
			if (target.isAlikeDead())
				continue;

			byte shld = Formulas.calcShldUse(activeChar, target);
			boolean crit = false;
			if (getBaseCritRate() > 0)
				crit = Formulas.calcCrit(getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar));

			boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

			// damage calculation, crit is static 2x
			int damage = (int) Formulas.calcPhysDam(activeChar, target, this, shld, false, false, soul);
			if (crit)
				damage *= 2;

			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, this);
			if (skillIsEvaded)
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_DODGES_ATTACK);
					sm.addCharName(target);
					activeChar.sendPacket(sm);
				}
				if (target instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_S1_ATTACK);
					sm.addCharName(activeChar);
					target.sendPacket(sm);
				}
			}
			else if (damage > 0)
			{
				double finalDamage = damage * modifier;
				target.reduceCurrentHp(finalDamage, activeChar, this);

				activeChar.sendDamageMessage(target, (int) finalDamage, false, crit, false);

				if ((Formulas.calcSkillReflect(target, this) & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
					activeChar.reduceCurrentHp(damage, target, this);

				if (soul && weapon != null)
					weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE,true);
			}
			else
				activeChar.sendDamageMessage(target, 0, false, false, true);
		} // effect self :]
		L2Effect seffect = activeChar.getFirstEffect(getId());
		if (seffect != null && seffect.isSelfEffect())
			//Replace old effect with new one.
			seffect.exit();
		// cast self effect if any
		getEffectsSelf(activeChar);
	}
}