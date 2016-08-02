package com.solo.nair.easynotes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import static com.solo.nair.easynotes.DataUtils.BACKUP_FILE_NAME;
import static com.solo.nair.easynotes.DataUtils.BACKUP_FOLDER_PATH;
import static com.solo.nair.easynotes.DataUtils.NEW_NOTE_REQUEST;
import static com.solo.nair.easynotes.DataUtils.NOTES_FILE_NAME;
import static com.solo.nair.easynotes.DataUtils.NOTE_BODY;
import static com.solo.nair.easynotes.DataUtils.NOTE_COLOUR;
import static com.solo.nair.easynotes.DataUtils.NOTE_FAVOURED;
import static com.solo.nair.easynotes.DataUtils.NOTE_FONT_SIZE;
import static com.solo.nair.easynotes.DataUtils.NOTE_REQUEST_CODE;
import static com.solo.nair.easynotes.DataUtils.NOTE_TITLE;
import static com.solo.nair.easynotes.DataUtils.deleteNotes;
import static com.solo.nair.easynotes.DataUtils.isExternalStorageReadable;
import static com.solo.nair.easynotes.DataUtils.isExternalStorageWritable;
import static com.solo.nair.easynotes.DataUtils.retrieveData;
import static com.solo.nair.easynotes.DataUtils.saveData;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, View.OnClickListener, NoteAdapter.OnFavoriteClickListener {

    private static File localPath, backupPath;

    // Layout components
    private ListView listView;
    private FloatingActionButton newNoteFab;
    private TextView noNotes;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private JSONArray notes;
    private NoteAdapter adapter;

    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched notes

    private int lastFirstVisibleItem = -1;
    private float newNoteButtonBaseYCoordinate;

    private AlertDialog backupCheckDialog, backupOKDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initControls();
        initDialogs(this);
    }

    private void initControls() {
        localPath = new File(getFilesDir() + "/" + NOTES_FILE_NAME);
        File backupFolder = new File(Environment.getExternalStorageDirectory() +
                BACKUP_FOLDER_PATH);

        if (isExternalStorageReadable() && isExternalStorageWritable() && !backupFolder.exists())
            backupFolder.mkdir();

        backupPath = new File(backupFolder, BACKUP_FILE_NAME);
        notes = new JSONArray();
        JSONArray tempNotes = retrieveData(localPath);
        if (tempNotes != null)
            notes = tempNotes;

        toolbar = (Toolbar) findViewById(R.id.toolbarMain);
        listView = (ListView) findViewById(R.id.listView);
        newNoteFab = (FloatingActionButton) findViewById(R.id.newNote);
        if (newNoteFab != null)
            newNoteFab.setOnClickListener(this);
        noNotes = (TextView) findViewById(R.id.noNotes);

        if (toolbar != null)
            initToolbar();

        newNoteButtonBaseYCoordinate = newNoteFab.getY();

        adapter = new NoteAdapter(getApplicationContext(), notes);
        adapter.setOnFavoriteClickListener(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If last first visible item not initialized -> set to current first
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                // If scrolled up -> hide newNoteFab button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newNoteButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newNoteFab button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {
                    newNoteButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
            }
        });

        if (notes.length() == 0)
            noNotes.setVisibility(View.VISIBLE);
        else
            noNotes.setVisibility(View.INVISIBLE);
    }


    /**
     * Initialize toolbar with required components such as
     * - title, menu/OnMenuItemClickListener and searchView -
     */
    protected void initToolbar() {
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            searchMenu = menu.findItem(R.id.action_search);

            if (searchMenu != null) {
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenu);
                if (searchView != null) {
                    searchView.setQueryHint(getString(R.string.action_search));
                    searchView.setOnQueryTextListener(this);
                    MenuItemCompat.setOnActionExpandListener(searchMenu,
                            new MenuItemCompat.OnActionExpandListener() {

                                @Override
                                public boolean onMenuItemActionExpand(MenuItem item) {
                                    searchActive = true;
                                    newNoteButtonVisibility(false);
                                    listView.setLongClickable(false);
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < notes.length(); i++)
                                        realIndexesOfSearchResults.add(i);
                                    adapter.notifyDataSetChanged();

                                    return true;
                                }

                                @Override
                                public boolean onMenuItemActionCollapse(MenuItem item) {
                                    searchEnded();
                                    return true;
                                }
                            });
                }
            }
        }
    }


    /**
     * Implementation of AlertDialogs such as
     * - backupCheckDialog, backupOKDialog, restoreCheckDialog, restoreFailedDialog -
     *
     * @param context The Activity context of the dialogs; in this case MainActivity context
     */
    protected void initDialogs(Context context) {
        backupCheckDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.action_backup)
                .setMessage(R.string.dialog_check_backup_if_sure)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (notes.length() > 0) {
                            boolean backupSuccessful = saveData(backupPath, notes);

                            if (backupSuccessful)
                                showBackupSuccessfulDialog();
                            else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_backup_failed),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_backup_no_notes),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        backupOKDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_backup_created_title)
                .setMessage(getString(R.string.dialog_backup_created) + " "
                        + backupPath.getAbsolutePath())
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    // Method to dismiss backup check and show backup successful dialog
    protected void showBackupSuccessfulDialog() {
        backupCheckDialog.dismiss();
        backupOKDialog.show();
    }


    /**
     * If item clicked in list view -> Start EditActivity intent with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        // If search is active -> use position from realIndexesOfSearchResults for EditActivity
        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                intent.putExtra(NOTE_TITLE, notes.getJSONObject(newPosition).getString(NOTE_TITLE));
                intent.putExtra(NOTE_BODY, notes.getJSONObject(newPosition).getString(NOTE_BODY));
                intent.putExtra(NOTE_COLOUR, notes.getJSONObject(newPosition).getString(NOTE_COLOUR));
                intent.putExtra(NOTE_FONT_SIZE, notes.getJSONObject(newPosition).getInt(NOTE_FONT_SIZE));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            intent.putExtra(NOTE_REQUEST_CODE, newPosition);
            startActivityForResult(intent, newPosition);
        } else {
            try {
                intent.putExtra(NOTE_TITLE, notes.getJSONObject(position).getString(NOTE_TITLE));
                intent.putExtra(NOTE_BODY, notes.getJSONObject(position).getString(NOTE_BODY));
                intent.putExtra(NOTE_COLOUR, notes.getJSONObject(position).getString(NOTE_COLOUR));
                intent.putExtra(NOTE_FONT_SIZE, notes.getJSONObject(position).getInt(NOTE_FONT_SIZE));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            intent.putExtra(NOTE_REQUEST_CODE, position);
            startActivityForResult(intent, position);
        }
    }


    /**
     * Item clicked in Toolbar menu callback method
     *
     * @param menuItem Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_backup:
                backupCheckDialog.show();
                return true;

            default:
                break;
        }
        return false;
    }


    /**
     * During multi-choice menu_delete selection mode, callback method if items checked changed
     *
     * @param mode     ActionMode of selection
     * @param position Position checked
     * @param id       ID of item, if exists
     * @param checked  true if checked, false otherwise
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked)
            checkedArray.add(position);
        else {
            int index = -1;
            for (int i = 0; i < checkedArray.size(); i++) {
                if (position == checkedArray.get(i)) {
                    index = i;
                    break;
                }
            }

            if (index != -1)
                checkedArray.remove(index);
        }
        mode.setTitle(checkedArray.size() + " " + getString(R.string.action_delete_selected_number));
        adapter.notifyDataSetChanged();
    }

    /**
     * Callback method when 'Delete' icon pressed
     *
     * @param mode ActionMode of selection
     * @param item MenuItem clicked, in our case just action_delete
     * @return true if clicked, false otherwise
     */
    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_delete)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            notes = deleteNotes(notes, checkedArray);
                            adapter = new NoteAdapter(getApplicationContext(), notes);
                            adapter.setOnFavoriteClickListener(MainActivity.this);
                            listView.setAdapter(adapter);
                            Boolean saveSuccessful = saveData(localPath, notes);
                            if (saveSuccessful) {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_deleted),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                            listView.post(new Runnable() {
                                public void run() {
                                    listView.smoothScrollToPosition(0);
                                }
                            });
                            if (notes.length() == 0)
                                noNotes.setVisibility(View.VISIBLE);
                            else
                                noNotes.setVisibility(View.INVISIBLE);

                            mode.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            return true;
        }

        return false;
    }

    // Long click detected on ListView item -> start selection ActionMode (delete mode)
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
        deleteActive = true;
        newNoteButtonVisibility(false);
        adapter.notifyDataSetChanged();
        return true;
    }

    // Selection ActionMode finished (delete mode ended)
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>();
        deleteActive = false;
        newNoteButtonVisibility(true);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }


    /**
     * Method to show and hide the newNoteFab button
     *
     * @param isVisible true to show button, false to hide
     */
    protected void newNoteButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newNoteFab.animate().cancel();
            newNoteFab.animate().translationY(newNoteButtonBaseYCoordinate);
        } else {
            newNoteFab.animate().cancel();
            newNoteFab.animate().translationY(newNoteButtonBaseYCoordinate + 500);
        }
    }


    /**
     * Callback method for 'searchView' menu item widget text change
     *
     * @param s String which changed
     * @return true if text changed and logic finished, false otherwise
     */
    @Override
    public boolean onQueryTextChange(String s) {
        s = s.toLowerCase();
        if (s.length() > 0) {
            JSONArray notesFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            for (int i = 0; i < notes.length(); i++) {
                JSONObject note = null;
                try {
                    note = notes.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (note != null) {
                    try {
                        if (note.getString(NOTE_TITLE).toLowerCase().contains(s) ||
                                note.getString(NOTE_BODY).toLowerCase().contains(s)) {
                            notesFound.put(note);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            NoteAdapter searchAdapter = new NoteAdapter(getApplicationContext(), notesFound);
            searchAdapter.setOnFavoriteClickListener(this);
            listView.setAdapter(searchAdapter);
        } else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < notes.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new NoteAdapter(getApplicationContext(), notes);
            adapter.setOnFavoriteClickListener(this);
            listView.setAdapter(adapter);
        }

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }


    /**
     * When search mode is finished
     * Collapse searchView widget, searchActive to false, reset adapter, enable listView long clicks
     * and show newNoteFab button
     */
    protected void searchEnded() {
        searchActive = false;
        adapter = new NoteAdapter(getApplicationContext(), notes);
        adapter.setOnFavoriteClickListener(this);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newNoteButtonVisibility(true);
    }


    /**
     * Callback method when EditActivity finished adding new note or editing existing note
     *
     * @param requestCode requestCode for intent sent, in our case either NEW_NOTE_REQUEST or position
     * @param resultCode  resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param data        Data bundle passed back from EditActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();
            Bundle mBundle = null;
            if (data != null)
                mBundle = data.getExtras();

            if (mBundle != null) {
                if (requestCode == NEW_NOTE_REQUEST) {
                    JSONObject newNoteObject = null;

                    try {
                        newNoteObject = new JSONObject();
                        newNoteObject.put(NOTE_TITLE, mBundle.getString(NOTE_TITLE));
                        newNoteObject.put(NOTE_BODY, mBundle.getString(NOTE_BODY));
                        newNoteObject.put(NOTE_COLOUR, mBundle.getString(NOTE_COLOUR));
                        newNoteObject.put(NOTE_FAVOURED, false);
                        newNoteObject.put(NOTE_FONT_SIZE, mBundle.getInt(NOTE_FONT_SIZE));
                        notes.put(newNoteObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.notifyDataSetChanged();
                    Boolean saveSuccessful = saveData(localPath, notes);
                    if (saveSuccessful) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.toast_new_note),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    if (notes.length() == 0)
                        noNotes.setVisibility(View.VISIBLE);
                    else
                        noNotes.setVisibility(View.INVISIBLE);

                } else {
                    JSONObject newNoteObject = null;

                    try {
                        newNoteObject = notes.getJSONObject(requestCode);
                        newNoteObject.put(NOTE_TITLE, mBundle.getString(NOTE_TITLE));
                        newNoteObject.put(NOTE_BODY, mBundle.getString(NOTE_BODY));
                        newNoteObject.put(NOTE_COLOUR, mBundle.getString(NOTE_COLOUR));
                        newNoteObject.put(NOTE_FONT_SIZE, mBundle.getInt(NOTE_FONT_SIZE));
                        notes.put(requestCode, newNoteObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (newNoteObject != null) {
                        adapter.notifyDataSetChanged();
                        Boolean saveSuccessful = saveData(localPath, notes);
                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_note_saved),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Bundle mBundle = null;
            if (data != null && data.hasExtra("request") && requestCode == NEW_NOTE_REQUEST) {
                mBundle = data.getExtras();
                if (mBundle != null && mBundle.getString("request").equals("discard")) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.toast_empty_note_discarded),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Favourite or un-favourite the note at position
     * @param favourite true to favourite, false to un-favourite
     * @param position  position of note
     */
    public void setFavourite(boolean favourite, int position) {
        JSONObject newFavourite = null;
        try {
            newFavourite = notes.getJSONObject(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                try {
                    newFavourite.put(NOTE_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (position > 0) {
                    JSONArray newArray = new JSONArray();
                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < notes.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(notes.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    notes = newArray;
                    adapter = new NoteAdapter(this, notes);
                    adapter.setOnFavoriteClickListener(this);
                    listView.setAdapter(adapter);
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                } else {
                    try {
                        notes.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.notifyDataSetChanged();
                }
            } else {
                try {
                    newFavourite.put(NOTE_FAVOURED, false);
                    notes.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();
            }
            saveData(localPath, notes);
        }
    }


    /**
     * If back button pressed while search is active -> collapse view and end search mode
     */
    @Override
    public void onBackPressed() {
        if (searchActive && searchMenu != null) {
            searchMenu.collapseActionView();
            return;
        }

        super.onBackPressed();
    }


    /**
     * Orientation changed callback method
     * If orientation changed -> If any AlertDialog is showing, dismiss it to prevent WindowLeaks
     *
     * @param newConfig New Configuration passed by system
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (backupCheckDialog != null && backupCheckDialog.isShowing())
            backupCheckDialog.dismiss();

        if (backupOKDialog != null && backupOKDialog.isShowing())
            backupOKDialog.dismiss();

        super.onConfigurationChanged(newConfig);
    }


    // Static method to return File at localPath
    public static File getLocalPath() {
        return localPath;
    }

    // Static method to return File at backupPath
    public static File getBackupPath() {
        return backupPath;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newNote:
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra(NOTE_REQUEST_CODE, NEW_NOTE_REQUEST);
                startActivityForResult(intent, NEW_NOTE_REQUEST);
                break;

            default:
                break;
        }
    }

    @Override
    public void onFavoriteClick(boolean favoured, int position) {
        setFavourite(favoured, position);
    }
}
