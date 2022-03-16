package finalyearproject;

import org.chocosolver.solver.Solution;

public class Main {
	public static void main(String[] args) {
		Employee e1 = new Employee("John", 2, 4, 1);
		Employee e2 = new Employee("Mary", 2, 4, 1);
		Employee e3 = new Employee("Paul", 2, 4, 1);
		Employee e4 = new Employee("Joanne", 1, 4, 1);
		Employee e5 = new Employee("Joanne", 0, 40, 1);
		Employee e6 = new Employee("Joanne", 0, 40, 1);
		Employee e7 = new Employee("Joanne", 0, 40, 1);
		
		Controller c = new Controller();
		c.addEmployee(e1);
		c.addEmployee(e2);
		c.addEmployee(e3);
		c.addEmployee(e4);
		/*
		c.addEmployee(e4);
		c.addEmployee(e5);
		c.addEmployee(e6);
		c.addEmployee(e7);
		*/
		
		c.initialiseScheduler();
		
		e1.addPreference(c.week, 0,  1);
		e1.addPreference(c.week, 1,  2);
		e1.addPreference(c.week, 2,  3);
		
		
		e2.addPreference(c.week, 0,  1);
		e2.addPreference(c.week, 1,  2);
		e2.addPreference(c.week, 2,  3);
		
		
		e3.addPreference(c.week, 3,  1);
		e3.addPreference(c.week, 4,  2);
		e3.addPreference(c.week, 5,  3);
		
		e4.addPreference(c.week, 0,  1);
		e4.addPreference(c.week, 4,  2);
		e4.addPreference(c.week, 6,  3);
		
		
		Solution s = c.runWeek();
		c.scheduler.printSolution(s);
		c.completeWeek();
		c.newWeek();
		
		s = c.runWeek();
		c.scheduler.printSolution(s);
		c.completeWeek();
		c.newWeek();
		
		s = c.runWeek();
		c.scheduler.printSolution(s);
		c.completeWeek();
		c.newWeek();
		
		s = c.runWeek();
		c.scheduler.printSolution(s);
		c.completeWeek();
		c.newWeek();
		
		e1.printStats();
		e2.printStats();
		e3.printStats();
		e4.printStats();
	}
}

/* --Notes about this model--
 * Credit system works and over time people will receive the same score and the same number of preferences
 * However during an arbritary week there is no balance
 * If 2 people ask for exactly the same preferences then the system will give all 3 to the same person with the highest bank
 * There is fairness in the long term, but on a short basis this is not good and employees may struggle to understand why someone is getting 3 preferences and they are getting none
 * 
 * If someone asks for the same thing every single week, and is the only one to ask, they will always get it, but there bank will eventually hit 0 if everyone else is getting less than them.
 * 
 */

