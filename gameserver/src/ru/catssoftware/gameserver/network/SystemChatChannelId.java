/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.network;

/**
 * @author  Noctarius
 */
public enum SystemChatChannelId
{
	Chat_Normal("ALL"), // id = 0 , white
	Chat_Shout("SHOUT"), // ! id = 1 , dark orange
	Chat_Tell("WHISPER"), // " id = 2, purple
	Chat_Party("PARTY"), // # id = 3, green
	Chat_Clan("CLAN"), // @ id = 4, blue/purple
	Chat_System("EMOTE"), // ( id = 5
	Chat_User_Pet("USERPET"), // * id = 6
	Chat_GM_Pet("GMPET"), // * id = 7
	Chat_Market("TRADE"), // + id = 8 pink
	Chat_Alliance("ALLIANCE"), // $ id = 9 light green
	Chat_Announce("ANNOUNCE"), // id = 10 light cyan
	Chat_Custom("CRASH"), // id = 11 --> Crashes client
	Chat_L2Friend("L2FRIEND"), // id = 12
	Chat_MSN("MSN"), // id = 13
	Chat_Party_Room("PARTYROOM"), // id = 14
	Chat_Commander("COMMANDER"), // id = 15
	Chat_Inner_Partymaster("INNERPARTYMASTER"), // id = 16
	Chat_Hero("HERO"), // % id = 17 blue
	Chat_Critical_Announce("CRITANNOUNCE"), // id = 18 dark cyan
	Chat_None("NONE");

	private String	_channelName;

	private SystemChatChannelId(String channelName)
	{
		_channelName = channelName;
	}

	public int getId()
	{
		return this.ordinal();
	}

	public String getName()
	{
		return _channelName;
	}

	public static SystemChatChannelId getChatType(int channelId)
	{
		for (SystemChatChannelId channel : SystemChatChannelId.values())
		{
			if (channel.getId() == channelId)
				return channel;
		}

		return SystemChatChannelId.Chat_None;
	}
}