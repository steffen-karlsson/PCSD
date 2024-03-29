/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.*;
import java.util.concurrent.Callable;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {

    private Random r = new Random(System.currentTimeMillis());

	private WorkloadConfiguration configuration = null;
	private int numSuccessfulFrequentBookStoreInteraction = 0;
	private int numTotalFrequentBookStoreInteraction = 0;

	public Worker(WorkloadConfiguration config) {
		configuration = config;
	}

	/**
	 * Run the appropriate interaction while trying to maintain the configured
	 * distributions
	 * 
	 * Updates the counts of total runs and successful runs for customer
	 * interaction
	 * 
	 * @param chooseInteraction
	 * @return
	 */
	private boolean runInteraction(float chooseInteraction) {
		try {
			if (chooseInteraction < configuration
					.getPercentRareStockManagerInteraction()) {
				runRareStockManagerInteraction();
			} else if (chooseInteraction < configuration
					.getPercentFrequentStockManagerInteraction()) {
				runFrequentStockManagerInteraction();
			} else {
				numTotalFrequentBookStoreInteraction++;
				runFrequentBookStoreInteraction();
				numSuccessfulFrequentBookStoreInteraction++;
			}
		} catch (BookStoreException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Run the workloads trying to respect the distributions of the interactions
	 * and return result in the end
	 */
	public WorkerRunResult call() throws Exception {
		int count = 1;
		long startTimeInNanoSecs = 0;
		long endTimeInNanoSecs = 0;
		int successfulInteractions = 0;
		long timeForRunsInNanoSecs = 0;

		Random rand = new Random();
		float chooseInteraction;

		// Perform the warmup runs
		while (count++ <= configuration.getWarmUpRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			runInteraction(chooseInteraction);
		}

		count = 1;
		numTotalFrequentBookStoreInteraction = 0;
		numSuccessfulFrequentBookStoreInteraction = 0;

		// Perform the actual runs
		startTimeInNanoSecs = System.nanoTime();
		while (count++ <= configuration.getNumActualRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			if (runInteraction(chooseInteraction)) {
				successfulInteractions++;
			}
		}
		endTimeInNanoSecs = System.nanoTime();
		timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
		return new WorkerRunResult(successfulInteractions,
				timeForRunsInNanoSecs, configuration.getNumActualRuns(),
				numSuccessfulFrequentBookStoreInteraction,
				numTotalFrequentBookStoreInteraction);
	}

	/**
	 * Runs the new stock acquisition interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runRareStockManagerInteraction() throws BookStoreException {
        StockManager manager = configuration.getStockManager();
        Set<StockBook> allBooks = new HashSet<StockBook>(manager.getBooks());
        Set<StockBook> possibleNewBooks = configuration.getBookSetGenerator().nextSetOfStockBooks(
                r.nextInt(configuration.getNumBooksToAdd()));
        if (possibleNewBooks.removeAll(allBooks)) {
            manager.addBooks(possibleNewBooks);
        }
	}

	/**
	 * Runs the stock replenishment interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentStockManagerInteraction() throws BookStoreException {
        StockManager manager = configuration.getStockManager();
        List<StockBook> allBooks = manager.getBooks();
        Collections.sort(allBooks, new Comparator<StockBook>() {
            @Override
            public int compare(StockBook o1, StockBook o2) {
                return o1.getNumCopies() - o2.getNumCopies();
            }
        });
        Set<BookCopy> newCopies = new HashSet<BookCopy>();
        for (StockBook book : allBooks.subList(0, configuration.getNumBooksWithLeastCopies()))
            newCopies.add(new BookCopy(book.getISBN(), configuration.getNumAddCopies()));
        manager.addCopies(newCopies);
	}

	/**
	 * Runs the customer interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentBookStoreInteraction() throws BookStoreException {
        BookStore bookStore = configuration.getBookStore();
        List<Book> editorBooks = bookStore.getEditorPicks(configuration.getNumEditorPicksToGet());
        Set<Integer> isbns = new HashSet<Integer>();
        for (Book book : editorBooks)
            isbns.add(book.getISBN());
        Set<Integer> isbnsToBuy = configuration.getBookSetGenerator().sampleFromSetOfISBNs(
                isbns, configuration.getNumBooksToBuy());
        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
        for (Integer isbn : isbnsToBuy)
            booksToBuy.add(new BookCopy(isbn, configuration.getNumBookCopiesToBuy()));
        bookStore.buyBooks(booksToBuy);
    }

}
