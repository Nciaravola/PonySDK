
package com.ponysdk.core.ui.datagrid2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Interval;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

/**
 *
 */
public class SimpleCacheDataSource<K, V> extends SimpleDataSource<K, V> {

    protected final Map<K, Row<V>> cache = new HashMap<>();
    private final List<Row<V>> liveData = new ArrayList<>();

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------------------ Row Getters --------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public Row<V> getRow(final K k) {
        return cache.get(k);
    }

    @Override
    public Collection<Row<V>> getRows() {
        return cache.values();
    }

    @Override
    public List<Row<V>> getRows(final int index, int size) {
        // Reset size so that it doesn't exceed boundaries
        size = index + size > liveData.size() ? liveData.size() - index : size;
        final List<Row<V>> tmp = new ArrayList<>();
        for (int i = index; i < index + size; i++) {
            tmp.add(liveData.get(i));
        }
        return tmp;
    }

    @Override
    public int getRowCount() {
        return liveData.size();
    }

    //----------------------------------------------------------------------------------------------------------//
    //-------------------------------------- Gestion de cahce, liveData ----------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    // Insert data in the cache or update it if it already exists
    @Override
    public Interval setData(final V v) {
        Objects.requireNonNull(v);
        final K k = adapter.getKey(v);
        final Row<V> row = cache.get(k);
        Interval interval;
        if (row != null) {
            if (row.data == v) return null;
            interval = updateData(k, row, v);
        } else {
            interval = insertData(k, v);
        }
        return interval;
    }

    // Rows are updated when they already exist
    private Interval updateData(final K k, final Row<V> row, final V newV) {
        if (row.accepted) {
            final int oldLiveDataSize = liveData.size();
            final int oldRowIndex = removeRow(liveData, row);
            final boolean selected = selectedKeys.contains(k);
            if (selected) removeRow(liveSelectedData, row);
            row.data = newV;
            return onWasAcceptedAndRemoved(selected, row, oldLiveDataSize, oldRowIndex);
        } else {
            row.data = newV;
            return onWasNotAccepted(k, row);
        }
    }

    @Override
    public Interval updateData(final K k, final Consumer<V> updater) {
        final Row<V> row = cache.get(k);
        if (row == null) return null;
        if (row.accepted) {
            final int oldLiveDataSize = liveData.size();
            final int oldRowIndex = removeRow(liveData, row);
            final boolean selected = selectedKeys.contains(k);
            if (selected) removeRow(liveSelectedData, row);
            updater.accept(row.data);
            return onWasAcceptedAndRemoved(selected, row, oldLiveDataSize, oldRowIndex);
        } else {
            updater.accept(row.data);
            return onWasNotAccepted(k, row);
        }
    }

    @Override
    public V removeData(final K k) {

        final Row<V> row = cache.remove(k);
        final boolean selected = selectedKeys.remove(k);
        if (row.accepted) {
            //            final int oldLiveDataSize = liveData.size();
            //            final int rowIndex = removeRow(liveData, row);
            if (selected) {
                removeRow(liveSelectedData, row);
            }
            //            refreshRows(rowIndex, oldLiveDataSize);
        }
        return row.data;
    }

    // Here rows are created and inserted in liveData
    private Interval insertData(final K k, final V data) {
        final Row<V> row = new Row<>(rowCounter++, data);
        row.accepted = accept(row);
        cache.put(k, row);
        if (!row.accepted) return null;
        final int rowIndex = insertRow(liveData, row);
        return new Interval(rowIndex, liveData.size());
    }

    private Interval onWasAcceptedAndRemoved(final boolean selected, final Row<V> row, final int oldLiveDataSize,
                                             final int oldRowIndex) {
        clearRenderingHelpers(row);
        if (accept(row)) {
            final int rowIndex = insertRow(liveData, row);
            if (selected) insertRow(liveSelectedData, row);
            if (oldRowIndex <= rowIndex) {
                return new Interval(oldRowIndex, rowIndex + 1);
            } else {
                return new Interval(rowIndex, oldRowIndex + 1);
            }
        } else {
            row.accepted = false;
            return new Interval(oldRowIndex, oldLiveDataSize);
        }
    }

    private Interval onWasNotAccepted(final K k, final Row<V> row) {
        clearRenderingHelpers(row);
        if (accept(row)) {
            row.accepted = true;
            final int rowIndex = insertRow(liveData, row);
            if (selectedKeys.contains(k)) insertRow(liveSelectedData, row);
            return new Interval(rowIndex, liveData.size());
        } //else do nothing
        return null;
    }

    private void clearRenderingHelpers(final Row<V> row) {
        renderingHelpersCache.remove(row);
    }

    @Override
    public void resetLiveData() {
        liveSelectedData.clear();
        liveData.clear();
        for (final Row<V> row : cache.values()) {
            row.accepted = accept(row);
            if (row.accepted) {
                insertRow(liveData, row);
                if (selectedKeys.contains(adapter.getKey(row.data))) {
                    insertRow(liveSelectedData, row);
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------------------------ Sorting -------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void sort() {
        super.sort();
        liveData.sort(this::compare);
    }

    @Override
    public String toString() {
        return cache.toString();
    }

    @Override
    public void forEach(final BiConsumer<K, V> action) {
        cache.forEach((k, r) -> action.accept(k, r.data));
    }

    @Override
    public void selectAllLiveData() {
        liveSelectedData.clear();
        for (final Row<V> row : liveData) {
            liveSelectedData.add(row);
            selectedKeys.add(adapter.getKey(row.data));
        }
    }

    //    @Override
    //    public List<V> onSelectAllLiveData() {
    //        final List<V> tmp = new ArrayList<>();
    //        for (final Row<V> element : liveData) {
    //            tmp.add(element.data);
    //        }
    //        return tmp;
    //    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Filtering ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void setFilter(final Object key, final boolean reinforcing, final AbstractFilter<V> filter) {
        final AbstractFilter<V> oldFilter = filters.put(key, filter);
        if (oldFilter == null || reinforcing) {
            //            final int oldLiveDataSize = liveData.size();
            //                        final int from = reinforceFilter(liveData, filter);
            reinforceFilter(liveData, filter);
            reinforceFilter(liveSelectedData, filter);
            //            if (from >= 0) {
            //                refreshRows(from, oldLiveDataSize);
            //            }
        } else {
            resetLiveData();
        }
    }

    private int reinforceFilter(final List<Row<V>> rows, final AbstractFilter<V> filter) {
        final Iterator<Row<V>> iterator = rows.iterator();
        int from = -1;
        for (int i = 0; iterator.hasNext(); i++) {
            final Row<V> row = iterator.next();
            if (!filter.test(row)) {
                row.accepted = false;
                iterator.remove();
                if (from < 0) from = i;
            }
        }
        return from;
    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Selecting ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//
    @Override
    public void select(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null || !selectedKeys.add(k) || !row.accepted) return;
        insertRow(liveSelectedData, row);
    }

    @Override
    public void unselect(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null || !selectedKeys.remove(k) || !row.accepted) return;
        removeRow(liveSelectedData, row);
    }
}
