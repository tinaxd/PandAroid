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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import work.tinax.pandagui.Kadai;

public class KadaiAdapter extends RecyclerView.Adapter<KadaiCard> {
    private final List<Kadai> kadais;

    public KadaiAdapter(List<Kadai> kadais) {
        this.kadais = kadais;
    }

    @NonNull
    @Override
    public KadaiCard onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.kadai_element, parent, false);
        return new KadaiCard(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KadaiCard holder, int position) {
        Kadai kadai = kadais.get(position);
        holder.setTitle(kadai.getKadaiName());
        holder.setLecture(kadai.getLectureName());
        holder.setDescription(kadai.getDescription());
        holder.setDueText(kadai.getDue());
    }

    @Override
    public int getItemCount() {
        return kadais.size();
    }
}
