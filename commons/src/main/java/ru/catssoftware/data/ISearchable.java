package ru.catssoftware.data;

public interface ISearchable<T> {
	public ISearchable<T> search(String criteria);
}
