package com.nebulaevo.demo.GameOfLife.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javafx.application.Platform;


/** 
 * Object encapsulating the world grid state and update logic.
 * <p>
 * All the mutation of the object happens in the same secondary thread
 * during the update cycle to prevent concurrency issues as well as 
 * preventing the grid update operation from blocking the main thread.
 */
public class GridModel {
	
	/** Size of the current grid 
	 * (Dynamically allocated depending on the size of the application window)*/
	private Size size;
	
	/** State of the world grid */
	private boolean[][] grid = new boolean[0][0];
	
	/** keep track of cell positions that will need to be computed and 
	 * allow to divide them into chunks that can be processed in parallel */
	private CandidateCellPositions candidateCellPos;
	
	/** keeps track of grid positions which state changed (to minimise re-draw operations)*/
	private RedrawPositions redrawPos; 
	
	private ExecutorService threadExecService;
	
	/** Creates and initialises a {@code GridModel} instance, 
	 * managing the world grid for the simulation. 
	 * <p>
	 * This constructor starts a thread {@code ExecutorService} 
	 * to handle update cycles and sets up a shutdown hook to kill it on exit.
	 * 
	 * @param size
	 * 		The initial size for the grid dynamically allocated 
	 * 		depending on the canvas size in the window.
	 * 		(can sometimes be 0 - 0 on start up)
	 */
	public GridModel(final Size size) {
		this.redrawPos = RedrawPositions.getFresh();
		this.candidateCellPos = new CandidateCellPositions(size);
		
		this.setSize(size);
		this.initThreadExecutionService();
	}
	
	/** 
	 * Returns current size of the grid 
	 */
	public Size getSize() { return this.size; }
	
	/**
	 * Sets the new size, resizes the world grid and adjusts 
	 * the size of the candidate cell positions container
	 * 
	 * @param size
	 * 		The new size to apply to the world grid
	 */
	private void setSize(final Size size) {
		this.size = size;
		this.resizeGrid();
		this.candidateCellPos.checkLocalSize(size);
	}
	
	/** Expands or contracts the world grid to fit the current grid size */
	private void resizeGrid() {
		boolean[][] resizedGrid = new boolean[ this.size.width() ][];
		int prevLength = this.grid.length;
		
		for (int i=0; i<resizedGrid.length; i++) {
			if (i < prevLength) {
				resizedGrid[i] = Arrays.copyOf(this.grid[i], this.size.height());
			} else {
				resizedGrid[i] = new boolean[this.size.height()];
			}
		}
		
		this.grid = resizedGrid;
	}
	
	/** 
	 * Creates the thread executor service and 
	 * sets a shutdown hook to close it when the program exits 
	 */
	private void initThreadExecutionService() {
		this.threadExecService = Executors.newVirtualThreadPerTaskExecutor();
		
		// shutdown executor service on close
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			this.threadExecService.shutdownNow();
		}));
	}
	
	/**
	 * Returns the current instance of {@code RedrawPositions}
	 * to be used for the render phase, and creates a fresh instance.
	 */
	private RedrawPositions popRedrawPos() {
		RedrawPositions currentDrawingPos = this.redrawPos;
		this.redrawPos = RedrawPositions.getFresh();
		return currentDrawingPos;
	}
	
	/**
	 * Inserts living cells in the world grid
	 * 
	 * @param drawings
	 * 		List of {@code Drawing} objects to be drawn on the grid
	 * 		containing a shape and an offset position
	 */
	private void insertDrawings(final List<Drawing> drawings) {
		
		for (Drawing drawing: drawings) {
			String[] shape = drawing.shape();
			Position offset = drawing.offset();
			
			for (int y=0; y<shape.length; y ++) {
				String line = shape[y];
				for (int x=0; x<line.length(); x++) {
					
					char c = line.charAt(x);
					if (Shape.isCellChar(c)) {
						
						Position pos = new Position(x+offset.x(), y+offset.y());
						if (!pos.isInBounds(this.size)) continue;
						
						this.grid[pos.x()][pos.y()] = true;
						
						// We mark the cell and its neighbours as active for the next grid computing
						this.markActiveCells( pos );
						
						// we mark the position as modified for drawing
						this.redrawPos.cellBirthsPos().add( pos );
					}
				}
			}
		}
	}
	
	/** Resets the grid and the underlying data structures */
	private void resetGrid() {
		this.grid = this.getEmptyGrid();
		this.candidateCellPos = new CandidateCellPositions(this.size);
		this.redrawPos = RedrawPositions.getFresh();
	}
	
	/**
	 * Starts an virtual thread that executes a grid update cycle
	 * 
	 * @param runSimulation
	 * 		Indicates if the update cycle should run a simulation step
	 * 
	 * @param uiGridSize
	 * 		Current {@code Size} of the canvas UI
	 * 
	 * @param actions
	 * 		Collection of {@code ScheduledAction} values representing actions to execute in the update cycle
	 * 
	 * @param drawings
	 * 		List of {@code Drawing} objects to insert in the grid 
	 * 		at the end of the update cycles
	 * 
	 * @param renderCallback
	 * 		{@code Consumer} Function executed on the main thread once the
	 * 		update cycle finishes
	 */
	public void runAsyncUpdateCycle(
			final boolean runSimulation,
			final Size uiGridSize,
			final Collection<ScheduledAction> actions, 
			final List<Drawing> drawings, 
			final Consumer<GridRenderingRessources> renderCallback 
	) {
		
		this.threadExecService.submit(() -> {
			
			int updatedCellsCount;
			
    		this.executeScheduledActions(actions, uiGridSize);
    		if (runSimulation) {
    			updatedCellsCount = this.candidateCellPos.length();
    			this.computeNextGridState(); // don't run simulation on pause
    		} else updatedCellsCount = 0;
    		
    		this.insertDrawings(drawings);
    		
    		// Scheduling rendering to be executed next tick
    		// on the main thread
    		GridRenderingRessources renderingRessources = new GridRenderingRessources(
    			this.grid, this.size, this.popRedrawPos(), updatedCellsCount
    		);
    		Platform.runLater(() -> renderCallback.accept(renderingRessources));
    		
		});
	}
	
	/**
	 * Executes all side effects related to the {@code GridModel} instance 
	 * for the given set of actions
	 * <p>
	 * This allows side effects modifying the world grid to be executed sequentially 
	 * with the other world grid mutation and avoir concurrency problems
	 * 
	 * @param actions
	 * 		Collection of {@code ScheduledAction} values representing actions to execute in the update cycle
	 * 
	 * @param uiGridSize
	 * 		Current {@code Size} of the canvas UI
	 */
	private void executeScheduledActions(final Collection<ScheduledAction> actions, final Size uiGridSize) {
		for (ScheduledAction action: actions) {
			switch (action) {
				case ScheduledAction.CLEAR:
					this.resetGrid();
					break;
				
				case ScheduledAction.RESIZE_GRID:
					this.setSize(uiGridSize);
					break;
					
				default:
					break;
			}
		}
	}
	
	/**
	 * Starts multiple thread to compute the next step of the simulation
	 * <p>
	 * 1. Candidate positions are extracted and split into independent {@code ThreadWorkload} 
	 * using the current {@code CandidateCellPositions} instance.<br>
	 * 2. Each thread gets assigned a task computing the chunk of the grid corresponding to its workload<br>
	 * 3. The results are merged to assemble a new grid, set the redraw positions and the candidate cells for the next simulation step.
	 */
	private void computeNextGridState() {
		
		boolean[][] newGrid = this.getEmptyGrid(); 
		List<ThreadWorkload> threadWorkloads = this.candidateCellPos.asThreadWorkloads(this.size);
		this.candidateCellPos = new CandidateCellPositions(this.size);
		
		// Generating thread tasks
		List<Callable<ThreadResult>> tasks = new ArrayList<>();
		for (ThreadWorkload workload: threadWorkloads) {
			
			tasks.add(() -> {
				boolean[][] gridChunk = new boolean[workload.chunkWidth()][this.size.height()];
				RedrawPositions localdrawingPos = RedrawPositions.getFresh();
				List<Position> livingCells = new ArrayList<>(workload.candidatePos().size() / 5);
				
				for (Position pos: workload.candidatePos()) {
					Position chunkPosition = new Position(
						pos.x()-workload.getChunkFirstColIdx(),
						pos.y()
					);
					
					// double check position is on grid
					if (!pos.isInBounds(this.size)) continue;
					
					boolean wasAlive = this.grid[pos.x()][pos.y()];
					boolean isAlive = false;
					int aliveNeighbourCount = this.getAliveNeighboursCount(pos);
					
					if (wasAlive) {
						isAlive = aliveNeighbourCount>1 && aliveNeighbourCount<4;
					} else {
						isAlive = aliveNeighbourCount==3;
					}
					
					if (isAlive) {
						gridChunk[chunkPosition.x()][chunkPosition.y()] = true;
					}
					
					// we mark the position as modified if start and end state are different
					if (wasAlive != isAlive) {
						if (isAlive) localdrawingPos.cellBirthsPos().add(pos);
						else localdrawingPos.dyingCellsPos().add(pos);
					}
					
					// We mark the cell and its neighbours as active for the next grid computing
					if (isAlive) livingCells.add(pos);
				}
				return new ThreadResult(
					workload.getChunkFirstColIdx(), 
					gridChunk, 
					localdrawingPos, 
					livingCells
				);
			});
		}
		
		// Executing tasks and gathering results
		List<ThreadResult> results = new ArrayList<>();
		if (tasks.size() > 1) {
			// Spawning threads only if multiple tasks are defined
			List<Future<ThreadResult>> futureResults;
			try {
				futureResults = this.threadExecService.invokeAll(tasks);
				
				futureResults.forEach(result -> {
					try {
						results.add( result.get() );
					} catch (Exception err) {
						err.printStackTrace();
						throw new RuntimeException( "Thread execution failed with : " + err.getMessage() );
					}
			    });
			} catch (Exception err) {
				err.printStackTrace();
				throw new RuntimeException( "Thread execution failed with : " + err.getMessage() );
			}
		
		} else {
			// If only one task is defined we execute it in the current thread
			ThreadResult result;
			try {
				result = tasks.get(0).call();
			} catch (Exception err) {
				err.printStackTrace();
				throw new RuntimeException( "Thread execution failed with : " + err.getMessage() );
			}
			results.add(result);
		}
		
		// Assembling the new grid 
		// and registering tracked positions into the corresponding data structures
		for (ThreadResult result: results) {
			int xOffset = result.xStartIdx();
			for (int i=0; i<result.gridChunk().length; i++) {
				newGrid[i + xOffset] = result.gridChunk()[i];
			}
			
			this.redrawPos.inject(result.redrawPos());
			for (Position pos: result.livingCells()) this.markActiveCells(pos);
		}
		
		this.grid = newGrid;
	}
	
	/** Returns an empty world grid with the current grid size */
	private boolean[][] getEmptyGrid() {
		return new boolean[this.size.width()][this.size.height()]; 
	}
	
	/**
	 * Returns the amount of alive neighbour cells for a given position
	 * 
	 * @param pos
	 * 		The position for which we want to count alive neighbours
	 */
	private int getAliveNeighboursCount(final Position pos) {
		int acc = 0;
		Position[] inBoundNeighboursPos = this.getInBoundNeighbourPositions(pos);
		for (Position nPos: inBoundNeighboursPos) {
			if (this.grid[nPos.x()][nPos.y()]) {
				acc ++;
			}
		}
		
		return acc;
	}
	
	/** 
	 * Register the given alice cell position as well as its circle of influence 
	 * as candidate cells to be checked on the next simulation step.
	 * 
	 * @param aliveCellPos
	 * 		The alive cell position for which we want to register 
	 * 		the circle of influence as candidate cells
	 */
	private void markActiveCells(final Position aliveCellPos) {
		this.candidateCellPos.register(aliveCellPos);
		
		// listing surrounding in bound neighbours
		Position[] inBoundNeighboursPos = this.getInBoundNeighbourPositions(aliveCellPos);
		for (Position neighbourPos: inBoundNeighboursPos) this.candidateCellPos.register(neighbourPos);
	}
	
	/**
	 * Returns an array containing the neighbour positions on the grid for a given position
	 * 
	 * @param pos
	 * 		The position on the grid we want the neighbours of
	 */
	private Position[] getInBoundNeighbourPositions(final Position pos) {
		int x = pos.x();
		int y = pos.y();
		Position[] possibleNeighbours = {
			new Position(x-1, y-1),
			new Position(x, y-1),
			new Position(x+1, y-1),
			new Position(x+1, y),
			new Position(x+1, y+1),
			new Position(x, y+1),
			new Position(x-1, y+1),
			new Position(x-1, y),
		};
		
		return Arrays.stream(possibleNeighbours)
          .filter(p -> p.isInBounds(this.size))
          .toArray(Position[]::new);
	}
}

