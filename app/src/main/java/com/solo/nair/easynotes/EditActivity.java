package com.solo.nair.easynotes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.solo.nair.easynotes.ColorPicker.ColorPickerDialog;

import static com.solo.nair.easynotes.ColorPicker.ColorPickerSwatch.OnColorSelectedListener;
import static com.solo.nair.easynotes.ColorPicker.ColorPickerSwatch.OnTouchListener;
import static com.solo.nair.easynotes.DataUtils.NEW_NOTE_REQUEST;
import static com.solo.nair.easynotes.DataUtils.NOTE_BODY;
import static com.solo.nair.easynotes.DataUtils.NOTE_COLOUR;
import static com.solo.nair.easynotes.DataUtils.NOTE_FONT_SIZE;
import static com.solo.nair.easynotes.DataUtils.NOTE_REQUEST_CODE;
import static com.solo.nair.easynotes.DataUtils.NOTE_TITLE;


public class EditActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private final String COLOR_PICKER_TAG = "Color Picker";

    private EditText mTitleEditText, mBodyEditText;
    private RelativeLayout mRootRl;
    private Toolbar mToolbar;

    private InputMethodManager mInputManager;
    private Bundle mBundle;

    private String[] colourArr;
    private int[] colourArrResId;
    private int[] fontSizeArr;
    private String[] fontSizeNameArr;

    // Defaults
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default

    private AlertDialog fontDialog, saveChangesDialog;
    private ColorPickerDialog colorPickerDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        colourArr = getResources().getStringArray(R.array.colours);
        colourArrResId = new int[colourArr.length];
        for (int i = 0; i < colourArr.length; i++)
            colourArrResId[i] = Color.parseColor(colourArr[i]);

        fontSizeArr = new int[] {14, 18, 22}; // 0 for small, 1 for medium, 2 for large
        fontSizeNameArr = getResources().getStringArray(R.array.fontSizeNames);

        setContentView(R.layout.activity_edit);

        // Init layout components
        mToolbar = (Toolbar)findViewById(R.id.toolbarEdit);
        mTitleEditText = (EditText)findViewById(R.id.titleEdit);
        mBodyEditText = (EditText)findViewById(R.id.bodyEdit);
        mRootRl = (RelativeLayout)findViewById(R.id.relativeLayoutEdit);
        ScrollView scrollView = (ScrollView)findViewById(R.id.scrollView);

        mInputManager = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);

        if (mToolbar != null)
            initToolbar();

        if (scrollView != null)
            scrollView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!mBodyEditText.isFocused()) {
                        mBodyEditText.requestFocus();
                        mBodyEditText.setSelection(mBodyEditText.getText().length());
                        // Force show keyboard
                        mInputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                InputMethodManager.HIDE_IMPLICIT_ONLY);

                        return true;
                    }

                    return false;
                }
            });

        mBundle = getIntent().getExtras();

        if (mBundle != null) {
            // If current note is not new -> initialize colour, font, hideBody and EditTexts
            if (mBundle.getInt(NOTE_REQUEST_CODE) != NEW_NOTE_REQUEST) {
                colour = mBundle.getString(NOTE_COLOUR);
                fontSize = mBundle.getInt(NOTE_FONT_SIZE);

                mTitleEditText.setText(mBundle.getString(NOTE_TITLE));
                mBodyEditText.setText(mBundle.getString(NOTE_BODY));
                mBodyEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            }

            // If current note is new -> request keyboard focus to note title and show keyboard
            else if (mBundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST) {
                mTitleEditText.requestFocus();
                mInputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            mRootRl.setBackgroundColor(Color.parseColor(colour));
        }

        initDialogs(this);
    }


    /**
     * Initialize mToolbar with required components such as
     * - title, navigation icon + listener, menu/OnMenuItemClickListener, menuHideBody -
     */
    protected void initToolbar() {
        mToolbar.setTitle("");

        mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mToolbar.inflateMenu(R.menu.menu_edit);
        mToolbar.setOnMenuItemClickListener(this);
    }


    /**
     * @param context The Activity context of the dialogs; in this case EditActivity context
     */
    protected void initDialogs(Context context) {
        colorPickerDialog = ColorPickerDialog.newInstance(R.string.dialog_note_colour,
                colourArrResId, Color.parseColor(colour), 3,
                isTablet(this) ? ColorPickerDialog.SIZE_LARGE : ColorPickerDialog.SIZE_SMALL);

        colorPickerDialog.setOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                String selectedColourAsString = String.format("#%06X", (0xFFFFFF & color));

                for (String aColour : colourArr)
                    if (aColour.equals(selectedColourAsString))
                        colour = aColour;

                mRootRl.setBackgroundColor(Color.parseColor(colour));
            }
        });


        fontDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_font_size)
                .setItems(fontSizeNameArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fontSize = fontSizeArr[which];
                        mBodyEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        saveChangesDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.dialog_save_changes)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!isEmpty(mTitleEditText))
                            saveChanges();
                        else
                            showEmptyTextSnackBar();
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBundle != null && mBundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST) {
                            Intent intent = new Intent();
                            intent.putExtra("request", "discard");
                            setResult(RESULT_CANCELED, intent);
                            mInputManager.hideSoftInputFromWindow(mTitleEditText.getWindowToken(), 0);
                            dialog.dismiss();
                            finish();
                            overridePendingTransition(0, 0);
                        }
                    }
                })
                .create();
    }


    /**
     * Check if current device has tablet screen size or not
     * @param context current application context
     * @return true if device is tablet, false otherwise
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    /**
     * Item clicked in Toolbar menu callback method
     * @param item Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_font_size:
                fontDialog.show();
                return true;

            case R.id.action_note_colour:
                colorPickerDialog.show(getFragmentManager(), COLOR_PICKER_TAG);
                return true;

            case R.id.action_save:
                saveChanges();
                return true;

            default:
                return false;
        }
    }


    /**
     * Create an Intent with title, body, colour, font size and hideBody extras
     * Set RESULT_OK and go back to MainActivity
     */
    protected void saveChanges() {
        Intent intent = new Intent();
        intent.putExtra(NOTE_TITLE, mTitleEditText.getText().toString());
        intent.putExtra(NOTE_BODY, mBodyEditText.getText().toString());
        intent.putExtra(NOTE_COLOUR, colour);
        intent.putExtra(NOTE_FONT_SIZE, fontSize);
        setResult(RESULT_OK, intent);
        mInputManager.hideSoftInputFromWindow(mTitleEditText.getWindowToken(), 0);
        finish();
        overridePendingTransition(0, 0);
    }


    /**
     * Back or navigate home pressed
     */
    @Override
    public void onBackPressed() {
        if (mBundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST)
            saveChangesDialog.show();

        else {
            /*
             * If title is not empty -> Check if note changed
             *  If yes -> saveChanges
             *  If not -> hide keyboard if showing and finish
             */
            if (!isEmpty(mTitleEditText)) {
                if (!(mTitleEditText.getText().toString().equals(mBundle.getString(NOTE_TITLE))) ||
                    !(mBodyEditText.getText().toString().equals(mBundle.getString(NOTE_BODY))) ||
                    !(colour.equals(mBundle.getString(NOTE_COLOUR))) ||
                    fontSize != mBundle.getInt(NOTE_FONT_SIZE)) {
                    saveChanges();
                } else {
                    mInputManager.hideSoftInputFromWindow(mTitleEditText.getWindowToken(), 0);
                    finish();
                    overridePendingTransition(0, 0);
                }
            } else
                showEmptyTextSnackBar();
        }
    }


    /**
     * Check if passed EditText text is empty or not
     * @param editText The EditText widget to check
     * @return true if empty, false otherwise
     */
    protected boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    /**
     * Show SnackBar for 'Title cannot be empty'
     */
    protected void showEmptyTextSnackBar() {
        Snackbar.make(findViewById(android.R.id.content),
                getString(R.string.toast_edittext_cannot_be_empty), Snackbar.LENGTH_LONG)
                .setAction("Type", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTitleEditText.requestFocus();
                    }
                })
                .show();
    }


    /**
     * If current window loses focus -> hide keyboard
     * @param hasFocus parameter passed by system; true if focus changed, false otherwise
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            if (mInputManager != null && mTitleEditText != null)
                mInputManager.hideSoftInputFromWindow(mTitleEditText.getWindowToken(), 0);
    }


    /**
     * Orientation changed callback method
     * If orientation changed -> If any AlertDialog is showing -> dismiss it to prevent WindowLeaks
     * @param newConfig Configuration passed by system
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (colorPickerDialog != null && colorPickerDialog.isDialogShowing())
            colorPickerDialog.dismiss();

        if (fontDialog != null && fontDialog.isShowing())
            fontDialog.dismiss();

        if (saveChangesDialog != null && saveChangesDialog.isShowing())
            saveChangesDialog.dismiss();

        super.onConfigurationChanged(newConfig);
    }
}
