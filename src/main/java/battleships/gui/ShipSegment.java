package battleships.gui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.AbsoluteLayout;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.LayoutManager;
import com.googlecode.lanterna.gui2.LinearLayout;
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
	private final TextColor held = TextColor.Factory.fromString("#889999");
	private TextColor currentHighlighted = highlighted;
	private TextColor currentHeld = held;
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
			s.currentHeld = error;
		});
		new Thread(() -> {
			try {
				Thread.sleep(200);
				ship.getChildren().stream().map(c -> (ShipSegment) c).forEach(s -> {
					s.currentHighlighted = highlighted;
					s.currentHeld = held;
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
						ship.setHeld(false);
						sea.sendRipple(ship);
						sea.getDrawer().takeFocus();
					}
					return Result.HANDLED;
				case Escape:
					if (sea.equals(ship.getOriginalParent())) {
						ship.resetOriginalPlacement();
					} else {
						ship.doSwitch();
					}
					ship.setOriginalPosition(null);
					ship.setOriginalParent(null);
					ship.setHeld(false);
					sea.getDrawer().takeFocus();
					return Result.HANDLED;
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
			var sea = ((Sea) ship.getParent());
			switch (keyStroke.getKeyType()) {
				case ArrowDown:
				case ArrowRight:
					return Result.MOVE_FOCUS_NEXT;
				case ArrowUp:
				case ArrowLeft:
					return Result.MOVE_FOCUS_PREVIOUS;
				case Tab:
					sea.getDrawer().getGame().getActionBar().takeFocus();
					return Result.HANDLED;
				case ReverseTab:
				case Escape:
					sea.getDrawer().takeFocus(true);
					return Result.HANDLED;
				case Enter:
					ship.savePlacement();
					ship.saveParent();
					ship.setHeld(true);
					return Result.HANDLED;
				default:
			}
		}

		if (isInDrawer()) {
			var drawer = ((Drawer) ship.getParent());
			switch (keyStroke.getKeyType()) {
				case ArrowDown:
					if (drawer.isLastShip(ship)) {
						drawer.focusFirstShip();
						return Result.HANDLED;
					} else {
						return Result.MOVE_FOCUS_DOWN;
					}
				case ArrowUp:
					if (drawer.isFirstShip(ship)) {
						drawer.focusLastShip();
						return Result.HANDLED;
					} else {
						return Result.MOVE_FOCUS_UP;
					}
				case Tab:
					drawer.getSea().takeFocus();
					return Result.HANDLED;
				case ReverseTab:
					drawer.getGame().getActionBar().takeFocus(true);
					return Result.HANDLED;
				case Enter:
					ship.setHeld(true);

					ship.savePlacement();
					ship.saveParent();
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

	@Override
	protected void afterEnterFocus(FocusChangeDirection direction, Interactable previouslyInFocus) {
		/*if (ship.isHeld() && ship.getParent() instanceof Drawer) {
			previouslyInFocus.takeFocus();
		}*/
		super.afterEnterFocus(direction, previouslyInFocus);
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
				if (shipSegment.ship.isHeld()) {
					graphics.setBackgroundColor(shipSegment.currentHeld);
				} else {
					graphics.setBackgroundColor(shipSegment.currentHighlighted);
				}

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
