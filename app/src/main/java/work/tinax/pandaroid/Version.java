package work.tinax.pandaroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Version implements Comparable<Version> {
    private int major;
    private int minor;
    private int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Version)) return false;
        Version o = (Version)obj;
        return (major == o.major) && (minor == o.minor) && (patch == o.patch);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(10000*major+100*minor+patch);
    }

    @Override
    public int compareTo(Version o) {
        int majorCmp = Integer.compare(major, o.major);
        if (majorCmp != 0) return majorCmp;
        int minorCmp = Integer.compare(minor, o.minor);
        if (minorCmp != 0) return minorCmp;
        return Integer.compare(patch, o.patch);
    }

    @NonNull
    @Override
    public String toString() {
        return "<Version " + major + "." + minor + "." + patch + ">";
    }
}
