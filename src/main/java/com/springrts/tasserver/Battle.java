/*
 * Created on 2005.6.19
 */

package com.springrts.tasserver;


import java.util.*;
import org.apache.commons.logging.LogFactory;

/**
 * @author Betalord
 */
public class Battle implements ContextReceiver {

	/** the id that the next battle will have */
	private static int nextId;

	/**
	 * This is used as a null kind of value,
	 * whenever we want to refer to no-battle.
	 */
	public static final int NO_BATTLE_ID = -1;
	/** unique id */
	public int id;
	/** 0 = normal battle, 1 = battle replay */
	public int type;
	/** NAT traversal technique used by the host. Use 0 for none. */
	public int natType;
	/** description set by the founder of the battle */
	public String title;
	/** founder (host) of the battle */
	public Client founder;
	/** clients without the founder (host) */
	private List<Client> clients;
	/** bots added by clients participating in this battle */
	private List<Bot> bots;
	/** name of the map currently selected for this battle */
	public String mapName;
	/** maximum number of players (including bots) that can participate */
	public int maxPlayers;
	/** use restricted() method to find out if battle is password-protected */
	public String password;
	/** IP port number this battle will be hosted on (default is 8452). */
	public int port;
	/** see notes for description */
	public int hashCode;
	/**
	 * If 0, no rank limit is set.
	 * If 1 or higher, only players with this rank (or higher)
	 * can join the battle
	 * (Note: rank index 1 means seconds rank, not the first one,
	 * since you can not limit the game to players of the first rank,
	 * because that means the game is open to all players
	 * and you do not have to limit it in that case)
	 */
	public int rank;
	/** see protocol description for details */
	public int mapHash;
	/** see protocol description for details */
	public String modName;
	/** list of unit-definition names which are not allowed to be built */
	public List<String> disabledUnits;
	/** list of start rectangles */
	public List<StartRect> startRects;
	/**
	 * if the battle is locked, no-one can join it
	 * (until the lock is released by founder)
	 */
	public boolean locked;
	/**  */
	public Map<String, String> scriptTags;
	/**
	 * contains lines of the script file.
	 * This is only used if type == 1.
	 */
	public List<String> replayScript;
	/**
	 * Stores script lines until we receive the SCRIPTEND command.
	 * Then we copy it to the "replayScript" object,
	 * and notify all clients about it.
	 * This is only used if type == 1.
	 */
	public List<String> tempReplayScript;

	private Context context = null;


	@Override
	public void receiveContext(Context context) {

		this.context = context;
		initStartRects();
	}


	public Battle(int type, int natType, Client founder, String password, int port, int maxPlayers, int hashCode, int rank, int mapHash, String mapName, String title, String modName) {
		this.id = nextId++;
		this.type = type;
		this.natType = natType;
		this.title = title;
		this.founder = founder;
		this.clients = new ArrayList<Client>();
		this.bots = new ArrayList<Bot>();
		this.mapName = mapName;
		this.maxPlayers = maxPlayers;
		this.password = password;
		this.port = port;
		this.hashCode = hashCode;
		this.rank = rank;
		this.mapHash = mapHash;
		this.modName = modName;
		this.disabledUnits = new ArrayList<String>();
		this.locked = false; // we assume this by default. Client must make sure it is unlocked.
		this.scriptTags = new HashMap<String, String>();
		this.replayScript = new ArrayList<String>();
		this.tempReplayScript = new ArrayList<String>();
	}

	private void initStartRects() {

		int maxAllyTeams = context.getEngine().getMaxAllyTeams();
		this.startRects = new ArrayList<StartRect>(maxAllyTeams);
		for (int at = 0; at < maxAllyTeams; at++) {
			this.startRects.add(new StartRect());
		}
	}

	/**
	 * Creates BATTLEOPENED command from this battle and returns it as a result
	 * using the external IP.
	 */
	public String createBattleOpenedCommand() {
		return createBattleOpenedCommandEx(false);
	}

	/**
	 * Same as createBattleOpenedCommand() but requires sender to specify
	 * what IP to use (local or external).
	 * @see #createBattleOpenedCommand()
	 */
	public String createBattleOpenedCommandEx(boolean local) {

		String ip;
		if (local) {
			ip = founder.getLocalIP();
		} else {
			ip = founder.getIp();
		}

		return new StringBuilder("BATTLEOPENED ")
				.append(id).append(" ")
				.append(type).append(" ")
				.append(natType).append(" ")
				.append(founder.getAccount().getName()).append(" ")
				.append(ip).append(" ")
				.append(port).append(" ")
				.append(maxPlayers).append(" ")
				.append(Misc.boolToStr(restricted())).append(" ")
				.append(rank).append(" ")
				.append(mapHash).append(" ")
				.append(mapName).append("\t")
				.append(title).append("\t")
				.append(modName).toString();
	}

	/**
	 * Sends series of CLIENTBATTLESTATUS command to client telling him
	 * about the battle stati of all clients in this battle EXCEPT for himself!
	 */
	public void notifyOfBattleStatuses(Client client) {

		for (int i = 0; i < this.clients.size(); i++) {
			if (this.clients.get(i) == client) {
				continue;
			} else {
				client.sendLine(new StringBuilder("CLIENTBATTLESTATUS ")
						.append(this.clients.get(i).getAccount().getName()).append(" ")
						.append(this.clients.get(i).getBattleStatus()).append(" ")
						.append(this.clients.get(i).getTeamColor()).toString());
			}
		}
		if (founder != client) {
			client.sendLine(new StringBuilder("CLIENTBATTLESTATUS ")
					.append(founder.getAccount().getName()).append(" ")
					.append(founder.getBattleStatus()).append(" ")
					.append(founder.getTeamColor()).toString());
		}
	}

	/**
	 * Notifies all clients in the battle (including the client)
	 * about the new battle status of the client.
	 */
	public void notifyClientsOfBattleStatus(Client client) {
		sendToAllClients(new StringBuilder("CLIENTBATTLESTATUS ")
				.append(client.getAccount().getName()).append(" ")
				.append(client.getBattleStatus()).append(" ")
				.append(client.getTeamColor()).toString());
	}

	/**
	 * Sends <code>s</code> to all clients participating in this battle.
	 */
	public void sendToAllClients(String s) {

		for (int i = 0; i < this.clients.size(); i++) {
			clients.get(i).sendLine(s);
		}
		founder.sendLine(s);
	}

	/**
	 * Sends <code>s</code> to all clients participating in this battle
	 * except for the founder.
	 */
	public void sendToAllExceptFounder(String s) {
		for (int i = 0; i < this.clients.size(); i++) {
			clients.get(i).sendLine(s);
		}
	}

	public String clientsToString() {

		if (this.clients.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(clients.get(0).getAccount().getName());
		for (int i = 1; i < this.clients.size(); i++) {
			sb.append(" ").append(clients.get(i).getAccount().getName());
		}
		return sb.toString();
	}

	/**
	 * Returns the number of clients participating in this battle.
	 * @return number of clients participating in this battle
	 */
	public int getClientsSize() {
		return clients.size();
	}

	public Client getClient(int index) {
		try {
			return clients.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public boolean addClient(Client client) {
		return this.clients.add(client);
	}

	public boolean removeClient(Client client) {
		return this.clients.remove(client);
	}

	public boolean restricted() {
		return !password.equals("*");
	}

	public boolean inGame() {
		return founder.getInGameFromStatus();
	}

	/**
	 * Returns number of spectators in this battle. Note that this operation is
	 * not very fast - we have to go through the entire list of clients in this
	 * battle to figure out spectator count.
	 * @return number of spectators in this battle
	 */
	public int spectatorCount() {

		int count = 0;

		for (int i = 0; i < clients.size(); i++) {
			if (Misc.getModeFromBattleStatus(clients.get(i).getBattleStatus()) == 0) {
				count++;
			}
		}
		if (Misc.getModeFromBattleStatus(founder.getBattleStatus()) == 0) {
			count++;
		}

		return count;
	}

	public int nonSpectatorCount() {
		return clients.size() + 1 - spectatorCount();
	}

	public boolean isClientInBattle(Client client) {

		// TODO: possible simplification if equals and hashCode method of Client are good
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i) == client) {
				return true;
			}
		}
		if (founder == client) {
			return true;
		}

		return false;
	}

	public boolean isClientInBattle(String username) {

		// TODO: possible simplification if equals and hashCode method of Client are good
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getAccount().getName().equals(username)) {
				return true;
			}
		}
		if (founder.getAccount().getName().equals(username)) {
			return true;
		}

		return false;
	}

	public Client getClient(String username) {

		// TODO: possible simplification if equals and hashCode method of Client are good
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getAccount().getName().equals(username)) {
				return clients.get(i);
			}
		}
		if (founder.getAccount().getName().equals(username)) {
			return founder;
		}

		return null;
	}

	public void sendDisabledUnitsListToClient(Client client) {

		if (disabledUnits.isEmpty()) {
			// nothing to send
			return ;
		}

		StringBuilder line = new StringBuilder("DISABLEUNITS ");
		line.append(disabledUnits.get(0));
		for (int i = 1; i < disabledUnits.size(); i++) {
			line.append(" ").append(disabledUnits.get(i));
		}

		client.sendLine(line.toString());
	}

	/** Returns number of bots in this battle (size of the bot list) */
	public int getBotsSize() {
		return bots.size();
	}

	/**
	 * Returns Bot object of the specified bot, or <code>null</code>
	 * if the bot does not exist
	 */
	public Bot getBot(String name) {

		for (int i = 0; i < bots.size(); i++) {
			Bot bot = bots.get(i);
			if (bot.getName().equals(name)) {
				return bot;
			}
		}
		return null;
	}

	/** Returns <code>null</code> if index is out of bounds */
	public Bot getBot(int index) {

		Bot bot = null;

		try {
			bot = bots.get(index);
		} catch (IndexOutOfBoundsException e) {
			LogFactory.getLog(TASServer.class).error("Failed fetching a bot by index", e);
			bot = null;
		}

		return bot;
	}

	/** Adds bot to the bot list */
	public void addBot(Bot bot) {
		bots.add(bot);
	}

	/** Removes first bot in the bots list which is owned by the client */
	public boolean removeFirstBotOfClient(Client client) {

		for (int i = 0; i < bots.size(); i++) {
			Bot bot = bots.get(i);
			if (bot.getOwnerName().equals(client.getAccount().getName())) {
				sendToAllClients(new StringBuilder("REMOVEBOT ")
						.append(id).append(" ")
						.append(bot.getName()).toString());
				bots.remove(bot);
				return true;
			}
		}
		return false;
	}

	/** Removes specified bot from the bot list */
	public boolean removeBot(Bot bot) {
		return bots.remove(bot);
	}

	/* Removes all bots owned by client */
	public void removeClientBots(Client client) {
		while (removeFirstBotOfClient(client)) {
			// empty loop body
		}
	}

	public void sendBotListToClient(Client client) {

		for (int i = 0; i < bots.size(); i++) {
			Bot bot = bots.get(i);
			client.sendLine(new StringBuilder("ADDBOT ")
					.append(id).append(" ")
					.append(bot.getName()).append(" ")
					.append(bot.getOwnerName()).append(" ")
					.append(bot.getBattleStatus()).append(" ")
					.append(bot.getTeamColor()).append(" ")
					.append(bot.getSpecifier()).toString());
		}
	}

	public void sendStartRectsListToClient(Client client) {

		for (int i = 0; i < startRects.size(); i++) {
			StartRect curStartRect = startRects.get(i);
			if (curStartRect.enabled) {
				client.sendLine(new StringBuilder("ADDSTARTRECT ")
						.append(i).append(" ")
						.append(curStartRect.left).append(" ")
						.append(curStartRect.top).append(" ")
						.append(curStartRect.right).append(" ")
						.append(curStartRect.bottom).toString());
			}
		}
	}

	public void sendScriptToClient(Client client) {

		client.sendLine("SCRIPTSTART");
		for (int i = 0; i < replayScript.size(); i++) {
			client.sendLine("SCRIPT " + replayScript.get(i));
		}
		client.sendLine("SCRIPTEND");
	}

	public void sendScriptToAllExceptFounder() {

		for (int i = 0; i < clients.size(); i++) {
			sendScriptToClient(clients.get(i));
		}
	}

	/**
	 * Will put together all script tags in a single line
	 * to be taken as an argument to the SETSCRIPTTAGS command.
	 */
	private String joinScriptTags() {

		StringBuilder joined = new StringBuilder();
		for (Map.Entry<String, String> e : scriptTags.entrySet()) {
			if (joined.length() > 0) {
				joined.append("\t");
			}
			joined.append(e.getKey()).append("=").append(e.getValue());
		}
		return joined.toString();
	}

	public void sendScriptTagsToClient(Client client) {

		if (scriptTags.isEmpty()) {
			// nothing to send
			return;
		}
		client.sendLine("SETSCRIPTTAGS " + joinScriptTags());
	}

	public void sendScriptTagsToAll() {

		for (int i = 0; i < clients.size(); i++) {
			sendScriptTagsToClient(clients.get(i));
		}
	}

	/**
	 * Will copy tempReplayScript to replayScript.
	 * This method is called when SCRIPTEND command is received.
	 */
	public void ratifyTempScript() {

		replayScript = new ArrayList<String>(tempReplayScript.size());
		Collections.copy(replayScript, tempReplayScript);
	}
}
