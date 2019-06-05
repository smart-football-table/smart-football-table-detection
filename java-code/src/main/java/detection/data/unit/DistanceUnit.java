package detection.data.unit;

public enum DistanceUnit {

	CENTIMETER {
		@Override
		public String symbol() {
			return "cm";
		}

		@Override
		public double toCentimeter(double value) {
			return value;
		}

		@Override
		public double toInch(double value) {
			return value / 2.54;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toCentimeter(value);
		}
	},
	INCH {
		@Override
		public String symbol() {
			return "inch";
		}

		@Override
		public double toCentimeter(double value) {
			return value * 2.54;
		}

		@Override
		public double toInch(double value) {
			return value;
		}

		@Override
		public double convert(double value, DistanceUnit source) {
			return source.toInch(value);
		}
	};

	public abstract String symbol();

	public abstract double toCentimeter(double value);

	public abstract double toInch(double value);

	public abstract double convert(double value, DistanceUnit source);

}