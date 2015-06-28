/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.utils;

import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOpSpi;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterRegistry;
import org.esa.snap.modules.ModulePackager;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.esa.snap.util.io.FileUtils;
import org.openide.modules.ModuleInstall;
import org.openide.modules.Places;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Tool Adapter module installer class for NetBeans.
  * and menu entries.
 *
 * @author Cosmin Cara
 */
public class ModuleInstaller extends ModuleInstall {

    private final static String descriptionKeyName = "OpenIDE-Module-Short-Description";
    private final static Attributes.Name typeKey = new Attributes.Name("OpenIDE-Module-Type");
    private final static FilenameFilter jarFilter = (dir, name) -> name.endsWith("jar");
    private final static Set<String> processedJars = new HashSet<>();

    private final Logger logger = Logger.getGlobal();
    private final Path nbUserModulesPath = Paths.get(Places.getUserDirectory().getAbsolutePath(), "modules");
    private final File userModulePath = ToolAdapterIO.getUserAdapterPath();

    @Override
    public void restored() {
        Map<String, File> jarAdapters = getJarAdapters(nbUserModulesPath.toFile());
        jarAdapters.keySet().stream().filter(key -> !processedJars.contains(key)).forEach(key -> {
            try {
                File destination = new File(userModulePath, key);
                if (!destination.exists()) {
                    ModulePackager.unpackAdapterJar(jarAdapters.get(key), destination);
                } else {
                    File versionFile = new File(destination, "version.txt");
                    if (versionFile.exists()) {
                        String versionText = FileUtils.readText(versionFile);
                        String jarVersion = ModulePackager.getAdapterVersion(jarAdapters.get(key));
                        if (jarVersion != null && !versionText.equals(jarVersion)) {
                            ModulePackager.unpackAdapterJar(jarAdapters.get(key), destination);
                            logger.info(String.format("The adapter with the name %s and version %s was replaced by version %s", key, versionText, jarVersion));
                        } else {
                            logger.info(String.format("An adapter with the name %s and version %s already exists", key, versionText));
                        }
                    } else {
                        ModulePackager.unpackAdapterJar(jarAdapters.get(key), destination);
                    }
                }
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
            processedJars.add(key);
        });
        synchronized (ToolAdapterIO.class) {
            Collection<ToolAdapterOpSpi> toolAdapterOpSpis = ToolAdapterIO.searchAndRegisterAdapters();
            for (ToolAdapterOpSpi opSpi : toolAdapterOpSpis) {
                ToolAdapterOperatorDescriptor operatorDescriptor = (ToolAdapterOperatorDescriptor) opSpi.getOperatorDescriptor();
                if (operatorDescriptor != null) {
                    ToolAdapterActionRegistrar.registerOperatorMenu(operatorDescriptor);
                }
            }
        }
    }

    @Override
    public void uninstalled() {
        List<ToolAdapterOperatorDescriptor> registeredDescriptors = new ArrayList<>();
        registeredDescriptors.addAll(ToolAdapterRegistry.INSTANCE.getOperatorMap().values()
                            .stream()
                            .map(e -> (ToolAdapterOperatorDescriptor) e.getOperatorDescriptor())
                            .filter(ToolAdapterOperatorDescriptor::isFromPackage)
                            .collect(Collectors.toList()));
        Map<String, File> jarAdapters = getJarAdapters(nbUserModulesPath.toFile());
        // we have a "package" adapter, but no jar was found in NB modules folder
        registeredDescriptors.stream()
                             .filter(descriptor -> !jarAdapters.containsKey(descriptor.getAlias()))
                             .forEach(descriptor -> {
                                 ToolAdapterActionRegistrar.removeOperatorMenu(descriptor);
                                 ToolAdapterIO.removeOperator(descriptor);
                                 logger.info(String.format("%s was removed from adapter user location", descriptor.getAlias()));
                             });
        super.uninstalled();
    }

    private Map<String, File> getJarAdapters(File fromPath) {
        Map<String, File> output = new HashMap<>();
        File[] files = fromPath.listFiles(jarFilter);
        if (files != null) {
            logger.info(String.format("Found %s packed user module" + (files.length > 1 ? "s" : ""), String.valueOf(files.length)));
            try {

                for (File file : files) {
                    JarFile jarFile = new JarFile(file);
                    Manifest manifest = jarFile.getManifest();
                    Attributes manifestEntries = manifest.getMainAttributes();
                    if (manifestEntries.containsKey(typeKey) &&
                            "STA".equals(manifestEntries.getValue(typeKey.toString()))) {
                        logger.info(String.format("Module %s was detected as a STA module", file.getName()));
                        output.put(manifestEntries.getValue(descriptionKeyName), file);
                    }
                }
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }
        }
        return output;
    }
}
