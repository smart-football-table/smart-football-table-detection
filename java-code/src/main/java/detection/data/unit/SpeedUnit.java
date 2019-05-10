package detection.data.unit;

public enum SpeedUnit {

	MPS {
		@Override
		public double toMps(double value) {
			return value;
		}
	},
	KMH {
		@Override
		public double toMps(double value) {
			return value * 3.6;
		}
	};

	public abstract double toMps(double metersPerSecond);
}