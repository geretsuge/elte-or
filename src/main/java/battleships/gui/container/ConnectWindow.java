package battleships.gui.container;

import java.util.Arrays;
import java.util.regex.Pattern;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import battleships.Client;
import io.reactivex.schedulers.Schedulers;

public class ConnectWindow extends BasicModal {

	private static final Pattern IP_ADDRESS_PART = Pattern.compile(
			"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])[.]){0,3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])?$");

	private static final String IP_ADDRESS_FULL =
			"^(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})$";

	private Panel connectForm = new Panel();

	private Label hostLabel = new Label("IP Address");
	private TextBox hostBox;
	private Label portLabel = new Label("Port");
	private TextBox portBox;
	private EmptySpace emptySpace = new EmptySpace();
	private Button connectButton;
	Client client;

	public ConnectWindow(Client client) {
		this.client = client;
		setTitle("Connect");
		setHints(Arrays.asList(Window.Hint.MODAL, Window.Hint.CENTERED));

		connectForm.setLayoutManager(new GridLayout(2));
		hostBox = new TextBox(client.getHost()).setValidationPattern(IP_ADDRESS_PART);
		portBox = new TextBox(client.getPort().toString()).setValidationPattern(Pattern.compile("[0-9]{0,4}"));
		connectButton = new Button("Connect", () -> {
			Boolean valid = true;
			if (!portBox.getText().matches("[0-9]{4}")) {
				briefError(portBox);
				valid &= false;
			}
			if (!(!hostBox.getText().isEmpty() && hostBox.getText().matches(IP_ADDRESS_FULL))) {
				briefError(hostBox);
				valid &= false;
			}
			if (valid) {
				connectForm.removeAllComponents();
				connectForm.addComponent(new Label("Connecting"));
				invalidate();
				client.tryConnect(hostBox.getText(), Integer.parseInt(portBox.getText()));
			}
		});

	}


	public void takeFocus() {
		hostBox.takeFocus();
	}


	public void show(MultiWindowTextGUI gui) {

		setComponent(connectForm);
		connectForm.removeAllComponents();
		connectForm.addComponent(new Label("Connecting"));
		client.getConnection().subscribeOn(Schedulers.io()).subscribe(optional -> {
			optional.ifPresentOrElse((conn) -> {
				close();
			}, () -> {
				connectForm.removeAllComponents();
				connectForm.addComponent(hostLabel);
				connectForm.addComponent(hostBox);
				connectForm.addComponent(portLabel);
				connectForm.addComponent(portBox);
				connectForm.addComponent(emptySpace);
				connectForm.addComponent(connectButton);
				hostBox.takeFocus();
			});
		});
		gui.addWindow(this);
		gui.moveToTop(this);
		this.takeFocus();
		client.tryConnect(hostBox.getText(), Integer.parseInt(portBox.getText()));
		waitUntilClosed();


		client.showRegistrationWindow();
	}

}
