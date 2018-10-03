package ui;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;

import ballPosition_module.BallPositionMainPanel;

public class Window extends JFrame{

	PanelHolder panelHolder;

	public Window() throws IOException {
		init();
	}

	private void init() throws IOException {

		panelHolder = new PanelHolder();
		
		this.setLayout(new BorderLayout());

		this.add(panelHolder, BorderLayout.CENTER);

		this.setSize(1200, 900);
		this.setVisible(true);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

	}
	
}
