package ru.catssoftware.gameserver.model.quest.pack;

public class SagaOfThePhoenixKnight extends SagasSuperClass
{
	public static String qn1 = "70_SagaOfThePhoenixKnight";
	public static int qnu = 70;
	public static String qna = "Сага Рыцаря Феникса";

	public SagaOfThePhoenixKnight()
	{
		super(qnu,qn1,qna);
		NPC = new int[]{30849,31624,31277,30849,31631,31646,31647,31650,31654,31655,31657,31277};
		Items = new int[]{7080,7534,7081,7485,7268,7299,7330,7361,7392,7423,7093,6482};
		Mob = new int[]{27286,27219,27278};
		qn = qn1;
		classid = new int[]{90};
		prevclass = new int[]{0x05};
		X = new int[]{191046,46087,46066};
		Y = new int[]{-40640,-36372,-36396};
		Z = new int[]{-3042,-1685,-1685};
		Text = new String[]{"PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
	                  "... Oh ... good! So it was ... let's begin!","I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
	                  "Paying homage to those who disrupt the orderly will be PLAYERNAME's death!","Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
	                  "Why do you interfere others' battles?","This is a waste of time.. Say goodbye...!","...That is the enemy",
	                  "...Goodness! PLAYERNAME you are still looking?","PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
	                  "Your sword is not an ornament. Don't you think, PLAYERNAME?","Goodness! I no longer sense a battle there now.","let...","Only engaged in the battle to bar their choice. Perhaps you should regret.",
	                  "The human nation was foolish to try and fight a giant's strength.","Must...Retreat... Too...Strong.","PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker","....! Fight...Defeat...It...Fight...Defeat...It..."};
		registerNPCs();
	}
}