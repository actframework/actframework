package act.db;

import act.db.util.Page;

/**
 * An interface used to control query execution.
 * @param <MODEL_TYPE>
 *     the model class
 * @param <QUERY_TYPE>
 *     the query class
 */
public interface Query<MODEL_TYPE, QUERY_TYPE extends Query<MODEL_TYPE, QUERY_TYPE>> {
    /**
     * Set the offset of the first record.
     *
     * @param pos
     *      it shall skip `pos` to get the first record. must not be negative
     * @return this Query instance.
     */
    QUERY_TYPE offset(int pos);

    /**
     * Set maximum number of records to be fetched.
     *
     * @param limit
     *      the limit of the records.
     * @return this Query instance.
     */
    QUERY_TYPE limit(int limit);

    /**
     * Set the order by field list, e.g. `"-level,lastName,firstName"`
     *
     * Each element in the list shall be a field name, optionally prefixed
     * with `-` symbol, meaning sort by the field in descending order.
     *
     * @param fieldList
     *      the order by field list.
     * @return this Query instance.
     */
    QUERY_TYPE orderBy(String... fieldList);

    /**
     * Set the filter {@link CriteriaComponent criteria}.
     * @param criteriaComponent
     *      the criteria
     * @return this Query instance
     */
    QUERY_TYPE filterBy(CriteriaComponent criteriaComponent);

    /**
     * Returns the first record matched the criteria
     * {@link #filterBy(CriteriaComponent) set} on this Query instance.
     *
     * If {@link #offset(int) offset} has been set on this Query
     * instance, then it shall be effect and impact the return value.
     *
     * If there is no criteria set on this Query instance, then
     * it fetches the first record in the data storage.
     *
     * If no record in the data storage found by criteria then `null`
     * shall be returned.
     *
     * @return this Query instance.
     */
    MODEL_TYPE first();

    /**
     * Fetch records matches the {@link #filterBy(CriteriaComponent) criteria set}
     * on this Query instance.
     *
     * If {@link #offset(int) offset} and/or {@link #limit(int) limit}
     * has been set on this Query instance, then they should be effect
     * and impact the return value.
     *
     * if there is no criteria set on this Query instance, then
     * it fetches all records in the data storage.
     *
     * If no record found then it shall return an `Iterable` without
     * any component; otherwise it shall return an `Iterable` iterates
     * all records matches by the order {@link #orderBy(String...) set}
     * on this Query instance, or without determined order if no
     * order has been set on this Query instance.
     *
     * @return this Query interface.
     */
    Iterable<MODEL_TYPE> fetch();

    /**
     * Fetch records matches {}
     * @param pageSpec
     * @return
     */
    Page<MODEL_TYPE> page(Page<MODEL_TYPE> pageSpec);

    /**
     * Returns the number of records in the data storage
     * matches the
     * @return
     */
    long count();
}
