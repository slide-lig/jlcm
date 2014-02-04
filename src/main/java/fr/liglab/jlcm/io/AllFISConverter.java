package fr.liglab.jlcm.io;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fr.liglab.jlcm.internals.ExplorationStep;

public final class AllFISConverter implements PatternsCollector {
	public final PatternsWriter wrapped;
	
	public AllFISConverter(PatternsWriter destination) {
		this.wrapped = destination;
	}
	
	/**
	 *  invoked only once per *closed* frequent itemset, so we have to re-generate intermediate 
	 *  itemsets
	 */
	public void collect(ExplorationStep state) {
		final int[] closed = state.pattern;
		final int[] tmp = new int[closed.length];
		final int supportCount = state.counters.transactionsCount;
		
		final LinkedList<Integer> extensions = new LinkedList<Integer>();
		final int newItemsLength = state.counters.closure.length;
		
		if (newItemsLength < closed.length) {
			
			extensions.add(closed[newItemsLength]);
			
			for (int i = 0; i < newItemsLength; i++) {
				// note that the following line uses internal item IDs
				if (state.parent.getFailedFPTest(state.counters.closure[i]) == state.core_item) {
					// but this line uses original item IDs
					extensions.add(closed[i]);
					// the trick is that they have the same indexes in both arrays
				}
			}
			
			enumerateExtensionSets(extensions, 0, closed, tmp, 0, supportCount);
		} else {
			enumerateBasePatternSets(0, closed, tmp, 0, extensions, supportCount);
		}
	}
	
	private void enumerateExtensionSets(List<Integer> extensions, int i, final int[] closed, 
			int[] buffer, int buflen, final int support) {
		
		if (i < extensions.size()) {
			buffer[buflen] = extensions.get(i);
			this.wrapped.collect(support, buffer, buflen+1);
			enumerateBasePatternSets(0, closed, buffer, buflen+1, extensions, support);
			
			enumerateExtensionSets(extensions, i+1, closed, buffer, buflen+1, support);
			enumerateExtensionSets(extensions, i+1, closed, buffer, buflen, support);
		}
	}
	
	private void enumerateBasePatternSets(int i, final int[] closed, int[] buffer, int buflen, 
			Collection<Integer> ignore, final int support) {
		
		if (i < closed.length) {
			if (ignore.contains(closed[i])) {
				enumerateBasePatternSets(i+1, closed, buffer, buflen, ignore, support);
			} else {
				buffer[buflen] = closed[i];
				this.wrapped.collect(support, buffer, buflen+1);
				enumerateBasePatternSets(i+1, closed, buffer, buflen+1, ignore, support);
				enumerateBasePatternSets(i+1, closed, buffer, buflen, ignore, support);
			}
		}
	}
	
	public long close() {
		return this.wrapped.close();
	}

	public int getAveragePatternLength() {
		return this.wrapped.getAveragePatternLength();
	}

}
