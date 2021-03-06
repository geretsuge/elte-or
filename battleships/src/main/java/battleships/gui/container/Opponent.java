package battleships.gui.container;

import battleships.gui.Palette;
import battleships.gui.element.ReadyLabel;
import battleships.model.Admiral;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Interactable.Result;

import java.util.stream.Collectors;

public class Opponent extends Panel {

	private GameWindow game;
	private ReadyLabel label;
	private Admiral admiral;
	private Boolean dead = false;

	public Opponent(GameWindow game, Admiral admiral) {
		this.game = game;
		this.admiral = admiral;
		label = new ReadyLabel(game, admiral);
		if (admiral.getSea() == null) {
			admiral.setSea(new Sea(game.getTableSize()));
		}
		admiral.setOpponent(this);
		addComponent(label);
		addComponent(new SeaContainer(admiral.getSea()));
	}

	public GameWindow getGame() {
		return game;
	}

	/**
	 * @return the label
	 */
	public ReadyLabel getLabel() {
		return label;
	}

	public Admiral getAdmiral() {
		return admiral;
	}

	@Override
	public String toString() {
		return getAdmiral().getName() + "\n Ships:\n" + getAdmiral().getSea().getShips().stream().map(Object::toString).collect(Collectors.joining("\n"));
	}

	public void highlight() {
		label.setBackgroundColor(Palette.WATER.getColor());
		getGame().getOpponentBar().setCurrent(this);
		invalidate();
	}

	public void unHighlight() {
		label.setBackgroundColor(Palette.BASE.getColor());
		invalidate();
	}

	public Result takeFocus() {
		if(!dead) {
			highlight();
			getGame().getInspector().inspect(this);
			return getAdmiral().getSea().takeFocus();
		} else {
			return Result.UNHANDLED;
		}
	}

	public Boolean isDead() {
		return dead;
	}

	public void die() {
		dead = true;
		getAdmiral().getSea().setEnabled(false);
		getGame().getOpponentBar().setCurrent(null);
		getGame().getOpponentBar().focusNext();
		getAdmiral().getSea().doTremor(true);
	}
}
