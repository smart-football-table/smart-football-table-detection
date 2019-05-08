package detection2.detector;

import detection2.data.position.AbsolutePosition;

public interface Detector {
	void detect(AbsolutePosition pos);

	Detector newInstance();
}