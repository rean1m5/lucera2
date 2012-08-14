package ru.catssoftware.gameserver.network.serverpackets;

import java.io.UnsupportedEncodingException;

/**
 * @author Administrator
 */
public class ExShowScreenMessage extends L2GameServerPacket
{
	private int _type; 
	private int _sysMessageId; 
	private int _unk1; 
	private int _unk2; 
	private int _unk3; 
	private int _unk4; 
	private int _size; 
	private int _position; 
	private boolean _effect; 	
	private String _text;
	private int _time;

	public ExShowScreenMessage(String text, int time)
	{
		_type = 1;
		_sysMessageId = -1; 
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 0;
		_unk4 = 0;
		_position = 0x02; 
		_text = text;
		_time = time;
		_size = 1;
		_effect = false;
	}
	public ExShowScreenMessage(String text, int time, int color,int pos, int size)
	{
		_type = 1;
		_sysMessageId = -1; 
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 0;
		_unk4 = color;
		_position = pos; 
		_text = text;
		_time = time;
		_size = size;
		_effect = false;
	}

	public ExShowScreenMessage (int type, int messageId, int position,  int size,  boolean showEffect, int time,String text)
	{
		_type = type;
		_sysMessageId = messageId;
		_unk1 = 0;
		_unk2 = 0;
		_unk3 = 0;
		_unk4 = 0;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
	}

	@Override
	public String getType()
	{
		return "[S]FE:39 ExShowScreenMessage";
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x38);
		writeD(_type); // 0 - system messages, 1 - your defined text
		writeD(_sysMessageId); // system message id (_type must be 0 otherwise no effect)
		writeD(_position); // message position
		writeD(_unk1); // ?
		writeD(_size); // font size 0 - normal, 1 - small
		writeD(_unk2); // ?
		writeD(_unk3); // ? 
		writeD(_effect == true ? 1 : 0); // upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect
		writeD(_time); // time
		writeD(_unk4); // ?
		try {
			byte [] b = _text.getBytes("UTF-16LE");
			byte [] data = new byte[b.length+2];
			System.arraycopy(b, 0, data, 0, b.length);
			data[data.length-2] = 0;
			data[data.length-1] = 0;
			writeB(data);
		} catch (UnsupportedEncodingException e) {
			writeS(_text);
		} 
	}
}