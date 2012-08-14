package ru.catssoftware.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtils
{
	public static void addURL(URL u) throws IOException {
		
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class<?> sysclass = URLClassLoader.class;
	 
		try {
			Method method = sysclass.getDeclaredMethod("addURL",URL.class);
			method.setAccessible(true);
			method.invoke(sysloader,u );
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}
			
	}
	public static List<String> enumClasses(String pckgname) throws ClassNotFoundException {
		ArrayList<String> classes = new ArrayList<String>();
		File directory = null;
		try {
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			if (cld == null) 
				throw new ClassNotFoundException("Can't get class loader.");
			String path = pckgname.replace('.', '/');
			Enumeration<URL> resource = null;
			try {
				resource = cld.getResources(path);
			} catch(IOException e) {
				throw new ClassNotFoundException("No resource for " + path);
			}
			if (resource == null) 
				throw new ClassNotFoundException("No resource for " + path);
			while(resource.hasMoreElements()) {
				URL url = resource.nextElement();
				directory = new File(url.getFile());
				if(url.toString().startsWith("jar:")) try {
					String uriString = url.toURI().toString().replace("!/"+path, "").replace("jar:", "");
					JarFile jar = new JarFile(new File(new URI(uriString)));
					Enumeration<JarEntry> e = jar.entries();
					while(e.hasMoreElements()) {
						JarEntry je = e.nextElement();
						if(je.getName().endsWith(".class") && je.getName().startsWith(path)) {
							String cname = je.getName().replace("/", ".").replace(".class", "");
							if(!classes.contains(cname))
								classes.add(cname);
						}
							
					}
					jar.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
				else if (directory.exists()) {
					String[] files = directory.list();
					for (int i = 0; i < files.length; i++) {
						if (files[i].endsWith(".class")) {
							String cname = pckgname + '.'+ files[i].substring(0, files[i].length() - 6);
							if(!classes.contains(cname))
								classes.add(cname);
							else
								// Для парсинга подпапок.
								classes.addAll(enumClasses(pckgname + "." + files[i]));
						}
					}
				} else {
					throw new ClassNotFoundException(pckgname+ " does not appear to be a valid package");
				}
			}
			} catch (NullPointerException x) {
				throw new ClassNotFoundException(pckgname + " (" + directory+ ") does not appear to be a valid package");
			}
			return classes;
	}	

}
