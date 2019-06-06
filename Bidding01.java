import java.util.LinkedList;
import java.util.List;

import repast.simphony.random.RandomHelper;

// This class contains a bidding strategy based on: 
// P. Vytelingum, D. Cliff, N.R. Jennings, Strategic bidding in continuous double auctions, 
// Artificial Intelligence, Volume 172, Issue 14, September 2008, Pages 1700-1729, ISSN 0004-3702, 
// http://dx.doi.org/10.1016/j.artint.2008.06.001.
// (http://www.sciencedirect.com/science/article/pii/S0004370208000787)
// Keywords: Continuous double auction; Bidding strategy; Evolutionary game theory

public class AAggressive {
	
	// Variables used to support the bidding strategy
	
	private int N;
	private double Pest;
	private double MyAggressiveness;
	private double delta;
	private double rshout;
	private double lr;
	private double la;
	private double b1;
	private double b2;
	private double alpha;
	private double amin;
	private double amax;
	private double theta;
	private double thmax;
	private double thmin;
	private double gama;
	private double cj;
	private double li;
	private double eta;
	private double Mylastprice;
	private double tau;
	private double MAX_VALUE;
	
	private LinkedList<Double> PriceList; 
	private LinkedList<Double> WPrice;
	private boolean MyLastTrans;
	
	private boolean Prosumer;
	
	private double willingness;
	
	/*public NrgXLearningAgent(int houseID, Consumer consumer, OrderBook orderBook) {
		super(houseID, consumer, orderBook);
		this.houseID = houseID;
		setValues4Bidding();
	}*/	
	
	public AAggressive(boolean Role){ // If Role is false then consumer, otherwise prosumer
		this.Prosumer = Role;
//		setValues4Bidding();
	}
	
	public void setValues4Bidding(){

		MAX_VALUE = 0.430;
		eta = 3.0; 
		N = 4;
		
		PriceList = new LinkedList<Double>();
		WPrice    = new LinkedList<Double>();
		
		// Adding 'previous prices'
		PriceList.add(0.01); PriceList.add(0.215); PriceList.add(0.215); PriceList.add(0.01);
		// Adding weights for moving average method
		WPrice.add(0.63); WPrice.add(0.72); WPrice.add(0.81); WPrice.add(0.9);
		
		//set PriceList [0.18 0.2 0.2 0.18] ; CHANGE
		//;set WPrice [0.1062 0.1181 0.13122 0.1458 0.162 0.18 0.2] ; weights for moving average price, sum = 1 
		//set WPrice [0.7 0.8 0.9 1.0]
		Pest  = 0.18; // it changes dynamically
		  		  
		MyAggressiveness = RandomHelper.nextDoubleFromTo(0.5,1.0); //random-normal 0.8 0.2
		delta  = RandomHelper.nextDoubleFromTo(0.1,1.0); // random-normal 0.5 0.1
		rshout = RandomHelper.nextDoubleFromTo(0.1,1.0); // random-normal 0.5 0.1
		lr     = RandomHelper.nextDoubleFromTo(0.1,1.0); // random-normal 0.5 0.1 
		la     = RandomHelper.nextDoubleFromTo(0.1,1.0); // random-normal 0.5 0.1  
		b1     = RandomHelper.nextDoubleFromTo(0.1,1.0); // random-normal 0.5 0.1 
		b2     = RandomHelper.nextDoubleFromTo(0.1,1.0); // random-normal 0.5 0.1  
		  
		alpha = RandomHelper.nextDoubleFromTo(0.08,0.12); // random-normal 0.1 0.01 ; Smith�s coefficient of convergence 
		amin  = (alpha - 0.1);
		amax  = (alpha + 0.1);
		theta = 4.0; // initial value as it will change during the learning  
		thmax = 8.0; 
		thmin = -8.0;
		gama  = 0.9;
		
		tau = 0.01;  // initial value as it will change during the learning
		
		Mylastprice = -1.0;  
		MyLastTrans = false;		

		cj = RandomHelper.nextDoubleFromTo(0.0001,MAX_VALUE/2.0); // limit price seller - RandomHelper.nextDoubleFromTo(Constants.maxBidForTokens - 0.200, Constants.maxBidForTokens - 0.015);
		li = RandomHelper.nextDoubleFromTo(0.0001,MAX_VALUE/2.0); // limit price buyer
		
		// Willingness to pay more
		willingness = RandomHelper.nextDoubleFromTo(0.1,0.3); //0.2; // 20% more
	}
	
	public double getTau()
	{
		return tau;
	}

	public void updateTau()
	{
		double thetaM   = 2; // this is calculated such that the function is continuous as myr = 0, that is there is no jump in the first derivative of ? .
		               // Play with this in matlab
		               // x = [-1.0:0.1:1.0];
		               // plot(x,Pest * (1 - (exp(-x * thetaM) - 1)/ (exp(thetaM) - 1)) );
		               // where Pest is target price

		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("             Computing tau Pest = " + Pest);
		if (Prosumer)
		{
			// Seller
			// A (seller) is intra-marginal if its limit price is (lower) than the competitive equilibrium price
			if(cj < Pest)
			{
				// intra-marginal seller
				if(MyAggressiveness <= 0) // Eq 4
				{
					// We use ThetaM
					tau = (Pest + (MAX_VALUE - Pest) * ((Math.exp(-1 * MyAggressiveness * thetaM) - 1) / (Math.exp(thetaM) - 1)) );
				}
				else
				{
					// Otherwise we use theta
					tau = (cj + (Pest - cj) * (1 - ( (Math.exp(MyAggressiveness * theta) - 1) / (Math.exp(theta) - 1) ))); 
				}
			}
			else
			{
				// extra-marginal seller
				if(MyAggressiveness <= 0) // using eq 6
				{
					tau = (cj + (MAX_VALUE - cj) * ((Math.exp(-1 * MyAggressiveness * theta) - 1) / (Math.exp(theta) - 1)));
				}
				else
				{
					tau = cj;
				}
			}
		}
		else
		{
			// Buyer
			// A buyer is intra-marginal if its limit price is higher than the competitive equilibrium price
			if(li > Pest)
			{
				// intra-marginal buyer
				if(MyAggressiveness <= 0) // using eq 3
				{
					tau = (Pest * (1 - ( (Math.exp(-1 * MyAggressiveness * thetaM) - 1) / (Math.exp(thetaM) - 1))));
				}
				else
				{
					tau = (Pest + (li - Pest) * ((Math.exp(MyAggressiveness * theta) - 1) / (Math.exp(theta) - 1)));
				}
			}
			else
			{
				// extra-marginal buyer
				if(MyAggressiveness <= 0) // using eq 5
				{
					tau = (li * (1 - ((Math.exp(-1 * MyAggressiveness * theta) - 1) / (Math.exp(theta) - 1))));
				}
				else
				{
					tau = li;
				}
			}
		}
		
		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("             Computing tau = " + tau);		
	}	
	
	public void updateTheta()
	{
		// After every transaction
		double ePest = getPest();
		double sumPP = 0.0;
		
		// computing alpha
		for(Double thisPrice: PriceList)
		{
			sumPP += Math.pow((thisPrice - ePest), 2);
		}
		
		// Equation 8
		alpha = ((Math.sqrt(sumPP/N))/ePest);
		
		// Equation 9
		double aEst  = ( (alpha - amin) / (amax - amin)); 
		double thest = ((thmax - thmin) * (1 - aEst * Math.exp(gama * (aEst - 1))) + thmin);
		
		// Second part Eq 8
		theta = ( theta + b2 * (thest - theta));
	}
	
	public void updateMyAggressiveness(int tradingSlot, OrderBook orderBook)
	{
		double lastBprice = 0.0;
		double lastSprice = 0.0;
		double ThisOA = orderBook.getLowestAsk();
		double ThisOB = orderBook.getHighestBid();
		
		// get inside market from order book (for that slot)		
		List<Double> buys = orderBook.getAllTradeBuys(tradingSlot);
		List<Double> sells = orderBook.getAllTradeSells(tradingSlot);
		
		if ((buys != null) && buys.size() > 1)
		{
			lastBprice = Math.abs(buys.get(buys.size()-1));
		}
		
		if ((sells != null) && sells.size() > 1)
		{
			lastSprice = Math.abs(sells.get(sells.size()-1));
		}
				
		if (Prosumer)
		{
			// Seller
			if((sells != null) && sells.size() > 1 && lastSprice > 0.0)
			{
				// There was at least one transaction
				if(tau <= lastSprice)
				{
					// less aggressive
					delta = ( (1 - lr) * rshout - la);
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            Trans - SELLER must be less aggressive ����� " + delta);
				}
				else
				{
					//  more aggressive
					delta = ( (1 + lr) * rshout + la);
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            Trans - SELLER must be MORE aggressive ����� " + delta);
				}
			}
			else
			{
				// No transaction
				if((ThisOA > 0) && (tau >= ThisOA))
				{
					// seller must be more aggressive
					delta = ( (1 + lr) * rshout + la);
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            NoTrans - SELLER must be MORE aggressive ����� " + delta);
				}
			}
		}
		else
		{
			// Buyer
			if((buys != null) && buys.size() > 1 && lastBprice > 0.0)
			{
				// there was at least one transaction
				if(tau >= lastBprice)
				{
					// less aggressive
					delta = ( (1 - lr) * rshout - la);
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            Trans - BUYER must be less aggressive " + delta);
				}
				else
				{
					// more aggressive
					delta = ( (1 + lr) * rshout + la);
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            Trans - BUYER must be MORE aggressive " + delta);
				}
			}
			else
			{
				// No transaction
				if((ThisOB > 0) && (tau <= ThisOB))
				{
					// More aggressive
					delta = ( (1 + lr) * rshout + la);
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            NoTrans - BUYER must be MORE aggressive " + delta);
				}
			}
			
		}// end else - if (owner instanceof Prosumer)
		
		// update aggressiveness
		MyAggressiveness = (MyAggressiveness + b1 * (delta - MyAggressiveness));
	}
	
	public double getMyAggressiveness()
	{
		return MyAggressiveness;
	}
	
	public void computePest(int slot, OrderBook orderBook)
	{
		double ePrice = 0.0;
		double price = 0.0;
		int index = 0;
		
		List<Double> BuyP  = orderBook.getAllTradeBuys(slot+1);
		List<Double> SellP = orderBook.getAllTradeSells(slot+1);
		
		if( (BuyP != null) && (SellP != null))
		{
			// average price
			price = (BuyP.get(BuyP.size() - 1) + SellP.get(SellP.size() - 1)) / 2;
		}
		else
		{
			if(BuyP != null)
			{
				// get buy price
				price = BuyP.get(BuyP.size() - 1);
			}
			else
			{
				if(SellP != null)
				{
					// get sell price
					price = SellP.get(SellP.size() - 1);
				}
			}
		}
		
		if(price > 0.0) // once there is a transaction, update new price
		{
			PriceList.add(price);
			PriceList.removeFirst();
		}
		
		if (Constants.VERBOSE_LEVEL > 2)  System.out.print("            COMPUTING PEST : ");
		for(Double thisprice : PriceList)
		{
			if (Constants.VERBOSE_LEVEL > 2)  System.out.print(" " + thisprice + " (" + WPrice.get(index) + ") ");
			ePrice += (thisprice * WPrice.get(index));
			index++;
		}
		Pest = ePrice / N;
		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("            FINAL ePrice = " + ePrice + " Pest = " + Pest);		
	}
	
	public double getPest()
	{
		return Pest;
	}
	
	public void updateValues(int tradingSlot, boolean trans, OrderBook orderBook)
	{
		MyLastTrans = trans;
		computePest(tradingSlot,orderBook);
		updateTheta();
		updateTau();
		updateMyAggressiveness(tradingSlot,orderBook);
	}
	
	public double getPrice2Buy(int tradingSlot, OrderBook orderBook) {
		// BUYER
		double myBid = 0.0;
		double thisOA    = orderBook.getLowestAsk();
		double thisOB    = orderBook.getHighestBid();
		double OASKP = 0.0;

		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         BUYER ");		
		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         thisOA = " + thisOA);
		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         thisOB = " + thisOB);
		
		//if(Double.isNaN(thisOA) && Double.isNaN(thisOB))
		if(Double.isNaN(thisOA))
			thisOA = MAX_VALUE;
		
		if(Double.isNaN(thisOB))
			thisOB = 0.0;
		
		if(li <= thisOB)
		{
			if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         SUBMIT NO BID li is not <= to Obid");
			// Change based on willingness
			
		}
		else
		{
			if(Mylastprice < 0.0) // if first round
			{
				// it starts with LOW BIDS that progressively approach the minimum of its limit price, li, and the outstanding ask, oask
		        // Bidding using eq. 10
		        OASKP = ( (1 + lr) * thisOA + la );
		        myBid = (thisOB + ( ( Math.min (li,OASKP)) - thisOB ) / eta);
		        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         FIRST ROUND li = " + li);
		        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         OASKP = " + OASKP);  
		        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         my-bid-coin = " + myBid);				
			}
			else
			{
				if((thisOA <= getTau()) && (thisOA != MAX_VALUE))
				{
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         ACCEPT Oask directly!!! tau = " + tau);
		            myBid = thisOA; // this buyer is going to get one item					
				}
				else
				{
		            // bidding using eq. 10            
		            myBid = (thisOB + (getTau() - thisOB) / eta );
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         OTHER ROUND tau = " + tau);
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         ThisOB = " + thisOB);  
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         eta = " + eta);
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         my-bid = " + myBid);					
				}// end else - if((thisOA <= getTau()) && (thisOA != MAX_VALUE))
				
				// ============================ EXTRA, NOT DEFINED IN PAPER ======================
				if( (myBid >= thisOA) && (thisOA < MAX_VALUE) && (myBid < li))
				{
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         EXTRA ACCEPT Oask directly!!! tau was wrong = " + tau);
			        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         EXTRA ACCEPT Oask directly!!! old my-bid-coin = " + myBid);  
			        myBid = thisOA; // this buyer is going to get one item  					
				}
				
			}// end else - if first round
		}// end else - if(li <= ThisOB)
		
		Mylastprice = myBid;
		return myBid;
	}
	
	public double getPrice2Sell(int tradingSlot, OrderBook orderBook) {
		// SELLER
		double myAsk = 0.0;

		double thisOA    = orderBook.getLowestAsk();
		double thisOB    = orderBook.getHighestBid();
		double OBID = 0.0;

		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         SELLER ");		
		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         thisOA = " + thisOA);
		if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         thisOB = " + thisOB);
		
		//if(Double.isNaN(thisOA) && Double.isNaN(thisOB))
		if(Double.isNaN(thisOA))
			thisOA = MAX_VALUE;
		
		if(Double.isNaN(thisOB))
			thisOB = 0.0;

		if(cj >= thisOA)
		{
			if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         Submit no ask cj = " + cj);
		}
		else
		{
			if(Mylastprice < 0.0) // if first round
			{
				// Submit ask using eq. 11
		        OBID  = ( (1 - lr) * thisOB - la );
		        myAsk = ( thisOA - (thisOA - (Math.max(cj,OBID))) / eta );
		        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         Submit ask using eq. 11 OBID = " + OBID);
		        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         cj = " + cj);
		        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         my-ask-coin = " + myAsk);
			}
			else
			{
				if((thisOB >= getTau()) && (thisOB > 0))
				{
					// accept obid
		            myAsk = thisOB; // this seller is going to sell one item
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         Accept bid directly!!!! tau = " + tau);
				}
				else
				{
					// submit ask using eq. 11
		            myAsk = (thisOA - (thisOA - getTau()) / eta);
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         OTHER ROUND - submit ask using eq. 11");
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         tau = " + tau);
		            if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         my-ask-coin = " + myAsk);
				}// end else - if((thisOB >= getTau()) && (thisOB > 0)) 
				
				
				// =========================== EXTRA TOO!!!!!!!!!!!!!
				if(myAsk <= thisOB)
				{
					// Take bid
					if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         EXTRA-SELL Accepting bid directly!!!! tau was wrong = " + tau);    
			        if (Constants.VERBOSE_LEVEL > 2)  System.out.println("         EXTRA-SELL Accepting bid directly!!!! my-ask-coin = " + myAsk);           
			        myAsk = thisOB; //this seller is going to sell one item
				}
				
			}// end else - if(Mylastprice < 0.0)
		} // end else - if(cj >= thisOA)
		
		Mylastprice = myAsk;
		return myAsk;
	}	
}
