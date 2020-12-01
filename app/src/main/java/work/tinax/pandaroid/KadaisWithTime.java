package work.tinax.pandaroid;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;

import work.tinax.pandagui.Kadai;

public class KadaisWithTime implements Serializable {
    private final ArrayList<Kadai> kadais;
    private final LocalDateTime time;

    public KadaisWithTime(ArrayList<Kadai> kadais, LocalDateTime time) {
        this.kadais = kadais;
        this.time = time;
    }

    public ArrayList<Kadai> getKadais() {
        return kadais;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
