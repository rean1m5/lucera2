package ru.catssoftware.util;

import java.lang.reflect.Array;

public final class ArrayUtils
{
	public static final int INDEX_NOT_FOUND = -1;

	public static <T> T valid(T[] array, int index)
	{
		if (array == null)
			return null;
		if (index < 0 || array.length <= index)
			return null;
		return array[index];
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] add(T[] array, T element)
	{
		Class type = array != null ? array.getClass().getComponentType() : (element != null ? element.getClass() : Object.class);
		T[] newArray = (T[])copyArrayGrow(array, type);
		newArray[newArray.length - 1] = element;
		return newArray;
	}

	public static int[] add(int[] array, int element)
	{
		int arrayLength = array==null?1:array.length+1;
		int[] newArray = new int[arrayLength];
		if(array!=null)
			for(int i =0 ;i < array.length;i++)
				newArray[i] = array[i];
		newArray[arrayLength - 1] = element;
		return newArray;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T[] copyArrayGrow(T[] array, Class<? extends T> type)
	{
		if (array != null)
		{
			int arrayLength = Array.getLength(array);
			T[] newArray = (T[])Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
			System.arraycopy(array, 0, newArray, 0, arrayLength);
			return newArray;
		}
		return (T[])Array.newInstance(type, 1);
	}

	public static <T> boolean contains(T[] array, T value)
	{
		if (array == null)
			return false;

		for (int i = 0; i < array.length; i++)
		{
			if (value == array[i])
				return true;
		}
		return false;
	}

	public static <T> int indexOf(T[] array, T value, int index)
	{
		if (index < 0 || array.length <= index)
			return INDEX_NOT_FOUND;

		for (int i = index; i < array.length; i++)
		{
			if (value == array[i])
				return i;
		}
		return INDEX_NOT_FOUND;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] remove(T[] array, T value)
	{
		if (array == null)
			return null;

		int index = indexOf(array, value, 0);
		if (index == INDEX_NOT_FOUND)
			return array;

		int length = array.length;

		T[] newArray = (T[])Array.newInstance(array.getClass().getComponentType(), length - 1);
		System.arraycopy(array, 0, newArray, 0, index);
		if (index < length - 1)
			System.arraycopy(array, index + 1, newArray, index, length - index - 1);
		return newArray;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] remove(T[] array, int index)
	{
		if (array == null)
			return null;

		
		if (index <0 || index >=array.length)
			return array;

		int length = array.length;

		T[] newArray = (T[])Array.newInstance(array.getClass().getComponentType(), length - 1);
		System.arraycopy(array, 0, newArray, 0, index);
		if (index < length - 1)
			System.arraycopy(array, index + 1, newArray, index, length - index - 1);
		return newArray;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] join(T[][] array)
	{
		if(array==null)
			return null;
	
		int newSize = 0;
		int idx = 0;
	
		for(T[] a : array)
			newSize += a.length;

		T[] newArray = (T[])Array.newInstance(array[0].getClass().getComponentType(), newSize);
		
		for(T[] a : array)
			for(int i = 0;i<a.length;i++)
				newArray[idx++] = a[i];

		return newArray;
	}

	public static int[] join(int[][] array)
	{
		if(array==null)
			return null;

		int newSize = 0;
		int idx = 0;

		for(int[] a : array)
			newSize += a.length;
			
		int[] newArray = (int[])Array.newInstance(int.class, newSize);

		for(int[] a : array)
			for(int i = 0;i<a.length;i++)
				newArray[idx++] =a[i];

		return newArray;
	}
}
