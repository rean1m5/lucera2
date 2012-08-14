package ru.catssoftware.gameserver.datatables;


public final class CrownTable
{
	public static boolean						giveCrown			= false;
	private static final int[] 					CROWN_IDS			=
	{
		6841, // Crown of the lord
		6834, // Innadril
		6835, // Dion
		6836, // Goddard
		6837, // Oren
		6838, // Gludio
		6839, // Giran
		6840, // Aden
		8182, // Rune
		8183, // Schuttgart
	};

	public static void getInstance()
	{
	}

	public static int[] getCrownIds()
	{
		return CROWN_IDS;
	}

	public static int getCrownId(int castleId)
	{
		switch (castleId)
		{
			case 1:// Gludio
				return 6838;
			case 2: // Dion
				return 6835;
			case 3: // Giran
				return 6839;
			case 4: // Oren
				return 6837;
			case 5: // Aden
				return 6840;
			case 6: // Innadril
				return 6834;
			case 7: // Goddard
				return 6836;
			case 8:// Rune
				return 8182;
			case 9: // Schuttgart
				return 8183;
		}
		return 0;
	}
	
}