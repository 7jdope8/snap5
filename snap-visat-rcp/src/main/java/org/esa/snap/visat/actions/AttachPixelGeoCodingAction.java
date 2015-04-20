/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.BasicPixelGeoCoding;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoCodingFactory;
import org.esa.snap.framework.datamodel.PixelGeoCoding;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.ExpressionPane;
import org.esa.snap.framework.ui.GridBagUtils;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.UIUtils;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.framework.ui.product.ProductExpressionPane;
import org.esa.snap.util.ArrayUtils;
import org.esa.snap.util.StringUtils;
import org.esa.snap.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class AttachPixelGeoCodingAction extends ExecCommand {

    private static final String ATTACH_TITLE = "Attach Pixel Geo-Coding";

    @Override
    public void actionPerformed(final CommandEvent event) {
        attachPixelGeoCoding();
    }

    @Override
    public void updateState(final CommandEvent event) {
        boolean enabled = false;
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product != null) {
            final boolean hasPixelGeoCoding = product.getGeoCoding() instanceof BasicPixelGeoCoding;
            final boolean hasSomeBands = product.getNumBands() >= 2;
            enabled = !hasPixelGeoCoding && hasSomeBands;
        }
        setEnabled(enabled);
    }

    private static void attachPixelGeoCoding() {

        // todo - open dialog here where user can select PixelGeoCoding info

        final VisatApp visatApp = VisatApp.getApp();
        final Product product = visatApp.getSelectedProduct();
        final Window mainFrame = visatApp.getMainFrame();
        final PixelGeoCodingSetupDialog setupDialog = new PixelGeoCodingSetupDialog(mainFrame,
                                                                                    ATTACH_TITLE,
                                                                                    "pixelGeoCodingSetup",
                                                                                    product);   /*I18N*/
        if (setupDialog.show() != ModalDialog.ID_OK) {
            return;
        }
        final Band lonBand = setupDialog.getSelectedLonBand();
        final Band latBand = setupDialog.getSelectedLatBand();
        final int searchRadius = setupDialog.getSearchRadius();
        final String validMask = setupDialog.getValidMask();
        final String msgPattern = "New Pixel Geo-Coding: lon = ''{0}'' ; lat = ''{1}'' ; radius=''{2}'' ; mask=''{3}''";
        visatApp.getLogger().log(Level.INFO, MessageFormat.format(msgPattern,
                                                                  lonBand.getName(), latBand.getName(),
                                                                  searchRadius, validMask));


        final long requiredBytes = PixelGeoCoding.getRequiredMemory(product, validMask != null);
        final long requiredMegas = requiredBytes / (1024 * 1024);
        final long freeMegas = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        if (freeMegas < requiredMegas) {
            // TODO - make this a common dialog, e.g. for RGB image creation etc
            final String message = MessageFormat.format("This operation requires to load at least {0} M\n" +
                                                        "of additional data into memory.\n\n" +
                                                        "Do you really want to continue?",
                                                        requiredMegas
            );   /*I18N*/
            final int answer = visatApp.showQuestionDialog(ATTACH_TITLE,
                                                           message, null);


            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final ProgressMonitorSwingWorker<Void, Void> swingWorker = new ProgressMonitorSwingWorker<Void, Void>(mainFrame, ATTACH_TITLE) {

            @Override
            protected Void doInBackground(ProgressMonitor pm) throws Exception {
                final GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(latBand,
                                                                                       lonBand,
                                                                                       validMask,
                                                                                       searchRadius,
                                                                                       pm);
                product.setGeoCoding(pixelGeoCoding);
                return null;
            }

            @Override
            public void done() {
                UIUtils.setRootFrameDefaultCursor(mainFrame);
                try {
                    get();
                    visatApp.showInfoDialog(ATTACH_TITLE, "Pixel geo-coding has been attached.", null);
                } catch (Exception e) {
                    Throwable cause = e;
                    if (e instanceof ExecutionException) {
                        cause = e.getCause();
                    }
                    String msg = "An internal error occurred:\n" + e.getMessage();
                    if (cause instanceof IOException) {
                        msg = "An I/O error occurred:\n" + e.getMessage();
                    }
                    visatApp.showErrorDialog(ATTACH_TITLE,msg);
                } finally {
                    visatApp.updateState();
                }
            }
        };

        UIUtils.setRootFrameWaitCursor(mainFrame);
        swingWorker.executeWithBlocking();
    }

    private static class PixelGeoCodingSetupDialog extends ModalDialog {

        private String _selectedLonBand;
        private String _selectedLatBand;
        private String[] _bandNames;
        private JComboBox<String> _lonBox;
        private JComboBox<String> _latBox;
        private Product _product;
        private JTextField _validMaskField;
        private JSpinner _radiusSpinner;
        private final int _defaultRadius = 6;
        private final int _minRadius = 0;
        private final int _maxRadius = 99;
        private final int _bigRadiusStep = 0;
        private final int _smallRadiusStep = 1;

        public PixelGeoCodingSetupDialog(final Window parent, final String title,
                                         final String helpID, final Product product) {
            super(parent, title, ModalDialog.ID_OK_CANCEL_HELP, helpID);
            _product = product;
            final Band[] bands = product.getBands();
            _bandNames = new String[bands.length];
            for (int i = 0; i < bands.length; i++) {
                _bandNames[i] = bands[i].getName();
            }

        }

        @Override
        public int show() {
            createUI();
            return super.show();
        }


        public Band getSelectedLonBand() {
            return _product.getBand(_selectedLonBand);
        }

        public Band getSelectedLatBand() {
            return _product.getBand(_selectedLatBand);
        }

        public int getSearchRadius() {
            return ((Number) _radiusSpinner.getValue()).intValue();
        }

        public String getValidMask() {
            return _validMaskField.getText();
        }

        @Override
        protected void onOK() {
            final String lonValue = (String) _lonBox.getSelectedItem();
            _selectedLonBand = findBandName(lonValue);
            final String latValue = (String) _latBox.getSelectedItem();
            _selectedLatBand = findBandName(latValue);

            if (_selectedLatBand == null || _selectedLonBand == null || _selectedLatBand == _selectedLonBand) {
                VisatApp.getApp().showWarningDialog(super.getJDialog().getTitle(),
                                                    "You have to select two different bands for the Pixel Geo-Coding."); /*I18N*/
            } else {
                super.onOK();
            }
        }

        @Override
        protected void onCancel() {
            _selectedLatBand = null;
            _selectedLonBand = null;
            super.onCancel();
        }


        private void createUI() {
            final JPanel panel = new JPanel(new GridBagLayout());
            final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
            final JLabel lonLabel = new JLabel("Longitude band:");      /*I18N*/
            final JLabel latLabel = new JLabel("Latitude band:");       /*I18N*/
            final JLabel radiusLabel = new JLabel("Search radius:");    /*I18N*/
            final JLabel maskLabel = new JLabel("Valid mask:");         /*I18N*/
            _lonBox = new JComboBox<>(_bandNames);
            _latBox = new JComboBox<>(_bandNames);
            doPreSelection(_lonBox, "lon");
            doPreSelection(_latBox, "lat");
            _radiusSpinner = UIUtils.createSpinner(_defaultRadius, _minRadius, _maxRadius,
                                                   _smallRadiusStep, _bigRadiusStep, "#0");
            _validMaskField = new JTextField(createDefaultValidMask(_product));
            _validMaskField.setCaretPosition(0);
            final JButton exprDialogButton = new JButton("...");
            exprDialogButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    invokeExpressionEditor();
                }
            });
            final int preferredHeight = _validMaskField.getPreferredSize().height;
            exprDialogButton.setPreferredSize(new Dimension(preferredHeight, preferredHeight));
            _radiusSpinner.setPreferredSize(new Dimension(60, preferredHeight));

            gbc.insets = new Insets(3, 2, 3, 2);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(lonLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            gbc.gridwidth = 1;
            panel.add(_lonBox, gbc);

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(latLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            gbc.gridwidth = 1;
            panel.add(_latBox, gbc);

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(maskLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            panel.add(_validMaskField, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            panel.add(exprDialogButton, gbc);

            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            panel.add(radiusLabel, gbc);
            gbc.weightx = 1;
            gbc.gridx++;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(_radiusSpinner, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            panel.add(new JLabel("pixels"), gbc);
            setContent(panel);
        }

        private void invokeExpressionEditor() {
            final Window window = SwingUtilities.getWindowAncestor(VisatApp.getApp().getMainFrame());
            final ExpressionPane pane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{_product},
                                                                                          _product,
                                                                                          VisatApp.getApp().getPreferences());
            pane.setCode(_validMaskField.getText());
            final int status = pane.showModalDialog(window, "Edit Valid Mask Expression");  /*I18N*/
            if (status == ModalDialog.ID_OK) {
                _validMaskField.setText(pane.getCode());
                _validMaskField.setCaretPosition(0);
            }
        }

        private void doPreSelection(final JComboBox comboBox, final String toFind) {
            final String bandToSelect = getBandNameContaining(toFind);

            if (StringUtils.isNotNullAndNotEmpty(bandToSelect)) {
                comboBox.setSelectedItem(bandToSelect);
            }
        }

        private String getBandNameContaining(final String toFind) {
            for (final String bandName : _bandNames) {
                if (bandName.contains(toFind)) {
                    return bandName;
                }
            }
            return null;
        }

        private String findBandName(final String bandName) {
            for (final String band : _bandNames) {
                if (band.equals(bandName)) {
                    return band;
                }
            }
            return null;
        }

        private static String createDefaultValidMask(final Product product) {
            String validMask = null;
            final String[] flagNames = product.getAllFlagNames();
            final String invalidFlagName = "l1_flags.INVALID";
            if (ArrayUtils.isMemberOf(invalidFlagName, flagNames)) {
                validMask = "NOT " + invalidFlagName;
            }
            return validMask;
        }

    }
}
