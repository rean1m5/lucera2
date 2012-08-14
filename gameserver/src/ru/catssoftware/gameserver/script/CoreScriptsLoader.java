package ru.catssoftware.gameserver.script;

import ru.catssoftware.gameserver.model.quest.pack.ai.*;

public class CoreScriptsLoader
{
	public static void Register()
	{
		registerAIScripts();
	}

	
	private static void registerAIScripts()
	{
		new CatsEyeBandit();
		new Chests();
		new DeluLizardmanSpecialAgent();
		new DeluLizardmanSpecialCommander();
		new DrChaos();
		new Evabox();
		new FairyTrees();
		new FeedableBeasts();
		new FindAndAttackMaster();
		new Gordon();
		new HotSprings();
		new IceFairySirra();
		new KarulBugbear();
		new LastImperialTomb();
		new Monastery();
		new OlMahumGeneral();
		new PolymorphingAngel();
		new PolymorphingOnAttack();
		new SummonMinions();
		new TimakOrcOverlord();
		new TimakOrcTroopLeader();
		new TurekOrcFootman();
		new TurekOrcSupplier();
		new TurekOrcWarlord();
		new VarkaKetraAlly();
		new ScarletStokateNoble();
		new FOGMobs();
	}


}
