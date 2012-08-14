package ru.catssoftware.gameserver.network.daemons;

import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.L2DatabaseFactory;
import ru.catssoftware.gameserver.util.L2Utils;
import ru.catssoftware.gameserver.util.sql.StoreVote;
import ru.catssoftware.tools.random.Rnd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;


 /**
  * Автор: Azagthtot
  * L2TopDaemon
  **/

public class L2TopDeamon implements IDeamon
{
	
	private static Logger _log = Logger.getLogger(L2TopDeamon.class);
	private boolean							_firstRun										= false;
	private Timestamp						_lastVote;
	@Override
	public String getName() {
		return "L2Top";
	}

	@Override
	public String getUrl() {
		if (Config.L2TOP_REW_MODE == Config.RewardMode.SMS)
			return "http://l2top.ru/index.php?top=editServ&adminAct=lasVotes&uid=" + String.valueOf(Config.L2TOPDEMON_SERVERID) + "_sms"+(Config.L2TOPDEMON_KEY.length()>0?("&key="+Config.L2TOPDEMON_KEY):"");
		else
			if (Config.L2TOP_REW_MODE == Config.RewardMode.WEB)
				return "http://l2top.ru/index.php?top=editServ&adminAct=lasVotes&uid=" + String.valueOf(Config.L2TOPDEMON_SERVERID) + "_web"+(Config.L2TOPDEMON_KEY.length()>0?("&key="+Config.L2TOPDEMON_KEY):"");
			else
				return "http://l2top.ru/index.php?top=editServ&adminAct=lasVotes&uid=" + String.valueOf(Config.L2TOPDEMON_SERVERID)+(Config.L2TOPDEMON_KEY.length()>0?("&key="+Config.L2TOPDEMON_KEY):"");
	}

	@Override
	public boolean load() {
		_lastVote = null;
		if (Config.L2TOPDEMON_ENABLED)
		{
			try
			{
				Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement stm = con.prepareStatement("select max(votedate) from character_votes where deamon_name=?");
				stm.setString(1, getName());
				ResultSet r = stm.executeQuery();
				if(r.next())
					_lastVote = r.getTimestamp(1);

				if (_lastVote==null)
				{
					_firstRun = true;
					_lastVote = new Timestamp(0);
				}

				r.close();
				stm.close();
				con.close();
				return true;
			}
			catch(SQLException e)
			{
				_log.info("L2Top: Error connection to database: ",e);
			}
		}
		return false;
	}

	@Override
	public void parse(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is,"windows-1251"));
		String line;
		int votes = 0;
		Timestamp last = _lastVote;

		while((line=reader.readLine()) != null)
		{
			if (line.contains("\t"))
			{
				Timestamp voteDate = Timestamp.valueOf(line.substring(0,line.indexOf("\t")).trim());
				if (voteDate.after(_lastVote))
				{
					if(voteDate.after(last))
						last = voteDate;

					String charName = line.substring(line.indexOf("\t")+1).toLowerCase();

					if (Config.L2TOPDEMON_PREFIX != null && Config.L2TOPDEMON_PREFIX.length() > 0)
					{
						if(charName.startsWith(Config.L2TOPDEMON_PREFIX))
							charName = charName.substring(Config.L2TOPDEMON_PREFIX.length()+1);
						else
							continue;
					}
					StoreVote.store(this, charName, voteDate, Config.L2TOPDEMON_IGNOREFIRST?!_firstRun:true);
					votes++;
					if(votes>=Config.DEAMON_MAX_VOTES)
						break;
				}
			}
			try { Thread.sleep(20); } catch(InterruptedException e) {
				break;
			}
		}
		_lastVote = last;
		_log.info("L2Top: "+votes+" processed");
		
	}

	@Override
	public void rewardPlayer(String playerName) {
			int numItems = Config.L2TOPDEMON_MIN;
			if (Config.L2TOPDEMON_MAX > Config.L2TOPDEMON_MIN)
				numItems += Rnd.get(Config.L2TOPDEMON_MAX - Config.L2TOPDEMON_MIN);
			L2Utils.addItem(playerName, Config.L2TOPDEMON_ITEM, numItems);
	}
}
