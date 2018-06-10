package neo.Implementations.Wallets;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import neo.Core.Block;
import neo.Core.Transaction;
import neo.wallet.Account;
import neo.wallet.Coin;
import neo.wallet.Contract;
import neo.wallet.Wallet;


public class AbstractWallet extends Wallet {

	protected AbstractWallet(String path, String password, boolean create)
			throws BadPaddingException, IllegalBlockSizeException {
		super(path, password, create);
	}

	@Override
	protected Account[] loadAccounts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Contract[] loadContracts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Coin[] loadCoins() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected byte[] loadStoredData(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void saveStoredData(String name, byte[] value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onProcessNewBlock(Block block, Coin[] added, Coin[] changed,
			Coin[] deleted) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onSaveTransaction(Transaction tx, Coin[] added,
			Coin[] changed) {
		// TODO Auto-generated method stub
		
	}

}
