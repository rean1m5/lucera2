package ru.catssoftware.protection;

import ru.catssoftware.tools.security.BlowFishKeygen;
import ru.catssoftware.tools.security.GameCrypt;


public class LameStub {
	public static boolean ISLAME = false; 
	public static void main(String []args) {
		BlowFishKeygen._ISLAME = true;
		GameCrypt._ISLAME = true;
		ISLAME = true;
	}
}
