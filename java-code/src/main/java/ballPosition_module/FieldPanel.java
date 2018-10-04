package ballPosition_module;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class FieldPanel extends JPanel {

	private int x = -1000;
	private int y = -1000;

	BallPositionMainPanel ballPositionMainPanel;

	public FieldPanel(BallPositionMainPanel panelHolder) {
		this.ballPositionMainPanel = panelHolder;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.BLACK);
		g.drawRect(100, 20, 800, 600);

		if (x == -1 && y == -1) {
			g.drawString("Kein Objekt erkannt", 410, 230);
		} else {
			g.fillOval(x + 100, y + 20, 15, 15);
		}

	}

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;

		repaint();
	}

	public BallPositionMainPanel getBallPositionMainPanel() {
		return ballPositionMainPanel;
	}

}
