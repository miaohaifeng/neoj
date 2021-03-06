package neo.Core;

import java.io.IOException;

import neo.IO.BinaryReader;
import neo.IO.BinaryWriter;

/**
 * 存证交易
 * 
 */
public class RecordTransaction extends Transaction {
	public String recordType;
	public byte[] recordData;
	
	public RecordTransaction() {
		super(TransactionType.RecordTransaction);
	}
	
	@Override
	protected void deserializeExclusiveData(BinaryReader reader) throws IOException {
		recordType = reader.readVarString();
		recordData = reader.readVarBytes();
	}
	
	@Override
	protected void serializeExclusiveData(BinaryWriter writer) throws IOException {
		writer.writeVarString(recordType);
		writer.writeVarBytes(recordData);
	}
}
