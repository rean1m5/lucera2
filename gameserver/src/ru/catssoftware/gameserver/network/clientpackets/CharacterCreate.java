package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.CharNameTable;
import ru.catssoftware.gameserver.datatables.CharTemplateTable;
import ru.catssoftware.gameserver.datatables.ItemTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.idfactory.IdFactory;
import ru.catssoftware.gameserver.instancemanager.QuestManager;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2ShortCut;
import ru.catssoftware.gameserver.model.L2SkillLearn;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.base.Experience;
import ru.catssoftware.gameserver.model.itemcontainer.PcInventory;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.L2GameClient;
import ru.catssoftware.gameserver.network.serverpackets.CharCreateFail;
import ru.catssoftware.gameserver.network.serverpackets.CharCreateOk;
import ru.catssoftware.gameserver.network.serverpackets.CharSelectionInfo;
import ru.catssoftware.gameserver.taskmanager.SQLQueue;
import ru.catssoftware.gameserver.templates.chars.L2PcTemplate;
import ru.catssoftware.gameserver.templates.chars.L2PcTemplate.PcTemplateItem;

@SuppressWarnings("unused")
public class CharacterCreate extends L2GameClientPacket
{
	private static final String	_C__0B_CHARACTERCREATE	= "[C] 0B CharacterCreate";
	private String				_name;
	private int					_race, _classId, _int, _str, _con, _men, _dex, _wit;
	private byte				_sex, _hairStyle, _hairColor, _face;

	private static final Object _lock = new Object();

	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}

	@Override
	protected void runImpl()
	{
		synchronized (_lock)
		{
			if (CharNameTable.getInstance().doesCharNameExist(_name))
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS);
				sendPacket(ccf);
				return;
			}
			else if (CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT && Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0)
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS);
				sendPacket(ccf);
				return;
			}
			else if (!Config.CNAME_PATTERN.matcher(_name).matches())
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS);
				sendPacket(ccf);
				return;
			}

			L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(_classId);
			if (template == null || template.getClassBaseLevel() > 1)
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED);
				sendPacket(ccf);
				return;
			}

			int objectId = IdFactory.getInstance().getNextId();
			L2PcInstance newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(),_name, _hairStyle, _hairColor, _face, _sex!=0);
			newChar.getStatus().setCurrentHp(template.getBaseHpMax());
			newChar.getStatus().setCurrentCp(template.getBaseCpMax());
			newChar.getStatus().setCurrentMp(template.getBaseMpMax());

			// send acknowledgement
			CharCreateOk cco = new CharCreateOk();
			sendPacket(cco);

			initNewChar(getClient(), newChar);
		}
	}

	private void initNewChar(L2GameClient client, L2PcInstance newChar)
	{
		L2World.getInstance().storeObject(newChar);

		L2PcTemplate template = newChar.getTemplate();

		if (Config.STARTING_ADENA > 0)
			newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		if (Config.STARTING_AA > 0)
			newChar.addAncientAdena("Init", Config.STARTING_AA, null, false);

		for (int[] startingItems : Config.CUSTOM_STARTER_ITEMS)
		{
			if (newChar == null)
			{
				continue;
			}
			PcInventory inv = newChar.getInventory();
			if (ItemTable.getInstance().createDummyItem(startingItems[0]).isStackable())
			{ 
				inv.addItem("Starter Items", startingItems[0], startingItems[1], newChar, null);
			}
			else
			{
				for (int i = 0; i < startingItems[1]; i++)
				{
					inv.addItem("Starter Items", startingItems[0], 1, newChar, null);
				}
			}
		}
		if (Config.ALLOW_NEW_CHAR_CUSTOM_POSITION)
			newChar.getPosition().setXYZInvisible(Config.NEW_CHAR_POSITION_X, Config.NEW_CHAR_POSITION_Y, Config.NEW_CHAR_POSITION_Z);
		else
			newChar.getPosition().setXYZInvisible(template.getSpawnX(), template.getSpawnY(), template.getSpawnZ());
		if (Config.ALLOW_NEW_CHARACTER_TITLE)
			newChar.setTitle(Config.NEW_CHARACTER_TITLE);
		else
			newChar.setTitle("");
		if(Config.ENABLE_STARTUP_LVL)
		{
			long EXp = Experience.LEVEL[Config.ADD_LVL_NEWBIE];
			newChar.addExpAndSp(EXp , 0);
		}
		if (Config.NEW_CHAR_IS_NOBLE)
			newChar.setNoble(true);


		// new char give Lucky Protection
		newChar.addSkill(SkillTable.getInstance().getInfo(194, 1), true);

		L2ShortCut shortcut;
		//add attack shortcut
		shortcut = new L2ShortCut(0, 0, 3, 2, 0, 1);
		newChar.registerShortCut(shortcut);
		//add take shortcut
		shortcut = new L2ShortCut(3, 0, 3, 5, 0, 1);
		newChar.registerShortCut(shortcut);
		//add sit shortcut
		shortcut = new L2ShortCut(10, 0, 3, 0, 0, 1);
		newChar.registerShortCut(shortcut);

		for (PcTemplateItem ia : template.getItems())
		{
			L2ItemInstance item = newChar.getInventory().addItem("Init", ia.getItemId(), ia.getAmount(), newChar, null);

			// add tutbook shortcut
			if (item.getItemId() == 5588)
			{
				shortcut = new L2ShortCut(11, 0, 1, item.getObjectId(), 0, 1);
				newChar.registerShortCut(shortcut);
			}
			if (item.isEquipable() && ia.isEquipped())
				newChar.getInventory().equipItemAndRecord(item);
		}


		for (L2SkillLearn skill: SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId()))
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel()), true);
			if (skill.getId() == 1001 || skill.getId() == 1177)
			{
				shortcut = new L2ShortCut(1, 0, 2, skill.getId(), skill.getLevel(), 1);
				newChar.registerShortCut(shortcut);
			}
			if (skill.getId() == 1216)
			{
				shortcut = new L2ShortCut(10, 0, 2, skill.getId(), skill.getLevel(), 1);
				newChar.registerShortCut(shortcut);
			}
		}
		startTutorialQuest(newChar);
		startNewbieHelperQuest(newChar);
		//new Disconnection(getClient(), newChar).defaultSequence(true);
		newChar.store();
		newChar.deleteMe();

		// send char list
		CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}

	public void startTutorialQuest(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		Quest q = null;
		if (qs == null)
			q = QuestManager.getInstance().getQuest("255_Tutorial");
		if (q != null)
			q.newQuestState(player);
	}
	public void startNewbieHelperQuest(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("7003_NewbieHelper");
		Quest q = null;
		if (qs == null)
			q = QuestManager.getInstance().getQuest("7003_NewbieHelper");
		if (q != null)
		{
			q.newQuestState(player);
			player.getQuestState("7003_NewbieHelper").set("cond","0");
		}
		qs = player.getQuestState("1201_NewbieToken");
		q = null;
		if (qs == null)
			q = QuestManager.getInstance().getQuest("1201_NewbieToken");
		if (q != null)
			q.newQuestState(player);
	}

	@Override
	public String getType()
	{
		return _C__0B_CHARACTERCREATE;
	}
}