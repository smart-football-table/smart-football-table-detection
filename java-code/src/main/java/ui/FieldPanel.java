package ui;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class FieldPanel extends JPanel {

	private int x = -10;
	private int y = -10;
	
	public FieldPanel() {
		init();
	}

	private void init() {
		// TODO Auto-generated method stub

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 2000, 2000);
		g.setColor(Color.BLUE);
		g.fillOval(x, y, 15, 15);

	}

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;
		
		repaint();
	}
	
	
	

}
