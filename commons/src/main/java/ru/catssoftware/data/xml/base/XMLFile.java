package ru.catssoftware.data.xml.base;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ru.catssoftware.annotations.XmlFile;

public abstract class XMLFile extends XMLList {
	public XMLFile() {
	}
	public void load() {
		if(this.getClass().isAnnotationPresent(XmlFile.class)) {
			XmlFile dataFileInfo = this.getClass().getAnnotation(XmlFile.class);
			parseFile(dataFileInfo.fileName());
		}
	}
	

	protected void parseFile(String fileName) {
		parseFile(new File(fileName));
	}
	
	protected void parseFile(File file) {
		if(file.exists()) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			try {
				Document doc = factory.newDocumentBuilder().parse(file);
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
					if(n.getNodeName().equals("list")) {
						load(n);
					}
				}
			} catch(Exception e) {
				_log.error("Error parsing "+file.getAbsolutePath()+": ",e);
			}
		}
		
	}
}
