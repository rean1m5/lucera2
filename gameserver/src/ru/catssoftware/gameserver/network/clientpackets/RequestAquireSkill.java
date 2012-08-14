package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillSpellbookTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SkillTreeTable;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2PledgeSkillLearn;
import ru.catssoftware.gameserver.model.L2ShortCut;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2SkillLearn;
import ru.catssoftware.gameserver.model.actor.instance.L2FishermanInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2FolkInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2VillageMasterInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ExStorageMaxCount;
import ru.catssoftware.gameserver.network.serverpackets.PledgeSkillList;
import ru.catssoftware.gameserver.network.serverpackets.ShortCutRegister;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.util.IllegalPlayerAction;
import ru.catssoftware.gameserver.util.Util;

public class RequestAquireSkill extends L2GameClientPacket
{
	private static final String	_C__6C_REQUESTAQUIRESKILL	= "[C] 6C RequestAquireSkill";

	private int					_id, _level, _skillType;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		L2FolkInstance trainer = player.getLastFolkNPC();
		if (trainer == null)
		{
			if (player.isGM())
				player.sendMessage("Каст скила прерван, некоректный NPC");
			return;
		}

		int npcid = trainer.getNpcId();

		if (!player.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false) && !player.isGM())
			return;
		if (!Config.ALT_GAME_SKILL_LEARN)
			player.setSkillLearningClassId(player.getClassId());
		if (player.getSkillLevel(_id) >= _level)
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);

		int counts = 0;
		int _requiredSp = 0;

		switch (_skillType)
		{
			case 0:
			{
				// normal skills
				L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getSkillLearningClassId());

				for (L2SkillLearn s : skills)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
					if (sk == null || sk != skill || !sk.getCanLearn(player.getSkillLearningClassId()) || !sk.canTeachBy(npcid))
						continue;
					counts++;
					_requiredSp = SkillTreeTable.getInstance().getSkillCost(player, skill);
				}

				if (counts == 0 && !Config.ALT_GAME_SKILL_LEARN)
				{
					player.sendMessage("Вы совершили противоправное действие. Гм проинформирован");
					Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " пытался выучить недопустимый скил!!!",
						IllegalPlayerAction.PUNISH_KICK);
					return;
				}

				if (player.getSp() >= _requiredSp)
				{
					if (Config.SP_BOOK_NEEDED)
					{
						int spbId = -1;
						if (skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION)
							spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill.getId(), _level);
						else
							spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill.getId(), _level);

						if (skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION || skill.getLevel() == 1 && spbId > -1)
						{
							L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);

							if (spb == null)
							{
								player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
								return;
							}
							player.destroyItem("Consume", spb.getObjectId(), 1, trainer, true);
						}
					}
				}
				else
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					return;
				}
				break;
			}
			case 1:
			{
				int costid = 0;
				int costcount = 0;
				// Skill Learn bug Fix
				L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSkills(player);

				for (L2SkillLearn s : skillsc)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

					if (sk == null || sk != skill)
						continue;

					counts++;
					costid = s.getIdCost();
					costcount = s.getCostCount();
					_requiredSp = s.getSpCost();
				}

				if (counts == 0)
				{
					player.sendMessage("Вы совершили противоправное действие. Гм проинформирован");
					Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " пытался выучить недопустимый скил!!!",
						IllegalPlayerAction.PUNISH_KICK);
					return;
				}

				if (player.getSp() >= _requiredSp)
				{
					if (!player.destroyItemByItemId("Consume", costid, costcount, trainer, false))
					{
						// Haven't spellbook
						player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
						return;
					}

					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addNumber(costcount);
					sm.addItemName(costid);
					sendPacket(sm);
					sm = null;
				}
				else
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					return;
				}
				break;
			}
			case 2:
			{
				if (!player.isClanLeader())
				{
					player.sendMessage("Доступно только для клан-лидера");
					return;
				}

				int itemId = 0;
				int repCost = 100000000;
				L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);

				for (L2PledgeSkillLearn s : skills)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

					if (sk == null || sk != skill)
						continue;

					counts++;
					itemId = s.getItemId();
					repCost = s.getRepCost();
				}
				if (counts == 0)
				{
					player.sendMessage("Вы совершили противоправное действие. Гм проинформирован");
					Util.handleIllegalPlayerAction(player, "Игрок " + player.getName() + " пытался выучить недопустимый скил!!!",
						IllegalPlayerAction.PUNISH_KICK);
					return;
				}
				if (player.getClan().getReputationScore() >= repCost)
				{
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						if (!player.destroyItemByItemId("Consume", itemId, 1, trainer, false))
						{
							// Haven't spellbook
							player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
							return;
						}

						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(itemId);
						sm.addNumber(1);
						sendPacket(sm);
						sm = null;
					}
				}
				else
				{
					player.sendPacket(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
					return;
				}

				player.getClan().setReputationScore(player.getClan().getReputationScore() - repCost, true);
				player.getClan().addNewSkill(skill);

				SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				cr.addNumber(repCost);
				player.sendPacket(cr);
				SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
				sm.addSkillName(_id);
				player.sendPacket(sm);
				sm = null;

				player.getClan().broadcastToOnlineMembers(new PledgeSkillList(player.getClan()));

				for (L2PcInstance member : player.getClan().getOnlineMembers(0))
				{
					member.sendSkillList();
				}
				((L2VillageMasterInstance) trainer).showPledgeSkillList(player);
				return;
			}
			default:
			{
				_log.warn("Recived Wrong Packet Data in Aquired Skill - unk1:" + _skillType);
				return;
			}
		}
		player.addSkill(skill, true);
		player.sendSkillList();
		if(_requiredSp>0) {
			player.setSp(player.getSp() - _requiredSp);
			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.SP, player.getSp());
			player.sendPacket(su);
		}

		SystemMessage sm = new SystemMessage(SystemMessageId.LEARNED_SKILL_S1);
		sm.addSkillName(_id);
		player.sendPacket(sm);
		sm = null;

		if (_level > 1)
		{
			L2ShortCut[] allShortCuts = player.getAllShortCuts();
			for (L2ShortCut sc : allShortCuts)
			{
				if (sc.getId() == _id && sc.getType() == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _level, 1);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
			}
		}
		player.sendSkillList();
		if (trainer instanceof L2FishermanInstance)
			((L2FishermanInstance) trainer).showSkillList(player);
		else
			trainer.showSkillList(player, player.getSkillLearningClassId());

		if (_id >= 1368 && _id <= 1372)
		{
			ExStorageMaxCount esmc = new ExStorageMaxCount(player);
			player.sendPacket(esmc);
		}
	}

	@Override
	public String getType()
	{
		return _C__6C_REQUESTAQUIRESKILL;
	}
}