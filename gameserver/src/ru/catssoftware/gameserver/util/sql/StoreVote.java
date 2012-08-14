package ru.catssoftware.gameserver.util.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.network.daemons.IDeamon;
import ru.catssoftware.gameserver.network.daemons.SuperDeamon;
import ru.catssoftware.gameserver.taskmanager.SQLQueue;
import ru.catssoftware.sql.SQLQuery;



/**
 * 
 * @author Azagthtot
 * Хелпер-класс сохранения информации в таблицу голоcований<br>
 * Предназначен для вызова из имплементаций IDeamon<br>
 * Вызов - StoreVote.store()
 */
public class StoreVote implements SQLQuery {

	private IDeamon	_deamon;
	private String _charName;
	private Timestamp _votedate;
	private boolean _canGiveReward;
	private static Logger _log = Logger.getLogger(SuperDeamon.class);
	
	/**
	 * store() - сохранение результатов. 
	 * @param deamon as IDeamon - демон,для которого сохраняются результаты<br>
	 * @param charName as String - имя персонажа<br>
	 * @param voteDate as Timestamp - дата/время голосования<br>
	 * @param canGiveReward as Boolean - выдавать ли награду<br>
	 */
	public static void store(IDeamon deamon, String charName, Timestamp voteDate, boolean canGiveReward) {
		new StoreVote(deamon,charName,voteDate,canGiveReward);
	}

	private StoreVote(IDeamon deamon, String charName, Timestamp voteDate, boolean canGiveReward) {
		_deamon = deamon;
		_charName = charName;
		_votedate = voteDate;
		_canGiveReward = canGiveReward;
		SQLQueue.getInstance().add(this);
	}
	
	@Override
	public void execute(Connection con) {
		try
		{
			PreparedStatement stm = con.prepareStatement("insert into character_votes select ?,?,? from characters where not exists(select * from character_votes where votedate=? and charName =? and deamon_name=?) limit 1");
			stm.setTimestamp(1, _votedate);
			stm.setTimestamp(4, _votedate);
			stm.setString(3,_deamon.getName());
			stm.setString(6,_deamon.getName());
			stm.setString(2, _charName);
			stm.setString(5, _charName);
			boolean sendPrize  = stm.executeUpdate() > 0;
			stm.close();
			if (sendPrize && _canGiveReward)
			{
					_deamon.rewardPlayer(_charName);
			}
		}
		catch (SQLException e)
		{
			_log.warn("SuperDeamon: Error storing data ",e);
		}
	}

}
