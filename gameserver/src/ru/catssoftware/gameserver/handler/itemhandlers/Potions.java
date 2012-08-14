package ru.catssoftware.gameserver.handler.itemhandlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.handler.IItemHandler;
import ru.catssoftware.gameserver.instancemanager.clanhallsiege.RainbowSpringSiege;
import ru.catssoftware.gameserver.model.L2Effect;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PlayableInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2SummonInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.network.serverpackets.UseSharedGroupItem;
import ru.catssoftware.gameserver.templates.skills.L2EffectType;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;

public class Potions implements IItemHandler
{
	private static final int[]	ITEM_IDS	=
	{
		65,
		725,
		726,
		727,
		728,
		733,
		734,
		735,
		1060,
		1061,
		1073,
		1374,
		1375,
		1539,
		1540,
		5283,
		5591,
		5592,
		6035,
		6036,
		6652,
		6653,
		6654,
		6655,
		8193,
		8194,
		8195,
		8196,
		8197,
		8198,
		8199,
		8200,
		8201,
		8202,
		8600,
		8601,
		8602,
		8603,
		8604,
		8605,
		8606,
		8607,
		8608,
		8609,
		8610,
		8611,
		8612,
		8613,
		8614,
		8786,
		//elixir of life
		8622,
		8623,
		8624,
		8625,
		8626,
		8627,
		//elixir of Strength
		8628,
		8629,
		8630,
		8631,
		8632,
		8633,
		//elixir of cp
		8634,
		8635,
		8636,
		8637,
		8638,
		8639,
		// Bless of Eva
		4679,
		// Blessing of [Fire,Water,Wind,Earth,Darkness,Sanctity]
		7906,
		7907,
		7908,
		7909,
		7910,
		7911,
		// Crystal of [Fire,Water,Wind,Earth,Darkness,Sanctity]
		7912,
		7913,
		7914,
		7915,
		7916,
		7917,
		// HotSpringSiege Potions
		8030,
		8031,
		8032,
		8033
	};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean par)
	{
	}

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		boolean res = false;
		if (playable instanceof L2PcInstance)
			activeChar = (L2PcInstance) playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance) playable).getOwner();
		else
			return;

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}
		if (playable.isAllSkillsDisabled())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if(!FloodProtector.tryPerformAction(activeChar,Protected.USE_POTION )) {
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		int itemId = item.getItemId();
		switch (itemId)
		{
			// Mana potions
			case 726: // mana drug, xml: 2003
				if (!Config.ALT_MANA_POTIONS)
					return;
				res = usePotion(playable, 2003, 1,item); // configurable through xml
				break;
			// till handler implemented
			case 728: // mana_potion, xml: 2005
				if (!Config.ALT_MANA_POTIONS)
					return;
				res = usePotion(playable, 2005, 1,item);
				break;
			// Healing and speed potions
			case 65: // red_potion, xml: 2001
				if (!isUseable(playable, item, 2001))
					return;
				res = usePotion(playable, 2001, 1,item);
				break;
			case 725: // healing_drug, xml: 2002
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2002))
					return;
				res = usePotion(playable, 2002, 1,item);
				break;
			case 727: // _healing_potion, xml: 2032
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2032))
					return;
				res = usePotion(playable, 2032, 1,item);
				break;
			case 733: //_Endeavor potion, xml: 2010
				if (!isUseable(playable, item, 2010))
					return;
				res = usePotion(playable, 2010, 1,item);
				break;
			case 734: // quick_step_potion, xml: 2011
				if (!isUseable(playable, item, 2011))
					return;
				res = usePotion(playable, 2011, 1,item);
				break;
			case 735: // swift_attack_potion, xml: 2012
				if (!isUseable(playable, item, 2012))
					return;
				res = usePotion(playable, 2012, 1,item);
				break;
			case 1060: // lesser_healing_potion,
			case 1073: // beginner's potion, xml:
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2031))
					return;
				res = usePotion(playable, 2031, 1,item);
				break;
			case 1061: // healing_potion, xml: 2032
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2032))
					return;
				res = usePotion(playable, 2032, 1,item);
				break;
			case 1374: // adv_quick_step_potion, xml: 2034
				if (!isUseable(playable, item, 2034))
					return;
				res = usePotion(playable, 2034, 1,item);
				break;
			case 1375: // adv_swift_attack_potion, xml: 2035
				if (!isUseable(playable, item, 2035))
					return;
				res = usePotion(playable, 2035, 1,item);
				break;
			case 1539: // greater_healing_potion, xml: 2037
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2037))
					return;
				res = usePotion(playable, 2037, 1,item);
				break;
			case 1540: // quick_healing_potion, xml: 2038
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2038))
					return;
				res = usePotion(playable, 2038, 1,item);
				break;
			case 5283: // Rice Cake, xml: 2136
				if (!isUseable(playable, L2EffectType.HEAL_OVER_TIME, item, 2136))
					return;
				playable.broadcastPacket(new MagicSkillUse(playable, playable, 2136, 1, 1, 0, false));
				res = usePotion(playable, 2136, 1,item);
				break;
			case 5591: // CP and Greater CP
			case 5592: // Potion
				// elixir of Mental Strength
				if (!isUseable(playable, L2EffectType.COMBAT_POINT_HEAL_OVER_TIME, item, 2166))
					return;
				res = usePotion(playable, 2166, (itemId == 5591) ? 1 : 2,item);
				break;
			case 6035:
			case 6036: // Magic Haste Potion, xml: 2169
				if (!isUseable(playable, item, 2169))
					return;
				res = usePotion(playable, 2169, (itemId == 6035) ? 1 : 2,item);
				break;
			// ELIXIR
			case 8622:
			case 8623:
			case 8624:
			case 8625:
			case 8626:
			case 8627:
			{
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				// elixir of Life
				if (!isUseable(activeChar, item, 2287))
					return;
				byte expIndex = (byte) activeChar.getExpertiseIndex();
				res = usePotion(activeChar, 2287, (expIndex > 5 ? expIndex : expIndex + 1),item);
				break;
			}
			case 8628:
			case 8629:
			case 8630:
			case 8631:
			case 8632:
			case 8633:
			{
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				// elixir of Strength
				if (!isUseable(activeChar, item, 2288))
					return;
				byte expIndex = (byte) activeChar.getExpertiseIndex();
				res = usePotion(activeChar, 2288, (expIndex > 5 ? expIndex : expIndex + 1),item);
				break;
			}
			case 8634:
			case 8635:
			case 8636:
			case 8637:
			case 8638:
			case 8639:
			{
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				// elixir of cp
				if (!isUseable(activeChar, item, 2289))
					return;
				byte expIndex = (byte) activeChar.getExpertiseIndex();
				res = usePotion(activeChar, 2289, (expIndex > 5 ? expIndex : expIndex + 1),item);
				break;
			}
			// Valakas Amulets
			case 6652: // Amulet Protection of Valakas
				res = usePotion(playable, 2231, 1,item);
				break;
			case 6653: // Amulet Flames of Valakas
				res = usePotion(playable, 2233, 1,item);
				break;
			case 6654: // Amulet Flames of Valakas
				res = usePotion(playable, 2233, 2,item);
				break;
			case 6655: // Amulet Slay Valakas
				res = usePotion(playable, 2232, 1,item);
				break;
			// Herbs
			case 8600: // Herb of Life
				res = usePotion(playable, 2278, 1,item);
				break;
			case 8601: // Greater Herb of Life
				res = usePotion(playable, 2278, 2,item);
				break;
			case 8602: // Superior Herb of Life
				res = usePotion(playable, 2278, 3,item);
				break;
			case 8603: // Herb of Mana
				res = usePotion(playable, 2279, 1,item);
				break;
			case 8604: // Greater Herb of Mane
				res = usePotion(playable, 2279, 2,item);
				break;
			case 8605: // Superior Herb of Mane
				res = usePotion(playable, 2279, 3,item);
				break;
			case 8606: // Herb of Strength
				res = usePotion(playable, 2280, 1,item);
				break;
			case 8607: // Herb of Magic
				res = usePotion(playable, 2281, 1,item);
				break;
			case 8608: // Herb of Atk. Spd.
				res = usePotion(playable, 2282, 1,item);
				break;
			case 8609: // Herb of Casting Spd.
				res = usePotion(playable, 2283, 1,item);
				break;
			case 8610: // Herb of Critical Attack
				res = usePotion(playable, 2284, 1,item);
				break;
			case 8611: // Herb of Speed
				res = usePotion(playable, 2285, 1,item);
				break;
			case 8612: // Herb of Warrior
				res = usePotion(playable, 2280, 1,item);// Herb of Strength
				res = usePotion(playable, 2282, 1,item);// Herb of Atk. Spd
				res = usePotion(playable, 2284, 1,item);// Herb of Critical Attack
				break;
			case 8613: // Herb of Mystic
				res = usePotion(playable, 2281, 1,item);// Herb of Magic
				res = usePotion(playable, 2283, 1,item);// Herb of Casting Spd.
				break;
			case 8614: // Herb of Warrior
				res = usePotion(playable, 2278, 3,item);// Superior Herb of Life
				res = usePotion(playable, 2279, 3,item);// Superior Herb of Mana
				break;
			case 8193: // Fisherman's Potion - Green
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 3)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 1,item);
				break;
			case 8194: // Fisherman's Potion - Jade
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 6)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 2,item);
				break;
			case 8195: // Fisherman's Potion - Blue
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 9)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 3,item);
				break;
			case 8196: // Fisherman's Potion - Yellow
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 12)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 4,item);
				break;
			case 8197: // Fisherman's Potion - Orange
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 15)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 5,item);
				break;
			case 8198: // Fisherman's Potion - Purple
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 18)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 6,item);
				break;
			case 8199: // Fisherman's Potion - Red
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 21)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 7,item);
				break;
			case 8200: // Fisherman's Potion - White
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (activeChar.getSkillLevel(1315) <= 24)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				res = usePotion(activeChar, 2274, 8,item);
				break;
			case 8201: // Fisherman's Potion - Black
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				res = usePotion(activeChar, 2274, 9,item);
				break;
			case 8202: // Fishing Potion
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				res = usePotion(activeChar, 2275, 1,item);
				break;
			case 4679: // Bless of Eva
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (!isUseable(activeChar, item, 2076))
					return;
				res = usePotion(activeChar, 2076, 1,item);
				break;
			case 8786: // Primeval Potion
				if (!(playable instanceof L2PcInstance))
				{
					itemNotForPets(activeChar);
					return;
				}
				if (!isUseable(activeChar, item, 2305))
					return;
				res = usePotion(activeChar, 2305, 1,item);
				break;
			case 7906:
				if (!isUseable(playable, item, 2248))
					return;
				res = usePotion(playable, 2248, 1,item);
				break;
			case 7907:
				if (!isUseable(playable, item, 2249))
					return;
				res = usePotion(playable, 2249, 1,item);
				break;
			case 7908:
				if (!isUseable(playable, item, 2250))
					return;
				res = usePotion(playable, 2250, 1,item);
				break;
			case 7909:
				if (!isUseable(playable, item, 2251))
					return;
				res = usePotion(playable, 2251, 1,item);
				break;
			case 7910:
				if (!isUseable(playable, item, 2252))
					return;
				res = usePotion(playable, 2252, 1,item);
				break;
			case 7911:
				if (!isUseable(playable, item, 2253))
					return;
				res = usePotion(playable, 2253, 1,item);
			case 7912:
				if (!isUseable(playable, item, 2254))
					return;
				res = usePotion(playable, 2254, 1,item);
				break;
			case 7913:
				if (!isUseable(playable, item, 2255))
					return;
				res = usePotion(playable, 2255, 1,item);
				break;
			case 7914:
				if (!isUseable(playable, item, 2256))
					return;
				res = usePotion(playable, 2256, 1,item);
				break;
			case 7915:
				if (!isUseable(playable, item, 2257))
					return;
				res = usePotion(playable, 2257, 1,item);
				break;
			case 7916:
				if (!isUseable(playable, item, 2258))
					return;
				res = usePotion(playable, 2258, 1,item);
				break;
			case 7917:
				if (!isUseable(playable, item, 2259))
					return;
				res = usePotion(playable, 2259, 1,item);
				break;
			case 8030:
			case 8031:
			case 8032:
			case 8033:
				res = RainbowSpringSiege.getInstance().usePotion(playable,itemId);
				break;
			default:
		}
		if (res) {
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		playable.actionFail();
	}

	private boolean isEffectReplaceable(L2PlayableInstance playable, L2EffectType effectType, L2ItemInstance item)
	{
		L2Effect[] effects = playable.getAllEffects();

		if (effects == null)
			return true;

		L2PcInstance activeChar =  ((playable instanceof L2PcInstance) ? ((L2PcInstance) playable) : ((L2Summon) playable).getOwner());

		for (L2Effect e : effects)
		{
			if (e.getEffectType() == effectType)
			{
				// One can reuse pots after 2/3 of their duration is over.
				// It would be faster to check if its > 10 but that would screw custom pot durations...
				if (e.getElapsedTaskTime() > (e.getTotalTaskTime() * 2 / 3))
					return true;
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addItemName(item);
				activeChar.sendPacket(sm);
				return false;
			}
		}

		return true;
	}

	private boolean isUseable(L2PlayableInstance playable, L2ItemInstance item, int skillid)
	{
		L2PcInstance activeChar =  ((playable instanceof L2PcInstance) ? ((L2PcInstance) playable) : ((L2Summon) playable).getOwner());
		if (activeChar.isSkillDisabled(skillid))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
			sm.addItemName(item);
			activeChar.sendPacket(sm);
			return false;
		}

		return true;
	}

	private boolean isUseable(L2PlayableInstance playable, L2EffectType effectType, L2ItemInstance item, int skillid)
	{
		return (isEffectReplaceable(playable, effectType, item) && isUseable(playable, item, skillid));
	}

	public boolean usePotion(L2PlayableInstance activeChar, int magicId, int level, L2ItemInstance item)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
		if (skill != null)
		{
			// Return false if potion is in reuse
			// so is not destroyed from inventory
			if (activeChar.isSkillDisabled(skill.getId()))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
				return false;
			}
			activeChar.doSimultaneousCast(skill);
			if(skill.getReuseDelay()>500)
				activeChar.sendPacket(new UseSharedGroupItem(item.getItemId(),skill.getId(),skill.getReuseDelay(),skill.getReuseDelay() ));

			if (activeChar instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) activeChar;
				//only for Heal potions
				if (magicId == 2031 || magicId == 2032 || magicId == 2037)
					player.shortBuffStatusUpdate(magicId, level, 15);
				// Summons should be affected by herbs too, self time effect is handled at L2Effect constructor
				else if (((magicId > 2277 && magicId < 2286) || (magicId >= 2512 && magicId <= 2514)) && (player.getPet() instanceof L2SummonInstance))
					player.getPet().doSimultaneousCast(skill);
				
				if (!(player.isSitting() && !skill.isPotion()))
					return true;
			}
			else if (activeChar instanceof L2PetInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.PET_USES_S1);
				sm.addString(skill.getName());
				((L2PetInstance) activeChar).getOwner().sendPacket(sm);
				return true;
			}
		}
		return false;
	}

	private void itemNotForPets(L2PcInstance activeChar)
	{
		activeChar.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}