package com.nebulaevo.demo.GameOfLife.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a workload that can be processed independently by 
 * a thread to compute the next step of the simulation
 * <p>
 * Each workload owns a delimited column-based chunk of the world grid,
 * and lists the candidate cell positions to evaluate in it.
 * 
 * @param candidatePos
 * 		List of positions that might need to be updated 
 * 		in the grid chunk
 * 
 * @param xRange
 * 		length 2 integer array defining the first and last column 
 * 		indexes of the current world grid chunk
 */
public record ThreadWorkload(List<Position> candidatePos, int[] xRange) {

	/**
	 * Builds a fresh instance of {@code ThreadWorkload}
	 * 
	 * @param lowRange
	 * 		Index of the first column of the chunk 
	 * 		in the world grid
	 * 
	 * @param highRange
	 * 		Initial value of the Index of the last column 
	 * 		of the chunk in the world grid
	 */
	public static ThreadWorkload build(final int lowRange, final int highRange) {
		return new ThreadWorkload(
			new ArrayList<>(20),
			new int[] { lowRange, highRange }
		);
	}
	
	/**
	 * Registers positions as needing to be checked for update
	 * 
	 * @param candidatePos
	 * 		List of positions on the current chunk
	 */
	public void addCandidatePos(final List<Position> candidatePos) {
		this.candidatePos().addAll(candidatePos);
	}
	
	/** Returns the index of the first column of the chunk in the world grid */
	public int getChunkFirstColIdx() {return this.xRange[0]; }
	
	/** Returns the index of the last column of the chunk in the world grid */
	public int getChunkLastColIdx(){return this.xRange[1]; }
	
	/** Returns the width of the chunk (number of columns) */
	public int chunkWidth() {
		return this.xRange[1] - this.xRange[0] +1;
	};
	
	/**
	 * Moves the index of the last column of the chunk
	 * 
	 * @param lastColIdx
	 * 		The new index of the last column of the chunk
	 */
	public void moveChunkEndIdx(final int lastColIdx) {
		this.xRange()[1] = lastColIdx;
	}
	
	/** 
	 * Returns the amount of cells that need to be evaluated in the 
	 * current chunk to compute the next step of the simulation
	 */
	public int getCandidateCellCount() {
		return this.candidatePos().size();
	}
}
