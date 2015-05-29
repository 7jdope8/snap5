/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.rcp.actions.help;

import org.esa.snap.util.SystemUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;

/**
 * This action launches the default browser to display the project tutorials.
 */
@ActionID(
        category = "Help",
        id = "ShowTutorialsPageAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowTutorialsPageAction_MenuText",
        popupText = "#CTL_ShowTutorialsPageAction_MenuText",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Help",
                position = 310
        )
})
@NbBundle.Messages({
        "CTL_ShowTutorialsPageAction_MenuText=Tutorials",
        "CTL_ShowTutorialsPageAction_ShortDescription=Show the toolboxes tutorials web page"
})
public class ShowTutorialsPageAction extends AbstractAction {
    private static final String HOME_PAGE_URL_DEFAULT = "http://step.esa.int/main/tutorials";

    /**
     * Launches the default browser to display the tutorials.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final String homePageUrl = System.getProperty(SystemUtils.getApplicationContextId() + ".homePageUrl", HOME_PAGE_URL_DEFAULT);
        final Desktop desktop = Desktop.getDesktop();

        try {
            desktop.browse(URI.create(homePageUrl));
        } catch (IOException e) {
            // TODO - handle
        } catch (UnsupportedOperationException e) {
            // TODO - handle
        }
    }
}
