package io.github.avatarhurden.daybyday.components;

import java.util.function.BiConsumer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.Duration;

public class DropdownPane extends AnchorPane {

	public enum DropDownDirection {
		TOP_DOWN, BOTTOM_UP, LEFT_TO_RIGHT, RIGHT_TO_LEFT
	}
	
	private Pane parent, dropdown;
	
	private DoubleProperty shown;
	
	private NumberBinding location;
	private DoubleProperty spacing;
	
	private BooleanProperty blockParent;
	
	private ImageView parentImageView;
	private SnapshotParameters snapshotParameters;

	private DropDownDirection direction;
	
	private BiConsumer<Node, Double> setNearestAnchor, setFurthestAnchor, setConstantAnchor;
	private Callback<Pane, Double> preferredParallelLength, preferredPerpendicularLength, perpendicularLength, parallelLength;
	private Callback<Pane, ReadOnlyDoubleProperty> parallelLenghtProperty;
	
	public DropdownPane(Pane parent, Pane dropdown) {
		this(parent, dropdown, DropDownDirection.TOP_DOWN);
	}
	
	public DropdownPane(Pane parent, Pane dropdown, DropDownDirection direction) {
		this.parent = parent;
		this.dropdown = dropdown;
		this.direction = direction;
		setAnchorConsumers();

		createScreenShotComponents();
		createListeners();
		
		getChildren().addAll(parent, dropdown);
		
		AnchorPane.setTopAnchor(parent, 0d);
		AnchorPane.setRightAnchor(parent, 0d);
		AnchorPane.setBottomAnchor(parent, 0d);
		AnchorPane.setLeftAnchor(parent, 0d);
		
		setFurthestAnchor.accept(dropdown, -preferredParallelLength.call(dropdown));
		setNearestAnchor.accept(dropdown, preferredParallelLength.call(dropdown));
		
		setConstantAnchor.accept(dropdown, (preferredPerpendicularLength.call(parent) - preferredPerpendicularLength.call(dropdown)) / 2);
	}
	
	private void setAnchorConsumers() {
		switch (direction) {
		case TOP_DOWN:
			parallelLenghtProperty = Pane::prefHeightProperty;
			preferredParallelLength = Pane::getPrefHeight;
			preferredPerpendicularLength = Pane::getPrefWidth;
			perpendicularLength = Pane::getWidth;
			parallelLength = Pane::getHeight;

			setNearestAnchor = AnchorPane::setBottomAnchor;
			setFurthestAnchor = AnchorPane::setTopAnchor;
			setConstantAnchor = (node, value) -> {
				AnchorPane.setLeftAnchor(node, value);
				AnchorPane.setRightAnchor(node, value);
			};
			break;
		case BOTTOM_UP:
			parallelLenghtProperty = Pane::prefHeightProperty;
			preferredParallelLength = Pane::getPrefHeight;
			preferredPerpendicularLength = Pane::getPrefWidth;
			perpendicularLength = Pane::getWidth;
			parallelLength = Pane::getHeight;

			setNearestAnchor = AnchorPane::setTopAnchor;
			setFurthestAnchor = AnchorPane::setBottomAnchor;
			setConstantAnchor = (node, value) -> {
				AnchorPane.setLeftAnchor(node, value);
				AnchorPane.setRightAnchor(node, value);
			};
			
			break;
		case LEFT_TO_RIGHT:
			parallelLenghtProperty = Pane::prefWidthProperty;
			preferredParallelLength = Pane::getPrefWidth;
			preferredPerpendicularLength = Pane::getPrefHeight;
			perpendicularLength = Pane::getHeight;
			parallelLength = Pane::getWidth;

			setNearestAnchor = AnchorPane::setRightAnchor;
			setFurthestAnchor = AnchorPane::setLeftAnchor;
			setConstantAnchor = (node, value) -> {
				AnchorPane.setTopAnchor(node, value);
				AnchorPane.setBottomAnchor(node, value);
			};
			break;
		case RIGHT_TO_LEFT:
			parallelLenghtProperty = Pane::prefWidthProperty;
			preferredParallelLength = Pane::getPrefWidth;
			preferredPerpendicularLength = Pane::getPrefHeight;
			perpendicularLength = Pane::getHeight;
			parallelLength = Pane::getWidth;

			setNearestAnchor = AnchorPane::setLeftAnchor;
			setFurthestAnchor = AnchorPane::setRightAnchor;
			setConstantAnchor = (node, value) -> {
				AnchorPane.setTopAnchor(node, value);
				AnchorPane.setBottomAnchor(node, value);
			};
			break;
		}
	}

	private void createScreenShotComponents() {
		snapshotParameters = new SnapshotParameters();
		
		parentImageView = new ImageView();
		parentImageView.setEffect(new BoxBlur(7, 7, 3));
		
		parent.getChildren().add(parentImageView);
		parentImageView.setVisible(false);
	}

	private void createListeners() {
		shown = new SimpleDoubleProperty();
		spacing = new SimpleDoubleProperty(0);
		location = shown.multiply(parallelLenghtProperty.call(dropdown).add(spacing));
		
		location.addListener((obs, oldValue, newValue) -> {
			setFurthestAnchor.accept(dropdown, -preferredParallelLength.call(dropdown) + newValue.doubleValue());
			setNearestAnchor.accept(dropdown, parallelLength.call(parent) - newValue.doubleValue());
		});
		
		parent.layoutBoundsProperty().addListener((obs, oldValue, newValue) -> {

			setNearestAnchor.accept(dropdown, parallelLength.call(parent) - location.doubleValue());
			setConstantAnchor.accept(dropdown, (perpendicularLength.call(parent) - preferredPerpendicularLength.call(dropdown)) / 2);

	    	if (blockParent.get()) {
	    		snapshotParameters.setViewport(new Rectangle2D(0, 0, newValue.getWidth(), newValue.getHeight()));
				takeScreenShot();
	    	}
		});
		
		blockParent = new SimpleBooleanProperty(true);
	}
	
	private void takeScreenShot() {
		boolean wasImageVisible = parentImageView.isVisible();
		boolean wasDropdownVisible = dropdown.isVisible();
		
		parentImageView.setVisible(false);
		dropdown.setVisible(false);
		
		Image frostImage = parent.snapshot(snapshotParameters, null);
		parentImageView.setImage(frostImage);

		parentImageView.setVisible(wasImageVisible);
		dropdown.setVisible(wasDropdownVisible);
	}
	
	private void setPosition(boolean animate, double value) {
		parentImageView.setVisible(value > 0);
		
		if (blockParent.get())
			takeScreenShot();
		
		if (!animate)
			shown.setValue(value);
		else {
			Timeline timeline = new Timeline();
			timeline.getKeyFrames().add(new KeyFrame(new Duration(200),	
    				new KeyValue(parentImageView.opacityProperty(), value),
    				new KeyValue(shown, value)));
			timeline.play();
		}
	}
	
	public void hide(boolean animate) {
		setPosition(animate, 0);
	}
	
	public void show(boolean animate) {
		setPosition(animate, 1);
	}
	
	public void setBlockParent(boolean block) {
		blockParent.setValue(block);
	}
	
	public boolean getBlockParent() {
		return blockParent.get();
	}
	
	public BooleanProperty blockParentProperty() {
		return blockParent;
	}
	
	
	
	public void setSpacing(double spacing) {
		this.spacing.setValue(spacing);
	}
	
	public double getSpacing() {
		return spacing.get();
	}
	
	public DoubleProperty spacingProperty() {
		return spacing;
	}
}
