import java.io.*;
import java.util.*;

public class PredictionEngine
{
	//================================================================================
    // Fields
    //================================================================================

	// Optimal prediction algorithm vote-weights, determined through testing
	final double NB_WEIGHT = .681;	/** Optimal Naive Bayes vote-weight */
	final double FB_WEIGHT = .642;	/** Optimal Full Bayes vote-weight */
	final double ANN_WEIGHT = .639;	/** Optimal Adapted Nearest Neighbor vote-weight */

	File dataFile;	/** Handle to file holding the game data */
	
	/**
	 * Container for all historical game data (courtesy of Shawn Bayern)
	 * Maps concatenation of player and computer histories to counts of players' next
	 * move at that game state (in previous played matches)
	 */
	HashMap<String, int[]> data;
	
	ArrayList<Datapoint> testPoints;
	ArrayList<Datapoint> testData;

	double[] PY;	/** Calculated evidence term for Full Bayes implementation */
	int numRocks;	/** Number of instances where player played "rocks" in the dataset */
	int numPapers;	/** Number of instances where player played "paper" in the dataset */
	int numScissors;	/** Number of instances where player played "scissors" in the dataset */
	
	
	//================================================================================
    // Constructors
    //================================================================================

	/**
	 * Creates a new PredictionEngine instance from a File handle
	 * holding the data
	 * @param  fileName	- handle to File with hada
	 */
	public PredictionEngine(File fileName)
	{
		dataFile = fileName;
		train();
		// expandInput();
	}

	/**
	 * Creates a new PredictionEngine instance from a specified file name
	 * holding the data
	 * @param  fileName	- name of file with data
	 */	
	public PredictionEngine(String fileName)
	{
		this(new File(fileName));
	}
	
	/**
	 * Creates a new PredictionEngine instance with default filename
	 * of "openings.txt" for file holding the data
	 */	
	public PredictionEngine()
	{
		this(new File("openings.txt"));
	}
	

	//================================================================================
    // Functions
    //================================================================================

	/**
	 * Get data from file and store in data hashmap, then populate evidence term
	 * and move-count variables
	 */
	private void train()
	{
		Scanner line = null;
		try {
			line = new Scanner(dataFile);
		}
		catch (IOException e) {
			System.out.println("IOException has occured. Exiting.");
			System.exit(1);
		}
		
		data = new HashMap<String, int[]>(6210);

		PY = new double[3];
		numRocks = numPapers = numScissors = 0;
		
		// All terminal moves
		while(line.hasNextLine() && line.hasNext())
		{
			String key = line.next() + line.next();
			
			int[] RPS = new int[3];
			RPS[0] = line.nextInt();
			RPS[1] = line.nextInt();
			RPS[2] = line.nextInt();
			
			numRocks += RPS[0];
			numPapers += RPS[1];
			numScissors += RPS[2];
			
			data.put(key, RPS);
		}
		
		double total = numRocks + numPapers + numScissors;
		
		PY[0] = numRocks / total;
		PY[1] = numPapers / total;
		PY[2] = numScissors / total;
	}


	/**
	 * Uses Naive Bayes to generate a prediction for the player's next move
	 * @param  player	- current history of player's moves
	 * @param  computer	- current history of computer's moves
	 * @return character ('R', 'P', or 'S') representing the predicted player's move
	 */
	public char naiveBayes(String player, String computer)
	{
		String key = player + computer;
		int[] result = data.get(key);
	
		if (player.length() > 5)
		{
			player = player.substring(player.length()-5);
			computer = computer.substring(computer.length()-5);
		}

		// Generate every valid opponent and player move
		int[] computerCounts = new int[3];
		int[] playerCounts = new int[3];
		generateComputerCombinations(player, "", player.length(), playerCounts, computerCounts);
		generatePlayerCombinations("", computer, computer.length(), playerCounts, computerCounts);
		
		// P(Y|X)=P(X1|Y)*P(X2|Y)*P(Y)
		double pRock = ((double)playerCounts[0]/numRocks) * ((double)computerCounts[0]/numRocks) * PY[0];
		double pPaper = ((double)playerCounts[1]/numPapers) * ((double)computerCounts[1]/numPapers) * PY[1];
		double pScissors = ((double)playerCounts[2]/numScissors) * ((double)computerCounts[2]/numScissors) * PY[2];
		
		// Return prediction based on calculated P(Y|X)
		if (pRock > pPaper)
		{
			if (pRock > pScissors)
			{
				return 'R';
			}
			else
			{
				return 'S';
			}
		}
		else if (pPaper > pScissors)
		{
			return 'P';
		}
		else
		{
			return 'S';
		}
	}
	
	/**
	 * Generates all valid moves that the computer can make at the current (given) state
	 * @param player	- given history of player
	 * @param partialHistory	- current partial history
	 * @param charsLeft	- represents the number of characters (moves) that can still be added
	 * @param playerCounts	- reference to 3-tuple (R, P, and S counts) for player's state
	 * @param computerCounts	- reference to 3-tuple (R, P, and S counts) for computer's state
	 */
	public void generateComputerCombinations(String player, String partialHistory, int charsLeft, int[] playerCounts, int[] computerCounts)
	{
		if (charsLeft == 0)
		{
			String key = player + partialHistory;
			int[] result = data.get(key);
			if (result != null)
			{
				playerCounts[0] += result[0];
				playerCounts[1] += result[1];
				playerCounts[2] += result[2];
			}

			return;
		}

		generateComputerCombinations(player, partialHistory.concat("r"), charsLeft-1, playerCounts, computerCounts);
		generateComputerCombinations(player, partialHistory.concat("p"), charsLeft-1, playerCounts, computerCounts);
		generateComputerCombinations(player, partialHistory.concat("s"), charsLeft-1, playerCounts, computerCounts);
	}
	
	/**
	 * Generates all valid moves that the player can make at the current (given) state
	 * @param player	- given history of player
	 * @param partialHistory	- current partial history
	 * @param charsLeft	- represents the number of characters (moves) that can still be added
	 * @param playerCounts	- reference to 3-tuple (R, P, and S counts) for player's state
	 * @param computerCounts	- reference to 3-tuple (R, P, and S counts) for computer's state
	 */
	public void generatePlayerCombinations(String partialHistory, String computer, int charsLeft, int[] playerCounts, int[] computerCounts)
	{
		if (charsLeft == 0)
		{
			String key = partialHistory + computer;
			int[] result = data.get(key);
			if (result != null)
			{
				computerCounts[0] += result[0];
				computerCounts[1] += result[1];
				computerCounts[2] += result[2];
			}

			return;
		}

		generatePlayerCombinations(partialHistory.concat("r"), computer, charsLeft-1, playerCounts, computerCounts);
		generatePlayerCombinations(partialHistory.concat("p"), computer, charsLeft-1, playerCounts, computerCounts);
		generatePlayerCombinations(partialHistory.concat("s"), computer, charsLeft-1, playerCounts, computerCounts);
	}
	
	/**
	 * Uses "Full Bayes" (no conditional independence assumption) to generate a 
	 * prediction for the player's next move
	 * @param  player	- current history of player's moves
	 * @param  computer	- current history of computer's moves
	 * @return character ('R', 'P', or 'S') representing the predicted player's move
	 */
	public char fullBayes(String player, String computer)
	{
		String key = player + computer;
		int[] result = data.get(key);
		
		// If point not in dataset, predict on the prior
		if (result == null)
		{
			result = new int[3];
			result[0] = (int)(100*PY[0]);
			result[1] = (int)(100*PY[1]);
			result[2] = (int)(100*PY[2]);
		}
		
		// Choose prediction over a probability distribution (to increase variability)
		char prediction;
		double rand = Math.random();
		double total = result[0] + result[1] + result[2];
		if (rand < result[0] / total)
		{
			prediction = 'R';
		}
		else if (rand < (result[0] + result[1]) / total)
		{
			prediction = 'P';
		}
		else
		{
			prediction = 'S';
		}
		
		return prediction;
	}
	
	/**
	 * Uses "Adapted Nearest Neighbor" (essentially combining Full Bayes with a subgame
	 * history search) to generate a prediction for the player's next move
	 * @param  player	- current history of player's moves
	 * @param  computer	- current history of computer's moves
	 * @return character ('R', 'P', or 'S') representing the predicted player's move
	 */
	public char adaptedNN(String player, String computer)
	{
		String key = player + computer;
		int[] result = data.get(key);
		
		// If point not in dataset, find subgame history result
		while (result == null)
		{
			player = player.substring(1);
			computer = computer.substring(1);
			key = player + computer;
			result = data.get(key);
		}
		
		// Choose prediction over a probability distribution
		char prediction;
		double rand = Math.random();
		double total = result[0] + result[1] + result[2];
		if (rand < result[0] / total)
		{
			prediction = 'R';
		}
		else if (rand < (result[0] + result[1]) / total)
		{
			prediction = 'P';
		}
		else
		{
			prediction = 'S';
		}
		
		return prediction;
	}

	/**
	 * Uses the combination of Naive Bayes, Full Bayes, and Adapted Nearest Neighbor in an
	 * ensemble-vote style to predict the player's next move from the current game history
	 * @param  player	- history of player moves
	 * @param  computer	- history of computer moves
	 * @return the character ('R', 'P', 'S') representing the player's predicted move
	 */
	public char determineOptimalMove(String player, String computer) {
		double R, P, S;
		R = P = S = 0;

		char nbPrediction, fbPrediction, annPrediction;

		// get the Naive Bayes predicted move, increase the weight for corresponding computer move
		nbPrediction = chooseMove(naiveBayes(player, computer));
		if (nbPrediction == 'R') {
			R += NB_WEIGHT;
		}
		else if (nbPrediction == 'P') {
			P += NB_WEIGHT;
		}
		else {
			S += NB_WEIGHT;
		}
		
		// get the Full Bayes predicted move, increase the weight for corresponding computer move
		fbPrediction = chooseMove(fullBayes(player, computer));
		if (fbPrediction == 'R') {
			R += FB_WEIGHT;
		}
		else if (fbPrediction == 'P') {
			P += FB_WEIGHT;
		}
		else {
			S += FB_WEIGHT;
		}
		
		// get the Adapted Nearest Neighbor predicted move, increase the weight for corresponding computer move
		annPrediction = chooseMove(adaptedNN(player, computer));
		if (annPrediction == 'R') {
			R += ANN_WEIGHT;
		}
		else if (annPrediction == 'P') {
			P += ANN_WEIGHT;
		}
		else {
			S += ANN_WEIGHT;
		}
		
		// return optimal weighted computer move
		if (R > P)
		{
			if (R > S)
				return 'R';
			else
				return 'S';
		}
		else if (P > S)
		{
			return 'P';
		}
		else
		{
			return 'S';
		}
	}

	/**
	 * From a prediction, chooses the move for the computer that would win the game
	 * @param  prediction	- prediction of the player's move
	 * @return the suggested move for the computer
	 */
	public char chooseMove(char prediction)
	{
		if (prediction == 'R')
			return 'P';
		else if (prediction == 'P')
			return 'S';
		else
			return 'R';
	}
	
	/**
	 * From both players' moves, determines the winner of that game
	 * @param  playerMove	- character representing player's current move
	 * @param  compMove	- character representing computer's current move
	 * @return character ('H', 'C', 'D') representing who won the game: 'human',
	 * 'computer', or 'draw'
	 */
	public char determineWinner(char playerMove, char compMove)
	{
		playerMove = Character.toUpperCase(playerMove);
		compMove = Character.toUpperCase(compMove);
		if (playerMove == compMove)
		{
			return 'D';
		}
		else if (playerMove == 'R')
		{
			if (compMove == 'P')
				return 'C';
			else
				return 'H';
		}
		else if (playerMove == 'P')
		{
			if (compMove == 'S')
				return 'C';
			else
				return 'H';
		}
		else
		{
			if (compMove == 'R')
				return 'C';
			else
				return 'H';
		}
	}
	

	//================================================================================
    // Testing functions	TODO: [Potentially] move to separate class
    //================================================================================

	public void expandInput()
	{	
		Scanner line = null;
		try {
			line = new Scanner(dataFile);
		}
		catch (IOException e) {
			System.out.println("IOException has occured. Exiting.");
			System.exit(1);
		}

		testPoints = new ArrayList<Datapoint>(44100);

		// All terminal moves
		while (line.hasNextLine() && line.hasNext())
		{
			String player = line.next();
			String computer = line.next();
				
			int[] RPS = new int[3];
			RPS[0] = line.nextInt();
			RPS[1] = line.nextInt();
			RPS[2] = line.nextInt();
			
			for (int i = 0; i < RPS[0]; i++)
			{
				testPoints.add(new Datapoint(player, computer, 'R'));
			}
			
			for (int i = 0; i < RPS[1]; i++)
			{
				testPoints.add(new Datapoint(player, computer, 'P'));
			}
			
			for (int i = 0; i < RPS[2]; i++)
			{
				testPoints.add(new Datapoint(player, computer, 'S'));
			}
		}
	}

	public void testAll(double testPercentage)
	{
		generateTestData(testPercentage);
		
		testNaiveBayes();
		testFullBayes();
		testAdaptedNN();
	}

	public void testNaiveBayes()
	{
		int win = 0;
		int draw = 0;
		int loss = 0;
		for (Datapoint dp : testData)
		{
			char prediction = naiveBayes(dp.player, dp.computer);
			char myMove = chooseMove(prediction);
			
			char result = determineWinner(dp.result, myMove);
			if (result == 'C') {
				win++;
			}
			else if (result == 'D') {
				draw++;
			}
			else {
				loss++;
			}
		}
		System.out.println("Naive Bayes Test Results");
		System.out.println("Wins: " + win + ", Draws: " + draw + " , Losses: " + loss);
		System.out.println("Win percent (-Draws): " + (double)win/(win+loss));
		System.out.println("Win percent (+Draws): " + (double)win/(win+draw+loss) + "\n");
	}
	
	public void testFullBayes()
	{
		int win = 0;
		int draw = 0;
		int loss = 0;
		for (Datapoint dp : testData)
		{
			char prediction = fullBayes(dp.player, dp.computer);
			char myMove = chooseMove(prediction);
			
			char result = determineWinner(dp.result, myMove);
			if (result == 'C') {
				win++;
			}
			else if (result == 'D') {
				draw++;
			}
			else {
				loss++;
			}
		}
		System.out.println("Full Bayes Test Results");
		System.out.println("Wins: " + win + ", Draws: " + draw + " , Losses: " + loss);
		System.out.println("Win percent (-Draws): " + (double)win/(win+loss));
		System.out.println("Win percent (+Draws): " + (double)win/(win+draw+loss) + "\n");
	}
	
	public void testAdaptedNN()
	{
		int win = 0;
		int draw = 0;
		int loss = 0;
		for (Datapoint dp : testData)
		{
			char prediction = adaptedNN(dp.player, dp.computer);
			char compMove = chooseMove(prediction);
			
			char result = determineWinner(dp.result, compMove);
			if (result == 'C') {
				win++;
			}
			else if (result == 'D') {
				draw++;
			}
			else {
				loss++;
			}
		}
		System.out.println("Adapted-NN Results");
		System.out.println("Wins: " + win + ", Draws: " + draw + " , Losses: " + loss);
		System.out.println("Win percent (-Draws): " + (double)win/(win+loss));
		System.out.println("Win percent (+Draws): " + (double)win/(win+draw+loss) + "\n");
	}
	
	public void generateTestData(double testPercentage)
	{
		int testSize = (int)(testPoints.size() * testPercentage);
		testData = new ArrayList<Datapoint>(testSize);
		
		Collections.shuffle(testPoints);
		
		for (int i = 0; i < testSize; i++)
		{
			Datapoint dp = testPoints.get(i);
			testData.add(dp);
			
			String key = dp.player + dp.computer;
			int[] RPS = data.remove(key);
			if (dp.result == 'R')
			{
				RPS[0]--;
				numRocks--;
			}
			else if (dp.result == 'P')
			{
				RPS[1]--;
				numPapers--;
			}
			else
			{
				RPS[2]--;
				numScissors--;
			}
			data.put(key, RPS);
		}
		
		double total = numRocks + numPapers + numScissors;
		PY[0] = numRocks / total;
		PY[1] = numPapers / total;
		PY[2] = numScissors / total;
	}
	

	//================================================================================
    // Inner-classes
    //================================================================================

	public static class Datapoint
	{
		String player;
		String computer;
		char result;
		
		public Datapoint(String player, String computer, char result)
		{
			this.player = player;
			this.computer = computer;
			this.result = result;
		}
		
		public String toString()
		{
			return(player + " " + computer + " " + result);
		}
	}
}