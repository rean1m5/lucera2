package ru.catssoftware.gameserver.model.zone;

import java.util.List;

import ru.catssoftware.Config;
import ru.catssoftware.Message;
import ru.catssoftware.gameserver.instancemanager.InstanceManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.entity.Instance;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.network.SystemMessageId;
import ru.catssoftware.gameserver.network.serverpackets.MagicSkillUse;
import ru.catssoftware.gameserver.network.serverpackets.ShowMiniMap;
import ru.catssoftware.gameserver.network.serverpackets.SystemMessage;


public class L2DefaultZone extends L2Zone
{
	public final static int		REASON_OK					= 0;
	public final static int		REASON_MULTIPLE_INSTANCE	= 1;
	public final static int		REASON_INSTANCE_FULL		= 2;
	public final static int		REASON_SMALL_GROUP			= 3;

	private static class InstanceResult
	{
		public int instanceId = 0;
		public int reason = REASON_OK;
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (_onEnterMsg != null && character instanceof L2PcInstance)
			character.sendPacket(_onEnterMsg);

		if (_abnormal > 0)
			character.startAbnormalEffect(_abnormal);

		if (_removeAll)
		{
				character.stopAllEffects();
		}

		if (_applyEnter != null)
		{
			if (!character.isDead())
			{
				for (L2Skill sk : _applyEnter)
				{
					MagicSkillUse msu = new MagicSkillUse(character, character, sk.getId(), sk.getLevel(), 100, 0, sk.isPositive());
					character.broadcastPacket(msu);
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(sk.getId());
					character.sendPacket(sm);
					sk.getEffects(character, character);
				}
			}
		}
		if (_removeEnter != null)
		{
			for (L2Skill sk : _removeEnter)
				character.stopSkillEffects(sk.getId());
		}

		if (_funcTemplates != null)
			character.addStatFuncs(getStatFuncs(character));

		if (_pvp == PvpSettings.ARENA)
		{
			character.setInsideZone(this,FLAG_NOSUMMON, true);
			character.setInsideZone(this,FLAG_PVP, true);
			if (character instanceof L2PcInstance)
				character.sendPacket(SystemMessageId.ENTERED_COMBAT_ZONE);
		}
		else if (_pvp == PvpSettings.PEACE)
		{
			if (Config.ZONE_TOWN != 2)
				character.setInsideZone(this,FLAG_PEACE, true);
		}
		if (_noLanding && character instanceof L2PcInstance)
		{
			character.setInsideZone(this,FLAG_NOLANDING, true);
			if (((L2PcInstance) character).getMountType() == 2)
			{
				character.setInstanceId(0);
				character.teleToLocation(TeleportWhereType.Town);
				character.sendPacket(SystemMessageId.AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_WYVERN);
				((L2PcInstance) character).enteredNoLanding();
			}
		}
		if (_noEscape)
			character.setInsideZone(this,FLAG_NOESCAPE, true);
		if (_noPrivateStore)
			character.setInsideZone(this,FLAG_NOSTORE, true);
		if (_noSummon)
			character.setInsideZone(this,FLAG_NOSUMMON, true);
		if (_noMiniMap && character instanceof L2PcInstance)
		{
			((L2PcInstance) character).sendMessage(Message.getMessage((L2PcInstance) character, Message.MessageId.MSG_MAP_NOT_ALLOWED));
			character.setInsideZone(this,FLAG_NOMAP, true);
			if (((L2PcInstance)character).isMiniMapOpen())
			{
				((L2PcInstance)character).setMiniMapOpen(false);
				character.sendPacket(new ShowMiniMap());
			}
		}
		if (_nochat)
		{
			if (character instanceof L2PcInstance)
				((L2PcInstance) character).sendMessage(Message.getMessage((L2PcInstance) character, Message.MessageId.MSG_NO_CHAT));
			character.setInsideZone(this,FLAG_NOCHAT, true);
		}
		if (_trade)
		{
			if (character instanceof L2PcInstance)
				((L2PcInstance) character).sendMessage(Message.getMessage((L2PcInstance) character, Message.MessageId.MSG_ENTER_TRADE_ZONE));
			character.setInsideZone(this,FLAG_TRADE, true);
		}
		if (_Queen)
			character.setInsideZone(this, FLAG_QUEEN, true);
		if (_Baium && character.isGrandBoss())
			character.setInsideZone(this,FLAG_BAIUM, true);
		if (_Zaken && character.isGrandBoss())
			character.setInsideZone(this,FLAG_ZAKEN, true);
		if (_instanceName != null && _instanceGroup != null && character instanceof L2PcInstance)
		{
			L2PcInstance pl = (L2PcInstance) character;
			InstanceResult ir = new InstanceResult();

			if (_instanceGroup.equals("party"))
			{
				if (pl.isInParty())
				{
					List<L2PcInstance> list = pl.getParty().getPartyMembers();
					getInstanceFromGroup(ir, list, false);
					checkPlayersInside(ir, list);
				}
			}
			else if (_instanceGroup.equals("clan"))
			{
				if (pl.getClan() != null)
				{
					List<L2PcInstance> list = pl.getClan().getOnlineMembersList();
					getInstanceFromGroup(ir, list, true);
					checkPlayersInside(ir, list);
				}
			}
			else if (_instanceGroup.equals("alliance"))
			{
				if (pl.getAllyId() > 0)
				{
					List<L2PcInstance> list = pl.getClan().getOnlineAllyMembers();
					getInstanceFromGroup(ir, list, true);
					checkPlayersInside(ir, list);
				}
			}

			if (ir.reason == REASON_MULTIPLE_INSTANCE)
				pl.sendMessage("You cannot enter this instance while other " + _instanceGroup + " members are in another instance.");
			else if (ir.reason == REASON_INSTANCE_FULL)
				pl.sendMessage("This instance is full. There is a maximum of " + _maxPlayers + " players inside.");
			else if (ir.reason == REASON_SMALL_GROUP)
				pl.sendMessage("Your " + _instanceGroup + " is too small. There is a minimum of " + _minPlayers + " players inside.");
			else
			{
				try
				{
					if (ir.instanceId == 0)
						ir.instanceId = InstanceManager.getInstance().createDynamicInstance(_instanceName);
					portIntoInstance(pl, ir.instanceId);
				}
				catch (Exception e)
				{
					pl.sendMessage(Message.getMessage(pl, Message.MessageId.MSG_CANT_CREATE_INSTANCE));
				}
			}
		}
		if (_artefactCast)
			character.setInsideZone(this,FLAG_ARTEFACTCAST, true);
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (_onExitMsg != null && character instanceof L2PcInstance)
			character.sendPacket(_onExitMsg);

		if (_abnormal > 0)
			character.stopAbnormalEffect(_abnormal);

		if (_applyExit != null)
		{
			for (L2Skill sk : _applyExit)
				sk.getEffects(character, character);
		}
		if (_removeExit != null)
		{
			for (L2Skill sk : _removeExit)
				character.stopSkillEffects(sk.getId());
		}
		if (_funcTemplates != null)
			character.removeStatsOwner(this);

		if (_pvp == PvpSettings.ARENA)
		{
			character.setInsideZone(this,FLAG_NOSUMMON, false);
			character.setInsideZone(this,FLAG_PVP, false);
			if (character instanceof L2PcInstance)
				character.sendPacket(SystemMessageId.LEFT_COMBAT_ZONE);
		} 
		else if (_pvp == PvpSettings.PEACE) {
			character.setPreventedFromReceivingBuffs(false);
			if(character instanceof L2PcInstance && character.isPreventedFromReceivingBuffs())
				((L2PcInstance)character).sendMessage("Block buff is off");
			character.setInsideZone(this,FLAG_PEACE, false);
		}

		if (_noLanding && character instanceof L2PcInstance)
		{
			character.setInsideZone(this,FLAG_NOLANDING, false);
			if (((L2PcInstance) character).getMountType() == 2)
				((L2PcInstance) character).exitedNoLanding();
		}
		if (_noEscape)
			character.setInsideZone(this,FLAG_NOESCAPE, false);
		if (_noPrivateStore)
			character.setInsideZone(this,FLAG_NOSTORE, false);
		if (_noSummon)
			character.setInsideZone(this,FLAG_NOSUMMON, false);
		if (_noMiniMap && character instanceof L2PcInstance)
		{
			if (character.isInsideZone(FLAG_NOMAP) && character instanceof L2PcInstance)
				((L2PcInstance) character).sendMessage(Message.getMessage((L2PcInstance) character, Message.MessageId.MSG_MAP_ON));
			character.setInsideZone(this,FLAG_NOMAP, false);
		}
		if (_nochat)
			character.setInsideZone(this,FLAG_NOCHAT, false);
		if (_trade)
		{
			character.setInsideZone(this,FLAG_TRADE, false);
			if (character instanceof L2PcInstance)
				((L2PcInstance) character).sendMessage(Message.getMessage((L2PcInstance)character, Message.MessageId.MSG_EXIT_TRADE_ZONE));
		}
		if (character instanceof L2PcInstance && _instanceName != null && character.getInstanceId() > 0)
			portIntoInstance((L2PcInstance) character, 0);
		if (_Queen)
			character.setInsideZone(this, FLAG_QUEEN, false);
		if (_Baium && character.isGrandBoss())
			character.setInsideZone(this,FLAG_BAIUM, false);
		if (_Zaken && character.isGrandBoss())
			character.setInsideZone(this,FLAG_ZAKEN, false);
		if (_artefactCast)
			character.setInsideZone(this,FLAG_ARTEFACTCAST, false);
	}

	@Override
	public void onDieInside(L2Character character)
	{
		if (_exitOnDeath)
			onExit(character);
	}

	@Override
	public void onReviveInside(L2Character character)
	{
		if (_exitOnDeath)
			onEnter(character);
	}

	private void getInstanceFromGroup(InstanceResult ir, List<L2PcInstance> group, boolean allowMultiple)
	{
		for (L2PcInstance mem : group)
		{
			if (mem == null || mem.getInstanceId() == 0)
				continue;

			Instance i = InstanceManager.getInstance().getInstance(mem.getInstanceId());
			if (i.getName().equals(_instanceName))
			{
				ir.instanceId = i.getId(); // Player in this instance template found
				return;
			}
			else if (!allowMultiple)
			{
				ir.reason = REASON_MULTIPLE_INSTANCE;
				return;
			}
		}
	}

	private void checkPlayersInside(InstanceResult ir, List<L2PcInstance> group)
	{
		if (ir.reason != REASON_OK)
			return;

		int valid = 0, all = 0;

		for (L2PcInstance mem : group)
		{
			if (mem != null && mem.getInstanceId() == ir.instanceId)
				valid++;
			all++;

			if (valid == _maxPlayers)
			{
				ir.reason = REASON_INSTANCE_FULL;
				return;
			}
		}
		if (all < _minPlayers)
			ir.reason = REASON_SMALL_GROUP;
	}

	private void portIntoInstance(L2PcInstance pl, int instanceId)
	{
		pl.setInstanceId(instanceId);
		pl.getKnownList().updateKnownObjects();
		L2Summon pet = pl.getPet();
		if (pet != null)
		{
			pet.setInstanceId(instanceId);
			pet.getKnownList().updateKnownObjects();
		}
	}
}
