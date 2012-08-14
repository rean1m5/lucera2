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
package ru.catssoftware.gameserver.network.serverpackets;

import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Item;

import java.util.Vector;


/**
 * @author kombat
 * Format: c dd[d s/d/dd/ddd] dd
 */
public class ConfirmDlg extends L2GameServerPacket
{
	private static final String	_S__F3_CONFIRMDLG	= "[S] f3 ConfirmDlg";

	private Vector<String>		_values				= new Vector<String>();	// Average parameter size for most common messages

	private int _messageId = 0;
	private int _requesterId = 0;

	public ConfirmDlg(int messageId)
	{
		_messageId = messageId;
	}

	public ConfirmDlg(String text)
	{
		_messageId = SystemMessageId.S1.getId();
		_values.add(text);
	}

	public ConfirmDlg addString(String text)
	{
		_values.add(text);
		return this;
	}

	public ConfirmDlg addNumber(int number)
	{
		_values.add(String.valueOf(number));
		return this;
	}

	public ConfirmDlg addCharName(L2Character cha)
	{
		if (cha instanceof L2NpcInstance)
			return addNpcName((L2NpcInstance) cha);
		if (cha instanceof L2PcInstance)
			return addPcName((L2PcInstance) cha);
		if (cha instanceof L2Summon)
			return addNpcName((L2Summon) cha);
		return addString(cha.getName());
	}

	public ConfirmDlg addPcName(L2PcInstance pc)
	{
		return addString(pc.getAppearance().getVisibleName());
	}

	public ConfirmDlg addNpcName(L2NpcInstance npc)
	{
		return addNpcName(npc.getTemplate());
	}

	public ConfirmDlg addNpcName(L2Summon npc)
	{
		return addNpcName(npc.getNpcId());
	}

	public ConfirmDlg addNpcName(L2NpcTemplate tpl)
	{
		if (tpl.isCustom())
			return addString(tpl.getName());
		return addNpcName(tpl.getNpcId());
	}

	public ConfirmDlg addNpcName(int id)
	{
		_values.add(NpcTable.getInstance().getTemplate(id).getName());
		return this;
	}

	public ConfirmDlg addItemName(L2ItemInstance item)
	{
		return addItemName(item.getItem().getItemId());
	}

	public ConfirmDlg addItemName(L2Item item)
	{
		_values.add(item.getName());
		return this;
	}

	public ConfirmDlg addItemName(int id)
	{
		return addItemName(ItemTable.getInstance().getTemplate(id));
	}

	public ConfirmDlg addZoneName(int x, int y, int z)
	{
		return this;
	}

	public ConfirmDlg addSkillName(L2Effect effect)
	{
		return addSkillName(effect.getSkill());
	}

	public ConfirmDlg addSkillName(L2Skill skill)
	{
		if (skill.getId() != skill.getDisplayId()) //custom skill -  need nameId or smth like this.
			return addString(skill.getName());
		return addSkillName(skill.getId(), skill.getLevel());
	}

	public ConfirmDlg addSkillName(int id)
	{
		return addSkillName(id, 1);
	}

	public ConfirmDlg addSkillName(int id, int lvl)
	{
		_values.add(SkillTable.getInstance().getInfo(id, lvl).getName());
		return this;
	}

	public ConfirmDlg addTime(int time)
	{
		return this;
	}

	public ConfirmDlg addRequesterId(int id)
	{
		_requesterId = id;
		return this;
	}

	@Override
	protected final void writeImpl()
	{
		/*
		 * Р_Р°РєРчС' С_ РїР_Р_С'Р_РчС_РР_РчР_РёРчР_ РїС_Рё С_РчС_С_С_РчРєС'Рч:
		 * "Nemu is making an attempt at resurrection. Do you want to continue with this resurrection?"
		 * ED //C
		 * E6 05 00 00 - Р_Р_Р_РчС_ С_РёС_С'РчР_Р_Р_Р_Р_ С_Р_Р_Р+С%РчР_РёС_ int 1510 "$s1 is making an attempt at resurrection. Do you want to continue with this resurrection?"
		 * 02 00 00 00 00 00 00 - Р Р°Р·Р_РчС_ "РїС_РёРєС_РчРїР>РчР_РёР№" ($S1, $S2, $S3, ...)
		 * 00 - unknown
		 * 00 4E 00 65 00 6D 00 75 00 00 - $S1 (custom string), Р_ Р_Р°Р_Р_Р_Р_ С_Р>С_С╪Р°Рч Nemu
		 * 00 06 00 00 - Р'С_РчР_С_ Р_С'Р_РчС'Р° Р_Р° Р_РёР°Р>Р_Р_, С'С_РчР+С_РчС'С_С_ С_РєР°Р·Р°С'С_ Р_ РєР_Р_С_С'С_С_РєС'Р_С_Рч
		 * 00 - id, С'С_РчР+С_РчС'С_С_ С_РєР°Р·Р°С'С_ Р_ РєР_Р_С_С'С_С_РєС'Р_С_Рч РїС_РёС_Р_Р_РчР_РёРч
		 */
		writeC(0xed); //ED
		writeD(_messageId); //id system message
		writeD(_values.size()); // size custom
		writeD(0x00); // unknown
		for(int i = 0; i < _values.size(); i++)
		{
			writeS(_values.get(i));
		}
		writeD(0x6000); // time
		writeD(_requesterId); // id?
	}

	/* (non-Javadoc)
	 * @see ru.catssoftware.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__F3_CONFIRMDLG;
	}
}
