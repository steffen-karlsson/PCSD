package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyStockManager;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * SlaveCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedReadOnlyBookStore and ReplicatedReadOnlyStockManager
 * interfaces
 * 
 * This class must also handle replication requests sent by the master
 * 
 */
public class SlaveCertainBookStore implements ReplicatedReadOnlyBookStore,
		ReplicatedReadOnlyStockManager {
	private CertainBookStore bookStore = null;
	private long snapshotId = 0;

	public SlaveCertainBookStore() {
		bookStore = new CertainBookStore();
	}

	public synchronized BookStoreResult getBooks() throws BookStoreException {
		BookStoreResult result = new BookStoreResult(bookStore.getBooks(),
				snapshotId);
		return result;
	}

	public synchronized BookStoreResult getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult getBooks(Set<Integer> ISBNList)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooks(ISBNList), snapshotId);
		return result;
	}

	public synchronized BookStoreResult getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult getEditorPicks(int numBooks)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getEditorPicks(numBooks), snapshotId);
		return result;
	}

    public synchronized void replicateRequest(ReplicationRequest request)
            throws BookStoreException {
        switch (request.getMessageType()) {
            case ADDBOOKS:
                bookStore.addBooks((Set<StockBook>) request.getDataSet());
                break;
            case ADDCOPIES:
                bookStore.addCopies((Set<BookCopy>) request.getDataSet());
                break;
            case UPDATEEDITORPICKS:
                bookStore.updateEditorPicks((Set<BookEditorPick>) request.getDataSet());
                break;
            case REMOVEALLBOOKS:
                bookStore.removeAllBooks();
                break;
            case REMOVEBOOKS:
                bookStore.removeBooks((Set<Integer>) request.getDataSet());
                break;
            default:
                throw new BookStoreException();
        }
    }

	public BookStoreResult getBooksByISBN(Set<Integer> isbns)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooksByISBN(isbns), snapshotId);
		return result;
	}

}
