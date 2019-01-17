package smartfootballtable;

import java.util.ArrayList;
import java.util.List;

public class DistributionHeatMapController {

	private List<Integer> list = new ArrayList<Integer>();
	
	private List<Integer> positions = new ArrayList<Integer>();
	
	public void configureFieldGranularity(int fieldCount) {
		for (int i = 0; i < fieldCount; i++) {
			list.add(0);
		}
		
		
	}

	public List<Integer> getFields() {
		return list;
	}

	public void addPositionToField(int fieldNumber) {
		positions.add(fieldNumber);
		
	}

	public double getDistributionInPercentOfField(int fieldNumber) {
		
		double sum = 0;
		
		for (Integer fieldPosition : positions) {
			
			if(fieldNumber == fieldPosition) {
				sum++;
			}
			
		}
		
		return (sum/positions.size()) * 100;
	}

}
