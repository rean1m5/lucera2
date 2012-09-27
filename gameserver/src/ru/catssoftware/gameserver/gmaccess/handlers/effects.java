package ru.catssoftware.gameserver.gmaccess.handlers;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.gmaccess.gmHandler;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2ChestInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.Earthquake;
import ru.catssoftware.gameserver.network.serverpackets.ExRedSky;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.PlaySound;
import ru.catssoftware.gameserver.network.serverpackets.SSQInfo;
import ru.catssoftware.gameserver.network.serverpackets.SocialAction;
import ru.catssoftware.gameserver.network.serverpackets.StopMove;
import ru.catssoftware.gameserver.network.serverpackets.SunRise;
import ru.catssoftware.gameserver.network.serverpackets.SunSet;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;
import ru.catssoftware.gameserver.skills.AbnormalEffect;

public class effects extends gmHandler
{
	private static final String[] commands =
	{
		"vis",
		"invis",	
		"visible",		
		"invisible",
		"earthquake",
		"atmosphere",
		"sounds",
		"play_sounds",
		"play_sound",
		"para",
		"unpara",
		"unpara_all",
		"para_all",		
		"bighead",
		"shrinkhead",
		"gmspeed",
		"social",
		"abnormal",
		"abnormal_menu",
		"social_menu"
	};

	@Override
	public void runCommand(L2PcInstance admin, String... params)
	{
		if (admin == null)
			return;

		final String command = params[0];
		
		if (command.equals("vis") || command.equals("visible"))
		{
			admin.getAppearance().setVisible();
			admin.broadcastUserInfo(true);
			return;
		}
		else if (command.equals("invis") || command.equals("invisible"))
		{
			admin.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			admin.setTarget(admin);
			admin.disableAllSkills();
			admin.getAppearance().setInvisible();
			admin.updateInvisibilityStatus();
			admin.enableAllSkills();
			return;
		}
		else if (command.equals("earthquake"))
		{
			try
			{
				int intensity = Integer.parseInt(params[1]);
				int duration = Integer.parseInt(params[2]);
				Earthquake eq = new Earthquake(admin.getX(), admin.getY(), admin.getZ(), intensity, duration);
				admin.broadcastPacket(eq);
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //earthquake <intensity> <duration>");
			}
			sendHtml(admin, "effects");
			return;
		}
		else if (command.equals("atmosphere"))
		{
			try
			{
				String type = params[1];
				String state = params[2];
				adminAtmosphere(type, state, admin);
			}
			catch (Exception ex)
			{
			}
			sendHtml(admin, "effects");
			return;
		}
		else if (command.equals("sounds"))
		{
			methods.showHelpPage(admin, "songs/songs.htm");
			return;
		}
		else if (command.equals("play_sounds"))
		{
			try
			{
				String cmd ="";
				for (int x=1; x<params.length;x++)
					cmd += (" " + params[x]);
				methods.showHelpPage(admin, "songs/songs" + cmd + ".htm");
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
			return;
		}
		else if (command.startsWith("play_sound"))
		{
			try
			{
				String cmd ="";
				for (int x=1; x<params.length;x++)
					cmd += (" " + params[x]);
				playAdminSound(admin, cmd);
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
			return;
		}
		else if (command.equals("para"))
		{
			String type = "1";
			if (params.length > 1)
				type = params[1];
			try
			{
				L2Object target = admin.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					if (type.equals("1"))
						player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					else
						player.startAbnormalEffect(AbnormalEffect.HOLD_2);
					player.setIsParalyzed(true);
					StopMove sm = new StopMove(player);
					player.sendPacket(sm);
					player.broadcastPacket(sm);
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.equals("unpara"))
		{
			try
			{
				L2Object target = admin.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
					player.setIsParalyzed(false);
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.equals("para_all") || command.equals("unpara_all"))
		{
			boolean para = command.equals("para_all");
			try
			{
				for (L2PcInstance player : admin.getKnownList().getKnownPlayers().values())
				{
					if (!player.isGM())
					{
						if (para)
						{
							player.startAbnormalEffect(AbnormalEffect.HOLD_1);
							player.setIsParalyzed(true);
							player.broadcastPacket(new StopMove(player));
						}
						else
						{
							player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
							player.setIsParalyzed(false);
						}
					}
				}
			}
			catch (Exception e)
			{
			}
			sendHtml(admin, "effects");
			return;
		}
		else if (command.equals("bighead") || command.equals("shrinkhead"))
		{
			try
			{
				L2Object target = admin.getTarget();
				if (target == null)
					target = admin;
				if (target != null && target instanceof L2Character)
				{
					if (command.equals("shrinkhead"))
						((L2Character)target).stopAbnormalEffect(AbnormalEffect.BIG_HEAD);
					else
						((L2Character)target).startAbnormalEffect(AbnormalEffect.BIG_HEAD);
				}
			}
			catch (Exception e)
			{
			}
			sendHtml(admin, "effects");
			return;
		}
		else if (command.equals("gmspeed"))
		{
			try
			{
				int val = Integer.parseInt(params[1]);
				admin.stopSkillEffects(7029);
				if (val == 0 && admin.getFirstEffect(7029) != null)
					admin.sendPacket(new SystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED).addSkillName(7029));
				else if ((val >= 1) && (val <= 4))
				{
					L2Skill gmSpeedSkill = SkillTable.getInstance().getInfo(7029, val);
					admin.doSimultaneousCast(gmSpeedSkill);
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //gmspeed [0-4]");
			}
			finally
			{
				admin.updateEffectIcons();
			}
			return;
		}
		else if (command.equals("social") || command.equals("social_menu"))
		{
			if (command.equals("social_menu"))
			{
				sendHtml(admin, "submenus/social_menu");
				return;
			}

			try
			{
				if (params.length == 3)
				{
					int social = Integer.parseInt(params[1]);
					int radius = Integer.parseInt(params[2]);
					
					for (L2PcInstance pl: admin.getKnownList().getKnownPlayersInRadius(radius))
					{
						if (pl == null || pl.isAlikeDead() || pl.isOfflineTrade() || pl.isTeleporting() || pl.inPrivateMode())
							continue;
						performSocial(social, pl, admin);
					}
					admin.sendMessage("Социальное дествие отправлено всем в радиусе " + radius);
				}
				else if(params.length == 2)
				{
					int social = Integer.parseInt(params[1]);
					L2Object obj = admin.getTarget();
					if(obj==null)
						obj=admin;
					
					performSocial(social, obj, admin);		
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //social [id]");
			}
			sendHtml(admin, "submenus/social_menu");
			return;
		}
		else if (command.equals("abnormal") || command.equals("abnormal_menu"))
		{
			if (command.equals("abnormal_menu"))
			{
				sendHtml(admin, "submenus/abnormal_menu");
				return;
			}

			try
			{
				if (params.length == 3)
				{
					int abnormal = Integer.decode("0x" + params[1]);
					int radius = Integer.parseInt(params[2]);
					
					for (L2PcInstance pl: admin.getKnownList().getKnownPlayersInRadius(radius))
					{
						if (pl == null || pl.isAlikeDead() || pl.isOfflineTrade() || pl.isTeleporting() || pl.inPrivateMode())
							continue;
						performAbnormal(abnormal, pl);
					}
					admin.sendMessage("Эффект отправлен всем в радиусе " + radius);
				}
				else if(params.length == 2)
				{
					int abnormal = Integer.decode("0x" + params[1]);
					L2Object obj = admin.getTarget();
					if(obj==null)
						obj=admin;
					
					performAbnormal(abnormal, obj);		
				}
			}
			catch (Exception e)
			{
				admin.sendMessage("Используйте: //social [id]");
			}
			sendHtml(admin, "submenus/abnormal_menu");
			return;
		}
	}

	/**
	 * @param action bitmask that should be applied over target's abnormal
	 * @param target
	 * @return <i>true</i> if target's abnormal state was affected , <i>false</i> otherwise.
	 */
	private boolean performAbnormal(int action, L2Object target)
	{
		if (target instanceof L2Character)
		{
			L2Character character = (L2Character) target;
			if ((character.getAbnormalEffect() & action) == action)
				character.stopAbnormalEffect(action);
			else
				character.startAbnormalEffect(action);
			return true;
		}
		return false;
	}

	private void performSocial(int action, L2Object target, L2PcInstance admin)
	{
		if (target == null)
			return;

		try
		{
			if (target instanceof L2Character)
			{
				if (target instanceof L2ChestInstance)
				{
					admin.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				if ((target instanceof L2NpcInstance) && (action < 1 || action > 6))
				{
					admin.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				if ((target instanceof L2PcInstance) && (action < 2 || action > 16))
				{
					admin.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return;
				}
				L2Character character = (L2Character) target;
				character.broadcastPacket(new SocialAction(target.getObjectId(), action));
			}
			else
				return;
		}
		catch (Exception e)
		{
		}
		return;
	}

	private void adminAtmosphere(String type, String state, L2PcInstance admin)
	{
		L2GameServerPacket packet = null;

		if (type.equals("signsky"))
		{
			if (state.equals("dawn"))
				packet = new SSQInfo(2);
			else if (state.equals("dusk"))
				packet = new SSQInfo(1);
		}
		else if (type.equals("sky"))
		{
			if (state.equals("night"))
				packet = SunSet.STATIC_PACKET;
			else if (state.equals("day"))
				packet = SunRise.STATIC_PACKET;
			else if (state.equals("red"))
				packet = new ExRedSky(10);
		}
		else
			admin.sendMessage("Используйте: //atmosphere <signsky dawn|dusk>|<sky day|night|red>");
		if (packet != null)
			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
				player.sendPacket(packet);
	}

	private void playAdminSound(L2PcInstance admin, String sound)
	{
		PlaySound _snd = new PlaySound(1, sound);
		admin.sendPacket(_snd);
		admin.broadcastPacket(_snd);
		admin.sendMessage("Playing " + sound + ".");
	}

	private void sendHtml(L2PcInstance admin, String patch)
	{
		String name = (patch + ".htm");
		NpcHtmlMessage html = new NpcHtmlMessage(admin.getObjectId());
		html.setFile("data/html/admin/menus/" + name);
		admin.sendPacket(html);
	}

	@Override
	public String[] getCommandList()
	{
		return commands;
	}
}