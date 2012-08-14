package ru.catssoftware.gameserver.util;

/**
 * @author luisantonioa
 */
import org.apache.log4j.Logger;
import ru.catssoftware.Config;
import ru.catssoftware.gameserver.ThreadPoolManager;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.Location;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.taskmanager.MemoryWatchDog;
import ru.catssoftware.lang.L2Thread;
import ru.catssoftware.tools.util.CustomFileNameFilter;
import ru.catssoftware.util.ValueSortMap;

import java.io.File;
import java.io.FileFilter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * General Utility functions related to Gameserver
 */
public final class Util
{
	private final static Logger	_log	= Logger.getLogger(Util.class);

	public static String[] getMemUsage()
	{
		return L2Thread.getMemoryUsageStatistics();
	}

	public static void handleIllegalPlayerAction(L2PcInstance actor, String message, int punishment)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IllegalPlayerAction(actor, message, punishment), 5000);
	}

	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}

	/** Return degree value of object 2 to the horizontal line with object 1 being the origin */
	public final static double calculateAngleFrom(L2Object obj1, L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}

	/** Return degree value of object 2 to the horizontal line with object 1 being the origin */
	public final static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget += 360;

		return angleTarget;
	}

	public final static double convertHeadingToDegree(int clientHeading)
	{
		double degree = clientHeading / 182.044444444;

		return degree;
	}

	public final static int convertDegreeToClientHeading(double degree)
	{
		if (degree < 0)
			degree += 360;

		return (int) (degree * 182.044444444);
	}

	public final static int calculateHeadingFrom(L2Object obj1, L2Object obj2)
	{
		return calculateHeadingFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}

	public final static int calculateHeadingFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget += 360;

		return (int) (angleTarget * 182.044444444);
	}

	public final static int calculateHeadingFrom(double dx, double dy)
	{
		double angleTarget = Math.toDegrees(Math.atan2(dy, dx));
		if (angleTarget < 0)
			angleTarget += 360;

		return (int) (angleTarget * 182.044444444);
	}

	/**
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 */
	public final static double calculateDistance(int x1, int y1, int z1, int x2, int y2)
	{
		return calculateDistance(x1, y1, 0, x2, y2, 0, false);
	}

	public final static double calculateDistance(int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		double dx = (double) x1 - x2;
		double dy = (double) y1 - y2;

		if (includeZAxis)
		{
			double dz = z1 - z2;

			return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		}

		return Math.sqrt((dx * dx) + (dy * dy));
	}

	public final static double calculateDistance(L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return 1000000;
		return calculateDistance(obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis);
	}

	/**
	 * Capitalizes the first letter of a string, and returns the result.<BR>
	 * (Based on ucfirst() function of PHP)
	 *
	 * @param String str
	 * @return String containing the modified string.
	 */
	public static String capitalizeFirst(String str)
	{
		str = str.trim();

		if (str.length() > 0 && Character.isLetter(str.charAt(0)))
			return str.substring(0, 1).toUpperCase() + str.substring(1);

		return str;
	}

	/**
	* Capitalizes the first letter of every "word" in a string.<BR>
	* (Based on ucwords() function of PHP)
	*
	* @param String str
	* @return String containing the modified string.
	*/
	public static String capitalizeWords(String str)
	{
		char[] charArray = str.toCharArray();
		String result = "";

		// Capitalize the first letter in the given string!
		charArray[0] = Character.toUpperCase(charArray[0]);

		for (int i = 0; i < charArray.length; i++)
		{
			if (Character.isWhitespace(charArray[i]))
				charArray[i + 1] = Character.toUpperCase(charArray[i + 1]);

			result += Character.toString(charArray[i]);
		}

		return result;
	}

	public static String reverseColor(String color)
	{
		char[] ch1 = color.toCharArray();
		char[] ch2 = new char[6];
		ch2[0] = ch1[4];
		ch2[1] = ch1[5];
		ch2[2] = ch1[2];
		ch2[3] = ch1[3];
		ch2[4] = ch1[0];
		ch2[5] = ch1[1];

		return new String(ch2);
	}

	/**
	 *  Checks if object is within range, adding collisionRadius
	 */
	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if(obj1==null || obj2==null)
			return false;
		if (obj1.getInstanceId() != obj2.getInstanceId())
			return false;

		if (range == -1)
			return true; // not limited

		int rad = 0;
		if (obj1 instanceof L2Character) try {
			rad += ((L2Character) obj1).getTemplate().getCollisionRadius();
		} catch(NullPointerException npe) {
			rad += 20;
		}
		if (obj2 instanceof L2Character) try {
			rad += ((L2Character) obj2).getTemplate().getCollisionRadius();
		} catch(NullPointerException npe) {
			rad += 20;
		}

		double dx = obj1.getX() - obj2.getX();
		double dy = obj1.getY() - obj2.getY();

		if (includeZAxis)
		{
			double dz = obj1.getZ() - obj2.getZ();
			double d = dx * dx + dy * dy + dz * dz;

			return d <= range * range + 2 * range * rad + rad * rad;
		}

		double d = dx * dx + dy * dy;

		return d <= range * range + 2 * range * rad + rad * rad;
	}

	/*
	 *  Checks if object is within short (sqrt(int.max_value)) radius,
	 *  not using collisionRadius. Faster calculation than checkIfInRange
	 *  if distance is short and collisionRadius isn't needed.
	 *  Not for long distance checks (potential teleports, far away castles etc)
	 */
	public static boolean checkIfInShortRadius(int radius, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;
		if (radius == -1)
			return true; // not limited

		int dx = obj1.getX() - obj2.getX();
		int dy = obj1.getY() - obj2.getY();

		if (includeZAxis)
		{
			int dz = obj1.getZ() - obj2.getZ();
			return dx * dx + dy * dy + dz * dz <= radius * radius;
		}

		return dx * dx + dy * dy <= radius * radius;
	}

	/**
	 * Returns a delimited string for an given array of string elements.<BR>
	 * (Based on implode() in PHP)
	 *
	 * @param String[] strArray
	 * @param String strDelim
	 * @return String implodedString
	 */
	public static String implodeString(String[] strArray, String strDelim)
	{
		String result = "";

		for (String strValue : strArray)
			result += strValue + strDelim;

		return result;
	}

	/**
	 * Returns a delimited string for an given collection of string elements.<BR>
	 * (Based on implode() in PHP)
	 *
	 * @param Collection&lt;String&gt; strCollection
	 * @param String strDelim
	 * @return String implodedString
	 */
	public static String implodeString(Collection<String> strCollection, String strDelim)
	{
		return implodeString(strCollection.toArray(new String[strCollection.size()]), strDelim);
	}

	/**
	 * Returns the rounded value of val to specified number of digits
	 * after the decimal point.<BR>
	 * (Based on round() in PHP)
	 *
	 * @param float val
	 * @param int numPlaces
	 * @return float roundedVal
	 */
	public static float roundTo(float val, int numPlaces)
	{
		if (numPlaces <= 1)
			return Math.round(val);

		float exponent = (float) Math.pow(10, numPlaces);

		return (Math.round(val * exponent) / exponent);
	}

	public static File[] getDatapackFiles(String dirname, String extention)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);

		//L2EMU_EDIT
		if (!dir.exists())
		{
			_log.error("Unable to Find File " + dir + extention + ", Please Update Your Datapack!");
			return null;
		}
		//L2EMU_EDIT

		CustomFileNameFilter filter = new CustomFileNameFilter(extention);

		return dir.listFiles(filter);
	}

	public static boolean isAlphaNumeric(String text)
	{
		if (text == null)
			return false;

		boolean result = true;
		char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (!Character.isLetterOrDigit(chars[i]))
			{
				result = false;
				break;
			}
		}

		return result;
	}

	/**
	 * Returns a number formatted with "," delimiter
	 * @param value
	 * @return String formatted number
	 */
	public static String formatNumber(double value)
	{
		return NumberFormat.getInstance(Locale.ENGLISH).format(value);
	}

	/**
	 * @param s
	 */

	public static Map<Integer, Integer> sortMap(Map<Integer, Integer> map, boolean asc)
	{
		ValueSortMap vsm = new ValueSortMap();

		return vsm.sortThis(map, asc);
	}

	// ---------------  L2Emu Addons
	/**
	 * returns how many processors are installed on this system.
	 */
	private static void printCpuInfo()
	{
		_log.info("Avaible CPU(s): " + Runtime.getRuntime().availableProcessors());
		_log.info("Processor(s) Identifier: " + System.getenv("PROCESSOR_IDENTIFIER"));
		_log.info("..................................................");
		_log.info("..................................................");
	}

	/**
	 * returns the operational system server is running on it.
	 */
	private static void printOSInfo()
	{
		_log.info("OS: " + System.getProperty("os.name") + " Build: " + System.getProperty("os.version"));
		_log.info("OS Arch: " + System.getProperty("os.arch"));
		_log.info("..................................................");
		_log.info("..................................................");
	}

	/**
	 * returns JAVA Runtime Enviroment properties
	 */
	private static void printJreInfo()
	{
		_log.info("Java Platform Information");
		_log.info("Java Runtime  Name: " + System.getProperty("java.runtime.name"));
		_log.info("Java Version: " + System.getProperty("java.version"));
		_log.info("Java Class Version: " + System.getProperty("java.class.version"));
		_log.info("..................................................");
		_log.info("..................................................");
	}

	/**
	 * returns general infos related to machine
	 */
	private static void printRuntimeInfo()
	{
		_log.info("Runtime Information");
		_log.info("Current Free Heap Size: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " mb");
		_log.info("Current Heap Size: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " mb");
		_log.info("Maximum Heap Size: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " mb");
		_log.info("..................................................");
		_log.info("..................................................");
	}

	/**
	 * calls time service to get system time.
	 */
	private static void printSystemTime()
	{
		// instanciates Date Objec
		Date dateInfo = new Date();

		//generates a simple date format
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");

		//generates String that will get the formater info with values
		String dayInfo = df.format(dateInfo);

		_log.info("..................................................");
		_log.info("System Time: " + dayInfo);
		_log.info("..................................................");
	}

	/**
	 * gets system JVM properties.
	 */
	private static void printJvmInfo()
	{
		_log.info("Virtual Machine Information (JVM)");
		_log.info("JVM Name: " + System.getProperty("java.vm.name"));
		_log.info("JVM installation directory: " + System.getProperty("java.home"));
		_log.info("JVM version: " + System.getProperty("java.vm.version"));
		_log.info("JVM Vendor: " + System.getProperty("java.vm.vendor"));
		_log.info("JVM Info: " + System.getProperty("java.vm.info"));
		_log.info("..................................................");
		_log.info("..................................................");
	}

	/**
	 * prints all other methods.
	 */
	public static void printGeneralSystemInfo()
	{
		printSystemTime();
		printOSInfo();
		printCpuInfo();
		printRuntimeInfo();
		printJreInfo();
		printJvmInfo();
	}

	/**
	 * converts a given time from seconds -> miliseconds
	 * @param seconds
	 * @return
	 */
	public static int convertSecondsToMiliseconds(int secondsToConvert)
	{
		return secondsToConvert * 1000;
	}

	/**
	 * converts a given time from minutes -> miliseconds
	 * @param string
	 * @return
	 */
	public static int convertMinutesToMiliseconds(int minutesToConvert)
	{
		return minutesToConvert * 60000;
	}

	/**
	 * converts a given time from minutes -> seconds
	 * @param string
	 * @return
	 */
	public static int convertMinutesToSeconds(int minutesToConvert)
	{
		return minutesToConvert * 60;
	}

	/**
	 * @param valueX100
	 * @return
	 */
	public double convertPercentageByMultipler(double multiplerX100)
	{
		return 100 - multiplerX100;
	}

	/**
	 * @param number
	 * @param percentage
	 * @return
	 */
	public double calculatePercentage(double number, double percentage)
	{
		double values = number * percentage;
		double tmp = values / 100;

		return tmp;
	}

	/**
	 * @param path
	 * @param extension
	 * @return
	 */
	public File[] listDirFiles(String path, final String extension)
	{
		File F = new File(path);
		File[] files = F.listFiles(new FileFilter()
		{
			public boolean accept(File pathname)
			{
				return pathname.getName().toLowerCase().endsWith(extension);
			}
		});

		return files;
	}

	/**
	 * @param path
	 * @return
	 */
	public File[] listFolders(String path)
	{
		File F = new File(path);
		File[] files = F.listFiles(new FileFilter()
		{
			public boolean accept(File pathname)
			{
				return pathname.getName().toLowerCase().endsWith("");
			}
		});

		return files;
	}
	public static long gc(int i, int delay)
	{
		long freeMemBefore = MemoryWatchDog.getMemFree();
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		while(--i > 0)
		{
			try
			{
				Thread.sleep(delay);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			rt.gc();
		}
		rt.runFinalization();
		return MemoryWatchDog.getMemFree() - freeMemBefore;
	}

	public static Location findPointForDistance(Location src, Location dst, double distance) {
		double fullDistance = calculateDistance(src.getX(), src.getY(), src.getZ(), dst.getX(), dst.getY());
		if(Math.abs(distance)>=fullDistance)
			return distance>0?dst:src;
		if(distance<0)
			distance = fullDistance+distance;
		if(distance>=fullDistance)
			return dst;
		double t = distance / fullDistance;
		double l = dst.getX()-src.getX();
		double m = dst.getY()-src.getY();
		double n = dst.getZ()-src.getZ();
		int x = (int)(src.getX()+ l *t);
		int y = (int)(src.getY()+ m *t);
		int z = (int)(src.getZ()+ n*t);
		return new Location(x,y,z,calculateHeadingFrom(src.getX(), src.getY(), dst.getX(), dst.getY()));
	}
	
	public static <T> Collection<T> filter(Collection<T> l, Filter<T> filter) {
		ArrayList<T> result = new ArrayList<T>();
	    Iterator<T> it= l.iterator();
	    while(it.hasNext()) {
	      T item = it.next();
	      if(filter.match(item)) {
	        result.add(item);
	      }
	    }
	    return result;
	}
	

	public interface Filter<T> {
	  public boolean match(T o);
	}

	public static void pause(long time)
	{
		try
		{
			Thread.sleep(time);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}