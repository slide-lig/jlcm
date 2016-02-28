package fr.liglab.jlcm.internals.tidlist;

import gnu.trove.iterator.TIntIterator;

public interface TidList extends Cloneable {

	void addTransaction(int item, int transaction);

	TIntIterable getIterable(int item);

	TIntIterator get(int item);

	TidList clone();

}
