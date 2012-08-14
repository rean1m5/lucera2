package ru.catssoftware.gameserver.taskmanager.tasks;

import org.apache.log4j.Logger;


import ru.catssoftware.Config;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.network.serverpackets.ExPCCafePointInfo;
import ru.catssoftware.gameserver.threadmanager.ExclusiveTask;
import ru.catssoftware.tools.random.Rnd;

import javolution.util.FastMap;

public class TaskPcCaffe extends ExclusiveTask {

	private static Logger _log = Logger.getLogger(TaskPcCaffe.class);
	public TaskPcCaffe() {
		_log.info("PCCaffe: Task scheduled every "+Config.PC_CAFFE_INTERVAL+" minutes");
	}
	private FastMap<Integer,Location> _lastLoc = new FastMap<Integer, Location>(); 
	@Override
	public void onElapsed() {
		for(L2PcInstance pc : L2World.getInstance().getAllPlayers()) {
			if(pc.isOfflineTrade() || pc.isAway() || pc.isAlikeDead() || pc.getLevel() < Config.PC_CAFFE_MIN_LEVEL || pc.getLevel() > Config.PC_CAFFE_MAX_LEVEL) 
				continue;
			if(_lastLoc.containsKey(pc.getObjectId())) try {
				Location l = _lastLoc.get(pc.getObjectId());
				Location l2 = pc.getLoc();
				if(Math.abs((l.getX()*l.getX() + l.getY()*l.getY())-(l2.getX()*l2.getX()+l2.getY()+l2.getY())) < 200)
					continue;
			} finally {
				_lastLoc.put(pc.getObjectId(),pc.getLoc());
			}
			
			int score = Config.PC_CAFFE_MIN_SCORE+ Rnd.get(Config.PC_CAFFE_MAX_SCORE-Config.PC_CAFFE_MIN_SCORE);
			if(score<=0)
				continue;
			pc.setPcCaffePoints(pc.getPcCaffePoints()+score);
			pc.sendPacket(new ExPCCafePointInfo(pc,score,true,24,false));
		}
		schedule(Config.PC_CAFFE_INTERVAL*60000);
	}

}
