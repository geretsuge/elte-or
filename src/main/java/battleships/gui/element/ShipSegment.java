package battleships.gui.element;

import battleships.gui.Palette;
import battleships.gui.container.Drawer;
import battleships.gui.container.Sea;
import battleships.model.Coord;
import battleships.model.Direction;
import battleships.state.Phase;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class ShipSegment extends AbstractInteractableComponent<ShipSegment> {


	private Ship ship;
	private Boolean destroyed = false;

	private final TextColor highlighted = TextColor.Factory.fromString("#787777");
	private final TextColor held = TextColor.Factory.fromString("#889999");
	private TextColor currentHighlighted = highlighted;
	private TextColor currentHeld = held;
	private TextColor basic = TextColor.Factory.fromString("#555555");
	private TextColor error = TextColor.Factory.fromString("#AA5555");
	private Water water;

	public ShipSegment(Ship ship) {

		this.ship = ship;
	}

	public void damage() {
		this.destroyed = true;
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
		Observable.timer(200, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation()).subscribe(next ->
			ship.getChildren().stream().map(c -> (ShipSegment) c).forEach(s -> {
				s.currentHighlighted = highlighted;
				s.currentHeld = held;
			})
		);
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

	public Result onSeaLeft(Sea sea) {
		if (sea.placementValidFromLeft(ship)) {
			moveShipInDirection(battleships.model.Direction.LEFT);
		} else {
			briefError();
		}
		return Result.HANDLED;
	}

	public Result onSeaDown(Sea sea) {
		if (sea.placementValidFromBottom(ship)) {
			moveShipInDirection(battleships.model.Direction.DOWN);
		} else {
			briefError();
		}
		return Result.HANDLED;
	}

	public Result onSeaRight(Sea sea) {
		if (sea.placementValidFromRight(ship)) {
			moveShipInDirection(battleships.model.Direction.RIGHT);
		} else {
			briefError();
		}
		return Result.HANDLED;
	}

	public Result onSeaUp(Sea sea) {
		if (sea.placementValidFromTop(ship)) {
			moveShipInDirection(battleships.model.Direction.UP);
		} else {
			briefError();
		}
		return Result.HANDLED;
	}

	public Result inDrawerUpLeft(Drawer drawer) {
		if (drawer.isFirstShip(ship)) {
			drawer.focusLastShip();
			return Result.HANDLED;
		} else {
			return Result.MOVE_FOCUS_UP;
		}
	}

	public Result inDrawerDownRight(Drawer drawer) {
		if (drawer.isLastShip(ship)) {
			drawer.focusFirstShip();
			return Result.HANDLED;
		} else {
			return Result.MOVE_FOCUS_DOWN;
		}
	}

	@Override
	public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
		if (isOnSea() && ship.isHeld()) {
			var sea = ((Sea) ship.getParent());

			if (keyStroke.getCharacter() != null) {
				switch (keyStroke.getCharacter()) {
					case 'W':
					case 'w':
						return onSeaUp(sea);
					case 'A':
					case 'a':
						return onSeaLeft(sea);
					case 'S':
					case 's':
						return onSeaDown(sea);
					case 'D':
					case 'd':
						return onSeaRight(sea);
					default:
				}
			}

			switch (keyStroke.getKeyType()) {
				case ArrowDown:
					return onSeaDown(sea);
				case ArrowLeft:
					return onSeaLeft(sea);
				case ArrowRight:
					return onSeaRight(sea);
				case ArrowUp:
					return onSeaUp(sea);
				case Enter:
					if(Phase.PLACEMENT.equals(getShip().getSea().getAdmiral().getPhase())) {
						if (!sea.placementValid(ship)) {
							briefError();
						} else {
							ship.setHeld(false);
							sea.sendRipple(ship);
							// Update all segments, set water/segment relationship
							ship.updateWaterRelations();
							sea.getDrawer().takeFocus();
						}
					}
					return Result.HANDLED;
				case Escape:
					if (sea.equals(ship.getOriginalParent())) {
						ship.resetOriginalPlacement();
					} else {
						ship.doSwitch();
						ship.getBody().forEach(body -> body.setWater(null));
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

			if (keyStroke.getCharacter() != null) {
				switch (keyStroke.getCharacter()) {
					case 'W':
					case 'w':
					case 'A':
					case 'a':
						return Result.MOVE_FOCUS_PREVIOUS;
					case 'S':
					case 's':
					case 'D':
					case 'd':
						return Result.MOVE_FOCUS_NEXT;
					default:
				}
			}

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
					if(Phase.PLACEMENT.equals(getShip().getSea().getAdmiral().getPhase())) {
						ship.savePlacement();
						ship.saveParent();
						ship.setHeld(true);
					} else {
						// Do Inspect
						getShip().getSea().getAdmiral().inspect(getShip());
					}

					return Result.HANDLED;
				default:
			}
		}

		if (isInDrawer()) {
			var drawer = ((Drawer) ship.getParent());
			if (keyStroke.getCharacter() != null) {
			switch (keyStroke.getCharacter()) {
				case 'W':
				case 'w':
				case 'A':
				case 'a':
					return inDrawerUpLeft(drawer);
				case 'S':
				case 's':
				case 'D':
				case 'd':
					return inDrawerDownRight(drawer);
				default:
			}}
			switch (keyStroke.getKeyType()) {
				case ArrowRight:
				case ArrowDown:
					return inDrawerDownRight(drawer);
				case ArrowLeft:
				case ArrowUp:
					return inDrawerUpLeft(drawer);
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
					drawer.getGame().getInspector().inspect(ship);
					return Result.HANDLED;
				default:
			}
		}
		return Result.UNHANDLED;
	}

	public Water getWater() {
		return water;
	}

	public void setWater(Water water) {
		setWater(water, true);
	}

	public void setWater(Water water, Boolean otherSide) {
		if(this.water != null) { // Detach the old if there's one
			this.water.setShipSegment(null);
		}
		if(otherSide && water != null) { // Attach the new if there's one
			water.setShipSegment(this, false);
		}
		this.water = water;
	}

	public TerminalPosition getRelativePosition() {
		return getPosition().withRelative(getParent().getPosition());
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

	public Boolean isDestroyed() {
		return destroyed;
	}

	public void destroy() {
		this.destroyed = true;
		getShip().getSea().sendExplosion(getWater());
		getShip().getSea().sendRipple(getWater(), 400);
	}

	public void reveal() {
		getWater().reveal();

		var reveals = Direction.cornersAndAxis(getShip().getOrientation().equals(com.googlecode.lanterna.gui2.Direction.VERTICAL)); // Reveal these too

		if (getShip().getBody().size() == 1) { // This shipSegment is alone, only reveal the corners
			reveals = Direction.corners();
		}

		reveals.stream()
			.map(dir -> dir.vector)
			.map(Coord::convertToTerminalPosition)
			.forEach(pos -> getWater().getSea().getWaterAt(getWater().getPosition().withRelative(pos)).ifPresent(Water::reveal));

	}

	/**
	 * This is the effectless version, should only used by the ship
	 * @param destroyed
	 */
	void setDestroyed(boolean destroyed) {
		this.destroyed = destroyed;
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
			if (shipSegment.destroyed) {
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