package edu.ezip.ing1.pds;


import edu.ezip.ing1.pds.client.commons.ConfigLoader;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.services.UserClientService;
import edu.ezip.ing1.pds.ui.MainFrame;


import javax.swing.*;


public class MainApp {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                NetworkConfig net = ConfigLoader.loadConfig(NetworkConfig.class, "network.yaml");
                UserClientService userSvc = new UserClientService(net);
                MainFrame frame = new MainFrame(userSvc);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to load network config or start UI.\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
