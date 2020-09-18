# Blockchain Bootcamp: Using Corda on Chainstack for Decentralized Governance

You will be using the auction CorDapp developed by Ashutosh Meher, an R3 developer advocate.

You can read more about the auction CorDapp in [this blog post](https://www.corda.net/blog/creating-a-sample-auction-house-cordapp-from-scratch-part1/).

## Before the bootcamp

1. Join the Corda network as described in the email that you should have received.
1. Download the auction CorDapp workflow and contract JAR files from the [release tab](https://github.com/akegaviar/auction-cordapp/releases/tag/1.0).
1. Download the Corda finance CorDapp workflow and contract from Corda artifactory:
	* [Corda finance contract](https://software.r3.com/artifactory/corda-releases/net/corda/corda-finance-contracts/4.5-RC05/corda-finance-contracts-4.5-RC05.jar)
	* [Corda finance workflow](https://software.r3.com/artifactory/corda-releases/net/corda/corda-finance-workflows/4.5-RC05/corda-finance-workflows-4.5-RC05.jar)
1. Create a local directory named `cordapps` on your machine. Put all four CorDapp JAR files in the directory. You will need this at Step 7.
1. Install the auction workflow and contract one after the other on your Corda node. For instructions, see [Installing a CorDapp](https://docs.chainstack.com/operations/corda/installing-a-cordapp).
1. Install Corda finance workflow first on your node. Then install Corda finance contract on your node. It's important that you install the workflow first and only then the contract. Otherwise the installation will not work.
1. Connect to your node as described in [Corda tools](https://docs.chainstack.com/operations/corda/tools). Note that you will need to have either Java 8 installed or Docker as described in the documentation. For the Java version, see [Corda standalone shell](https://docs.chainstack.com/operations/corda/tools#corda-standalone-shell); for the Docker version, see [Chainstack standalone shell](https://docs.chainstack.com/operations/corda/tools#chainstack-standalone-shell).

To ensure you did everything correctly, once connected to your node, run the `flow list` command. The output should show you all the CorDapps installed.

Example:

```
Fri Sep 18 00:35:14 GMT 2020>>> flow list
net.corda.core.flows.ContractUpgradeFlow$Authorise
net.corda.core.flows.ContractUpgradeFlow$Deauthorise
net.corda.core.flows.ContractUpgradeFlow$Initiate
net.corda.finance.flows.CashExitFlow
net.corda.finance.flows.CashIssueAndPaymentFlow
net.corda.finance.flows.CashIssueFlow
net.corda.finance.flows.CashPaymentFlow
net.corda.finance.internal.CashConfigDataFlow
net.corda.samples.flows.AuctionDvPFlow$Initiator
net.corda.samples.flows.AuctionExitFlow$Initiator
net.corda.samples.flows.AuctionSettlementFlow
net.corda.samples.flows.BidFlow$Initiator
net.corda.samples.flows.CreateAssetFlow
net.corda.samples.flows.CreateAuctionFlow$Initiator
net.corda.samples.flows.IssueCashFlow
```

At this point, you are ready for the bootcamp.

## During the bootcamp

During the bootcamp, you will be issued 100 USD to bid on one of the action items.

Here are the commands that you will need:

## Checking that you've been issued the cash

```
run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State
```

## Bidding

```
start BidFlow bidAmount: AMOUNT, auctionId: ID
```

where

* AMOUNT — the amount you want to bid on an auction item.
* ID — the ID of the auction ID. Can be queried with `run vaultQuery contractStateType: net.corda.samples.states.AuctionState`.

Example:

```
start BidFlow bidAmount: 20 USD, auctionId: 774bb8fb-8769-490d-9c88-9687f978d5e3
```

## Checking the existing auction assets

```
run vaultQuery contractStateType: net.corda.samples.states.Asset
```
Look for the `linearId` field in the response.

## Checking the existing auction items and item IDs

```
run vaultQuery contractStateType: net.corda.samples.states.AuctionState
```

Look for the `auctionId` field in the response.

## Running the node explorer

You can also run the node explorer for a visual representation of everything happening on the network. For instructions, see [Node explorer](https://docs.chainstack.com/operations/corda/node-explorer).

In the Node Explorer UI, make sure you provide the path to your `cordapps` directory in **Settings**. Otherwise you won't be able to see the transactions.

## Additional information

If you have any difficulties or have questions, [talk to us on Gitter](https://gitter.im/chainstack/corda-bootcamp).
