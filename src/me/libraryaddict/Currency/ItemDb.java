package me.libraryaddict.Currency;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemDb {
    private final transient Map<String, Long> items = new HashMap();
    private final transient ManagedFile file;
    private static final Pattern SPLIT = Pattern.compile("[^a-zA-Z0-9]");

    private final Pattern idMatch = Pattern.compile("^\\d+[:+',;.]\\d+$");
    private final Pattern metaSplit = Pattern.compile("[:+',;.]");
    private final Pattern number = Pattern.compile("^\\d+$");
    private final Pattern conjoined = Pattern.compile("^[^:+',;.]+[:+',;.]\\d+$");

    public ItemDb() {
        this.file = new ManagedFile("items.csv");
        onReload();
    }

    public void onReload() {
        List<String> lines = this.file.getLines();

        if (lines.isEmpty()) {
            return;
        }

        this.items.clear();

        for (String line : lines) {
            line = line.trim();
            if ((line.length() <= 0) || (line.charAt(0) != '#')) {
                String[] parts = SPLIT.split(line);
                if (parts.length >= 2) {
                    long numeric = Integer.parseInt(parts[1]);

                    long durability = (parts.length > 2) && ((parts[2].length() != 1) || (parts[2].charAt(0) != '0')) ? Short.parseShort(parts[2]) : 0;
                    this.items.put(parts[0].toLowerCase(Locale.ENGLISH), Long.valueOf(numeric | durability << 32));
                }
            }
        }
    }

    public ItemStack get(String id, int quantity) throws Exception {
        ItemStack retval = get(id.toLowerCase(Locale.ENGLISH));
        retval.setAmount(quantity);
        return retval;
    }

    public ItemStack get(String id) throws Exception {
        int itemid = 0;
        String itemname = null;
        short metaData = 0;
        if (this.idMatch.matcher(id).matches()) {
            String[] split = this.metaSplit.split(id);
            itemid = Integer.parseInt(split[0]);
            metaData = Short.parseShort(split[1]);
        } else if (this.number.matcher(id).matches()) {
            itemid = Integer.parseInt(id);
        } else if (this.conjoined.matcher(id).matches()) {
            String[] split = this.metaSplit.split(id);
            itemname = split[0].toLowerCase(Locale.ENGLISH);
            metaData = Short.parseShort(split[1]);
        } else {
            itemname = id.toLowerCase(Locale.ENGLISH);
        }

        if (itemname != null) {
            if (this.items.containsKey(itemname)) {
                long item = ((Long) this.items.get(itemname)).longValue();
                itemid = (int) (item & 0xFFFFFFFF);
                if (metaData == 0) metaData = (short) (int) (item >> 32 & 0xFFFF);
            } else if (Material.matchMaterial(itemname) != null) {
                itemid = Material.matchMaterial(itemname).getId();
                metaData = 0;
            } else {
                throw new Exception("unknownItemName");
            }
        }

        Material mat = Material.getMaterial(itemid);
        if (mat == null) {
            throw new Exception("unknownItemId");
        }
        ItemStack retval = new ItemStack(mat, mat.getMaxStackSize(), metaData);
        return retval;
    }
}