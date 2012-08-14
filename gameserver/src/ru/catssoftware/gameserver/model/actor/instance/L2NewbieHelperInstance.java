package ru.catssoftware.gameserver.model.actor.instance;

import ru.catssoftware.gameserver.ai.CtrlIntention;
import ru.catssoftware.gameserver.model.base.ClassId;
import ru.catssoftware.gameserver.model.base.Race;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.serverpackets.ActionFailed;
import ru.catssoftware.gameserver.network.serverpackets.MyTargetSelected;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.RadarControl;
import ru.catssoftware.gameserver.network.serverpackets.ValidateLocation;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;

public class L2NewbieHelperInstance extends L2NpcInstance
{
	public L2NewbieHelperInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;
		if (this != player.getTarget())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
			{
				Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
				if (qlsa != null && qlsa.length > 0)
					player.setLastQuestNpcObject(getObjectId());
				Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
				if ((qlst != null) && qlst.length == 1)
					qlst[0].notifyFirstTalk(this, player);
				else
					showChatWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void showChatWindow(L2PcInstance pl)
	{
		if (pl==null)
			return;
		String filename="no";
		if (pl.getClassId()==ClassId.darkFighter
				||pl.getClassId()==ClassId.darkMage
				||pl.getClassId()==ClassId.mage
				||pl.getClassId()==ClassId.fighter
				||pl.getClassId()==ClassId.elvenMage
				||pl.getClassId()==ClassId.elvenFighter
				||pl.getClassId()==ClassId.dwarvenFighter
				||pl.getClassId()==ClassId.darkMage
				||pl.getClassId()==ClassId.orcFighter
				||pl.getClassId()==ClassId.orcMage
				)
		{
			QuestState st = pl.getQuestState("7003_NewbieHelper");
			int plLvl=pl.getLevel();
			if (st!=null && plLvl<20)
			{
				if (st.getInt("cond")<3 && plLvl>5)
					st.set("cond","3");
				if (st.getInt("cond")<6 && plLvl>9)
					st.set("cond","6");
				if (st.getInt("cond")<8 && plLvl>14)
					st.set("cond","8");
				if (st.getInt("cond")<10 && plLvl>17)
					st.set("cond","10");

				if (st.getInt("cond")==9)
				{
					st.addExpAndSp(285670,58155);
					st.set("cond","10");
				}
				if (st.getInt("cond")==10)
				{
					filename="data/html/newbiehelper/50.htm";
				}
				if (st.getInt("cond")==7)
				{
					st.addExpAndSp(183128,8242);
					st.set("cond","8");
				}
				if (st.getInt("cond")==8)
				{
					switch(getTemplate().getIdTemplate())
					{
					case 30598: //Люди
						filename="data/html/newbiehelper/41.htm";
						pl.sendPacket(new RadarControl(0,2,-84057,242832,-3729));
						break;
					case 30601://Гномы
						filename="data/html/newbiehelper/42.htm";
						pl.sendPacket(new RadarControl(0,2,116266,-177518,-883));
						break;
					case 30599://Эльфы
						filename="data/html/newbiehelper/44.htm";
						pl.sendPacket(new RadarControl(0,2,45859,50827,-3058));
						break;
					case 30600://Т.Эльфы
						filename="data/html/newbiehelper/43.htm";
						pl.sendPacket(new RadarControl(0,2,11258,14431,-4242));
						break;
					case 30602://Орки
						filename="data/html/newbiehelper/45.htm";
						pl.sendPacket(new RadarControl(0,2,-45863,-112621,-200));
						break;
					case 32135://Камаэли
						filename="data/html/newbiehelper/46.htm";
						pl.sendPacket(new RadarControl(0,2,-125872,38208,1232));
						break;
					}
				}
				if (st.getInt("cond")==5)
				{
					st.addExpAndSp(42192,1753);
					st.set("cond","6");
				}
				if (st.getInt("cond")==6 )
				{
					if (pl.getClassId()==ClassId.darkFighter)
					{
						filename="data/html/newbiehelper/31.htm";
						pl.sendPacket(new RadarControl(0,2,10584,17581,-4557));
					}
					if (pl.getClassId()==ClassId.darkMage)
					{
						filename="data/html/newbiehelper/38.htm";
						pl.sendPacket(new RadarControl(0,2,10775,14190,-4242));
					}
					if (pl.getClassId()==ClassId.mage)
					{
						filename="data/html/newbiehelper/32.htm";
						pl.sendPacket(new RadarControl(0,2,-91008,248016,-3568));
					}
					if (pl.getClassId()==ClassId.fighter)
					{
						filename="data/html/newbiehelper/33.htm";
						pl.sendPacket(new RadarControl(0,2,-71384,258304,-3109));
					}
					if (pl.getRace()==Race.Elf)
					{
						filename="data/html/newbiehelper/34.htm";
						pl.sendPacket(new RadarControl(0,2,47595,51569,-2996));
					}
					if (pl.getRace()==Race.Orc)
					{
						filename="data/html/newbiehelper/35.htm";
						pl.sendPacket(new RadarControl(0,2,-46808,-113184,-112));
					}
					if (pl.getRace()==Race.Dwarf)
					{
						filename="data/html/newbiehelper/37.htm";
						pl.sendPacket(new RadarControl(0,2,115717,-183488,-1483));
					}
				}
				if (st.getInt("cond")==4)
				{
					switch(getTemplate().getIdTemplate())
					{
					case 30598: //Люди
						filename="data/html/newbiehelper/21.htm";
						pl.sendPacket(new RadarControl(0,2,-82236,241573,-3728));
						break;
					case 30601://Гномы
						filename="data/html/newbiehelper/22.htm";
						pl.sendPacket(new RadarControl(0,2,116103,-178407,-948));
						break;
					case 30599://Эльфы
						filename="data/html/newbiehelper/23.htm";
						pl.sendPacket(new RadarControl(0,2,42812,51138,-2996));
						break;
					case 30600://Т.Эльфы
						filename="data/html/newbiehelper/24.htm";
						pl.sendPacket(new RadarControl(0,2,7644,18048,-4377));
						break;
					case 30602://Орки
						filename="data/html/newbiehelper/25.htm";
						pl.sendPacket(new RadarControl(0,2,-46802,-114011,-112));
						break;
					case 32135://Камаэли
						filename="data/html/newbiehelper/26.htm";
						pl.sendPacket(new RadarControl(0,2,-119378,49242,8));
						break;
					}
				}
				else if((st.getInt("cond")==2 || st.getInt("cond")==3))
				{
					if (st.getInt("cond")==2)
					{
						st.addExpAndSp(3154,127);
						st.set("cond","3");
					}
					filename="data/html/newbiehelper/13.htm";
				}
				else if(st.getInt("cond")==1)
				{
					switch(getTemplate().getIdTemplate())
					{
					case 30598: //Люди
						filename="data/html/newbiehelper/2.htm";
						pl.sendPacket(new RadarControl(0,2,-84436,242793,-3729));
						break;
					case 30601://Гномы
						filename="data/html/newbiehelper/4.htm";
						pl.sendPacket(new RadarControl(0,2,112656,-174864,-611));
						break;
					case 30599://Эльфы
						filename="data/html/newbiehelper/6.htm";
						pl.sendPacket(new RadarControl(0,2,42978,49115,-2994));
						break;
					case 30600://Т.Эльфы
						filename="data/html/newbiehelper/8.htm";
						pl.sendPacket(new RadarControl(0,2,25856,10832,-3724));
						break;
					case 30602://Орки
						filename="data/html/newbiehelper/10.htm";
						pl.sendPacket(new RadarControl(0,2,-47360,-113791,-237));
						break;
					case 32135://Камаэли
						filename="data/html/newbiehelper/12.htm";
						pl.sendPacket(new RadarControl(0,2,-119378,49242,8));
						break;
					}
				}
				else if(st.getInt("cond")==0)
				{
					if (plLvl<2)
						st.addExpAndSp(68,50);
					st.set("cond","1");
					switch(getTemplate().getIdTemplate())
					{
					case 30598: //Люди
						filename="data/html/newbiehelper/1.htm";
						break;
					case 30601://Гномы
						filename="data/html/newbiehelper/3.htm";
						break;
					case 30599://Эльфы
						filename="data/html/newbiehelper/5.htm";
						break;
					case 30600://Т.Эльфы
						filename="data/html/newbiehelper/7.htm";
						break;
					case 30602://Орки
						filename="data/html/newbiehelper/9.htm";
						break;
					case 32135://Камаэли
						filename="data/html/newbiehelper/11.htm";
						break;
					}
				}
			}
		}
		if (filename.equalsIgnoreCase("no"))
		{
			super.showChatWindow(pl);
			return;
		}
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		pl.sendPacket(html);
	}
}