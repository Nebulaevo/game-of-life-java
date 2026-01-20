package com.nebulaevo.demo.GameOfLife.models;

import java.util.ArrayList;
import java.util.List;


/**
 * Object storing a collection of unique candidate cell positions 
 * and allowing to divide them into {@code ThreadWorkload} that can be processed in parallel.
 */
public class CandidateCellPositions {
	
	// STATIC ATTRIBUTES
	final private static int CORE_COUNT = Runtime.getRuntime().availableProcessors();
	final private static int MIN_CANDIDATES_BEFORE_SPLIT = 5000;
	final private static int CELLS_PER_CHUNK = 10000; // nb of grid cells for a base chunk
	final private static int DEFAULT_COLS_PER_CHUNK = 10;
	
	// INSTANCE ATTRIBUTES
	/** ⚠️ local size is not guaranteed to be the same as world grid size */
	private Size localSize;
	private int colsPerChunk;
	private int length = 0;
	
	/** Array allowing to efficiently keep track of added positions to avoid duplicates */
	private boolean[] existingPosMask;

	/** 
	 * List of chunks containing the candidate positions.
	 * <p>
	 * Candidate positions are stored in the chunk corresponding to their column range on the world grid 
	 * (making it easier to sub-divide the candidate positions into thread workloads later)
	 */
	private List<List<Position>> candidatePosChunks;
	
	/**
	 * Creates and initialises a {@code CandidateCellPositions} instance
	 * for a grid of the given size.
	 * <p>
	 * This constructor computes internal chunking parameters 
	 * based on the given dimensions.
	 * And initialises underlying data structures to sub-divide 
	 * and de-duplicated the stored candidate cell positions 
	 * based on the given dimensions.
	 * (cells that need to be computed on the next simulation update)
	 * 
	 * @param gridSize
	 * 		The size of the world grid at this moment 
	 * 		(used to initialise chunking parameters and underlying data structures)
	 */
	public CandidateCellPositions(final Size gridSize) {
		this.localSize = gridSize;
		
		int maskSize = 0;
		int lastColIdx = 0;
		int colsPerChunk = CandidateCellPositions.DEFAULT_COLS_PER_CHUNK;
		if (gridSize.height() > 0 && gridSize.width() > 0) {
			lastColIdx = gridSize.width()-1;
			maskSize = gridSize.height() * gridSize.width();
			colsPerChunk = CandidateCellPositions.CELLS_PER_CHUNK / gridSize.height();
		}
		
		if (colsPerChunk <= 0) colsPerChunk = 1;
		this.colsPerChunk = colsPerChunk;
		
		// Creating a mask matching the grid size to keep track of inserted positions
		this.existingPosMask = new boolean[maskSize];
		
		// Initialising the list of chunks 
		// depending on the computed number of columns per chunk
		int chunkCount = getChunkIndex(lastColIdx) +1;
		this.candidatePosChunks = new ArrayList<>( chunkCount );
		for (int i=0; i<chunkCount; i++) this.candidatePosChunks.add(new ArrayList<>(20));
	}
	
	/** 
	 * Expands the underlying data structures to fit a larger size.
	 * <p>
	 * Allows to guarantee that any point within the updated size can be inserted.
	 * 
	 * @param updatedSize
	 * 		The updated size of the grid, defining the range of points 
	 * 		that should fit into the structure (is required to be bigger than the current size)
	 */
	private void expand(final Size updatedSize) {
		
		// Making sure the updated size is bigger than the current one
		if ( updatedSize.width() < this.localSize.width() 
			|| updatedSize.height() < this.localSize.height()
		) {
			throw new RuntimeException(
				"Attempting to reduce the size of a CandidateCellPositions instance"
			);
		}
		
		// Creating a bigger mask structure and 
		// re-mapping the previously inserted positions
		Size previousLocalSize = this.localSize;
		boolean[] previouMask = this.existingPosMask;
		this.localSize = updatedSize;
		this.existingPosMask = new boolean[this.localSize.width() * this.localSize.height()];
		int maskIndex;
		int x = 0;
		int y = -1;
		for (boolean exists: previouMask) {
			y++;
			if (y==previousLocalSize.height()) {
				y=0;
				x++;
			}
			if (exists) {
				// insert the active cell in the new mask
				maskIndex = this.getMaskIndex(new Position(x,y));
				this.existingPosMask[maskIndex] = true;
			}
		}
		
		// Making sure we can map every column to a position chunk for the new grid size
		int lastColIdx = this.localSize.width() -1;
		int chunkCount = getChunkIndex(lastColIdx) +1;
		while (this.candidatePosChunks.size() < chunkCount) {
			this.candidatePosChunks.add(new ArrayList<>(20));
		}
	}
	
	/** 
	 * Converts the index of a column in the world grid 
	 * into its corresponding chunk index.
	 * 
	 * @param posX 
	 * 		The column index (X) of the candidate cell position.
	 */
	private int getChunkIndex(final int posX) {
		return (int) Math.floor((double) posX / this.colsPerChunk);
	}
	
	/** 
	 * Converts the positions of a cell on the world grid 
	 * into its corresponding mask index.
	 * 
	 * @param pos
	 * 		Candidate cell position
	 */
	private int getMaskIndex(final Position pos) {
		return pos.x() * this.localSize.height() + pos.y();
	}
	
	/** 
	 * Ensures the instance can fit any point within the provided boundaries.
	 * <p>
	 * Checks if the current local size can allow any points within the given grid size,
	 * and resizes the underlying data structures if needed.
	 * 
	 * @param gridSize
	 * 		The size of the grid defining the range of point 
	 * 		that should fit into the structure.
	 */
	public void checkLocalSize(final Size gridSize) {
		int necessaryWidth = Math.max(gridSize.width(), this.localSize.width());
		int necessaryHeigth = Math.max(gridSize.height(), this.localSize.height());
		
		if (necessaryWidth > this.localSize.width() || necessaryHeigth > this.localSize.height()) {
			this.expand(new Size(necessaryWidth, necessaryHeigth));
		}
	}
	
	/** 
	 * Ensures the instance can fit the provided point.
	 * <p>
	 * Checks if the given point fits within the boundaries defined by the local size,
	 * and resizes the underlying data structures if needed.
	 * 
	 * @param pos
	 * 		The candidate cell position that we want to insert
	 */
	public void checkLocalSize(final Position pos) {
		int necessaryWidth = Math.max(pos.x() +1, this.localSize.width());
		int necessaryHeigth = Math.max(pos.y() +1, this.localSize.height());
		
		if (necessaryWidth > this.localSize.width() || necessaryHeigth > this.localSize.height()) {
			this.expand(new Size(necessaryWidth, necessaryHeigth));
		}
	}
	
	/**
	 * Register a candidate cell position.
	 * <p>
	 * Positions are de-duplicated and stored into a grid chunk depending on their column range (X).
	 * 
	 * @param pos
	 * 		The candidate cell position to insert
	 */
	public void register(final Position pos) {
		// we resize the structure if the point can't be inserted
		this.checkLocalSize(pos);
		
		int maskIndex = this.getMaskIndex(pos);
		if (!this.existingPosMask[maskIndex]) {
			int chunkIndex = this.getChunkIndex(pos.x());
			
			this.existingPosMask[maskIndex] = true;
			this.candidatePosChunks.get(chunkIndex).add(pos);
			this.length ++;
		}
	}
	
	/** Returns the number of positions stored */
	public int length() { return this.length; }
	
	/** 
	 * Returns the stored candidate cells split into independent {@code ThreadWorkload} that can be processed in parallel.
	 * <p>
	 * Splits the world grid into colomn-based chunks, dividing the candidate cells positions evenly between {@code ThreadWorkload}.
	 * Each {@code ThreadWorkload} is assigned a section of the world grid and the corresponding positions to update.<br>
	 * The number of workloads created depends on the amount of candidate cells and the available processors.
	 * 
	 * @param currentGridSize
	 * 		The current size of the world grid 
	 * 		(used to ignore chunks if they are outside of the grid) 
	 */
	public List<ThreadWorkload> asThreadWorkloads(final Size currentGridSize) { 
		// ⚠️ grid size can be different than the local size (expanded structure, resized grid..)
		
		// Splitting the workload only if the number of update candidates if over the threshold
		final int THREAD_COUNT = this.length > CandidateCellPositions.MIN_CANDIDATES_BEFORE_SPLIT 
			? CandidateCellPositions.CORE_COUNT 
			: 1;
		
		List<ThreadWorkload> threadWorkloads = new ArrayList<>(THREAD_COUNT);

		// Ideal number of candidate cell position per thread
		int candidatePosPerThread = this.length / THREAD_COUNT;
		
		// Last index of current grid (might be different than the local size)
		int lastColIndex = currentGridSize.width() -1;
		
		int columnPointer = -1;
		ThreadWorkload workload = ThreadWorkload.build(0, 0);
		threadWorkloads.add(workload);
		
		for (List<Position> candidatePosChunk: this.candidatePosChunks) {
			// Move the pointer to the index of the last column in the current grid chunk
			columnPointer += this.colsPerChunk;
			
			// Adds the candidate cell positions of the current grid chunk
			// to the current workload, and updates its last column pointer
			workload.addCandidatePos(candidatePosChunk);
			workload.moveChunkEndIdx(columnPointer);
			
			// if the last column pointer of the current grid chunk 
			// is out of the world grid we don't need to keep going
			// (possible if the grid has been resized)
			if (columnPointer>=lastColIndex) { break; }
			
			// If current workload has reached the desired amount of candidate cells
			// and we haven't yet reached the desired amount of threads, 
			// we create a fresh workload to accept the next cell positions chunk 
			boolean isFull = workload.getCandidateCellCount() >= candidatePosPerThread;
			boolean isLastWorkload = threadWorkloads.size() >= THREAD_COUNT;
			if ( isFull && !isLastWorkload ) {
				workload = ThreadWorkload.build(columnPointer+1, columnPointer+1);
				threadWorkloads.add(workload);
			}
		}
		
		// In case the grid was resized, or the chunk finishes out of the grid 
		// we force the last chunk's last column pointer to be the last column of the world grid
		workload.moveChunkEndIdx(lastColIndex);
		
		return threadWorkloads;
	}
}
