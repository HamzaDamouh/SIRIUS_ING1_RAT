package edu.ezip.ing1.pds.ui;

import edu.ezip.ing1.pds.business.dto.User;

import javax.swing.*;
import java.awt.*;

public class AuthDialogs {

    // ---- Login ----
    public static class LoginResult {
        public final boolean cancelled;
        public final boolean goRegister;
        public final String email;
        public final String password;
        public LoginResult(boolean cancelled, boolean goRegister, String email, String password) {
            this.cancelled = cancelled; this.goRegister = goRegister; this.email = email; this.password = password;
        }
    }

    public static LoginResult showLoginDialog() {
        JTextField email = new JTextField(20);
        JPasswordField pass = new JPasswordField(20);

        Object[] message = {
                label("Connexion"),
                new JLabel("Email :"), email,
                new JLabel("Mot de passe :"), pass
        };

        Object[] options = {"Se connecter", "Créer un compte", "Annuler"};
        int opt = JOptionPane.showOptionDialog(null, message, "Connexion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (opt == 2 || opt == JOptionPane.CLOSED_OPTION) return new LoginResult(true, false, null, null);
        if (opt == 1) return new LoginResult(false, true, null, null);
        return new LoginResult(false, false, email.getText().trim(), new String(pass.getPassword()));
    }

    // ---- Register ----
    public static class RegisterData {
        String email, password, fullName, sex, activity;
        Double heightCm, weightKg; Integer age;
        User toUser() {
            User u = new User();
            u.setEmail(email); u.setPassword(password); u.setFullName(fullName);
            u.setHeightCm(heightCm); u.setWeightKg(weightKg);
            u.setSex(sex); u.setAge(age); u.setActivityLevel(activity);
            return u;
        }
    }

    public static RegisterData showRegisterDialog() {
        JTextField email = new JTextField(22);
        JPasswordField pass = new JPasswordField(22);
        JTextField full = new JTextField(22);
        JTextField height = new JTextField(8);
        JTextField weight = new JTextField(8);
        JComboBox<String> sex = new JComboBox<>(new String[]{"M","F","O"});
        JTextField age = new JTextField(6);
        JComboBox<String> act = new JComboBox<>(new String[]{"sedentary","light","moderate","active"});

        Object[] msg = {
                label("Créer un compte"),
                new JLabel("Email :"), email,
                new JLabel("Mot de passe :"), pass,
                new JLabel("Nom complet :"), full,
                new JLabel("Taille (cm) :"), height,
                new JLabel("Poids (kg) :"), weight,
                new JLabel("Sexe :"), sex,
                new JLabel("Âge :"), age,
                new JLabel("Activité :"), act
        };

        int ok = JOptionPane.showConfirmDialog(null, msg, "Inscription", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;

        try {
            double h = Double.parseDouble(height.getText().trim());
            double w = Double.parseDouble(weight.getText().trim());
            int a = Integer.parseInt(age.getText().trim());
            if (h<50||h>300||w<10||w>500||a<10||a>100) throw new IllegalArgumentException("Valeurs hors limites.");

            RegisterData rd = new RegisterData();
            rd.email = email.getText().trim();
            rd.password = new String(pass.getPassword());
            rd.fullName = full.getText().trim();
            rd.heightCm = h;
            rd.weightKg = w;
            rd.sex = (String) sex.getSelectedItem();
            rd.age = a;
            rd.activity = (String) act.getSelectedItem();
            return rd;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Saisies invalides : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        return l;
    }
}
