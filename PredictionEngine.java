import java.io.*;
import java.util.*;

public class PredictionEngine
{
	HashMap<String, int[]> data;
	ArrayList<Datapoint> testPoints;
	
	ArrayList<Datapoint> testData;
    
	File dataFile;

	double[] PY;
	int numRocks, numPapers, numScissors;
	
	
	public PredictionEngine(File fileName)
	{
		dataFile = fileName;
		
		try { train(); }
		catch(IOException e)
		{ System.out.println("IOException has occured. Exiting."); }
	}
	
	public PredictionEngine(String fileName)
	{
		this(new File(fileName));
	}
	
	public PredictionEngine()
	{
		this(new File("openings.txt"));
	}
	
	public void train() throws IOException
	{
		input(); // builds 'data'
		expandInput(); // builds 'testPoints'
	}
	
	int[] globalPlayerCounts, globalComputerCounts;
	public char naiveBayes(String player, String computer)
	{
		String key = player + computer;
		int[] result = data.get(key);
	
		if (player.length() > 5)
		{
			player = player.substring(player.length()-5);
			computer = computer.substring(computer.length()-5);
		}
		int lengthClass = player.length();
		
		// Generate every valid opponent move
		globalPlayerCounts = new int[3];
		generateComputerCombinations(player, "", player.length());
		
		// Generate every valid player move
		globalComputerCounts = new int[3];
		generatePlayerCombinations("", computer, computer.length());
		
		// P(Y|X)=P(X1|Y)*P(X2|Y)*P(Y)
		double pRock = ((double)globalPlayerCounts[0]/numRocks) * ((double)globalComputerCounts[0]/numRocks) * PY[0];
		double pPaper = ((double)globalPlayerCounts[1]/numPapers) * ((double)globalComputerCounts[1]/numPapers) * PY[1];
		double pScissors = ((double)globalPlayerCounts[2]/numScissors) * ((double)globalComputerCounts[2]/numScissors) * PY[2];
		
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
	
	public void generateComputerCombinations(String player, String partialHistory, int charsLeft)
	{
		if (charsLeft == 0)
		{
			String key = player + partialHistory;
			int[] result = data.get(key);
			if (result != null)
			{
				globalPlayerCounts[0] += result[0];
				globalPlayerCounts[1] += result[1];
				globalPlayerCounts[2] += result[2];
			}
		}
		else
		{
			generateComputerCombinations(player, partialHistory.concat("r"), charsLeft-1);
			generateComputerCombinations(player, partialHistory.concat("p"), charsLeft-1);
			generateComputerCombinations(player, partialHistory.concat("s"), charsLeft-1);
		}
	}
	
	public void generatePlayerCombinations(String partialHistory, String computer, int charsLeft)
	{
		if (charsLeft == 0)
		{
			String key = partialHistory + computer;
			int[] result = data.get(key);
			if (result != null)
			{
				globalComputerCounts[0] += result[0];
				globalComputerCounts[1] += result[1];
				globalComputerCounts[2] += result[2];
			}
		}
		else
		{
			generatePlayerCombinations(partialHistory.concat("r"), computer, charsLeft-1);
			generatePlayerCombinations(partialHistory.concat("p"), computer, charsLeft-1);
			generatePlayerCombinations(partialHistory.concat("s"), computer, charsLeft-1);
		}
	}
	
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
			if (result == 'C')
				win++;
			else if (result == 'D')
				draw++;
			else
				loss++;
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
			if (result == 'C')
				win++;
			else if (result == 'D')
				draw++;
			else
				loss++;
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
			if (result == 'C')
				win++;
			else if (result == 'D')
				draw++;
			else
				loss++;
		}
		System.out.println("Adapted-NN Results");
		System.out.println("Wins: " + win + ", Draws: " + draw + " , Losses: " + loss);
		System.out.println("Win percent (-Draws): " + (double)win/(win+loss));
		System.out.println("Win percent (+Draws): " + (double)win/(win+draw+loss) + "\n");
	}
	
	public void input() throws IOException
	{
		Scanner line = new Scanner(dataFile);
		
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
	
	public char chooseMove(char prediction)
	{
		if (prediction == 'R')
			return 'P';
		else if (prediction == 'P')
			return 'S';
		else
			return 'R';
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
	
	public void expandInput() throws IOException
	{
		testPoints = new ArrayList<Datapoint>(44100);
		
		Scanner line = new Scanner(dataFile);

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