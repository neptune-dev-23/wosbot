package cl.camodev.wosbot.ot;

public class DTOPoint {
	private double x;
	private double y;

	public DTOPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	@Override
	public String toString() {
		return "DTOPoint [x=" + x + ", y=" + y + "]";
	}
}
