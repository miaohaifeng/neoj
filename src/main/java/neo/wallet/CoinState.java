package neo.wallet;

public enum CoinState {
    Unconfirmed,
    Unspent,
    Spending,
    Spent,
    SpentAndClaimed
}