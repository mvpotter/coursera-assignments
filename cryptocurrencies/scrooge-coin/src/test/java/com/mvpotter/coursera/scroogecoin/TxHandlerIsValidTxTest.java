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
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;


/**
 * @author mvpotter
 * @since 28/01/17
 */
public class TxHandlerIsValidTxTest {

    private UTXOPool utxoPool = new UTXOPool();
    private TxHandler txHandler;

    private Transaction initTransaction;
    private Transaction validTransaction;

    private KeyPair scroogeKeyPair;
    private KeyPair aliceKeyPair;

    @Before
    public void before()
            throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException,
                   InvalidKeyException {
        initKeys();
        initTransactions();
        txHandler = new TxHandler(utxoPool);
    }

    private void initKeys() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = new SecureRandom();
        keyGen.initialize(512, random);
        scroogeKeyPair = keyGen.generateKeyPair();
        aliceKeyPair = keyGen.generateKeyPair();
    }

    private void initTransactions()
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

        validTransaction = new Transaction();
        validTransaction.addInput(initTransaction.getHash(), 0);
        sign(validTransaction, scroogeKeyPair.getPrivate(), 0);
        validTransaction.finalize();
    }

    @Test
    public void testInputNotInPull() {
        Transaction transaction = new Transaction(validTransaction);
        transaction.addInput(initTransaction.getHash(), 4);
        Assert.assertFalse(txHandler.isValidTx(transaction));
    }

    @Test
    public void testInputWrongSignature() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Transaction transaction = new Transaction(validTransaction);
        sign(transaction, aliceKeyPair.getPrivate(), 0);
        Assert.assertFalse(txHandler.isValidTx(transaction));
    }

    @Test
    public void testUtxoClaimedMultipleTimes()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Transaction transaction = new Transaction(validTransaction);
        transaction.addInput(initTransaction.getHash(), 0);
        sign(transaction, scroogeKeyPair.getPrivate(), 0);
        Assert.assertFalse(txHandler.isValidTx(transaction));
    }

    @Test
    public void testNegativeValue()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Transaction transaction = new Transaction(validTransaction);
        transaction.addOutput(-1, aliceKeyPair.getPublic());
        sign(transaction, scroogeKeyPair.getPrivate(), 0);
        Assert.assertFalse(txHandler.isValidTx(transaction));
    }

    @Test
    public void testOutputMoreThanInput()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Transaction transaction = new Transaction(validTransaction);
        transaction.addOutput(20, aliceKeyPair.getPublic());
        sign(transaction, scroogeKeyPair.getPrivate(), 0);
        Assert.assertFalse(txHandler.isValidTx(transaction));
    }

    private void sign(Transaction transaction, PrivateKey privateKey, int index)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(privateKey);
        rsa.update(transaction.getRawDataToSign(index));
        transaction.addSignature(rsa.sign(), index);
    }

}
