package fr.liglab.jlcm.internals.transactions;

public interface TransactionsList extends Iterable<IterableTransaction>, Cloneable {

	void startWriting();

	int beginTransaction(int transactionSupport);

	void addItem(int item);

	void compress(int coreItem);

	/**
	 * @return how many distinct transactions are stored
	 */
	int size();

	TransactionIterator getIterator();

	public TransactionsList clone();
}
