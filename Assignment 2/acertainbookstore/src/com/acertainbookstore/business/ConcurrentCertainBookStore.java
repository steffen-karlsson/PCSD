/**
 *
 */
package com.acertainbookstore.business;

import java.util.*;
import java.util.Map.Entry;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * ConcurrentCertainBookStore implements the bookstore and its functionality which is
 * defined in the BookStore
 */
public class ConcurrentCertainBookStore implements BookStore, StockManager {
	private MapLock<Integer, BookStoreBook> bookMap;

    private void readBookMapLock() {
        for (BookStoreBook book : bookMap.values())
            book.readLock();
    }

    private void writeBookMapLock() {
        for (BookStoreBook book : bookMap.values())
            book.writeLock();
    }

    private void readBookMapUnlock() {
        for (BookStoreBook book : bookMap.values())
            book.readUnlock();
    }

	public ConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new MapLock<Integer, BookStoreBook>();
	}

	public void addBooks(Set<StockBook> bookSet)
			throws BookStoreException {

		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check if all are there
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			String bookTitle = book.getTitle();
			String bookAuthor = book.getAuthor();
			int noCopies = book.getNumCopies();
			float bookPrice = book.getPrice();
			if (BookStoreUtility.isInvalidISBN(ISBN)
					|| BookStoreUtility.isEmpty(bookTitle)
					|| BookStoreUtility.isEmpty(bookAuthor)
					|| BookStoreUtility.isInvalidNoCopies(noCopies)
					|| bookPrice < 0.0) {
				throw new BookStoreException(BookStoreConstants.BOOK
						+ book.toString() + BookStoreConstants.INVALID);
			} else if (bookMap.containsKey(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.DUPLICATED);
			}
		}

        bookMap.dirtyWriteLock();
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			bookMap.put(ISBN, new BookStoreBook(book));
		}
        bookMap.dirtyWriteUnlock();
		return;
	}

	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

        ArrayList<BookCopy> books = new ArrayList<BookCopy>(bookCopiesSet);
        Collections.sort(books);

        int ISBN, numCopies;
        for (BookCopy bookCopy : books) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
			if (BookStoreUtility.isInvalidNoCopies(numCopies))
				throw new BookStoreException(BookStoreConstants.NUM_COPIES
						+ numCopies + BookStoreConstants.INVALID);

		}

        List<BookStoreBook> lockedBooks = new ArrayList<BookStoreBook>();

		BookStoreBook book;
		// Update the number of copies
		for (BookCopy bookCopy : books) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
            book = bookMap.get(ISBN);
            book.writeLock();
            lockedBooks.add(book);
			book.addCopies(numCopies);
		}

        for (BookStoreBook locked : lockedBooks)
            locked.writeUnlock();
	}

	public List<StockBook> getBooks() {
        readBookMapLock();

        List<StockBook> listBooks = new ArrayList<StockBook>();
		Collection<BookStoreBook> bookMapValues = bookMap.values();
		for (BookStoreBook book : bookMapValues) {
            listBooks.add(book.immutableStockBook());
		}

        readBookMapUnlock();
		return listBooks;
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

        ArrayList<BookEditorPick> picks = new ArrayList<BookEditorPick>(editorPicks);
        Collections.sort(picks);

		int ISBNVal;
		for (BookEditorPick editorPickArg : picks) {
			ISBNVal = editorPickArg.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
						+ BookStoreConstants.NOT_AVAILABLE);
		}

        List<BookStoreBook> lockedBooks = new ArrayList<BookStoreBook>();

		for (BookEditorPick editorPickArg : picks) {
            BookStoreBook book = bookMap.get(editorPickArg.getISBN());
            book.writeLock();
            lockedBooks.add(book);
			book.setEditorPick(editorPickArg.isEditorPick());
		}

        for (BookStoreBook locked : lockedBooks)
            locked.writeUnlock();
		return;
	}

	public void buyBooks(Set<BookCopy> bookCopiesToBuy)
			throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

        List<BookStoreBook> lockedBooks = new ArrayList<BookStoreBook>();

        try {
            // Check that all ISBNs that we buy are there first.
            int ISBN;
            BookStoreBook book;
            Boolean saleMiss = false;

            ArrayList<BookCopy> books = new ArrayList<BookCopy>(bookCopiesToBuy);
            Collections.sort(books);

            for (BookCopy bookCopyToBuy : books) {
                ISBN = bookCopyToBuy.getISBN();
                if (bookCopyToBuy.getNumCopies() < 0)
                    throw new BookStoreException(BookStoreConstants.NUM_COPIES
                            + bookCopyToBuy.getNumCopies()
                            + BookStoreConstants.INVALID);
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                            + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                            + BookStoreConstants.NOT_AVAILABLE);
                book = bookMap.get(ISBN);
                book.writeLock();
                lockedBooks.add(book);
                if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
                    book.addSaleMiss(); // If we cannot sell the copies of the book
                    // its a miss
                    saleMiss = true;
                }
            }

            // We throw exception now since we want to see how many books in the
            // order incurred misses which is used by books in demand
            if (saleMiss)
                throw new BookStoreException(BookStoreConstants.BOOK
                        + BookStoreConstants.NOT_AVAILABLE);

            // Then make purchase
            for (BookCopy bookCopyToBuy : books) {
                book = bookMap.get(bookCopyToBuy.getISBN());
                book.buyCopies(bookCopyToBuy.getNumCopies());
            }
        } finally {
            for (BookStoreBook locked : lockedBooks)
                locked.writeUnlock();
        }
        return;
	}


	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

        ArrayList<Integer> isbns = new ArrayList<Integer>(isbnSet);
        Collections.sort(isbns);

		for (Integer ISBN : isbns) {
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
		}

		List<StockBook> listBooks = new ArrayList<StockBook>();
        List<BookStoreBook> lockedBooks = new ArrayList<BookStoreBook>();

		for (Integer ISBN : isbns) {
            BookStoreBook book = bookMap.get(ISBN);
            book.readLock();
            lockedBooks.add(book);
			listBooks.add(book.immutableStockBook());
		}

        for (BookStoreBook locked : lockedBooks)
            locked.readUnlock();

		return listBooks;
	}

	public List<Book> getBooks(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

        ArrayList<Integer> isbns = new ArrayList<Integer>(isbnSet);
        Collections.sort(isbns);

		// Check that all ISBNs that we rate are there first.
		for (Integer ISBN : isbns) {
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
		}

        List<Book> listBooks = new ArrayList<Book>();
        List<BookStoreBook> lockedBooks = new ArrayList<BookStoreBook>();

        for (Integer ISBN : isbns) {
            BookStoreBook book = bookMap.get(ISBN);
            book.readLock();
            lockedBooks.add(book);
            listBooks.add(book.immutableStockBook());
        }

        for (BookStoreBook locked : lockedBooks)
            locked.readUnlock();

		return listBooks;
	}

	public List<Book> getEditorPicks(int numBooks)
			throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks
					+ ", but it must be positive");
		}

        readBookMapLock();

		List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
		List<Book> listEditorPicks = new ArrayList<Book>();
		Iterator<Entry<Integer, BookStoreBook>> it = bookMap.entrySet()
				.iterator();
		BookStoreBook book;

		// Get all books that are editor picks
		while (it.hasNext()) {
			Entry<Integer, BookStoreBook> pair = (Entry<Integer, BookStoreBook>) it
					.next();
			book = (BookStoreBook) pair.getValue();
			if (book.isEditorPick()) {
				listAllEditorPicks.add(book);
			}
		}

		// Find numBooks random indices of books that will be picked
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<Integer>();
		int rangePicks = listAllEditorPicks.size();
		if (rangePicks <= numBooks) {
			// We need to add all the books
			for (int i = 0; i < listAllEditorPicks.size(); i++) {
				tobePicked.add(i);
			}
		} else {
			// We need to pick randomly the books that need to be returned
			int randNum;
			while (tobePicked.size() < numBooks) {
				randNum = rand.nextInt(rangePicks);
				tobePicked.add(randNum);
			}
		}

		// Get the numBooks random books
		for (Integer index : tobePicked) {
			book = listAllEditorPicks.get(index);
			listEditorPicks.add(book.immutableBook());
		}

        readBookMapUnlock();
		return listEditorPicks;

	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	@Override
	public List<StockBook> getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public void removeAllBooks() throws BookStoreException {
        writeBookMapLock();
		bookMap.clear();
        // No need to unlock since all objects are removed,
        // and therefore is there locks also deleted
	}

	public void removeBooks(Set<Integer> isbnSet)
			throws BookStoreException {

		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

        ArrayList<Integer> isbns = new ArrayList<Integer>(isbnSet);
        Collections.sort(isbns);

		for (Integer ISBN : isbns) {
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN
						+ BookStoreConstants.NOT_AVAILABLE);
		}

        for (int isbn : isbns) {
            BookStoreBook book = bookMap.get(isbn);
			book.writeLock();
            bookMap.remove(isbn);
            // Since the book is removed is there no need to release the lock,
            // because its also deleted
		}
	}
}
