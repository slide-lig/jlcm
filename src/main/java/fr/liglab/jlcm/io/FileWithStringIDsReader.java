package fr.liglab.jlcm.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fr.liglab.jlcm.internals.TransactionReader;

/**
 * Parse a dataset as a text file, where each line represents a transaction and contains 
 * single-space-separated item IDs
 */
public final class FileWithStringIDsReader implements Iterator<TransactionReader> {

	private FileReader fileReader;
	private BufferedReader reader;
	private Map<String,Integer> mapping; 
	private int itemId = 0;
	private String nextLine;
	private LineReader translator = new LineReader();
	
	public FileWithStringIDsReader(String filename) {
		this(filename, null);
	}
	
	public FileWithStringIDsReader(String filename, Map<String,Integer> reuseMapping) {
		try {
			if (reuseMapping == null) {
				this.mapping = new HashMap<String, Integer>();
			} else {
				this.mapping = reuseMapping;
			}
			
			this.fileReader = new FileReader(new File(filename));
			this.reader = new BufferedReader(fileReader);
			this.nextLine = this.reader.readLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Close the input objects
	 * @return the Map(file's item ID, internal integer ID)
	 */
	public Map<String,Integer> close() {
		try {
			this.reader.close();
			this.fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.mapping;
	}
	
	@Override
	public boolean hasNext() {
		return this.nextLine != null;
	}

	@Override
	public TransactionReader next() {
		this.translator.reset(this.nextLine);
		
		try {
			this.nextLine = this.reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return this.translator;
	}

	@Override
	public void remove() {
		
	}
	
	
	private final class LineReader implements TransactionReader {
		private String[] splitted;
		private int i;
		
		void reset(String line){
			this.splitted = line.split(" ");
			i = 0;
		}
		
		@Override
		public int getTransactionSupport() {
			return 1;
		}

		@Override
		public int next() {
			String id = this.splitted[i++];
			Integer intId = mapping.get(id);
			if (intId == null) {
				intId = itemId++;
				mapping.put(id, intId);
			}
			return intId;
		}

		@Override
		public boolean hasNext() {
			return this.i < this.splitted.length;
		}
		
	}
}
