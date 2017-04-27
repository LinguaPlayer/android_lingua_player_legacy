package org.videolan.vlc.gui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.videolan.vlc.R;
import org.videolan.vlc.gui.video.VideoPlayerActivity;
import org.videolan.vlc.util.Dictionary;

import java.io.IOException;
import java.util.regex.Pattern;


/**
 * Created by habib on 4/20/17.
 */

public class DictionaryDialog extends DialogFragment implements AdapterView.OnItemSelectedListener{

    private EditText mWordToTranslateEditText;
    private TextView mCaptionTextView;
    private TextView mTranslationTextView;
    private Dictionary mDictionary;
    private ProgressBar mDictionaryLoading;
    private ImageView mSpeakButton;


    String[] dictionaryValues = null;


    public DictionaryDialog(){

    }


    public static DictionaryDialog newInstance (String dialog,String wordToTranslate){
        DictionaryDialog dictionaryFragment = new DictionaryDialog();
        Bundle args = new Bundle();
        args.putString("word",wordToTranslate);
        args.putString("dialog",dialog);

        dictionaryFragment.setArguments(args);
        return dictionaryFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container , Bundle savedInstanceState){
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);

        View v = inflater.inflate(R.layout.dictionary,container);
        return v;
    }

    private void translate(String word){
        mWordToTranslateEditText.setText(word);
        mWordToTranslateEditText.setSelection(word.length());

        mTranslateTask = new Translate();
        mTranslateTask.execute(new String[]{word});
    }

    @Override
    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        final String word = (String) getArguments().get("word");
        String dialog = (String) getArguments().get("dialog");
        SpannableStringBuilder clickableCaption = makeClickable(dialog);

        mWordToTranslateEditText = (EditText) view.findViewById(R.id.edit_text);
        mCaptionTextView = (TextView) view.findViewById(R.id.caption);
        mTranslationTextView = (TextView) view.findViewById(R.id.translation);
        mDictionaryLoading = (ProgressBar) view.findViewById(R.id.dictionary_loading);
        mSpeakButton = (ImageView) view.findViewById(R.id.speak);




        mWordToTranslateEditText.setText(word);
        mWordToTranslateEditText.setSelection(word.length());

        mCaptionTextView.setText(clickableCaption);
        mCaptionTextView.setMovementMethod(new LinkTouchMovementMethod());
        mCaptionTextView.setHighlightColor(getResources().getColor(R.color.orange500));

        mSpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(mDictionary != null)
                   mDictionary.speakOut(mWordToTranslateEditText.getText().toString());
            }
        });

        mWordToTranslateEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
//                    Toast.makeText(getActivity(), mWordToTranslateEditText.getText(), Toast.LENGTH_SHORT).show();
                    SpannableStringBuilder styledString = (SpannableStringBuilder) Html.fromHtml("");
                    translate(mWordToTranslateEditText.getText().toString());
                    return false;
                }
                return false;
            }
        });

        Spinner spinner = (Spinner) view.findViewById(R.id.language_list);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.dictionaries_entries, R.layout.language_list_spinner_item);
        dictionaryValues = getResources().getStringArray(R.array.dictionaries_values);
        adapter.setDropDownViewResource(R.layout.language_list_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

    }
    private Translate mTranslateTask = null;

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String dbName = dictionaryValues[position];
        mDictionaryLoadertask = new DictionaryLoader();
        mDictionaryLoadertask.execute(new String[]{dbName, mWordToTranslateEditText.getText().toString()});

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class Translate extends AsyncTask<String, Void, SpannableStringBuilder> {

        @Override
        protected SpannableStringBuilder doInBackground(String... params) {
            String translation = "";
            if(mDictionary != null)
                translation = mDictionary.getTranslation(params[0]);

            SpannableStringBuilder styledTranslation = (SpannableStringBuilder) Html.fromHtml(translation.replaceAll("\n","<br />"));
            return styledTranslation;
        }

        @Override
        protected void onPostExecute (SpannableStringBuilder styledTranslation) {
            mTranslationTextView.setText(styledTranslation);
            mTranslationTextView.setVisibility(View.VISIBLE);
        }

    }

    private DictionaryLoader mDictionaryLoadertask = null;
    private class DictionaryLoader extends AsyncTask<String, Void, Dictionary> {

        private String word;
        @Override
        protected Dictionary doInBackground(String... params) {
            String dbName = params[0];
            word = params[1];
            try {
                mDictionary = Dictionary.getInstance(getContext(),dbName);
                return mDictionary;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Dictionary db) {
            mDictionaryLoading.setVisibility(View.GONE);
            mDictionary = db;
            translate(word);
        }

        @Override
        protected void onPreExecute() {
            mDictionaryLoading.setVisibility(View.VISIBLE);
            mTranslationTextView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity activity = getActivity();
        ((VideoPlayerActivity) activity).onDictionaryDialogDismissed();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        Window window = getDialog().getWindow();
        Point size = new Point();

        Display display = window.getWindowManager().getDefaultDisplay();

        int width;
        int height;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            width = display.getWidth();
            height = display.getHeight();
        } else {
            display.getSize(size);
            width = size.x;
            height = size.y;
        }


        window.setLayout((int) (width * 0.75), (int) (height * 0.65));
        window.setGravity(Gravity.CENTER);
    }


    private SpannableStringBuilder makeClickable(String text){
        SpannableStringBuilder ss = new SpannableStringBuilder("");
        String pattern = "[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+]";
        Pattern r = Pattern.compile(pattern);
        String[] words = text.split("(?=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])|(?<=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])");
        int start,end;
        start = end =  0;
        int index = 0;
        for(final String word : words){
            index++;
            ss.append(word);
            end = start + word.length();

            if(!r.matcher(word).find()) {
                ClickableSpan clickableSpan = new SubTouchSpan(word,Color.BLACK,getResources().getColor(R.color.orange500),Color.parseColor("#ffdfae"));
                ss.setSpan(clickableSpan, start, end, 0);
            }

            start = end;
        }

        return ss;
    }

    private class LinkTouchMovementMethod extends LinkMovementMethod {
        private SubTouchSpan mPressedSpan;

        @Override
        public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mPressedSpan = getPressedSpan(textView, spannable, event);
                if (mPressedSpan != null) {
                    mPressedSpan.setPressed(true);
                    Selection.setSelection(spannable, spannable.getSpanStart(mPressedSpan),
                            spannable.getSpanEnd(mPressedSpan));
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                SubTouchSpan touchedSpan = getPressedSpan(textView, spannable, event);
                if (mPressedSpan != null && touchedSpan != mPressedSpan) {
                    mPressedSpan.setPressed(false);
                    mPressedSpan = null;
                    Selection.removeSelection(spannable);
                }
            } else {
                if (mPressedSpan != null) {
                    mPressedSpan.setPressed(false);
                    super.onTouchEvent(textView, spannable, event);
                }
                mPressedSpan = null;
                Selection.removeSelection(spannable);
            }
            return true;
        }

        private SubTouchSpan getPressedSpan(TextView textView, Spannable spannable, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= textView.getTotalPaddingLeft();
            y -= textView.getTotalPaddingTop();

            x += textView.getScrollX();
            y += textView.getScrollY();

            Layout layout = textView.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            SubTouchSpan[] link = spannable.getSpans(off, off, SubTouchSpan.class);
            SubTouchSpan touchedSpan = null;
            if (link.length > 0) {
                touchedSpan = link[0];
            }
            return touchedSpan;
        }

    }

    private class SubTouchSpan extends ClickableSpan {
        private final String mText;
        private boolean mIsPressed;
        private int mPressedBackgroundColor;
        private int mNormalTextColor;
        private int mPressedTextColor;

        private SubTouchSpan(final String text, int normalTextColor, int pressedTextColor, int pressedBackgroundColor) {
            mText = text;
            mNormalTextColor = normalTextColor;
            mPressedTextColor = pressedTextColor;
            mPressedBackgroundColor = pressedBackgroundColor;
        }

        public void setPressed(boolean isSelected) {
            mIsPressed = isSelected;
        }

        @Override
        public void onClick(final View widget) {
            translate(mText);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(mIsPressed ? mPressedTextColor : mNormalTextColor);
            ds.bgColor = mIsPressed ? mPressedBackgroundColor :Color.parseColor("#ffdfae") ;
//            ds.linkColor = Color.BLACK;
            ds.setUnderlineText(false);
        }
    }

}
