package battleships.gui.layout;

import battleships.gui.element.Water;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.AbsoluteLayout;
import com.googlecode.lanterna.gui2.Component;
import java.util.List;
import java.util.stream.Collectors;

public class SeaLayout extends AbsoluteLayout {

	private TerminalSize size;

	public SeaLayout(TerminalSize size) {
		super();
		this.size = size;
	}

	@Override
	public TerminalSize getPreferredSize(List<Component> components) {
		return size;
	}

	@Override
	public void doLayout(TerminalSize area, List<Component> components) {
		int x = 0;
		int y = 0;
		for (var c : components.stream().filter(c -> c instanceof Water).collect(Collectors.toList())) {
			c.setPosition(new TerminalPosition(x, y));
			x++;
			if (x >= size.getColumns()) {
				x = 0;
				y++;
			}
		}
	}

}
