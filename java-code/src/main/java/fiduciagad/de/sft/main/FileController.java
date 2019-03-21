package fiduciagad.de.sft.main;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileController {

	private static String path = System.getProperty("user.dir").replace('\\', '/');

	public static void loadDataFromFile() {

		List<String> data = null;

		Path p = Paths.get(path + "confFile.txt");

		try {
			data = Files.readAllLines(p, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}

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

		ConfiguratorValues.setMillimeterPerPixel(Integer.parseInt(data.get(10)));

	}

}
