/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.help.HelpSys;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;
import org.esa.snap.util.io.SnapFileChooser;
import org.esa.snap.util.io.SnapFileFilter;
import org.esa.snap.visat.VisatApp;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This actions exports ground control points of the selected product in a ENVI format.
 *
 */
public class ExportEnviGcpFileAction extends ExecCommand {

    private static final String GCP_FILE_DESCRIPTION = "ENVI Ground Control Points";
    private static final String GCP_FILE_EXTENSION = ".pts";
    private static final String GCP_LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String GCP_EXPORT_DIR_PREFERENCES_KEY = "user.gcp.export.dir";
    private static final String DIALOG_TITLE = "Export ENVI Ground Control Points";

    @Override
    public void actionPerformed(CommandEvent event) {
        exportGroundControlPoints();
    }

    @Override
    public void updateState(CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null);
    }

    private void exportGroundControlPoints() {
        VisatApp visatApp = VisatApp.getApp();
        final Product product = visatApp.getSelectedProduct();
        if (product == null) {
            return;
        }

        JFileChooser fileChooser = createFileChooser(visatApp);
        int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (file == null || file.getName().equals("")) {
            return;
        }
        final File absoluteFile = FileUtils.ensureExtension(file.getAbsoluteFile(), GCP_FILE_EXTENSION);
        String lastDirPath = absoluteFile.getParent();
        visatApp.getPreferences().setPropertyString(GCP_EXPORT_DIR_PREFERENCES_KEY, lastDirPath);

        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            return;
        }
        if (!visatApp.promptForOverwrite(absoluteFile)) {
            return;
        }
        if (absoluteFile.exists()) {
            absoluteFile.delete();
        }
        try {
            FileWriter writer = new FileWriter(absoluteFile);
            writer.write(createLineString("; ENVI Registration GCP File"));
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            final int resolution = visatApp.getPreferences().getPropertyInt("gcp.resolution", new Integer(112));
            final int gcpWidth = Math.max(width / resolution + 1, 2); //2 minimum
            final int gcpHeight = Math.max(height / resolution + 1, 2);//2 minimum
            final float xMultiplier = 1f * (width - 1) / (gcpWidth - 1);
            final float yMultiplier = 1f * (height - 1) / (gcpHeight - 1);
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();
            for (int y = 0; y < gcpHeight; y++) {
                for (int x = 0; x < gcpWidth; x++) {
                    final float imageX = xMultiplier * x;
                    final float imageY = yMultiplier * y;
                    pixelPos.x = imageX + 0.5f;
                    pixelPos.y = imageY + 0.5f;
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    final double mapX = geoPos.lon; //longitude
                    final double mapY = geoPos.lat; //latitude
                    writer.write(createLineString(mapX, mapY,
                                                  pixelPos.x + 1,
                                                  // + 1 because ENVI uses a one-based pixel co-ordinate system
                                                  pixelPos.y + 1));
                }
            }
        } catch (IOException e) {
            visatApp.showErrorDialog(DIALOG_TITLE, "An I/O error occurred:\n" + e.getMessage());
        }
    }

    private static String createLineString(final String str) {
        return str.concat(GCP_LINE_SEPARATOR);
    }

    private static String createLineString(final double mapX, final double mapY, final double imageX, final double imageY) {
        return "" + mapX + "\t" + mapY + "\t" + imageX + "\t" + imageY + GCP_LINE_SEPARATOR;
    }

    private JFileChooser createFileChooser(final VisatApp visatApp) {
        String lastDirPath = visatApp.getPreferences().getPropertyString(GCP_EXPORT_DIR_PREFERENCES_KEY,
                                                                         SystemUtils.getUserHomeDir().getPath());
        SnapFileChooser fileChooser = new SnapFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(new File(lastDirPath));

        fileChooser.setFileFilter(
                new SnapFileFilter(GCP_FILE_DESCRIPTION, GCP_FILE_EXTENSION, GCP_FILE_DESCRIPTION));
        fileChooser.setDialogTitle(DIALOG_TITLE);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return fileChooser;
    }
}
