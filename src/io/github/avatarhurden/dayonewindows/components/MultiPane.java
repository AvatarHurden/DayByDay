package io.github.avatarhurden.dayonewindows.components;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class MultiPane extends AnchorPane {

	public enum MultiPaneOrientation {
		HORIZONTAL, VERTICAL;
	}
	
	private DoubleProperty shownChild;
	private NumberBinding shownHeight;
	
	private MultiPaneOrientation orientation;
	private Supplier<Double> getLength;
	private Supplier<ReadOnlyDoubleProperty> lenghtProperty;
	private BiConsumer<Node, Double> directAnchor, inverseAnchor, emptyAnchor;
	
	public MultiPane(MultiPaneOrientation orientation) {
		this(orientation, new Node[]{});
	}

	public MultiPane(MultiPaneOrientation orientation, Node... children) {
		this.orientation = orientation;
		setAnchorConsumers();
		
		shownChild = new SimpleDoubleProperty(1);
		shownHeight = shownChild.multiply(lenghtProperty.get());
		
		bindProperty();
		
		for (int i = 0; i < children.length; i++)
			getChildren().add(children[i]);
		
		shownChild.setValue(0);
	}
	
	private void setAnchorConsumers() {
		if (orientation == MultiPaneOrientation.VERTICAL) {
			directAnchor = AnchorPane::setTopAnchor;
			inverseAnchor = AnchorPane::setBottomAnchor;
			emptyAnchor = (node, value) -> {
				AnchorPane.setLeftAnchor(node, value);
				AnchorPane.setRightAnchor(node, value);
			};
			
			lenghtProperty = this::heightProperty;
			getLength = this::getHeight;
		} else if (orientation == MultiPaneOrientation.HORIZONTAL) {
			directAnchor = AnchorPane::setLeftAnchor;
			inverseAnchor = AnchorPane::setRightAnchor;
			emptyAnchor = (node, value) -> {
				AnchorPane.setTopAnchor(node, value);
				AnchorPane.setBottomAnchor(node, value);
			};

			lenghtProperty = this::widthProperty;
			getLength = this::getWidth;
		}
	}
	
	private void bindProperty() {
		getChildren().addListener((ListChangeListener.Change<? extends Node> event) -> {
			while (event.next()) {
				for (Node n : event.getAddedSubList()) {
					int i = getChildren().indexOf(n);
					emptyAnchor.accept(n, 0d);
					directAnchor.accept(n, i * getLength.get());
					inverseAnchor.accept(n, -i * getLength.get());
				}
			}
		});
		
		shownHeight.addListener((obs, oldValue, newValue) -> {
			for (int i = 0; i < getChildren().size(); i++) {
				directAnchor.accept(getChildren().get(i), i*getLength.get() - newValue.doubleValue());
				inverseAnchor.accept(getChildren().get(i), -i*getLength.get() + newValue.doubleValue());
			}
			
		});
		
		lenghtProperty.get().addListener((obs, oldValue, newValue) -> {
			for (int i = 0; i < getChildren().size(); i++)
				if (shownChild.getValue().intValue() != i) {
					directAnchor.accept(getChildren().get(i), i*getLength.get() - shownHeight.doubleValue());
					inverseAnchor.accept(getChildren().get(i), -i*getLength.get() + shownHeight.doubleValue());
				}
		});
	}
	
	public void show(Node child, boolean transition) {
		show(getChildren().indexOf(child), transition);
	}
	
	public void show(int child, boolean transition) {
		if (!transition)
			shownChild.setValue(child);
		else {
			Timeline timeline = new Timeline();
			timeline.getKeyFrames().add(new KeyFrame(new Duration(300),	new KeyValue(shownChild, child)));
			timeline.play();
		}
	}
	
}
