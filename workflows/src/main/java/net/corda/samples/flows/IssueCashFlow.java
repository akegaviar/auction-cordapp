package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.CashIssueAndPaymentFlow;
import net.corda.samples.contracts.AssetContract;
import net.corda.samples.states.AuctionState;
import net.corda.samples.states.Ledger;

import java.util.*;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class IssueCashFlow extends FlowLogic<SignedTransaction> {

    private Amount<Currency> amount;
    private Party recipient;
    private List<Party> recipients;

    public IssueCashFlow(Amount<Currency> amount, Party recipient) {
        this.amount = amount;
        this.recipient = recipient;
        this.recipients = null;
    }

    public IssueCashFlow(Amount<Currency> amount) {
        this.amount = amount;
        this.recipient = null;
        this.recipients = null;
    }
    public IssueCashFlow(Amount<Currency> amount, List<Party> recipients) {
        this.amount = amount;
        this.recipient = null;
        this.recipients = recipients;
    }


    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        List<StateAndRef<Ledger>> ledgerStateAndRefs = getServiceHub().getVaultService()
                .queryBy(Ledger.class).getStates();

        Ledger ledger;

        if(ledgerStateAndRefs.size() == 0){
            Map<Party, Long> ledgerBal = new LinkedHashMap<>();
            ledger = new Ledger(getOurIdentity(), ledgerBal);
        }else{
            ledger = new Ledger(getOurIdentity(), new LinkedHashMap<>(ledgerStateAndRefs.get(0).getState().getData().getBalanceMap()));
        }

        // Issue to all participants
        if(recipient == null){

            if(this.recipients == null) {
                recipients = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                        .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                        .collect(Collectors.toList());
                recipients.remove(getOurIdentity());
                recipients.remove(notary);
            }

            for(Party thisParty : recipients) {
                subFlow(new CashIssueAndPaymentFlow(amount, OpaqueBytes.of(getOurIdentity().toString().getBytes()), thisParty, false, notary));
                ledger.getBalanceMap().put(thisParty, amount.getQuantity());
            }

        }else {
            subFlow(new CashIssueAndPaymentFlow(amount, OpaqueBytes.of(getOurIdentity().toString().getBytes()), recipient, false, notary));
            ledger.getBalanceMap().put(recipient, amount.getQuantity());
        }

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(ledger)
                .addCommand(new AssetContract.Commands.LegerOp(), Collections.singletonList(getOurIdentity().getOwningKey()));

        if(ledgerStateAndRefs.size() > 0) {
            transactionBuilder.addInputState(ledgerStateAndRefs.get(0));
        }


        // Verify the transaction
        transactionBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        // Notarise the transaction and record the state in the ledger.
        return subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
    }
}
