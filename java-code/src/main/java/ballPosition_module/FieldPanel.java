package ballPosition_module;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class FieldPanel extends JPanel {

	private int x = -10;
	private int y = -10;
	
	MainPanel mainPanel;
	
	public FieldPanel(MainPanel panelHolder) {
		this.mainPanel = panelHolder;
		init();
		
	}

	private void init() {
		// TODO Auto-generated method stub

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		
		g.setColor(Color.BLACK);
		g.drawRect(100, 100, 800, 600);
		
		g.setColor(Color.BLUE);
		g.fillOval(x+100, y+100, 15, 15);

	}

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;
		
		repaint();
	}
	
	
	

}
