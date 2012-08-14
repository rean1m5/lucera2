package ru.catssoftware.configurations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import org.apache.log4j.Logger;


import ru.catssoftware.annotations.Property;



public class ConfigFile {
	protected static Logger _log = Logger.getLogger(ConfigFile.class);
	public static String CONFIG_FOLDER="./"; 
	public static void load(Class<?> configClass, String configFile)  {
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(new File(CONFIG_FOLDER+"/"+configFile)));
			for(Field f : configClass.getDeclaredFields()) {
				if(f.isAnnotationPresent(Property.class) ) {
					Property prop = f.getAnnotation(Property.class);
					String value = p.getProperty(prop.key());
					if(value==null) {
						_log.warn("Property `"+prop.key()+"` not exists in file `"+configFile+"`, using default value ("+prop.defaultValue()+")");
						value = prop.defaultValue();
					}
					PropertyTransformer<?> transformer = TransformFactory.getTransformer(f);
					if(transformer==null) {
						_log.error("Unknown property datatype "+f.getType().getSimpleName()+" for `"+prop.key()+"` in file `"+configFile+"`");
						continue;
					}
					boolean access = f.isAccessible();
					f.setAccessible(true);
					try {
						f.set(null,transformer.transform(value, f));
					} catch(Exception e) {
						_log.error("Can't access filed "+f.getName(),e);
						continue;
					}
					finally {
						f.setAccessible(access);
					}
				}
			}
		} catch(IOException ioe) {
			_log.error("Error loading "+configFile);
		}
	}
	
}
