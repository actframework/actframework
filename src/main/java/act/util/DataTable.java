package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.annotations.Label;
import org.osgl.$;
import org.osgl.util.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * A `DataTable` contains a list of heading (column labels) plus
 * a list of data rows.
 * <p>
 * The sequence of data fields in each data row matches the corresponding
 * sequence of field labels in heading list.
 * <p>
 * **Note**
 * 1. `DataTable` does data processing in memory, it is not suitable
 * for huge amount of data.
 * 2. `DataTable` does not suit big sparse matrix unless heading is
 * provided with {@link PropertySpec} or the row data type is a concrete
 * POJO. In case sparse matrix row is supplied with {@link Map} or
 * {@link org.osgl.util.AdaptiveMap}, `DataTable` will try to probe at
 * most 10 first rows to find out the heading fields(keys). Thus if
 * the first 10 rows has missing columns, they will not be able to output
 */
public class DataTable implements Iterable {

    public static final Keyword HTML_TABLE = Keyword.of("html-table");

    public static final String KEY_FIELD = "_field";

    /**
     * A pseudo column key for the entire data object
     * - used with data table of {@link org.osgl.Lang#isSimpleType(Class) simple types}.
     */
    public static final String KEY_THIS = "_this_";

    private List<String> colKeys;
    private Iterable rows;
    private int rowCount;
    private boolean isMap;
    private boolean isAdaptiveMap;
    private boolean isTranspose;
    private Object firstRow;
    private Set<String> rightAlignCols;
    // key: col key; val: label
    private C.Map<String, String> labelLookup;
    // key: label, val: col key
    private C.Map<String, String> reverseLabelLookup;

    // the transpose table of this table
    private transient volatile DataTable transpose;

    // used to build transpose table
    private DataTable() {
    }

    /**
     * Construct a `DataTable` with data.
     * <p>
     * This constructor will call `{@link ThreadLocal#get()}` on
     * {@link PropertySpec#currentSpec} to get the current
     * PropertySpec meta info.
     *
     * @param data the data used to populate the data table
     */
    public DataTable(Object data) {
        init(data, PropertySpec.currentSpec.get());
    }

    /**
     * Construct a `DataTable` with data and {@link PropertySpec.MetaInfo column spec}.
     *
     * @param data    the data used to populate the data table
     * @param colSpec the {@link PropertySpec.MetaInfo} specifies column headings
     */
    public DataTable(Object data, PropertySpec.MetaInfo colSpec) {
        init(data, colSpec);
    }

    /**
     * Return number of rows in the data table.
     * <p>
     * If {@link #rows} is an iterable and cannot be cast to a collection, then
     * it returns `-1`, meaning row number cannot be count.
     *
     * @return the number of rows in the table
     */
    public int rowCount() {
        return rowCount;
    }

    /**
     * Return number of columns in the data table.
     *
     * @return the number of columns.
     */
    public int colCount() {
        return colKeys.size();
    }

    /**
     * Return {@link Iterator} of {@link #rows}.
     *
     * @return the row iterator.
     */
    @Override
    public Iterator iterator() {
        return rows.iterator();
    }

    /**
     * Get the heading row.
     *
     * @return the heading row
     */
    public List<String> heading() {
        if (isTranspose) {
            return colKeys;
        }
        List<String> heading = new ArrayList<>();
        List<String> colKeys = new ArrayList(this.colKeys);
        if (colKeys.remove("id")) {
            String idLabel = labelLookup.get("id");
            if (null != idLabel) {
                heading.add(idLabel);
            } else {
                heading.add("id");
            }
        }
        for (String colKey : colKeys) {
            String label = labelLookup.get(colKey);
            heading.add(null != label ? label : colKey);
        }
        return heading;
    }

    /**
     * Get column value on a row by col index.
     *
     * @param row      the row object
     * @param colIndex the column index
     * @return the value of the column on the row
     */
    public Object val(Object row, int colIndex) {
        String colKey = colKeys.get(colIndex);
        return $.getProperty(row, colKey);
    }

    /**
     * Get column value on a row by label.
     *
     * @param row   the row object
     * @param label the column label
     * @return the value of the column on the row
     */
    public Object val(Object row, String label) {
        if (KEY_THIS == label) {
            return row;
        }
        String colKey = label;
        if (!isTranspose) {
            colKey = reverseLabelLookup.get(label);
            if (null == colKey) {
                colKey = label;
            }
        }
        return $.getProperty(row, colKey);
    }

    /**
     * Get a transpose table of this table.
     *
     * @return a transpose table of this table.
     */
    public DataTable transpose() {
        if (null == transpose) {
            synchronized (this) {
                if (null == transpose) {
                    transpose = buildTranspose();
                }
            }
        }
        return transpose;
    }

    /**
     * Check if a label is right aligned column.
     * @param label the label to check
     * @return `true` if the column of the label should be right aligned.
     */
    public boolean isRightAligned(String label) {
        if (isTranspose) {
            return false;
        }
        String key = reverseLabelLookup.get(label);
        if (null == key) {
            key = label;
        }
        return rightAlignCols.contains(key);
    }

    /**
     * Reports if this data table is a transposed table.
     *
     * @return `true` if this table is transposed, or `false` otherwise.
     */
    public boolean isTransposed() {
        return isTranspose;
    }

    private DataTable buildTranspose() {
        E.illegalArgumentIf(rowCount < 0, "transpose table not applied to table does not have row count.");
        DataTable transpose = new DataTable();
        transpose = isTranspose() ? buildReverseTranspose(transpose) : buildTranspose(transpose);
        transpose.labelLookup = this.labelLookup;
        transpose.reverseLabelLookup = this.reverseLabelLookup;
        transpose.isTranspose = !this.isTranspose;
        return transpose;
    }

    private DataTable buildReverseTranspose(DataTable target) {
        target.colKeys = buildReverseTransposeColKeys();
        target.rowCount = this.colKeys.size() - 1;
        target.rows = buildReverseTransposeRows(target.rowCount);
        return target;
    }

    private DataTable buildTranspose(DataTable target) {
        target.colKeys = buildTransposeColKeys();
        target.rowCount = this.colKeys.size();
        target.rows = buildTransposeRows();
        return target;
    }

    private List<String> buildReverseTransposeColKeys() {
        List<String> colKeys = new ArrayList<>(this.rowCount);
        for (Object row : rows) {
            colKeys.add(S.string($.getProperty(row, KEY_FIELD)));
        }
        return colKeys;
    }

    private List<String> buildTransposeColKeys() {
        List<String> colKeys = new ArrayList<>(this.rowCount + 1);
        colKeys.add(KEY_FIELD);
        for (int i = 0; i < this.rowCount; ++i) {
            colKeys.add("_r" + i);
        }
        return colKeys;
    }

    private Map<String, Object> buildTransposeRow(String colKey) {
        Map<String, Object> row = new HashMap<>(this.rowCount + 1);
        String label = labelLookup.get(colKey);
        if (null == label) {
            label = colKey;
        }
        row.put(KEY_FIELD, label);
        int rowId = 0;
        for (Object data : this.rows) {
            row.put("_r" + rowId++, $.getProperty(data, colKey));
        }
        return row;
    }

    private Iterable buildTransposeRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : colKeys) {
            rows.add(buildTransposeRow(key));
        }
        return rows;
    }

    private Iterable buildReverseTransposeRows(int targetRowCount) {
        List<Map<String, Object>> rows = new ArrayList<>(targetRowCount);
        for (int i = 0; i < targetRowCount; ++i) {
            String key = "_r" + i;
            rows.add(buildReverseTransposeRow(key));
        }
        return rows;
    }

    private Map<String, Object> buildReverseTransposeRow(String key) {
        Map<String, Object> row = new HashMap<>();
        for (Object data : this.rows) {
            String targetKey = $.getProperty(data, KEY_FIELD);
            Object targetVal = $.getProperty(data, key);
            row.put(targetKey, targetVal);
        }
        return row;
    }

    private boolean isTranspose() {
        return isTranspose;
    }

    private void init(Object data, PropertySpec.MetaInfo colSpec) {
        initRightAlignCols();
        initRows(data);
        initHeading(data, colSpec);
    }

    private void initRightAlignCols() {
        this.rightAlignCols = new HashSet<>();
    }

    private void initRows(Object data) {
        if (data instanceof Collection) {
            Collection<?> col = $.cast(data);
            rowCount = col.size();
            rows = col;
        } else if (data instanceof Iterable) {
            Iterable iterable = $.cast(data);
            if (iterable.iterator() == iterable) {
                initRows(C.list(iterable));
                return;
            }
            firstRow = iterable.iterator().hasNext() ? iterable.iterator().next() : null;
            rows = $.cast(data);
            rowCount = -1;
        } else {
            rows = C.list(data);
            rowCount = 1;
            firstRow = data;
        }
        if (rowCount > 0) {
            firstRow = rows.iterator().next();
        }
        if (null != firstRow) {
            isMap = firstRow instanceof Map;
            isAdaptiveMap = !isMap && firstRow instanceof AdaptiveMap;
        }
    }

    // pre-condition - `initRows(Object)` method must be called to setup the context
    // for exploring output fields in case heading is not specified with `PropertySpec`
    private void initHeading(Object data, PropertySpec.MetaInfo colSpec) {
        Set<String> excludes = C.Set();
        boolean headingLoaded = false;
        boolean colSpecPresented = null != colSpec;
        if (colSpecPresented) {
            ActContext<?> context = ActContext.Base.currentContext();
            setLabelLookup(colSpec.labelMapping(context));
            excludes = colSpec.excludedFields(context);
            if ($.not(excludes)) {
                colKeys = colSpec.outputFields(context);
                if ($.bool(colKeys)) {
                    headingLoaded = true;
                }
            }
            colSpecPresented = $.bool(colKeys) || $.bool(excludes);
        }
        if (null == labelLookup) {
            setLabelLookup(C.<String, String>newMap());
        }
        // explore data rows to probe fields
        E.illegalArgumentIf(0 == rowCount, "Unable to probe table heading: no data found");
        int max = Math.min(rowCount, 10); // probe at most 10 rows of data for labels
        Set<String> keys;
        if (isPojo()) {
            if (null == firstRow) {
                firstRow = rows.iterator().next();
            }
            keys = keysOf(firstRow, colSpecPresented);
        } else {
            keys = new TreeSet<>();
            if (max < 0) {
                max = 10;
            }
            for (Object row : rows) {
                if (--max < 0) break;
                if (isMap) {
                    keys.addAll(keysOf((Map) row));
                } else if (isAdaptiveMap) {
                    keys.addAll(keysOf(((AdaptiveMap) row).asMap()));
                } else {
                    assert false;
                }
            }
        }
        if (!headingLoaded) {
            keys.removeAll(excludes);
            colKeys = C.list(keys);
        }
    }

    private boolean isNumeric(Object v) {
        if (null == v) {
            return false;
        }
        if (v instanceof Number) {
            return true;
        }
        if (v instanceof $.Var) {
            v = (($.Var) v).get();
            return isNumeric(v);
        }
        if (v instanceof Const) {
            v = ((Const) v).get();
            return isNumeric(v);
        }
        return false;
    }

    private Set<String> keysOf(Map<?, ?> row) {
        Set<String> set = new HashSet<>();
        for (Map.Entry entry: row.entrySet()) {
            String key = S.string(entry.getKey());
            set.add(key);
            if (rightAlignCols.contains(key)) {
                continue;
            }
            Object v = entry.getValue();
            if (isNumeric(v)) {
                rightAlignCols.add(key);
            }
        }
        return set;
    }

    private boolean isNumeric(Class<?> type, Type genericType) {
        if (isNumeric(type)) {
            return true;
        }
        if ($.Var.class.isAssignableFrom(type) || Const.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = $.cast(genericType);
                Type[] tArgs = pt.getActualTypeArguments();
                if (null != tArgs && tArgs.length > 0) {
                    Type t = tArgs[0];
                    if (t instanceof Class) {
                        return isNumeric((Class<?>) t);
                    }
                }
            }
        }
        return false;
    }

    private boolean isNumeric(Class<?> ft) {
        if ($.isSimpleType(ft)) {
            ft = $.wrapperClassOf(ft);
        }
        return Number.class.isAssignableFrom(ft);
    }

    private Set<String> keysOf(Object pojo, boolean propSpecPresented) {
        Class<?> type = pojo.getClass();
        Set<String> keys = propSpecPresented ? new LinkedHashSet<String>() : new TreeSet<String>();
        if ($.isSimpleType(type)) {
            keys.add(KEY_THIS);
            return keys;
        }
        // Check all public fields
        for (Field f : type.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            String fn = f.getName();
            keys.add(fn);
            if (!labelLookup.containsKey(fn)) {
                Label label = f.getAnnotation(Label.class);
                if (null != label) {
                    addLabelLookup(fn, label.value());
                }
            }
            Class<?> ft = f.getType();
            Type gft = f.getGenericType();
            if (isNumeric(ft, gft)) {
                rightAlignCols.add(fn);
            }
        }
        // Check all getters
        for (Method m : type.getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getParameterTypes().length > 0) {
                continue;
            }
            if (m.getReturnType().equals(Void.class)) {
                continue;
            }
            String mn = m.getName();
            if (S.eq("getClass", mn)) {
                continue;
            }
            if (mn.length() < 4 || !mn.startsWith("get") || !Character.isUpperCase(mn.charAt(3))) {
                continue;
            }
            S.Buffer buf = S.buffer(Character.toLowerCase(mn.charAt(3)));
            buf.a(mn.substring(4));
            String key = buf.toString();
            keys.add(key);
            if (!labelLookup.containsKey(key)) {
                Label label = m.getAnnotation(Label.class);
                if (null != label) {
                    addLabelLookup(key, label.value());
                }
            }
            Class<?> mt = m.getReturnType();
            Type gmt = m.getGenericReturnType();
            if (isNumeric(mt, gmt)) {
                rightAlignCols.add(key);
            }
        }
        return keys;
    }

    private boolean isPojo() {
        return !isMap && !isAdaptiveMap;
    }

    private void setLabelLookup(Map<String, String> lookup) {
        this.labelLookup = C.newMap(lookup);
        this.reverseLabelLookup = this.labelLookup.flipped();
    }

    private void addLabelLookup(String colKey, String label) {
        this.labelLookup.put(colKey, label);
        this.reverseLabelLookup.put(label, colKey);
    }
}
