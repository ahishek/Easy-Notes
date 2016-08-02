package com.solo.nair.easynotes;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.solo.nair.easynotes.DataUtils.NOTE_BODY;
import static com.solo.nair.easynotes.DataUtils.NOTE_COLOUR;
import static com.solo.nair.easynotes.DataUtils.NOTE_FAVOURED;
import static com.solo.nair.easynotes.DataUtils.NOTE_FONT_SIZE;
import static com.solo.nair.easynotes.DataUtils.NOTE_TITLE;
import static com.solo.nair.easynotes.MainActivity.deleteActive;
import static com.solo.nair.easynotes.MainActivity.searchActive;
import static com.solo.nair.easynotes.MainActivity.setFavourite;


public class NoteAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of notes
     */
    public NoteAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    @Override
    public JSONObject getItem(int position) {
        if (this.adapterData != null)
            return this.adapterData.optJSONObject(position);

        else
            return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_note, parent, false);

        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

        JSONObject noteObject = getItem(position);

        if (noteObject != null) {
            String title = context.getString(R.string.note_title);
            String body = context.getString(R.string.note_body);
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int fontSize = 18;
            Boolean hideBody = false;
            Boolean favoured = false;

            try {
                title = noteObject.getString(NOTE_TITLE);
                body = noteObject.getString(NOTE_BODY);
                colour = noteObject.getString(NOTE_COLOUR);

                if (noteObject.has(NOTE_FONT_SIZE))
                    fontSize = noteObject.getInt(NOTE_FONT_SIZE);

                favoured = noteObject.getBoolean(NOTE_FAVOURED);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (favoured)
                favourite.setImageResource(R.drawable.ic_fav);
            else
                favourite.setImageResource(R.drawable.ic_unfav);

            if (searchActive || deleteActive)
                favourite.setVisibility(View.INVISIBLE);
            else
                favourite.setVisibility(View.VISIBLE);


            titleView.setText(title);
            if (hideBody)
                bodyView.setVisibility(View.GONE);
            else {
                bodyView.setVisibility(View.VISIBLE);
                bodyView.setText(body);
                bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            }

            relativeLayout.setBackgroundColor(Color.parseColor(colour));

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that note to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
