package io.github.avatarhurden.dayonewindows.models;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import org.joda.time.DateTime;

public class MonthEntry implements Entry {

	private DateTime date;
	
	public MonthEntry(int year, int month) {
		date = new DateTime(year, month, 1, 0, 0);
	}
	
	public MonthEntry(DateTime date) {
		this.date = date.withMillisOfDay(0).withDayOfMonth(1);
	}

	public String getUUID() {
		return "";
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

	public boolean equals(Object o) {
		if (!(o instanceof MonthEntry))
			return false;
		else
			return hashCode() == o.hashCode();
	}

	public int hashCode() {
		return date.hashCode();
	}
	
	public Property<DateTime> creationDateProperty() {
		return new SimpleObjectProperty<DateTime>();
	}
}
