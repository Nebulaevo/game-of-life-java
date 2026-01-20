package com.nebulaevo.demo.GameOfLife.models;

/** 
 * Defines a set of actions that triggers side effects both in the
 * main thread and the {@code GridModel} update cycle.
 * <p>
 * Action scheduling happens on the main thread, and the side effects
 * are triggered sequentially during the process of computing a new frame.<br>
 * Side effects are executed on the relevant thread to avoid 
 * concurrency issues.
 */
public enum ScheduledAction {
	
	/** Play/pause the simulation */
	TOGGLE_PLAY,
	
	/** Clears the current world grid */
	CLEAR,
	
	/** Signals that the world grid needs to update its size */
	RESIZE_GRID,
}
