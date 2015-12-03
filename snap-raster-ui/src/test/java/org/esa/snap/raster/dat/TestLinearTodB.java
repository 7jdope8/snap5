/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.raster.dat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.esa.snap.raster.rcp.actions.LinearTodBAction;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for LinearTodB.
 */
public class TestLinearTodB {

    private static final String dBStr = "_" + Unit.DB;

    @Test
    public void testLinearTodB() {

        final Product product = createTestProduct(16, 4);
        final Band band1 = product.getBandAt(0);

        LinearTodBAction.convert(product, band1, true);
        assertTrue(product.getNumBands() == 2);

        final Band band2 = product.getBandAt(1);
        assertTrue(band2.getUnit().endsWith(dBStr));
        assertTrue(band2.getName().endsWith(dBStr));
    }

    @Test
    public void testdBToLinear() {

        final Product product = createTestProduct(16, 4);
        final Band band1 = product.getBandAt(0);
        band1.setName(band1.getName() + dBStr);
        band1.setUnit(band1.getUnit() + dBStr);

        LinearTodBAction.convert(product, band1, false);
        assertTrue(product.getNumBands() == 2);

        final Band band2 = product.getBandAt(1);
        assertTrue(band2.getUnit().equals(Unit.AMPLITUDE));
        assertTrue(band2.getName().equals("Amplitude"));
    }

    /**
     * @param w width
     * @param h height
     * @return the created product
     */
    private static Product createTestProduct(int w, int h) {

        final Product testProduct = TestUtils.createProduct("ASA_APG_1P", w, h);

        // create a Band: band1
        final Band band1 = testProduct.addBand("Amplitude", ProductData.TYPE_INT32);
        band1.setUnit(Unit.AMPLITUDE);
        final int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band1.setData(ProductData.createInstance(intValues));

        final float[] incidence_angle = new float[64];
        Arrays.fill(incidence_angle, 30.0f);
        testProduct.addTiePointGrid(new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, 16, 4, 0, 0, 1, 1, incidence_angle));

        return testProduct;
    }
}
