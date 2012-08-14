package ru.catssoftware.loginserver.model;

import ru.catssoftware.tools.network.SubNet;


/**
 *
 * A class that represent the information for a ban
 *
 */
public class BanInfo
{
	/**
	 * The ip adress banned
	 */
	private SubNet	_net;

	/**
	 * expiration of the ban : represent the difference, measured in milliseconds, between the expiration time of the ban and midnight, January 1, 1970 UTC.
	 */
	private long	_expiration;

	/**
	 * Constructor
	 * @param ipAddress
	 * @param expiration
	 */
	public BanInfo(SubNet net, long expiration)
	{
		_net = net;
		_expiration = expiration;
	}

	public SubNet getNet()
	{
		return _net;
	}

	/**
	 * check if the ban is eternal (equal to 0)
	 * @return true or false
	 */
	public void setExpired(long val) {
		_expiration = val;
	}
	
	public boolean isBanEternal()
	{
		return _expiration == 0;
	}

	/**
	 * Check if ban expired : current time > _expiration
	 * @return true if ban is expired
	 */
	public boolean hasExpired()
	{
		return System.currentTimeMillis() > _expiration;
	}
}