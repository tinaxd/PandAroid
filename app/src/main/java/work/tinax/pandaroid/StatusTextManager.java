package work.tinax.pandaroid;

import android.widget.TextView;

public class StatusTextManager {
    private final TextView view;

    public StatusTextManager(TextView view) {
        this.view = view;
    }

    public void showText(String text) {
        view.setText(text);
    }

    public void removeText() {
        view.setText("");
    }
}
