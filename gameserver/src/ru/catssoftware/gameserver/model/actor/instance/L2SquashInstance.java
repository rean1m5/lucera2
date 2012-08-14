package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.network.SystemChatChannelId;
import ru.catssoftware.gameserver.network.serverpackets.CreatureSay;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.tools.random.Rnd;
import javolution.util.FastList;

public class L2SquashInstance extends L2MonsterInstance
{
	private L2PcInstance _owner;
	private int _level;
	private FastList<Integer> _hitterId= new FastList<Integer>();
	
	public L2SquashInstance(int objectId, L2NpcTemplate template,L2PcInstance owner)
	{
		super(objectId, template);
		_owner = owner;
		_hitterId.clear();
	}
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	@Override
	public final int getLevel()
	{
		return _level;
	}
	public void setLevel(int par)
	{
		_level=par;
	}
	@Override
	public boolean canReduceHp(double damage, L2Character attacker) {
		
		if (attacker.getActiveWeaponItem()==null)
		{
			monSay();
			return false;
		}
		if (attacker.getActiveWeaponItem().getItemId()!=7058)
		{
			monSay();
			return false;
		}
		if (!_hitterId.contains(attacker.getObjectId()))
			_hitterId.add(attacker.getObjectId());
		boolean hitRnd=false;
		if (((getNpcId()==13016)||(getNpcId()==13017)))
		{
			if (_hitterId.size()>1)
				if (attacker==getOwner())				
					hitRnd = Rnd.nextBoolean();
		}
		else
		{
			if (attacker==getOwner())
				hitRnd = Rnd.nextBoolean();
		}
		return hitRnd;
	}
	public void monSay()
	{
		if(Rnd.get(100)>30)
			return;
		String text="Ты не сможешь меня убить.";
		if (!getKnownList().getKnownPlayers().isEmpty())
		{
			broadcastPacket(new CreatureSay(0, SystemChatChannelId.Chat_Normal, "Тыква", text));
		}
	}
}