package act.data;

/**
 * Specify the Date time format
 */
public @interface Format {
    /**
     * The date time format value. E.g
     *
     * * "yyyy-MM-dd"
     * * "dd/MM/yyyy HH:MM"
     *
     * @return the format string
     */
    String value();
}
