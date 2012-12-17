package ru.catssoftware.gameserver.network.clientpackets;


import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.ai.L2SummonAI;
import ru.catssoftware.gameserver.datatables.PetSkillsTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.RecipeShopManageList;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.util.FloodProtector;
import ru.catssoftware.gameserver.util.FloodProtector.Protected;
import ru.catssoftware.gameserver.util.PcAction;

public class RequestActionUse extends L2GameClientPacket
{
	private static final String	_C__45_REQUESTACTIONUSE	= "[C] 45 RequestActionUse";
	private int					_actionId;
	private boolean				_ctrlPressed, _shiftPressed;

	public static final int ACTION_SIT_STAND = 0;
	public static final int ACTION_MOUNT = 38;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = (readD() == 1);
		_shiftPressed = (readC() == 1);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;
		if (activeChar.isAlikeDead() || activeChar.isOutOfControl() || !getClient().checkKeyProtection())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Summon pet = activeChar.getPet();
		L2Object target = activeChar.getTarget();
		switch (_actionId)
		{
			case ACTION_SIT_STAND:
				if (activeChar.getMountType() != 0)
					break;
				if (target != null &&
						target instanceof L2StaticObjectInstance &&
						!activeChar.isSitting()) {
					if (!((L2StaticObjectInstance) target).useThrone(activeChar))
						activeChar.sendMessage("Вы не можете сесть на трон.");
					break;
				}

				if (activeChar.isSitting())
				{
					activeChar.standUp(false); // false - No forced standup but user requested - Checks if animation already running.
					if (activeChar.getObjectSittingOn() != null)
					{
						activeChar.getObjectSittingOn().setBusyStatus(null);
						activeChar.setObjectSittingOn(null);
					}
				}
				else
					activeChar.sitDown(false); // false - No forced sitdown but user requested - Checks if animation already running.
				break;
			case 1:
				if (activeChar.isRunning())
					activeChar.setWalking();
				else
					activeChar.setRunning();
				break;
			case 10:
				// Private Store Sell
				activeChar.tryOpenPrivateSellStore(false);
				break;
			case 15:
			case 21: // pet follow/stop
				if (pet instanceof L2SiegeSummonInstance && ((L2SiegeSummonInstance)pet).isOnSiegeMode())
				{
					activeChar.sendMessage("Невозможно в осадном режиме.");
				}
				else if (pet != null && !pet.isOutOfControl())
					((L2SummonAI) pet.getAI()).notifyFollowStatusChange();
				if(pet instanceof L2SiegeSummonInstance)
					((L2SiegeSummonInstance)pet).resetSiegeModeChange();
				break;
			case 16:
			case 22: // pet attack
				if (target != null && pet != null && pet != target && activeChar != target && !pet.checkStartAttacking() && !pet.isOutOfControl())
				{
					if (pet instanceof L2PetInstance && (pet.getLevel() - activeChar.getLevel() > 20))
					{
						activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
						return;
					}
					if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					{
						ActionFailed();
						return;
					}
					if (!activeChar.allowPeaceAttack() && L2Character.isInsidePeaceZone(pet, target))
					{
						if (!activeChar.isInFunEvent() || !target.isInFunEvent())
						{
							activeChar.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
							return;
						}
					}
					if (pet.getNpcId() == 12564 || pet.getNpcId() == 12621)
					{
						ActionFailed();
						return;
					}
					if (target.isAutoAttackable(activeChar) || _ctrlPressed)
					{
						if (target.isDoor())
						{
							if (target.getDoor().isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
						else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
					}
					else
					{
						pet.setFollowStatus(false);
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
					}
				}
				break;
			case 17:
			case 23: // pet - cancel action
				if (pet != null && !pet.isMovementDisabled() && !pet.isOutOfControl())
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);

				break;
			case 19: // pet unsummon
				if (pet != null && !pet.isOutOfControl())
				{
					//returns pet to control item
					if (pet.isDead())
						activeChar.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
					else if (pet.isAttackingNow() || pet.isRooted())
						activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
					else
					{
						// if it is a pet and not a summon
						if (pet instanceof L2PetInstance)
						{
							if (!pet.isHungry())
							{
								if (pet.isInCombat())
									activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
								else
									pet.unSummon(activeChar);
							}
							else
								activeChar.sendPacket(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS);
						}
					}
				}
				break;
			case ACTION_MOUNT: // pet mount
				activeChar.mountPlayer(pet);
				break;
			case 12:
				useSocialAction(2);
				break;
			case 13:
				useSocialAction(3);
				break;
			case 14:
				useSocialAction(4);
				break;
			case 24:
				useSocialAction(6);
				break;
			case 25:
				useSocialAction(5);
				break;
			case 26:
				useSocialAction(7);
				break;
			case 28:
				activeChar.tryOpenPrivateBuyStore();
				break;
			case 29:
				useSocialAction(8);
				break;
			case 30:
				useSocialAction(9);
				break;
			case 31:
				useSocialAction(10);
				break;
			case 32: // Wild Hog Cannon - Mode Change
				if (pet instanceof L2SiegeSummonInstance)
					((L2SiegeSummonInstance)pet).changeSiegeMode();
				break;
			case 33:
				useSocialAction(11);
				break;
			case 34:
				useSocialAction(12);
				break;
			case 35:
				useSocialAction(13);
				break;
			case 36: // Soulless - Toxic Smoke
				useSkill(4259);
				break;
			case 37: // Manufacture - Dwarven
				if (activeChar.isAlikeDead())
				{
					ActionFailed();
					return;
				}
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo(true);
				}
				if (activeChar.isSitting())
					activeChar.standUp();

				if (activeChar.getCreateList() == null)
					activeChar.setCreateList(new L2ManufactureList());

				activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
				break;
			case 39: // Soulless - Parasite Burst
				useSkill(4138);
				break;
			case 41: // Wild Hog Cannon - Attack
				if( target != null && (target instanceof L2DoorInstance || target instanceof L2SiegeFlagInstance))
				{
					if (pet instanceof L2SiegeSummonInstance && ((L2SiegeSummonInstance)pet).isOnSiegeMode())
						useSkill(4230);
					else
						activeChar.sendMessage("Возможно только в осадном режиме.");
				}
				else
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				break;
			case 42: // Kai the Cat - Self Damage Shield
				useSkill(4378, activeChar);
				break;
			case 43: // Unicorn Merrow - Hydro Screw
				useSkill(4137);
				break;
			case 44: // Big Boom - Boom Attack
				useSkill(4139, activeChar.getTarget());
				break;
			case 45: // Unicorn Boxer - Master Recharge
				useSkill(4025, activeChar);
				break;
			case 46: // Mew the Cat - Mega Storm Strike
				useSkill(4261);
				break;
			case 47: // Silhouette - Steal Blood
				useSkill(4260);
				break;
			case 48: // Mechanic Golem - Mech. Cannon
				useSkill(4068);
				break;
			case 51: // Manufacture -  non-dwarfen
				if (activeChar.isAlikeDead())
				{
					ActionFailed();
					return;
				}
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo(true);
				}
				if (activeChar.isSitting())
					activeChar.standUp();

				if (activeChar.getCreateList() == null)
					activeChar.setCreateList(new L2ManufactureList());

				activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			case 52: // unsummon
				if (pet != null && pet instanceof L2SummonInstance)
				{
					if (pet.isOutOfControl())
						activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					else if (pet.isAttackingNow() || pet.isInCombat())
						activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
					else if (pet.isDead())
						activeChar.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
					else
						pet.unSummon(activeChar);
				}
				break;
			case 53: // move to target
				if (pet instanceof L2SiegeSummonInstance && ((L2SiegeSummonInstance)pet).isOnSiegeMode())
				{
						activeChar.sendMessage("Невозможно в осадном режиме.");
						break;
				}
				else if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isOutOfControl())
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ(), 0));
				}
				if(pet instanceof L2SiegeSummonInstance)
					((L2SiegeSummonInstance)pet).resetSiegeModeChange();
				break;
			case 54: // move to target hatch/strider
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isOutOfControl())
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 61: // Private Store Package Sell
				activeChar.tryOpenPrivateSellStore(true);
				break;
			case 62:
				useSocialAction(14);
				break;
			case 64: // Teleport bookmark button
				break;
			case 65:
				// Bot report Button.
				L2PcInstance player = null;

				L2Object botPlayer = activeChar.getTarget();
				if (botPlayer != null && botPlayer.isPlayer() & botPlayer != activeChar)
				{
					player = (L2PcInstance) botPlayer;
					if (!FloodProtector.tryPerformAction(activeChar, Protected.BOT_REPORT))
					{
						activeChar.sendMessage("Защита от флуда. Попробуйте позже.");
						ActionFailed();
						return;
					}
					activeChar.sendMessage("Ваше уведомление учтено. Спасибо!");
					PcAction.botMessage(activeChar, player);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				break;
			case 66:
				useSocialAction(999915);
				break;
			case 96: // Quit Party Command Channel
				break;
			case 97: // Request Party Command Channel Info
				break;
			case 1000: // Siege Golem - Siege Hammer
				if (target instanceof L2DoorInstance)
					useSkill(4079);
				break;
			case 1001:
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
				useSkill(4710);
				break;
			case 1004: // Wind Hatchling/Strider - Wild Defense
				useSkill(4711, activeChar);
				break;
			case 1005: // Star Hatchling/Strider - Bright Burst
				useSkill(4712);
				break;
			case 1006: // Star Hatchling/Strider - Bright Heal
				useSkill(4713, activeChar);
				break;
			case 1007: // Cat Queen - Blessing of Queen
				useSkill(4699, activeChar);
				break;
			case 1008: // Cat Queen - Gift of Queen
				useSkill(4700, activeChar);
				break;
			case 1009: // Cat Queen - Cure of Queen
				useSkill(4701);
				break;
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				useSkill(4702, activeChar);
				break;
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				useSkill(4703, activeChar);
				break;
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				useSkill(4704);
				break;
			case 1013: // Nightshade - Curse of Shade
				useSkill(4705);
				break;
			case 1014: // Nightshade - Mass Curse of Shade
				//useSkill(4706, activeChar);
				useSkill(4706);
				break;
			case 1015: // Nightshade - Shade Sacrifice
				useSkill(4707);
				break;
			case 1016: // Cursed Man - Cursed Blow
				useSkill(4709);
				break;
			case 1017: // Cursed Man - Cursed Strike/Stun
				useSkill(4708);
				break;
			case 1031: // Feline King - Slash
				useSkill(5135);
				break;
			case 1032: // Feline King - Spinning Slash
				useSkill(5136);
				break;
			case 1033: // Feline King - Grip of the Cat
				useSkill(5137);
				break;
			case 1034: // Magnus the Unicorn - Whiplash
				useSkill(5138);
				break;
			case 1035: // Magnus the Unicorn - Tridal Wave
				useSkill(5139);
				break;
			case 1036: // Spectral Lord - Corpse Kaboom
				useSkill(5142);
				break;
			case 1037: // Spectral Lord - Dicing Death
				useSkill(5141);
				break;
			case 1038: // Spectral Lord - Force Curse
				useSkill(5140);
				break;
			case 1039: // Swoop Cannon - Cannon Fodder
				if (!(target instanceof L2DoorInstance))
					useSkill(5110);
				break;
			case 1040: // Swoop Cannon - Big Bang
				if (!(target instanceof L2DoorInstance))
					useSkill(5111);
				break;
			case 1041: // Great Wolf - Bite Attack
				useSkill(5442);
				break;
			case 1042: // Great Wolf - Maul
				useSkill(5444);
				break;
			case 1043: // Great Wolf - Cry of the Wolf
				useSkill(5443);
				break;
			case 1044: // Great Wolf - Awakening
				useSkill(5445);
				break;
			case 1045: // Great Wolf - Howl
				useSkill(5584);
				break;
			case 1046: // Strider - Roar
				useSkill(5585);
				break;
			case 1047: // Divine Beast - Bite
				useSkill(5580);
				break;
			case 1048: // Divine Beast - Stun Attack
				useSkill(5581);
				break;
			case 1049: // Divine Beast - Fire Breath
				useSkill(5582);
				break;
			case 1050: // Divine Beast - Roar
				useSkill(5583);
				break;
			case 1051: // Bless the Body (Feline Queen)
				useSkill(5638);
				break;
			case 1052: // Bless the Soul (Feline Queen)
				useSkill(5639);
				break;
			case 1053: // Haste (Feline Queen)
				useSkill(5640);
				break;
			case 1054: // Acumen (Seraphim Unicorn)
				useSkill(5643);
				break;
			case 1055: // Clarity (Seraphim Unicorn)
				useSkill(5647);
				break;
			case 1056: // Empower (Seraphim Unicorn)
				useSkill(5648);
				break;
			case 1057: // Wild Magic (Seraphim Unicorn)
				useSkill(5646);
				break;
			case 1058: // Death Whisper (Nightshade)
				useSkill(5652);
				break;
			case 1059: // Focus (Nightshade)
				useSkill(5653);
				break;
			case 1060: // Guidance (Nightshade)
				useSkill(5654);
				break;
			case 1061: // CT 2.2 SpecialPet action
				useSkill(5745); // Death blow
				break;
			case 1062: // CT 2.2 SpecialPet action
				useSkill(5746); // Double attack
				break;
			case 1063: // CT 2.2 SpecialPet action
				useSkill(5747); // Spin attack
				break;
			case 1064: // CT 2.2 SpecialPet action
				useSkill(5748); // Meteor Shower
				break;
			case 1065: // CT 2.2 SpecialPet action
				useSkill(5753); // Awakening
				break;
			case 1066: // CT 2.2 SpecialPet action
				useSkill(5749); // Thunder Bolt
				break;
			case 1067: // CT 2.2 SpecialPet action
				useSkill(5750); // Flash
				break;
			case 1068: // CT 2.2 SpecialPet action
				useSkill(5751); // Lightning Wave
				break;
			case 1069: // CT 2.2 SpecialPet action
				useSkill(5752); // Flare
				break;
			case 1070: // CT 2.2 SpecialPet action
				useSkill(5771); // Buff control
				break;
			case 1071: // CT 2.2 SpecialPet action
				useSkill(5761); // Power Strike
				break;
			default:
				activeChar.sendMessage("Команда не обработана. ID: " + _actionId);
		}
	}

	private void useSkill(int skillId, L2Object target)
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Summon activeSummon = activeChar.getPet();

		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendMessage("Запрещено использование во время торговли.");
			return;
		}

		if (activeSummon != null && !activeSummon.isOutOfControl())
		{
			if (activeSummon instanceof L2PetInstance && (activeSummon.getLevel() - activeChar.getLevel() > 20))
			{
				activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
				return;
			}
			int lvl = PetSkillsTable.getInstance().getAvailableLevel(activeSummon, skillId);
			if (lvl == 0)
				return;

			L2Skill skill = SkillTable.getInstance().getInfo(skillId, lvl);
			if (skill == null)
				return;
			if (skill.isOffensive() && activeChar == target)
				return;

			activeSummon.setTarget(target);
			activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
	}

	private void useSocialAction(int id)
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;

		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			return;
		}

		if (player.getPrivateStoreType() == 0 && player.getActiveRequester() == null && !player.isAlikeDead() && (!player.isAllSkillsDisabled()
		|| player.isInDuel()) && !player.isCastingNow() && !player.isCastingSimultaneouslyNow() && player.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			player.broadcastPacket(new SocialAction(player.getObjectId(), id));
	}

	private void useSkill(int skillId)
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		useSkill(skillId, activeChar.getTarget());
	}

	@Override
	public String getType()
	{
		return _C__45_REQUESTACTIONUSE;
	}
}