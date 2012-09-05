package edu.ucla.cens.whatsinvasive;

import java.util.Hashtable;
import java.util.Map;

/**
 * The type of the tag.
 * @author Jameel Al-Aziz
 *
 */
public enum TagType {
    WEED(0, "phone/gettags.php", R.string.tag_title_weeds),
    BUG(1, "phone/getanimaltags.php", R.string.tag_title_pests),
    BIOLIB(2, "DWN/smspecies-cs.json", R.string.tag_title_mapping_species);
    
    private static final Map<Integer,TagType> lookup = new Hashtable<Integer,TagType>();
    private final int value;
    private final String url;
    private final int resId;
    
    TagType(int value, String url, int resId)
    {
        this.value = value;
        this.url = url;
        this.resId = resId;
    }
    static {
        for(TagType t : TagType.values())
             lookup.put(t.value(), t);
    }
    
    public static TagType lookup(int value) { return lookup.get(value); }
    public int value() { return value; }
    public String url() { return url; }
    public int titleResource() { return resId; }
}
