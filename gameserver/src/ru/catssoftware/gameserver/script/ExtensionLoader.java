package ru.catssoftware.gameserver.script;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ExtensionLoader
{
	private static Logger _log = Logger.getLogger(ExtensionLoader.class);
	private HashMap<String, IExtensionScript> _cache;

	private static ExtensionLoader _instance;
	public static ExtensionLoader getInstance()
	{
		if (_instance == null)
			_instance = new ExtensionLoader();
		return _instance;
	}

	private ExtensionLoader()
	{
		_cache = new HashMap<String, IExtensionScript>();

		File folder = new File("extensions");
		if (!folder.canRead())
		{
			_log.error("Not read folder 'extensions'.");
			return;
		}
		File[] files = folder.listFiles();
		if (files == null)
		{
			_log.error("Not list files for folder 'extensions'.");
			return;
		}

		IExtensionScript script;
		for(File file : files)
		{
			if(file.exists())
			{
				JarInputStream stream = null;
				try
				{
					stream = new JarInputStream(new FileInputStream(file));
					JarEntry entry;
					while((entry = stream.getNextJarEntry()) != null)
					{
						//Вложенные класс
						if(entry.getName().contains(ClassUtils.INNER_CLASS_SEPARATOR) || !entry.getName().endsWith(".class"))
							continue;

						String name = entry.getName().replace(".class", "").replace("/", ".");

						Class<?> clazz = Class.forName(name);
						try
						{
							if (clazz.getMethod("extensionLoad") == null)
								continue;
						}
						catch (Exception e)
						{continue;}

						try
						{
							script = ((IExtensionScript) clazz.newInstance());
							_cache.put(name,script);
							script.extensionLoad();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
				catch (Exception e)
				{
				}
				finally
				{
					IOUtils.closeQuietly(stream);
				}
			}
		}
		_log.info("Loaded " + _cache.size() + " extensions.");
	}
}
