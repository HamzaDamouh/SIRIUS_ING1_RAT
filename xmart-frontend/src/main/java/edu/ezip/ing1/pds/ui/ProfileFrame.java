package edu.ezip.ing1.pds.ui;

import edu.ezip.ing1.pds.business.dto.MealPlan;
import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.services.MealPlanClientService;
import edu.ezip.ing1.pds.services.UserClientService;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ProfileFrame extends JFrame {
    private final UserClientService users;
    private final MealPlanClientService plans;
    private User me;

    private final JTextField full = new JTextField(20);
    private final JTextField height = new JTextField(8);
    private final JTextField weight = new JTextField(8);
    private final JTextField age = new JTextField(6);
    private final JComboBox<String> sex = new JComboBox<>(new String[]{"M","F","O"});
    private final JComboBox<String> act = new JComboBox<>(new String[]{"sedentary","light","moderate","active"});

    private final JLabel bmiVal = new JLabel("-");
    private final JLabel kcalBig = new JLabel("-");

    public ProfileFrame(User me, UserClientService users, MealPlanClientService plans) {
        super("Profil");
        this.me = me;
        this.users = users;
        this.plans = plans;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildUI());
        setSize(640, 460);
        setLocationRelativeTo(null);
        refreshFields();
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        // Top: big daily calories
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Objectif kcal/jour", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.PLAIN, 14f));
        kcalBig.setHorizontalAlignment(SwingConstants.CENTER);
        kcalBig.setFont(kcalBig.getFont().deriveFont(Font.BOLD, 36f));
        top.add(title, BorderLayout.NORTH);
        top.add(kcalBig, BorderLayout.CENTER);

        // Center: profile form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.fill = GridBagConstraints.HORIZONTAL;
        int r=0;
        addRow(form, g, r++, "Nom complet :", full);
        addRow(form, g, r++, "Taille (cm) :", height);
        addRow(form, g, r++, "Poids (kg) :", weight);
        addRow(form, g, r++, "Âge :", age);
        addRow(form, g, r++, "Sexe :", sex);
        addRow(form, g, r++, "Activité :", act);
        addRow(form, g, r++, "IMC :", bmiVal);

        // Bottom: actions
        JButton save = new JButton("Enregistrer");
        JButton genPlan = new JButton("Générer plan (±10%)");
        JButton showPlan = new JButton("Afficher le plan");
        JButton logout = new JButton("Déconnexion");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        left.add(logout);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.add(showPlan);
        right.add(genPlan);
        right.add(save);

        JPanel actions = new JPanel(new BorderLayout());
        actions.add(left, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);

        save.addActionListener(e -> doSave());
        genPlan.addActionListener(e -> doGenerateAndShowPlan());
        showPlan.addActionListener(e -> doShowPlan());
        logout.addActionListener(e -> doLogout());

        root.add(top, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent c) {
        g.gridx=0; g.gridy=row; p.add(new JLabel(label), g);
        g.gridx=1; p.add(c, g);
    }

    private void refreshFields() {
        full.setText(nullTo("", me.getFullName()));
        height.setText(me.getHeightCm()==null?"":String.valueOf(me.getHeightCm()));
        weight.setText(me.getWeightKg()==null?"":String.valueOf(me.getWeightKg()));
        age.setText(me.getAge()==null?"":String.valueOf(me.getAge()));
        sex.setSelectedItem(me.getSex()==null?"O":me.getSex());
        act.setSelectedItem(me.getActivityLevel()==null?"sedentary":me.getActivityLevel());

        bmiVal.setText(me.getBmi()==null?"-":String.valueOf(me.getBmi()));
        kcalBig.setText(me.getDailyKcalTarget()==null?"-":String.valueOf(me.getDailyKcalTarget()));
    }

    private void doSave() {
        try {
            Double h = Double.valueOf(height.getText().trim());
            Double w = Double.valueOf(weight.getText().trim());
            Integer a = Integer.valueOf(age.getText().trim());
            if (h<50||h>300||w<10||w>500||a<10||a>100) {
                JOptionPane.showMessageDialog(this, "Valeurs hors limites.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            User upd = new User();
            upd.setId(me.getId());
            upd.setFullName(full.getText().trim());
            upd.setHeightCm(h);
            upd.setWeightKg(w);
            upd.setAge(a);
            upd.setSex((String) sex.getSelectedItem());
            upd.setActivityLevel((String) act.getSelectedItem());

            new SwingWorker<User, Void>() {
                @Override protected User doInBackground() throws Exception { return users.update(upd); }
                @Override protected void done() {
                    try {
                        User fresh = get();
                        if (fresh == null) {
                            JOptionPane.showMessageDialog(ProfileFrame.this, "Échec de mise à jour.", "Erreur", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        me = fresh;
                        refreshFields();
                        JOptionPane.showMessageDialog(ProfileFrame.this, "Profil mis à jour.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(ProfileFrame.this, "Erreur réseau/serveur.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Saisies invalides.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Generate then immediately fetch & show
    private void doGenerateAndShowPlan() {
        new SwingWorker<MealPlan, Void>() {
            @Override protected MealPlan doInBackground() throws Exception {
                plans.generate(me.getId(), 0.10);
                return plans.get(me.getId());
            }
            @Override protected void done() {
                try {
                    MealPlan plan = get();
                    if (plan == null) {
                        JOptionPane.showMessageDialog(ProfileFrame.this, "Aucun plan trouvé.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    showPlanDialog(plan);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ProfileFrame.this, "Erreur réseau/serveur.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Only fetch & show
    private void doShowPlan() {
        new SwingWorker<MealPlan, Void>() {
            @Override protected MealPlan doInBackground() throws Exception { return plans.get(me.getId()); }
            @Override protected void done() {
                try {
                    MealPlan plan = get();
                    if (plan == null) {
                        JOptionPane.showMessageDialog(ProfileFrame.this, "Aucun plan trouvé.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    showPlanDialog(plan);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ProfileFrame.this, "Erreur réseau/serveur.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showPlanDialog(MealPlan plan) {
        String[] cols = {"Jour","Petit-déjeuner","Déjeuner","Dîner","Collation","Total (kcal)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){ return false; } };

        if (plan.getWeek() != null) {
            for (var dp : plan.getWeek()) {
                model.addRow(new Object[]{
                        safe(dp.getDay()),
                        mealLabel(dp.getBreakfast()),
                        mealLabel(dp.getLunch()),
                        mealLabel(dp.getDinner()),
                        mealLabel(dp.getSnack()),
                        dp.getTotalKcal()==null? "-" : dp.getTotalKcal().toString()
                });
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(22);
        JScrollPane sp = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout(8,8));
        JLabel top = new JLabel("Plan (cible : " + (plan.getTargetKcal()==null?"-":plan.getTargetKcal()) + " kcal, tolérance : " +
                (plan.getTolerance()==null?"-":plan.getTolerance()) + ")", SwingConstants.CENTER);
        panel.add(top, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        JDialog dlg = new JDialog(this, "Plan de repas", true);
        dlg.setContentPane(panel);
        dlg.setSize(740, 340);
        dlg.setLocationRelativeTo(this);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Fermer");
        bottom.add(close);
        panel.add(bottom, BorderLayout.SOUTH);
        close.addActionListener(e -> dlg.dispose());

        dlg.setVisible(true);
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }


    private void doLogout()  {
        dispose();
        // relance la boucle login/inscription minimale
        try {
            MainSwingApp.main(new String[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String mealLabel(MealPlan.MealRef r) {
        if (r == null) return "-";
        String name = r.getName()==null? "-" : r.getName();
        String kcal = r.getKcal()==null? "-" : r.getKcal().toString();
        return name + " (" + kcal + ")";
    }

    private static String nullTo(String def, String v) { return v==null?def:v; }
}
