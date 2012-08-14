package ru.catssoftware.loginserver.network.serverpackets;

import ru.catssoftware.loginserver.L2LoginClient;
import ru.catssoftware.loginserver.mmocore.SendablePacket;


/**
 *
 * @author  KenM
 */
public abstract class L2LoginServerPacket extends SendablePacket<L2LoginClient>
{
	/**
	* @see ru.catssoftware.loginserver.mmocore.SendablePacket#getHeaderSize()
	*/
	@Override
	protected int getHeaderSize()
	{
		return 2;
	}

	/**
	* @see ru.catssoftware.loginserver.mmocore.SendablePacket#writeHeader(int)
	*/
	@Override
	protected void writeHeader(int dataSize)
	{
		writeH(dataSize + this.getHeaderSize());
	}
}
