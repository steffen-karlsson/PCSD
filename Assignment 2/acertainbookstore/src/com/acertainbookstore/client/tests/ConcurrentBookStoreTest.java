package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import com.acertainbookstore.business.*;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by thorbjorn on 11/27/14.
 */
public class ConcurrentBookStoreTest {

    private static final int TEST_ISBN = 3044560;
    private static final int NUM_COPIES = 10000;
    private final static StockManager storeManager =  new ConcurrentCertainBookStore();
    private static final int TEST_ISBN2 = 1312314;
    private int i = 0;


    @BeforeClass
    public static void setUpBeforeClass() {
    }

    public StockBook getDefaultBook() {
        return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit",
                "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0, false);
    }

    /**
     * Helper method to add some books
     */
    public void addBooks(int isbn, int copies) throws BookStoreException {
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
        StockBook book = new ImmutableStockBook(isbn, "Test of Thrones",
                "George RR Testin'", (float) 10, copies, 0, 0, 0, false);
        booksToAdd.add(book);
        storeManager.addBooks(booksToAdd);
    }

    /**
     * Method to add a book, executed before every test case is run
     */
    @Before
    public void initializeBooks() throws BookStoreException {
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
        booksToAdd.add(getDefaultBook());
        booksToAdd.add(new ImmutableStockBook(TEST_ISBN2,"Hello Word","Mig", (float) 10, NUM_COPIES, 0,0,0, false));
        storeManager.addBooks(booksToAdd);

        i = 0;
    }

    /**
     * Method to clean up the book store, execute after every test case is run
     */
    @After
    public void cleanupBooks() throws BookStoreException {
        storeManager.removeAllBooks();
    }

    @Test
    public void test1() throws InterruptedException {
        int w = 500;
        while (w-- > 0) {
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    BookStore client = (BookStore) storeManager;
                    int i = 5000;
                    while (i-- > 0) {
                        Set<BookCopy> copies = new HashSet<BookCopy>();
                        copies.add(new BookCopy(TEST_ISBN, 1));
                        try {
                            client.buyBooks(copies);
                        } catch (Exception e) {
                            //
                        }
                    }
                }
            });

            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    StockManager manager = storeManager;
                    int i = 5000;
                    while (i-- > 0) {
                        Set<BookCopy> copies = new HashSet<BookCopy>();
                        copies.add(new BookCopy(TEST_ISBN, 1));
                        try {
                            storeManager.addCopies(copies);
                        } catch (Exception e) {
                            //
                        }
                    }
                }
            });

            t1.start();
            t2.start();

            t1.join();
            t2.join();

            Set<Integer> isbs = new HashSet<Integer>();
            isbs.add(TEST_ISBN);
            StockBook b = null;
            try {
                b = storeManager.getBooksByISBN(isbs).get(0);
            } catch (Exception e) {
                fail();
            }

            assertEquals(NUM_COPIES, b.getNumCopies());
        }
    }

    @Test
    public void test2() throws Exception {

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                BookStore client = (BookStore) storeManager;
                int i = 1000;
                Set<BookCopy> copies = new HashSet<BookCopy>();
                copies.add(new BookCopy(TEST_ISBN, 10));
                copies.add(new BookCopy(TEST_ISBN2, 10));
                while(i-- > 0) {
                    if ((new Random()).nextInt(2) == 1) {
                        try {
                            client.buyBooks(copies);
                        } catch (Exception e) {
                            //
                        }
                    } else {
                        try {
                            ((StockManager) client).addCopies(copies);
                        } catch (Exception e) {
                            //
                        }
                    }
                }
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 1000;
                while(i-- > 0) {
                    try {
                        Set<Integer> isbs = new HashSet<Integer>();
                        isbs.add(TEST_ISBN);
                        isbs.add(TEST_ISBN2);
                        List<StockBook> books = storeManager.getBooksByISBN(isbs);
                        assertEquals(2, books.size());
                        StockBook b1 = books.get(0);
                        StockBook b2 = books.get(1);
                        assertEquals(b1.getNumCopies(), b2.getNumCopies());
                    } catch (BookStoreException e) {
                        // just ignore if the other thread hasn't put the book into the store.
                    } catch (NullPointerException e) {
                        //
                    }
                }
            }
        });

        Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                System.out.println("Uncaught exception: " + ex);
                i++;
            }
        };
        t2.setUncaughtExceptionHandler(h);
        t1.setUncaughtExceptionHandler(h);
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertEquals(0,i);
    }

    @Test
    public void test3() throws InterruptedException, BookStoreException {
        int q = 100;
        while (q-- > 0) {
            storeManager.removeAllBooks();
            i = 10;
            List<Thread> threads = new ArrayList<Thread>();
            while (i-- > 0) {
                threads.add(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StockManager client = storeManager;
                        int j = 10;
                        while (j-- > 0) {
                            StockBook book = new ImmutableStockBook((j * 1000 + (int) Thread.currentThread().getId()), "Hello Word", "Mig", (float) 10, NUM_COPIES, 0, 0, 0, false);
                            Set<StockBook> books = new HashSet<StockBook>();
                            books.add(book);
                            try {
                                client.addBooks(books);
                            } catch (BookStoreException e) {
                                //
                            }
                        }
                    }
                }));
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            assertEquals(threads.size() * 10, storeManager.getBooks().size());
        }
    }

    @Test
    public void test4() throws BookStoreException, InterruptedException {
        int q = 100;
        while (q-- > 0) {
            storeManager.removeAllBooks();
            Set<StockBook> books = new HashSet<StockBook>();
            for(int i = 0; i < 10 ; i++) {
                StockBook book = new ImmutableStockBook(i + 100, "Hello Word", "Mig", (float) 10, NUM_COPIES, 0, 0, 0, false);
                books.add(book);
            }
            storeManager.addBooks(books);
            
            i = 10;
            List<Thread> threads = new ArrayList<Thread>();
            while (i-- > 0) {
                threads.add(new Thread(new BookRunnable(i + 100)));
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            List<StockBook> bookss = storeManager.getBooks();

            for(StockBook b : bookss){
                assertEquals(10 + NUM_COPIES,b.getNumCopies());
            }
        }
    }
    
    class BookRunnable implements Runnable {

        private final int i;

        public BookRunnable(int i){
            this.i = i;
        }

        @Override
        public void run() {
            int j = 10;
            while (j-- > 0) {
                Set<BookCopy> ints = new HashSet<BookCopy>();
                ints.add(new BookCopy(100 + ((i + j) % 10),1));
                try {
                    storeManager.addCopies(ints);
                } catch (BookStoreException e) {
                   // e.printStackTrace();
                }
            }
        }
    }

}
