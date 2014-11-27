package com.acertainbookstore.business;

/**
 * Simple class for updating the number of book copies.
 * 
 */
public class BookCopy implements Comparable<BookCopy> {
	private int ISBN;
	private int numCopies;

	/**
	 * Creates a BookCopy representing numCopies book copies of ISBN
	 * 
	 * @return
	 */
	public BookCopy(int ISBN, int numCopies) {
		this.setISBN(ISBN);
		this.setNumCopies(numCopies);
	}

	/**
	 * Returns the ISBN of the book.
	 * 
	 * @return
	 */
	public int getISBN() {
		return ISBN;
	}

	/**
	 * Returns the number of book copies.
	 * 
	 * @return
	 */
	public int getNumCopies() {
		return numCopies;
	}

	/**
	 * Sets the ISBN of the book.
	 * 
	 * @return
	 */
	public void setISBN(int iSBN) {
		ISBN = iSBN;
	}

	/**
	 * Sets the number of book copies.
	 * 
	 * @return
	 */
	public void setNumCopies(int numCopies) {
		this.numCopies = numCopies;
	}

	public boolean equals(Object obj) {
        return !(obj == null || getClass() != obj.getClass())
                && (this.getISBN() == ((BookCopy) obj).getISBN());
    }

	public int hashCode() {
		return getISBN();
	}

    @Override
    public int compareTo(BookCopy o) {
        if (o == null)
            return -1;
        return this.getISBN() - o.getISBN();
    }
}
