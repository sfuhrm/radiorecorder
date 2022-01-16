package de.sfuhrm.radiorecorder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class ListHelper<T> {
    private List<T> list;
    private List<ColumnInfo> columns;
    @Getter
    @Setter
    @AllArgsConstructor
    private class ColumnInfo {
        int index;
        private String name;
        private Function<T, String> formatter;
        int maxLength;
    }

    ListHelper(List<T> inList) {
        this.list = new ArrayList<>(inList);
        columns = new ArrayList<>();
    }

    void addColumn(String name, Function<T, String> formatter) {
        this.columns.add(new ColumnInfo(columns.size(), name, formatter, name.length()));
    }

    private void calculateSizes() {
        for (T item : list) {
            for (ColumnInfo columnInfo : columns) {
                columnInfo.maxLength = Math.max(columnInfo.maxLength, columnInfo.formatter.apply(item).length());
            }
        }
    }

    String fill(String in, int length) {
        StringBuilder sb = new StringBuilder(in);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    void print(PrintStream ps) {
        calculateSizes();

        for (ColumnInfo columnInfo : columns) {
            ps.print(fill(columnInfo.name, columnInfo.maxLength));
            ps.print(' ');
        }
        ps.print("\n");

        for (T item : list) {
            for (ColumnInfo columnInfo : columns) {
                ps.print(fill(columnInfo.formatter.apply(item), columnInfo.maxLength));
                ps.print(' ');
            }
            ps.print("\n");
        }
    }
}
