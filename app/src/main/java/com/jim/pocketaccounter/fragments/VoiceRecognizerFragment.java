package com.jim.pocketaccounter.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.utils.Utils;
import com.jim.pocketaccounter.PocketAccounter;
import com.jim.pocketaccounter.PocketAccounterApplication;
import com.jim.pocketaccounter.R;
import com.jim.pocketaccounter.animations.Cieo;
import com.jim.pocketaccounter.database.Account;
import com.jim.pocketaccounter.database.AccountDao;
import com.jim.pocketaccounter.database.Currency;
import com.jim.pocketaccounter.database.CurrencyDao;
import com.jim.pocketaccounter.database.DaoSession;
import com.jim.pocketaccounter.database.FinanceRecord;
import com.jim.pocketaccounter.database.FinanceRecordDao;
import com.jim.pocketaccounter.database.RootCategory;
import com.jim.pocketaccounter.database.SubCategory;
import com.jim.pocketaccounter.database.TemplateAccount;
import com.jim.pocketaccounter.database.TemplateVoice;
import com.jim.pocketaccounter.managers.PAFragmentManager;
import com.jim.pocketaccounter.utils.PocketAccounterGeneral;
import com.jim.pocketaccounter.utils.cache.DataCache;
import com.jim.pocketaccounter.utils.speech.PASpeechRecognizer;
import com.jim.pocketaccounter.utils.speech.SpeechListener;

import java.io.File;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class VoiceRecognizerFragment extends Fragment {
    public static final int DEBTBORROW = 0;
    public static final int CATEGORY = 1;
    public static final int SUBCATEGORY = 2;
    public static final int ACCOUNT = 3;
    //Not speech mode layout
    private RelativeLayout rlNotSpeechMode;
    //Speech mode layout
    private RelativeLayout llSpeechMode;
    //Not speech mode balance stripe textviews
    private TextView tvNotSpeechModeIncome, tvNotSpeechModeBalance, tvNotSpeechModeExpense;
    //Not speech mode records list
    private RecyclerView rvNotSpeechModeRecordsList;
    //Speech mode adjective recognition
    private TextView tvSpeechModeAdjective;
    //Speech mode category recognition and its icon
    private TextView tvSpeechModeCategory;
    //Speech mode amount and currency recognition
    private TextView tvSpeechAmount;
    private Spinner spSpeechCurrency;
    //Speech mode account recognition
    private Spinner spSpeechAccount;
    //Speech mode entered text
    private TextView tvSpeechModeEnteredText;
    //listening indicator
    private TextView tvListeningIndicator;
    //is listening started?
    boolean started = false;
    //made finance records;
    private List<FinanceRecord> records = new ArrayList<>();
    //collection finance record;
    private FinanceRecord record = null;
    //adjective definition array
    private String[] definitionArrays;
    //Center clickable button
    private RelativeLayout rlCenterButton;
    //bg must be changed
    private ImageView ivCenterButton;
    //icon
    private ImageView ivMicrophoneIcon;
    //Speech recognize manager
    private PASpeechRecognizer recognizer;
    //record start left image
    private FrameLayout recStartLeft;
    //record start right image
    private FrameLayout recStartRight;
    //auto save voice
    private TextView autoSave;
    //switching between modes
    @Inject DaoSession daoSession;
    @Inject PAFragmentManager paFragmentManager;
    @Inject List<TemplateVoice> voices;
    @Inject List<TemplateAccount> templateAccountVoices;
    @Inject DataCache dataCache;
    private String[] curString;
    private String[] accString;
    private CountDownTimer timer;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = LayoutInflater.from(getContext()).inflate(R.layout.voice_recognizer, container, false);
        ((PocketAccounter) getContext()).component((PocketAccounterApplication) getContext().getApplicationContext()).inject(this);
        rlCenterButton = (RelativeLayout) rootView.findViewById(R.id.rlCenterButton);
        ivCenterButton = (ImageView) rootView.findViewById(R.id.ivCenterButton);
        llSpeechMode = (RelativeLayout) rootView.findViewById(R.id.llSpeechMode);
        rlNotSpeechMode = (RelativeLayout) rootView.findViewById(R.id.rlNotSpeechMode);
        ivMicrophoneIcon = (ImageView) rootView.findViewById(R.id.ivMicrophoneIcon);
        recStartLeft = (FrameLayout) rootView.findViewById(R.id.flVoiceRecordStartLeft);
        recStartRight = (FrameLayout) rootView.findViewById(R.id.flVoiceRecordStartRight);
        tvSpeechAmount = (TextView) rootView.findViewById(R.id.tvSpeechAmount);
        tvSpeechModeCategory = (TextView) rootView.findViewById(R.id.tvSpeechModeCategory);
        rvNotSpeechModeRecordsList = (RecyclerView) rootView.findViewById(R.id.rvNotSpeechModeRecordsList);
        spSpeechCurrency = (Spinner) rootView.findViewById(R.id.spSpeechCurrency);
        spSpeechAccount = (Spinner) rootView.findViewById(R.id.spSpeechAccount);
        tvSpeechModeAdjective = (TextView) rootView.findViewById(R.id.tvSpeechModeAdjective);
        autoSave = (TextView) rootView.findViewById(R.id.tvAutoSaveVoice);
        curString = new String[daoSession.getCurrencyDao().loadAll().size()];
        for (int i = 0; i < curString.length; i++) {
            curString[i] = daoSession.getCurrencyDao().loadAll().get(i).getAbbr();
        }
        ArrayAdapter<String> curAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, curString);
        spSpeechCurrency.setAdapter(curAdapter);
        accString = new String[daoSession.getAccountDao().loadAll().size()];
        for (int i = 0; i < accString.length; i++) {
            accString[i] = daoSession.getAccountDao().loadAll().get(i).getName();
        }
        final ArrayAdapter<String> accAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, accString);
        spSpeechAccount.setAdapter(accAdapter);
        rlCenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!started) {
                    askForContactPermission();
                    startRecognition();
                    visibilLR();
                    if (rlNotSpeechMode.getVisibility() == View.VISIBLE) {
                        refreshMode(true);
                    }
                } else {
                    visibilityGoneLR();
                    stopRecognition();
                }
                started = !started;
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvNotSpeechModeRecordsList.setLayoutManager(layoutManager);
        ViewGroup.LayoutParams params = rvNotSpeechModeRecordsList.getLayoutParams();
        params.height = (int) (8 * Utils.convertDpToPixel(getResources().getDimension(R.dimen.thirty_dp) + 26));
        rvNotSpeechModeRecordsList.setLayoutParams(params);
        tvSpeechModeEnteredText = (TextView) rootView.findViewById(R.id.tvSpeechModeEnteredText);
        tvListeningIndicator = (TextView) rootView.findViewById(R.id.tvListeningIndicator);
        recStartLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timer != null) {
                    // canceled task
                    timer.cancel();
                    timer = null;
                    tvSpeechModeAdjective.setText("");
                    tvSpeechAmount.setText("0.0");
                    categoryId = "";
                    accountId = "";
                    currencyId = "";
                    summ = 0;
                }
                visibilityGoneLR();
                refreshMode(false);
            }
        });
        recStartRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!categoryId.isEmpty())
                    savingVoice();
                else
                    visibilityGoneLR();
                refreshMode(false);
            }
        });
        recognizer = new PASpeechRecognizer(getContext());
        recognizer.setSpeechListener(new SpeechListener() {
            @Override
            public void onSpeechEnd(List<String> speechResult) {
                processSpeechResults(speechResult);
            }

            @Override
            public void onSpeechPartialListening(List<String> speechResult) {
                processSpeechResults(speechResult);
            }

            @Override
            public void onChangeState(final boolean started) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (started) {
                            ivCenterButton.setBackgroundResource(R.drawable.speech_pressed_circle);
                            ivMicrophoneIcon.setColorFilter(Color.WHITE);
                        } else {
                            ivCenterButton.setBackgroundResource(R.drawable.white_circle);
                            ivMicrophoneIcon.setColorFilter(Color.parseColor("#414141"));
                        }
                        VoiceRecognizerFragment.this.started = started;
                    }
                });
            }
        });
        visibilityGoneLR();
        refreshMode(false);
        return rootView;
    }

    private void visibilityGoneLR() {
        recStartRight.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.left_anim));
        recStartLeft.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.right_gone_anim));
        recStartRight.setVisibility(View.GONE);
        recStartLeft.setVisibility(View.GONE);
        paFragmentManager.setVerticalScrolling(true);
        stopRecognition();
    }

    private void visibilLR() {
        recStartRight.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.left_gone_anim));
        recStartLeft.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.right_anim));
        rlNotSpeechMode.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.left_gone_anim));
        recStartRight.setVisibility(View.VISIBLE);
        recStartLeft.setVisibility(View.VISIBLE);
        paFragmentManager.setVerticalScrolling(false);
    }

    private void refreshMode(boolean isRecord) {
        if (isRecord) {
            rlNotSpeechMode.setVisibility(View.GONE);
            llSpeechMode.setVisibility(View.VISIBLE);
        } else {
            rlNotSpeechMode.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.left_gone_anim));
            rlNotSpeechMode.setVisibility(View.VISIBLE);
            llSpeechMode.setVisibility(View.GONE);
            rvNotSpeechModeRecordsList.setAdapter(new MyAfterSavedAdapter());
        }
    }

    private void processSpeechResults(final List<String> speechResult) {
        if (speechResult != null && !speechResult.isEmpty()) {
            tvSpeechModeEnteredText.setText(speechResult.get(0));
            parseVoice(speechResult.get(0));
        }
    }

    private void startRecognition() {
        ivCenterButton.setBackgroundResource(R.drawable.speech_pressed_circle);
        ivMicrophoneIcon.setColorFilter(Color.WHITE);
        recognizer.startVoiceRecognitionCycle();
    }

    private void stopRecognition() {
        ivCenterButton.setBackgroundResource(R.drawable.white_circle);
        ivMicrophoneIcon.setColorFilter(Color.parseColor("#414141"));
        recognizer.stopVoiceRecognition();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.stopVoiceRecognition();
    }

    private String categoryId = "";
    private String accountId = "";
    private String currencyId = "";
    private double summ = 0;

    private TemplateVoice templateVoice = null;

    private class MyTask extends AsyncTask<Void, Void, Void> {
        private String newLetter = "";
        private List<TemplateVoice> successTemplates;
        private List<TemplateAccount> templateAccounts;

        public MyTask(String newLetter) {
            this.newLetter = newLetter;
            successTemplates = new ArrayList<>();
            templateAccounts = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //finding a category
            //accumulating suitable to collection of regular expressions
            for (TemplateVoice temp : voices) {
                if (newLetter.matches(temp.getRegex())) {
                    successTemplates.add(temp);
                }
            }
            //after accumulating we begin finding suitable for us category
            if (!successTemplates.isEmpty()) {
                if (successTemplates.size() == 1) { // if only one suitable
                    categoryId = successTemplates.get(0).getCategoryId();
                } else { // otherwise
                    //It need to find and to concentrate attention in last said word, which suits with one of category or subcategory
                    //end pos of last said word, which suits to one of category or subcategory
                    int endPos = -1;
                    //finding end pos
                    for (TemplateVoice successTemplate : successTemplates) {
                        if (newLetter.lastIndexOf(successTemplate.getCatName())
                                + successTemplate.getCatName().length() > endPos || endPos < 0) {
                            endPos = newLetter.lastIndexOf(successTemplate.getCatName())
                                    + successTemplate.getCatName().length();
                        }
                        if (successTemplate.getSubCatName() == null) continue;
                        if (newLetter.lastIndexOf(successTemplate.getSubCatName()) +
                                successTemplate.getSubCatName().length() > endPos || endPos < 0) {
                            endPos = newLetter.lastIndexOf(successTemplate.getSubCatName())
                                    + successTemplate.getSubCatName().length();
                        }
                    }
                    //after end pos found, filtering templates, those not suitable for said sentence's last found category
                    for (int i = 0; i < successTemplates.size(); i++) {
                        boolean isAccess = true;
                        String name = successTemplates.get(i).getCatName();
                        if (newLetter.lastIndexOf(name) >= 0 &&
                                name.length() + newLetter.lastIndexOf(name) == endPos) {
                            isAccess = false;
                        }
                        name = successTemplates.get(i).getSubCatName();
                        if (name != null) {
                            if (newLetter.lastIndexOf(name) >= 0 &&
                                    name.length() + newLetter.lastIndexOf(name) == endPos) {
                                isAccess = false;
                            }
                        }
                        if (isAccess) {
                            successTemplates.remove(i);
                            i--;
                        }
                    }

                    //filtering again for choose more suitable template. if sad: "expense motor oil 100$", we must choose as category word motor oil, not only oil.
                    List<TemplateVoice> cloneTemp = new ArrayList<>();
                    cloneTemp.addAll(successTemplates);
                    for (int i = 0; i < cloneTemp.size(); i++) {
                        for (int j = 0; j < successTemplates.size(); j++) {
                            if (!cloneTemp.get(i).getCategoryId().equals(successTemplates.get(j).getCategoryId())) {
                                if (cloneTemp.get(i).getCatName().toLowerCase().contains(successTemplates.get(j).getCatName().toLowerCase())) {
                                    successTemplates.remove(j);
                                    break;
                                }
                                if (!cloneTemp.get(i).getSubCatName().equals(successTemplates.get(j).getSubCatName())
                                        && cloneTemp.get(i).getSubCatName().toLowerCase().contains(successTemplates.get(j).getSubCatName().toLowerCase())) {
                                    successTemplates.remove(j);
                                    break;
                                }
                            }
                        }
                    }

                    //check, there is found more than one template, we must choose one of them using order priority
                    if (successTemplates.size() > 1) {
                        //giving priority to each of templates:
                        //priority 1: only one of between category and subcategory is suitable - bad
                        //priority 2: both suitable - good
                        //priority 3: for template, which hasn't subcategory - norm
                        for (TemplateVoice successTemplate : successTemplates) {
                            setPriority(successTemplate, newLetter);
                        }
                        Collections.sort(successTemplates, new Comparator<TemplateVoice>() {
                            @Override
                            public int compare(TemplateVoice templateVoice, TemplateVoice t1) {
                                return (new Integer(templateVoice.getPriority())).compareTo((new Integer(t1.getPriority())));
                            }
                        });
                        //boolean for finding better priority
                        boolean tek = false;
                        for (TemplateVoice successTemplate : successTemplates) {
                            if (successTemplate.getPriority() == 2) {
                                tek = true;
                                break;
                            }
                        }
                        int pr;
                        if (tek) {
                            pr = 2; //for good priority
                        } else {
                            pr = successTemplates.get(successTemplates.size() - 1).getPriority(); // for other priorities
                        }
                        //after found, we are leaving only one template which priority is better
                        for (int i = 0; i < successTemplates.size(); i++) {
                            if (pr != successTemplates.get(i).getPriority()) {
                                successTemplates.remove(i);
                                i--;
                            }
                        }
                        //after priority check
                        if (successTemplates.size() == 1) { // if found
                            categoryId = successTemplates.get(0).getCategoryId();
                        } else { // otherwise
                            //finding nearest pair to last said word, which suits to one of category of subcategory
                            //pos for saving position of found template
                            int pos = 0;
                            //for saving data, which contains distance between pairs
                            int diff = -1;
                            String[] splitText = newLetter.split(" ");
                            for (int i = 0; i < successTemplates.size(); i++) {
                                int first = 0;
                                int second = 0;
                                for (int j = 0; j < splitText.length; j++) {
                                    if (splitText[j].equals(successTemplates.get(i).getCatName())) {
                                        first = j;
                                    }
                                    if (successTemplates.get(i).getSubCatName() != null &&
                                            splitText[j].equals(successTemplates.get(i).getSubCatName())) {
                                        second = j;
                                    }
                                }
                                if (diff < 0 || Math.abs(first - second) < diff) {
                                    diff = Math.abs(first - second);
                                    pos = i;
                                }
                            }
                            //after finding
                            categoryId = successTemplates.get(pos).getCategoryId();
                        }
                    } else if (!successTemplates.isEmpty()) { // if success templates size = 1
                        categoryId = successTemplates.get(0).getCategoryId();
                    }
                }
                //passing data to another block
                for (TemplateVoice voice : successTemplates) {
                    if (voice.getCategoryId().equals(categoryId)) {
                        templateVoice = voice;
                        break;
                    }
                }
            }
            //finding an account
            for (TemplateAccount voice : templateAccountVoices) {
                if (newLetter.matches(voice.getRegex())) {
                    templateAccounts.add(voice);
                }
            }
            //found suitable account regular expressions
            if (!templateAccounts.isEmpty()) {
                if (templateAccounts.size() == 1) {
                    accountId = templateAccounts.get(0).getAccountId();
                } else {
                    String[] split = newLetter.split(" ");
                    int pos = -1;

                    for (String s : split) {
                        for (int i = 0; i < templateAccounts.size(); i++) {
                            if (pos < 0 || (templateAccounts.get(i).getAccountName().startsWith(s) && pos < i)) {
                                pos = i;
                            }
                        }
                    }
                    for (int i = 0; i < templateAccounts.size(); i++) {
                        if (!split[pos].equals(templateAccounts.get(i).getAccountName().toLowerCase())) {
                            templateAccounts.remove(i);
                            i--;
                        }
                    }
                    if (!templateAccounts.isEmpty()) {
                        accountId = templateAccounts.get(0).getAccountId();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            String s = "";
            if (templateVoice != null) {
                if (templateVoice.getSubCatName() != null && !templateVoice.getSubCatName().isEmpty())
                    s = " " + templateVoice.getSubCatName();
                tvSpeechModeCategory.setText(templateVoice.getCatName() + s);
            }
            String amountRegex = "([([^0-9]*)\\s*([0-9]+[.,]?[0-9]*)]*\\s([$]*)([0-9]+[.,]?[0-9]*).*)|(^([0-9]+[.,]?[0-9]*).*)";
            Pattern pattern = Pattern.compile(amountRegex);
            Matcher matcher = pattern.matcher(newLetter);
            final int firstOrGroup = 3, secondOrGroup = 5;
            if (matcher.matches()) {
                if (matcher.group(firstOrGroup) != null)
                    summ = Double.parseDouble(matcher.group(firstOrGroup));
                if (matcher.group(secondOrGroup) != null)
                    summ = Double.parseDouble(matcher.group(secondOrGroup));
            }
            tvSpeechAmount.setText(Double.toString(summ));
            if (accountId != null && !accountId.isEmpty()) {
                Account account = daoSession.getAccountDao().load(accountId);
                for (int i = 0; i < accString.length; i++) {
                    if (account.getName().toLowerCase().equals(accString[i].toLowerCase())) {
                        spSpeechAccount.setSelection(i);
                        break;
                    }
                }
            }
            if (categoryId != null && !categoryId.isEmpty() && summ != 0)
                savingVoice();
        }
    }

    private MyTask myTask;

    private void parseVoice(final String newLetter) {
        List<TemplateVoice> successTemplates = new ArrayList<>();
        List<TemplateAccount> templateAccounts = new ArrayList<>();
        //finding a category
        //accumulating suitable to collection of regular expressions
        for (TemplateVoice temp : voices) {
            if (newLetter.matches(temp.getRegex())) {
                successTemplates.add(temp);
            }
        }
        //after accumulating we begin finding suitable for us category
        if (!successTemplates.isEmpty()) {
            if (successTemplates.size() == 1) { // if only one suitable
                categoryId = successTemplates.get(0).getCategoryId();
            } else { // otherwise
                //It need to find and to concentrate attention in last said word, which suits with one of category or subcategory
                //end pos of last said word, which suits to one of category or subcategory
                int endPos = -1;
                //finding end pos
                for (TemplateVoice successTemplate : successTemplates) {
                    if (newLetter.lastIndexOf(successTemplate.getCatName())
                            + successTemplate.getCatName().length() > endPos || endPos < 0) {
                        endPos = newLetter.lastIndexOf(successTemplate.getCatName())
                                + successTemplate.getCatName().length();
                    }
                    if (successTemplate.getSubCatName() == null) continue;
                    if (newLetter.lastIndexOf(successTemplate.getSubCatName()) +
                            successTemplate.getSubCatName().length() > endPos || endPos < 0) {
                        endPos = newLetter.lastIndexOf(successTemplate.getSubCatName())
                                + successTemplate.getSubCatName().length();
                    }
                }
                //after end pos found, filtering templates, those not suitable for said sentence's last found category
                for (int i = 0; i < successTemplates.size(); i++) {
                    boolean isAccess = true;
                    String name = successTemplates.get(i).getCatName();
                    if (newLetter.lastIndexOf(name) >= 0 &&
                            name.length() + newLetter.lastIndexOf(name) == endPos) {
                        isAccess = false;
                    }
                    name = successTemplates.get(i).getSubCatName();
                    if (name != null) {
                        if (newLetter.lastIndexOf(name) >= 0 &&
                                name.length() + newLetter.lastIndexOf(name) == endPos) {
                            isAccess = false;
                        }
                    }
                    if (isAccess) {
                        successTemplates.remove(i);
                        i--;
                    }
                }

                //filtering again for choose more suitable template. if sad: "expense motor oil 100$", we must choose as category word motor oil, not only oil.
                List<TemplateVoice> cloneTemp = new ArrayList<>();
                cloneTemp.addAll(successTemplates);
                for (int i = 0; i < cloneTemp.size(); i++) {
                    for (int j = 0; j < successTemplates.size(); j++) {
                        if (!cloneTemp.get(i).getCategoryId().equals(successTemplates.get(j).getCategoryId())) {
                            if (cloneTemp.get(i).getCatName().toLowerCase().contains(successTemplates.get(j).getCatName().toLowerCase())) {
                                successTemplates.remove(j);
                                break;
                            }
                            if (!cloneTemp.get(i).getSubCatName().equals(successTemplates.get(j).getSubCatName())
                                    && cloneTemp.get(i).getSubCatName().toLowerCase().contains(successTemplates.get(j).getSubCatName().toLowerCase())) {
                                successTemplates.remove(j);
                                break;
                            }
                        }
                    }
                }

                //check, there is found more than one template, we must choose one of them using order priority
                if (successTemplates.size() > 1) {
                    //giving priority to each of templates:
                    //priority 1: only one of between category and subcategory is suitable - bad
                    //priority 2: both suitable - good
                    //priority 3: for template, which hasn't subcategory - norm
                    for (TemplateVoice successTemplate : successTemplates) {
                        setPriority(successTemplate, newLetter);
                    }
                    Collections.sort(successTemplates, new Comparator<TemplateVoice>() {
                        @Override
                        public int compare(TemplateVoice templateVoice, TemplateVoice t1) {
                            return (new Integer(templateVoice.getPriority())).compareTo((new Integer(t1.getPriority())));
                        }
                    });
                    //boolean for finding better priority
                    boolean tek = false;
                    for (TemplateVoice successTemplate : successTemplates) {
                        if (successTemplate.getPriority() == 2) {
                            tek = true;
                            break;
                        }
                    }
                    int pr;
                    if (tek) {
                        pr = 2; //for good priority
                    } else {
                        pr = successTemplates.get(successTemplates.size() - 1).getPriority(); // for other priorities
                    }
                    //after found, we are leaving only one template which priority is better
                    for (int i = 0; i < successTemplates.size(); i++) {
                        if (pr != successTemplates.get(i).getPriority()) {
                            successTemplates.remove(i);
                            i--;
                        }
                    }
                    //after priority check
                    if (successTemplates.size() == 1) { // if found
                        categoryId = successTemplates.get(0).getCategoryId();
                    } else { // otherwise
                        //finding nearest pair to last said word, which suits to one of category of subcategory
                        //pos for saving position of found template
                        int pos = 0;
                        //for saving data, which contains distance between pairs
                        int diff = -1;
                        String[] splitText = newLetter.split(" ");
                        for (int i = 0; i < successTemplates.size(); i++) {
                            int first = 0;
                            int second = 0;
                            for (int j = 0; j < splitText.length; j++) {
                                if (splitText[j].equals(successTemplates.get(i).getCatName())) {
                                    first = j;
                                }
                                if (successTemplates.get(i).getSubCatName() != null &&
                                        splitText[j].equals(successTemplates.get(i).getSubCatName())) {
                                    second = j;
                                }
                            }
                            if (diff < 0 || Math.abs(first - second) < diff) {
                                diff = Math.abs(first - second);
                                pos = i;
                            }
                        }
                        //after finding
                        categoryId = successTemplates.get(pos).getCategoryId();
                    }
                } else if (!successTemplates.isEmpty()) { // if success templates size = 1
                    categoryId = successTemplates.get(0).getCategoryId();
                }
            }
            //passing data to another block
            for (TemplateVoice voice : successTemplates) {
                if (voice.getCategoryId().equals(categoryId)) {
                    templateVoice = voice;
                    break;
                }
            }
        }
        //finding an account
        for (TemplateAccount voice : templateAccountVoices) {
            if (newLetter.matches(voice.getRegex())) {
                templateAccounts.add(voice);
            }
        }
        //found suitable account regular expressions
        if (!templateAccounts.isEmpty()) {
            if (templateAccounts.size() == 1) {
                accountId = templateAccounts.get(0).getAccountId();
            } else {
                String[] split = newLetter.split(" ");
                int pos = -1;

                for (String s : split) {
                    for (int i = 0; i < templateAccounts.size(); i++) {
                        if (pos < 0 || (templateAccounts.get(i).getAccountName().startsWith(s) && pos < i)) {
                            pos = i;
                        }
                    }
                }
                for (int i = 0; i < templateAccounts.size(); i++) {
                    if (!split[pos].equals(templateAccounts.get(i).getAccountName().toLowerCase())) {
                        templateAccounts.remove(i);
                        i--;
                    }
                }
                if (!templateAccounts.isEmpty()) {
                    accountId = templateAccounts.get(0).getAccountId();
                }
            }
        }

        String s = "";
        if (templateVoice != null) {

            if (templateVoice.getSubCatName() != null && !templateVoice.getSubCatName().isEmpty())
                s = " " + templateVoice.getSubCatName();
            tvSpeechModeCategory.setText(templateVoice.getCatName() + s);
        }
        String amountRegex = "([([^0-9]*)\\s*([0-9]+[.,]?[0-9]*)]*\\s([$]*)([0-9]+[.,]?[0-9]*).*)|(^([0-9]+[.,]?[0-9]*).*)";
        Pattern pattern = Pattern.compile(amountRegex);
        Matcher matcher = pattern.matcher(newLetter);
        final int firstOrGroup = 3, secondOrGroup = 5;
        if (matcher.matches()) {
            if (matcher.group(firstOrGroup) != null)
                summ = Double.parseDouble(matcher.group(firstOrGroup));
            if (matcher.group(secondOrGroup) != null)
                summ = Double.parseDouble(matcher.group(secondOrGroup));
        }
        tvSpeechAmount.setText(Double.toString(summ));
        if (accountId != null && !accountId.isEmpty()) {
            Account account = daoSession.getAccountDao().load(accountId);
            for (int i = 0; i < accString.length; i++) {
                if (account.getName().toLowerCase().equals(accString[i].toLowerCase())) {
                    spSpeechAccount.setSelection(i);
                    break;
                }
            }
        }
        if (categoryId != null && !categoryId.isEmpty() && summ != 0) savingVoice();

//        if (myTask != null) {
//            myTask.cancel(true);
//            myTask = null;
//        }
//        myTask = new MyTask(newLetter);
//        myTask.execute();

//        accountThread.start();
//        categoryId = idTemp;
//        String amountRegex = "[([^0-9]*)\\s*([0-9]+[.,]?[0-9]*)]*\\s([$]*)([0-9]+[.,]?[0-9]*).*";
//        Pattern pattern = Pattern.compile(amountRegex);
//        Matcher matcher = pattern.matcher(newLetter);
//        if (matcher.matches()) {
//            summ = Double.parseDouble(matcher.group(matcher.groupCount()));
//        }
//        tvSpeechAmount.setText("" + summ);
//        if (!categoryId.isEmpty()) {
//            if (daoSession.getRootCategoryDao().load(categoryId) != null) {
//                RootCategory rootCategory = daoSession.getRootCategoryDao().load(categoryId);
//                if (rootCategory.getType() == PocketAccounterGeneral.INCOME)
//                    tvSpeechModeAdjective.setText(getResources().getString(R.string.income));
//                else tvSpeechModeAdjective.setText(getResources().getString(R.string.expanse));
//                tvSpeechModeCategory.setText(rootCategory.getName());
//            } else if (daoSession.getSubCategoryDao().load(categoryId) != null) {
//                SubCategory subCategory = daoSession.getSubCategoryDao().load(categoryId);
//                if (daoSession.getRootCategoryDao().load(subCategory.getParentId()).getType() == PocketAccounterGeneral.INCOME)
//                    tvSpeechModeAdjective.setText(getResources().getString(R.string.income));
//                else tvSpeechModeAdjective.setText(getResources().getString(R.string.expanse));
//                tvSpeechModeCategory.setText(daoSession.getRootCategoryDao().load
//                        (subCategory.getParentId()).getName() + ", " + subCategory.getName());
//            }
//            if (!accountId.isEmpty()) {
//                Account account = daoSession.getAccountDao().load(accountId);
//                for (int i = 0; i < accString.length; i++) {
//                    if (account.getName().toLowerCase().equals(accString[i])) {
//                        spSpeechAccount.setSelection(i);
//                        break;
//                    }
//                }
//            }
//            if (!currencyId.isEmpty()) {
//                Currency currency = daoSession.getCurrencyDao().load(currencyId);
//                for (int i = 0; i < curString.length; i++) {
//                    if (currency.getAbbr().equals(curString[i])) {
//                        spSpeechCurrency.setSelection(i);
//                        break;
//                    }
//                }
//            }
//            leftSaving = 5;
//            if (timer != null) {
//                timer.cancel();
//                timer = null;
//            }
//            autoSave.setVisibility(View.VISIBLE);
//            timer = new CountDownTimer(6000, 1000) {
//                @Override
//                public void onTick(long l) {
//                    autoSave.setText("sec " + Math.ceil(l / 1000));
//                }
//
//                @Override
//                public void onFinish() {
//                    autoSave.setVisibility(View.GONE);
//                    autoSave.setText("");
//                    savingVoice();
//                }
//            }.start();
//        }
    }

    private void setPriority(TemplateVoice voice, String newLine) {
        Pattern pattern = Pattern.compile(voice.getRegex());
        Matcher matcher = pattern.matcher(newLine);
        matcher.matches();
        if (voice.getSubCatName() == null || voice.getSubCatName().isEmpty()) {
            voice.setPriority(3);
            return;
        }
        for (List<Integer> val : voice.getPairs().values()) {
            int pr = 0;
            if (matcher.group(val.get(0)) != null) {
                pr++;
            }
            if (matcher.group(val.get(1)) != null) {
                pr++;
            }
            if (voice.getPriority() <= pr) {
                voice.setPriority(pr);
            }
        }
    }

    private void savingVoice() {
        if (!categoryId.isEmpty() && summ != 0) {
            if (daoSession.getRootCategoryDao().load(categoryId) != null ||
                    daoSession.getSubCategoryDao().load(categoryId) != null) {
                FinanceRecord financeRecord = new FinanceRecord();
                financeRecord.setDate(dataCache.getEndDate());
                financeRecord.setAmount(summ);
                financeRecord.setComment("");
                financeRecord.setRecordId(UUID.randomUUID().toString());
                if (accountId.isEmpty()) {
                    financeRecord.setAccount(daoSession.getAccountDao().queryBuilder().
                            where(AccountDao.Properties.Name.eq(spSpeechAccount.getSelectedItem())).unique());
                } else {
                    financeRecord.setAccount(daoSession.getAccountDao().load(accountId));
                }
                if (currencyId.isEmpty()) {
                    financeRecord.setCurrency(daoSession.getCurrencyDao().queryBuilder()
                            .where(CurrencyDao.Properties.Abbr.eq(spSpeechCurrency.getSelectedItem())).unique());
                } else {
                    financeRecord.setCurrency(daoSession.getCurrencyDao().load(currencyId));
                }
                if (daoSession.getRootCategoryDao().load(categoryId) != null) {
                    financeRecord.setCategory(daoSession.getRootCategoryDao().load(categoryId));
                } else {
                    SubCategory subCategory = daoSession.getSubCategoryDao().load(categoryId);
                    financeRecord.setCategory(daoSession.getRootCategoryDao().load(subCategory.getParentId()));
                    financeRecord.setSubCategory(subCategory);
                }
                daoSession.getFinanceRecordDao().insertOrReplace(financeRecord);
                paFragmentManager.updateAllFragmentsPageChanges();
                paFragmentManager.updateAllFragmentsOnViewPager();
//                timer.cancel();
//                timer = null;
                tvSpeechModeAdjective.setText("");
                tvSpeechAmount.setText("0.0");
                categoryId = "";
                accountId = "";
                currencyId = "";
                summ = 0;
            }
        }
        visibilityGoneLR();
        stopRecognition();
    }

    private class MyAfterSavedAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private List<FinanceRecord> financeRecords;
        private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        public MyAfterSavedAdapter() {
            financeRecords = daoSession.getFinanceRecordDao().queryBuilder()
                    .where(FinanceRecordDao.Properties.Date.eq(simpleDateFormat.format(
                            Calendar.getInstance().getTime()))).list();
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.itemAmount.setText("" + financeRecords.get(position).getAmount() +
                    financeRecords.get(position).getCurrency().getAbbr());
            if (financeRecords.get(position).getSubCategory() != null) {
                holder.itemCatName.setText(financeRecords.get(position).getCategory().getName()
                        + " " + financeRecords.get(position).getSubCategory().getName());
                holder.itemIcon.setImageResource(getResources().getIdentifier(financeRecords.get(position)
                        .getSubCategory().getIcon(), "drawable", getContext().getPackageName()));
            } else {
                holder.itemCatName.setText(financeRecords.get(position).getCategory().getName());
                holder.itemIcon.setImageResource(getResources().getIdentifier(financeRecords.get(position)
                        .getCategory().getIcon(), "drawable", getContext().getPackageName()));
            }
        }

        @Override
        public int getItemCount() {
            return financeRecords.size();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_after_saved_fragment, parent, false);
            return new MyViewHolder(view);
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemIcon;
        public TextView itemCatName;
        public TextView itemAmount;

        public MyViewHolder(View itemView) {
            super(itemView);
            itemIcon = (ImageView) itemView.findViewById(R.id.ivItemAfterSavedIcon);
            itemAmount = (TextView) itemView.findViewById(R.id.tvItemAfterSavedAmount);
            itemCatName = (TextView) itemView.findViewById(R.id.tvItemAfterSavedCatName);
        }
    }

    private final int PERMISSION_REQUEST_RECORD = 0;

    public void askForContactPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.RECORD_AUDIO)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.contact_access_needed);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setMessage(R.string.please_confirm_contact_access);//TODO put real question
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            requestPermissions(
                                    new String[]
                                            {Manifest.permission.RECORD_AUDIO}
                                    , PERMISSION_REQUEST_RECORD);
                        }
                    });
                    builder.show();
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_RECORD);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                return;
            }
        }
    }
}