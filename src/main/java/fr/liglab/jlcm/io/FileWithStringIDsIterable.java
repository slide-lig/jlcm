package fr.liglab.jlcm.io;

import java.util.Iterator;
import java.util.Map;

import fr.liglab.jlcm.internals.TransactionReader;

/**
 * Parse a dataset as a text file, where each line represents a transaction and contains 
 * single-space-separated item IDs
 */
public final class FileWithStringIDsIterable implements Iterable<TransactionReader> {
	
	private FileWithStringIDsReader previous = null;
	private Map<String, Integer> map = null;
	private final String path;
	
	public FileWithStringIDsIterable(String filename) {
		this.path = filename;
	}
	
	@Override
	public Iterator<TransactionReader> iterator() {
		FileWithStringIDsReader iterator;
		
		if (this.previous == null) {
			iterator = new FileWithStringIDsReader(path);
		} else {
			if (this.map == null) {
				this.map = this.previous.close();
			} else {
				this.previous.close();
			}
			iterator = new FileWithStringIDsReader(path, this.map);
		}
		
		this.previous = iterator;
		
		return iterator;
	}
	
	public Map<String, Integer> getMap() {
		return map;
	}
	
	public void close() {
		if (this.previous != null) {
			this.previous.close();
		}
	}
}
