package battleships.gui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import battleships.gui.container.Drawer;
import battleships.gui.container.Sea;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ShipSegment extends AbstractInteractableComponent<ShipSegment> {


	private Ship ship;
	private Boolean damaged = false;

	private final TextColor highlighted = TextColor.Factory.fromString("#787777");
	private TextColor currentHighlighted = highlighted;
	private TextColor basic = TextColor.Factory.fromString("#555555");
	private TextColor error = TextColor.Factory.fromString("#AA5555");

	public ShipSegment(Ship ship) {
		this.ship = ship;
	}

	public void damage() {
		this.damaged = true;
	}



	/**
	 * @return the ship
	 */
	public Ship getShip() {
		return ship;
	}



	public void briefError() {
		ship.getChildren().stream().map(c -> (ShipSegment) c).forEach(s -> {
			s.currentHighlighted = error;
		});
		new Thread(() -> {
			try {
				Thread.sleep(200);
				ship.getChildren().stream().map(c -> (ShipSegment) c).forEach(s -> {
					s.currentHighlighted = highlighted;
				});
				this.invalidate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	public Boolean isInDrawer() {
		return ship.getParent() instanceof Drawer;
	}

	public Boolean isOnSea() {
		return ship.getParent() instanceof Sea;
	}

	public Boolean isTargeting() {
		return false;
	}


	@Override
	public synchronized Result handleKeyStroke(KeyStroke keyStroke) {

		if (isOnSea() && ship.isHeld()) {
			var sea = ((Sea) ship.getParent());
			switch (keyStroke.getKeyType()) {
				case ArrowDown:
					if (sea.placementValidFromBottom(ship)) {
						moveShipInDirection(battleships.model.Direction.DOWN);
					} else {
						briefError();
					}
					return Result.HANDLED;
				case ArrowLeft:
					if (sea.placementValidFromLeft(ship)) {
						moveShipInDirection(battleships.model.Direction.LEFT);
					} else {
						briefError();
					}
					return Result.HANDLED;
				case ArrowRight:
					if (sea.placementValidFromRight(ship)) {
						moveShipInDirection(battleships.model.Direction.RIGHT);
					} else {
						briefError();
					}
					return Result.HANDLED;
				case ArrowUp:
					if (sea.placementValidFromTop(ship)) {
						moveShipInDirection(battleships.model.Direction.UP);
					} else {
						briefError();
					}
					return Result.HANDLED;
				case Enter:
					if (!sea.placementValid(ship)) {
						briefError();
					} else {
						Drawer d = sea.getDrawer();
						ship.setHeld(false);
						sea.sendRipple(ship);
						if (d.getShips().size() > 0) {
							d.takeFocus();
						} else {
							// TODO: Finished placement
						}
					}
					return Result.HANDLED;
				case Escape:
					if (sea.equals(ship.getOriginalParent())) {
						ship.setPosition(ship.getOriginalPosition());
					} else {
						ship.doSwitch();
					}
					ship.setOriginalPosition(null);
					ship.setOriginalParent(null);
					ship.setHeld(false);
				default:
			}
			switch (keyStroke.getCharacter()) {
				case ' ':
					ship.changeOrientation();
					return Result.HANDLED;
				default:
			}
		}

		// When the cursor is on the sea and not holding any items and when in placement mode
		if (isOnSea() && !ship.isHeld()) {
			switch (keyStroke.getKeyType()) {
				case ArrowDown:
					return Result.MOVE_FOCUS_DOWN;
				case ArrowLeft:
					return Result.MOVE_FOCUS_LEFT;
				case ArrowRight:
					return Result.MOVE_FOCUS_RIGHT;
				case ArrowUp:
					return Result.MOVE_FOCUS_UP;
				case Tab:
					return Result.MOVE_FOCUS_NEXT;
				case ReverseTab:
					return Result.MOVE_FOCUS_PREVIOUS;
				case Enter:
					ship.setHeld(true);

					return Result.HANDLED;
				default:
			}
		}

		if (isInDrawer()) {
			switch (keyStroke.getKeyType()) {
				case ArrowDown:
					return Result.MOVE_FOCUS_DOWN;
				case ArrowUp:
					return Result.MOVE_FOCUS_UP;
				case Enter:
					ship.setHeld(true);
					ship.setOriginalParent(ship.getParent());
					ship.setOriginalPosition(ship.getPosition());
					ship.doSwitch();
					return Result.HANDLED;
				default:
			}
		}
		return Result.UNHANDLED;
	}

	public void moveShipInDirection(battleships.model.Direction direction) {
		ship.setPosition(new TerminalPosition(ship.getPosition().getColumn() + direction.vector.getX(),
				ship.getPosition().getRow() - direction.vector.getY()));
	}

	/**
	 * Alternative button renderer that displays buttons with just the label and minimal decoration
	 */
	public static class ShipRenderer implements InteractableRenderer<ShipSegment> {

		@Override
		public TerminalPosition getCursorLocation(ShipSegment component) {
			return null;
		}


		@Override
		public TerminalSize getPreferredSize(ShipSegment component) {
			return new TerminalSize(1, 1);
		}


		@Override
		public void drawComponent(TextGUIGraphics graphics, ShipSegment shipSegment) {
			//if (!ship.isTargeting()) {

			//} else {

			graphics.setForegroundColor(Palette.SHIP_FORE);

			if (shipSegment.ship.getHead().isFocused()) {
				graphics.setBackgroundColor(shipSegment.currentHighlighted);
			} else {
				graphics.setBackgroundColor(Palette.SHIP_BACK);
			}

			if (shipSegment.damaged) {
				graphics.fill('▒');
			} else {
				graphics.fill(' ');
			}


			//}


		}

	}

	@Override
	protected InteractableRenderer<ShipSegment> createDefaultRenderer() {
		return new ShipRenderer();
	}

}
