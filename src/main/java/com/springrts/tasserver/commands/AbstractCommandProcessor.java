package com.springrts.tasserver.commands;


import com.springrts.tasserver.Account;
import com.springrts.tasserver.Client;
import com.springrts.tasserver.Context;
import java.util.List;

/**
 * Utility base class for command processors.
 * @author hoijui
 */
public abstract class AbstractCommandProcessor implements CommandProcessor {

	private static final int ARGS_MIN_NOCHECK = -1;
	private static final int ARGS_MAX_NOCHECK = -1;
	private static final Account.Access ACCES_NOCHECK = null;
	private Context context = null;
	private final String commandName;
	private final int argsMin;
	private final int argsMax;
	private final Account.Access accessMin;

	protected AbstractCommandProcessor(int argsMin, int argsMax, Account.Access accessMin) {

		this.commandName = CommandProcessors.extractCommandName(this.getClass());
		this.argsMin = argsMin;
		this.argsMax = argsMax;
		this.accessMin = accessMin;
	}
	protected AbstractCommandProcessor(int argsMin, int argsMax) {
		this(argsMin, argsMax, ACCES_NOCHECK);
	}
	protected AbstractCommandProcessor(Account.Access accessMin) {
		this(ARGS_MIN_NOCHECK, ARGS_MAX_NOCHECK, accessMin);
	}
	protected AbstractCommandProcessor() {
		this(ARGS_MIN_NOCHECK, ARGS_MAX_NOCHECK);
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}
	protected Context getContext() {
		return context;
	}

	/**
	 * Returns the name of the command supported by this processor.
	 * @see SupportedCommand
	 */
	public String getCommandName() {
		return this.commandName;
	}

	/** Returns the minimum number of arguments supported by the command */
	public int getArgsMin() {
		return this.argsMin;
	}
	/** Returns the maximum number of arguments supported by the command */
	public int getArgsMax() {
		return this.argsMax;
	}

	/**
	 * Returns the minimum access right required to execute the command.
	 * @return minimum access right required to execute, or <code>null</code>.
	 *         if no check should be performed.
	 */
	public Account.Access getAccessMin() {
		return this.accessMin;
	}

	/**
	 * Perform common checks.
	 */
	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException {

		if ((getAccessMin() != ACCES_NOCHECK) &&
				client.getAccount().getAccess().compareTo(getAccessMin()) < 0) {
			throw new CommandProcessingException(
					"Insufficient access rights to execute command \"" +
					getCommandName() + "\"; minimum required is " +
					getAccessMin().toString() + ", but the account has " +
					client.getAccount().getAccess() + ".");
		}

		if ((getArgsMin() != ARGS_MIN_NOCHECK) &&
				(args.size() < getArgsMin())) {
			throw new CommandProcessingException(
					"Too few arguments to command \"" + getCommandName() +
					"\"; minimum is " + getArgsMin() + ", but we recieved " +
					args.size() + ".");
		}
		if ((getArgsMax() != ARGS_MAX_NOCHECK) &&
				(args.size() > getArgsMax())) {
			throw new CommandProcessingException(
					"Too many arguments to command \"" + getCommandName() +
					"\"; maximum is " + getArgsMax() + ", but we recieved " +
					args.size() + ".");
		}

		return true;
	}
}