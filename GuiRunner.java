import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GuiRunner extends JPanel implements ActionListener {

	private JButton rockButton,
					paperButton,
					scissorsButton,
					resetButton;
	
	private static String pHist, cHist;
	private static double wins, ties, losses, games;
	private static PredictionEngine engine;
	
	private static ImageIcon rockIcon,
			paperIcon,
			scissorsIcon,
			cRockIcon,
			cPaperIcon,
			cScissorsIcon,
			defaultIcon;
	
	private static JLabel pMove,
						  cMove;
	
	private static JLabel stats;
	
	private static GridBagConstraints c;
	
	public static void main(String[] args) throws IOException {
		rockIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/rock.png")));
		paperIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/paper.png")));
		scissorsIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/scissors.png")));
		cRockIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/Crock.png")));
		cPaperIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/Cpaper.png")));
		cScissorsIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/Cscissors.png")));
		defaultIcon = new ImageIcon(ImageIO.read(GuiRunner.class.getResourceAsStream("images/default.png")));
		
		JFrame f = new RPSFrame();
	    f.setVisible(true);
	    
	    wins = 0;
	    ties = 0;
	    losses = 0;
	    games = 0;
	    pHist = "0";
	    cHist = "0";
	    engine = new PredictionEngine();
	}
	
	public GuiRunner()
	{
		setLayout(new GridBagLayout());
	    c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 80;
		c.ipady = 10;
		
		rockButton = new JButton("Rock");
		paperButton = new JButton("Paper");
		scissorsButton = new JButton("Scissors");
		pMove = new JLabel(defaultIcon);
		cMove = new JLabel(defaultIcon);
		stats = new JLabel("Win/Loss: -% (0-0-0/0)");
		resetButton = new JButton("Reset");
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		add(rockButton,c);
		c.gridy = 1;
		add(paperButton,c);
		c.gridy = 2;
		add(scissorsButton,c);
		
		rockButton.addActionListener(this);
		paperButton.addActionListener(this);
		scissorsButton.addActionListener(this);
		resetButton.addActionListener(this);
		
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 3;
		add(pMove, c);
		c.gridy = 3;
		add(cMove, c);
		
		c.gridx = 0;
		c.gridy = 3;
		c.gridheight = 1;
		add(stats, c);
		
		c.gridy = 4;
		c.gridheight = 2;
		add(resetButton, c);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	private static class RPSFrame extends JFrame {

		public RPSFrame() {
			setLayout(new GridBagLayout());
			setTitle("Intelli-Rock-Paper-Scissors");
			setSize(800, 800);
			setLocation(10, 10);

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				} // windowClosing
			}); // addWindowLister

			Container contentPane = getContentPane();
			contentPane.add(new GuiRunner());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String com = e.getActionCommand();
		char playerMove = 'N';
		if (com.equals("Rock"))
		{
			playerMove = 'R';
			pMove.setIcon(rockIcon);
		}
		else if (com.equals("Paper"))
		{
			playerMove = 'P';	
			pMove.setIcon(paperIcon);
		}
		else if (com.equals("Scissors"))
		{
			playerMove = 'S';
			pMove.setIcon(scissorsIcon);
		}
		else if (com.equals("Reset"))
		{
			pHist = "0";
			cHist = "0";
			games = 0;
			ties = 0;
			losses = 0;
			wins = 0;
			stats.setText("Win/Loss: -% (0-0-0/0)");
			pMove.setIcon(defaultIcon);
			cMove.setIcon(defaultIcon);
			return;
		}
		
		char compMove = engine.determineOptimalMove(pHist, cHist);
		switch (compMove)
		{
		case 'R':
			cMove.setIcon(cRockIcon);
			break;
		case 'P':
			cMove.setIcon(cPaperIcon);
			break;
		case 'S':
			cMove.setIcon(cScissorsIcon);
			break;
		}
		
		if (pHist.equals("0"))
		{
			pHist = playerMove + "";
		}
		else
		{
			pHist += playerMove;
		}
		
		if (cHist.equals("0"))
		{
			cHist = compMove + "";
		}
		else
		{
			cHist += compMove;
		}	
		
		games++;
		char outcome = engine.determineWinner(playerMove, compMove);
		if (outcome == 'H')
		{
			wins++;
		}
		else if (outcome == 'C')
		{
			losses++;
		}
		else
		{
			ties++;
		}
		double percent = wins / (wins + losses);
		String s = String.format("Win/Loss: %2.2f%% (%d-%d-%d/%d)", percent, (int) wins, (int) ties, (int) losses, (int) games);
		stats.setText(s);
	}
}
