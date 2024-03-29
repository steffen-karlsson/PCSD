/**
 * 
 */
package com.acertainbookstore.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.MasterCertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResponse;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * 
 * MasterBookStoreHTTPMessageHandler implements the message handler class which
 * is invoked to handle messages received by the master book store HTTP server
 * It decodes the HTTP message and invokes the MasterCertainBookStore API
 * 
 */
public class MasterBookStoreHTTPMessageHandler extends AbstractHandler {
	private MasterCertainBookStore myBookStore = null;

	public MasterBookStoreHTTPMessageHandler(MasterCertainBookStore bookStore) {
		myBookStore = bookStore;
	}

	@SuppressWarnings("unchecked")
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
        BookStoreMessageTag messageTag;
		String numBooksString = null;
		int numBooks = -1;
		String requestURI;
		BookStoreResponse bookStoreResponse = null;

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		requestURI = request.getRequestURI();

		// Need to do request multi-plexing
		if (!BookStoreUtility.isEmpty(requestURI)
				&& requestURI.toLowerCase().startsWith("/stock")) {
			messageTag = BookStoreUtility.convertURItoMessageTag(requestURI
					.substring(6)); // the request is from store
			// manager, more
			// sophisticated security
			// features could be added
			// here
		} else {
			messageTag = BookStoreUtility.convertURItoMessageTag(requestURI);
		}
		// the RequestURI before the switch
		if (messageTag == null) {
			System.out.println("Unknown message tag");
		} else {
			switch (messageTag) {
			case REMOVEBOOKS:
				String xml = BookStoreUtility
						.extractPOSTDataFromRequest(request);

				Set<Integer> bookSet = (Set<Integer>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();
				try {

					bookStoreResponse.setResult(myBookStore
							.removeBooks(bookSet));

				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				String listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			case REMOVEALLBOOKS:
                System.out.println("HALLO! REMOVEALLBOOKS");
                xml = BookStoreUtility.extractPOSTDataFromRequest(request);

				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore.removeAllBooks());
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			case ADDBOOKS:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);

				Set<StockBook> newBooks = (Set<StockBook>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore.addBooks(newBooks));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}

				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);

				response.getWriter().println(listBooksxmlString);
				break;

			case ADDCOPIES:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);

				Set<BookCopy> listBookCopies = (Set<BookCopy>) BookStoreUtility
						.deserializeXMLStringToObject(xml);
				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore
							.addCopies(listBookCopies));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			case UPDATEEDITORPICKS:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);
				Set<BookEditorPick> mapEditorPicksValues = (Set<BookEditorPick>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();

				try {
					bookStoreResponse.setResult(myBookStore
							.updateEditorPicks(mapEditorPicksValues));

				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}

				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			case BUYBOOKS:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);
				Set<BookCopy> bookCopiesToBuy = (Set<BookCopy>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				// Make the purchase
				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore
							.buyBooks(bookCopiesToBuy));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			case LISTBOOKS:
				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore.getBooks());
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				response.getWriter().println(
						BookStoreUtility
								.serializeObjectToXMLString(bookStoreResponse));
				break;

			case GETBOOKS:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);
				Set<Integer> isbnSet = (Set<Integer>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore.getBooks(isbnSet));

				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			case GETSTOCKBOOKSBYISBN:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);
				isbnSet = (Set<Integer>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore
							.getBooksByISBN(isbnSet));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				response.getWriter().println(
						BookStoreUtility
								.serializeObjectToXMLString(bookStoreResponse));
				break;

			case EDITORPICKS:
				numBooksString = URLDecoder
						.decode(request
								.getParameter(BookStoreConstants.BOOK_NUM_PARAM),
								"UTF-8");
				bookStoreResponse = new BookStoreResponse();
				try {
					numBooks = BookStoreUtility
							.convertStringToInt(numBooksString);
					bookStoreResponse.setResult(myBookStore
							.getEditorPicks(numBooks));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				listBooksxmlString = BookStoreUtility
						.serializeObjectToXMLString(bookStoreResponse);
				response.getWriter().println(listBooksxmlString);
				break;

			default:
				System.out.println("Unhandled message tag: " + messageTag);
				break;
			}
		}
		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);

	}
}
