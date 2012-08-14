package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.gameserver.datatables.CharTemplateTable;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.network.serverpackets.NewCharacterSuccess;
import ru.catssoftware.gameserver.templates.chars.L2PcTemplate;

public class NewCharacter extends L2GameClientPacket
{
	private static final String	_C__0E_NEWCHARACTER	= "[C] 0E NewCharacter";

	@Override
	protected void readImpl(){}

	@Override
	protected void runImpl()
	{
		NewCharacterSuccess ct = new NewCharacterSuccess();

		L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(0);
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.fighter); // human fighter
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.mage); // human mage
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.elvenFighter); // elf fighter
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.elvenMage); // elf mage
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.darkFighter); // dark elf fighter
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.darkMage); // dark elf mage
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.orcFighter); // orc fighter
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.orcMage); // orc mage
		ct.addChar(template);

		template = CharTemplateTable.getInstance().getTemplate(ClassId.dwarvenFighter); // dwarf fighter
		ct.addChar(template);


		sendPacket(ct);
	}

	@Override
	public String getType()
	{
		return _C__0E_NEWCHARACTER;
	}
}
