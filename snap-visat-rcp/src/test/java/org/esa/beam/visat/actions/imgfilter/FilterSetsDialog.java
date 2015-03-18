package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.actions.imgfilter.model.Filter;
import org.esa.beam.visat.actions.imgfilter.model.FilterSet;

import javax.swing.JDialog;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;

/**
 * @author Norman
 */
public class FilterSetsDialog implements FilterSetForm.Listener {

    private Window parentWindow;
    private FilterSet[] filterSets;
    private JDialog dialog;

    public FilterSetsDialog(Window parentWindow, FilterSet... filterSets) {
        this.parentWindow = parentWindow;
        this.filterSets = filterSets;
    }

    public void show() {
        if (dialog == null) {
            dialog = new JDialog(parentWindow, FilterSetsDialog.class.getSimpleName());
            FilterSetsForm filterSetsForm = new FilterSetsForm("wraw", this, new FilterSetFileStore(getFiltersDir()), new FilterWindow(dialog), filterSets);
            dialog.setContentPane(filterSetsForm);
            dialog.setBounds(new Rectangle(100,100,300, 300));
            //dialog.pack();
        }
        dialog.setVisible(true);
    }

    public void hide() {
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

    @Override
    public void filterSelected(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelSelected: filterModel = " + filter);
    }

    @Override
    public void filterAdded(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelAdded: filterModel = " + filter);
    }

    @Override
    public void filterRemoved(FilterSet filterSet, Filter filter) {
        System.out.println("filterModelRemoved: filterModel = " + filter);
    }

    @Override
    public void filterChanged(FilterSet filterSet, Filter filter, String propertyName) {
        System.out.println("filterModelChanged: filterModel = " + filter);
    }

    private File getFiltersDir() {
        return new File(SystemUtils.getApplicationDataDir(), "snap-ui/auxdata/image-filters");
    }


}
