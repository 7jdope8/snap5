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
package org.esa.snap.visat;

import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.application.ApplicationDescriptor;
import org.esa.snap.util.SystemUtils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * This class pertains to the "about" dialog box for the application.
 */
public class VisatAboutBox extends ModalDialog {

    public VisatAboutBox() {
        this(new JButton[]{
//                new JButton(),
                new JButton(),
        });
    }

    private VisatAboutBox(JButton[] others) {
        super(VisatApp.getApp().getMainFrame(), String.format("About %s", VisatApp.getApp().getAppName()),
              ModalDialog.ID_OK, others, null);    /*I18N*/


        final JButton systemButton = others[0];
        systemButton.setText("System Info...");  /*I18N*/
        systemButton.addActionListener(e -> showSystemDialog());

        final ApplicationDescriptor applicationDescriptor = VisatApp.getApp().getApplicationDescriptor();
        String aboutImagePath = applicationDescriptor.getAboutImagePath();
        Icon imageIcon = null;
        if (aboutImagePath != null) {
            aboutImagePath = aboutImagePath.trim();
            URL resource = getClass().getResource(aboutImagePath);
            if (resource != null) {
                imageIcon = new ImageIcon(resource);
            } else {
                SystemUtils.LOG.severe("Missing icon resource: " + aboutImagePath);
            }
        }

        JLabel imageLabel = new JLabel(imageIcon);
        JPanel dialogContent = new JPanel(new BorderLayout());
        String versionText = getVersionHtml();
        JLabel versionLabel = new JLabel(versionText);

        JPanel labelPane = new JPanel(new BorderLayout());
        labelPane.add(BorderLayout.NORTH, versionLabel);

        dialogContent.setLayout(new BorderLayout(4, 4));
        dialogContent.add(BorderLayout.NORTH, imageLabel);
        dialogContent.add(BorderLayout.SOUTH, labelPane);

        setContent(dialogContent);
    }

    @Override
    protected void onOther() {
        // override default behaviour by doing nothing
    }


    private void showSystemDialog() {
        final ModalDialog modalDialog = new ModalDialog(getJDialog(), "System Info", ID_OK, null);
        final Object[][] sysInfo = getSystemInfo();
        final JTable sysTable = new JTable(sysInfo, new String[]{"Property", "Value"}); /*I18N*/
        final JScrollPane systemScroll = new JScrollPane(sysTable);
        systemScroll.setPreferredSize(new Dimension(400, 400));
        modalDialog.setContent(systemScroll);
        modalDialog.show();
    }

    private static String getVersionHtml() {
        return "<html>" +
               "<b>Sentinel-3 Toolbox</b>" +
               "<br>(c) Copyright 2014 by Brockmann Consult and contributors. All rights reserved." +
               "<br>" +
               "<b>Sentinel-1 Toolbox</b>" +
               "<br>(c) Copyright 2014 by Array Systems Computing Inc. and contributors. All rights reserved." +
               "<br>" +
               "<b>Sentinel-2 Toolbox</b>" +
               "<br>(c) Copyright 2014 by C-S and contributors. All rights reserved." +
               "<br>" +
               "<br>This software is build on the heritage of:" +
               "<br>" +
               "<b>BEAM</b>" +
               "<br>(c) Copyright 2002-2014 by Brockmann Consult and contributors. All rights reserved." +
               "<br>" +
               "<b>NEST</b>" +
               "<br>(c) Copyright 2007-2014 by Array Systems Computing Inc. and contributors. All rights reserved." +
               "<br>" +
               "<br>This program has been developed under contract to ESA (ESRIN)." +
               "<br>" +
               "<br>This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License" +
               "<br>as published by the Free Software Foundation. This program is distributed in the hope it will be useful, but WITHOUT ANY" +
               "<br>WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." +
               "<br>See the GNU General Public License for more details." +
               "<br>" +
               "</html>";
    }

    private static Object[][] getSystemInfo() {

        final List<Object[]> data = new ArrayList<>();

        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (RuntimeException e) {
            //ignore
        }
        if (sysProps != null) {
            final String[] names = new String[sysProps.size()];
            final Enumeration<?> e = sysProps.propertyNames();
            for (int i = 0; i < names.length; i++) {
                names[i] = (String) e.nextElement();
            }
            Arrays.sort(names);
            for (String name : names) {
                final String value = sysProps.getProperty(name);
                data.add(new Object[]{name, value});
            }
        }

        final Object[][] dataArray = new Object[data.size()][2];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = data.get(i);
        }
        return dataArray;
    }
}
