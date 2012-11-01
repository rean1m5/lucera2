package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2ControllableMobInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.network.Disconnection;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.SetSummonRemainTime;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.network.serverpackets.StatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.UserInfo;
import ru.catssoftware.gameserver.util.PcAction;

 /**
  * @author m095
  * @version 1.0
  */

public class control extends gmHandler
{
	private static final String[] commands =
	{
		"control",
		"kick",
		"gmcancel",
		"kill",
		"invul",
		"setinvul",
		"heal",
		"start_regen",
		"stop_regen",
		"nokarma",
		"setkarma",
		"fullfood",
		"sethero",
		"setnoble",
		"setfame",
		"remclanwait",
		"setcp",
		"sethp",
		"setmp"
	};
	
	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];

		if (command.equals("control"))
		{
			methods.showMenuPage(admin, "control.htm");
			return;
		}
		else if (command.equals("remclanwait"))
		{
			L2Object target = admin.getTarget();
			L2PcInstance player = null;

			if (target != null && target.isPlayer())
				player = (L2PcInstance) target;
			else
				return;

			if (player.getClan() == null)
			{
				player.setClanJoinExpiryTime(0);
				player.sendMessage("GM удалил время штрафа на вступление в клан");
				admin.sendMessage("Штраф успешно удален");
			}
			else
				admin.sendMessage("Игрок " + player.getName() + " состоит в клане");
		}
		else if (command.equals("setfame"))
		{
			try
			{
				int fame = Integer.parseInt(params[1]);
				L2Object target = admin.getTarget();

				if (target != null && target.isPlayer())
				{
					L2PcInstance player = (L2PcInstance) target;
					player.setFame(fame);
					player.sendPacket(new UserInfo(player));
					player.sendMessage("Количество Fame очков измнено GM'ом. Текущее значение составляет " + fame);
					admin.sendMessage("Уровень репутации Fame успешно изменен");
				}
				else
					admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			}
			catch (Exception e)
			{
			}			
		}
		else if (command.equals("setnoble"))
		{
			L2Object target = admin.getTarget();

			if (target != null && target.isPlayer())
			{
				L2PcInstance player = (L2PcInstance) target;

				if (player.isNoble())
					player.sendMessage("Вы потеряли статус дворянина.");
				else
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
					player.sendMessage("Вы получили статус дворянина.");
				}
				player.setNoble(!player.isNoble());
				player.broadcastUserInfo(true);
			}
		}	
		else if (command.equals("sethero"))
		{
			L2Object target = admin.getTarget();

			if (target != null && target.isPlayer())
			{
				L2PcInstance player = (L2PcInstance) target;
				if (player.isHero())
					player.sendMessage("Вы потеряли статус героя");
				else
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
					player.sendMessage("Поздравляем, вы стали героем");
				}
				PcAction.admGiveHero(player, player.isHero());
				player.setHero(!player.isHero());
				player.broadcastUserInfo(true);
			}
		}
		else if (command.equals("fullfood"))
		{
			L2Object target = admin.getTarget();
			if (target instanceof L2PetInstance)
			{
				L2PetInstance targetPet = (L2PetInstance) target;
				targetPet.setCurrentFed(targetPet.getMaxFed());
				targetPet.getOwner().sendPacket(new SetSummonRemainTime(targetPet.getMaxFed(), targetPet.getCurrentFed()));
				admin.sendMessage("Вы успешно накормили питомца");
			}
			else
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else if (command.equals("nokarma"))
		{
			setTargetKarma(admin, 0);
			return;
		}
		else if (command.equals("setkarma"))
		{
			try
			{
				int karma = Integer.parseInt(params[1]);
					setTargetKarma(admin, karma);
			}
			catch (Exception e)
			{
			}
			return;
		}
		else if (command.equals("start_regen"))
		{
			L2Object obj = admin.getTarget();
			if (obj == null)
				obj = admin;
			
			if (obj.isPlayer())
				((L2PcInstance)obj).getStatus().startHpMpRegeneration();
			admin.sendMessage("Восстановление Hp восстановлено");
		}
		else if (command.equals("stop_regen"))
		{
			L2Object obj = admin.getTarget();
			if (obj == null)
				obj = admin;
			
			if (obj.isPlayer())
				((L2PcInstance)obj).getStatus().stopHpMpRegeneration();
			admin.sendMessage("Восстановление Hp приостановлено");
		}
		else if (command.equals("invul"))
		{
			handleInvul(admin);
			return;
		}
		else if (command.equals("heal"))
		{
			if (params.length == 1)
			{
				if (admin.getTarget() != null && admin.getTarget() instanceof L2Character)
				{
					handleHeal((L2Character) admin.getTarget());
					admin.sendMessage("Вы вылечили " + admin.getTarget().getName());
				}
				return;
			}
			else
			{
				try
				{
					try
					{
						int radius = Integer.parseInt(params[1]);
						for (L2Character cha : admin.getKnownList().getKnownCharactersInRadius(radius))
						{
							if (cha == null || cha.isAlikeDead())
								continue;
							handleHeal(cha);
						}
						admin.sendMessage("Вылечены все в радиусе " + radius);
					}
					catch (NumberFormatException e)
					{
						L2PcInstance target = L2World.getInstance().getPlayer(params[1]);
						if (target != null)
						{
							handleHeal(target);
							admin.sendMessage("Вы вылечили " + target.getName());
						}
					}
				}
				catch (StringIndexOutOfBoundsException e)
				{
					admin.sendMessage("Введите ник или радиус");
				}
				return;
			}
		}
		else if (command.equals("setinvul"))
		{
			L2Object target = admin.getTarget();
			if (target.isPlayer())
				handleInvul((L2PcInstance) target);
			return;
		}
		else if (command.equals("kill"))
		{
			if (params.length > 1)
			{
				L2PcInstance player = L2World.getInstance().getPlayer(params[1]);
				if (player != null)
				{
					if (params.length > 2)
					{
						try
						{
							int radius = Integer.parseInt(params[2]);
							for (L2Character knownChar : player.getKnownList().getKnownCharactersInRadius(radius))
							{
								if (knownChar == null || knownChar instanceof L2ControllableMobInstance || knownChar == admin)
									continue;
								kill(admin, knownChar);
							}
							admin.sendMessage("Убиты все в радиусе " + radius + " игрока " + player.getName());
							return;
						}
						catch (NumberFormatException e)
						{
							admin.sendMessage("Раидус задан неверно");
							return;
						}
					}
					kill(admin, player);
				}
				else
				{
					try
					{
						int radius = Integer.parseInt(params[1]);

						for (L2Character knownChar : admin.getKnownList().getKnownCharactersInRadius(radius))
						{
							if (knownChar == null || knownChar instanceof L2ControllableMobInstance || knownChar == admin)
								continue;
							kill(admin, knownChar);
						}

						admin.sendMessage("Убиты все в радиусе " + radius);
						return;
					}
					catch (NumberFormatException e)
					{
						admin.sendMessage("Раидус задан неверно");
						return;
					}
				}
			}
			else
			{
				L2Object obj = admin.getTarget();
				if (obj == null || obj instanceof L2ControllableMobInstance || !(obj instanceof L2Character))
					admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else
				{
					kill(admin, (L2Character) obj);
					admin.sendMessage("Вы убили " + obj.getName());
				}
			}
			return;
		}
		else if (command.equals("kick"))
		{
			L2PcInstance player = null;
			if (params.length > 1)
				player = L2World.getInstance().getPlayer(params[1]);
	
			if (player == null)
			{
				L2Object obj = admin.getTarget();
				if (obj != null && obj.isPlayer())
					player = (L2PcInstance)obj;
			}

			if (player != null)
			{
				String kickName = player.getName();

				// Фикс кика из игры для Оффлайн Трэйдеров
				if (player.isOfflineTrade())
				{
					player.setOfflineTrade(false);
					player.standUp();
				}
				player.sendMessage("Вы удалены из игры администрацией");
				new Disconnection(player).defaultSequence(false);
				admin.sendMessage("Игрок " + kickName + " удален из игры");
			}
			else
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		else if (command.equals("gmcancel"))
		{
			try
			{
				L2Object target = admin.getTarget();
				if (target != null && target instanceof L2Character)
				{
					((L2Character)target).stopAllEffects();
					admin.sendMessage("Отмена эффектов " + ((L2Character)target).getName() + " завершена");
					return;
				}
				else if (params.length > 1)
				{
					int radius = 0;
					radius = Integer.parseInt(params[1]);
					if (radius > 0)
					{
						for (L2PcInstance temp : admin.getKnownList().getKnownPlayersInRadius(radius))
						{
							if (temp != null)
							{
								temp.stopAllEffects();
							}
						}
						admin.sendMessage("Всем игрокам в радиусе " + radius + " отменены эффекты");
						return;
					}
				}
			}
			catch (Exception e)
			{
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}
			admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		else if (command.startsWith("set"))
		{
			L2Object obj = admin.getTarget();
			if (obj == null)
				obj = admin;
			if (!(obj instanceof L2Character))
			{
				admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}
			try
			{
				int val = Integer.parseInt(params[1]);
				if (command.equals("setcp"))
				{
					if (obj.isPlayer())
					{
						((L2PcInstance)obj).getStatus().setCurrentCp(val);
						admin.sendMessage("Уровень CP изменен");
					}
					else
						admin.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				else if (command.equals("sethp"))
				{
					((L2Character)obj).getStatus().setCurrentHp(val);
					admin.sendMessage("Уровень HP изменен");
				}
				else if (command.equals("setmp"))
				{
					((L2Character)obj).getStatus().setCurrentMp(val);
					admin.sendMessage("Уровень MP изменен");
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Задайте аргумент");
			}
			return;
		}
	}
	
	private void setTargetKarma(L2PcInstance activeChar, int karma)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target != null && target.isPlayer())
			player = (L2PcInstance) target;
		else
			return;

		if (karma >= 0)
		{
			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.KARMA, karma);
			player.setKarma(karma);
			player.sendPacket(su);
			if (player != activeChar)
				player.sendMessage("GM изменил количество Вашей кармы. Значение крамы составило " + karma);
			activeChar.sendMessage("Значение кармы игрока " + player.getName() + " успешно изменено");
		}
		else
			activeChar.sendMessage("Значение кармы должно быть выше 0");
	}

	/**
	 * Убить L2Character
	 * @param activeChar
	 * @param target
	 */
	private void kill(L2PcInstance activeChar, L2Character target)
	{
		if (target.isPlayer())
		{
			if (!((L2PcInstance) target).isGM())
				target.stopAllEffects();
			target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar);
		}
		else
		{
			if (target.isInvul())
				target.setIsInvul(false);
			if (target.isChampion())
				target.reduceCurrentHp(target.getMaxHp() * Config.CHAMPION_HP + 1, activeChar);
			else
				target.reduceCurrentHp(target.getMaxHp() + 1, activeChar);
		}
	}
	
	/**
	 * Включения инвула, отключение
	 * @param activeChar
	 */
	private void handleInvul(L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;

		String text;
		if (activeChar.isInvul())
		{
			activeChar.setIsInvul(false);
			if (activeChar.getPet() != null)
				activeChar.getPet().setIsInvul(false);

			text = activeChar.getName() + " становится уязвимым";
		}
		else
		{
			activeChar.setIsInvul(true);
			if (activeChar.getPet() != null)
				activeChar.getPet().setIsInvul(true);

			text = activeChar.getName() + " становится неуязвимым";
		}
		activeChar.sendMessage(text);
	}
	
	/**
	 * Хеал
	 * @param target
	 */
	private void handleHeal(L2Character target)
	{
		target.getStatus().setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
		if (target.isPlayer())
			target.getStatus().setCurrentCp(target.getMaxCp());
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}