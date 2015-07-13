package io.github.avatarhurden.dayonewindows.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class VisibleListItems<K> {

	private ObservableList<K> items;
	private List<Integer> sizes;
	private Property<Comparator<? super K>> comparator;
	
	private DoubleProperty maxHeight;
	private double height = 0;
	
	public VisibleListItems(Comparator<? super K> comparator) {
		items = FXCollections.observableArrayList();
		sizes = new ArrayList<Integer>();
		this.comparator = new SimpleObjectProperty<Comparator<? super K>>(comparator);
		maxHeight = new SimpleDoubleProperty();
	}
	
	public Property<Comparator<? super K>> comparatorProperty() {
		return comparator;
	}

	public Comparator<? super K> getComparator() {
		return comparator.getValue();
	}

	public void setComparator(Comparator<? super K> comparator) {
		this.comparator.setValue(comparator);
	}
	
	public void add(K item, int itemHeight) {
		
		boolean addedBeggining = true;
		
		if (items.isEmpty()) {
			items.add(item);
			sizes.add(itemHeight);
		} else if (comparator.getValue().compare(item, get(0)) < 0) {
			items.add(0, item);
			sizes.add(0, itemHeight);
		} else if (comparator.getValue().compare(item, get(-1)) > 0) {
			addedBeggining = false;
			items.add(item);
			sizes.add(itemHeight);
		} else
			return;
		
		height += itemHeight;
		if (height > maxHeight.doubleValue() + itemHeight) {
			int index = addedBeggining ? items.size() - 1 : 0;
			height -= sizes.get(index);
			items.remove(index);
			sizes.remove(index);
		}
	}
	
	/**
	 * Gets the item at the specified index. If index is -1, returns the last item.
	 * 
	 * @param index
	 * @return
	 */
	public K get(int index) {
		if (index == -1)
			return items.get(items.size() - 1);
		else
			return items.get(index);
	}
	
	public DoubleProperty maxHeightProperty() {
		return maxHeight;
	}

	public Double getMaxHeight() {
		return maxHeight.getValue();
	}

	public void setMaxHeight(Double maxHeight) {
		this.maxHeight.setValue(maxHeight);
	}
	
	public ObservableList<K> getList() {
		return items;
	}

	public void clear() {
		items.clear();
		sizes.clear();
		height = 0;
	}
}
