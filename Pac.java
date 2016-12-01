/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
 */

package sim.app.pacman;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Bag;
import sim.util.Double2D;

/* The Pac is the Pac Man in the game.  Pac is an Agent and is also Steppable.  The Pac moves first, then the ghosts. */

public class Pac extends Agent implements Steppable
{
	private static final long serialVersionUID = 1;

	/** How long we wait while the Pac dies (not spinning). */
	public static final int WAIT_TIME = 100;

	/** How long we wait while the Pac spins around while dying. */
	public static final int SPIN_TIME = 100;

	/** How often the Pac rotates 90 degrees while spinning. */
	public static final int SPIN_SPEED = 5;

	/** The Pac's discretization (9), which makes him faster than the ghosts, whose discretization is 10. */
	public static final int PAC_DISCRETIZATION = 9;

	/** The current score the Pac receives for eating a ghost. */
	public int eatGhostScore = 200;

	/** The Pac's index in the player array.  This will be used to allow multiple Pacs. */
	public int tag;

	/** The stoppable for this Pac so he can remove himself when he dies if it's multiplayer */
	public Stoppable stopper;
	
	//Instance of Sensor
	Sensor sensor;
	
	//nextAction has to be available for getToGo Method
	public int nextAction;

	/** Creates a Pac assigned to the given tag, puts him in pacman.agents at the start location, and schedules him on the schedule. */
	public Pac(PacMan pacman, int tag, int[][] env) 
	{
		super(pacman);
		this.tag = tag;
		discretization = PAC_DISCRETIZATION;  // I go a bit faster
		stopper = pacman.schedule.scheduleRepeating(this, 0, 1);  // schedule at time 0

		sensor = new Sensor(pacman);
		sensor.sensEnv = env;
	}

	// the pac's start location
	public Double2D getStartLocation() { return new Double2D(pacStartX, pacStartY); }

	// Here we will check, if there are undiscovered Fields in Pacs column
	public boolean forcePacToGo (){
	
		boolean result = false;
		int [][] sensEnv = sensor.getSensEnv();
		sensor.getPositionPacX();
		sensor.getPositionPacY();
		
		//REPORT
		
				int xx = 0;
				int yy = 0;
				
				while (yy < 35){
					while (xx < 28){
						System.out.print(sensEnv[xx][yy]);
						xx++;
					}
				System.out.println();
				xx=0;
				yy++;
				}
			System.out.println("________________");
				
				
					
					
		///REPORT
		
		int s = (int)positionPacX;
		int z = (int)positionPacY;		
		
		//Check all fields in the south direction and Check, where Pac  
		//Go down step by step and check, when Pac saw the next Wall, of if he saw an open direction
		while (z < 34){
			if (sensEnv[s][z + 1] == 1){
				return result; //return 4 as direction--> we won't go south because there is a wall before there is an open space
			} else if (sensEnv [s][z + 1] == 0){
				return result = true;
			}
			z++;
		}
		
		return result;
	}

	/* Default policy implementation: Pac is controlled through the joystick/keyboard
	 * To changhe Pacs behavior derived classes should override this method
	 */
	protected void doPolicyStep(SimState state)
	{	
		sensor.setPositionPacX(positionPacX);
		sensor.setPositionPacY(positionPacY);		
		
		// Get's Pac going
		if (positionPacX == 13.5 && positionPacY == 25.0){
			nextAction = getToGo();
		}
		// If Pac stands in a columm, where he recognized a steppable Path, but he didn't chose this one (So in the 
		// sensEnv Array follows a 0 after the 2).
		// We force Pac to walk in this direction
//		else if (forcePacToGo()){
//			nextAction = 2; //We force Pac to go north, because the next undiscovered Path is in the northern direction..
//			System.out.println("We'll explore the south!");
//		}

		// If Pac's Position is even getToGo gets executed
		else if (positionPacY % 1.0 == 0 && positionPacX % 1.0 == 0.0){
			nextAction = getToGo();
		} else {
			nextAction = lastAction;
		}
		
		// pac man delays the next action until he can do it.  This requires a bit of special code
		if (isPossibleToDoAction(nextAction))
		{
			performAction(nextAction);
		}
		else if (isPossibleToDoAction(lastAction))
		{
			performAction(lastAction);
		}

	}

	/** The Method getToGo says Pac where he should go after checking Pacs surroundings.
	 * (For this getToGo calls the Methods of the Sensor "getNorth, getEast, getSouth, getWest").
	 * @returns the direction Pac has to go. 
	 *   returns 0 for north, 1 for east, 2 for south, 3 for west.
	 */
	private int getToGo() {
		// First, let's check with vision = 1
		int vision = 1;
		// Reset array for the values of the sensor check
		Double[] preferredWay = {0.0, 0.0, 0.0, 0.0};
		
		// Calls the Sensord perceptions in Pacs direct environment. x/y +-1 
		preferredWay[0] = sensor.getNorth(vision);
		preferredWay[1] = sensor.getEast(vision);
		preferredWay[2] = sensor.getSouth(vision);
		preferredWay[3] = sensor.getWest(vision);
		
		// Randomizes the values in the array with a number from 0.51 to 1 in order to shuffle the possibilities.
		// By cutting the range of possible random numbers, Pac will always choose a path with Coins if he can.
		int i = 0;
		while (i < preferredWay.length) {
			Double rdm = Math.random() + 0.51;
			if (rdm > 1.0) rdm = 1.0;
			preferredWay[i] = preferredWay[i] * rdm;
			i++;
		}
		
		// Counts how many possible ways Pac has. If there are only 2, Pac will continue 
		// walking in the previously chosen direction until he gets to a spot, where he can't walk through.
		// This stabilizes Pac, when there are no coins around him.--> We get out of those "ugly spots" a lot quicker
		int zaehler = 0;
		i = 0;
		//iterates through the array which stores the numbers which already got multiplicated with random numbers.
		//When a number is still = 0 (means: in this direction is a wall or a ghost) it's getting counted.
		while (i < preferredWay.length) {
			if (preferredWay[i] == 0.0){
				zaehler++;
			}
			i++;
		}
		//if there are at least 2 ways blocked BUT not in the direction we chose to go in the last round, Pac keeps going in this direction 
		if (zaehler > 1 && preferredWay[nextAction] > 0) {
			return lastAction;
		}

		// Gets the index of the highest number in the array a.k.a. our preferred way.
		double maxValue = 0.0;
		int richtIndex = 0;
		i = 0;

		while (i < preferredWay.length) {
			if (preferredWay[i] > maxValue) {
				maxValue = preferredWay[i];
				richtIndex = i;
			}
			i++;
		}

		// standard: path is safe to go.
		// check for vision = 2 in order to check if there's a ghost coming.
		double richtRes = 1.0;
//		System.out.println("Checking for vision = 2");
		switch (richtIndex) {
		case 0: richtRes = sensor.getNorth(vision + 1);
			break;
		case 1: richtRes = sensor.getEast(vision + 1);
			break;
		case 2: richtRes = sensor.getSouth(vision + 1);
			break;
		case 3: richtRes = sensor.getWest(vision + 1);
			break;
		default:
			System.out.println("####### This case should never occur. #######");
		}
		
		
		double maxValue2 = 0.0;
		// Check if a ghost is in our way.
		if (richtRes == 0.0) {
			i = 0;
			// If so, get the second highest number in our array so that we avoid the ghost.
			while (i < preferredWay.length) {
				if (preferredWay[i] < maxValue && preferredWay[i] > maxValue2) {
					maxValue2 = preferredWay[i];
					richtIndex = i;
				}
				i++;
			}
		}
		return richtIndex;
	}

	/* Steps the Pac.  This does various things.  First, we look up the action from the user (getNextAction).
       Then we determine if it's possible to do the action.  If not, we determine if it's possible to do the
       previous action.  Then we do those actions.  As a result we may have eaten an energizer or a dot.  If so
       we remove the dot or energizer, update the score, and possibly frighten the ghosts.  If we've eaten all
       the dots, we schedule an event to reset the level.  Next we check to see if we've encountered a ghost.
       If the ghost is frightened, we eat it and put him in jail.  Otherwise we die.
	 */
	public void step(SimState state)
	{
		doPolicyStep(state);
		// now maybe we eat a dot or energizer...

		Bag nearby = pacman.dots.getNeighborsWithinDistance(new Double2D(location), 0.3);  //0.3 seems reasonable.  We gotta be right on top anyway
		for(int i=0; i < nearby.numObjs; i++)
		{
			Object obj = nearby.objs[i];
			if (obj instanceof Energizer && pacman.dots.getObjectLocation(obj).equals(location))  // uh oh
			{
				pacman.score+=40; // only 40 because there is a dot right below the energizer.  Total should appear to be 50
				pacman.dots.remove(obj);
				eatGhostScore = 200;  // reset
				pacman.frightenGhosts = true;

				// create a Steppable to turn off ghost frightening after the ghosts have had a chance to
				// be sufficiently frightened
				pacman.schedule.scheduleOnce(new Steppable()  // the pac goes first, then the ghosts, so they'll get frightened this timestep, so we turn it off first thing next time
						{
					public void step(SimState state)
					{
						pacman.frightenGhosts = false;
					}
						}, -1);
			}
			if (obj instanceof Dot && pacman.dots.getObjectLocation(obj).equals(location))
			{
				pacman.score+=10;
				pacman.dots.remove(obj);
			}
		}
		if (nearby.numObjs > 0)
			if (pacman.dots.size() == 0)  // empty!
			{
				pacman.schedule.scheduleOnceIn(0.25, new Steppable()            // so it happens next
						{
					public void step(SimState state)
					{ 
						resetLevel();
					}
						});  // the Ghosts move a bit more
			}

		// a ghost perhaps?

		nearby = pacman.agents.getNeighborsWithinDistance(new Double2D(location), 0.3);  // 0.3 seems reasonable.  We gotta be right on top anyway
		for(int i=0; i < nearby.numObjs; i++)
		{
			Object obj = nearby.objs[i];
			if (obj instanceof Ghost && location.distanceSq(pacman.agents.getObjectLocation(obj)) <= 0.2) // within 0.4 roughly
			{
				Ghost m = (Ghost)obj;
				if (m.frightened > 0)  // yum
				{
					pacman.score += eatGhostScore;
					eatGhostScore *= 2;  // each Ghost is 2x more
					m.putInJail();
				}
				else // ouch
				{
					pacman.schedule.scheduleOnceIn(0.5, new Steppable()             // so it happens next.  Should be after resetLEvel(), so we do 0.5 rather than 0.25
							{
						public void step(SimState state)
						{ 
							die();
						}
							});  // the ghosts move a bit more
				}
			}
		}
	}


	/** Resets the level as a result of eating all the dots.  To do this we first clear out the entire
        schedule; this will eliminate everything because resetLevel() was itself scheduled at a half-time
        timestep so it's the only thing going on right now.  Clever right?  I know!  So awesome.  Anyway,
        we then schedule a little pause to occur.  Then afterwards we reset the game.
	 */
	public void resetLevel()
	{
		// clear out the schedule, we're done
		pacman.schedule.clear();

		// do a little pause
		pacman.schedule.scheduleOnce(
				new Steppable()
				{
					public int count = 0;
					public void step(SimState state) 
					{ 
						if (++count < WAIT_TIME * 2) pacman.schedule.scheduleOnce(this); 
					} 
				});

		pacman.schedule.scheduleOnceIn(WAIT_TIME * 2,
				new Steppable()
		{
			public void step(SimState state) { pacman.level++; pacman.resetGame(); }
		});
	}




	/** Dies as a result of encountering a monster.  To do this we first clear out the entire
        schedule; this will eliminate everything because die() was itself scheduled at a half-time
        timestep so it's the only thing going on right now.  Clever right?  I know!  So awesome.  Anyway,
        we then schedule a little pause to occur.  Then afterwards we schedule a period where the pac
        spins around and around by changing his lastAction.  Then finally we wait a little bit more,
        then reset the agents so they're at their start locations again.
	 */

	public void die()
	{
		pacman.deaths++;
		if (pacman.pacsLeft() > 1)
		{
			// there are other pacs playing.  We just delete ourselves.
			if (stopper != null) stopper.stop();
			stopper = null;
			pacman.agents.remove(this);
			pacman.pacs[tag] = null;
			return;
		}

		// okay so we're the last pac alive.  Let's do the little dance

		// clear out the schedule, we're done
		pacman.schedule.clear();

		// do a little pause
		pacman.schedule.scheduleOnce(
				new Steppable()
				{
					public int count = 0;
					public void step(SimState state) 
					{ 
						if (++count < WAIT_TIME) pacman.schedule.scheduleOnce(this); 
					} 
				});

		// wait a little more.
		pacman.schedule.scheduleOnceIn(WAIT_TIME,
				new Steppable()
		{
			public void step(SimState state)
			{
				// remove the Ghosts
				Bag b = pacman.agents.getAllObjects();
				for(int i = 0; i < b.numObjs; i++) {if (b.objs[i] != Pac.this) { b.remove(i); i--; } }
			}
		});

		// do a little spin
		pacman.schedule.scheduleOnceIn(WAIT_TIME + 1,
				new Steppable() 
		{ 
			public int count = 0;
			public void step(SimState state) 
			{ 
				if (count % SPIN_SPEED == 0) { lastAction = (lastAction + 1) % 4; }  // spin around
				if (++count < SPIN_TIME) pacman.schedule.scheduleOnce(this); 
			} 
		});

		// wait a little more, then reset the agents.
		pacman.schedule.scheduleOnceIn(WAIT_TIME * 2 + SPIN_TIME,
				new Steppable()
		{
			public void step(SimState state) { pacman.saveEnvironment(sensor.sensEnv); pacman.resetAgents();}
		});
	}
}
