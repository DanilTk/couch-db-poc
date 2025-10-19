package pl.home.couchdbpoc.data;

public enum Country {
    POLAND("Poland"),
    ITALY("Italy"),
    KENYA("Kenya"),
    LIECHTENSTEIN("Liechtenstein"),
    UKRAINE("Ukraine"),
    FRANCE("France"),
    ICELAND("Iceland"),
    SEYCHELLES("Seychelles"),
    CANADA("Canada"),
    GIBRALTAR("Gibraltar");

    private final String displayName;

    Country(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
