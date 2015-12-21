package neoe.github;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginDialog {

	 

	public static String login() {

		JPanel userPanel = new JPanel();
		userPanel.setLayout(new GridLayout(2, 2));

		JLabel usernameLbl = new JLabel("Username:");
		JLabel passwordLbl = new JLabel("Password:");
		JTextField username = new JTextField();
		JPasswordField passwordFld = new JPasswordField();
		userPanel.add(usernameLbl);
		userPanel.add(username);
		userPanel.add(passwordLbl);
		userPanel.add(passwordFld);
		JPanel userPanel2 = new JPanel();
		userPanel2.setLayout(new BorderLayout());
		userPanel2.add(
				new JLabel("<html>Login to github for up to 5,000 requests per hour instead of 60 requests per hour.<br>"
						+ " see https://developer.github.com/v3/#rate-limiting</html>"),
				BorderLayout.CENTER);
		userPanel2.add(userPanel, BorderLayout.SOUTH);
		int input = JOptionPane.showConfirmDialog(null, userPanel2, "Input your password", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (input == 0) { 
			return username.getText() + ":" + new String(passwordFld.getPassword());
		}
		return null;
	}

}