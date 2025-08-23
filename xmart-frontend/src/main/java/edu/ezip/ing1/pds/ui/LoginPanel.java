package edu.ezip.ing1.pds.ui;;

import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.services.UserClientService;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private final MainFrame frame;
    private final UserClientService userSvc;

    private final JTextField emailField = new JTextField(24);
    private final JPasswordField passField = new JPasswordField(24);
    private final JButton loginBtn = new JButton("Login");
    private final JButton gotoRegisterBtn = new JButton("Create account");
    private final JLabel status = new JLabel(" ");

    public LoginPanel(MainFrame frame, UserClientService userSvc) {
        this.frame = frame;
        this.userSvc = userSvc;

        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        JLabel title = new JLabel("Login", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        g.gridx=0; g.gridy=row++; g.gridwidth=2; add(title, g);
        g.gridwidth=1;

        g.gridx=0; g.gridy=row; add(new JLabel("Email:"), g);
        g.gridx=1; add(emailField, g); row++;

        g.gridx=0; g.gridy=row; add(new JLabel("Password:"), g);
        g.gridx=1; add(passField, g); row++;

        g.gridx=0; g.gridy=row; add(loginBtn, g);
        g.gridx=1; add(gotoRegisterBtn, g); row++;

        status.setForeground(Color.GRAY);
        g.gridx=0; g.gridy=row; g.gridwidth=2; add(status, g);

        loginBtn.addActionListener(e -> doLogin());
        gotoRegisterBtn.addActionListener(e -> frame.switchTo("register"));
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String pass = new String(passField.getPassword());

        if (email.isEmpty() || !email.contains("@")) {
            status.setText("Enter a valid email.");
            return;
        }
        if (pass.isEmpty()) {
            status.setText("Password required.");
            return;
        }

        setBusy(true);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() {
                try { return userSvc.login(email, pass); }
                catch (Exception ex) { ex.printStackTrace(); return null; }
            }
            @Override protected void done() {
                setBusy(false);
                try {
                    User u = get();
                    if (u == null) {
                        JOptionPane.showMessageDialog(LoginPanel.this, "Invalid credentials.", "Login Failed",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    frame.onLoginSuccess(u);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginPanel.this, "Network or server error:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setBusy(boolean b) {
        loginBtn.setEnabled(!b);
        gotoRegisterBtn.setEnabled(!b);
        status.setText(b ? "Signing in..." : " ");
    }
}

