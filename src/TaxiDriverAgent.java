import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;


public class TaxiDriverAgent extends Agent {
	private int X = 9000;
	private int Y = 8500;
	
	private double taxi_X = (int)(Math.random() * X);
	private double taxi_Y = (int)(Math.random() * Y);
	private boolean isBusy = false;
	
	static double balance = 150000;
	
	// Put agent initializations here
	protected void setup() {
		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("taxi-drivers");
		sd.setName("JADE-taxi");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from passengers agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from passengers agents
		addBehaviour(new PurchaseOrdersServer());
		
		/*
		addBehaviour(new TickerBehaviour(this, 60000) { // second lab
			protected void onTick() {
				System.out.println("BALANCE OF MONEY " + balance);
				
			}
		});*/
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Printout a dismissal message
		System.out.println("Taxi-driver "+getAID().getName()+" terminating.");
	}
	
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			//MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP); 
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("taxi-order"),
					MessageTemplate.MatchPerformative(ACLMessage.CFP));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				
				String position = msg.getContent();
				
				double customer_X = Double.parseDouble(position.split(" ")[0]);
				double customer_Y = Double.parseDouble(position.split(" ")[1]);
				double d = Math.sqrt(Math.pow(customer_X - taxi_X, 2) + Math.pow(customer_Y - taxi_Y, 2));
				ACLMessage reply = msg.createReply();

				//Integer price = (Integer) catalogue.get(title);
				
				if (!isBusy) {
					// The requested book is available for sale. Reply with the price
					try {
						//Thread.sleep(5000);  
						Thread.sleep((long)(d*60000/60000));
				    } catch (InterruptedException e) {
				    	e.printStackTrace();
				    }
					
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(d));
					
					taxi_X = customer_X;
					taxi_Y = customer_Y;
					
				}
				else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
	
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// Message received. Process it
				String destination = msg.getContent();
				
				ACLMessage reply = msg.createReply();

				if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					reply.setPerformative(ACLMessage.INFORM);
					//System.out.println(getAID().getName()+" took "+msg.getSender().getName());
					
					double dest_X = Double.parseDouble(destination.split(" ")[0]);
					double dest_Y = Double.parseDouble(destination.split(" ")[1]);
					double d = Math.sqrt(Math.pow(dest_X - taxi_X, 2) + Math.pow(dest_Y - taxi_Y, 2));
					try {
						Thread.sleep((long)(d*60000/60000));
				    } catch (InterruptedException e) {
				    	e.printStackTrace();
				    }
					System.out.println(getAID().getName()+" send "+msg.getSender().getName()+" to the destination place");
					taxi_X = dest_X;
					taxi_Y = dest_Y;
					
					balance += d*2.5/1000;
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
}

