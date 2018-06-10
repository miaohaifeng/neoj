package neo.Implementations.Wallets;

import java.util.Map;

import neo.Fixed8;
import neo.UInt160;
import neo.Core.SignatureContext;
import neo.Core.Transaction;
import neo.wallet.Account;
import neo.wallet.Coin;
import neo.wallet.CoinException;
import neo.wallet.Contract;

public interface IUserManager {
	
	public void start();
	
	public void close();
	
	public Account createAccount();
	
	public Account createAccount(byte[] privateKey);
	
	public Contract getContract(String address);
	
	public Account[] getAccounts();
	
	public Contract[] getContracts();
	
	public void rebuild();

	public <T extends Transaction> T makeTransaction(T regTx,Fixed8 zero) throws CoinException;
	
	public <T extends Transaction> T makeTransaction(T tx, Fixed8 fee, UInt160 from) throws CoinException;
	
    public boolean saveTransaction(Transaction tx);

    public boolean sign(SignatureContext context);

    public boolean hasFinishedSyncBlock() throws Exception;

	public Account getAccount(UInt160 publicKeyHash);

	public Account getAccountByScriptHash(UInt160 scriptHash);

	public Coin[] findUnspentCoins();
	
	public Coin[] findUnconfirmedCoins();

	public Map<Transaction, Integer> LoadTransactions();
	
	public Coin[] getCoin();
	
	public int getBlockHeight();
	public int getWalletHeight();
	
}
