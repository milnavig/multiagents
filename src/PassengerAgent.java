import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.StaleProxyException;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

public class PassengerAgent extends Agent {
	static double delay;
	static double full_wait = 0;
	static double num_orders = 0;
	
	private int X = 9000;
	private int Y = 8500;
	
	private int user_X = (int)(Math.random() * X);
	private int user_Y = (int)(Math.random() * Y);
	
	private double wallet = 100;
	
	// The list of known taxi agents
	private AID[] taxiAgents;
	// The list of known taxi users
	private AID[] customers;
	
	private boolean isBusy = false;
	
	//private ACLMessage cfp_for_d;
	
	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! Passenger-agent "+getAID().getName()+" is ready.");
		
		// Register the customers service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("customers");
		sd.setName("JADE-customer");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		/*
		addBehaviour(new TickerBehaviour(this, 30000) { 
			protected void onTick() {
				//System.out.println(getAID().getName() + " ordering a taxi");
				// Update the list of taxi agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("taxi-drivers");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					//System.out.println("Found the following taxi drivers:");
					taxiAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						taxiAgents[i] = result[i].getName();
						//System.out.println(taxiAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				
				// Update the list of customers
				DFAgentDescription template_c = new DFAgentDescription();
				ServiceDescription sd_c = new ServiceDescription();
				sd_c.setType("customers");
				template_c.addServices(sd_c);
				try {
					DFAgentDescription[] result_c = DFService.search(myAgent, template_c);
					//System.out.println("Found the following taxi drivers:");
					customers = new AID[result_c.length];
					for (int i = 0; i < result_c.length; ++i) {
						customers[i] = result_c[i].getName();
						//System.out.println(taxiAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}

				// Perform the request
				myAgent.addBehaviour(new RequestPerformer());
			}
		});
		*/
		
		addBehaviour(new CyclicBehaviour() { 
			public void action() {
				try {
					Thread.sleep((long)(Math.random() * 30000));
			    } catch (InterruptedException e) {
			    	e.printStackTrace();
			    }
				
				//System.out.println(getAID().getName() + " ordering a taxi");
				// Update the list of taxi agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("taxi-drivers");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					//System.out.println("Found the following taxi drivers:");
					taxiAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						taxiAgents[i] = result[i].getName();
						//System.out.println(taxiAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				
				// Update the list of customers
				DFAgentDescription template_c = new DFAgentDescription();
				ServiceDescription sd_c = new ServiceDescription();
				sd_c.setType("customers");
				template_c.addServices(sd_c);
				try {
					DFAgentDescription[] result_c = DFService.search(myAgent, template_c);
					//System.out.println("Found the following taxi drivers:");
					customers = new AID[result_c.length];
					for (int i = 0; i < result_c.length; ++i) {
						customers[i] = result_c[i].getName();
						//System.out.println(taxiAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				
				//isBusy = false;

				// Perform the request
				if (!isBusy) {
					myAgent.addBehaviour(new RequestPerformer());
				}
				
			}
		});
		
		addBehaviour(new CyclicBehaviour() { 
			public void action() {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					// CFP Message received. Process it
					ACLMessage reply = msg.createReply();
					if (!isBusy) {
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(Integer.toString(user_X) + " " + Integer.toString(user_Y));
						
					}
					else {
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setContent("");
					}
					myAgent.send(reply);
				}
				else {
					block();
				}
			}
		});
		
		addBehaviour(new TickerBehaviour(this, 60000) { // second lab
			protected void onTick() {
				//System.out.println("Daily increase of the amount of money");
				wallet += 20;
			}
		});
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Passenger-agent "+getAID().getName()+" terminating.");
	}
	
	private class RequestPerformer extends Behaviour {
		private AID closestDriver; // The agent who provides the best offer
		private double bestDistance;
		private int repliesCnt = 0; // The counter of replies from taxi agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		long t_start;
		long t_full;
		double user_X_new;
		double user_Y_new;
		int pos;

		public void action() {
			switch (step) {
			case 0:
				isBusy = true;
				t_start = System.currentTimeMillis();
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < taxiAgents.length; ++i) {
					cfp.addReceiver(taxiAgents[i]);
				} 
				cfp.setContent(Integer.toString(user_X) + " " + Integer.toString(user_Y));
				cfp.setConversationId("taxi-order");
				cfp.setReplyWith("cfp"+ System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("taxi-order"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						double distance = Double.parseDouble(reply.getContent());
						if (closestDriver == null || distance < bestDistance) {
							// This is the best offer at present
							bestDistance = distance;
							closestDriver = reply.getSender();
						}
						//System.out.println("///" + reply.getSender().getName() + " " + distance + " " + (repliesCnt+1));
					}
					repliesCnt++;
					if (repliesCnt >= taxiAgents.length) {
						// We received all replies
						//System.out.println(reply.getSender().getName());
						step = 2; 
					}
				}
				else {
					block();
				}
				isBusy = false;
				
				break;
			case 2:
				ACLMessage cfp_c = new ACLMessage(ACLMessage.CFP); // call for proposal
				pos = (int)(Math.random() * customers.length);
				
				while (getAID().getName().equals(customers[pos].getName())) {
					pos = (int)(Math.random() * customers.length);
				}
				
				cfp_c.addReceiver(customers[pos]);
				cfp_c.setConversationId("get-info");
				cfp_c.setReplyWith("cfp"+ System.currentTimeMillis()); // Unique value
				myAgent.send(cfp_c);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("get-info"),
						MessageTemplate.MatchInReplyTo(cfp_c.getReplyWith()));
				step = 3;
				
				break;
			case 3:
				ACLMessage reply_c = myAgent.receive(mt);
				if (reply_c != null) {
					// Reply received
					
					if (reply_c.getPerformative() == ACLMessage.INFORM) {
						// This is an offer 
						System.out.println("Passenger-agent "+getAID().getName()+" decided to go to " + customers[pos].getName());
						String position = reply_c.getContent();
						user_X_new = Double.parseDouble(position.split(" ")[0]);
						user_Y_new = Double.parseDouble(position.split(" ")[1]);
						step = 4;
					} else {
						step = 2;
					} 
					
				}
				else {
					block();
				}
				break;
			case 4:
				//user_X_new = (int)(Math.random() * X);
				//user_Y_new = (int)(Math.random() * Y);
				t_full = System.currentTimeMillis() - t_start;
				ACLMessage order;
				if ((t_full/600000 + (bestDistance*2.5/1000)/1000) < 1) {
					// Send the purchase order to the seller that provided the best offer
					order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(closestDriver);
					order.setContent(Double.toString(user_X_new) + " " + Double.toString(user_Y_new));
					order.setConversationId("taxi-order");
					order.setReplyWith("order"+ System.currentTimeMillis());
					step = 5;
				}
				else { 
					order = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
					order.addReceiver(closestDriver);
					order.setContent(Integer.toString(user_X) + " " + Integer.toString(user_Y));
					order.setConversationId("taxi-order");
					order.setReplyWith("order"+ System.currentTimeMillis());
					System.out.println("Attempt failed: Price and waiting time is too huge. " + (t_full/60000 + (bestDistance*2.5/1000)/1000000));
					step = 6;
				}
				/*
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(closestDriver);
				order.setContent(Integer.toString(user_X) + " " + Integer.toString(user_Y));
				order.setConversationId("taxi-order");
				order.setReplyWith("order"+ System.currentTimeMillis());*/
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("taxi-order"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				
				//step = 5;
				break;
			case 5:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						double dist = Math.sqrt(Math.pow(user_X_new-user_X, 2) + Math.pow(user_Y_new-user_Y, 2));
						System.out.println(getAID().getName()+" successfully ordered taxi from agent "+reply.getSender().getName());
						System.out.println("Distance = "+ dist + ", price = " + dist*2.5/1000);
						//myAgent.doDelete();
						//t_full = System.currentTimeMillis() - t_start;
						System.out.println("Waiting time = "+t_full);
						
						full_wait += t_full;
						num_orders++;
						delay = full_wait/num_orders;
						user_X = (int) user_X_new;
						user_Y = (int) user_Y_new;
						
						try {
							Thread.sleep((long)(Math.random() * 30000));
					    } catch (InterruptedException e) {
					    	e.printStackTrace();
					    }
						
						
					} 
					else {
						System.out.println("Attempt failed!");
					}

					step = 6;
					
				}
				else {
					block();
				}
				break;
			}        
		}
		
		public boolean done() {
			if (step == 2 && closestDriver == null) {
				System.out.println("Attempt failed: "+" there is no free taxi drivers");
			}
			return ((step == 2 && closestDriver == null) || step == 6);
		}
	}
}