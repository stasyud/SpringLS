/*
 * Created on 2006.9.30
 */

package com.springrts.tasserver;


/**
 * The correct use of ServerNotification is to first create the object and then
 * add message lines to it. Finally, add it to server notifications.
 * @see ServerNotifications#addNotification(ServerNotification)
 *
 * @author Betalord
 */
public class ServerNotification {

	/**
	 * Milliseconds passed since 1st Jan 1970
	 * @see System.currentTimeMillis()
	 */
	private long time = 0;
	private String title;
	private String author;
	/**
	 * This contains either an empty string or it starts with a new-line.
	 */
	private StringBuilder message;

	public ServerNotification(String title) {
		this(title, "$" + Server.getApplicationName());
	}

	public ServerNotification(String title, String author, String firstLine) {

		this.time    = System.currentTimeMillis();
		this.title   = title;
		this.author  = author;
		this.message = new StringBuilder(firstLine);
	}

	public ServerNotification(String title, String author) {
		this(title, author, "");
	}

	public void addLine(String line) {
		message.append(Misc.EOL).append(line);
	}

	/**
	 * This method is thread-safe; or at least it is if not called from multiple
	 * threads with the same Exception object.
	 * It has to be thread-safe, since multiple threads may call it.
	 */
	public void addException(Exception ex) {

		message.append(ex.toString());

		StackTraceElement[] trace = ex.getStackTrace();
		for (int i = 0; i < trace.length; i++) {
			message.append(Misc.EOL).append("\tat ").append(trace[i].toString());
		}
	}

	@Override
	public String toString() {

		StringBuilder str = new StringBuilder();

		str.append(getAuthor()).append(Misc.EOL);
		str.append(getTitle()).append(Misc.EOL);
		str.append(getTime()).append(Misc.EOL);
		str.append(getMessage());

		return str.toString();
	}

	/**
	 * Milliseconds passed since 1st Jan 1970
	 * @see java.lang.System#currentTimeMillis()
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Either an empty string or starts with a new-line.
	 * @return the message
	 */
	public String getMessage() {
		return message.toString();
	}
}
