# 🦠 Conway's Game of Life
**High-efficiency cellular automaton simulation optimised for large grids, developed using `Java 21`, `Maven 3.8` and `JavaFX 21`**

📈 Up to **2.7 million** cells computed every second for a **1.9 million** cell grid simulation running on a Debian VM with 6 dedicated CPU threads (3GHz base speed)

![](/doc/media/intense-simulation.gif)

▶️ **[Download the full simulation video (1m41s)](https://github.com/Nebulaevo/game-of-life-java/releases/download/v1.0.0/simulation-recording.mp4)**

## What is this Project ?

The goal of this project is to explore performance and scalability limits of Conway's Game of Life while serving as a practical playground for experimenting with modern Java and its concurrency features.

## Goals and Specifications

### 🎯 Respecting Game of Life Simulation Rules

- The world is divided into cells, each having two possible states: 
    - Alive: *(blue)* 
    - Dead: *(grey)*
- Each cell state is re-evaluated on every cycle, following a simple set of rules:
    - Living cells with less than 2 living neighbours die *(isolation)*
    - Living cells with more than 3 living neighbours die *(overcrowding)*
    - Dead cells with exactly 3 neighbours become alive *(birth)*

### 🎯 Additional specifications 

- Only the world that can be drawn on the canvas exists: the grid's size changes dynamically when resizing the window
- If a cell touches the edge of the grid, it dies
- Basic simulation controls:
    - Play/pause
    - Clear grid
    - Modify rendered cell size (2 options)
    - Inserting common Game of Life shapes on the grid
    - Displaying simulation statistics


## Main Optimisations

### 🛠️ Limiting update operations

- Each update cycle lists currently active cell positions and their neighbours to limit the number of cell updates on the next cycle.
- Registered positions are deduplicated using a masked array. 

### 🛠️ Limiting draw operations on the canvas

- Each update cycle lists positions where the cell state changed to limit the draw operations.

### 🛠️ Multithreading (virtual threads)

- Grid update operations are performed on a secondary thread to prevent blocking the main thread.
- If the number of cells scheduled for update is over a certain threshold, 
the next grid state is divided into column-based chunks computed in parallel by multiple threads.
    - The number of threads created depends on the number of processors available to the program
    - The number of columns assigned to each thread is determined dynamically to ensure a similar number of update operations per thread

## Cloning and Executing the Program Locally

Prerequisites :
- **git**
- **Java** (21 or later)
- **Maven** (3.8 or later)

1. Clone the repository
```bash
git clone https://github.com/Nebulaevo/game-of-life-java.git
```

2. Move to the root folder & run the program
```bash
cd game-of-life-java
mvn javafx:run
```