package ru.catssoftware.gameserver.mmocore;

@SuppressWarnings("unchecked")
public interface IMMOExecutor<T extends MMOClient>
{
	public void execute(ReceivablePacket<T> packet);
}