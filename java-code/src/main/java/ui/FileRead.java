package ui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileRead {

	public List<LocationAtTimepoint> readFile() throws IOException {

		FileReader fr = new FileReader("test.txt");
		BufferedReader br = new BufferedReader(fr);

		String line = "";
		List<LocationAtTimepoint> locationsOfBall = new ArrayList<LocationAtTimepoint>();

	    while( (line = br.readLine()) != null )
	    {
	    	
	    	String[] substring = line.split("|");
	    	
	    	LocationAtTimepoint location = new LocationAtTimepoint();
	    	setValuesFromLineInLocation(substring, location);

	    	locationsOfBall.add(location);
	    	
	    }

		br.close();
		
		return locationsOfBall;

	}

	private void setValuesFromLineInLocation(String[] substring, LocationAtTimepoint location) {
		location.setxCoordinate(Integer.parseInt(substring[1]));
		location.setyCoordinate(Integer.parseInt(substring[1]));
		location.setTimepoint(Long.parseLong(substring[1]));
	}
}
