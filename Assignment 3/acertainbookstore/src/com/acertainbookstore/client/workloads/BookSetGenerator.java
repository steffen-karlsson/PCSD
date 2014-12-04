package com.acertainbookstore.client.workloads;

import java.util.*;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

    private Random r = new Random(System.currentTimeMillis());
    private int currentISBN = 2;

	public BookSetGenerator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
        ArrayList<Integer> arrayIsbns = new ArrayList<Integer>(isbns);
        Collections.shuffle(arrayIsbns);
        return new HashSet<Integer>(arrayIsbns.subList(0, num));
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
        Set<StockBook> books = new HashSet<StockBook>();
        for (int i = 0; i<num; i++) {
            int isbn = currentISBN++;
            books.add(new ImmutableStockBook(
                    isbn,
                    "Book " + isbn,
                    "Unknown",
                    42.0f,
                    2 + r.nextInt(100),
                    0,
                    r.nextInt(10),
                    1 + r.nextInt(3),
                    r.nextBoolean()));
        }
		return books;
	}

}
