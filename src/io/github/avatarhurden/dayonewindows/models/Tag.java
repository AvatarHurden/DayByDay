package io.github.avatarhurden.dayonewindows.models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Tag {

	private ObservableList<DayOneEntry> entries;
	private final String name;
	
	public Tag(String name) {
		this.name = name;
		entries = FXCollections.observableArrayList();
	}
	
	public String getName() {
		return name;
	}
	
	public ObservableList<DayOneEntry> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		return name;
	}
}
