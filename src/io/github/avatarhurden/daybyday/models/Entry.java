package io.github.avatarhurden.daybyday.models;

import javafx.beans.property.Property;

import org.joda.time.DateTime;

public interface Entry extends Comparable<Entry> {

	public String getUUID();

	public DateTime getCreationDate();
	
	public Property<DateTime> creationDateProperty();
	
	public boolean isEmpty();
}
