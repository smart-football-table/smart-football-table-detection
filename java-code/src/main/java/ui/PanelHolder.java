package ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PanelHolder extends JFrame{

	JPanel contentPanel;
	JPanel sliderPanel;
	
	JSlider timepoint;
	
	
	public PanelHolder() {
		init();
	}

	private void init() {
		
		contentPanel = getContentPanel();
		sliderPanel = getSliderPanel();
		
		
		timepoint = getTimepoint();
		
		sliderPanel.add(timepoint, BorderLayout.CENTER);
		
		this.setLayout(new BorderLayout());
		
		this.add(contentPanel, BorderLayout.NORTH);
		this.add(sliderPanel, BorderLayout.SOUTH);
		
		this.setSize(800, 600);
		this.setVisible(true);
		
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
	}
	
	

	public JSlider getTimepoint() {
		
		if(timepoint == null) {
			timepoint = new JSlider(JSlider.HORIZONTAL,
	                0, 100, 50);

			timepoint.addChangeListener(new ChangeListener() {
				
				public void stateChanged(ChangeEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			
			timepoint.setMajorTickSpacing(10);
			timepoint.setMinorTickSpacing(1);
			timepoint.setPaintTicks(true);
			timepoint.setPaintLabels(true);
			
		}
		
		
		
		return timepoint;
		
		
		
	}

	public void setTimepoint(JSlider timepoint) {
		this.timepoint = timepoint;
	}

	public JPanel getSliderPanel() {
		if(sliderPanel == null) {
			sliderPanel = new JPanel();
			sliderPanel.setLayout(new BorderLayout());
		}
		
		return sliderPanel;
	}

	public void setSliderPanel(JPanel sliderPanel) {
		this.sliderPanel = sliderPanel;
	}

	public void setContentPanel(JPanel contentPanel) {
		this.contentPanel = contentPanel;
	}

	private JPanel getContentPanel() {
		
		if(contentPanel == null) {
			contentPanel = new JPanel();
		}
		
		return contentPanel;
	
	}
	
	
	
	
}


