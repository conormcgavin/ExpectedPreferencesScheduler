package finalyearproject;

import java.util.ArrayList;
import java.util.List;

import org.chocosolver.util.tools.ArrayUtils;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.variables.IntVar;

public class Scheduler {
	Model model;
	
	int[] workers_needed_per_day = {1, 1, 1, 1, 1, 1, 1};
	int num_workers;
	IntVar[][] timetable;
	ArrayList<IntVar[]> preferences;
	IntVar[] total_preferences_per_person;
	IntVar total_overall_preferences;
	ArrayList<Employee> employees;
	Solution finalSolution;
	
	int[] banks;
	float[] bank_percentages;
	IntVar[] bank_percentagesInt;
	
	IntVar[] exScores;
	IntVar[] scoreDiffs;
	IntVar total_score_difference;
	
	public Scheduler(ArrayList<Employee> employees) {
		Model model = new Model("FYP");
		this.model = model;
		this.num_workers = employees.size();
		this.employees = employees;
		
		int sum = 0;
		this.banks = new int[this.num_workers];
		this.bank_percentages = new float[this.num_workers];
		for (int i = 0; i < this.num_workers; i++) {
			this.banks[i] = employees.get(i).bank;
			sum += employees.get(i).bank;
		}
		for (int i = 0; i < this.num_workers; i++) {
			this.bank_percentages[i] = (float) (employees.get(i).bank*100 / sum);
		}
		
		this.bank_percentagesInt = model.intVarArray("BankPercentages", num_workers, 0, 100);
		for (int i=0; i<num_workers; i++) {
			bank_percentagesInt[i] = model.intVar((int)bank_percentages[i]);
		}
		
		
		this.timetable = model.intVarMatrix("Timetable", num_workers, Constants.days_per_week, 0, 1);
		
		this.preferences = new ArrayList<IntVar[]>();
		for (int i=0; i<num_workers; i++) {
			preferences.add(model.intVarArray("PreferenceAssignments", Constants.PREFERENCES_PER_PERSON, 0, 1));
		}
		
		this.total_preferences_per_person = model.intVarArray("PrefsPerPerson", this.num_workers, 0, 100000);
		this.total_overall_preferences = model.intVar("TotalOverallScore", 0, 100000);
		
		
		this.exScores = model.intVarArray("exScores", this.num_workers, 0, 100000);
		this.scoreDiffs = model.intVarArray("scoreDiffs", this.num_workers, 0, 100000);
		this.total_score_difference = model.intVar("TotalScoreDiff", 0, 100000);
		
		this.addHardConstraints();
	}

	private void addHardConstraints() {
		// the total of the entire matrix should add up to the sum of workers_hours_per_week
		int sum_workers_needed = 0;
	    for (int value : workers_needed_per_day) {
	        sum_workers_needed += value;
	    }
		model.sum(ArrayUtils.flatten(timetable), "=", sum_workers_needed).post();
		
		// every column should have a sum of exactly the sum of workers needed in that day
		for (int i=0; i<Constants.days_per_week; i++) {
			model.sum(ArrayUtils.getColumn(timetable, i), "=", workers_needed_per_day[i]).post();
		}
		
		// workers must work between their working day limits every week
		for (int i=0; i<num_workers; i++) {
			model.sum(timetable[i], "<=", employees.get(i).max_days_per_week).post();
			model.sum(timetable[i], ">=", employees.get(i).min_days_per_week).post();
		}
	}
	
	public void addPreference(int person, Preference p) {
		if (p.modelAsHard == false) {
			model.ifOnlyIf(this.model.arithm(timetable[person][p.day], "=", 0), this.model.arithm(preferences.get(person)[p.order-1], "=", 1));
		} else {
			model.arithm(timetable[person][p.day], "=", 0).post();
			model.arithm(preferences.get(person)[p.order-1], "=", 1).post();
		}
	}
	
	public void optimise() { 
		for (int i = 0; i < num_workers; i++) {
			model.sum(preferences.get(i), "=", total_preferences_per_person[i]).post();
		}
		model.sum(total_preferences_per_person, "=", total_overall_preferences).post();
		
		
		for (int i=0; i<num_workers; i++) {
			model.arithm(total_overall_preferences, "*", bank_percentagesInt[i], "=", exScores[i]).post();
		}
		
		IntVar[] tmp = model.intVarArray(this.num_workers, 0,10000000);
		for (int i = 0; i < num_workers; i++) {
			tmp[i] = model.intScaleView(total_preferences_per_person[i], 100);
		}
		
		for (int i=0; i<num_workers; i++) {
			model.distance(tmp[i], exScores[i], "=", scoreDiffs[i]).post();
		}
		model.sum(scoreDiffs, "=", this.total_score_difference).post();
		
	}
	
	public Solution solve() {
		System.out.println("Solving...");
		Solver solver = model.getSolver();
		
		/*
		List<Solution> sols = solver.findAllOptimalSolutions(total_overall_preferences, true);
		if (sols.size() == 0) {
			System.out.println("No solutions.");
			return null;
		}
		
		Solution whichSol = null;
		int currentMin = 1000000000;
		System.out.println("Printing all solutions that maximise total preferences...");
		for (Solution solution : sols) {
			
			int max = -1;
			
			for (int i = 0; i < num_workers; i++) {
				if (solution.getIntVal(scoreDiffs[i]) > max) {
					max = solution.getIntVal(scoreDiffs[i]);
				}
			}
			
			if (max < currentMin) {
				currentMin = max;
				whichSol = solution;
			}
		}
		
		this.finalSolution = whichSol;
		System.out.println("Minimum Maximum ExScore Distance Found: " + currentMin);
		return whichSol;
		*/
		
		// create an object that will store the best solutions and remove dominated ones
		ParetoMaximizer po = new ParetoMaximizer(new IntVar[]{total_overall_preferences,model.intMinusView(total_score_difference)});
		solver.plugMonitor(po);

		// optimization
		while(solver.solve());

		// retrieve the pareto front
		List<Solution> paretoFront = po.getParetoFront();
		System.out.println("The pareto front has "+paretoFront.size()+" solutions : ");
		for(Solution s:paretoFront){
		        System.out.println("Overall preferences = "+s.getIntVal(total_overall_preferences)+" and total score diff = "+s.getIntVal(total_score_difference));
		}
		if (paretoFront.size() > 0) {
			this.finalSolution = paretoFront.get(0);
			return paretoFront.get(0);
		}
		return null;
	}
	
	public void printSolution(Solution s) {
		System.out.println("Printing Optimal Solution...\n");
		System.out.println("-----------------------------------------------------------------------------");
		String row_sol;
		for (int i = 0; i < num_workers; i++) {
			int count = 0;
			row_sol = "Worker " + i + ":\t";
			for (int j = 0; j < Constants.days_per_week; j++) {
				row_sol += s.getIntVal(timetable[i][j]) + "\t";
				count += 1;
				if (count == Constants.hours_per_day) {
					row_sol += " | \t";
					count = 0;
				}
			}
			System.out.println(row_sol);
			
		}	
		System.out.println();
		System.out.print("Expected Scores: ");
		for (int j = 0; j < num_workers; j++) {
			System.out.print((float)(s.getIntVal(exScores[j])) / 100.0 + "\t");
		}
		System.out.println("\nActual Scores: ");
		for (int j = 0; j < num_workers; j++) {
			System.out.print(s.getIntVal(total_preferences_per_person[j]) + "\t");
		}
		
		System.out.println();
		System.out.println("Total Preferences: " + s.getIntVal(total_overall_preferences));
		System.out.println("Total Overall Distance: " + s.getIntVal(total_score_difference));
		System.out.println("-----------------------------------------------------------------------------\n");
	}
	
	public void printInformation() {
		System.out.println("\n********************************************************************************");
		System.out.println("Printing information from this weekly run...");
		System.out.println("-----------------------------------");
		System.out.println("Printing preference assignments...");
		for (int i = 0; i < num_workers; i++) {
			System.out.println("Worker " + i + ": ");
			for (int j = 0; j < Constants.PREFERENCES_PER_PERSON; j++) {
				System.out.print(preferences.get(i)[j].getValue() + "\t");
			}
			System.out.println();
		}
		System.out.println("-----------------------------------");
		System.out.println("Printing total overall preferences...");
		System.out.println(total_overall_preferences.getValue());
		System.out.println("-----------------------------------");
		System.out.println("Printing average score...");
		System.out.println(total_overall_preferences.getValue() / num_workers);
		System.out.println("-----------------------------------");
		System.out.println("********************************************************************************\n");
	}

}



