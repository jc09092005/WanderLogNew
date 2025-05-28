package com.example.wanderlognew;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class JournalEntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // View type constraints (from JournalEntry_
    private static final int TYPE_TITLE = JournalEntry.TYPE_TITLE;
    private static final int TYPE_ENTRY = JournalEntry.TYPE_ENTRY;

    private List<JournalEntry> journalList; // List of journal entries to display
    private Context context;

    // Constructor
    public JournalEntryAdapter(Context context, List<JournalEntry> journalList) {
        this.context = context;
        this.journalList = journalList;
    }

    // returns total number of items in the list
    @Override
    public int getItemCount() {
        return journalList.size();
    }

    // Returns the type of view for a given position
    @Override
    public int getItemViewType(int position) {
        return journalList.get(position).type;
    }

    // Creates the appropriate ViewHolder depending on the item type (title or empty)
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_TITLE) {
            View view = inflater.inflate(R.layout.item_journal_title, parent, false);
            return new TitleViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_journal_entry, parent, false);
            return new EntryViewHolder(view);
        }
    }

    // Binds data to the appropriate ViewHolder
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JournalEntry entry = journalList.get(position);

        if (holder instanceof EntryViewHolder) {
            EntryViewHolder h = (EntryViewHolder) holder;

            // Set entry text data
            Log.d("JournalImageDebug", "Image path: " + entry.imageName);
            h.titleView.setText(entry.title);
            h.countryView.setText(entry.country);
            h.dateView.setText(entry.dateStart + " - " + entry.dateEnd);

            // Loads image with Glide (from content URI or drawable name)
            if (entry.imageName != null && entry.imageName.startsWith("content://")) {
                Glide.with(context)
                        .load(Uri.parse(entry.imageName))
                        .placeholder(R.drawable.image_icon)
                        .into(h.imageView);
            } else {
                int resId = context.getResources().getIdentifier(
                        entry.imageName, "drawable", context.getPackageName());

                if (resId != 0) {
                    Glide.with(context).load(resId).into(h.imageView);
                } else {
                    Glide.with(context).load(R.drawable.image_icon).into(h.imageView);
                }
            }

            // On click, open JournalDetailFragment for the selected entry
            h.itemView.setOnClickListener(v -> {
                Fragment fragment = JournalDetailFragment.newInstance(entry.id);
                ((AppCompatActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    // ViewHolder for a journal entry card
    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, countryView, dateView;
        ImageView imageView;

        public EntryViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.card_title);
            countryView = view.findViewById(R.id.card_country);
            dateView = view.findViewById(R.id.card_dates);
            imageView = view.findViewById(R.id.card_image);
        }
    }

    // Placeholder ViewHolder for section titles (if used)
    static class TitleViewHolder extends RecyclerView.ViewHolder {
        public TitleViewHolder(View view) {
            super(view);
        }
    }
}
