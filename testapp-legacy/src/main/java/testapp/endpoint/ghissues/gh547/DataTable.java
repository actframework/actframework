package testapp.endpoint.ghissues.gh547;

import act.data.annotation.Data;
import act.util.SimpleBean;

import java.util.List;
import java.util.Map;

public class DataTable {

    @Data
    public static class Search implements SimpleBean {
        public String value;
        public boolean regex;
    }

    @Data
    public static class Column implements SimpleBean {
        public String id;
        public String data;
        public String name;
        public boolean searchable;
        public boolean orderable;
        public Search search;
    }

    @Data
    public static class Order implements SimpleBean {
        public Integer column;
        public String dir;
    }

    private int draw;
    private int recordsTotal;

    private int recordsFiltered;

    private List data;

    private String error;

    private int start;
    private int length;
    private Map<String, String> search;
    private List<Column> columns;
    private List<Order> order;


    public Order getOrderProperty() {
        if (order != null && order.size() > 0) {
            return order.get(0);
        } else {
            return null;
        }
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Map<String, String> getSearch() {
        return search;
    }

    public void setSearch(Map<String, String> search) {
        this.search = search;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Order> getOrder() {
        return order;
    }

    public void setOrder(List<Order> order) {
        this.order = order;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public int getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(int recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public int getRecordsFiltered() {
        return recordsFiltered;
    }

    public void setRecordsFiltered(int recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public List getData() {
        return data;
    }

    public void setData(List data) {
        this.data = data;
    }
}