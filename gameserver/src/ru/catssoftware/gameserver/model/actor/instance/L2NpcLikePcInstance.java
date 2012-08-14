package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.datatables.xml.NpcLikePcTemplate;
import ru.catssoftware.gameserver.datatables.xml.NpcLikePcTemplates;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.network.serverpackets.L2GameServerPacket;
import ru.catssoftware.gameserver.network.serverpackets.NpcLikePcInfo;
import ru.catssoftware.gameserver.network.serverpackets.StartRotation;
import ru.catssoftware.gameserver.network.serverpackets.StopRotation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Util;

public class L2NpcLikePcInstance extends L2MerchantInstance {
	private NpcLikePcInfo _info = new NpcLikePcInfo(this);
	public L2NpcLikePcInstance(int objectId, L2NpcTemplate template) {
		super(objectId, template);
		_tpl = NpcLikePcTemplates.getInstance().getTemplate(getNpcId());
	}
	private NpcLikePcTemplate _tpl;
	@Override
	public L2GameServerPacket getInfoPacket() { 
		return _info;
	}
	@Override
	public void broadcastFullInfoImpl() {
		broadcastPacket(_info);
	}
	public Race getRace() {
		if(_tpl!=null)
			return _tpl.getRace();
		return Race.Human;
	}
	public boolean isFemale() {
		return getTemplate().getSex().equals("female");
	}
	public int getFaceType() {
		if(_tpl!=null)
			return _tpl.getStats().getInteger("FaceType");
		return 0;
	}
	public int getHairStyle() {
		if(_tpl!=null)
			return _tpl.getStats().getInteger("HairStyle");

		return 0;
	}
	@Override
	public void onSpawn() {
		setHeading(getSpawn().getHeading());
		super.onSpawn();
	}
	public int getHairColor() {
		if(_tpl!=null)
			return _tpl.getStats().getInteger("HairColor");
		return 0;
	}
	public ClassId getClassId() {
		if(_tpl!=null)
			return _tpl.getClassId();
		return ClassId.bishop;
	}
	public int getNameColor() {
		return 0xFFFFF;
	}
	public int getTitleColor() {
		return 0xFFFFFF;
	}
	public int getPaperdollItemId(int slot) {
		if(_tpl!=null)
			return _tpl.getInventory()[slot];
		return 0;
	}
	public byte getEnchantEffect() {
		byte enchant = 0;
		if(_tpl!=null) {
			int [] e = _tpl.getEnchant();
			for(int val : e) 
				if(enchant<val) enchant = (byte)val;
		}
		if(enchant>127)
			enchant= 127;
		return enchant;
	}

	@Override
	public void onAction(L2PcInstance player) {
		super.onAction(player);
	}
	@Override
	public void onRandomAnimation(L2PcInstance player) {
		if(player!=null ) {
			player.sendPacket(new StartRotation(getObjectId(),getHeading(),1,32768));
			player.sendPacket(new StopRotation(getObjectId(),Util.calculateHeadingFrom(this, player),32768));
			player = null;
		}
		
	}
	@Override
	protected String getHtmlFolder() {
		return "default";
	}
}
