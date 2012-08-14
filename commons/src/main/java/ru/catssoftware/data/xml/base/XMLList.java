package ru.catssoftware.data.xml.base;

import java.lang.reflect.Field;


import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ru.catssoftware.configurations.PropertyTransformer;
import ru.catssoftware.configurations.TransformFactory;
import ru.catssoftware.configurations.TransformationException;

public abstract class XMLList extends XMLObject {
	protected static Logger _log = Logger.getLogger("DATABASE");

	private static class XMLListTransformer implements PropertyTransformer< XMLList> {

		@Override
		public XMLList transform(String value, Field field, Object... data)
				throws TransformationException {
			try {
				XMLList obj =  (XMLList)field.get(data[1]);
				if(obj==null)
					throw new TransformationException("Field "+field.getName()+" not initialized");
				obj.load((Node)data[0]);
				return obj;
			} catch(Exception e) {
				e.printStackTrace();
				throw new TransformationException(e.getMessage());
			}
			
		}
		
	}
	
	static {
		TransformFactory.registerTransformer(XMLList.class, new XMLListTransformer());
	}

	protected abstract XMLObject createNew(String nodeName, NamedNodeMap attr);
	
	@Override
	public void load(Node node) {
		super.load(node);
		for(Node d = node.getFirstChild();d!=null;d = d.getNextSibling()) {
						XMLObject obj  = createNew(d.getNodeName(),d.getAttributes());
						if(obj!=null) {
							obj.load(d);
							addObject(obj);
						}
		}
		
	}
	public abstract void addObject(XMLObject obj);

}
