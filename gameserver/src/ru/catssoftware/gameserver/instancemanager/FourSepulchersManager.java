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
package ru.catssoftware.gameserver.instancemanager;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.datatables.NpcTable;
import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.datatables.SpawnTable;
import ru.catssoftware.gameserver.datatables.xml.DoorTable;
import ru.catssoftware.gameserver.datatables.xml.XMLDocument;
import ru.catssoftware.gameserver.instancemanager.grandbosses.BossLair;
import ru.catssoftware.gameserver.model.*;
import ru.catssoftware.gameserver.model.actor.instance.*;
import ru.catssoftware.gameserver.model.mapregion.TeleportWhereType;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.gameserver.network.serverpackets.NpcSay;
import ru.catssoftware.gameserver.templates.chars.L2NpcTemplate;
import ru.catssoftware.gameserver.util.Broadcast;

import java.io.File;
import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * This class ...
 * @version $Revision: $ $Date: $
 * @author  sandman
 */
public class FourSepulchersManager 
{
	private static FourSepulchersManager	_instance;

	private static final String				QUEST_ID				= "620_FourGoblets";
	private static Logger _log = Logger.getLogger(FourSepulchersManager.class);
	
	public static class FourSepulchersMausoleum extends BossLair{
		public class FourSepulchersRoom {
			private class RoomStage {
				private int _stageNo;
				private List<L2Spawn> _mobs = new FastList<L2Spawn>();
				private int _bossId;
				private int _aliveId;
				private L2Spawn _keyboxSpawn;
				private L2Spawn _treasureBox;
				private L2Skill  _skill;
				private List<L2SepulcherMonsterInstance> _monsters = new FastList<L2SepulcherMonsterInstance>();
				public RoomStage(Node n) {
					_stageNo = Integer.parseInt(n.getAttributes().getNamedItem("no").getNodeValue());
					for(Node n1=n.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
						if(n1.getNodeName().equals("spawn")) {
							NamedNodeMap attr = n1.getAttributes();
							L2NpcTemplate template = NpcTable.getInstance().getTemplate(Integer.parseInt(attr.getNamedItem("npcid").getNodeValue()));
							L2Spawn spawn  = new L2Spawn(template);
							spawn.setLocx(Integer.parseInt(attr.getNamedItem("x").getNodeValue()));
							spawn.setLocy(Integer.parseInt(attr.getNamedItem("y").getNodeValue()));
							spawn.setLocz(Integer.parseInt(attr.getNamedItem("z").getNodeValue()));
							if(attr.getNamedItem("heading")!=null)
								spawn.setHeading(Integer.parseInt(attr.getNamedItem("heading").getNodeValue()));
							_mobs.add(spawn);
						}
						else if(n1.getNodeName().equals("skill")) {
							String [] sk = n1.getAttributes().getNamedItem("id").getNodeValue().split(",");
							_skill = SkillTable.getInstance().getInfo(Integer.parseInt(sk[0].trim()), Integer.parseInt(sk[1].trim()));
						}
						else if(n1.getNodeName().equals("openbox") ) {
							NamedNodeMap attr = n1.getAttributes();
							L2NpcTemplate template = NpcTable.getInstance().getTemplate(31472);
							_treasureBox = new L2Spawn(template);
							_treasureBox.setLocx(Integer.parseInt(attr.getNamedItem("x").getNodeValue()));
							_treasureBox.setLocy(Integer.parseInt(attr.getNamedItem("y").getNodeValue()));
							_treasureBox.setLocz(Integer.parseInt(attr.getNamedItem("z").getNodeValue()));
						}
						else if(n1.getNodeName().equals("keybox")) {
							L2NpcTemplate template = NpcTable.getInstance().getTemplate(31456);
							_keyboxSpawn = new L2Spawn(template);
						}
						else if(n1.getNodeName().equals("boss"))
							_bossId = Integer.parseInt(n1.getAttributes().getNamedItem("npcid").getNodeValue());
						else if(n1.getNodeName().equals("alive"))
							_aliveId = Integer.parseInt(n1.getAttributes().getNamedItem("npcid").getNodeValue());
						
					}
				}
				public void reset() {
					if(_treasureBox!=null && _treasureBox.getLastSpawn()!=null)
						_treasureBox.getLastSpawn().deleteMe();
					if(_keyboxSpawn!=null && _keyboxSpawn.getLastSpawn()!=null)
						_keyboxSpawn.getLastSpawn().deleteMe();
					for(L2NpcInstance mob : _monsters)
						mob.deleteMe();
					_monsters.clear();
				}
				public void init() {
					if(_treasureBox!=null) {
						L2SepulcherNpcInstance box = (L2SepulcherNpcInstance)_treasureBox.doSpawn();
						box.setMausoleum(FourSepulchersMausoleum.this);
					}
					else if(_mobs.size()>0) {
						for(L2Spawn spawn : _mobs) {
							L2SepulcherMonsterInstance mob =  (L2SepulcherMonsterInstance)spawn.doSpawn();
							mob.setMausoleum(FourSepulchersMausoleum.this);
							mob.setStage(_stageNo);
							_monsters.add(mob);
						}
						if(_skill!=null)
							ThreadPoolManager.getInstance().scheduleEffect(new Runnable() {
								public void run() {
									for(L2NpcInstance npc : _monsters)
										_skill.getEffects(npc, npc);
								}
							}, 1500);
					}
					else if(_keyboxSpawn!=null) {
						_keyboxSpawn.setLocx(_loc.getX()+70);
						_keyboxSpawn.setLocy(_loc.getY()+70);
						_keyboxSpawn.setLocz(_loc.getZ());
						((L2SepulcherNpcInstance)_keyboxSpawn.doSpawn()).setMausoleum(FourSepulchersMausoleum.this);
					}
				}
			}
			
			private int 				   _roomNo;
			private int					   _gateKeeperId;
			private RoomStage			   _stage;
			private int					   _stageIndex;	
			private Map<Integer,RoomStage> _stages = new FastMap<Integer,RoomStage>();
			private L2DoorInstance _door;
			
			public FourSepulchersRoom(Node n) {
				_roomNo = Integer.parseInt(n.getAttributes().getNamedItem("no").getNodeValue());
				for(Node n1=n.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
					if(n1.getNodeName().equals("door")) {
						_door = DoorTable.getInstance().getDoor(Integer.parseInt(n1.getAttributes().getNamedItem("id").getNodeValue()));
					}
					else if(n1.getNodeName().equals("gatekeeper")) {
						_gateKeeperId = Integer.parseInt(n1.getAttributes().getNamedItem("id").getNodeValue());
						L2Spawn[] spawns = SpawnTable.getInstance().findAllNpc(_gateKeeperId);
						((L2SepulcherNpcInstance) spawns[0].getLastSpawn()).setMausoleum(FourSepulchersMausoleum.this);
					}
					else if(n1.getNodeName().equals("stage")) {
						RoomStage stage = new RoomStage(n1);
						_stages.put(stage._stageNo, stage);
					}
				}
			}

			public  void reset() {
				_door.closeMe();
				for(RoomStage stage : _stages.values() )
					stage.reset();
			}
			public  void init(boolean open) {
				for(L2PcInstance pc : getPlayersInside())
					if(pc.getParty()!=_partyInside && !pc.isGM())
							pc.teleToLocation(TeleportWhereType.Town);
				_stageIndex = 0;
				FourSepulchersMausoleum.this._room = this;
				if(open) {
					if(_door!=null) 
						_door.openMe();
					ThreadPoolManager.getInstance().schedule(new Runnable() {
						public void run() {
							if(_door!=null)
								_door.closeMe();
							nextStage();
						}
					}, 25000);
				} else nextStage();
			}
			private Location _loc;
			private void nextStage() {
				_stageIndex++;
				_stage = _stages.get(_stageIndex);
				if(_stage!=null) {
					ThreadPoolManager.getInstance().schedule(new Runnable() {
						public void run() {
							_stage.init();
						}
					},2000);
				}
			}
				
		}
		

		private int _gatekeeper;
		private int _roomIndex;
		
		private FourSepulchersRoom _room;
		private L2Party _partyInside;
		private Map<Integer, FourSepulchersRoom> _rooms = new FastMap<Integer, FourSepulchersRoom>();
		public FourSepulchersMausoleum(Node n) {
			for(Node n1 = n.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
				if(n1.getNodeName().equals("gatekeeper"))
					_gatekeeper = Integer.parseInt(n1.getAttributes().getNamedItem("id").getNodeValue());
				else if(n1.getNodeName().equals("room")) {
					FourSepulchersRoom room = new FourSepulchersRoom(n1);
					_rooms.put(room._roomNo, room);
				}
			}
		}
		public void nextStage() {
			if(_room!=null)
				_room.nextStage();
		}
		public boolean enter(L2PcInstance leader) {
			if(_room == null) {
				_roomIndex = 1;
				_room = _rooms.get(_roomIndex);
				_partyInside = leader.getParty();
				if(_room._stages.get(1)._treasureBox!=null) {
					L2Spawn sp = _room._stages.get(1)._treasureBox;
					for(L2PcInstance pc : leader.getParty().getPartyMembers())
						if(!pc.isAlikeDead())
							pc.teleToLocation(sp.getLocx(),sp.getLocy(),sp.getLocz(),true);
				} else
					if(_room._door!=null) {
						_room._door.openMe();
						ThreadPoolManager.getInstance().schedule(new Runnable() {
							public void run() {
								_room._door.closeMe();
							}
						}, 25000);
					}
				return true;
			}
			return false;
		}
		public void startRoom(int id) {
			_roomIndex = id;
			_room = _rooms.get(_roomIndex);
			_room.init(true);
		}
		public void nextRoom() {
			if(_room!=null)
				_room.reset();
			_roomIndex++;
			_room = _rooms.get(_roomIndex);
			if(_room!=null)
				_room.init(true);
		}
		
		public synchronized void onKill(L2SepulcherMonsterInstance mob, L2Character killer) {
			if(mob.getStage()!=_room._stageIndex)
				return;
			if(_room._stage._monsters.contains(mob)) 
				_room._stage._monsters.remove(mob);
			_room._loc = mob.getLoc();
			if(mob.getNpcId()==_room._stage._bossId)
				_room.nextStage();
			else if (_room._stage._monsters.size()==0)
				_room.nextStage();
			else if(_room._stage._aliveId!=0 && _room._stage._monsters.size()==1) try {
				if(_room._stage._monsters.get(0).getNpcId()==_room._stage._aliveId)
					_room.nextStage();
			} catch(Exception e) { }
			int npcId = mob.getNpcId();
			if(npcId==25339 || npcId==25342 || npcId==25346 || npcId==25349) {
				for(L2PcInstance pc : getPlayersInside()) {
					if(!pc.isAlikeDead()) {
						int rewardId = 0;
						switch(npcId) {
							case 25339: rewardId = 7256; break;
							case 25342: rewardId = 7257; break;
							case 25346: rewardId = 7258; break;
							case 25349: rewardId = 7259; break;
						}
						if(rewardId!=0) {
							QuestState qs = pc.getQuestState(QUEST_ID);
							if(qs!=null && pc.getInventory().getItemByItemId(7262)==null)
								qs.giveItems(rewardId, 1);
						}
					}
				}
				finish();
			}
		}
		
		@Override
		public void init() {
		}
		@Override
		public void setRespawn() {
		}
		@Override
		public void setUnspawn() {
		}
		
		public void finish() {
			_partyInside = null;
		}
		public void start() {
			_room.init(false);
			NpcSay say = new NpcSay(_manager.getObjectId(),1,_manager.getNpcId(),"Fight will begin now!");
			for(L2PcInstance pc : _partyInside.getPartyMembers()) 
				pc.sendPacket(say);
			
		}
		public void reset() {
			clearLair();
			for(FourSepulchersRoom room : _rooms.values())
				room.reset();
			_room = null;
			_partyInside = null;
		}
		
	}
	
	private Map<Integer, FourSepulchersMausoleum> _mausoleums = new FastMap<Integer, FourSepulchersMausoleum>();
	private List<Integer> _opentime = new FastList<Integer>();
	private int _cooltime;
	private int _fighttime;
	public static final FourSepulchersManager getInstance()
	{
		if (_instance == null)
			_instance = new FourSepulchersManager();
		return _instance;
	}
	
	public void init() {
		_log.info("FourSepulchersManager: Loaded "+_mausoleums.size()+" mausoleum(s)");
		scheduleOpen();
	}
	private class FourSepulchersParser extends XMLDocument {
		@Override
		protected void parseDocument(Document doc) {
			for(Node n = doc.getFirstChild();n!=null;n=n.getNextSibling()) {
				if(n.getNodeName().equals("foursepulchers")) {
					NamedNodeMap m = n.getAttributes();
					if(m.getNamedItem("opentime")!=null)
						for(String s : m.getNamedItem("opentime").getNodeValue().split(" ")) try {
							_opentime.add(Integer.parseInt(s.trim()));
						} catch(NumberFormatException e) { }
						if(m.getNamedItem("cooltime")!=null)
							_cooltime = Integer.parseInt(m.getNamedItem("cooltime").getNodeValue());
					for(Node n1=n.getFirstChild();n1!=null;n1=n1.getNextSibling())
						if(n1.getNodeName().equals("mausoleum")) {
							FourSepulchersMausoleum mausoleum = new FourSepulchersMausoleum(n1); 
							_mausoleums.put(mausoleum._gatekeeper, mausoleum);
						}
				}
			}
		}
	}
	private FourSepulchersManager() {
		try {
			new FourSepulchersParser().load(new File(Config.DATAPACK_ROOT,"data/four_sepluchers.xml"));
			if(_opentime.size() == 0)
				_opentime.add(0);
			if(_cooltime==0)
				_cooltime = 5;
			if(_fighttime == 0)
				_fighttime = 50;
			_manager = SpawnTable.getInstance().findAllNpc(31453)[0].getLastSpawn();
		} catch (Exception e) {
			_log.error("FourSepulchersManager: Error while readingdefeintion",e);
		}
	}

	private boolean _canEnter;
	private static L2NpcInstance _manager;
	private void startPrepare() {
		NpcSay say = new NpcSay(_manager.getObjectId(),1,_manager.getNpcId(),"5 minutes till filght begin!");
		for(FourSepulchersMausoleum m : _mausoleums.values())
			if(m._partyInside!=null && m.getPlayersInside().size()>0) {
				for(L2PcInstance pc : m._partyInside.getPartyMembers()) 
					pc.sendPacket(say);
			}
			else 
				m.reset();
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				startFight();
			}
		}, 300000);
	}
	private void startFight() {
		scheduleOpen();
		boolean hasParty = false;
		for(FourSepulchersMausoleum m : _mausoleums.values())
			if(m._partyInside!=null && m.getPlayersInside().size()>0) {
				hasParty = true;
				m.start();
			}
		if(hasParty)
			ThreadPoolManager.getInstance().schedule(new Runnable() {
				public void run() {
					_remainTime -= 300000;
					NpcSay say = new NpcSay(_manager.getObjectId(),1,_manager.getNpcId(), String.format("%d minutes remain!",_remainTime/60000));
					for(FourSepulchersMausoleum m : _mausoleums.values())
						if(m._partyInside!=null) {
							for(L2PcInstance pc : m._partyInside.getPartyMembers()) 
								pc.sendPacket(say);
						}
					if(_remainTime>300000)
						ThreadPoolManager.getInstance().schedule(this,300000);
				}
			}, 300000);
		
	}
	
	private void reset() {
		for(FourSepulchersMausoleum m : _mausoleums.values())
			m.reset();
	}
	private void startCoolTime() {
		_canEnter = true;
		reset();
		NpcSay say = new NpcSay(_manager.getObjectId(),1,_manager.getNpcId(), "Four Sepulchers is open! 5 minutes for registration!");
		Broadcast.toAllPlayersInRadius(_manager, say, 15000);
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			public void run() {
				startPrepare();
			}
		}, (_cooltime * 60000));
	}
	private static long _remainTime;
	private void scheduleOpen() {
		_canEnter = false;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND,0);
		for(;;) {
			for(Integer min : _opentime) {
				cal.set(Calendar.MINUTE, min);
				if(cal.getTimeInMillis() > System.currentTimeMillis()) {
					_remainTime = cal.getTimeInMillis()-System.currentTimeMillis(); 
					ThreadPoolManager.getInstance().schedule(new Runnable() {
						public void run() {
							startCoolTime();
						}
					},_remainTime) ;
					
					Date dt = new Date(cal.getTimeInMillis());
					_log.info("FourSepulchersManager: Four Sepulchers will open at "+dt);
					return;
				}
			}
			cal.add(Calendar.HOUR_OF_DAY, 1);
		}
		
	}
	public synchronized void tryEntry(L2NpcInstance npc, L2PcInstance player) {
		FourSepulchersMausoleum mausoleum = _mausoleums.get(npc.getNpcId());
		NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		
		try {
			if(!_canEnter) {
				msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-NE.htm");
				return;
			}
			if(mausoleum!=null) {
				if(mausoleum._partyInside!=null) {
					msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-FULL.htm");
					return;
				}
				if(player.getParty()==null || player.getParty().getMemberCount() < Config.FS_PARTY_MEMBER_COUNT) {
					msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-NL.htm");
					return;
				}
				if(!player.getParty().isLeader(player)) {
					msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-NL.htm");
					return;
				}
				
				for(FourSepulchersMausoleum  m : _mausoleums.values())
					if(m._partyInside!=null && m._partyInside.getPartyMembers().contains(player)) {
						msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-NL.htm");
						return;
					}
				for(L2PcInstance pc : player.getParty().getPartyMembers()) {
					QuestState qs =pc.getQuestState(QUEST_ID); 
					if(qs==null) {
						msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-NS.htm");
						msg.replace("%member%", pc.getName());
						return;
					}
					if(qs.getQuestItemsCount(7075)==0) {
						msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-SE.htm");
						msg.replace("%member%", pc.getName());
						return;
					}
				}
				mausoleum.enter(player);
				for(L2PcInstance pc : player.getParty().getPartyMembers()) {
					QuestState qs =pc.getQuestState(QUEST_ID);
					qs.takeItems(7075, 1);
				}
				msg.setFile("data/html/SepulcherNpc/"+npc.getNpcId()+"-OK.htm");
			}
		} finally {
			player.sendPacket(msg);
		}
	}
	public FourSepulchersMausoleum findMausoleum(int npcid) {
		return _mausoleums.get(npcid);
	}

}
