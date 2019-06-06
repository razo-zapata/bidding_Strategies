import com.google.inject.Inject;
import java.util.Random;

public class DemoTradeAgent implements ITradeAgent, TransactionCompletedListener{

	private IOrderBook orderbook;
	private IEnergyAgent energyAgent;
	@SuppressWarnings("unused")
	private ISubstationEndpoint substation;
    
	// Variables used to support: An adaptive attitude bidding strategy for agents 
	// in continuous double auctions. Journal of Electronic Commerce Research and Applications. 
	private double MyEagerness, G1, G2, G3, G4, TransR, TransP, TR, TP, WS, WL, U, CIK, DJK, Pbasic, Ptarget;
	private double alpha, omega, delta, theta, beta, myepsilon, NUM_winner, NUNIT_traded, NUNIT_owned;
	private double W1, W2, W3, LastPrice;	
	private boolean LastTrans;
	private int NumTotalTrans;
	
	private Random RandomHelper;
	
	boolean SHOW_DEBUG_TEXT;
	
	@Inject
	public DemoTradeAgent(IOrderBook orderbook, ISubstationEndpoint substation){
		this.orderbook = orderbook;
		this.substation = substation;
		SHOW_DEBUG_TEXT = false;
	}
    
	@Override
	public void bid(){
		double bidPrice = 0;
		double bidQuantity = 0.0;
		boolean fullyMatch = RandomHelper.nextDouble() < 0.25; // Constants.immediateMatchProbability; 
		boolean immediateMatch = RandomHelper.nextDouble() < 0.25; // Constants.immediateMatchProbability;
		int tradingSlot = 1; // IMPORTANT! in token exchange market there are no slots! That is why all bids are submitted for the same slot
		bidQuantity = energyAgent.getCoins();
		
		// Prosumer or consumer
		if(energyAgent.isProsumer()){		
			displayText(" Computing ASK for Coins : " + bidQuantity);
			bidPrice = getPrice2Sell();
			displayText(" ASK = " + bidPrice);
			if( (bidPrice > 0.0) && (Math.abs(bidQuantity) >  0.01)) // Constants.minQuantitytoTrade)) // CHANGE !!!!
			{
				// Send ASK to cloud
			   Bid myBid = new Bid("SELL", bidQuantity, bidPrice, tradingSlot, fullyMatch, immediateMatch, energyAgent.getId());
			   displayText("Producer " + energyAgent.getId() + " is selling " + bidQuantity + " units at price " + bidPrice + " for slot " + tradingSlot + ". fullyMatch: " + fullyMatch + ", immediateMatch: " + immediateMatch);
			   orderbook.submitSell(myBid);
			}			
		}
		else{
			displayText(" Computing BID for Coins : " + bidQuantity);
			bidPrice = getPrice2Buy();
			displayText(" BID = " + bidPrice);
			if( (bidPrice > 0.0) && (Math.abs(bidQuantity) > 0.01)) // Constants.minQuantitytoTrade)) // CHANGE !!!!
			{
				// send BID to cloud
			   Bid myBid = new Bid("BUY", bidQuantity, bidPrice, tradingSlot, fullyMatch, immediateMatch, energyAgent.getId());			
			   displayText(energyAgent.getId() + " is buying. " + bidQuantity + " units at price " + bidPrice + " for slot " + tradingSlot + ". fullyMatch: " + fullyMatch + ", immediateMatch: " + immediateMatch);
			   orderbook.submitBuy(myBid); 
			}						
		}
	}
	
	@Override
	public void transactionCompleted(TransactionCompleted transaction) {
		//TODO implement this, update energyAgent data with transaction information
		
		// Updates applicable to THIS agent, we must filter by ID
		// Add or remove coins
		// Increase or decrease budget
		
		
		// Updates applicable to all agents
		// After each transaction, the final price must be updated
		LastPrice = transaction.getPrice();		
		// Update the value of eagerness
		computeEagerness();
		// There was a transaction
		NumTotalTrans++;
		LastTrans = true;		
	}

	@Override
	public void setEnergyAgent(IEnergyAgent energyAgent) {
		this.energyAgent = energyAgent;
		this.orderbook.addTransactionCompletedListener(this.energyAgent.getId(), this);
		// Setting values for bidding
		setValues4Bidding();
	}	
	
	// Determines the price to sell an item
	public double getPrice2Sell(){
		// Seller
		displayText("         SELLER " + energyAgent.getId());
		double thisPrice = 0.0;
		double Sstep     = 0.0;
		double thisOA    = orderbook.getTopSellPrice();
		double thisOB    = orderbook.getTopBuyPrice();		
		displayText("         thisOA = " + thisOA);
		displayText("         thisOB = " + thisOB);
		
		if((thisOA == 0.0) && (thisOB == 0.0)){
			displayText("   First phase OA=OB=NULL");
			thisPrice = (CIK + (orderbook.getPUL() - CIK) * G1);
		}
		else{
			if((thisOA == 0.0) && (thisOB > 0.0)){
				// If there is OB and no OA
				displayText("         there is OB and no OA ");
				if(thisOB >= omega){
					thisPrice = thisOB;
					displayText("         5.1 take it directly!");
				}
				else{
					if((thisOB < omega) && (thisOB > CIK)){
						thisPrice = (thisOB + (orderbook.getPUL() - thisOB) * getEagerness());
						displayText("         5.2");
					}
					else{
						if ( (thisOB < omega) && (thisOB <= CIK)){
							thisPrice = (CIK + (orderbook.getPUL() - CIK) * getEagerness());
							displayText("         5.3");
						}
						else{
							displayText("         Error while setting ask");
						}						
					}
				}
			}
			else{
				if( (thisOB == 0.0) && (thisOA > 0.0)){
					// If there is OA and no OB in the round
					displayText("         there is OA and no OB in the round ");
					if(thisOA < alpha){
						// OA is lower than alpha
						displayText("         the seller will submit no new ask because it thinks that the current round is not profitable at all");
					}
					else{
						// Otherwise, the seller will give a new ask slightly lower than the current OA.
						displayText("         new ask slightly lower than the current OA");
						thisPrice = thisOA - myepsilon;
					}
				}
				else{
					// In the third phase, both OA and OB exist in the market.
					displayText("         both OA and OB exist in the market ");
					// If OB is higher than or equal to omega, the seller will submit an ask equal to OB.
					if(thisOB >= omega){
						thisPrice = thisOB;
						displayText("         OB is higher than or equal to omega, the seller will submit an ask equal to OB ... so take it directly!!! ");
					}
					else{
						// If OA is lower than alpha, the seller will submit no new ask.
						if(thisOA < alpha){
							displayText("         OA is lower than alpha, the seller will submit no new ask.");
						}
						else{
							// If OA is not too low and OB is not so high, the seller will compute its ask according to its eagerness.
							displayText("         the seller will compute its ask according to its eagerness ... ");
							Pbasic = CIK * G2;
							if(Pbasic >= orderbook.getPUL()){
								Pbasic = orderbook.getPUL() - myepsilon;
							}
							
							if(getLastTrans()){
								// there was a successful transaction in the last round
								double ThisPt_last = getLastPrice();
								Ptarget  = Math.max((ThisPt_last + theta), thisOB);
								displayText("         Pbasic = " + Pbasic + " there was a successful transaction in the last round. Ptarget " + Ptarget);								
							}
							else{
								// there was no transaction in the last round
								double ThisOAlast = orderbook.getLastOA();
								Ptarget = Math.max((ThisOAlast - beta) , thisOB);
							    displayText("         Pbasic = " + Pbasic + " there was NO transaction in the last round. Ptarget " + Ptarget);
							}

							if(Ptarget >= orderbook.getPUL()){
								Ptarget = orderbook.getPUL();
							}
							
							
							if(Ptarget >= Pbasic){
								Sstep = (Ptarget - Pbasic) * getEagerness();
							}
							else{
								Sstep = ( ( Math.max(Ptarget,CIK) - Pbasic ) * (1 - getEagerness()) );
							}
							displayText("         Sstep = " + Sstep);
							thisPrice = Pbasic + Sstep;
						}
						// DONE ...
					}
				}
				
			}
		}// End: else -> if( (ThisOA == Double.NaN) && (ThisOB == Double.NaN))
		
		if( ( (thisPrice > orderbook.getPUL()) || (thisPrice < orderbook.getPLL())) && (thisPrice != 0.0)){
			displayText("         ERROR SETTING SELLING PRICE = " + thisPrice);
			// Send no ASK
			thisPrice = 0.0;
		}
		
		return thisPrice;

	}
	
	// Determines the price to buy an item
	public double getPrice2Buy(){
		// Buyer
		displayText("         BUYER ");				
		double thisPrice = 0.0;
		double Sstep     = 0.0;
		double thisOA    = orderbook.getTopSellPrice();
		double thisOB    = orderbook.getTopBuyPrice();

		displayText("         thisOA = " + thisOA);
		displayText("         thisOB = " + thisOB);
		
		if((thisOA == 0.0) && (thisOB == 0.0)){
			// In the first phase, the buyer has no information other than its reservation price
			displayText("         the buyer has no information other than its reservation price");
			thisPrice = (DJK - (DJK - orderbook.getPLL()) * G3);
		}
		else{
			if( (thisOA > 0.0) && (thisOB == 0.0)){
				displayText("         there exists OA and no OB");
				if(thisOA <= alpha){
					displayText("         the ask is low enough to be accepted directly");
					thisPrice = thisOA;
				}
				else
				{
					if((thisOA > alpha) && (thisOA <= DJK)){
						// A
						displayText("         12.2");
						thisPrice = (thisOA - (thisOA - orderbook.getPLL()) * getEagerness());
					}
					else{
						// B
						if( (thisOA > alpha) && (thisOA > DJK)){
							displayText("         12.3");
							thisPrice = (DJK - (DJK - orderbook.getPLL()) * getEagerness());
						}
						else{
							// A
							displayText("         Error while setting bid");
						}
					}
				}
			}
			else{
				// If there exists OB and no OA
				if( (thisOB > 0.0) && (thisOA == 0.0)){
					displayText("         there exists OB and no OA");
					// the buyer will submit no bid if the current OB is higher than omega --- there might be an error in the PAPER, it must be alpha
					if(thisOB > alpha){
						displayText("         the buyer will submit no bid if the current OB is higher than alpha = " + alpha + " DJK = " + DJK);
					}
					else{
						displayText("         the buyer will submit its new bid slightly higher than the current OB");
						thisPrice = thisOB + myepsilon;
					}
				}
				else{
					// there are already OB and OA
					displayText("         there are already OB and OA");
					// If the current OA is lower than or equal to alpha, the buyer will think that it is profitable and accept the OA directly.
					if(thisOA <= alpha){
						//
						thisPrice = thisOA;
						displayText("         the buyer will think that it is profitable and accept the OA directly");
					}
					else{
						// If the current OA is not profitable and the current OB is higher than omega, this buyer will not submit any new bid.
						if ( (thisOA > omega) && (thisOB > omega)){
							displayText("         this buyer will not submit any new bid");
						}
						else{
							displayText("         the buyer will compute the bid by the following steps :::");
							Pbasic = DJK * G4;
							
							if(Pbasic >= orderbook.getPUL()){
								Pbasic = orderbook.getPUL() - myepsilon;
							}							
							
							// If there was a successful transaction in the last round
							if(getLastTrans()){ // If there was a successful transaction in the last round
								Ptarget = Math.min( (getLastPrice() - theta), orderbook.getTopSellPrice() );
								displayText("         Pbasic = " + Pbasic + " there was a successful transaction in the last round. Ptarget " + Ptarget);
							}
							else{
								// If there was no transaction in the last round
								double ThisOBlast = orderbook.getLastOB();
								if(ThisOBlast > 0)
									Ptarget = Math.min( (ThisOBlast + beta), orderbook.getTopSellPrice() );
								else
									Ptarget = orderbook.getTopSellPrice();
								displayText("         Pbasic = " + Pbasic + " there was NO transaction in the last round. Ptarget " + Ptarget);
							}
							
							if(Ptarget >= orderbook.getPUL()){
								Ptarget = orderbook.getPUL();
							}
							
							if(Ptarget <= Pbasic){
								Sstep = (Ptarget - Pbasic) * getEagerness();
							}
							else{
								Sstep = (Math.min (Ptarget, DJK) - Pbasic) * (1 - getEagerness());
							}
							
							displayText("         Sstep = " + Sstep);
							thisPrice = Pbasic + Sstep;
						}
						// DONE
					}
				}
			}
		} // END else - if((thisOA == Double.NaN) && (thisOB == Double.NaN))

		if( ( (thisPrice > orderbook.getPUL()) || (thisPrice < orderbook.getPLL())) && (thisPrice != 0.0)){
			displayText("         ERROR SETTING BUYING PRICE = " + thisPrice);
			// Send no BID
			thisPrice = 0.0;
		}
		
		return thisPrice;
	}
	
	// Return the eagerness of the trading agent
	public double getEagerness(){		
		return MyEagerness;
	}
	
	// Computes agent's eagerness
	public void computeEagerness(){
		// 
		// transaction rate 
		if( (NUM_winner > 0) && (getNumTotalTrans() > 0)){
			TransR = NUM_winner / getNumTotalTrans();
		}
		else{
			TransR = 0.5 + ( RandomHelper.nextDouble() * 0.5); // A value between 0.5 and 1.0
		}
		
		// transaction percentage
		if( (NUNIT_owned > 0) && (NUNIT_traded > 0)){
			TransP = NUNIT_traded / NUNIT_owned; // can be improved
		}
		else{
			TransP = 0.5 + ( RandomHelper.nextDouble() * 0.5); // A value between 0.5 and 1.0
		}
		
		// short-term attitude
		TR = TransR * TransR; // as suggested in the paper but can be improved
		
		// long-term attitude
		TP = TransP * TransP; // as suggested in the paper but can be improved
		
		// weight of the short-term attitude, this can also be improved
		if(TR < 0.25){
		  // TR is small
		  WS = W1 * (TR * TR);
		}
		else{
		  if(TR < 0.7){
			// TR is medium
			WS = W2 * (TR * TR);
		  }
		  else{
			// TR is large
			WS = W3 * (TR * TR);
		  }
		}
		
		// weight of the long-term attitude
		if(TP == 1.0){
		  WL = U + delta;
		}
		else{
		  WL = U - delta;
		}
		
		MyEagerness = WS * WL;
		
		if( (MyEagerness > 1.0) || (MyEagerness <= 0.0)){
			displayText("         ERROR while computing Eagerness = " + MyEagerness);
			MyEagerness = 1.0; // RandomHelper.nextDoubleFromTo(0.5,1.0);
		}		
	}
	
	// Sets default values for trading agent
	public void setValues4Bidding(){

		RandomHelper = new Random();
		// Eagerness between 0.5 and 1.0
		MyEagerness = 0.5 + ( RandomHelper.nextDouble() * 0.5);
		
		double minBidForTokens = 0.005;
		// 
		CIK         = minBidForTokens + ( RandomHelper.nextDouble() * ( minBidForTokens + 0.15)) ;
		double maxBidForTokens = 0.215;
		DJK         = (maxBidForTokens - 0.200) + (RandomHelper.nextDouble() * (maxBidForTokens - 0.015));
		omega       = CIK + 0.02; // omega should be higher than CIK 
		alpha       = DJK - 0.02; // alpha should be lower than DJK 
		theta       = 0.0015 + RandomHelper.nextDouble() * 0.0010; // (0.0015,0.0025);
		beta        = 0.0015 + RandomHelper.nextDouble() * 0.0010; // (0.0015,0.0025);
		
		G1          = 0.85 + RandomHelper.nextDouble() * 0.15; // (0.85,1.0);
		G2          = 1.00 + RandomHelper.nextDouble() * 0.50; // (1.0,1.5);
		G3          = 0.85 + RandomHelper.nextDouble() * 0.15; // (0.85,1.0);
		G4          = 0.50 + RandomHelper.nextDouble() * 0.50; // (0.5,1.0);
		
		W1 = 0.5;  // Small
		W2 = 1.0;  // Medium  NOT CLEAR HOW TO SET THESE PARAMETERS, ONLY CONSTRAINT W1 < W2 < W3
		W3 = 1.5;  // Big   
		
		delta = 0.001;
		U     = 0.9;
		
		myepsilon = (minBidForTokens/2) * RandomHelper.nextDouble(); // (0,minBidForTokens/2) 
		
		NUM_winner    = 0;
		NUNIT_traded  = 0;
		NUNIT_owned   = 0; // This value must be changed at runtime
		LastPrice     = 0;
		LastTrans     = false;
		NumTotalTrans = 0;
	}	
	
	public double getLastPrice(){
		// After each transaction, the final price must be updated
		return LastPrice;
	}

	public double getNumTotalTrans(){
		return NumTotalTrans;
	}
	
	public Boolean getLastTrans(){
		return LastTrans;
	}	
	
	public void displayText(String str) {
		if (SHOW_DEBUG_TEXT) 
			System.out.println(str);
	}
	
}
