package detection2.data.unit;

public enum DistanceUnit {

	CENTIMETER {
		@Override
		public double toCentimeter(double value) {
			return value;
		}

		@Override
		public double convert(double value, DistanceUnit target) {
			return toCentimeter(value);
		}
	};

	public abstract double toCentimeter(double value);

	public abstract double convert(double value, DistanceUnit target);

}