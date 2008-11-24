import jade.lang.acl.*;
import talker.*;

public class Bid implements Comparable {
	ACLMessage bidder;
	Bundle2d bundle;
	
	public Bid(ACLMessage bidder, Bundle2d bundle) {
		this.bidder = bidder;
		this.bundle = bundle;
	}
	
	public int compareTo(Object another_bid) throws ClassCastException {
	    if (!(another_bid instanceof Bid))
	      throw new ClassCastException("Bid::compareTo(): A Bid object expected.");

		// multiply with high number to make diff significant on an int level
	    return (int) ((this.bundle.cost - ((Bid) another_bid).bundle.cost) * 1000.0);
	}
}
