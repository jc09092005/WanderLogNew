package com.example.wanderlognew;

// represents a journal entry or a section title in the journal list
public class JournalEntry {
    // Constants to differentiate between title headers and entry item
    public static final int TYPE_TITLE = 0;
    public static final int TYPE_ENTRY = 1;

    // type of the item (either TYPE_TITLE or TYPE_ENTITY)
    public int type;

    // Fields for actual journal entries
    public int id;
    public String title;
    public String country;
    public String dateStart;
    public String dateEnd;
    public String description;
    public String imageName;

    // Constructor for title
    public JournalEntry(int type) {
        this.type = type;
    }

    // Constructor for full journal entry
    public JournalEntry(int id, String title, String country, String dateStart,
                        String dateEnd, String description, String imageName) {
        this.type = TYPE_ENTRY;
        this.id = id;
        this.title = title;
        this.country = country;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.description = description;
        this.imageName = imageName;
    }
}
