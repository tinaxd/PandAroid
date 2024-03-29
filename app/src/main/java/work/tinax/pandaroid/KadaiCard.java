// Copyright 2020 tinaxd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package work.tinax.pandaroid;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class KadaiCard extends RecyclerView.ViewHolder {
    public CardView cardView;
    public KadaiCard(@NonNull CardView itemView) {
        super(itemView);
        cardView = itemView;
    }

    public void setTitle(String title, boolean isQuiz) {
        String quizPrefix = isQuiz ? "(クイズ) " : "";
        ((TextView)cardView.findViewById(R.id.kadaiNameText)).setText(quizPrefix + title);
    }

    public void setTitle(String title) {
        setTitle(title, false);
    }

    public void setLecture(String lecture) {
        ((TextView)cardView.findViewById(R.id.kadaiLectureText)).setText(lecture);
    }

    public void setDescription(String description) {
        ((TextView)cardView.findViewById(R.id.kadaiDescriptionText)).setText(
                TAG_REMOVE_PATTERN.matcher(description).replaceAll(""));
    }

    public void setDueText(LocalDateTime due) {
        ((TextView)cardView.findViewById(R.id.kadaiDueText)).setText(due.format(DateTimeFormatter.ofPattern("y/M/d HH:mm")));
        LocalDateTime now = LocalDateTime.now();
        if (now.plusDays(RED_DAYS).isAfter(due)) {
            setBackgroundColor(RED_COLOR);
        } else if (now.plusDays(YELLOW_DAYS).isAfter(due)) {
            setBackgroundColor(YELLOW_COLOR);
        } else if (now.plusDays(GREEN_DAYS).isAfter(due)) {
            setBackgroundColor(GREEN_COLOR);
        } else {
            setBackgroundColor(GRAY_COLOR);
        }
    }

    public void setBackgroundColor(int color) {
        cardView.setBackgroundColor(color);
    }

    private static final Pattern TAG_REMOVE_PATTERN = Pattern.compile("<.+?>");

    public static final int RED_COLOR = Color.parseColor("#ffa3a3");
    public static final int YELLOW_COLOR = Color.parseColor("#ffffb7");
    public static final int GREEN_COLOR = Color.parseColor("#a3ffa3");
    public static final int GRAY_COLOR = Color.parseColor("#CFCECE");

    private static final int RED_DAYS = 1;
    private static final int YELLOW_DAYS = 5;
    private static final int GREEN_DAYS = 14;
}
