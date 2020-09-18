package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;
import net.corda.samples.states.Ledger;

import java.util.*;

/**
 * This flow is used to put a bid on an asset put on auction.
 */
public class BidFlow {

    private BidFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private final Amount<Currency> bidAmount;
        private final UUID auctionId;

        /**
         * Constructor to initialise flow parameters received from rpc.
         *
         * @param bidAmount is the amount the bidder is bidding for for the asset on auction.
         * @param auctionId is the unique identifier of the auction on which this bid it put.
         */
        public Initiator(Amount<Currency> bidAmount, UUID auctionId) {
            this.bidAmount = bidAmount;
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {


            // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
            // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
            // transaction.
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();

            StateAndRef<AuctionState> inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
                AuctionState auctionState = auctionStateAndRef.getState().getData();
                return auctionState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));


            AuctionState input = inputStateAndRef.getState().getData();

            FlowSession auctioneerSession = initiateFlow(input.getAuctioneer());
            auctioneerSession.send(true);
            boolean hasFunds = auctioneerSession.sendAndReceive(Boolean.class, bidAmount.getQuantity()).unwrap(it -> it);
            if(!hasFunds){
                throw new FlowException("Insufficient Balance");
            }

            Amount<Currency> highestBid;
            Party highestBidder;
            if(input.getHighestBid() == null) {
                highestBid = bidAmount;
                highestBidder = getOurIdentity();
            }
            else{
                highestBid = bidAmount.getQuantity()>input.getHighestBid().getQuantity()? bidAmount: input.getHighestBid();
                highestBidder = bidAmount.getQuantity()>input.getHighestBid().getQuantity()?  getOurIdentity(): input.getHighestBidder();
            }

            //Create the output state
            AuctionState output = new AuctionState(input.getAuctionItem(), input.getAuctionId(), input.getBasePrice(),
                    highestBid, input.getTotalBids().plus(bidAmount), highestBidder, input.getBidEndTime(), null, true,
                    input.getAuctioneer(), input.getBidders(), null);

            // Build the transaction. On successful completion of the transaction the current auction state is consumed
            // and a new auction state is create as an output containg tge bid details.
            TransactionBuilder builder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(new AuctionContract.Commands.Bid(), Arrays.asList(getOurIdentity().getOwningKey()));

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow to notarise and commit the transaction in all the participants ledger.
            List<FlowSession> allSessions = new ArrayList<>();
            List<Party> bidders = new ArrayList<>(input.getBidders());
            bidders.remove(getOurIdentity());
            for(Party bidder: bidders) {
                FlowSession session = initiateFlow(bidder);
                allSessions.add(session);
                session.send(false);
            }

            allSessions.add(auctioneerSession);
            SignedTransaction ftx = subFlow(new FinalityFlow(selfSignedTransaction, allSessions));
            return ftx;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            long amount = 0;
            boolean isAuctioneerSession = counterpartySession.receive(Boolean.class).unwrap(it->it);
            if(isAuctioneerSession){
                amount = counterpartySession.receive(Long.class).unwrap(it->it);
                List<StateAndRef<Ledger>> ledgerStateAndRefs = getServiceHub().getVaultService()
                        .queryBy(Ledger.class).getStates();

                if(ledgerStateAndRefs.size() > 0){
                    Ledger ledger = ledgerStateAndRefs.get(0).getState().getData();
                    long balance = ledger.getBalanceMap().get(counterpartySession.getCounterparty());
                    if(balance >= amount)
                        counterpartySession.send(true);
                    else
                        counterpartySession.send(false);

                }else{
                    counterpartySession.send(false);
                }

            }
            SignedTransaction ftx = subFlow(new ReceiveFinalityFlow(counterpartySession));
            if(isAuctioneerSession){
                subFlow(new UpdateLedgerFlow(amount, counterpartySession.getCounterparty()));
            }
            return ftx;
        }
    }
}