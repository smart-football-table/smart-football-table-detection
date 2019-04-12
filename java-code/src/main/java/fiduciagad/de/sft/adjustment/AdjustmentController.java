package fiduciagad.de.sft.adjustment;

import java.util.List;

import fiduciagad.de.sft.main.ConfiguratorValues;

public class AdjustmentController {

	public static void convertOutputIntoValues(List<String> theOutput) {

		String[] colorValues = theOutput.get(0).split(",");

		ConfiguratorValues.setColorHSVMinH(Integer.parseInt(colorValues[0]));
		ConfiguratorValues.setColorHSVMinS(Integer.parseInt(colorValues[1]));
		ConfiguratorValues.setColorHSVMinV(Integer.parseInt(colorValues[2]));
		ConfiguratorValues.setColorHSVMaxH(Integer.parseInt(colorValues[3]));
		ConfiguratorValues.setColorHSVMaxS(Integer.parseInt(colorValues[4]));
		ConfiguratorValues.setColorHSVMaxV(Integer.parseInt(colorValues[5]));

		String[] pointOne = theOutput.get(1).split(",");
		int xPointOneCamOne = Integer.parseInt(pointOne[0]);
		int yPointOneCamOne = Integer.parseInt(pointOne[1]);
		int xPointOneCamTwo = Integer.parseInt(pointOne[2]);
		int yPointOneCamTwo = Integer.parseInt(pointOne[3]);

		String[] pointTwo = theOutput.get(2).split(",");
		int xPointTwoCamOne = Integer.parseInt(pointTwo[0]);
		int yPointTwoCamOne = Integer.parseInt(pointTwo[1]);
		int xPointTwoCamTwo = Integer.parseInt(pointTwo[2]);
		int yPointTwoCamTwo = Integer.parseInt(pointTwo[3]);

		String[] pointThree = theOutput.get(3).split(",");
		int xPointThreeCamOne = Integer.parseInt(pointThree[0]);
		int yPointThreeCamOne = Integer.parseInt(pointThree[1]);
		int xPointThreeCamTwo = Integer.parseInt(pointThree[2]);
		int yPointThreeCamTwo = Integer.parseInt(pointThree[3]);

		String[] pointFour = theOutput.get(4).split(",");
		int xPointFourCamOne = Integer.parseInt(pointFour[0]);
		int yPointFourCamOne = Integer.parseInt(pointFour[1]);
		int xPointFourCamTwo = Integer.parseInt(pointFour[2]);
		int yPointFourCamTwo = Integer.parseInt(pointFour[3]);

		int xSizeGameField = Math.abs((xPointTwoCamOne - xPointOneCamOne) + (xPointThreeCamTwo - xPointTwoCamTwo));
		int ySizeGameField = Math.abs(yPointFourCamTwo - yPointThreeCamTwo);

		ConfiguratorValues.setGameFieldSize(xSizeGameField, ySizeGameField);

		// ConfiguratorValues.setxOffsetCameraOne(0 - xPointOneCamOne);
		// ConfiguratorValues.setyOffsetCameraOne(0 - yPointOneCamOne);
		//
		// ConfiguratorValues.setxOffsetCameraTwo((xSizeGameField / 2) -
		// xPointTwoCamTwo);
		// ConfiguratorValues.setyOffsetCameraTwo(0 - yPointTwoCamTwo);

		calculateMillimeterPerPixel(theOutput);

	}

	private static void calculateMillimeterPerPixel(List<String> theOutput) {
		int ballSizeInPixel = Integer.parseInt(theOutput.get(5));
		int ballSizeInMillimeter = 30;
		ConfiguratorValues.setMillimeterPerPixel(ballSizeInPixel / ballSizeInMillimeter);
	}
}
