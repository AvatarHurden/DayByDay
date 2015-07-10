package io.github.avatarhurden.dayonewindows.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class VisibleListItems<K> {

	private ObservableList<K> items;
	private List<Integer> sizes;
	private Comparator<K> comparator;
	
	private DoubleProperty maxHeight;
	private double height;
	
	public VisibleListItems(Comparator<K> comparator) {
		items = FXCollections.observableArrayList();
		sizes = new ArrayList<Integer>();
		setComparator(comparator);
		maxHeight = new SimpleDoubleProperty();
	}
	
	public void setComparator(Comparator<K> comparator) {
		this.comparator = comparator;
	}
	
	public void add(K item, int itemHeight) {
		
		boolean addedBeggining = true;
		
		if (items.isEmpty()) {
			items.add(item);
			sizes.add(itemHeight);
		} else if (comparator.compare(item, get(0)) < 0) {
			items.add(0, item);
			sizes.add(0, itemHeight);
		} else if (comparator.compare(item, get(-1)) > 0) {
			addedBeggining = false;
			items.add(item);
			sizes.add(itemHeight);
		} else
			return;
		
		height += itemHeight;
		if (height > maxHeight.doubleValue()) {
			int index = addedBeggining ? items.size() - 1 : 0;
			items.remove(index);
			height -= sizes.get(index);
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
}
