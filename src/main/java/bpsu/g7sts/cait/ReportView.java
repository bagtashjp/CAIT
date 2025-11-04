/*
 * Copyright (C) 2025 Xthliene
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package bpsu.g7sts.cait;

import com.mongodb.client.model.Updates;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 *
 * @author Xthliene
 */
public class ReportView extends JDialog {

    /**
     * Creates new form ReportView
     */
    CAIT parent;
    DragonBallZ db;
    User user;
    Issue issue;
    boolean needsRefresh = false;
    int heightBuilder = 110;

    public ReportView(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public boolean showDialog(Issue theIssue) {
        issue = theIssue;
        parent = (CAIT) this.getOwner();
        db = parent.db;
        user = parent.user;
        wTitle.setText(issue.title.toUpperCase());
        wDescription.setText(issue.description);
        wDepartment.setText(issue.department);
        wSeverity.setText(issue.severity.toString());
        wTags.setText(String.join(", ", issue.tags));
        if (issue.images.isEmpty()) {
            jPanel2.remove(jLabel8);
            jPanel2.remove(jScrollPane3);
            jLabel8 = null;
            jScrollPane3 = null;
            jPanel2.setPreferredSize(new Dimension(800, 240));
            jPanel2.setMaximumSize(new Dimension(800, 240));
            jPanel2.setMinimumSize(new Dimension(800, 240));
            jPanel1.revalidate();
            jPanel1.repaint();
        } else {
            for (ObjectId image : issue.images) {
                JLabel imgLbl = new JLabel();
                imgLbl.setIcon(db.getImage(image, 190));
                wImages.add(imgLbl);
            }
        }

        wStatusLbl.setText(issue.getStatus().toString());
        if (issue.getStatus() == Status.RESOLVED
                || (user.role != Role.MANAGER && user.role != Role.ADMIN)) {

            // Remove all action-related controls
            wProgressCon.remove(wResolver);
            wProgressCon.remove(wRejecter);
            wProgressCon.remove(wAssignTxt);
            wProgressCon.remove(wAssignBtn);

            // Nullify references
            wResolver = null;
            wRejecter = null;
            wAssignTxt = null;
            wAssignBtn = null;

            heightBuilder -= 40;

        } else {
            if (issue.getStatus() == Status.OPEN || !issue.isAllDone()) {
                wProgressCon.remove(wResolver);
                wResolver = null;
            } else {
                wResolver.addActionListener(evt -> {
                    String conf = JOptionPane.showInputDialog(ReportView.this, "Enter Reason/Remark.");
                    if (conf != null && !conf.isBlank()) {
                        db.updatePost(issue.id, Updates.combine(
                                Updates.set("status", "RESOLVED"),
                                Updates.set("closer", user.id),
                                Updates.set("close_reason", conf)
                        ));
                        issue.selfUpdate();
                        needsRefresh = true;
                        ReportView.this.dispose();
                        System.out.println("Resolved issue " + issue.id.toHexString());
                    }
                });
            }

            if (issue.getStatus() == Status.ONGOING) {
                wRejecter.setText("Mark as Canceled");
                wRejecter.addActionListener(evt -> {
                    String conf = JOptionPane.showInputDialog(ReportView.this, "Enter the reason for canceling this issue.");
                    if (conf != null && !conf.isBlank()) {
                        db.updatePost(issue.id, Updates.combine(
                                Updates.set("status", "CANCELED"),
                                Updates.set("closer", user.id),
                                Updates.set("close_reason", conf)
                        ));
                        issue.selfUpdate();
                        needsRefresh = true;
                        ReportView.this.dispose();
                        System.out.println("Canceled issue " + issue.id.toHexString());
                    }
                });
            } else {
                wRejecter.addActionListener(evt -> {
                    String conf = JOptionPane.showInputDialog(ReportView.this, "Enter the reason for canceling this issue.");
                    if (conf != null && !conf.isBlank()) {
                        db.updatePost(issue.id, Updates.combine(
                                Updates.set("status", "REJECTED"),
                                Updates.set("closer", user.id),
                                Updates.set("close_reason", conf)
                        ));
                        issue.selfUpdate();
                        needsRefresh = true;
                        ReportView.this.dispose();
                        System.out.println("Rejected issue " + issue.id.toHexString());
                    }
                });
            }

        }

        if (issue.getStatus() == Status.OPEN) {
            wProgressCon.remove(jLabel13);
            heightBuilder -= 30;
        } else {
            jLabel13.setLocation(jLabel13.getX(), heightBuilder - 30);
            jPanel3.setLocation(jPanel3.getX(), heightBuilder);
            updateCorrectives();
        }
        wProgressCon.setPreferredSize(new Dimension(wProgressCon.getPreferredSize().width, heightBuilder));

        setVisible(true);
        return needsRefresh;
    }

    public void updateCorrectives() {

        for (int i = 0; i < issue.progress.size(); i++) {
            Progress progress = issue.progress.get(i);
            JPanel vActionCon = new JPanel();
            vActionCon.setBackground(progress.isDone
                    ? new Color(203, 255, 212)
                    : new Color(255, 255, 204)
            );
            vActionCon.setLayout(null);
            JLabel vProgDesc = new JLabel();
            vProgDesc.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
            vProgDesc.setForeground(new Color(0, 0, 0));
            vProgDesc.setText(progress.description);
            vActionCon.add(vProgDesc);
            vProgDesc.setBounds(10, 10, 630, 20);

            if (!progress.isDone && user.id.equals(progress.handler.id)) {
                JButton vDonerBtn = new JButton();
                vDonerBtn.setBackground(new Color(153, 255, 153));
                vDonerBtn.setIcon(new ImageIcon(getClass().getResource("/done-icon.png"))); // NOI18N
                vDonerBtn.setToolTipText("Mark this action as done.");
                vDonerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                vActionCon.add(vDonerBtn);
                vDonerBtn.setBounds(700, 10, 40, 40);
                final int index = i;
                vDonerBtn.addActionListener(evt -> {
                    int op = JOptionPane.showConfirmDialog(this, "Do you want to mark this post done?", "Confirm Done", JOptionPane.OK_CANCEL_OPTION);
                    if (op == JOptionPane.OK_OPTION) {
                        db.updateProgressDone(issue.id, index);
                        progress.isDone = true;
                        needsRefresh = true;
                        ReportView.this.dispose();
                    }
                });

                /*
                    JButton vAttachBtn = new JButton();
                    vAttachBtn.setBackground(new Color(204, 255, 255));
                    vAttachBtn.setIcon(new ImageIcon(getClass().getResource("/attach-image.png"))); // NOI18N
                    vAttachBtn.setToolTipText("Attach Image");
                    vAttachBtn.addActionListener(this::wAttachBtnActionPerformed);
                    vActionCon.add(wAttachBtn);
                    vAttachBtn.setBounds(650, 10, 40, 40);
                 */
            }

            JLabel vAssignedToLbl = new JLabel();
            vAssignedToLbl.setForeground(new Color(180, 180, 180));
            vAssignedToLbl.setText("Assigned to:");
            vActionCon.add(vAssignedToLbl);
            vAssignedToLbl.setBounds(10, 30, 90, 16);

            JLabel vAssignedByLbl = new JLabel();
            vAssignedByLbl.setForeground(new Color(180, 180, 180));
            vAssignedByLbl.setText("Assigned by:");
            vActionCon.add(vAssignedByLbl);
            vAssignedByLbl.setBounds(300, 30, 90, 16);

            JLabel vAssigner = new JLabel();
            vAssigner.setFont(new Font("Segoe UI", 0, 13));
            vAssigner.setForeground(new Color(60, 63, 65));
            vAssigner.setText(progress.handler.getFullName());
            vActionCon.add(vAssigner);
            vAssigner.setBounds(370, 30, 220, 15);

            JLabel vAssignee = new JLabel();
            vAssignee.setFont(new Font("Segoe UI", 0, 13));
            vAssignee.setForeground(new Color(60, 63, 65));
            vAssignee.setText(progress.manager.getFullName());
            vActionCon.add(vAssignee);
            vAssignee.setBounds(80, 32, 220, 15);
            jPanel3.add(vActionCon);
            vActionCon.setBounds(0, jPanel3.getHeight(), 750, 50);
            jPanel3.setSize(jPanel3.getWidth(), jPanel3.getHeight() + 60);
            wProgressCon.setSize(wProgressCon.getWidth(), heightBuilder);
            heightBuilder += 60;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        wActionCon = new JPanel();
        wAssigner = new JLabel();
        wDonerBtn = new JButton();
        wAttachBtn = new JButton();
        jLabel1 = new JLabel();
        wProgDesc = new JLabel();
        jLabel5 = new JLabel();
        wAssignee = new JLabel();
        jPanel6 = new JPanel();
        jPanel9 = new JPanel();
        jPanel10 = new JPanel();
        jLabel11 = new JLabel();
        jLabel15 = new JLabel();
        jButton7 = new JButton();
        jScrollPane1 = new JScrollPane();
        jPanel1 = new JPanel();
        jPanel2 = new JPanel();
        wTitle = new JLabel();
        jScrollPane2 = new JScrollPane();
        wDescription = new JTextArea();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        jLabel4 = new JLabel();
        wDepartment = new JLabel();
        wSeverity = new JLabel();
        wTags = new JLabel();
        jScrollPane3 = new JScrollPane();
        wImages = new JPanel();
        jLabel8 = new JLabel();
        jButton1 = new JButton();
        wProgressCon = new JPanel();
        jLabel9 = new JLabel();
        wStatusLbl = new JLabel();
        wAssignBtn = new JButton();
        wAssignTxt = new JTextField();
        jLabel13 = new JLabel();
        wRejecter = new JButton();
        wResolver = new JButton();
        jPanel3 = new JPanel();
        jPanel8 = new JPanel();

        wActionCon.setBackground(new Color(203, 255, 212));
        wActionCon.setLayout(null);

        wAssigner.setFont(new Font("Segoe UI", 0, 13)); // NOI18N
        wAssigner.setForeground(new Color(60, 63, 65));
        wAssigner.setText("Harley Jet Bagtas");
        wActionCon.add(wAssigner);
        wAssigner.setBounds(370, 30, 220, 15);

        wDonerBtn.setBackground(new Color(153, 255, 153));
        wDonerBtn.setIcon(new ImageIcon(getClass().getResource("/done-icon.png"))); // NOI18N
        wDonerBtn.setToolTipText("Mark this action as done.");
        wActionCon.add(wDonerBtn);
        wDonerBtn.setBounds(700, 10, 40, 40);

        wAttachBtn.setBackground(new Color(204, 255, 255));
        wAttachBtn.setIcon(new ImageIcon(getClass().getResource("/attach-image.png"))); // NOI18N
        wAttachBtn.setToolTipText("Attach Image");
        wAttachBtn.addActionListener(this::wAttachBtnActionPerformed);
        wActionCon.add(wAttachBtn);
        wAttachBtn.setBounds(650, 10, 40, 40);

        jLabel1.setForeground(new Color(180, 180, 180));
        jLabel1.setText("Assigned by:");
        wActionCon.add(jLabel1);
        jLabel1.setBounds(300, 30, 90, 16);

        wProgDesc.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        wProgDesc.setForeground(new Color(0, 0, 0));
        wProgDesc.setText("Put a sticky note label ");
        wActionCon.add(wProgDesc);
        wProgDesc.setBounds(10, 10, 630, 20);

        jLabel5.setText("Assigned to:");
        wActionCon.add(jLabel5);
        jLabel5.setBounds(10, 30, 90, 16);

        wAssignee.setFont(new Font("Segoe UI", 0, 13)); // NOI18N
        wAssignee.setForeground(new Color(60, 63, 65));
        wAssignee.setText("Harley Jet Bagtas");
        wActionCon.add(wAssignee);
        wAssignee.setBounds(80, 32, 220, 15);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        getContentPane().setLayout(new OverlayLayout(getContentPane()));

        jPanel6.setLayout(null);

        jPanel9.setPreferredSize(new Dimension(300, 700));
        jPanel9.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));

        jPanel10.setBackground(new Color(255, 255, 255));
        jPanel10.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0), 2));
        jPanel10.setPreferredSize(new Dimension(280, 80));
        jPanel10.setLayout(null);

        jLabel11.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        jLabel11.setForeground(new Color(0, 0, 0));
        jLabel11.setText("Harley Jet Bagtas");
        jPanel10.add(jLabel11);
        jLabel11.setBounds(80, 5, 195, 40);

        jLabel15.setIcon(new ImageIcon(getClass().getResource("/testprofile.jpg"))); // NOI18N
        jPanel10.add(jLabel15);
        jLabel15.setBounds(5, 5, 70, 70);

        jButton7.setBackground(new Color(0, 102, 255));
        jButton7.setFont(new Font("Segoe UI", 1, 12)); // NOI18N
        jButton7.setForeground(new Color(255, 255, 255));
        jButton7.setText("Assign");
        jPanel10.add(jButton7);
        jButton7.setBounds(80, 45, 195, 30);

        jPanel9.add(jPanel10);

        jPanel6.add(jPanel9);
        jPanel9.setBounds(800, 0, 300, 700);

        jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setPreferredSize(new Dimension(800, 1414));

        jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.Y_AXIS));

        jPanel2.setBackground(new Color(255, 255, 255));
        jPanel2.setMaximumSize(new Dimension(800, 500));
        jPanel2.setMinimumSize(new Dimension(800, 500));
        jPanel2.setPreferredSize(new Dimension(755, 500));
        jPanel2.setLayout(null);

        wTitle.setFont(new Font("Segoe UI", 1, 24)); // NOI18N
        wTitle.setForeground(new Color(0, 0, 0));
        wTitle.setText("Title");
        jPanel2.add(wTitle);
        wTitle.setBounds(20, 40, 550, 40);

        wDescription.setEditable(false);
        wDescription.setBackground(new Color(255, 255, 255));
        wDescription.setColumns(20);
        wDescription.setFont(new Font("Segoe UI", 0, 14)); // NOI18N
        wDescription.setForeground(new Color(0, 0, 0));
        wDescription.setLineWrap(true);
        wDescription.setRows(5);
        wDescription.setText("The quick brown fox jumps over a lazy dog.");
        wDescription.setWrapStyleWord(true);
        jScrollPane2.setViewportView(wDescription);

        jPanel2.add(jScrollPane2);
        jScrollPane2.setBounds(20, 80, 750, 130);

        jLabel2.setForeground(new Color(0, 0, 0));
        jLabel2.setText("Tags:");
        jPanel2.add(jLabel2);
        jLabel2.setBounds(20, 210, 40, 30);

        jLabel3.setForeground(new Color(0, 0, 0));
        jLabel3.setText("Urgency:");
        jPanel2.add(jLabel3);
        jLabel3.setBounds(610, 30, 50, 20);

        jLabel4.setForeground(new Color(0, 0, 0));
        jLabel4.setText("Department:");
        jPanel2.add(jLabel4);
        jLabel4.setBounds(610, 50, 70, 30);

        wDepartment.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        wDepartment.setForeground(new Color(0, 0, 0));
        wDepartment.setText("ENSO");
        jPanel2.add(wDepartment);
        wDepartment.setBounds(690, 55, 80, 20);

        wSeverity.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        wSeverity.setForeground(new Color(0, 0, 0));
        wSeverity.setText("MEDIUM");
        jPanel2.add(wSeverity);
        wSeverity.setBounds(690, 30, 70, 20);

        wTags.setForeground(new Color(0, 0, 0));
        wTags.setText("Environmental, Lifting");
        jPanel2.add(wTags);
        wTags.setBounds(60, 210, 710, 30);

        jScrollPane3.setBorder(null);
        jScrollPane3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        wImages.setBackground(new Color(255, 255, 255));
        wImages.setLayout(new FlowLayout(FlowLayout.LEFT));
        jScrollPane3.setViewportView(wImages);

        jPanel2.add(jScrollPane3);
        jScrollPane3.setBounds(20, 260, 750, 210);

        jLabel8.setForeground(new Color(0, 0, 0));
        jLabel8.setText("Images");
        jPanel2.add(jLabel8);
        jLabel8.setBounds(20, 240, 60, 16);

        jButton1.setBackground(new Color(204, 204, 204));
        jButton1.setForeground(new Color(102, 102, 102));
        jButton1.setText("Save Report Snapshot (.JPG)");
        jButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jButton1.addActionListener(this::jButton1ActionPerformed);
        jPanel2.add(jButton1);
        jButton1.setBounds(20, 10, 190, 23);

        jPanel1.add(jPanel2);

        wProgressCon.setBackground(new Color(255, 255, 255));
        wProgressCon.setPreferredSize(new Dimension(755, 500));
        wProgressCon.setLayout(null);

        jLabel9.setForeground(new Color(0, 0, 0));
        jLabel9.setText("Status:");
        wProgressCon.add(jLabel9);
        jLabel9.setBounds(20, 10, 50, 20);

        wStatusLbl.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        wStatusLbl.setForeground(new Color(0, 0, 0));
        wStatusLbl.setText("Open");
        wProgressCon.add(wStatusLbl);
        wStatusLbl.setBounds(70, 10, 80, 20);

        wAssignBtn.setForeground(new Color(255, 255, 255));
        wAssignBtn.setText("Assign");
        wAssignBtn.addActionListener(this::wAssignBtnActionPerformed);
        wProgressCon.add(wAssignBtn);
        wAssignBtn.setBounds(650, 40, 120, 30);

        wAssignTxt.setBackground(new Color(255, 255, 255));
        wAssignTxt.setForeground(new Color(0, 0, 0));
        wProgressCon.add(wAssignTxt);
        wAssignTxt.setBounds(20, 40, 620, 30);

        jLabel13.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        jLabel13.setForeground(new Color(0, 0, 0));
        jLabel13.setText("CORRECTIVE ACTIONS");
        wProgressCon.add(jLabel13);
        jLabel13.setBounds(20, 80, 150, 20);

        wRejecter.setBackground(new Color(255, 0, 0));
        wRejecter.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        wRejecter.setForeground(new Color(0, 0, 0));
        wRejecter.setText("Mark as Rejected");
        wRejecter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        wProgressCon.add(wRejecter);
        wRejecter.setBounds(620, 10, 150, 27);

        wResolver.setBackground(new Color(0, 204, 51));
        wResolver.setFont(new Font("Segoe UI", 1, 14)); // NOI18N
        wResolver.setForeground(new Color(0, 0, 0));
        wResolver.setText("Mark as Resolved");
        wResolver.setCursor(new Cursor(Cursor.HAND_CURSOR));
        wProgressCon.add(wResolver);
        wResolver.setBounds(460, 10, 150, 27);

        jPanel3.setOpaque(false);
        jPanel3.setLayout(null);
        wProgressCon.add(jPanel3);
        jPanel3.setBounds(20, 100, 750, 0);

        jPanel1.add(wProgressCon);

        jScrollPane1.setViewportView(jPanel1);

        jPanel6.add(jScrollPane1);
        jScrollPane1.setBounds(0, 0, 800, 700);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(10);

        getContentPane().add(jPanel6);

        GroupLayout jPanel8Layout = new GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(jPanel8Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 800, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(jPanel8Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 700, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel8);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void wAttachBtnActionPerformed(ActionEvent evt) {//GEN-FIRST:event_wAttachBtnActionPerformed

    }//GEN-LAST:event_wAttachBtnActionPerformed

    private void wAssignBtnActionPerformed(ActionEvent evt) {//GEN-FIRST:event_wAssignBtnActionPerformed
        String desc = wAssignTxt.getText();
        Progress prog = new Progress();
        prog.description = desc;
        prog.manager = user;
        prog.handler = user;
        issue.progress.add(prog);
        Document updateOperation = new Document("$push",
                new Document("progress", prog.toDoc()));
        db.updatePost(issue.id, updateOperation);
        needsRefresh = true;
        this.dispose();
    }//GEN-LAST:event_wAssignBtnActionPerformed

    private void jButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        setAllComponentsVisible(jPanel1, false);
        jPanel1.revalidate();
        jPanel1.repaint();
        BufferedImage reportImage = captureComponent(jPanel1);
        String fileName = "Report_" + System.currentTimeMillis() + ".jpg";
        saveImage(reportImage, fileName, "jpeg");
        setAllComponentsVisible(jPanel1, true);
        jPanel1.revalidate();
        jPanel1.repaint();
    }//GEN-LAST:event_jButton1ActionPerformed
    public BufferedImage captureComponent(Component component) {
        if (component == null) {
            return null;
        }

        // Get the component's current dimensions
        int width = component.getWidth();
        int height = component.getHeight();

        // Create an image buffer in memory
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Get the graphics context and draw the component onto the image
        Graphics2D g2d = image.createGraphics();
        component.paint(g2d); // Renders the component and all its children
        g2d.dispose();

        return image;
    }

    public boolean saveImage(BufferedImage image, String filePath, String format) {
        if (image == null) {
            System.err.println("Error: No image data provided.");
            return false;
        }

        // Ensure the format is supported (e.g., "png", "jpeg", "gif")
        String safeFormat = format.toLowerCase();

        try {
            File outputFile = new File(filePath);

            // ImageIO writes the BufferedImage to the file in the specified format
            boolean success = ImageIO.write(image, safeFormat, outputFile);

            if (success) {
                System.out.println("Image saved successfully to: " + outputFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Image saved successfully to: " + outputFile.getAbsolutePath());
            }
            return success;

        } catch (IOException e) {
            System.err.println("Error writing image file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void setAllComponentsVisible(Container parent, boolean visible) {

        // 1. Iterate over all components in the current container
        for (Component c : parent.getComponents()) {

            // 2. Check if the component is a Container (like a JPanel)
            if (c instanceof Container) {
                // A. If it's a container, recursively call the method to handle its children.
                // DO NOT set the container's visibility yet, or you'll hide the background!
                setAllComponentsVisible((Container) c, visible);
            }

            // 3. Apply Visibility to the component itself
            // We generally want to hide interactive elements. 
            // This is a comprehensive list of common elements you'd want to hide/show:
            if (c instanceof javax.swing.JButton
                    || c instanceof javax.swing.JTextField
                    || c instanceof javax.swing.JComboBox
                    || c instanceof javax.swing.JCheckBox) {
                // Only set visibility for the component itself if it's one of the types we target.
                c.setVisible(visible);
            }

            // If you want to hide EVERYTHING that is NOT a Container, use a simpler check:
            // if (!(c instanceof Container)) {
            //     c.setVisible(visible);
            // }
        }

        // CRITICAL FIX: Repaint and Revalidate only needs to happen AFTER the entire 
        // hide/show sequence is complete on the top-level window.
        // Calling it inside the recursion loop is inefficient and causes issues. 
        // Also, the logic must be called on the TOP-LEVEL WINDOW (JFrame/JDialog),
        // not just the current parent container.
        // It's best to remove these two lines from inside this method:
        // parent.revalidate(); 
        // parent.repaint(); 
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton jButton1;
    private JButton jButton7;
    private JLabel jLabel1;
    private JLabel jLabel11;
    private JLabel jLabel13;
    private JLabel jLabel15;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel8;
    private JLabel jLabel9;
    private JPanel jPanel1;
    private JPanel jPanel10;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel6;
    private JPanel jPanel8;
    private JPanel jPanel9;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JPanel wActionCon;
    private JButton wAssignBtn;
    private JTextField wAssignTxt;
    private JLabel wAssignee;
    private JLabel wAssigner;
    private JButton wAttachBtn;
    private JLabel wDepartment;
    private JTextArea wDescription;
    private JButton wDonerBtn;
    private JPanel wImages;
    private JLabel wProgDesc;
    private JPanel wProgressCon;
    private JButton wRejecter;
    private JButton wResolver;
    private JLabel wSeverity;
    private JLabel wStatusLbl;
    private JLabel wTags;
    private JLabel wTitle;
    // End of variables declaration//GEN-END:variables
}
