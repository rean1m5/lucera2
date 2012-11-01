package ru.catssoftware.gameserver.network.serverpackets;

import java.util.Vector;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.templates.item.L2Item;


/**
 * This class ...
 *
 * @version $Revision: 1.18.2.5.2.8 $ $Date: 2005/04/05 19:41:08 $
 */
public final class SystemMessage extends L2GameServerPacket
{
	// d d (d S/d d/d dd)
	//      |--------------> 0 - String  1-number 2-textref npcname (1000000-1002655)  3-textref itemname 4-textref skills 5-??
	private static final int TYPE_ZONE_NAME = 7;
	private static final int TYPE_FORTRESS = 5; // maybe not only for fortress, rename if needed
	private static final int TYPE_SKILL_NAME = 4;
	private static final int TYPE_ITEM_NAME = 3;
	private static final int TYPE_NPC_NAME = 2;
	private static final int TYPE_NUMBER = 1;
	private static final int TYPE_TEXT = 0;
	private static final String _S__7A_SYSTEMMESSAGE = "[S] 62 SystemMessage";
	private int _messageId;
	private Vector<Integer> _types = new Vector<Integer>();
	private Vector<Object> _values = new Vector<Object>();
	private int _skillLvL = 1;

	public SystemMessage(SystemMessageId messageId)
	{
		_messageId = messageId.getId();
	}

	/**
	 * Use SystemMessage(SystemMessageId messageId) where possible instead
	 */
	public SystemMessage(int messageId)
	{
		_messageId = messageId;
	}

 	public static SystemMessage sendString(String msg)
	{
 		SystemMessage sm = new SystemMessage(SystemMessageId.S1);
 		sm.addString(msg);

 		return sm;
	}

	public SystemMessage addString(String text)
	{
		_types.add(Integer.valueOf(TYPE_TEXT));
		_values.add(text);

		return this;
	}

	public SystemMessage addFortId(int number)
	{
		_types.add(Integer.valueOf(TYPE_FORTRESS));
		_values.add(Integer.valueOf(number));
		return this;
	}

	public SystemMessage addNumber(int number)
	{
		_types.add(Integer.valueOf(TYPE_NUMBER));
		_values.add(Integer.valueOf(number));
		return this;
	}

	public SystemMessage addCharName(L2Character cha)
	{
		if (cha instanceof L2NpcInstance)
			return addNpcName((L2NpcInstance) cha);

		if (cha.isPlayer())
			return addPcName((L2PcInstance)cha);

		if (cha instanceof L2Summon)
			return addNpcName((L2Summon)cha);

		return addString(cha.getName());
	}

	public SystemMessage addPcName(L2PcInstance pc)
	{
		return addString(pc.getAppearance().getVisibleName());
	}

	public SystemMessage addNpcName(L2NpcInstance npc)
	{
		return addNpcName(npc.getTemplate());
	}

	public SystemMessage addNpcName(L2Summon npc)
	{
		return addNpcName(npc.getNpcId());
	}

	public SystemMessage addNpcName(L2NpcTemplate tpl)
	{
		if (tpl.isCustom())
			return addString(tpl.getName());

		return addNpcName(tpl.getNpcId());
	}

	public SystemMessage addNpcName(int id)
	{
		_types.add(Integer.valueOf(TYPE_NPC_NAME));
		_values.add(Integer.valueOf(1000000 + id));

		return this;
	}

	public SystemMessage addItemName(L2ItemInstance item)
	{
		if (item == null)
			return this;
		return addItemName(item.getItem());
	}

	public SystemMessage addItemName(L2Item item)
	{
		if(item.getItemDisplayId()!=item.getItemId())
			return addString(item.getName());
		else
			return addItemName(item.getItemId());
	}

	public SystemMessage addItemName(int id)
	{
		_types.add(Integer.valueOf(TYPE_ITEM_NAME));
		_values.add(Integer.valueOf(id));
		return this;
	}

	public SystemMessage addZoneName(int x, int y, int z)
	{
		_types.add(Integer.valueOf(TYPE_ZONE_NAME));
		int[] coord = {x, y, z};
		_values.add(coord);

		return this;
	}

	public SystemMessage addSkillName(L2Effect effect)
	{
		return addSkillName(effect.getSkill());
	}

	public SystemMessage addSkillName(L2Skill skill)
	{
		if (skill.getId() != skill.getDisplayId()) //custom skill -  need nameId or smth like this.
			return addString(skill.getName());

		return addSkillName(skill.getId(), skill.getLevel());
	}

	public SystemMessage addSkillName(int id)
	{
		return addSkillName(id, 1);
	}

	public SystemMessage addSkillName(int id, int lvl)
	{
		_types.add(Integer.valueOf(TYPE_SKILL_NAME));
		_values.add(Integer.valueOf(id));
		_skillLvL = lvl;

		return this;
	}

	@Override
	protected final void writeImpl(L2GameClient client,L2PcInstance activeChar)
	{
		writeC(0x64);

		writeD(_messageId);
		writeD(_types.size());

		for (int i = 0; i < _types.size(); i++)
		{
			int t = _types.get(i).intValue();

			writeD(t);

			switch (t)
			{
				case TYPE_TEXT:
				{
					writeS( (String)_values.get(i));
					break;
				}
				case TYPE_FORTRESS:
				case TYPE_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ITEM_NAME:
				{
					int t1 = ((Integer)_values.get(i)).intValue();
					writeD(t1);
					break;
				}
				case TYPE_SKILL_NAME:
				{
					int t1 = ((Integer)_values.get(i)).intValue();
					writeD(t1); // Skill Id
					writeD(_skillLvL); // Skill lvl
					break;
				}
				case TYPE_ZONE_NAME:
				{
					int t1 = ((int[])_values.get(i))[0];
					int t2 = ((int[])_values.get(i))[1];
					int t3 = ((int[])_values.get(i))[2];
					writeD(t1);
					writeD(t2);
					writeD(t3);
					break;
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__7A_SYSTEMMESSAGE;
	}

	public int getMessageID()
	{
		return _messageId;
	}
}