package ru.catssoftware.gameserver.model.actor.knownlist;

import java.util.Map;

import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Decoy;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2Summon;
import ru.catssoftware.gameserver.model.actor.instance.L2BoatInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2DoorInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PetInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2StaticObjectInstance;
import ru.catssoftware.gameserver.network.serverpackets.CharInfo;
import ru.catssoftware.gameserver.network.serverpackets.DeleteObject;
import ru.catssoftware.gameserver.network.serverpackets.DoorInfo;
import ru.catssoftware.gameserver.network.serverpackets.DoorStatusUpdate;
import ru.catssoftware.gameserver.network.serverpackets.DropItem;
import ru.catssoftware.gameserver.network.serverpackets.GetOnVehicle;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.NpcInfo;
import ru.catssoftware.gameserver.network.serverpackets.PetInfo;
import ru.catssoftware.gameserver.network.serverpackets.PetItemList;
import ru.catssoftware.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import ru.catssoftware.gameserver.network.serverpackets.PrivateStoreMsgSell;
import ru.catssoftware.gameserver.network.serverpackets.RecipeShopMsg;
import ru.catssoftware.gameserver.network.serverpackets.RelationChanged;
import ru.catssoftware.gameserver.network.serverpackets.Ride;
import ru.catssoftware.gameserver.network.serverpackets.SpawnItem;
import ru.catssoftware.gameserver.network.serverpackets.StaticObject;
import ru.catssoftware.gameserver.network.serverpackets.VehicleInfo;
import ru.catssoftware.util.SingletonMap;

public class PcKnownList extends PlayableKnownList
{
	private Map<Integer, Integer> _knownRelations;

	public PcKnownList(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	public final Map<Integer, Integer> getKnownRelations()
	{
		if (_knownRelations == null)
			_knownRelations = new SingletonMap<Integer, Integer>().setShared();
		return _knownRelations;
	}

	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		
		if (!super.addKnownObject(object, dropper))
			return false;

/*		Throwable t = new Throwable();
		t.fillInStackTrace();
		String msg="";
		for(StackTraceElement e :t.getStackTrace()) {
			if(!msg.isEmpty())
				msg+="->";
			msg+=e.getClassName()+"."+e.getMethodName();
		}
		System.out.println(msg); */
		if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
			getActiveChar().sendPacket(new SpawnItem(object));
		else
		{
			L2GameServerPacket infoPacket = object.getInfoPacket();
			if(infoPacket!=null) {
				getActiveChar().sendPacket(infoPacket);
			} else {
				if (object instanceof L2ItemInstance)
				{
					if (dropper != null) 
						getActiveChar().sendPacket(new DropItem((L2ItemInstance) object, dropper.getObjectId()));
					else
						getActiveChar().sendPacket(new SpawnItem(object));
				}
				else if (object instanceof L2DoorInstance)
				{
					getActiveChar().sendPacket(new DoorInfo((L2DoorInstance)object, false));
					getActiveChar().sendPacket(new DoorStatusUpdate((L2DoorInstance)object));
				}
				else if (object instanceof L2BoatInstance)
				{
					if (!getActiveChar().isInBoat())
					{
						if (object != getActiveChar().getBoat())
						{
							getActiveChar().sendPacket(new VehicleInfo((L2BoatInstance) object));
							((L2BoatInstance) object).sendVehicleDeparture(getActiveChar());
						}
					}
				}
				else if (object instanceof L2StaticObjectInstance)
					getActiveChar().sendPacket(new StaticObject((L2StaticObjectInstance) object));
				else if (object instanceof L2Decoy )
					getActiveChar().sendPacket(new CharInfo((L2Decoy) object));
				else if (object.isNpc())
				{
					getActiveChar().sendPacket(new NpcInfo(object.getNpc()));
				}
				else if (object instanceof L2Summon)
				{
					L2Summon summon = (L2Summon) object;
	
					if (getActiveChar() == summon.getOwner())
					{
						getActiveChar().sendPacket(new PetInfo(summon, 0));
						if (summon instanceof L2PetInstance)
							getActiveChar().sendPacket(new PetItemList((L2PetInstance) summon));
					}
					else
						getActiveChar().sendPacket(new NpcInfo(summon));
				}
				else if (object.isPlayer())
				{
					L2PcInstance otherPlayer = (L2PcInstance) object;
	
					if (!getActiveChar().showTraders())
					{
						if (otherPlayer.inPrivateMode())
							return true;
					}
	
					if (otherPlayer.isInBoat())
					{
						otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getPosition());
						getActiveChar().sendPacket(new CharInfo(otherPlayer));
						getActiveChar().sendPacket(new GetOnVehicle(otherPlayer, otherPlayer.getBoat(), otherPlayer.getInBoatPosition().getX(), otherPlayer.getInBoatPosition().getY(), otherPlayer.getInBoatPosition().getZ()));
					}
					else
						getActiveChar().sendPacket(new CharInfo(otherPlayer));
	
					getKnownRelations().put(object.getObjectId(), -1);
					RelationChanged.sendRelationChanged(otherPlayer, getActiveChar());
					
					if (otherPlayer.getMountType() == 4)
					{
						// TODO: Remove when horse mounts fixed
						getActiveChar().sendPacket(new Ride(otherPlayer, false, 0));
						getActiveChar().sendPacket(new Ride(otherPlayer, true, otherPlayer.getMountNpcId()));
					}
					if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
						getActiveChar().sendPacket(new PrivateStoreMsgSell(otherPlayer));
					else if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY)
						getActiveChar().sendPacket(new PrivateStoreMsgBuy(otherPlayer));
					else if (otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)
						getActiveChar().sendPacket(new RecipeShopMsg(otherPlayer));
				}
			}
			if (object instanceof L2Character)
			{
				L2Character obj = (L2Character) object;
				if (obj.getAI() != null)
					obj.getAI().describeStateToPlayer(getActiveChar());
				
			}
			
		}
		return true;
	}

	@Override
	public final void removeAllKnownObjects()
	{
		super.removeAllKnownObjects();
		getKnownRelations().clear();
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object)) {
			return false;
		}
		if (object.isPlayer())
			getKnownRelations().remove(object.getObjectId());
		getActiveChar().sendPacket(new DeleteObject(object));
		return true;
	}

	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance)_activeChar;
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		int knownlistSize = getKnownObjects().size();

		if (knownlistSize <= 25)
			return 4000;
		if (knownlistSize <= 35)
			return 3500;
		if (knownlistSize <= 70)
			return 2910;

		return 2310;
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		int knownlistSize = getKnownObjects().size();

		if (knownlistSize <= 25)
			return 3400;
		if (knownlistSize <= 35)
			return 2900;
		if (knownlistSize <= 70)
			return 2300;

		return 1700;
	}
}
