package ru.catssoftware.gameserver.network.daemons;

import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.config.L2Properties;
import ru.catssoftware.gameserver.util.L2Utils;
import ru.catssoftware.gameserver.util.sql.StoreVote;
import ru.catssoftware.tools.random.Rnd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MMOTop implements IDeamon {

	private static Logger _log = Logger.getLogger(SuperDeamon.class);
	private String _URL;
	private Timestamp _lastVote;
	private boolean _firstRun = false;
	private int _rewardID;
	private int [] _reward = {1,1};
	private boolean _ignoreFirst;

	@Override
	public String getName() {
		return "mmotop";
	}
	@Override
	public String getUrl() {
		return _URL;
	}
	@Override
	public boolean load() {
		 try {
			 L2Properties p = new L2Properties("./config/main/mmotop.properties");
			 boolean enabled = Boolean.parseBoolean(p.getProperty("Enabled","true"));
			 if(enabled) {
				 _URL = p.getProperty("SiteURL","");
				 _reward[0] = Integer.parseInt(p.getProperty("RewardMin","1"));
				 _reward[1] = Integer.parseInt(p.getProperty("RewardMax","1"));
				 _rewardID = Integer.parseInt(p.getProperty("ItemID","4037"));
				 _ignoreFirst = Boolean.parseBoolean(p.getProperty("RewardAtFirst","false"));
				 Connection con = L2DatabaseFactory.getInstance().getConnection();
				 PreparedStatement stm = con.prepareStatement("select max(votedate) from character_votes where deamon_name=?");
				 stm.setString(1, getName());
				 ResultSet rs = stm.executeQuery();
				 if(rs.next())
					 _lastVote = rs.getTimestamp(1);
				 if (_lastVote==null) {
					 _lastVote =  new Timestamp(0);
					 _firstRun = true;
				 }
				 rs.close();
				 stm.close();
				 con.close();
				 return true;
			 } else
				 return false;
			 
		 } catch(Exception e) {
			 return false;
		 }
		
	}
	@Override
	public void parse(InputStream is) throws Exception {
		BufferedReader r = new BufferedReader(new InputStreamReader(is,"windows-1251"));
		String line;
		int nVotes = 0;
		Timestamp vote = new Timestamp(0);
		while((line = r.readLine())!=null)
			try {
			Pattern p = Pattern.compile("(\\d+)\\t([\\d|\\.|]+ [\\d|:]+)\\t([\\d|\\.]+)\\t(.*?)\\t.+");
			Matcher m = p.matcher(line);
			if(m.matches())
			{
				Pattern p1 = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})");
				Matcher m1 = p1.matcher(m.group(2));
				Timestamp t = null;
				if(m1.matches()) 
					t = Timestamp.valueOf(String.format("%s-%s-%s %s:%s:%s", m1.group(3),m1.group(2),
													     m1.group(1),m1.group(4),m1.group(5),m1.group(6)));
				if(t!=null)
				{
					if(t.after(_lastVote)) {
						if(t.after(vote))
							vote = t;
						String charName = m.group(4);
						if(charName.length()>0) {
							StoreVote.store(this, charName, t, _ignoreFirst?!_firstRun:true);
							nVotes++;
							if(nVotes>=Config.DEAMON_MAX_VOTES)
								break;
						}
					}
				}
			}
			try { Thread.sleep(20); } catch(InterruptedException e) {
				break;
			}
			
		} catch(Exception e) { }
		r.close();
		if(vote.after(_lastVote))
			_lastVote = vote;
		_firstRun = false;
		_log.info("MMOTop: "+nVotes+" vote(s) processed");
	}
	@Override
	public void rewardPlayer(String playerName) {
		int count = _reward[0];
		if (_reward[1] > _reward[0]) count += Rnd.get(_reward[1]-_reward[0]);
		L2Utils.addItem(playerName, _rewardID, count);
	}

}
