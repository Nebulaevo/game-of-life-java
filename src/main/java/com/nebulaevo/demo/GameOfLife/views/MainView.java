package com.nebulaevo.demo.GameOfLife.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Separator;

import com.nebulaevo.demo.GameOfLife.models.CanvasDrawMode;
import com.nebulaevo.demo.GameOfLife.models.Drawing;
import com.nebulaevo.demo.GameOfLife.models.GridModel;
import com.nebulaevo.demo.GameOfLife.models.ScheduledAction;
import com.nebulaevo.demo.GameOfLife.models.SimulationStats;

/** Main view element coordinating data models and dedicated view elements */
public class MainView extends VBox {
	
	/** Maximum frames computed per seconds */
	private final static double BASE_FPS = 30.0d;
	
	// MODELS
	private GridModel gridModel;
	private SimulationStats simulationStats;
	
	// VIEWS
	private ToolBarView toolBarView;
	private Separator separatorUi;
	private GridView gridView;
	
	// FRAMES COMPUTING 
	private Timeline timeline;
	private AtomicBoolean isComputingFrame = new AtomicBoolean(false);
	
	private Set<ScheduledAction> scheduledActions = new HashSet<ScheduledAction>();
	private List<Drawing> scheduledDrawings = new ArrayList<>();
	
	private BooleanProperty playProperty = new SimpleBooleanProperty(false);
	private CanvasDrawMode drawMode = CanvasDrawMode.OPTIMISED;
	
	/** Creates an empty {@code MainView} instance that needs to be initialised */
	public MainView() { super(0); }
	
	
	/** Initialises the UI elements and the models of the main view
	 * <p>
	 * Using a separate method to initialise the main view allows to
	 * setup the models and UI elements on the second rendering tick, 
	 * which increases the chances of the window size to be defined.<br>
	 * Limiting the visual glitches and useless resizing operations 
	 * related to window size bindings.<br>
	 * (Note: size of the main window can still be 0, 0 on the second rendering tick)
	 */
	public void init() {
		this.simulationStats = new SimulationStats();
		
		// initialise and add UI elements 
		this.initUIElements();
		this.getChildren().addAll(this.toolBarView, this.separatorUi, this.gridView);
		
		// initialise the grid model and
		// starting the frame computing timeline
		this.initGridModel();
		
		// start the main rendering cycle
		this.start();
	}
	
	/** Creates the UI elements instance and sets up their bindings */
	private void initUIElements() {
		this.separatorUi = new Separator();
		this.gridView = new GridView();
		this.toolBarView = new ToolBarView(
			action -> this.scheduledActions.add(action),
			size -> this.gridView.setCellSize(size),
			margin -> this.gridView.setCellMargin(margin),
			drawing -> this.scheduledDrawings.add(drawing),
			() -> this.gridModel.getSize()
		);
		
		this.gridView.setupSizeBindings(
			this.widthProperty(),
			this.heightProperty()
				.subtract( this.separatorUi.heightProperty() ) 
				.subtract( this.toolBarView.heightProperty() )
		);
		
		// setup toolbar data bindings
		this.toolBarView.setupDataBindings( 
			this.playProperty, this.simulationStats
		);
	}
	
	/** 
	 * Initialises the grid model instance
	 * and adds an event listener triggering a resize action
	 * when the canvas size changes.
	 */
	private void initGridModel() {
		this.gridModel = new GridModel( this.gridView.getMaxGridSize() );
		
		// Set up automatic grid resizing
		// (Using debounce to prevent chain grid resizing.)
		PauseTransition debounce = new PauseTransition(Duration.millis(100));
		ChangeListener<Number> changeHandler = (obs, o, n) -> {
			debounce.setOnFinished(e -> this.scheduledActions.add(ScheduledAction.RESIZE_GRID));
			debounce.playFromStart();
		};
		
		this.gridView.widthProperty().addListener(changeHandler);
		this.gridView.heightProperty().addListener(changeHandler);
	}
	
	/** Starts the main rendering cycle */
	private void start() {
		if (this.timeline != null) this.timeline.stop();
		
		// initialise statistic timers
		this.simulationStats.startTimers();
		
		this.timeline = new Timeline(
			new KeyFrame(Duration.seconds(1.0d / MainView.BASE_FPS), e -> {
				this.computeFrame();
			})
		);
		
		this.timeline.setCycleCount(Animation.INDEFINITE);
		this.timeline.play();
	}
	
	/** 
	 * Execute scheduled actions, computes the next world grid state and renders it on the canvas
	 * <p>
	 * <li>Only one instance of this method can be executed at a time</li>
	 * <li>The new state of the grid model is computed in a secondary "update thread" 
	 * that calls the grid render callback on completion.</li>
	 * <li>Scheduled actions side effects are computed on the relevant thread</li>
	 */
	private void computeFrame() {
		// Using an atomic boolean to ensure only one rendering process
		// can run at the time (skipping frames if still computing)
    	if (!this.isComputingFrame.compareAndSet(false, true)) return;
    	
    	// scheduled actions: executing side effects linked to the main thread
    	this.preUpdateActions();
    	
    	this.gridModel.runAsyncUpdateCycle(
    		
			// Extracting and resetting scheduled actions before entering the
	    	// compute thread to avoid concurrency issues
			this.playProperty.get(), 
			this.gridView.getMaxGridSize(), 
			this.popScheduledActions(), 
			this.popScheduledDrawings(), 
			
			// Render callback that will be executed on the main thread 
			// when the grid has been updated
			renderingRessources -> {
    			if (this.drawMode == CanvasDrawMode.FULL) {
    				this.drawMode = CanvasDrawMode.OPTIMISED;
    				// Re-drawing the grid entirely
    				this.gridView.drawGrid(
    					renderingRessources.grid(), renderingRessources.size()
					);
    			} else {
    				// Re-drawing only provided positions
    				this.gridView.drawGrid(
    					renderingRessources.grid(), renderingRessources.size(),
    					renderingRessources.redrawPositions()
					);
    			}
    			
    			// Update simulation statistics
    			this.simulationStats.updateFpsStats();
    			this.simulationStats.updateComputedCellsStats(renderingRessources.updatedCellsCount());
    			
    			// Indicate that frame rendering finished
    			this.isComputingFrame.set(false);
    	});
	}
	
	/** Returns the current set of {@code ScheduledAction} and initialises a new set */
	private Set<ScheduledAction> popScheduledActions() {
		Set<ScheduledAction> actions = this.scheduledActions;
		this.scheduledActions = new HashSet<>();
		return actions;
	}
	
	/** Returns the current list of {@code Drawing} and initialises a new list */
	private List<Drawing> popScheduledDrawings() {
		List<Drawing> drawings = this.scheduledDrawings;
		this.scheduledDrawings = new ArrayList<>();
		return drawings;
	}
	
	/**
	 * Executes all side effects related to the {@code MainView} 
	 * instance for the given set of actions
	 * <p>
	 * Other side effects related to the {code GridModel} instance 
	 * are executed in the same thread as the grid update to prevent
	 * any concurrency issues
	 */
	private void preUpdateActions() {
		for (ScheduledAction action: this.scheduledActions) {
			switch (action) {
				case ScheduledAction.TOGGLE_PLAY:
					this.playProperty.set( !this.playProperty.get() );
					break;
				
				case ScheduledAction.CLEAR:
				case ScheduledAction.RESIZE_GRID:
					this.drawMode = CanvasDrawMode.FULL;
					break;
					
				default:
					break;
			}
		}
	}
}

