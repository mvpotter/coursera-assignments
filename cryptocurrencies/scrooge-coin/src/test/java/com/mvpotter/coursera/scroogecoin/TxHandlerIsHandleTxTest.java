package com.mvpotter.coursera.scroogecoin;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;


/**
 * @author mvpotter
 * @since 28/01/17
 */
public class TxHandlerIsHandleTxTest {

    private UTXOPool utxoPool = new UTXOPool();
    private TxHandler txHandler;

    private Transaction initTransaction;

    private KeyPair scroogeKeyPair;
    private KeyPair aliceKeyPair;
    private KeyPair bobKeyPair;

    @Before
    public void before()
            throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException,
                   InvalidKeyException {
        initKeys();
        initTransaction();
        txHandler = new TxHandler(utxoPool);
    }

    private void initKeys() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = new SecureRandom();
        keyGen.initialize(512, random);
        scroogeKeyPair = keyGen.generateKeyPair();
        aliceKeyPair = keyGen.generateKeyPair();
        bobKeyPair = keyGen.generateKeyPair();
    }

    private void initTransaction()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        initTransaction = new Transaction();
        initTransaction.addOutput(10, scroogeKeyPair.getPublic());
        initTransaction.addOutput(5, scroogeKeyPair.getPublic());
        initTransaction.addOutput(0.1, scroogeKeyPair.getPublic());
        initTransaction.finalize();
        for (int i = 0; i < initTransaction.getOutputs().size(); i++) {
            Transaction.Output output = initTransaction.getOutput(i);
            utxoPool.addUTXO(new UTXO(initTransaction.getHash(), i), output);
        }
    }

    @Test
    public void testSingleValidTransaction()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Transaction transaction = createTransaction(initTransaction, 0, 5,
                scroogeKeyPair.getPrivate(),
                aliceKeyPair.getPublic());
        Transaction[] transactions = new Transaction[] { transaction };
        Transaction[] accepted = txHandler.handleTxs(transactions);
        Assert.assertEquals(transaction, accepted[0]);
    }

    public Transaction createTransaction(Transaction inputTransaction, int inputIndex,
                                         double amount, PrivateKey sender, PublicKey recipient)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Transaction transaction = new Transaction();
        transaction.addInput(inputTransaction.getHash(), inputIndex);
        transaction.addOutput(amount, recipient);
        sign(transaction, sender, inputIndex);
        transaction.finalize();
        return transaction;
    }

    private void sign(Transaction transaction, PrivateKey privateKey, int index)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(privateKey);
        rsa.update(transaction.getRawDataToSign(index));
        transaction.addSignature(rsa.sign(), index);
    }

}
