package detection.detector;

import detection.data.position.AbsolutePosition;

public interface Detector {
	void detect(AbsolutePosition pos);

	Detector newInstance();
}