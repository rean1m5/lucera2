package ru.catssoftware.gameserver.mmocore;

@SuppressWarnings("unchecked")
public interface IClientFactory<T extends MMOClient>
{
	public T create(MMOConnection<T> con);
}