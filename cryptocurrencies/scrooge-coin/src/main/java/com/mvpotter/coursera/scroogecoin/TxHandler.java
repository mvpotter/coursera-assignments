package com.mvpotter.coursera.scroogecoin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        try {
            double inputSum = calculateInputSum(tx);
            double outputSum = calculateOutputSum(tx);
            return inputSum >= outputSum;
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
            return false;
        }

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Result result = processTransactions(utxoPool, possibleTxs);
        utxoPool = new UTXOPool(result.utxoPool);
        List<Transaction> acceptedTransactions = result.getAcceptedTransactions();
        return acceptedTransactions.toArray(new Transaction[acceptedTransactions.size()]);
    }

    public Result processTransactions(UTXOPool utxoPool, Transaction[] possibleTxs) {
        List<Transaction> transactions = new LinkedList<Transaction>(Arrays.asList(possibleTxs));
        List<Transaction> acceptedTransactions = new LinkedList<Transaction>();
        int transactionsSize = transactions.size() + 1;
        while (transactionsSize != transactions.size()) {
            transactionsSize = transactions.size();
            Iterator<Transaction> iterator = transactions.iterator();
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                if (isValidTx(transaction)) {
                    updateUTXOPool(utxoPool, transaction);
                    iterator.remove();
                    acceptedTransactions.add(transaction);
                }
            }
        }
        return new Result(acceptedTransactions, utxoPool);
    }

    private void updateUTXOPool(UTXOPool utxoPool, Transaction transaction) {
        for (Transaction.Input input: transaction.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            utxoPool.removeUTXO(utxo);
        }
        for(int i = 0; i < transaction.getOutputs().size(); i++) {
            UTXO newUTXO = new UTXO(transaction.getHash(), i);
            utxoPool.addUTXO(newUTXO, transaction.getOutputs().get(i));
        }
    }

    private double calculateInputSum(Transaction tx) {
        Set<UTXO> visitedUtxos = new HashSet<UTXO>();
        double inputSum = 0;
        for (int i = 0; i < tx.getInputs().size(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!this.utxoPool.contains(utxo)) {
                throw new ValidationException("No UTXO found for item " + input.outputIndex);
            }
            if (visitedUtxos.contains(utxo)) {
                throw new ValidationException("UTXO is duplicated for item " + input.outputIndex);
            }
            if(!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(i), input.signature)) {
                throw new ValidationException("Signature is invalid for item " + input.outputIndex);
            }
            visitedUtxos.add(utxo);
            inputSum += utxoPool.getTxOutput(utxo).value;
        }

        return inputSum;
    }

    private double calculateOutputSum(Transaction tx) {
        double outputSum = 0;
        for (int i = 0; i < tx.getOutputs().size(); i++) {
            Transaction.Output output = tx.getOutput(i);
            if (output.value <= 0) {
                throw new ValidationException("Value is negative for item " + i);
            }
            outputSum += output.value;
        }
        return outputSum;
    }

    private static class Result {
        private List<Transaction> acceptedTransactions;
        private UTXOPool utxoPool;

        public Result(List<Transaction> acceptedTransactions, UTXOPool utxoPool) {
            this.acceptedTransactions = acceptedTransactions;
            this.utxoPool = utxoPool;
        }

        public List<Transaction> getAcceptedTransactions() {
            return acceptedTransactions;
        }

        public UTXOPool getUtxoPool() {
            return utxoPool;
        }
    }

    private static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

}
