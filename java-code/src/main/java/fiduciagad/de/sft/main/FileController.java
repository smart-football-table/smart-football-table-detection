package fiduciagad.de.sft.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileController {

	private static String path = System.getProperty("user.dir").replace('\\', '/');

	public static void loadDataFromFile() {

		List<String> data = null;

		Path p = Paths.get(path + "/confFile.txt");

		try {
			data = Files.readAllLines(p, Charset.defaultCharset());

			String[] hsvLower = data.get(1).split(",");
			String[] hsvUpper = data.get(3).split(",");

			ConfiguratorValues.setColorHSVMinH(Integer.parseInt(hsvLower[0]));
			ConfiguratorValues.setColorHSVMinS(Integer.parseInt(hsvLower[1]));
			ConfiguratorValues.setColorHSVMinV(Integer.parseInt(hsvLower[2]));
			ConfiguratorValues.setColorHSVMaxH(Integer.parseInt(hsvUpper[0]));
			ConfiguratorValues.setColorHSVMaxS(Integer.parseInt(hsvUpper[1]));
			ConfiguratorValues.setColorHSVMaxV(Integer.parseInt(hsvUpper[2]));

			String[] gameFieldSize = data.get(5).split(",");

			ConfiguratorValues.setGameFieldSize(Integer.parseInt(gameFieldSize[0]), Integer.parseInt(gameFieldSize[1]));

			String[] offsetCameraOne = data.get(7).split(",");

			ConfiguratorValues.setxOffsetCameraOne(Integer.parseInt(offsetCameraOne[0]));
			ConfiguratorValues.setyOffsetCameraOne(Integer.parseInt(offsetCameraOne[1]));

			String[] offsetCameraTwo = data.get(9).split(",");

			ConfiguratorValues.setxOffsetCameraTwo(Integer.parseInt(offsetCameraTwo[0]));
			ConfiguratorValues.setyOffsetCameraTwo(Integer.parseInt(offsetCameraTwo[1]));

			ConfiguratorValues.setMillimeterPerPixel(Integer.parseInt(data.get(11)));

		} catch (Exception e) {
			ConfiguratorValues.setDefaultColorRangeYellow();
			ConfiguratorValues.setMillimeterPerPixel(1);
		}

	}

	public static void writeDataIntoFile() throws IOException {
		File fileOut = new File("confFile.txt");
		FileOutputStream fos = new FileOutputStream(fileOut);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		bw.write("//HSV Lower");
		bw.newLine();
		bw.write("" + ConfiguratorValues.getColorHSVMinH() + "," + ConfiguratorValues.getColorHSVMinS() + ","
				+ ConfiguratorValues.getColorHSVMinV());
		bw.newLine();
		bw.write("//HSV Upper");
		bw.newLine();
		bw.write("" + ConfiguratorValues.getColorHSVMaxH() + "," + ConfiguratorValues.getColorHSVMaxS() + ","
				+ ConfiguratorValues.getColorHSVMaxV());
		bw.newLine();
		bw.write("//GamefieldSize");
		bw.newLine();
		bw.write("" + ConfiguratorValues.getXMaxOfGameField() + "," + ConfiguratorValues.getYMaxOfGameField());
		bw.newLine();
		bw.write("//OffsetCameraOne");
		bw.newLine();
		bw.write("" + ConfiguratorValues.getxOffsetCameraOne() + "," + ConfiguratorValues.getyOffsetCameraOne());
		bw.newLine();
		bw.write("//OffsetCameraTwo");
		bw.newLine();
		bw.write("" + ConfiguratorValues.getxOffsetCameraTwo() + "," + ConfiguratorValues.getyOffsetCameraTwo());
		bw.newLine();
		bw.write("//MillimeterPerPixel");
		bw.newLine();
		bw.write("" + ConfiguratorValues.getMillimeterPerPixel());
		bw.newLine();

		bw.close();
	}

}
