package ui;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;

import ballPosition_module.MainPanel;

public class PanelHolder extends JFrame {

	MainPanel mainPanel;

	public PanelHolder() throws IOException {
		init();
	}

	private void init() throws IOException {

		mainPanel = new MainPanel();
		
		this.setLayout(new BorderLayout());

		this.add(mainPanel, BorderLayout.CENTER);

		this.setSize(1200, 900);
		this.setVisible(true);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

	}


}
