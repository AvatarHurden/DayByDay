package io.github.avatarhurden.dayonewindows.models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Tag {

	private ObservableList<JournalEntry> entries;
	private final String name;
	
	public Tag(String name) {
		this.name = name;
		entries = FXCollections.observableArrayList();
	}
	
	public String getName() {
		return name;
	}
	
	public ObservableList<JournalEntry> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		return name;
	}
}
