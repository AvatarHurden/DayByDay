package io.github.avatarhurden.dayonewindows.models;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import org.joda.time.DateTime;

public class MonthEntry implements Entry {

	private DateTime date;
	
	public MonthEntry(int year, int month) {
		date = new DateTime(year, month, 1, 0, 0);
	}
	
	public DateTime getCreationDate() {
		return date;
	}

	public int compareTo(Entry o) {
		if (o instanceof DayOneEntry 
				&& o.getCreationDate().getYear() == date.getYear()
				&& o.getCreationDate().getMonthOfYear() == date.getMonthOfYear())
			return -1;
		else
		return getCreationDate().compareTo(o.getCreationDate());
	}

	public String getUUID() {
		return "";
	}

	public Property<DateTime> creationDateProperty() {
		return new SimpleObjectProperty<DateTime>();
	}
}
