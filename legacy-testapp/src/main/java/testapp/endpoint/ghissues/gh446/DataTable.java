package testapp.endpoint.ghissues.gh446;

import act.data.annotation.Data;
import act.util.SimpleBean;

import java.util.List;
import java.util.Map;

/**
 * @author tobias
 * @date 2017/12/27 18:37
 * jquery dataTable 结果封装
 */
public class DataTable {

    @Data
    public static class Search implements SimpleBean {
        public String value;
        public boolean regex;
    }

    @Data
    public static class Column implements SimpleBean {
        public String id;
        public String name;
        public boolean searchable;
        public boolean orderable;
        public Search search;
    }
    /**
     * 和请求的保持一致，防止攻击
     */
    private int draw;
    /**
     * 总记录条数
     */
    private int recordsTotal;
    /**
     * 过滤后的记录条数
     */
    private int recordsFiltered;
    /**
     * 数据-可以是数组或者对象数组
     */
    private List data;
    /**
     * 错误提示
     */
    private String error;

    /**
     * 以下是请求参数
     */
    private int start;
    private int length;
    private Map<String, String> search;
    private List<Column> columns;
    private List<Map<String, String>> order;


    public Map<String, String> getOrderProperty() {
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

    public List<Map<String, String>> getOrder() {
        return order;
    }

    public void setOrder(List<Map<String, String>> order) {
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
