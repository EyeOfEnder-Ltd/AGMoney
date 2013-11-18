package me.libraryaddict.Currency;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.google.common.collect.Lists;

public class ManagedFile {
    private final transient File file;

    public ManagedFile(String filename) {
        this.file = new File(CurrencyMain.instance.getDataFolder(), filename);
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            try {
                copyResourceAscii("/" + filename, this.file);
            } catch (IOException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "itemsCsvNotLoaded", ex);
            }
        }
    }

    public static void copyResourceAscii(String resourceName, File file) throws IOException {
        InputStreamReader reader = new InputStreamReader(ManagedFile.class.getResourceAsStream(resourceName));
        try {
            MessageDigest digest = getDigest();
            DigestOutputStream digestStream = new DigestOutputStream(new FileOutputStream(file), digest);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(digestStream);
                try {
                    char[] buffer = new char[8192];
                    while (true) {
                        int length = reader.read(buffer);
                        if (length < 0) break;
                        writer.write(buffer, 0, length);
                    }

                    writer.write("\n");
                    writer.flush();
                    BigInteger hashInt = new BigInteger(1, digest.digest());
                    digestStream.on(false);
                    digestStream.write(35);
                    digestStream.write(hashInt.toString(16).getBytes());
                } finally {
                    writer.close();
                }
            } finally {
                digestStream.close();
            }
        } finally {
            reader.close();
        }
    }

    public static boolean checkForVersion(File file, String version) throws IOException {
        if (file.length() < 33L) {
            return false;
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        try {
            byte[] buffer = new byte[(int) file.length()];
            int position = 0;
            do {
                int length = bis.read(buffer, position, Math.min((int) file.length() - position, 8192));
                if (length < 0) {
                    break;
                }
                position += length;
            } while (position < file.length());
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            if (bais.skip(file.length() - 33L) != file.length() - 33L) {
                return false;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(bais));
            DigestInputStream digestStream;
            try {
                String hash = reader.readLine();
                if ((hash != null) && (hash.matches("#[a-f0-9]{32}"))) {
                    hash = hash.substring(1);
                    bais.reset();
                    String versionline = reader.readLine();
                    if ((versionline != null) && (versionline.matches("#version: .+"))) {
                        String versioncheck = versionline.substring(10);
                        if (!versioncheck.equalsIgnoreCase(version)) {
                            bais.reset();
                            MessageDigest digest = getDigest();
                            digestStream = new DigestInputStream(bais, digest);
                            try {
                                byte[] bytes = new byte[(int) file.length() - 33];
                                digestStream.read(bytes);
                                BigInteger correct = new BigInteger(hash, 16);
                                BigInteger test = new BigInteger(1, digest.digest());
                                if (correct.equals(test)) {
                                    return true;
                                }

                                Bukkit.getLogger().warning("File " + file.toString() + " has been modified by user and file version differs, please update the file manually.");
                            } finally {
                            }
                        }
                    }
                }
            } finally {
            }
        } finally {
            bis.close();
        }
        bis.close();

        return false;
    }

    public static MessageDigest getDigest() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    public List<String> getLines() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.file));
            try {
                List<String> lines = Lists.newArrayList();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    lines.add(line);
                }

                return lines;
            } finally {
                reader.close();
            }
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
        return Collections.emptyList();
    }
}