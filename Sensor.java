package sim.app.pacman;

import sim.field.continuous.Continuous2D;
import sim.field.grid.IntGrid2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

/**
 * Sensor class. This class is initialized with the new Pac. Each step it gets the new position of the Pac and can therefore
 * help the Pac navigate through the maze without (probably) getting eaten by a Ghost.
 *
 */
public class Sensor {

	public IntGrid2D maze;
	public Continuous2D dots;
	public Continuous2D agents;

	public int[][] umfeld;
	public int positionPacX;
	public int positionPacY;
	
	//We want to get Pacs X position in the Pac.class. For example in the forcePacToGo-Method.
	public int getPositionPacX() {
		return positionPacX;
	}

	//We want to get Pacs Y position in the Pac.class. For example in the forcePacToGo-Method.
	public int getPositionPacY() {
		return positionPacY;
	}


	
	/**
	 * Local instance of PacMan so that we can access relevant attributes and methods.
	 */
	PacMan pacman;
	
	/**
	 * This array stores the perceptions of Pac's sensor. It only stores if the space around Pac was steppable
	 * or not. No coins are observed.
	 * 0 = The spot hasn't been discovered yet.
	 * 1 = There was a wall at this spot.
	 * 2 = There was a steppable path.
	 * 
	 * Because of the header and footer of the game, relevant information about the maze is stored in the rows (X-dimension) 3 to 31.
	 * Pac's information about the maze is being transferred into his "next life". Only when he finishes the game it gets deleted.
	 * 
	 */
	public int [][] sensEnv = new int [28] [35];
	
	/**
	 * Get the array with Pac's sensor information.
	 * 
	 * @return The sensor array.
	 */
	public int[][] getSensEnv() {
		return sensEnv;
	}

	
	/**
	 * Used to check for ghosts in Pac's vicinity. This gets called in every step so that Pac
	 * is less likely to die from his own "blindness".
	 * 
	 * @return
	 */
	public boolean callCheckforGhosts (){
		boolean result = false;
		int vision = 1;
		
		// Get the location of Pac as a Double
		Pac pac = pacman.pacs[0];
		Double locX = (double) Math.round(pac.positionPacX);
		Double locY = (double) Math.round(pac.positionPacY);
		// Round the double according to: <0.5 - then round down, >=0.5 - then round up
		
		
		Double2D location = new Double2D(locX, locY - vision);
		if (checkForGhosts(location)){
			result = true;
		}
		location = new Double2D(locX, locY + vision);
		if (checkForGhosts(location)){
			result = true;
		}
		location = new Double2D(locX + vision, locY);
		if (checkForGhosts(location)){
			result = true;
		}
		location = new Double2D(locX - vision, locY);
		if (checkForGhosts(location)){
			result = true;
		}
		return result;
	}

	/**
	 * Function for updating the X position of the Pac for the Sensor.
	 * The X-position gets rounded instantly for the sensor class.
	 * 
	 * @param positionPacX
	 */
	public void setPositionPacX(double positionPacX) {
		if (positionPacX > 26.4) {
			this.positionPacX = 0;
		} else {
			positionPacX = Math.round(positionPacX);
			this.positionPacX = (int) positionPacX;
		}
	}

	
	/**
	 * Function for updating the Y position of the Pac for the Sensor.
	 * The Y-position gets rounded instantly for the sensor class.
	 * 
	 * @param positionPacY
	 */
	public void setPositionPacY(double positionPacY) {
		positionPacY = Math.round(positionPacY);
		this.positionPacY = (int) positionPacY;
	}
		
	/**
	 * Creates a new sensor instance for the Pac. With it, we can give the sensor the needed information to check
	 * the surroundings of Pac.
	 * Get information about the maze, the ghosts and the dots in order to extract information out of it
	 * while running around.
	 * 
	 * @param pacman
	 */
	public Sensor (PacMan pacman) {
		this.maze = pacman.maze;
		this.agents = pacman.agents;
		this.dots = pacman.dots;
		
		this.pacman = pacman;

		umfeld  = maze.field;
	}

	/**
	 * This function can check for Dots at a given location. Yummy for Pac!
	 * 
	 * @param location The location you want to check.
	 * @return True if there is a dot, false if there isn't.
	 */
	public boolean checkForDots (Double2D location) {
		boolean result = false;

		// Collect all dots at the given location
		Bag bagAtLocation = dots.getObjectsAtLocation(location);

		// Iterate through the bag and check for a dot
		try {	
			for (Object obj : bagAtLocation) {
				if (obj.getClass().equals(Dot.class)) {
					result = true;
				}
			} } catch (NullPointerException e) {
				//System.out.println("No dots found at location");
			}
		return result;
	}

	/**
	 * This checks for bad, bad Ghosts like Inky, Pinky ... you get the idea. Give this function
	 * a certain location and it will look for you into it. 
	 * 
	 * @param location Given location where you want to look at.
	 * 
	 * @return Returns a boolean. True if there are some bad ghost(s) and false if there isn't/aren't.
	 */
	public boolean checkForGhosts (Double2D location) {
		boolean result = false;
		
		MutableDouble2D loc = new MutableDouble2D(location);
		
		Bag nearby = null;
		// Get the bad boys into this bag in order to iterate through them. But only get those who
		// are in distance of 0.3 (see Pac.java - they use it to locate ghosts at a certain point).
		// CHANGE 25.11.16 -- distance changed to 0.5		
		nearby = agents.getNeighborsWithinDistance(new Double2D(loc), 0.5);

		for (int i = 0; i < nearby.numObjs; i++) {
			Object obj = nearby.objs[i];

			if (obj instanceof Ghost)
			{
				// Ghost found!
				result = true;
			
				// Now let's check for an extra: If the ghost if frightened, let's try to eat him!
				Ghost ghost = (Ghost) obj;
				if (ghost.frightened > 0) {
					// Way is clear, Pac! ;)
					result = false;	
				}
			} 
			// If there is at least one Ghost at the location: get the hell out of there - and out of this
			// iteration because we no longer care about any other Ghosts! Just run!
			if (result) {
				break;
			}
		}
		return result;
	}

	/**
	 * This function checks a location north of Pac.
	 * 
	 * @param vision How far is Pac able to see? Acceptable values are 1 and 2.
	 * @return Returns a double encoded for the calling function getToGo(). In short: the higher the value, the more
	 * 			likely Pac is going to go there.
	 */
	public Double getNorth(int vision) {
		// default case is 1. This means there is just a free path - without coins and without ghosts.
		Double result = 1.0;
		
		Double2D location = new Double2D (positionPacX, positionPacY - vision);


		if (vision == 1) {
			if (umfeld[(int) location.x][(int) location.y] == 1) { // we have a wall
				//save in an Array, that the spot is unsteppable	
				sensEnv [(int) location.x][(int) location.y] = 1;
				return result = 0.0;
			}
			//If there's no wall in this direction save it in an Array.
			sensEnv [(int) location.x][(int) location.y] = 2;
				
			// if we don't have a wall, check for coins at the specified location
			// first check for the dots - they are the most liked by Pac.
			if (checkForDots(location)) {
				result = 2.0;
			}
			// check for the ghosts - Pac doesn't like them. Therefore a 0.0.
			if (checkForGhosts(location)) { 
				return result = 0.0;
			}

			// check for vision = 2 - only check for ghosts
			// Here we only care for Ghosts who may be in our path of coiiiinsssss!
		} else if (vision == 2){
			if (checkForGhosts(location)) {
				// Ah crap, better if we don't go there
				return result = 0.0;
			}
		}
		// if there is no wall and no ghosts - it is safe to go there
		return result;
	}
	

	/**
	 *	This function checks a location east of Pac.
	 * @param vision How far is Pac able to see? Acceptable values are 1 and 2.
	 * @return Returns a double encoded for the calling function getToGo(). In short: the higher the value, the more
	 * 			likely Pac is going to go there.
	 */
	public Double getEast(int vision) {
		// default case is 1. This means there is just a free path - without coins and without ghosts.
		Double result = 1.0;
		Double2D location = null;
		
		// This part of the code is to fix the problem when Pac is at the right border of the map. Without
		// it, it would result in an ArrayOutOfBoundsException. Therefore we correct the location values so that
		// Pac can look what he can expect on the other side.
		if (positionPacX == 27 && vision == 1) {
			location = new Double2D(0.0, positionPacY);
		} else if ( positionPacX == 27 && vision == 2) {
			location = new Double2D(1.0, positionPacY);
		} else {
			location = new Double2D (positionPacX + vision, positionPacY);

		}

		if (vision == 1) {
			if (umfeld[(int) location.x][(int) location.y] == 1) { // we have a wall
				//save in an Array, that the spot is unsteppable	
				sensEnv [(int) location.x][(int) location.y] = 1;
				return result = 0.0;
			}
			//If there's no wall in this direction save it in an Array.
			sensEnv [(int) location.x][(int) location.y] = 2;
			
			// if we don't have a wall, check for coins at the specified location	
			// first check for the dots - they are the most liked by Pac.
			if (checkForDots(location)) {
				result = 2.0;
			}
			// check for the ghosts - Pac doesn't like them. Therefore a 0.0.
			if (checkForGhosts(location)) { 
				return result = 0.0;
			}
			
			// check for vision = 2 - only check for ghosts
			// Here we only care for Ghosts who may be in our path of coiiiinsssss!
		} else if (vision == 2){
			if (checkForGhosts(location)) {
				// Ah crap, better if we don't go there
				return result = 0.0;
			}
		}
		// if there is no wall and no ghosts - it is safe to go there
		return result;
	}
	
	/**
	 *	This function checks a location south of Pac.
	 *
	 * @param vision How far is Pac able to see? Acceptable values are 1 and 2.
	 * 
	 * @return Returns a double encoded for the calling function getToGo(). In short: the higher the value, the more
	 * 			likely Pac is going to go there.
	 */
	public Double getSouth(int vision) {
		// default case is 1. This means there is just a free path - without coins and without ghosts.
		Double result = 1.0;
		
		Double2D location = new Double2D (positionPacX, positionPacY + vision);

		if (vision == 1) {
			if (umfeld[(int) location.x][(int) location.y] == 1) { // we have a wall
				//write in an Array, that the spot is unsteppable	
				sensEnv [(int) location.x][(int) location.y] = 1;

			return result = 0.0;
			}
			//If there's no wall in this direction save it in an Array.
			sensEnv [(int) location.x][(int) location.y] = 2;
			
			// if we don't have a wall, check for coins at the specified location
			// first check for the dots - they are the most liked by Pac.
			if (checkForDots(location)) {
				result = 2.0;
			}
			// check for the ghosts - Pac doesn't like them. Therefore a 0.0.
			if (checkForGhosts(location)) { 
							
				return result = 0.0;
			}
			
			// check for vision = 2 - only check for ghosts
			// Here we only care for Ghosts who may be in our path of coiiiinsssss!
		} else if (vision == 2) {
			if (checkForGhosts(location)) {
				
				// Ah crap, better if we don't go there
				return result = 0.0;
			}
		}
		// if there is no wall and no ghosts - it is safe to go there
		return result;
	}

	/**
	 *	This function checks a location west of Pac.
	 *
	 * @param vision How far is Pac able to see? Acceptable values are 1 and 2.
	 * 
	 * @return Returns a double encoded for the calling function getToGo(). In short: the higher the value, the more
	 * 			likely Pac is going to go there.
	 */
	public Double getWest(int vision) {
		// default case is 1. This means there is just a free path - without coins and without ghosts.
		Double result = 1.0;
		Double2D location = null;
		
		// This part of the code is to fix the problem when Pac is at the left border of the map. Without
		// it, it would result in an ArrayOutOfBoundsException. Therefore we correct the location values so that
		// Pac can look what he can expect on the other side.
		if (positionPacX == 0.0 && vision == 1) {
			location = new Double2D(maze.getWidth() - 1, positionPacY);
		} else if (positionPacX == 0.0 && vision == 2) {
			location = new Double2D(maze.getWidth() - 2, positionPacY);
		} else {
			location = new Double2D(positionPacX - vision, positionPacY);
		}

		if (vision == 1) {
			if (umfeld[(int) location.x][(int) location.y] == 1) { // we have a wall
				//save in an Array, that the spot is steppable	(if we don't have a wall)
				sensEnv [(int) location.x][(int) location.y] = 1;
				return result = 0.0;
			}
			//If there's no wall in this direction save it in an Array.
			sensEnv [(int) location.x][(int) location.y] = 2;
			
			// if we don't have a wall, check for coins at the specified location
			// first check for the dots - they are the most liked by Pac.
			if (checkForDots(location)) {
				result = 2.0;
			}
			// check for the ghosts - Pac doesn't like them. Therefore a 0.0.
			if (checkForGhosts(location)) { 
				return result = 0.0;
			}
			
			// check for vision = 2 - only check for ghosts
			// Here we only care for Ghosts who may be in our path of coiiiinsssss!
		} else if (vision == 2){
			if (checkForGhosts(location)) {
				// Ah crap, better if we don't go there
				return result = 0.0;
			}
		}
		// if there is no wall and no ghosts - it is safe to go there
		return result;
	}
}
