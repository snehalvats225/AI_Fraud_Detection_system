package fraud.detection.util;

import java.util.List;
import java.util.function.Function;
import javax.swing.table.DefaultTableModel;

/**
 * Converts a List of ANY type into a read-only JTable model.
 * <T> makes this a GENERIC METHOD: it works for List<Transaction>, List<FraudCase>,
 * List<ModelVersion> ... The mapper says how one object becomes one row.
 * Column classes are taken from the first row so numbers sort as numbers.
 */
public final class TableModelBuilder {

    private TableModelBuilder() {
    }

    public static <T> DefaultTableModel build(String[] columns, List<T> rows, Function<T, Object[]> mapper) {
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = mapper.apply(rows.get(i));
        }
        final Class<?>[] types = new Class<?>[columns.length];
        for (int c = 0; c < columns.length; c++) {
            types[c] = data.length > 0 && data[0][c] != null ? data[0][c].getClass() : Object.class;
        }
        return new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return types[column];
            }
        };
    }
}
