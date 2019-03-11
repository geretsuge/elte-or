package battleships;

import battleships.model.Admiral;
import battleships.model.Table;
import battleships.net.Connection;
import battleships.state.Phase;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "server", sortOptions = false,
	header = {"", "@|cyan  _____     _   _   _     _____ _   _                                     |@",
		"@|cyan | __  |___| |_| |_| |___|   __| |_|_|___ ___    ___ ___ ___ _ _ ___ ___  |@",
		"@|cyan | __ -| .'|  _|  _| | -_|__   |   | | . |_ -|  |_ -| -_|  _| | | -_|  _| |@",
		"@|cyan |_____|__,|_| |_| |_|___|_____|_|_|_|  _|___|  |___|___|_|  \\_/|___|_|   |@",
		"@|cyan                                     |_|                                 |@"},
	descriptionHeading = "@|bold %nDescription|@:%n", description = {"", "Client application for BattleShips",},
	optionListHeading = "@|bold %nOptions|@:%n", footer = {"", "Author: AlexAegis"})
public class Server implements Runnable {

	@ParentCommand
	private App app;

	@Option(names = {"-p", "--port"}, paramLabel = "<host>", description = "Port of the server", defaultValue = "6668")
	private Integer port;

	public static void main(String[] args) {
		CommandLine.run(new Server(), System.err, args);
	}

	private Table table = new Table();
	private Map<String, Connection> connectedAdmirals = new HashMap<>();
	private Phase phase;
	private String currentAdmiral;

	@Override
	public void run() {
		phase = Phase.PLACEMENT;
		try {
			var server = new ServerSocket(port);
			Flowable.fromCallable(() -> new Connection(this, server))
				.repeat()
				.parallel()
				.runOn(Schedulers.newThread())
				.flatMap(connection -> connection.onTerminateDetach().toFlowable(BackpressureStrategy.BUFFER))
				.sequential()
				.blockingSubscribe(next -> {
					System.out.println("BLOCKSUBBED TO A PACKET");
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the connectedAdmirals
	 */
	public Map<String, Connection> getConnectedAdmirals() {
		return connectedAdmirals;
	}

	/**
	 * @return the phase
	 */
	public Phase getPhase() {
		return phase;
	}

	public Stream<Connection> getEveryOtherConnectedAdmiralsExcept(Admiral... admirals) {
		return getEveryOtherConnectedAdmiralsExcept(Arrays.stream(admirals).map(Admiral::getName).toArray(String[]::new));
	}
	/**
	 * @return the connectedAdmirals
	 */
	public Stream<Connection> getEveryOtherConnectedAdmiralsExcept(String... admirals) {
		return getConnectedAdmirals().entrySet().stream().filter(e -> !Arrays.asList(admirals).contains(e.getKey())).map(Entry::getValue).filter(Objects::nonNull);
	}


	public Stream<Connection> getEveryConnectedAdmirals() {
		return getConnectedAdmirals().entrySet().stream().map(Entry::getValue).filter(Objects::nonNull);
	}

	public Boolean isEveryOneOnTheSamePhase(Phase stage) {
		return getConnectedAdmirals().entrySet().stream()
			.map(Entry::getValue)
			.map(Connection::getAdmiral)
			.filter(Objects::nonNull)
			.map(Admiral::getPhase)
			.allMatch(stage::equals);
	}

	public Boolean isAtLeastNPlayers(int i) {
		return getConnectedAdmirals().entrySet().stream().map(Entry::getValue).filter(Objects::nonNull).count() >= i;
	}


	/**
	 * @return the table
	 */
	public Table getTable() {
		return table;
	}

	public void setPhase(Phase phase) {
		this.phase = phase;
	}

	/**
	 * @param currentAdmiral the currentAdmiral to set
	 */
	public void setCurrentAdmiral(Admiral currentAdmiral) {
		this.currentAdmiral = currentAdmiral.getName();
	}

	public Admiral getCurrentAdmiral() {
		if (currentAdmiral == null) {
			turnAdmirals();
		}
		return getConnectedAdmirals().get(currentAdmiral).getAdmiral();
	}

	public void turnAdmirals() {
		nextAdmiralInTurn().ifPresent(this::setCurrentAdmiral);
	}

	public Optional<Admiral> nextAdmiralInTurn() {
		if (currentAdmiral == null) {
			return Optional.ofNullable(getConnectedAdmirals().get(getConnectedAdmirals().keySet().stream().sorted().collect(Collectors.toList()).get(0)).getAdmiral());
		} else {
			Boolean thisOne = false;
			for (var admi : getConnectedAdmirals().keySet().stream().sorted().collect(Collectors.toList())) {
				if (thisOne) {
					return Optional.ofNullable(getConnectedAdmirals().get(admi).getAdmiral());
				}
				if (admi.equals(currentAdmiral)) {
					thisOne = true;
				}
			}
			return Optional.<Admiral>empty();
		}
	}


}
