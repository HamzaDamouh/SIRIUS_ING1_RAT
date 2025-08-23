package edu.ezip.ing1.pds.ui;

import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.client.commons.ConfigLoader;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.services.MealPlanClientService;
import edu.ezip.ing1.pds.services.UserClientService;

import javax.swing.*;

public class MainSwingApp {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                // charge config réseau
                NetworkConfig net = ConfigLoader.loadConfig(NetworkConfig.class, "network.yaml");
                UserClientService users = new UserClientService(net);
                MealPlanClientService plans = new MealPlanClientService(net);

                // boucle d’auth simple
                while (true) {
                    AuthDialogs.LoginResult lr = AuthDialogs.showLoginDialog();
                    if (lr.cancelled) return;

                    if (lr.goRegister) {
                        AuthDialogs.RegisterData rd = AuthDialogs.showRegisterDialog();
                        if (rd == null) continue;
                        try {
                            User created = users.register(rd.toUser());
                            if (created == null) {
                                JOptionPane.showMessageDialog(null, "Création échouée.", "Erreur", JOptionPane.ERROR_MESSAGE);
                                continue;
                            }
                            new ProfileFrame(created, users, plans).setVisible(true);
                            return;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Erreur réseau/serveur.", "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        try {
                            User me = users.login(lr.email, lr.password);
                            if (me == null) {
                                JOptionPane.showMessageDialog(null, "Identifiants invalides.", "Connexion", JOptionPane.WARNING_MESSAGE);
                                continue;
                            }
                            new ProfileFrame(me, users, plans).setVisible(true);
                            return;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Erreur réseau/serveur.", "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Démarrage impossible : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
