package ru.catssoftware.gameserver.network.serverpackets;


import org.apache.log4j.Logger;
import ru.catssoftware.gameserver.mmocore.SendablePacket;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.L2GameClient;


public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	protected static final Logger		_log	= Logger.getLogger(L2GameServerPacket.class);

	@Override
	protected final void write(L2GameClient client)
	{
		try
		{
			writeImpl();
			writeImpl(client, client.getActiveChar());
			getClient().can_runImpl = true;
		}
		catch (Exception e)
		{
			_log.error("Failed writing: " + client + " - Failed writing: " + getType());
			e.printStackTrace();
			
		}
	}

	public void runImpl(L2GameClient client, L2PcInstance activeChar)
	{
	}

	protected void writeImpl()
	{
	}

	protected void writeImpl(L2GameClient client, L2PcInstance activeChar)
	{
	}
	
	public boolean isImportant() {
		return false;
	}

	/**
	 * @return A String with this packet name for debuging purposes
	 */
	public String getType()
	{
		return getClass().getSimpleName();
	}

	/**
	 * @see ru.catssoftware.gameserver.mmocore.SendablePacket#getHeaderSize()
	 */
	@Override
	protected final int getHeaderSize()
	{
		return 2;
	}

	/**
	 * @see ru.catssoftware.gameserver.mmocore.SendablePacket#writeHeader(int)
	 */
	@Override
	protected final void writeHeader(int dataSize)
	{
		writeH(dataSize + getHeaderSize());
	}

	public boolean canBroadcast(L2PcInstance activeChar)
	{
		return true;
	}
}