// src/main/java/edu/ezip/ing1/pds/ui/RegisterPanel.java
package edu.ezip.ing1.pds.ui;

import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.services.UserClientService;

import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {
    private final MainFrame frame;
    private final UserClientService userSvc;

    private final JTextField email = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JTextField fullName = new JTextField(22);
    private final JTextField heightCm = new JTextField(8);
    private final JTextField weightKg = new JTextField(8);
    private final JComboBox<String> sex = new JComboBox<>(new String[]{"M","F","O"});
    private final JTextField target = new JTextField(8);

    private final JButton registerBtn = new JButton("Register");
    private final JButton backBtn = new JButton("Back to Login");
    private final JLabel status = new JLabel(" ");

    public RegisterPanel(MainFrame frame, UserClientService userSvc) {
        this.frame = frame;
        this.userSvc = userSvc;

        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        JLabel title = new JLabel("Create Account", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        g.gridx=0; g.gridy=row++; g.gridwidth=2; add(title, g);
        g.gridwidth=1;

        addRow(g, row++, "Email:", email);
        addRow(g, row++, "Password:", password);
        addRow(g, row++, "Full name:", fullName);
        addRow(g, row++, "Height (cm):", heightCm);
        addRow(g, row++, "Weight (kg):", weightKg);
        addRow(g, row++, "Sex:", sex);
        addRow(g, row++, "Daily kcal target:", target);

        g.gridx=0; g.gridy=row; add(registerBtn, g);
        g.gridx=1; add(backBtn, g); row++;

        status.setForeground(Color.GRAY);
        g.gridx=0; g.gridy=row; g.gridwidth=2; add(status, g);

        registerBtn.addActionListener(e -> doRegister());
        backBtn.addActionListener(e -> frame.switchTo("login"));
    }

    private void addRow(GridBagConstraints g, int row, String label, JComponent comp) {
        g.gridx=0; g.gridy=row; add(new JLabel(label), g);
        g.gridx=1; add(comp, g);
    }

    private void doRegister() {
        // basic validation
        if (email.getText().trim().isEmpty() || !email.getText().contains("@")) {
            status.setText("Enter a valid email."); return;
        }
        if (password.getPassword().length == 0) {
            status.setText("Password required."); return;
        }
        if (fullName.getText().trim().isEmpty()) {
            status.setText("Full name required."); return;
        }

        Double h, w; Integer kcal;
        try {
            h = Double.valueOf(heightCm.getText().trim());
            w = Double.valueOf(weightKg.getText().trim());
            kcal = Integer.valueOf(target.getText().trim());
            if (h < 50 || h > 300 || w < 10 || w > 500 || kcal < 1000 || kcal > 5000) {
                status.setText("Check numeric ranges."); return;
            }
        } catch (Exception ex) {
            status.setText("Numbers invalid."); return;
        }

        User u = new User();
        u.setEmail(email.getText().trim());
        u.setPassword(new String(password.getPassword()));
        u.setFullName(fullName.getText().trim());
        u.setHeightCm(h);
        u.setWeightKg(w);
        u.setSex((String)sex.getSelectedItem());
        u.setDailyKcalTarget(kcal);

        setBusy(true);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() {
                try { return userSvc.register(u); }
                catch (Exception ex) { ex.printStackTrace(); return null; }
            }
            @Override protected void done() {
                setBusy(false);
                try {
                    User created = get();
                    if (created == null) {
                        JOptionPane.showMessageDialog(RegisterPanel.this, "Sign up failed.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    frame.onRegisterSuccess(created);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RegisterPanel.this,
                            "Network or server error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setBusy(boolean b) {
        registerBtn.setEnabled(!b);
        backBtn.setEnabled(!b);
        status.setText(b ? "Creating account..." : " ");
    }
}
