package org.esa.beam.visat.actions.masktools;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class MagicWandModelTest {

    private Product product;
    private Band b1;
    private Band b2;
    private Band b3;

    @Before
    public void setUp() throws Exception {
        product = new Product("product", "t", 16, 16);
        product.addBand("a1", ProductData.TYPE_FLOAT32);
        b1 = product.addBand("b1", ProductData.TYPE_FLOAT32);
        product.addBand("a2", ProductData.TYPE_FLOAT32);
        b2 = product.addBand("b2", ProductData.TYPE_FLOAT32);
        product.addBand("a3", ProductData.TYPE_FLOAT32);
        b3 = product.addBand("b3", ProductData.TYPE_FLOAT32);
        b1.setSpectralWavelength(100);
        b2.setSpectralWavelength(200);
        b3.setSpectralWavelength(300);
    }

    @Test
    public void testConstructorSetsDefaultValues() throws Exception {
        MagicWandModel model = new MagicWandModel();
        assertEquals(MagicWandModel.PickMode.SINGLE, model.getPickMode());
        assertEquals(MagicWandModel.PixelTest.DISTANCE, model.getPixelTest());
        assertEquals(0.1, model.getTolerance(), 0.0);
        assertEquals("0", model.createMaskExpression());
    }

    @Test
    public void testGetSpectralBands() throws Exception {
        MagicWandModel model = new MagicWandModel();
        List<Band> bands;

        bands = model.getBands(product);
        assertEquals(0, bands.size());

        model.setBandNames("b1", "b2", "b3");
        bands = model.getBands(product);
        assertEquals(3, bands.size());
        assertSame(b1, bands.get(0));
        assertSame(b2, bands.get(1));
        assertSame(b3, bands.get(2));

        model.setBandNames("b1", "c2", "b3");
        bands = model.getBands(product);
        assertEquals(null, bands);
    }

    @Test
    public void testCreateExpressionWith3Bands() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setBandNames("b1", "b2", "b3");
        model.addSpectrum(0.4, 0.3, 0.2);
        model.setTolerance(0.3);
        String expression = model.createMaskExpression();
        assertEquals("distance(b1,b2,b3,0.4,0.3,0.2)/3 < 0.3", expression);

        model.setNormalize(true);
        expression = model.createMaskExpression();
        assertTrue(expression.startsWith("distance(b1/b1,b2/b1,b3/b1,1.0,0."));
        assertTrue(expression.endsWith(")/3 < 0.3"));
    }

    @Test
    public void testCreateExpressionWith1Band() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setBandNames("a2");
        model.addSpectrum(0.2);
        model.setTolerance(0.05);
        String expression = model.createMaskExpression();
        assertEquals("distance(a2,0.2) < 0.05", expression);

        model.setNormalize(true);
        expression = model.createMaskExpression();
        assertEquals("distance(a2/a2,1.0) < 0.05", expression);
    }

    @Test
    public void testCreateExpressionDistIdent() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.setPixelTest(MagicWandModel.PixelTest.DISTANCE);
        model.setSpectrumTransform(MagicWandModel.SpectrumTransform.IDENTITY);
        model.setBandNames("b1", "b2", "b3");
        model.addSpectrum(0.4, 0.3, 0.2);
        model.addSpectrum(0.6, 0.9, 0.7);
        model.setTolerance(0.25);
        String expression = model.createMaskExpression();
        assertEquals("distance(b1,b2,b3,0.4,0.3,0.2)/3 < 0.25" +
                             " || distance(b1,b2,b3,0.6,0.9,0.7)/3 < 0.25", expression);

        model.setNormalize(true);
        expression = model.createMaskExpression();
        assertTrue(expression.startsWith("distance(b1/b1,b2/b1,b3/b1,1.0,0."));
        assertTrue(expression.endsWith(")/3 < 0.25"));
    }

    @Test
    public void testCreateExpressionDistDeriv() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setBandNames("b1", "b2", "b3");
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.setPixelTest(MagicWandModel.PixelTest.DISTANCE);
        model.setSpectrumTransform(MagicWandModel.SpectrumTransform.DERIVATIVE);
        model.addSpectrum(0.4, 0.3, 0.2);
        model.addSpectrum(0.6, 0.9, 0.7);
        model.setTolerance(0.25);
        String expression = model.createMaskExpression();
        assertEquals("distance_deriv(b1,b2,b3,0.4,0.3,0.2)/3 < 0.25" +
                             " || distance_deriv(b1,b2,b3,0.6,0.9,0.7)/3 < 0.25", expression);

        model.setNormalize(true);
        expression = model.createMaskExpression();
        assertTrue(expression.startsWith("distance_deriv(b1/b1,b2/b1,b3/b1,1.0,0."));
        assertTrue(expression.endsWith(")/3 < 0.25"));
    }

    @Test
    public void testCreateExpressionDistInteg() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.setPixelTest(MagicWandModel.PixelTest.DISTANCE);
        model.setSpectrumTransform(MagicWandModel.SpectrumTransform.INTEGRAL);
        model.setBandNames("b1", "b2", "b3");
        model.addSpectrum(0.4, 0.3, 0.2);
        model.addSpectrum(0.6, 0.9, 0.7);
        model.setTolerance(0.25);
        String expression = model.createMaskExpression();
        assertEquals("distance_integ(b1,b2,b3,0.4,0.3,0.2)/3 < 0.25" +
                             " || distance_integ(b1,b2,b3,0.6,0.9,0.7)/3 < 0.25", expression);

        model.setNormalize(true);
        expression = model.createMaskExpression();
        assertTrue(expression.startsWith("distance_integ(b1/b1,b2/b1,b3/b1,1.0,0."));
        assertTrue(expression.endsWith(")/3 < 0.25"));
    }

    @Test
    public void testCreateExpressionLimitsIdent() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.setPixelTest(MagicWandModel.PixelTest.LIMITS);
        model.setSpectrumTransform(MagicWandModel.SpectrumTransform.IDENTITY);
        model.setBandNames("b1", "b2", "b3");
        model.addSpectrum(0.4, 0.3, 0.2);
        model.addSpectrum(0.6, 0.9, 0.7);
        model.setTolerance(0.1);
        String expression = model.createMaskExpression();

        // Test: assertEquals("inrange(b1,b2,b3,0.3,0.2,0.1,0.7,1.0,0.8)", expression);
        Pattern pattern = Pattern.compile("inrange\\(b1,b2,b3,([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*)\\)");
        Matcher matcher = pattern.matcher(expression);
        assertTrue(expression, matcher.find());
        assertEquals(expression, 6, matcher.groupCount());
        assertEquals(expression, 0.3, Double.parseDouble(matcher.group(1)), 1e-8);
        assertEquals(expression, 0.2, Double.parseDouble(matcher.group(2)), 1e-8);
        assertEquals(expression, 0.1, Double.parseDouble(matcher.group(3)), 1e-8);
        assertEquals(expression, 0.7, Double.parseDouble(matcher.group(4)), 1e-8);
        assertEquals(expression, 1.0, Double.parseDouble(matcher.group(5)), 1e-8);
        assertEquals(expression, 0.8, Double.parseDouble(matcher.group(6)), 1e-8);

        model.setNormalize(true);
        expression = model.createMaskExpression();

        // Test: assertEquals("inrange(b1/b1,b2/b1,b3/b1,0.9,0.6499999999999999,0.4,1.1,1.6,1.2666666666666668)", expression);
        pattern = Pattern.compile("inrange\\(b1/b1,b2/b1,b3/b1,([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*)\\)");
        matcher = pattern.matcher(expression);
        assertTrue(expression, matcher.find());
        assertEquals(expression, 6, matcher.groupCount());
        assertEquals(expression, 0.9, Double.parseDouble(matcher.group(1)), 1e-8);
        assertEquals(expression, 0.65, Double.parseDouble(matcher.group(2)), 1e-8);
        assertEquals(expression, 0.4, Double.parseDouble(matcher.group(3)), 1e-8);
        assertEquals(expression, 1.1, Double.parseDouble(matcher.group(4)), 1e-8);
        assertEquals(expression, 1.6, Double.parseDouble(matcher.group(5)), 1e-8);
        assertEquals(expression, 1.26666666, Double.parseDouble(matcher.group(6)), 1e-8);
    }

    @Test
    public void testCreateExpressionLimitsDeriv() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.setPixelTest(MagicWandModel.PixelTest.LIMITS);
        model.setSpectrumTransform(MagicWandModel.SpectrumTransform.DERIVATIVE);
        model.setBandNames("b1", "b2", "b3");
        model.addSpectrum(0.4, 0.3, 0.2);
        model.addSpectrum(0.6, 0.9, 0.7);
        model.setTolerance(0.1);
        String expression = model.createMaskExpression();

        // Test: assertEquals("inrange_deriv(b1,b2,b3,0.3,0.2,0.1,0.7,1.0,0.8)", expression);
        Pattern pattern = Pattern.compile("inrange_deriv\\(b1,b2,b3,([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*)\\)");
        Matcher matcher = pattern.matcher(expression);
        assertTrue(expression, matcher.find());
        assertEquals(expression, 6, matcher.groupCount());
        assertEquals(expression, 0.3, Double.parseDouble(matcher.group(1)), 1e-8);
        assertEquals(expression, 0.2, Double.parseDouble(matcher.group(2)), 1e-8);
        assertEquals(expression, 0.1, Double.parseDouble(matcher.group(3)), 1e-8);
        assertEquals(expression, 0.7, Double.parseDouble(matcher.group(4)), 1e-8);
        assertEquals(expression, 1.0, Double.parseDouble(matcher.group(5)), 1e-8);
        assertEquals(expression, 0.8, Double.parseDouble(matcher.group(6)), 1e-8);

        model.setNormalize(true);
        expression = model.createMaskExpression();

        // Test: assertEquals("inrange_deriv(b1,b2,b3,0.3,0.2,0.1,0.7,1.0,0.8)", expression);
        pattern = Pattern.compile("inrange_deriv\\(b1/b1,b2/b1,b3/b1,([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*)\\)");
        matcher = pattern.matcher(expression);
        assertTrue(expression, matcher.find());
        assertEquals(expression, 6, matcher.groupCount());
        assertEquals(expression, 0.9, Double.parseDouble(matcher.group(1)), 1e-8);
        assertEquals(expression, 0.65, Double.parseDouble(matcher.group(2)), 1e-8);
        assertEquals(expression, 0.4, Double.parseDouble(matcher.group(3)), 1e-8);
        assertEquals(expression, 1.1, Double.parseDouble(matcher.group(4)), 1e-8);
        assertEquals(expression, 1.6, Double.parseDouble(matcher.group(5)), 1e-8);
        assertEquals(expression, 1.26666666, Double.parseDouble(matcher.group(6)), 1e-8);
    }

    @Test
    public void testCreateExpressionLimitsInteg() throws Exception {
        MagicWandModel model = new MagicWandModel();
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.setPixelTest(MagicWandModel.PixelTest.LIMITS);
        model.setSpectrumTransform(MagicWandModel.SpectrumTransform.INTEGRAL);
        model.setBandNames("b1", "b2", "b3");
        model.addSpectrum(0.4, 0.3, 0.2);
        model.addSpectrum(0.6, 0.9, 0.7);
        model.setTolerance(0.1);
        String expression = model.createMaskExpression();

        // Test: assertEquals("inrange_integ(b1,b2,b3,0.3,0.2,0.1,0.7,1.0,0.8)", expression);
        Pattern pattern = Pattern.compile("inrange_integ\\(b1,b2,b3,([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*)\\)");
        Matcher matcher = pattern.matcher(expression);
        assertTrue(expression, matcher.find());
        assertEquals(expression, 6, matcher.groupCount());
        assertEquals(expression, 0.3, Double.parseDouble(matcher.group(1)), 1e-8);
        assertEquals(expression, 0.2, Double.parseDouble(matcher.group(2)), 1e-8);
        assertEquals(expression, 0.1, Double.parseDouble(matcher.group(3)), 1e-8);
        assertEquals(expression, 0.7, Double.parseDouble(matcher.group(4)), 1e-8);
        assertEquals(expression, 1.0, Double.parseDouble(matcher.group(5)), 1e-8);
        assertEquals(expression, 0.8, Double.parseDouble(matcher.group(6)), 1e-8);

        model.setNormalize(true);
        expression = model.createMaskExpression();

        // Test: assertEquals("inrange_integ(b1,b2,b3,0.3,0.2,0.1,0.7,1.0,0.8)", expression);
        pattern = Pattern.compile("inrange_integ\\(b1/b1,b2/b1,b3/b1,([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*),([0-9]*\\.[0-9]*)\\)");
        matcher = pattern.matcher(expression);
        assertTrue(expression, matcher.find());
        assertEquals(expression, 6, matcher.groupCount());
        assertEquals(expression, 0.9, Double.parseDouble(matcher.group(1)), 1e-8);
        assertEquals(expression, 0.65, Double.parseDouble(matcher.group(2)), 1e-8);
        assertEquals(expression, 0.4, Double.parseDouble(matcher.group(3)), 1e-8);
        assertEquals(expression, 1.1, Double.parseDouble(matcher.group(4)), 1e-8);
        assertEquals(expression, 1.6, Double.parseDouble(matcher.group(5)), 1e-8);
        assertEquals(expression, 1.26666666, Double.parseDouble(matcher.group(6)), 1e-8);
    }

    @Test
    public void testClone() throws Exception {
        final MagicWandModel model = new MagicWandModel();
        model.setBandNames("b1", "b2", "b3", "a1", "a2", "a3");
        model.setTolerance(0.005);
        model.setMinTolerance(0.0);
        model.setMaxTolerance(0.01);
        model.setNormalize(true);
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.addSpectrum(1, 2, 3, 4, 5, 6);
        model.addSpectrum(2, 3, 4, 5, 6, 7);
        model.setPickMode(MagicWandModel.PickMode.MINUS);
        model.addSpectrum(3, 4, 5, 6, 7, 8);
        model.addSpectrum(4, 5, 6, 7, 8, 9);

        final MagicWandModel modelCopy = model.clone();
        assertEquals(model, modelCopy);
    }

    @Test
    public void testXml() throws Exception {
        final MagicWandModel model = new MagicWandModel();
        model.setBandNames("b1", "b2", "b3", "a1", "a2", "a3");
        model.setTolerance(0.005);
        model.setMinTolerance(0.0);
        model.setMaxTolerance(0.01);
        model.setNormalize(true);
        model.setPickMode(MagicWandModel.PickMode.PLUS);
        model.addSpectrum(1, 2, 3, 4, 5, 6);
        model.addSpectrum(2, 3, 4, 5, 6, 7);
        model.setPickMode(MagicWandModel.PickMode.MINUS);
        model.addSpectrum(3, 4, 5, 6, 7, 8);
        model.addSpectrum(4, 5, 6, 7, 8, 9);

        final String xml = model.toXml();
        final MagicWandModel modelCopy = MagicWandModel.fromXml(xml);
        assertEquals(model, modelCopy);
    }
}
