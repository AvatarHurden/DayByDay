package io.github.avatarhurden.dayonewindows.models;

import javafx.beans.property.Property;

import org.joda.time.DateTime;

public interface Entry extends Comparable<Entry> {

	public DateTime getCreationDate();
	
	public String getUUID();

	public Property<DateTime> creationDateProperty();
}
