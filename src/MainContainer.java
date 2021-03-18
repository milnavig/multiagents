import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;


import jade.wrapper.AgentController;

public class MainContainer {
	
	ContainerController cc;
	
	private static final int numberOfAgents = 14;
	private int id = 1;
	void initAgents() {
		// Retrieve the singleton instance of the JADE Runtime
		Runtime rt = Runtime.instance();
		//Create a container to host the Default Agent
		Profile p = new ProfileImpl();
		p.setParameter(Profile.MAIN_HOST, "localhost");
		p.setParameter(Profile.MAIN_PORT, "10098");
		p.setParameter(Profile.GUI, "true");
		cc = rt.createMainContainer(p);
		try {
			for (int i=1; i <= MainContainer.numberOfAgents; i++) {
				AgentController agent1 = cc.createNewAgent("Passenger" + Integer.toString(i), "PassengerAgent", null);
				agent1.start();
				if (i == 1 || i == 2) {
					AgentController agent2 = cc.createNewAgent("TaxiDriver" + Integer.toString(i), "TaxiDriverAgent", null);
					TaxiDriverAgent.balance -= 15000; // price for one car
					agent2.start();
				}
				
				id++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void addTaxiDriver() {
		if (PassengerAgent.delay > 2500) {
			System.out.println("NEW TAXI DRIVER ADDED BECAUSE " + PassengerAgent.delay + " > 1 hour");
			AgentController agent;
			try {
				agent = cc.createNewAgent("TaxiDriver" + id++, "TaxiDriverAgent", null);
				agent.start();
				TaxiDriverAgent.balance -= 15000; // price for one car
				PassengerAgent.delay = 0;
				PassengerAgent.full_wait = 0;
				PassengerAgent.num_orders = 0;
			} catch (StaleProxyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		System.out.println("The balance of the taxi company = " + TaxiDriverAgent.balance);
		
		try {
			Thread.sleep(60000);
	    } catch (InterruptedException e) {
	    	e.printStackTrace();
	    }
		addTaxiDriver();
	}
}
