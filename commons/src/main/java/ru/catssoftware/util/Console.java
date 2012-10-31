package ru.catssoftware.util;

public class Console {
	public static void printSection(String s)
	{
		System.out.println(getSection(s));
	}

	public static String getSection(String s)
	{
		int maxlength = 79;
		s = "-[ " + s + " ]";
		int slen = s.length();
		if (slen > maxlength)
		{
			return s;
		}

		for (int i = 0; i < (maxlength - slen); i++)
			s = "=" + s;

		return s;
	}

}
