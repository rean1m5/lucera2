package ru.catssoftware.gameserver.datatables.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public abstract class XMLDocument
{
	public void load(File documentFile) throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		parseDocument(factory.newDocumentBuilder().parse(documentFile));
	}
	
	abstract protected void parseDocument(Document doc);

	public static String get(Node n, String item)
	{
		final Node d = n.getAttributes().getNamedItem(item);
		if(d == null)
			return "";
		final String val = d.getNodeValue();
		if(val == null)
			return "";
		return val;
	}

	public static boolean get(Node n, String item, boolean dflt)
	{
		final Node d = n.getAttributes().getNamedItem(item);
		if(d == null)
			return dflt;
		final String val = d.getNodeValue();
		if(val == null)
			return dflt;
		return Boolean.parseBoolean(val);
	}

	public static int get(Node n, String item, int dflt)
	{
		final Node d = n.getAttributes().getNamedItem(item);
		if(d == null)
			return dflt;
		final String val = d.getNodeValue();
		if(val == null)
			return dflt;
		return Integer.parseInt(val);
	}

	public static long get(Node n, String item, long dflt)
	{
		final Node d = n.getAttributes().getNamedItem(item);
		if(d == null)
			return dflt;
		final String val = d.getNodeValue();
		if(val == null)
			return dflt;
		return Long.parseLong(val);
	}

	public static boolean isNodeName(Node node, String name)
	{
		return node != null && node.getNodeName().equals(name);
	}
}
