package neo.Implementations.Wallets.SQLite;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Core.Block;
import neo.Core.TransactionInput;
import neo.Core.TransactionType;
import neo.IO.Serializable;
import neo.Implementations.Wallets.IUserManager;
import neo.wallet.CoinState;
import neo.wallet.Wallet;

public class UserWallet extends Wallet implements IUserManager {

    protected UserWallet(String path, String password, boolean create) throws BadPaddingException, IllegalBlockSizeException {
        super(path, password, create);
    }

    /**
     * 创建/打开钱包
     */
    public static UserWallet create(String path, String password) {
        UserWallet wallet;
		try {
			wallet = new UserWallet(path, password, true);
		} catch (BadPaddingException | IllegalBlockSizeException ex) {
			throw new RuntimeException(ex);
		}
        wallet.createAccount();		// 创建账户、合约
        return wallet;
    }
    
    public static UserWallet open(String path, String password) {
    	UserWallet wallet;
    	try {
			wallet = new UserWallet(path, password, false);
		} catch (BadPaddingException | IllegalBlockSizeException ex) {
			throw new RuntimeException(ex);
		}
		return wallet;
    }
    
    @Override
    protected void buildDatabase() {
    	File file = new File(dbPath());
    	file.delete();
    }

    /**
     * 创建账户/合约
     */
    @Override
    public neo.wallet.Account createAccount(byte[] privateKey)  {
    	// account
    	neo.wallet.Account account = super.createAccount(privateKey);
        onCreateAccount(account);
        // contract
        addContract(neo.wallet.Contract.createSignatureContract(account.publicKey));
        return account;
    }
    
	private void onCreateAccount(neo.wallet.Account account) {
        byte[] decryptedPrivateKey = new byte[96];
        System.arraycopy(account.publicKey.getEncoded(false), 1, decryptedPrivateKey, 0, 64);
        System.arraycopy(account.privateKey, 0, decryptedPrivateKey, 64, 32);
    	Account entity = new Account();
    	entity.privateKeyEncrypted = encryptPrivateKey(decryptedPrivateKey);
    	entity.publicKeyHash = account.publicKeyHash.toArray();
       
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	ctx.insertOrUpdate(entity);
        }
    	 Arrays.fill(decryptedPrivateKey, (byte)0);
    }
    
    @Override
    public void addContract(neo.wallet.Contract contract) {
        super.addContract(contract);
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	Contract entity = new Contract();
        	entity.scriptHash = contract.scriptHash().toArray();
        	entity.publicKeyHash = contract.publicKeyHash.toArray();
        	entity.rawData = contract.toArray();
        	ctx.insertOrUpdate(entity);
        }
    }

    @Override
    public boolean deleteAccount(UInt160 publicKeyHash) {
        boolean flag = super.deleteAccount(publicKeyHash);
        if (flag) {
            try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
            	ctx.deleteAccount(publicKeyHash);
            }
        }
        return flag;
    }
    
    @Override
    public boolean deleteContract(UInt160 scriptHash) {
        boolean flag = super.deleteContract(scriptHash);
        if (flag) {
            try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
            	ctx.deleteContract(scriptHash);
            }
        }
        return flag;
    }

    @Override
    public neo.wallet.Coin[] findUnspentCoins(UInt256 asset_id, Fixed8 amount) {
    	neo.wallet.Coin[] coins = findUnspentCoins(Arrays.stream(findUnspentCoins()).filter(p -> getContract(p.scriptHash).isStandard()), asset_id, amount);
    	if (coins == null) coins = super.findUnspentCoins(asset_id, amount);
        return coins;
    }

    @Override
    protected neo.wallet.Account[] loadAccounts() {
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	Account[] entities = ctx.getAccounts();
        	neo.wallet.Account[] accounts = new neo.wallet.Account[entities.length];
        	for (int i = 0; i < accounts.length; i++) {
        		byte[] decryptedPrivateKey = decryptPrivateKey(entities[i].privateKeyEncrypted);
        		accounts[i] = new neo.wallet.Account(decryptedPrivateKey);
                Arrays.fill(decryptedPrivateKey, (byte)0);
        	}
        	return accounts;
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
			throw new RuntimeException(ex);
		}
    }
    
    @Override
    public neo.wallet.Contract[] loadContracts() {
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	Contract[] entities = ctx.getContracts();
        	neo.wallet.Contract[] contracts = new neo.wallet.Contract[entities.length];
        	for (int i = 0; i < contracts.length; i++) {
        		contracts[i] = Serializable.from(entities[i].rawData, neo.wallet.Contract.class);
        	}
        	return contracts;
        } catch (InstantiationException | IllegalAccessException ex) {
        	throw new RuntimeException(ex);
		}
    }

    @Override
    protected neo.wallet.Coin[] loadCoins() {
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	Coin[] entities = ctx.getCoins();
        	neo.wallet.Coin[] coins = new neo.wallet.Coin[entities.length];
        	for (int i = 0; i < coins.length; i++) {
        		coins[i] = new neo.wallet.Coin();
        		coins[i].input = new TransactionInput();
        		coins[i].input.prevHash = new UInt256(entities[i].txid);
        		coins[i].input.prevIndex = (short)entities[i].index;
        		coins[i].assetId = new UInt256(entities[i].assetId);
        		coins[i].value = new Fixed8(entities[i].value);
        		coins[i].scriptHash = new UInt160(entities[i].scriptHash);
        		coins[i].setState(CoinState.values()[entities[i].state]);
        	}
        	return coins;
        }
    }

    @Override
    protected byte[] loadStoredData(String name) {
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
            Key entity = ctx.getKey(name);
            if (entity == null) return null;
            return entity.value;
        }
    }

    public Map<neo.Core.Transaction,Integer> LoadTransactions() {
    	Map<neo.Core.Transaction, Integer> txMap = new HashMap<neo.Core.Transaction, Integer>();
    	try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
    		Transaction[] trans  = ctx.getTransaction();
        	for (int i = 0; i < trans.length; i++) {
        		try {
        			byte type = trans[i].type;
        			if(TransactionType.RegisterTransaction.value() == type) {
        				neo.Core.Transaction tx = Serializable.from(trans[i].rawData, neo.Core.RegisterTransaction.class);
        				txMap.put(tx, trans[i].height);
        			} else if(TransactionType.IssueTransaction.value() == type) {
        				neo.Core.Transaction tx = Serializable.from(trans[i].rawData, neo.Core.IssueTransaction.class);
        				txMap.put(tx, trans[i].height);
        			} else if(TransactionType.TransferTransaction.value() == type) {
        				neo.Core.Transaction tx = Serializable.from(trans[i].rawData, neo.Core.TransferTransaction.class);
        				txMap.put(tx, trans[i].height);
        			}
				} catch (Exception e) {
					String errMsg = String.format("Failed to LoadTx,tx.type:%s,height:%s,errMsg:%s",trans[i].type,trans[i].height,e.getMessage());
					throw new RuntimeException(errMsg, e);
				}
        	}
        	return txMap;
    	}
    }
    
    private void onCoinsChanged(WalletDataContext ctx, neo.wallet.Coin[] added, neo.wallet.Coin[] changed, neo.wallet.Coin[] deleted) {
    	ctx.insert(Arrays.stream(added).map(p -> {
    		Coin entity = new Coin();
    		entity.txid = p.input.prevHash.toArray();
    		entity.index = Short.toUnsignedInt(p.input.prevIndex);
    		entity.assetId = p.assetId.toArray();
    		entity.value = p.value.getData();
    		entity.scriptHash = p.scriptHash.toArray();
//    		entity.state = CoinState.Unspent.ordinal();
    		entity.state = p.getState().ordinal();
    		return entity;
    	}).toArray(Coin[]::new));
    	ctx.update(Arrays.stream(changed).map(p -> {
    		Coin entity = new Coin();
    		entity.txid = p.input.prevHash.toArray();
    		entity.index = Short.toUnsignedInt(p.input.prevIndex);
    		entity.state = p.getState().ordinal();
    		return entity;
    	}).toArray(Coin[]::new));
    	ctx.delete(Arrays.stream(deleted).map(p -> {
    		Coin entity = new Coin();
    		entity.txid = p.input.prevHash.toArray();
    		entity.index = Short.toUnsignedInt(p.input.prevIndex);
    		return entity;
    	}).toArray(Coin[]::new));
    }

    @Override
    protected void onProcessNewBlock(Block block, neo.wallet.Coin[] added, neo.wallet.Coin[] changed, neo.wallet.Coin[] deleted) {
    	ArrayList<neo.Core.Transaction> tx_changed = new ArrayList<neo.Core.Transaction>();
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	ctx.beginTransaction();
        	// 更新入库tx
            for (neo.Core.Transaction tx : block.transactions) {
            	if (isWalletTransaction(tx)) {
            		tx_changed.add(tx);
            		Transaction entity = new Transaction();
            		entity.hash = tx.hash().toArray();
            		entity.type = tx.type.value();
            		entity.rawData = tx.toArray();
            		entity.height = block.height;
            		entity.time = new Date(block.timestamp * 1000);
            		ctx.insertOrUpdate(entity);
            	}
            }
            // 更新入库coin
            onCoinsChanged(ctx, added, changed, deleted);
            // 更新入库height
            if (tx_changed.size() > 0 || added.length > 0 || changed.length > 0 || deleted.length > 0) {
            	saveStoredData(ctx, "Height", ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(getWalletHeight()).array());
	            ctx.commit();
            }
        }
    }

    @Override
    protected void onSaveTransaction(neo.Core.Transaction tx, neo.wallet.Coin[] added, neo.wallet.Coin[] changed) {
        Transaction tx_changed = null;
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
        	// 更新入库tx
        	ctx.beginTransaction();
        	if(tx != null) {
	        	tx_changed = new Transaction();
	        	tx_changed.hash = tx.hash().toArray();
	        	tx_changed.type = tx.type.value();
	        	tx_changed.rawData = tx.toArray();
	        	tx_changed.height = -1;
	        	tx_changed.time = new Date(System.currentTimeMillis());
	    		ctx.insertOrUpdate(tx_changed);
        	}
    		// 更新入库coin
            onCoinsChanged(ctx, added, changed, new neo.wallet.Coin[0]);
            ctx.commit();
        }
    }

    @Override
    public void rebuild() {
        synchronized (locker) {
            super.rebuild();
        }
    }

    @Override
    protected void saveStoredData(String name, byte[] value) {
        try (WalletDataContext ctx = new WalletDataContext(dbPath())) {
            saveStoredData(ctx, name, value);
        }
    }

    private static void saveStoredData(WalletDataContext ctx, String name, byte[] value) {
    	Key entity = new Key();
    	entity.name = name;
    	entity.value = value;
    	ctx.insertOrUpdate(entity);
    }
    
    public String getWalletPath() {
    	return dbPath();
    }
    
    public boolean hasFinishedSyncBlock() throws Exception {
    	int bHH = getBlockHeight();
    	int cHH = getWalletHeight();
    	System.out.println("blockHH:"+bHH + "---------------currentHH:"+cHH);
    	return bHH+1 <= cHH;
    }
}
