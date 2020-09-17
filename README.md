# Blockchain Bootcamp: Using Corda on Chainstack for Decentralized Governance

You will be using the auction CorDapp developed by Ashutosh Meher, an R3 developer advocate.

You can read more about the auction CorDapp in [this blog post](https://www.corda.net/blog/creating-a-sample-auction-house-cordapp-from-scratch-part1/).

## Before the workshop

1. Join the Corda network as described in the email that you should have received.
1. Download the auction CorDapp workflow and contract JAR files from the [release tab](https://github.com/akegaviar/auction-cordapp/releases/tag/1.0).
1. Install the workflow and contract one after the other on your Corda node. For instructions, see [Installing a CorDapp](https://docs.chainstack.com/operations/corda/installing-a-cordapp)
1. Connect to your node as described in [Corda tools](https://docs.chainstack.com/operations/corda/tools). Note that you will need to have either Java 8 installed or Docker as described in the documentation. For the Java version, see [Corda standalone shell](https://docs.chainstack.com/operations/corda/tools#corda-standalone-shell); for the Docker version, see [Chainstack standalone shell](https://docs.chainstack.com/operations/corda/tools#chainstack-standalone-shell).

At this point, you are ready for the bootcamp.

## During the bootcamp

During the bootcamp, you will be issued 100 USD to bid on one of the action items.

Here are the commands that you will need:

Checking that you've been issued the cash:

```
run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State
```

Bidding:

```
start BidFlow bidAmount: AMOUNT, auctionId: LINEARID
```

where

AMOUNT — the amount you want to bid on an auction item.
LINEARID — the ID of the action item.

Example:

```
start BidFlow bidAmount: 20 USD, auctionId: 774bb8fb-8769-490d-9c88-9687f978d5e3
```

Checking the existing auction assets:

```
run vaultQuery contractStateType: net.corda.samples.states.Asset
```

Checking the existing auction items and item IDs:

```
run vaultQuery contractStateType: net.corda.samples.states.AuctionState
```

Look for the `linearId` field in the response.

## Additional information

If you have any difficulties or have questions, [talk to us on Gitter](https://gitter.im/chainstack/Lobby).