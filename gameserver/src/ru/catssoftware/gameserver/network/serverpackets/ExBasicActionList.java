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
package ru.catssoftware.gameserver.network.serverpackets;
// TODO: Remove!
/**
 *
 * @author  KenM/Crion
 */
public class ExBasicActionList extends L2GameServerPacket
{
	private static final String _S__FE_5E_EXBASICACTIONLIST = "[S] FE:5F ExBasicActionList";

//	private final int[] _actionIds;

	private static final int[] DEFAULT_ACTIONS = 
	{
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
		21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
		41, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
		60, 61, 62, 63, 64, 65, 66,

		1000, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013,
		1014, 1015, 1016, 1017, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1038,
		1039, 1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050,
		1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060
	};

	public static final ExBasicActionList DEFAULT_ACTION_LIST = new ExBasicActionList(DEFAULT_ACTIONS);

	/*
	0 - Sit/Stand
	1 - Walk/Run
	2 - Attack
	3 - Exchange
	4 - Next Target
	5 - Pick Up
	6 - Assist
	7 - Invite
	8 - Leave Party
	9 - Dismiss Party Member
	10 - Private Store - Sell
	11 - Party Matching
	12 - Greeting
	13 - Victory
	14 - Advance
	15 - Pet Change Movement Mode
	16 - Pet Attack
	17 - Pet Stop
	18 - Pet Pickup
	19 - Pet Unsummon
	20 - 
	21 - Summon Change Movement Mode
	22 - Summon Attack
	23 - Summon Stop
	24 - Yes
	25 - No
	26 - Bow
	27 - 
	28 - Private Store - Buy
	29 - Unaware
	30 - Social Waiting
	31 - Laugh
	32 - Mode Change (Wild Hog Cannon)
	33 - Applaud
	34 - Dance
	35 - Sorrow
	36 - Toxic Smoke (Soulless)
	37 - Dwarven Manufacture
	38 - Mount/Dismount
	39 - Parasite Burst (Soulless)
	40 - Recommend
	41 - Attack (Wild Hog Cannon)
	42 - Self Damage Shield (Kai the Cat)
	43 - Hydro Screw (Unicorn Mirage)
	44 - Boom Attack (Big Boom)
	45 - Master Recharge (Kat the Cat)
	46 - Mega Storm Strike (Mew the Cat)
	47 - Steal Blood (Silhouette)
	48 - Mech. Cannon (Mechanic Golem)
	49 - 
	50 - Change of Party Leader
	51 - General Manufacture
	52 - Summon Unsummon
	53 - Summon Move
	54 - Pet Move To Target
	55 - Start/End Recording Replay
	56 - Command Channel Invitation
	57 - Find Store
	58 - Duel
	59 - Withdraw
	60 - Party Duel
	61 - Package Sale
	62 - Charm
	63 - Mini Game
	64 - Teleport Bookmark
	65 - Bot Report
	66 - Shyness

	1000 - Siege Hammer (Siege Golem)
	1001 - 
	1002 - 
	1003 - Wild Stun (Wind Hatchling)
	1004 - Wild Defense (Wind Hatchling)
	1005 - Bright Burst (Star Hatchling)
	1006 - Bright Heal (Star Hatchling)
	1007 - Blessing of Queen (Feline Queen)
	1008 - Gift of Queen (Feline Queen)
	1009 - Cure of Queen (Feline Queen)
	1010 - Blessing of Seraphim (Unicorn Seraphim)
	1011 - Gift of Seraphim (Unicorn Seraphim)
	1012 - Cure of Seraphim (Unicorn Seraphim)
	1013 - Curse of Shade (Nightshade)
	1014 - Mass Curse of Shade (Nightshade)
	1015 - Shade Sacrifice (Nightshade)
	1016 - Cursed Blow (Cursed Man)
	1017 - Cursed Strike/Stun (Cursed Man)
	1018 - 
	1030 - 
	1031 - Slash (Feline King)
	1032 - Spinning Slash (Feline King)
	1033 - Grip of the Cat (Feline King)
	1034 - Whiplash (Magnus the Unicorn)
	1035 - Tidal Wave (Magnus the Unicorn)
	1036 - Corpse Kaboom (Spectral Lord)
	1037 - Dicing Death (Spectral Lord)
	1038 - Force Curse (Spectral Lord)
	1039 - Cannon Fodder (Swoop Cannon)
	1040 - Big Bang (Swoop Cannon)
	1041 - Bite Attack (Great Wolf)
	1042 - Maul (Great Wolf)
	1043 - Cry of the Wolf (Great Wolf)
	1044 - Awakening (Great Wolf)
	1045 - Howl (Great Wolf)
	1046 - Roar (Strider)
	1047 - Bite (Divine Beast)
	1048 - Stun Attack (Divine Beast)
	1049 - Fire Breath (Divine Beast)
	1050 - Roar (Divine Beast)
	1051 - Bless the Body (Feline Queen)
	1052 - Bless the Soul (Feline Queen)
	1053 - Haste (Feline Queen)
	1054 - Acumen (Seraphim Unicorn)
	1055 - Clarity (Seraphim Unicorn)
	1056 - Empower (Seraphim Unicorn)
	1057 - Wild Magic (Seraphim Unicorn)
	1058 - Death Whisper (Nightshade)
	1059 - Focus (Nightshade)
	1060 - Guidance (Nightshade)
	*/

	private ExBasicActionList(int... actionIds)
	{
//		_actionIds = actionIds;
	}

	/**
	* @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#writeImpl()
	*/
	@Override
	protected void writeImpl()
	{
		return;
/*		writeC(0xfe);
		writeH(0x5f);
		writeD(_actionIds.length);
		for (int i = 0; i < _actionIds.length; i++)
			writeD(_actionIds[i]); */
	}

	/**
	* @see ru.catssoftware.gameserver.serverpackets.L2GameServerPacket#getType()
	*/
	@Override
	public String getType()
	{
		return _S__FE_5E_EXBASICACTIONLIST;
	}
}