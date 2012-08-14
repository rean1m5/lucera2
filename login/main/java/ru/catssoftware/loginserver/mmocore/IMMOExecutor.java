package ru.catssoftware.loginserver.mmocore;

public interface IMMOExecutor<T extends MMOConnection<T>>
{
	public void execute(ReceivablePacket<T> packet);
}