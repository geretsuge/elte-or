package battleships.gui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.graphics.ThemeStyle;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.WindowDecorationRenderer;
import com.googlecode.lanterna.gui2.WindowPostRenderer;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import battleships.model.Admiral;
import battleships.gui.layout.SeaLayout;
import battleships.gui.layout.ShipContainer;
import battleships.gui.layout.WaterContainer;
import battleships.misc.Chainable;
import battleships.model.ShipType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;


public class Sea extends Panel implements Chainable, ShipContainer, WaterContainer {


	private TextColor colorWater = TextColor.Factory.fromString("#5555BB");

	private Drawer drawer;

	private Admiral admiral;
	private TerminalPosition cursor;

	private Integer width = 10;
	private Integer height = 10;

	public Sea(Admiral admiral) {

		setLayoutManager(new SeaLayout(new TerminalSize(width, height)));
		IntStream.range(0, width * height).forEach(i -> addComponent(new Water(this)));
		sendRipple();
		//admiral.getShips() // place ships
	}

	public void sendRipple() {
		this.getLayoutManager().doLayout(getPreferredSize(), (List<Component>) getChildren());

		var wave = ripple(new TerminalPosition(5, 5), 1, 6, Direction.HORIZONTAL);

		getWaters().forEach(water -> {

			if (wave.get(0).contains(water.getPosition())) {
				water.startRipple();
			}
			if (wave.get(1).contains(water.getPosition())) {
				water.startRipple0();
			}
			if (wave.get(2).contains(water.getPosition())) {
				water.startRipple1();
			}
		});

	}

	/**
	 * @return the drawer
	 */
	public Drawer getDrawer() {
		return drawer;
	}

	/**
	 * @param drawer the drawer to set
	 */
	public void setDrawer(Drawer drawer) {
		this.drawer = drawer;
	}

	@Override
	public Panel nextContainer() {
		return getDrawer();
	}



	public static List<TerminalPosition> nthRipple(TerminalPosition anchor, Integer length, Integer count,
			Integer iteration, Direction orientaton) {
		if (iteration < 1 || iteration > count) {
			throw new IllegalArgumentException();
		}
		return ripple(anchor, length, count, orientaton).get(iteration - 1);
	}

	public static List<List<TerminalPosition>> ripple(TerminalPosition anchor, Integer length, Integer count,
			Direction orientaton) {
		var result = new ArrayList<List<TerminalPosition>>();


		for (int c = 1; c < count; c++) {
			var iter = new ArrayList<TerminalPosition>();
			var headpiece = false;
			var tailpiece = false;
			for (int i = 1; i <= length; i++) {
				headpiece = i == 1;
				tailpiece = i == length;

				var vert = orientaton.equals(Direction.VERTICAL);
				var hor = orientaton.equals(Direction.HORIZONTAL);

				var translated = anchor.withRelative(vert ? i : 0, hor ? i : 0);


				if (headpiece) { // O--
					if (hor) {
						iter.add(anchor.withRelative(-c, 0));
					} else {
						iter.add(anchor.withRelative(0, -c));
					}
					for (int t = c; t > 0; t--) {
						if (hor) {
							iter.add(anchor.withRelative(-c, t));
							iter.add(anchor.withRelative(-c, -t));
						} else {
							iter.add(anchor.withRelative(t, -c));
							iter.add(anchor.withRelative(-t, -c));
						}
					}
				}

				if (tailpiece) {
					if (hor) {
						iter.add(anchor.withRelative(c, 0)); // --ox
					} else {
						iter.add(anchor.withRelative(0, c)); // --ox
					}
					for (int t = c; t > 0; t--) {
						if (hor) {
							iter.add(anchor.withRelative(c, t));
							iter.add(anchor.withRelative(c, -t));
						} else {
							iter.add(anchor.withRelative(t, c));
							iter.add(anchor.withRelative(-t, c));
						}
					}
				}
				// -O-

				if (hor) {
					iter.add(anchor.withRelative(0, -c));
				} else {
					iter.add(anchor.withRelative(-c, 0));
				}

				if (hor) {
					iter.add(anchor.withRelative(0, c));
				} else {
					iter.add(anchor.withRelative(c, 0));
				}
				for (int t = c; t > 0; t--) {

					if (hor) {
						iter.add(anchor.withRelative(t, -c)); //    x
						//  -o-
						iter.add(anchor.withRelative(-t, -c));//     x
						// -o-
						iter.add(anchor.withRelative(t, c));
						iter.add(anchor.withRelative(-t, c));
					} else {
						iter.add(anchor.withRelative(-c, t)); //    x
						//  -o-
						iter.add(anchor.withRelative(-c, -t));//     x
						// -o-
						iter.add(anchor.withRelative(c, t));
						iter.add(anchor.withRelative(c, -t));
					}

				}


			}
			result.add(iter);
		}


		return result;

	}
}
