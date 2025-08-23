package edu.ezip.ing1.pds.ui;

import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.services.UserClientService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final UserClientService userSvc;

    // screens
    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;

    // simple “session”
    private User currentUser;

    public MainFrame(UserClientService userSvc) {
        super("BMI Demo – Login / Sign Up");
        this.userSvc = userSvc;

        this.loginPanel = new LoginPanel(this, userSvc);
        this.registerPanel = new RegisterPanel(this, userSvc);

        root.add(loginPanel, "login");
        root.add(registerPanel, "register");

        setContentPane(root);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        switchTo("login");
    }

    public void switchTo(String name) { cards.show(root, name); }

    public void onLoginSuccess(User u) {
        this.currentUser = u;
        JOptionPane.showMessageDialog(this,
                "Welcome, " + u.getFullName() + "\nBMI: " + u.getBmi() + "\nTarget: " + u.getDailyKcalTarget() + " kcal",
                "Login Success", JOptionPane.INFORMATION_MESSAGE);
        // For now we stay on login; later we’ll switch to Profile/MealPlan.
    }

    public void onRegisterSuccess(User u) {
        this.currentUser = u;
        JOptionPane.showMessageDialog(this,
                "Account created for " + u.getFullName() + "\nBMI: " + u.getBmi(),
                "Sign Up Success", JOptionPane.INFORMATION_MESSAGE);
        switchTo("login"); // back to login after sign‑up
    }
}
