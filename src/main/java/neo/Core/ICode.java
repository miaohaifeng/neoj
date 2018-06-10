package neo.Core;

import neo.wallet.ContractParameterType;

public interface ICode {
	public byte[] getCode(); 
	public ContractParameterType[] getParameterList();
	public ContractParameterType[] getReturnType();
}
