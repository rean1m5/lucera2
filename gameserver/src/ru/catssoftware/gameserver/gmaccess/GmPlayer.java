package ru.catssoftware.gameserver.gmaccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель игрового администратора
 * @author m095
 * @version 1.0
 */

public class GmPlayer
{
	private String					name;
	private final int				ojId;
	private boolean					root 		= false;
	private boolean					norm 		= false;
	private boolean					FixedRes 	= false;
	private boolean					AltG 		= false;
	private boolean					PeaceAtk	= false;
	private boolean					CheckIP 	= false;
	private boolean					isTemp		= false;
	private String[]				SecureIP;
	private List<String> 			commands;
	
	public GmPlayer(int id)
	{
		ojId					= id;
		commands 				= new ArrayList<String>();
	}
	
	/* obj id */
	public int getObjId()
	{
		return ojId;
	}
	
	/* gm name */
	public void setName(String val)
	{
		name = val;
	}
	public String getName()
	{
		return name;
	}
	
	/* gm root */
	public void setRoot(boolean val)
	{
		root = val;
		if (val)
		{
			AltG = true;
			FixedRes = true;
			norm = true;
			PeaceAtk = true;
		}
	}
	public boolean isRoot()
	{
		return root;
	}
	
	/* gm admin */
	public void setGm(boolean val)
	{
		norm = val;
	}
	public boolean isGm()
	{
		return norm;
	}
	
	/* gm self res */
	public void seFixRes(boolean val)
	{
		FixedRes = val;
	}
	public boolean allowFixRes()
	{
		return FixedRes;
	}
	
	/* gm Alt G */
	public void seAltG(boolean val)
	{
		AltG = val;
	}
	public boolean allowAltG()
	{
		return AltG;
	}
	
	/* Peace Atk */
	public void setPeaceAtk(boolean val)
	{
		PeaceAtk = val;
	}
	public boolean allowPeaceAtk()
	{
		return PeaceAtk;
	}

	/* gm check ip */
	public void setCheckIp(boolean val)
	{
		CheckIP = val;
	}
	public boolean checkIp()
	{
		return CheckIP;
	}
	
	/* gm ip list */
	public void setIP(String[] val)
	{
		SecureIP = val;
	}
	public String[] secureIp()
	{
		return SecureIP;
	}
	
	/* gm command */
	public void putCommand(String val)
	{
		commands.add(val);
	}
	public List<String> getCommands()
	{
		return commands;
	}
	
	public boolean getIsTemp() {
		return isTemp;
	}
	public void setIsTemp(boolean val) {
		isTemp = val;
	}
}