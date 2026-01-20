package com.nebulaevo.demo.GameOfLife.models;

import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Object keeping track of the simulation statistics, and providing reactive
 * StringProperty values that can be easily displayed in the view.
 */
public class SimulationStats {
	
	/** Amount of time before updating the statistics (ms) */
	private static final int STAT_COMPUTE_FREQUENCY_MS = 2000;
	
	private int computedFrames = 0;
	private double fpsComputingStartTimeMs = 0.0d;
	public StringProperty avgComputedFpsProperty = new SimpleStringProperty(" - ");
	
	private int computedCells = 0;
	private double cellComputingStartTimeMs = 0.0d;
	public StringProperty avgComputedCellsPerSecProperty = new SimpleStringProperty(" - ");
	
	/** Initialises the FPS and computed cells counting timers */
	public void startTimers() {
		this.fpsComputingStartTimeMs = System.currentTimeMillis();
		this.cellComputingStartTimeMs = System.currentTimeMillis();
	}
	
	/** Signals that a frame have been processed */
	public void updateFpsStats() {
		this.computedFrames ++;
		
		// After the specified amount of time (SimulationStats.STAT_COMPUTE_FREQUENCY_MS)
		// we compute the performance on the last time period
		double nowMS = System.currentTimeMillis();
		double timeSpanMS = nowMS - this.fpsComputingStartTimeMs;
		if (timeSpanMS > SimulationStats.STAT_COMPUTE_FREQUENCY_MS) {
			String fpsString = String.format(
				Locale.FRANCE, "%.1f", 
				this.computedFrames / (timeSpanMS/1000)
			);
			this.avgComputedFpsProperty.set(fpsString);
			
			this.computedFrames = 0;
			this.fpsComputingStartTimeMs = nowMS;
		}
	}
	
	/** 
	 * Signals that the given number of cells have been computed 
	 * 
	 * @param computedCells
	 * 		The number of cells that have been computed
	 */
	public void updateComputedCellsStats(final int computedCells) {
		this.computedCells += computedCells;
		
		// After the specified amount of time (SimulationStats.STAT_COMPUTE_FREQUENCY_MS)
		// we compute the performance on the last time period
		double nowMS = System.currentTimeMillis();
		double timeSpanMS = nowMS - this.cellComputingStartTimeMs;
		if (timeSpanMS > SimulationStats.STAT_COMPUTE_FREQUENCY_MS) {
			double computedCellsPerSec = this.computedCells / (timeSpanMS/1000);
			String unit = "";
			
			if (computedCellsPerSec > 1_000_000) {
				computedCellsPerSec = computedCellsPerSec/1_000_000;
				unit = " Mil.";
				
			} else if (computedCellsPerSec > 1_000) {
				computedCellsPerSec = computedCellsPerSec/1_000;
				unit = " K";
			}
			
			String cellsPerSecString = String.format(
				Locale.FRANCE, "%.1f", 
				computedCellsPerSec
			) + unit;

			this.avgComputedCellsPerSecProperty.set(cellsPerSecString);
			
			this.computedCells = 0;
			this.cellComputingStartTimeMs = nowMS;
		}
	}
	
}
