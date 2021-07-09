/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.oitools.image.FitsImage;
import java.util.List;

/**
 *
 * @author martin
 */
public class SliderPanel extends javax.swing.JPanel {

    List<FitsImage> fitsImages;
    FitsImagePanel fitsImagePanel;

    /**
     * Creates new form SliderPanel
     * @param fitsImagePanel
     */
    public SliderPanel(FitsImagePanel fitsImagePanel) {
        initComponents();
        
        this.fitsImagePanel = fitsImagePanel;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFitsImageCubeSlider = new javax.swing.JSlider();
        jSliderButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jFitsImageCubeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jFitsImageCubeSliderStateChanged(evt);
            }
        });
        add(jFitsImageCubeSlider, new java.awt.GridBagConstraints());
        this.jFitsImageCubeSlider.setValue(1);
        this.jFitsImageCubeSlider.setMinimum(1);
        this.jFitsImageCubeSlider.setMaximum(1);

        jSliderButton.setText("Edit range");
        jSliderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSliderButtonActionPerformed(evt);
            }
        });
        add(jSliderButton, new java.awt.GridBagConstraints());
    }// </editor-fold>//GEN-END:initComponents

    private void jFitsImageCubeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jFitsImageCubeSliderStateChanged
        if (this.fitsImagePanel != null && this.fitsImages != null && this.fitsImages.size() > 1) {
            this.fitsImagePanel.setFitsImage(this.fitsImages.get(this.jFitsImageCubeSlider.getValue() - 1));
        }
    }//GEN-LAST:event_jFitsImageCubeSliderStateChanged

    private void jSliderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSliderButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jSliderButtonActionPerformed
    
    public void setFitsImages(List<FitsImage> fitsImages) {
        this.fitsImages = fitsImages;
        this.jFitsImageCubeSlider.setMaximum(this.fitsImages.size());
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider jFitsImageCubeSlider;
    private javax.swing.JButton jSliderButton;
    // End of variables declaration//GEN-END:variables
}
