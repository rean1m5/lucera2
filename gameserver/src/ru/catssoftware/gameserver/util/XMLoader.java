package ru.catssoftware.gameserver.util;

import org.w3c.dom.Node;

import java.util.logging.Logger;

/*
 * @author Ro0TT
 * @date 07.01.2012
 */

public abstract class XMLoader
{
	protected static Logger _log = Logger.getLogger(XMLoader.class.getName());

	/**
	 * Возращает по дефолту Empty String (isEmpty())
	 * @param node - элемент парсинга.
	 * @param param - что парсим.
	 * @param debug - нужно ли выводить оишбку если возникла ошибка при парсе параметра.
	 * @return что спарсили.
	 */
	protected static String get(Node node, String param, boolean debug)
	{
		if (node==null || node.getAttributes()==null || node.getAttributes().getNamedItem(param)==null || node.getAttributes().getNamedItem(param).getNodeValue()==null)
		{
			if(debug) _log.info("Error parsing for param: " + param);
			return "";
		}
		else
			return node.getAttributes().getNamedItem(param).getNodeValue();
	}

	protected static String get(Node n, String param)
	{
		return get(n,param,true);
	}

	/**
	 * Возращает дефолтное значение.
	 * @param n
	 * @param param
	 * @param def
	 * @return String
	 */
	protected static String get(Node n, String param, String def)
	{
		String ret = get(n, param, false);
		return ret.isEmpty() ? def : ret;
	}

	/**
	 * Возращает дефолтное значение.
	 * @param n
	 * @param param
	 * @param def
	 * @return int
	 */
	protected static int getInt(Node n, String param, int def)
	{
		return Integer.parseInt(get(n, param, Integer.toString(def)));
	}

	/**
	 * Возращает по дефолту 0.
	 * @return int
	 */
	protected static int getInt(Node n, String param)
	{
		return getInt(n, param, 0);
	}

	/**
	 * Возращает дефолтное значение.
	 * @return long
	 */
	protected static long getLong(Node n, String param, long def)
	{
		return Long.parseLong(get(n, param, Long.toString(def)));
	}

	/**
	 * Возращает по дефолту 0.
	 * @return long
	 */
	protected static long getLong(Node n, String param)
	{
		return getLong(n, param, 0);
	}

	/**
	 * Возращает дефолтное значение.
	 * @return boolean
	 */
	protected static boolean getBoolean(Node n, String param, boolean def)
	{
		return Boolean.parseBoolean(get(n, param, Boolean.toString(def)));
	}

	/**
	 * Возращает по дефолту false.
	 * @return boolean
	 */
	protected static boolean getBoolean(Node n, String param)
	{
		return getBoolean(n, param, false);
	}

	protected static boolean isNodeName(Node node, String name)
	{
		return name.equalsIgnoreCase(node.getNodeName());
	}

	public Node findSubNode(Node n, String name)
	{
		if(name == null)
			return null;

		for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			if(name.equalsIgnoreCase(d.getNodeName()))
				return d;

		return null;
	}

	public String getSubNodeAttr(Node n, String name, String atrName)
	{
		return getSubNodeAttr(n, name, atrName, "");
	}

	/**
	 * Ищет вложенный нод и возвращает значение его атрибута
	 * @param n
	 * @param name
	 * @param atrName
	 * @param def
	 * @return
	 */
	public String getSubNodeAttr(Node n, String name, String atrName, String def)
	{
		String ret = def;
		if(findSubNode(n, name) != null)
		{
			ret = get(findSubNode(n, name), atrName, def);
		}

		return ret;
	}

	public int getSubNodeAttr(Node n, String name, String atrName, int def)
	{
		return Integer.parseInt(getSubNodeAttr(n, name, atrName, "" + def));
	}

	public String getSet(Node n, String name, String def)
	{
		for(Node nn = n.getFirstChild(); nn != null; nn = nn.getNextSibling())
		{
			if(nn.getNodeName().equalsIgnoreCase("set") && get(nn, "name").equalsIgnoreCase(name))
				return get(nn, "val");
		}

		return def;
	}
}
