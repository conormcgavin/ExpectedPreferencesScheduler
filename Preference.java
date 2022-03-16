package finalyearproject;

public class Preference {
	int week;
	boolean granted;
	int day;
	int order;
	boolean modelAsHard;
	
		
	public Preference(int week, int day, int order, boolean modelAsHard) {
		this.week = week;
		this.day = day;
		this.granted = false;
		this.order = order;
		this.modelAsHard = modelAsHard;
	}
	
	public static Preference copy(Preference r) {
		Preference x = new Preference(r.week, r.day, r.order, r.modelAsHard);
		x.granted = r.granted;
		return x;
	}

}