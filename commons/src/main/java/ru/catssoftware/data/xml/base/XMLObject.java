package ru.catssoftware.data.xml.base;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ru.catssoftware.annotations.XmlField;
import ru.catssoftware.configurations.PropertyTransformer;
import ru.catssoftware.configurations.TransformFactory;
import ru.catssoftware.configurations.TransformationException;

public abstract class XMLObject {
	protected static Logger _log = Logger.getLogger("ENGINE"); 
	private static class XMLObjectTransformer implements PropertyTransformer<XMLObject> {

		@Override
		public XMLObject transform(String value, Field field,Object ...data)
				throws TransformationException {
			if(data==null)
				throw new TransformationException("No node given");
			else

				try {
					XMLObject obj = (XMLObject)field.get(data[1]);
					if(obj==null)
						throw new TransformationException("Field "+field.getName()+" not initialized");
					obj.load((Node)data[0]);
					return obj;
				} catch (Exception e) {
					throw new TransformationException("Error setting filed  "+field.getName(),e);
				}
		}
		
	}
	
	static {
		TransformFactory.registerTransformer(XMLObject.class, new XMLObjectTransformer());
	}
	
	public XMLObject() {
		
	}
	/**
	 * Вызывается после загрузки объекта.
	 */
	protected void onLoaded() { }
	/**
	 * Если для ноды не найдено определенных полей, то вызывается этот метод<br>
	 * @param node - читаемая XML-нода
	 */
	protected void readNode(Node node) {}
	/**
	 * Загрузка объекта из xml-ноды<br>
	 * @param node - нода из которой идет загрузка 
	 */
	public void load(Node node) {
		
		Map<String , List<Field>> _nodes = new HashMap<String, List<Field>>();
		try {
			Class<?> cl = getClass();
			while(cl!=XMLObject.class) {
					for(Field f : cl.getDeclaredFields()) 
						if(f.isAnnotationPresent(XmlField.class)) {
							f.setAccessible(true);
							XmlField fieldInfo = f.getAnnotation(XmlField.class);
							if(fieldInfo.nodeName().length()==0) 
								setFieldValue(f, node);
							else {
								String nodeName = fieldInfo.nodeName();
								List<Field> fieldList = _nodes.get(nodeName);
								if(fieldList==null) {
									fieldList = new ArrayList<Field>();
									_nodes.put(nodeName, fieldList);
								}
								fieldList.add(f);
							}
						}
					cl = cl.getSuperclass();
			}
			for(Node n = node.getFirstChild();n!=null;n=n.getNextSibling()) {
					boolean readed = false;
					for(String s : _nodes.keySet()) {
//						Node nameAttr = n.getAttributes()==null?null:n.getAttributes().getNamedItem("name");
						if(n.getNodeName().equals(s)) {// || (n.getNodeName().equals("set") &&  nameAttr!=null && nameAttr.getNodeValue().equals(s))) {
							for(Field f: _nodes.get(s)) {
								setFieldValue(f, n);
								readed = true;
							}
						}
					}
					if(!readed)
						readNode(n);
			}
			onLoaded();		
		} catch(Exception e) {
			_log.error("Error parsing "+node.getNodeName()+" in "+new File(node.getBaseURI()).getName(),e);
		}
	}
	private void setFieldValue(Field f, Node n) throws Exception {
		XmlField fieldInfo = f.getAnnotation(XmlField.class);
		String value  = null;
		Node valueNode = n.getAttributes().getNamedItem(fieldInfo.propertyName());
		
		if(XMLList.class.isAssignableFrom(f.getType())) {
			f.setAccessible(true);
			((XMLList)f.get(this)).load(n);
			return;
		}
		
		if(XMLObject.class.isAssignableFrom(f.getType())) {
			f.setAccessible(true);
			if(f.get(this)!=null) {
				((XMLObject)f.get(this)).load(n);
				return;
			}
		}
		
		if(valueNode!=null)
			value = valueNode.getNodeValue();
		if(fieldInfo.set().length()>0) {
			Class<?> clazz = getClass();
			while(clazz!=XMLObject.class) try {
				Method m  = null;
				try {
					m = clazz.getDeclaredMethod(fieldInfo.set(), String.class);
				} catch(NoSuchMethodException nsm) {
					m = clazz.getDeclaredMethod(fieldInfo.set(), Node.class);
				}
				if(m!=null) {
					m.setAccessible(true);
					
					if(m.getReturnType().getName().equals("void")) {
						Class<?> []params = m.getParameterTypes();
						if(params[0] == String.class) {
							if(value==null)
								return;
							m.invoke(this, value);
						} 
						else if(params[0] == Node.class)
							m.invoke(this, n);
						else
							throw new Exception("Invalid set method for "+f.getName());
						
					}
					else {
						if(value==null)
							return;
						f.set(this,m.invoke(this, value));
					}
					return;
				}
				
			} catch(NoSuchMethodException nsm) {
				clazz = clazz.getSuperclass();
			}
			catch(InvocationTargetException ite) {
				_log.error("Error setting field "+f.getName()+" in "+new File(n.getBaseURI()).getName(),ite);
				return;
			}
		}
		else  {
			if(value==null) 
				return;
			try {
				
				f.set(this, TransformFactory.getTransformer(f).transform(value, f,new Object[] {n,this}));
			} catch(TransformationException te) {
				System.out.println("Node "+n.getNodeName()+" "+(valueNode == null ? "none" : valueNode.getNodeName() )+" "+value);
				System.out.println("Node "+n.getParentNode().getNodeName());
				_log.error("Error setting "+f.getName()+" for class "+getClass().getName(),te);
			}
		}
		
	}
}
