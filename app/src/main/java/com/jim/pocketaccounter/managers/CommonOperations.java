package com.jim.pocketaccounter.managers;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import com.jim.pocketaccounter.PocketAccounterApplication;
import com.jim.pocketaccounter.R;
import com.jim.pocketaccounter.database.Account;
import com.jim.pocketaccounter.database.BoardButton;
import com.jim.pocketaccounter.database.CreditDetials;
import com.jim.pocketaccounter.database.Currency;
import com.jim.pocketaccounter.database.CurrencyCost;
import com.jim.pocketaccounter.database.CurrencyDao;
import com.jim.pocketaccounter.database.DaoSession;
import com.jim.pocketaccounter.database.DebtBorrow;
import com.jim.pocketaccounter.database.FinanceRecord;
import com.jim.pocketaccounter.database.Person;
import com.jim.pocketaccounter.database.PhotoDetails;
import com.jim.pocketaccounter.database.Recking;
import com.jim.pocketaccounter.database.ReckingCredit;
import com.jim.pocketaccounter.database.RootCategory;
import com.jim.pocketaccounter.database.RootCategoryDao;
import com.jim.pocketaccounter.database.SmsParseObject;
import com.jim.pocketaccounter.database.SubCategory;
import com.jim.pocketaccounter.utils.PocketAccounterGeneral;
import com.jim.pocketaccounter.utils.TemplateSms;

import org.apache.commons.lang3.math.NumberUtils;
import org.greenrobot.greendao.database.StandardDatabase;

import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by DEV on 01.09.2016.
 */

public class CommonOperations {
    @Inject
    DaoSession daoSession;
    private CurrencyDao currencyDao;
    private Context context;
    private Currency mainCurrency;
    @Inject @Named(value = "begin") Calendar begin;
    @Inject @Named(value = "end") Calendar end;
    @Inject SharedPreferences sharedPreferences;
    public CommonOperations(Context context) {
        ((PocketAccounterApplication) context.getApplicationContext()).component().inject(this);
        this.currencyDao = daoSession.getCurrencyDao();
        this.context = context;
    }
    public Currency getMainCurrency() {
        if (mainCurrency == null) {
            List<Currency> currencies = currencyDao.loadAll();
            for (Currency currency : currencies) {
                if (currency.getIsMain())
                    mainCurrency = currency;
            }
        }
        return mainCurrency;
    }
    public double getCost(FinanceRecord record) {
        double amount = 0.0;
        if (record.getCurrency().getMain())
            return record.getAmount();
        double koeff = 1.0;
        long diff = record.getDate().getTimeInMillis() - record.getCurrency().getCosts().get(0).getDay().getTimeInMillis();
        if (diff < 0) {
            koeff = record.getCurrency().getCosts().get(0).getCost();
            return record.getAmount()/koeff;
        }
        int pos = 0;
        while (diff >= 0 && pos < record.getCurrency().getCosts().size()) {
            diff = record.getDate().getTimeInMillis() - record.getCurrency().getCosts().get(pos).getDay().getTimeInMillis();
            if(diff>=0)
                koeff = record.getCurrency().getCosts().get(pos).getCost();
            pos++;
        }
        amount = record.getAmount()/koeff;
        return amount;
    }
    public double getCost(Calendar date, Currency currency, double amount) {
        if (currency.getMain()) return amount;
        double koeff = 1.0;
        long diff = date.getTimeInMillis() - currency.getCosts().get(0).getDay().getTimeInMillis();
        if (diff < 0) {
            koeff = currency.getCosts().get(0).getCost();
            return amount/koeff;
        }
        int pos = 0;
        while (diff >= 0 && pos < currency.getCosts().size()) {
            diff = date.getTimeInMillis() - currency.getCosts().get(pos).getDay().getTimeInMillis();
            if(diff>=0)
                koeff = currency.getCosts().get(pos).getCost();
            pos++;
        }
        amount = amount/koeff;
        return amount;
    }

    public double getCost(Calendar date, Currency fromCurrency, Currency toCurrency, double amount) {
        //TODO tekwir bir yana

        if (fromCurrency.getId().matches(toCurrency.getId())) return amount;
        double tokoeff = 1.0;
        double fromkoeff2 = 1.0;
        long todiff1 = date.getTimeInMillis() - toCurrency.getCosts().get(0).getDay().getTimeInMillis();
        long fromdiff = date.getTimeInMillis() - fromCurrency.getCosts().get(0).getDay().getTimeInMillis();
        if (todiff1 < 0) {
            tokoeff = toCurrency.getCosts().get(0).getCost();
        }
        if(fromdiff < 0){
            fromkoeff2 = fromCurrency.getCosts().get(0).getCost();
        }
        int pos = 0;
        while (todiff1 >= 0 && pos < toCurrency.getCosts().size()) {
            todiff1 = date.getTimeInMillis() - toCurrency.getCosts().get(pos).getDay().getTimeInMillis();
            if(todiff1>=0)
                tokoeff = toCurrency.getCosts().get(pos).getCost();
            pos++;
        }
        pos=0;
        while (fromdiff >= 0 && pos < fromCurrency.getCosts().size()) {
            fromdiff = date.getTimeInMillis() - fromCurrency.getCosts().get(pos).getDay().getTimeInMillis();
            if(fromdiff>=0)
                fromkoeff2 = fromCurrency.getCosts().get(pos).getCost();
            pos++;
        }
        amount = tokoeff*amount/fromkoeff2;
        return amount;
    }

    public int countOfDayBetweenCalendars(Calendar begin, Calendar end) {
        int countOfDays = 0;
        Calendar b = (Calendar) begin.clone();
        b.set(Calendar.HOUR_OF_DAY, 0);
        b.set(Calendar.MINUTE, 0);
        b.set(Calendar.SECOND, 0);
        b.set(Calendar.MILLISECOND, 0);
        Calendar e = (Calendar) end.clone();
        e.set(Calendar.HOUR_OF_DAY, 23);
        e.set(Calendar.MINUTE, 59);
        e.set(Calendar.SECOND, 59);
        e.set(Calendar.MILLISECOND, 59);
        while (b.compareTo(e) <= 0) {
            countOfDays++;
            b.add(Calendar.DAY_OF_MONTH, 1);
        }
        return countOfDays;
    }
    public float convertDpToPixel(float dp) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public  void ColorSubSeq(String text, String whichWordColor, String colorCode, TextView textView){
        String textUpper=text.toUpperCase();
        String whichWordColorUpper=whichWordColor.toUpperCase();
        SpannableString ss = new SpannableString(text);
        int strar=0;

        while (textUpper.indexOf(whichWordColorUpper,strar)>=0&&whichWordColor.length()!=0) {
            ss.setSpan(new BackgroundColorSpan(Color.parseColor(colorCode)),textUpper.indexOf(whichWordColorUpper,strar), textUpper.indexOf(whichWordColorUpper,strar)+whichWordColorUpper.length(),  Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            strar=textUpper.indexOf(whichWordColorUpper,strar)+whichWordColorUpper.length();
        }
        textView.setText(ss);
    }

    public long betweenDays(Calendar begin, Calendar end) {
        Calendar b = (Calendar) begin.clone();
        b.set(Calendar.HOUR_OF_DAY, 0);
        b.set(Calendar.MINUTE, 0);
        b.set(Calendar.SECOND, 0);
        b.set(Calendar.MILLISECOND, 0);
        Calendar e = (Calendar) end.clone();
        e.set(Calendar.HOUR_OF_DAY, 0);
        e.set(Calendar.MINUTE, 0);
        e.set(Calendar.SECOND, 0);
        e.set(Calendar.MILLISECOND, 0);
        long day = 24L*60L*60L*1000L;
        return (e.getTimeInMillis() - b.getTimeInMillis())/day;
    }


    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public List<TemplateSms> generateSmsTemplateList(String[] splittedText, int incExpPos, int amountPos, String[] incomeKeywords, String[] expenseKeywords, String[] amountKeywords) {
        String numberPattern = "[0-9]+[.,]?[0-9]*";
        List<TemplateSms> templates = new ArrayList<>();
        int amountBlockPos = 0;
        if (splittedText != null && incomeKeywords != null && amountKeywords == null) {
            for (String incomeKeyword : incomeKeywords) {
                int type = PocketAccounterGeneral.INCOME;
                String regex = ".*";
                if (incExpPos < amountPos) {
                    if (incExpPos+1 == amountPos) {
                        regex += "(\\b"+incomeKeyword+")\\s*";
                        regex += "("+numberPattern+").*";
                        amountBlockPos = 3;
                    }
                    else {
                        regex += "(\\b"+incomeKeyword+").*";
                        regex += "(\\b"+splittedText[amountPos-1]+")\\s*";
                        regex += "("+numberPattern+").*";
                        amountBlockPos = 2;
                    }
                }
                else {
                    regex = "";
                    if (amountPos != 0) {
                        regex += ".*";
                        regex += "(\\b" + splittedText[amountPos - 1] + ")\\s*";
                    }
                    regex += "("+numberPattern+").*";
                    regex += "(\\b"+incomeKeyword+").*";
                    if (amountPos != 0)
                        amountBlockPos = 2;
                    else
                        amountBlockPos = 1;

                }
                TemplateSms templateSms = new TemplateSms(regex, type, amountBlockPos);
                templates.add(templateSms);
            }
        }
        if (splittedText != null && expenseKeywords != null && amountKeywords == null) {
            for (String expenseKeyword : expenseKeywords) {
                int type = PocketAccounterGeneral.EXPENSE;
                String regex = ".*";
                if (incExpPos < amountPos) {
                    if (incExpPos+1 == amountPos) {
                        regex += "(\\b"+expenseKeyword+")\\s*";
                        regex += "("+numberPattern+").*";
                        amountBlockPos = 2;
                    }
                    else {
                        regex += "(\\b"+expenseKeyword+").*";
                        regex += "(\\b"+splittedText[amountPos-1]+")\\s*";
                        regex += "("+numberPattern+").*";
                        amountBlockPos = 3;
                    }
                }
                else {
                    regex = "";
                    if (amountPos != 0) {
                        regex += ".*";
                        regex += "(\\b" + splittedText[amountPos - 1] + ")\\s*";
                    }
                    regex += "("+numberPattern+").*";
                    regex += "(\\b"+expenseKeyword+").*";
                    if (amountPos != 0)
                        amountBlockPos = 2;
                    else
                        amountBlockPos = 1;
                }
                TemplateSms templateSms = new TemplateSms(regex, type, amountBlockPos);
                templates.add(templateSms);
            }
        }
        if (splittedText == null && amountKeywords != null && incomeKeywords != null) {
            for (String amountKeyword : amountKeywords) {
                for (String incomeKeyword : incomeKeywords) {
                    int type = PocketAccounterGeneral.INCOME;
                    String amountBlock = "amount_block";
                    String regex = "(.*((\\b"+incomeKeyword+").*(\\b"+amountKeyword+")\\s*("+numberPattern+")))|" +
                            "(.*((\\b"+amountKeyword+")\\s*("+numberPattern+").*(\\b"+incomeKeyword+")))";
                    amountBlockPos = 5;
                    int amountBlockPosSecond = 9;
                    TemplateSms templateSms = new TemplateSms(regex, type, amountBlockPos);
                    templateSms.setPosAmountGroupSecond(amountBlockPosSecond);
                    templates.add(templateSms);
                }
            }
        }
        return templates;
    }

    public Bitmap getRoundedCornerBitmap(Bitmap bitmap, int cornerRadius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, cornerRadius/8, cornerRadius/8, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public Calendar getFirstDay() {
        Calendar calendar = null;
        List<Account> accounts = daoSession.getAccountDao().loadAll();
        for (Account account : accounts) {
            if (calendar == null)
                calendar = (Calendar) account.getCalendar().clone();
            else {
                if (calendar.compareTo(account.getCalendar()) >= 0)
                    calendar = (Calendar) account.getCalendar().clone();
            }
        }
        return  calendar;
    }

    public boolean compareTimeInOneDay(Calendar source, Calendar target){
        begin.setTimeInMillis(source.getTimeInMillis());
        begin.set(Calendar.HOUR,0);
        begin.set(Calendar.MINUTE,0);
        begin.set(Calendar.SECOND,0);
        begin.set(Calendar.MILLISECOND,0);
        end.setTimeInMillis(source.getTimeInMillis());
        end.set(Calendar.HOUR,23);
        end.set(Calendar.MINUTE,59);
        end.set(Calendar.SECOND,59);
        end.set(Calendar.MILLISECOND,59);
        return begin.compareTo(target) <= 0 &&
                end.compareTo(target) >= 0;
    }

    public static void migrateDatabase(Context context, String filePath, DaoSession daoSession, SharedPreferences preferences) {
        preferences
                .edit()
                .putBoolean(PocketAccounterGeneral.DB_ONCREATE_ENTER, true)
                .commit();
        File migrationDbFile = new File(filePath);
        if (migrationDbFile.exists()) {
            SQLiteDatabase old = SQLiteDatabase.openDatabase(migrationDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            if (old.getVersion() == 2 && ((StandardDatabase) daoSession.getDatabase()).getSQLiteDatabase().getVersion() == 6) {
                old.execSQL("CREATE TABLE sms_parsing_table ("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "number TEXT,"
                        + "income_words TEXT,"
                        + "expense_words TEXT,"
                        + "amount_words TEXT,"
                        + "account_id TEXT,"
                        + "currency_id TEXT,"
                        + "type INTEGER,"
                        + "empty TEXT"
                        + ");");
			    upgradeFromThreeToFour(context, old);
                upgradeFromFourToFive(context, old);
                upgradeFiveToSix(context, old, daoSession);
            }
            if (old.getVersion() == 3 && ((StandardDatabase) daoSession.getDatabase()).getSQLiteDatabase().getVersion() == 6) {
                upgradeFromThreeToFour(context, old);
                upgradeFromFourToFive(context, old);
                upgradeFiveToSix(context, old, daoSession);
		    }

            if (old.getVersion() == 4 && ((StandardDatabase) daoSession.getDatabase()).getSQLiteDatabase().getVersion() == 6) {
                upgradeFromFourToFive(context, old);
                upgradeFiveToSix(context, old, daoSession);
            }
            if (old.getVersion() == 5 && ((StandardDatabase) daoSession.getDatabase()).getSQLiteDatabase().getVersion() == 6)
                upgradeFiveToSix(context, old, daoSession);
            //delete file
            if (old.getVersion()!=6 && migrationDbFile.delete())
                Log.d(PocketAccounterGeneral.TAG, migrationDbFile.getName() + " is deleted successfully !!!");
            else
                Log.d(PocketAccounterGeneral.TAG, "Can't delete file: " + migrationDbFile.getName() + ". Please try again...");
        }
    }
    public static void createDefaultDatas(SharedPreferences preferences, Context context, DaoSession daoSession) {
        preferences
                .edit()
                .putBoolean(PocketAccounterGeneral.DB_ONCREATE_ENTER, true)
                .commit();
        //inserting currencies
        String [] currencyNames = context.getResources().getStringArray(R.array.base_currencies);
        String [] currencyIds = context.getResources().getStringArray(R.array.currency_ids);
        String [] currencyCosts = context.getResources().getStringArray(R.array.currency_costs);
        String [] currencySigns = context.getResources().getStringArray(R.array.base_abbrs);

        for (int i=0; i<3; i++) {
            Currency currency = new Currency();
            currency.setName(currencyNames[i]);
            currency.setId(currencyIds[i]);
            currency.setMain(i == 0);
            currency.setAbbr(currencySigns[i]);
            CurrencyCost currencyCost = new CurrencyCost();
            currencyCost.setCurrencyId(currencyIds[i]);
            currencyCost.setDay(Calendar.getInstance());
            currencyCost.setCost(Double.parseDouble(currencyCosts[i]));
            daoSession.getCurrencyCostDao().insert(currencyCost);
            List<CurrencyCost> costs = new ArrayList<>();
            costs.add(currencyCost);
            currency.__setDaoSession(daoSession);
            currency.setCosts(costs);
            daoSession.getCurrencyDao().insertOrReplace(currency);
        }

        //inserting accounts
        String[] accountNames = context.getResources().getStringArray(R.array.account_names);
        String[] accountIds = context.getResources().getStringArray(R.array.account_ids);
        String[] accountIcons = context.getResources().getStringArray(R.array.account_icons);
        int[] icons = new int[accountIcons.length];
        for (int i=0; i<accountIcons.length; i++) {
            int resId = context.getResources().getIdentifier(accountIcons[i], "drawable", context.getPackageName());
            icons[i] = resId;
        }
        for (int i=0; i<accountNames.length; i++) {
            Account account = new Account();
            account.setName(accountNames[i]);
            account.setIcon(accountIcons[i]);
            account.setId(accountIds[i]);
            account.setStartMoneyCurrency(daoSession.getCurrencyDao().loadAll().get(0));
            account.setAmount(0.0d);
            account.setNoneMinusAccount(false);
            account.setCalendar(Calendar.getInstance());
            account.__setDaoSession(daoSession);
            daoSession.getAccountDao().insert(account);
        }

        //inserting categories
        String[] catValues = context.getResources().getStringArray(R.array.cat_values);
        String[] catTypes = context.getResources().getStringArray(R.array.cat_types);
        String[] catIcons = context.getResources().getStringArray(R.array.cat_icons);
        for (int i=0; i<catValues.length; i++) {
            RootCategory rootCategory = new RootCategory();
            int resId = context.getResources().getIdentifier(catValues[i], "string", context.getPackageName());
            rootCategory.setName(context.getResources().getString(resId));
            rootCategory.setId(catValues[i]);
            rootCategory.setType(Integer.parseInt(catTypes[i]));
            rootCategory.setIcon(catIcons[i]);
            int arrayId = context.getResources().getIdentifier(catValues[i], "array", context.getPackageName());
            if (arrayId != 0) {
                int subcatIconArrayId = context.getResources().getIdentifier(catValues[i]+"_icons", "array", context.getPackageName());
                String[] subCats = context.getResources().getStringArray(arrayId);
                String[] tempIcons = context.getResources().getStringArray(subcatIconArrayId);
                List<SubCategory> subCategories = new ArrayList<>();
                for (int j=0; j<subCats.length; j++) {
                    SubCategory subCategory = new SubCategory();
                    subCategory.setName(subCats[j]);
                    subCategory.setId(UUID.randomUUID().toString());
                    subCategory.setParentId(catValues[i]);
                    subCategory.setIcon(tempIcons[j]);
                    subCategories.add(subCategory);
                    subCategory.__setDaoSession(daoSession);
                    daoSession.getSubCategoryDao().insert(subCategory);
                }
                rootCategory.setSubCategories(subCategories);
                rootCategory.__setDaoSession(daoSession);
            }
            daoSession.getRootCategoryDao().insert(rootCategory);
        }

        List<RootCategory> incomes = daoSession.getRootCategoryDao()
                .queryBuilder().where(RootCategoryDao.Properties.Type.eq(PocketAccounterGeneral.INCOME))
                .list();
        String[] operationIds = context.getResources().getStringArray(R.array.operation_ids);
        String backIds = operationIds[5];
        String forwardId = operationIds[4];
        BoardButton boardButton;
        for (int i = 0; i < 4; i++) {
            if (incomes.size() - 1 == i) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(forwardId);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.INCOME);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
            else {
                boardButton = new BoardButton();
                if (incomes.size() <= i || incomes.get(i) == null)
                    boardButton.setCategoryId(null);
                else
                    boardButton.setCategoryId(incomes.get(i).getId());
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.INCOME);
                boardButton.setType(PocketAccounterGeneral.CATEGORY);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
        }
        int page = 2;
        for (int i=4; i<40; i++) {
            if ((i+1)%(page*4) == 0) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(forwardId);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.INCOME);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
                page++;
            }
            else {
                boardButton = new BoardButton();
                boardButton.setCategoryId(null);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.INCOME);
                boardButton.setType(PocketAccounterGeneral.CATEGORY);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
        }
        List<RootCategory> expenses = daoSession.getRootCategoryDao()
                .queryBuilder().where(RootCategoryDao.Properties.Type.eq(PocketAccounterGeneral.EXPENSE))
                .list();
        for (int i = 0; i < 16; i++) {
            if (i == expenses.size()-2) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(backIds);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
            else if (i == expenses.size()-1) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(forwardId);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
            else {
                boardButton = new BoardButton();
                if (expenses.size() <= i || expenses.get(i) == null)
                    boardButton.setCategoryId(null);
                else
                    boardButton.setCategoryId(expenses.get(i).getId());
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
        }
        page = 2;
        for (int i = 16; i < 160; i++) {
            if ((i+2)%(page*16) == 0) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(backIds);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
            else if ((i+1)%(page*16) == 0) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(forwardId);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
                page++;
            }
            else {
                boardButton = new BoardButton();
                boardButton.setCategoryId(null);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.CATEGORY);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
        }
    }
    private static void upgradeFiveToSix(Context context, SQLiteDatabase old, DaoSession daoSession) {
        Cursor cursor = old.query("currency_table", null, null, null, null, null, null);
        Cursor costCursor = old.query("currency_costs_table", null, null, null, null, null, null);
        //loading currencies
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        List<Currency> currencies = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Currency newCurrency = new Currency(cursor.getString(cursor.getColumnIndex("currency_name")));
            newCurrency.setAbbr(cursor.getString(cursor.getColumnIndex("currency_sign")));
            String currId = cursor.getString(cursor.getColumnIndex("currency_id"));
            newCurrency.setId(currId);
            newCurrency.setIsMain(cursor.getInt(cursor.getColumnIndex("currency_main"))!=0);
            newCurrency.setMain(cursor.getInt(cursor.getColumnIndex("currency_main"))!=0);
            costCursor.moveToFirst();
            while(!costCursor.isAfterLast()) {
                if (costCursor.getString(costCursor.getColumnIndex("currency_id")).matches(currId)) {
                    CurrencyCost newCurrencyCost = new CurrencyCost();
                    try {
                        Calendar day = Calendar.getInstance();
                        day.setTime(dateFormat.parse(costCursor.getString(costCursor.getColumnIndex("date"))));
                        newCurrencyCost.setDay(day);
                    } catch (ParseException e) {
                        e.printStackTrace();

                    }
                    newCurrencyCost.setCost(costCursor.getDouble(costCursor.getColumnIndex("cost")));
                    newCurrencyCost.setCurrencyId(currId);
                    newCurrency.__setDaoSession(daoSession);
                    newCurrency.getCosts().add(newCurrencyCost);
                    daoSession.getCurrencyCostDao().insertOrReplace(newCurrencyCost);
                }
                costCursor.moveToNext();
            }
            daoSession.getCurrencyDao().insertOrReplace(newCurrency);
            currencies.add(newCurrency);
            cursor.moveToNext();
        }
        costCursor.close();
        //loading categories
        List<RootCategory> categories = new ArrayList<>();
        Cursor catCursor = old.query("category_table", null, null, null, null, null, null);
        Cursor subcatCursor = old.query("subcategory_table", null, null, null, null, null, null);
        catCursor.moveToFirst();
        while(!catCursor.isAfterLast()) {
            RootCategory newCategory = new RootCategory();
            newCategory.setName(catCursor.getString(catCursor.getColumnIndex("category_name")));
            String catId = catCursor.getString(catCursor.getColumnIndex("category_id"));
            newCategory.setId(catId);
            newCategory.setType(catCursor.getInt(catCursor.getColumnIndex("category_type")));
            newCategory.setIcon(catCursor.getString(catCursor.getColumnIndex("icon")));
            subcatCursor.moveToFirst();
            List<SubCategory> subCats = new ArrayList<>();
            while(!subcatCursor.isAfterLast()) {
                if (subcatCursor.getString(subcatCursor.getColumnIndex("category_id")).matches(catId)) {
                    SubCategory newSubCategory = new SubCategory();
                    newSubCategory.setName(subcatCursor.getString(subcatCursor.getColumnIndex("subcategory_name")));
                    newSubCategory.setId(subcatCursor.getString(subcatCursor.getColumnIndex("subcategory_id")));
                    newSubCategory.setParentId(catId);
                    newSubCategory.setIcon(subcatCursor.getString(subcatCursor.getColumnIndex("icon")));
                    subCats.add(newSubCategory);
                    daoSession.getSubCategoryDao().insertOrReplace(newSubCategory);
                }
                subcatCursor.moveToNext();
            }
            newCategory.setSubCategories(subCats);
            daoSession.getRootCategoryDao().insertOrReplace(newCategory);
            categories.add(newCategory);
            catCursor.moveToNext();
        }
        catCursor.close();
        subcatCursor.close();
        //load incomes
        ArrayList<RootCategory> incomes = new ArrayList<>();
        cursor = old.query("incomes_table", null, null, null, null, null, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            RootCategory newCategory = new RootCategory();
            if (cursor.getString(cursor.getColumnIndex("category_name")).matches(context.getResources().getString(R.string.no_category))) {
                incomes.add(null);
                cursor.moveToNext();
                continue;
            }
            newCategory.setName(cursor.getString(cursor.getColumnIndex("category_name")));
            newCategory.setId(cursor.getString(cursor.getColumnIndex("category_id")));
            newCategory.setType(cursor.getInt(cursor.getColumnIndex("category_type")));
            newCategory.setIcon(cursor.getString(cursor.getColumnIndex("icon")));
            incomes.add(newCategory);
            cursor.moveToNext();
        }

        //load expenses
        ArrayList<RootCategory> expenses = new ArrayList<RootCategory>();
        cursor = old.query("expanses_table", null, null, null, null, null, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            RootCategory newCategory = new RootCategory();
            if (cursor.getString(cursor.getColumnIndex("category_name")).matches(context.getResources().getString(R.string.no_category))) {
                expenses.add(null);
            } else {
                newCategory.setName(cursor.getString(cursor.getColumnIndex("category_name")));
                newCategory.setId(cursor.getString(cursor.getColumnIndex("category_id")));
                newCategory.setType(cursor.getInt(cursor.getColumnIndex("category_type")));
                newCategory.setIcon(cursor.getString(cursor.getColumnIndex("icon")));
                expenses.add(newCategory);
            }
            cursor.moveToNext();
        }
        String[] operationIds = context.getResources().getStringArray(R.array.operation_ids);
        String backIds = operationIds[5];
        String forwardId = operationIds[4];
        BoardButton boardButton;
        for (int i = 0; i < 4; i++) {
            boardButton = new BoardButton();
            if (incomes.size() <= i || incomes.get(i) == null)
                boardButton.setCategoryId(null);
            else
                boardButton.setCategoryId(incomes.get(i).getId());
            boardButton.setPos(i);
            boardButton.setTable(PocketAccounterGeneral.INCOME);
            boardButton.setType(PocketAccounterGeneral.CATEGORY);
            daoSession.getBoardButtonDao().insertOrReplace(boardButton);
        }
        int page = 2;
        for (int i=4; i<40; i++) {
            if ((i+1)%(page*4) == 0) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(forwardId);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.INCOME);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
                page++;
            }
            else {
                boardButton = new BoardButton();
                boardButton.setCategoryId(null);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.INCOME);
                boardButton.setType(PocketAccounterGeneral.CATEGORY);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
        }

        for (int i = 0; i < 16; i++) {
            boardButton = new BoardButton();
            if (expenses.size() <= i || expenses.get(i) == null)
                boardButton.setCategoryId(null);
            else
                boardButton.setCategoryId(expenses.get(i).getId());
            boardButton.setPos(i);
            boardButton.setTable(PocketAccounterGeneral.EXPENSE);
            daoSession.getBoardButtonDao().insertOrReplace(boardButton);
        }
        page = 2;
        for (int i = 16; i < 160; i++) {
            if ((i+2)%(page*16) == 0) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(backIds);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
            else if ((i+1)%(page*16) == 0) {
                boardButton = new BoardButton();
                boardButton.setCategoryId(forwardId);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.FUNCTION);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
                page++;
            }
            else {
                boardButton = new BoardButton();
                boardButton.setCategoryId(null);
                boardButton.setPos(i);
                boardButton.setTable(PocketAccounterGeneral.EXPENSE);
                boardButton.setType(PocketAccounterGeneral.CATEGORY);
                daoSession.getBoardButtonDao().insertOrReplace(boardButton);
            }
        }
        ArrayList<Account> accounts = new ArrayList<>();
        cursor = old.query("account_table", null, null, null, null, null, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            Account newAccount = new Account();
            newAccount.setName(cursor.getString(cursor.getColumnIndex("account_name")));
            newAccount.setId(cursor.getString(cursor.getColumnIndex("account_id")));
            int resId = cursor.getInt(cursor.getColumnIndex("icon"));
            boolean iconFound = false;
            int pos = 0;
            String icons[] = context.getResources().getStringArray(R.array.icons);
            for (int i=0; i<icons.length; i++) {
                if (context.getResources().getIdentifier(icons[i], "drawable", context.getPackageName()) == resId) {
                    iconFound = true;
                    pos = i;
                    break;
                }
            }
            newAccount.setIcon(iconFound ? icons[pos] : "icons_25");
            newAccount.setAmount(cursor.getDouble(cursor.getColumnIndex("start_amount")));
            String startMoneyCurrencyId = cursor.getString(cursor.getColumnIndex("start_money_currency_id"));
            String limitCurrencyId = cursor.getString(cursor.getColumnIndex("limit_currency_id"));
            newAccount.setLimitCurId(limitCurrencyId);
            if (startMoneyCurrencyId != null) {
                for (Currency currency : currencies) {
                    if (currency.getId().matches(startMoneyCurrencyId)) {
                        newAccount.setStartMoneyCurrency(currency);
                        break;
                    }
                }
            }
            newAccount.setIsLimited(cursor.getInt(cursor.getColumnIndex("is_limited")) != 0);
            newAccount.setLimite(cursor.getDouble(cursor.getColumnIndex("limit_amount")));
            newAccount.setCalendar(Calendar.getInstance());
            accounts.add(newAccount);
            daoSession.getAccountDao().insertOrReplace(newAccount);
            cursor.moveToNext();
        }

        //loading records
        ArrayList<FinanceRecord> financeRecords = new ArrayList<>();
        cursor = old.query("daily_record_table", null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            FinanceRecord newRecord = new FinanceRecord();
            Calendar cal = Calendar.getInstance();
            String date = cursor.getString(cursor.getColumnIndex("date"));
            try {
                cal.setTime(dateFormat.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            newRecord.setDate(cal);
            for (int i=0; i<categories.size(); i++) {
                if (cursor.getString(cursor.getColumnIndex("category_id")).equals(categories.get(i).getId())) {
                    newRecord.setCategory(categories.get(i));
                    if (cursor.getString(cursor.getColumnIndex("subcategory_id")).matches(context.getResources().getString(R.string.no_category))) {
                        newRecord.setSubCategory(null);
                        break;
                    }
                    for (int j=0; j<categories.get(i).getSubCategories().size(); j++) {
                        if (cursor.getString(cursor.getColumnIndex("subcategory_id")).matches(categories.get(i).getSubCategories().get(j).getId()))
                            newRecord.setSubCategory(categories.get(i).getSubCategories().get(j));
                    }
                    break;
                }
            }
            for (int i=0; i<accounts.size(); i++) {
                if (cursor.getString(cursor.getColumnIndex("account_id")).matches(accounts.get(i).getId()))
                    newRecord.setAccount(accounts.get(i));
            }
            for (int i=0; i<currencies.size(); i++) {
                if (cursor.getString(cursor.getColumnIndex("currency_id")).matches(currencies.get(i).getId()))
                    newRecord.setCurrency(currencies.get(i));
            }
            newRecord.setRecordId(cursor.getString(cursor.getColumnIndex("record_id")));
            newRecord.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));
            newRecord.setComment(cursor.getString(cursor.getColumnIndex("empty")));
            List<PhotoDetails> phDet=new ArrayList<>();
            Cursor cursorPhotoTable = old.query("record_photo_table", null, null, null, null, null, null);
            cursorPhotoTable.moveToFirst();
            while (!cursorPhotoTable.isAfterLast()) {
                if(cursorPhotoTable.getString(cursorPhotoTable.getColumnIndex("record_id")).matches(newRecord.getRecordId())){
                    PhotoDetails temp=new PhotoDetails();
                    temp.setPhotopath(cursorPhotoTable.getString(cursorPhotoTable.getColumnIndex("photopath")));
                    temp.setPhotopathCache(cursorPhotoTable.getString(cursorPhotoTable.getColumnIndex("photopathCache")));
                    temp.setRecordId(cursorPhotoTable.getString(cursorPhotoTable.getColumnIndex("record_id")));
                    phDet.add(temp);
                }
                cursorPhotoTable.moveToNext();
            }
            cursorPhotoTable.close();
            newRecord.setAllTickets(phDet);
            financeRecords.add(newRecord);
            daoSession.getFinanceRecordDao().insertOrReplace(newRecord);
            cursor.moveToNext();
        }

        //loading debt borrows
        ArrayList<DebtBorrow> debtBorrows = new ArrayList();
        Cursor dbCursor = old.query("debt_borrow_table", null, null, null, null, null, null);
        Cursor reckCursor = old.query("debtborrow_recking_table", null, null, null, null, null, null);
        dbCursor.moveToFirst();
        while (!dbCursor.isAfterLast()) {
            DebtBorrow newDebtBorrow = new DebtBorrow();
            Person newPerson = new Person();
            newPerson.setName(dbCursor.getString(dbCursor.getColumnIndex("person_name")));
            newPerson.setPhoneNumber(dbCursor.getString(dbCursor.getColumnIndex("person_number")));
            newPerson.setPhoto(dbCursor.getString(dbCursor.getColumnIndex("photo_id")));
            newDebtBorrow.setPerId(newPerson.getId());
            newDebtBorrow.setPerson(newPerson);
            daoSession.getPersonDao().insertOrReplace(newPerson);
            try {
                Calendar takenCalendar = Calendar.getInstance();
                Calendar returnCalendar = Calendar.getInstance();
                takenCalendar.setTime(dateFormat.parse(dbCursor.getString(dbCursor.getColumnIndex("taken_date"))));
                if (dbCursor.getString(dbCursor.getColumnIndex("return_date")).matches(""))
                    returnCalendar = null;
                else
                    returnCalendar.setTime(dateFormat.parse(dbCursor.getString(dbCursor.getColumnIndex("return_date"))));
                newDebtBorrow.setTakenDate(takenCalendar);
                newDebtBorrow.setReturnDate(returnCalendar);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String accountId = dbCursor.getString(dbCursor.getColumnIndex("account_id"));
            String currencyId = dbCursor.getString(dbCursor.getColumnIndex("currency_id"));
            for (int i=0; i<accounts.size(); i++) {
                if (accounts.get(i).getId().matches(accountId)) {
                    newDebtBorrow.setAccount(accounts.get(i));
                    break;
                }
            }
            newDebtBorrow.setCalculate(dbCursor.getInt(dbCursor.getColumnIndex("calculate")) == 0 ? false : true);
            for (Currency cr : currencies) {
                if (cr.getId().equals(currencyId)) {
                    newDebtBorrow.setCurrency(cr);
                    break;
                }
            }
            newDebtBorrow.setAmount(dbCursor.getDouble(dbCursor.getColumnIndex("amount")));
            newDebtBorrow.setType(dbCursor.getInt(dbCursor.getColumnIndex("type")));
            newDebtBorrow.setTo_archive(dbCursor.getInt(dbCursor.getColumnIndex("to_archive")) == 0 ? false : true);
            String id = dbCursor.getString(dbCursor.getColumnIndex("id"));
            newDebtBorrow.setId(id);
            reckCursor.moveToFirst();
            ArrayList<Recking> list = new ArrayList<Recking>();
            while (!reckCursor.isAfterLast()) {
                if (id.matches(reckCursor.getString(reckCursor.getColumnIndex("id")))) {
                    try {
                        Recking recking = new Recking();
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(dateFormat.parse(reckCursor.getString(reckCursor.getColumnIndex("pay_date"))));
                        recking.setPayDate(calendar);
                        recking.setAccountId(reckCursor.getString(reckCursor.getColumnIndex("account_id")));
                        recking.setAmount(reckCursor.getDouble(reckCursor.getColumnIndex("amount")));
                        recking.setDebtBorrowsId(id);
                        recking.setComment(reckCursor.getString(reckCursor.getColumnIndex("comment")));
                        recking.setId(UUID.randomUUID().toString());
                        daoSession.getReckingDao().insertOrReplace(recking);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                reckCursor.moveToNext();
            }
            newDebtBorrow.setInfo(dbCursor.getString(dbCursor.getColumnIndex("empty")));
            newDebtBorrow.__setDaoSession(daoSession);
            newDebtBorrow.getReckings().addAll(list);
            daoSession.getDebtBorrowDao().insertOrReplace(newDebtBorrow);
            debtBorrows.add(newDebtBorrow);
            dbCursor.moveToNext();
        }
        dbCursor.close();
        reckCursor.close();
        //loading credits
        ArrayList<CreditDetials> creditDetialses = new ArrayList<>();
        Cursor curCreditTable = old.query("credit_table", null, null, null, null, null, null);
        Cursor curCreditRecking = old.query("credit_recking_table", null, null, null, null, null, null);
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        curCreditTable.moveToFirst();
        while (!curCreditTable.isAfterLast()) {
            CreditDetials credit = new CreditDetials();
            credit.setCredit_name(curCreditTable.getString(curCreditTable.getColumnIndex("credit_name")));
            int resId = curCreditTable.getInt(curCreditTable.getColumnIndex("icon_id"));
            boolean iconFound = false;
            int pos = 0;
            String icons[] = context.getResources().getStringArray(R.array.icons);
            for (int i=0; i<icons.length; i++) {
                if (context.getResources().getIdentifier(icons[i], "drawable", context.getPackageName()) == resId) {
                    iconFound = true;
                    pos = i;
                    break;
                }
            }
            credit.setIcon_ID(iconFound ? icons[pos] : "icons_4");
            try {
                Calendar takenDate = Calendar.getInstance();
                takenDate.setTime(format.parse(curCreditTable.getString(curCreditTable.getColumnIndex("taken_date"))));
                credit.setTake_time(takenDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            credit.setProcent(curCreditTable.getDouble(curCreditTable.getColumnIndex("percent")));
            credit.setProcent_interval(Long.parseLong(curCreditTable.getString(curCreditTable.getColumnIndex("percent_interval"))));
            credit.setPeriod_time(Long.parseLong(curCreditTable.getString(curCreditTable.getColumnIndex("period_time"))));
            credit.setMyCredit_id(Long.parseLong(curCreditTable.getString(curCreditTable.getColumnIndex("credit_id"))));
            credit.setValue_of_credit(curCreditTable.getDouble(curCreditTable.getColumnIndex("credit_value")));
            credit.setValue_of_credit_with_procent(curCreditTable.getDouble(curCreditTable.getColumnIndex("credit_value_with_percent")));
            credit.setPeriod_time_tip(Long.parseLong(curCreditTable.getString(curCreditTable.getColumnIndex("period_time_tip"))));
            credit.setKey_for_include(curCreditTable.getInt(curCreditTable.getColumnIndex("key_for_include"))!=0);
            credit.setKey_for_archive(curCreditTable.getInt(curCreditTable.getColumnIndex("key_for_archive"))!=0);
            String currencyId = curCreditTable.getString(curCreditTable.getColumnIndex("currency_id"));
            Currency currency = null;
            for (int i = 0; i<currencies.size(); i++)  {
                if (currencyId.matches(currencies.get(i).getId())) {
                    currency = currencies.get(i);
                    break;
                }
            }
            credit.setValyute_currency(currency);
            List<ReckingCredit> reckings = new ArrayList<ReckingCredit>();
            curCreditRecking.moveToFirst();
            while(!curCreditRecking.isAfterLast()) {
                if (Long.parseLong(curCreditRecking.getString(curCreditRecking.getColumnIndex("credit_id"))) == Long.parseLong(curCreditTable.getString(curCreditTable.getColumnIndex("credit_id")))) {
                    double amount = curCreditRecking.getDouble(curCreditRecking.getColumnIndex("amount"));
                    long payDate = Long.parseLong(curCreditRecking.getString(curCreditRecking.getColumnIndex("pay_date")));
                    String comment = curCreditRecking.getString(curCreditRecking.getColumnIndex("comment"));
                    String accountId = curCreditRecking.getString(curCreditRecking.getColumnIndex("account_id"));
                    long creditId = Long.parseLong(curCreditRecking.getString(curCreditRecking.getColumnIndex("credit_id")));
                    Calendar calen=Calendar.getInstance();
                    calen.setTimeInMillis(payDate);
                    ReckingCredit newReckingCredit = new ReckingCredit(calen, amount, accountId, creditId, comment);
                    daoSession.getReckingCreditDao().insertOrReplace(newReckingCredit);
                    reckings.add(newReckingCredit);
                }
                curCreditRecking.moveToNext();
            }
            credit.__setDaoSession(daoSession);
            credit.getReckings().addAll(reckings);
            credit.setInfo(curCreditTable.getString(curCreditTable.getColumnIndex("empty")));
            daoSession.getCreditDetialsDao().insertOrReplace(credit);
            creditDetialses.add(credit);
            curCreditTable.moveToNext();
        }
        curCreditRecking.close();
        curCreditTable.close();
        //loading smsparseobjects
        ArrayList<SmsParseObject> smsParseObjects = new ArrayList<SmsParseObject>();
        cursor = old.query("sms_parsing_table", null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            SmsParseObject object = new SmsParseObject();
            object.setNumber(cursor.getString(cursor.getColumnIndex("number")));
            object.setIncomeWords(cursor.getString(cursor.getColumnIndex("income_words")));
            object.setExpenseWords(cursor.getString(cursor.getColumnIndex("expense_words")));
            object.setAmountWords(cursor.getString(cursor.getColumnIndex("amount_words")));
            String accountId = cursor.getString(cursor.getColumnIndex("account_id"));
            for (int i=0; i<accounts.size(); i++) {
                if (accountId.matches(accounts.get(i).getId())) {
                    object.setAccount(accounts.get(i));
                    break;
                }
            }
            String currencyId = cursor.getString(cursor.getColumnIndex("currency_id"));
            for (int i=0; i<currencies.size(); i++) {
                if (currencyId.matches(currencies.get(i).getId())) {
                    object.setCurrency(currencies.get(i));
                    break;
                }
            }
            object.setType(cursor.getInt(cursor.getColumnIndex("type")));
            daoSession.getSmsParseObjectDao().insertOrReplace(object);
            smsParseObjects.add(object);
            cursor.moveToNext();
        }
        cursor.close();
    }
    private static void upgradeFromFourToFive(Context context, SQLiteDatabase db) {
        upgradeFromThreeToFour(context, db);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        ArrayList<Account> result = new ArrayList<Account>();
        Cursor curCursor = db.query("currency_table", null, null, null, null, null, null, null);
        Cursor curCostCursor = db.query("currency_costs_table", null, null, null, null, null, null, null);
        ArrayList<Currency> currencies = new ArrayList<Currency>();
        curCursor.moveToFirst();
        while (!curCursor.isAfterLast()) {
            Currency newCurrency = new Currency(curCursor.getString(curCursor.getColumnIndex("currency_name")));
            newCurrency.setAbbr(curCursor.getString(curCursor.getColumnIndex("currency_sign")));
            String currId = curCursor.getString(curCursor.getColumnIndex("currency_id"));
            newCurrency.setId(currId);
            newCurrency.setMain(curCursor.getInt(curCursor.getColumnIndex("currency_main"))!=0);
            curCostCursor.moveToFirst();
            while(!curCostCursor.isAfterLast()) {
                if (curCostCursor.getString(curCostCursor.getColumnIndex("currency_id")).matches(currId)) {
                    CurrencyCost newCurrencyCost = new CurrencyCost();
                    try {
                        Calendar day = Calendar.getInstance();
                        day.setTime(dateFormat.parse(curCostCursor.getString(curCostCursor.getColumnIndex("date"))));
                        newCurrencyCost.setDay(day);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    newCurrencyCost.setCost(curCostCursor.getDouble(curCostCursor.getColumnIndex("cost")));
                    newCurrency.getCosts().add(newCurrencyCost);
                }
                curCostCursor.moveToNext();
            }
            currencies.add(newCurrency);
            curCursor.moveToNext();
        }
        Cursor cursor = db.query("account_table", null, null, null, null, null, null);
        cursor.moveToFirst();
        Currency mainCurrency = null;
        for (Currency currency : currencies) {
            if (currency.getMain()) {
                mainCurrency = currency;
                break;
            }
        }
        while(!cursor.isAfterLast()) {
            Account newAccount = new Account();
            newAccount.setName(cursor.getString(cursor.getColumnIndex("account_name")));
            newAccount.setId(cursor.getString(cursor.getColumnIndex("account_id")));
            newAccount.setIcon("icons_25");
            newAccount.setLimitCurId(mainCurrency.getId());
            newAccount.setStartMoneyCurrency(mainCurrency);
            newAccount.setAmount(0);
			newAccount.setIsLimited(false);
			newAccount.setLimite(0.0d);
            result.add(newAccount);
            cursor.moveToNext();
        }
        db.execSQL("DROP TABLE account_table");
        //account table
        db.execSQL("CREATE TABLE account_table ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "account_name TEXT,"
                + "account_id TEXT,"
                + "icon INTEGER,"
                + "start_amount REAL,"
                + "start_money_currency_id TEXT,"
                + "limit_currency_id TEXT,"
                + "is_limited INTEGER,"
                + "limit_amount REAL"
                + ");");

        Cursor csr = db.query("account_table", null, null, null, null, null, null);
        Log.d("sss", "account_table_is_created " + (csr == null) + " cursor " + csr.getCount() + " accounts "+result.size());

        Log.d("sss", "start_amount: "+csr.getColumnIndex("start_money")+" account_name: "+csr.getColumnIndex("start_money"));
        ContentValues values = new ContentValues();
        for (Account account : result) {
            values.put("account_name", account.getName());
            values.put("account_id", account.getId());
            values.put("icon", account.getIcon());
            values.put("start_amount", account.getAmount());
            values.put("start_money_currency_id", currencies.get(0).getId());
            values.put("limit_currency_id", currencies.get(0).getId());
            values.put("is_limited", account.getIsLimited());
			values.put("limit_amount", account.getLimite());
            db.insert("account_table", null, values);
        }
        Log.d("sss", "in_account_table_put_datas");

        db.execSQL("CREATE TABLE record_photo_table ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "photopath TEXT,"
                + "photopathCache TEXT,"
                + "record_id TEXT"
                + ");");


    }
	private static void upgradeFromThreeToFour(Context context, SQLiteDatabase db) {
		String[] resCatsId = context.getResources().getStringArray(R.array.cat_values);
		String[] resCatIcons = context.getResources().getStringArray(R.array.cat_icons);
		String[] allIcons = context.getResources().getStringArray(R.array.icons);
		int[] allIconsId = new int[allIcons.length];
		for (int i=0; i<allIcons.length; i++)
			allIconsId[i] = context.getResources().getIdentifier(allIcons[i], "drawable", context.getPackageName());
		Cursor catsCursor = db.query("category_table", null, null, null, null, null, null);
		Cursor subCatsCursor = db.query("subcategory_table", null, null, null, null, null, null);
		catsCursor.moveToFirst();
		ArrayList<RootCategory> categories = new ArrayList<>();
		while (!catsCursor.isAfterLast()) {
			RootCategory category = new RootCategory();
			category.setName(catsCursor.getString(catsCursor.getColumnIndex("category_name")));
			category.setType(catsCursor.getInt(catsCursor.getColumnIndex("category_type")));
			String id = catsCursor.getString(catsCursor.getColumnIndex("category_id"));
			boolean catIdFound = false;
			int pos = 0;
			for (int i=0; i<resCatsId.length; i++) {
				if (resCatsId[i].matches(id)) {
					catIdFound = true;
					pos = i;
					break;
				}
			}
			ArrayList<SubCategory> subCategories = new ArrayList<>();
			if (catIdFound) {
				category.setIcon(resCatIcons[pos]);
				subCatsCursor.moveToFirst();
				while(!subCatsCursor.isAfterLast()) {
					if (id.matches(subCatsCursor.getString(subCatsCursor.getColumnIndex("category_id")))) {
						SubCategory subCategory = new SubCategory();
						subCategory.setName(subCatsCursor.getString(subCatsCursor.getColumnIndex("subcategory_name")));
						String subCatId = subCatsCursor.getString(subCatsCursor.getColumnIndex("subcategory_id"));
						int subcatIconArrayId = context.getResources().getIdentifier(id, "array", context.getPackageName());
						if (subcatIconArrayId != 0) {
							boolean q = false;
							int s = 0;
							String[] scn = context.getResources().getStringArray(subcatIconArrayId);
							for (int i=0; i<scn.length; i++) {
								if (scn[i].matches(subCatId)) {
									q = true;
									s = i;
									break;
								}
							}
							if(q){
								int h = context.getResources().getIdentifier(id+"_icons", "array", context.getPackageName());
								String[] subCatsId = context.getResources().getStringArray(h);
								subCategory.setIcon(subCatsId[s]);
							}
							else {
								int subCatIconId = subCatsCursor.getInt(subCatsCursor.getColumnIndex("icon"));
								boolean f = false;
								int p = 0;
								for (int i=0; i<allIconsId.length; i++) {
									if (subCatIconId == allIconsId[i]) {
										f = true;
										p = i;
										break;
									}
								}
								if (f)
									subCategory.setIcon(allIcons[p]);
								else
									subCategory.setIcon("category_not_selected");
							}
						} else {
							boolean s = false;
							int a = 0;
							for (int i=0; i<allIconsId.length; i++) {
								if (allIconsId[i] == subCatsCursor.getInt(subCatsCursor.getColumnIndex("icon"))) {
									s = true;
									a = i;
									break;
								}
							}
							if (s)
								subCategory.setIcon(allIcons[a]);
							else
								subCategory.setIcon("category_not_selected");
						}
						subCategory.setId(subCatId);
						subCategories.add(subCategory);
					}
					subCatsCursor.moveToNext();
				}
			}
			else {
				int iconId = catsCursor.getInt(catsCursor.getColumnIndex("icon"));
				boolean found = false;
				pos = 0;
				for (int i=0; i<allIconsId.length; i++) {
					if (allIconsId[i] == iconId) {
						found = true;
						pos = i;
						break;
					}
				}
				if (found)
					category.setIcon(allIcons[pos]);
				else
					category.setIcon("category_not_selected");
				subCatsCursor.moveToFirst();
				while (!subCatsCursor.isAfterLast()) {
					if (id.matches(subCatsCursor.getString(subCatsCursor.getColumnIndex("category_id")))) {
						SubCategory subCategory = new SubCategory();
						subCategory.setName(subCatsCursor.getString(subCatsCursor.getColumnIndex("subcategory_name")));
						String subCatId = subCatsCursor.getString(subCatsCursor.getColumnIndex("subcategory_id"));
						subCategory.setId(subCatId);
						iconId = subCatsCursor.getInt(subCatsCursor.getColumnIndex("icon"));
						found = false;
						pos = 0;
						for (int i=0; i<allIconsId.length; i++) {
							if (allIconsId[i] == iconId) {
								found = true;
								pos = i;
								break;
							}
						}
						if (found)
							subCategory.setIcon(allIcons[pos]);
						else
							subCategory.setIcon("category_not_selected");
						subCategories.add(subCategory);
					}
					subCatsCursor.moveToNext();
				}
			}
			category.setId(id);
//			category.setSubCategories(subCategories);
			categories.add(category);
			catsCursor.moveToNext();
		}
		db.execSQL("DROP TABLE category_table");
		db.execSQL("DROP TABLE subcategory_table");
		db.execSQL("create table category_table ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "category_name TEXT,"
				+ "category_id TEXT,"
				+ "category_type INTEGER,"
				+ "icon TEXT,"
				+ "empty TEXT"
				+ ");");
		//subcategries table
		db.execSQL("create table subcategory_table ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "subcategory_name TEXT,"
				+ "subcategory_id TEXT,"
				+ "category_id TEXT,"
				+ "icon TEXT,"
				+ "empty TEXT"
				+ ");");
		//saving categories begin
		for (int i=0; i<categories.size(); i++) {
			ContentValues values = new ContentValues();
			values.put("category_name", categories.get(i).getName());
			values.put("category_id", categories.get(i).getId());
			values.put("category_type", categories.get(i).getType());
			values.put("icon", categories.get(i).getIcon());
			db.insert("category_table", null, values);
			for (int j=0; j<categories.get(i).getSubCategories().size(); j++) {
				values.clear();
				values.put("subcategory_name", categories.get(i).getSubCategories().get(j).getName());
				values.put("subcategory_id", categories.get(i).getSubCategories().get(j).getId());
				values.put("category_id", categories.get(i).getId());
				values.put("icon", categories.get(i).getSubCategories().get(j).getIcon());
				db.insert("subcategory_table", null, values);
			}
		}
		//saving categories end
		Cursor incomesCursor = db.query("incomes_table", null, null, null, null, null, null);
		Log.d("sss", incomesCursor.getCount()+"");
		ArrayList<String> incomesId = new ArrayList<>();
		incomesCursor.moveToFirst();
		while (!incomesCursor.isAfterLast()) {
			incomesId.add(incomesCursor.getString(incomesCursor.getColumnIndex("category_id")));
			incomesCursor.moveToNext();
		}
		db.execSQL("DROP TABLE incomes_table");
		db.execSQL("create table incomes_table ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "category_name TEXT,"
				+ "category_id TEXT,"
				+ "category_type INTEGER,"
				+ "icon TEXT,"
				+ "empty TEXT"
				+ ");");
		for (int i=0; i<incomesId.size(); i++) {
			ContentValues values = new ContentValues();
			if (incomesId.get(i) == null) {
				values.put("category_name", context.getResources().getString(R.string.no_category));
				db.insert("incomes_table", null, values);
				continue;
			}
			for (int j=0; j<categories.size(); j++) {
				if (incomesId.get(i).matches(categories.get(j).getId())) {
					values.put("category_name", categories.get(j).getName());
					values.put("category_id", categories.get(j).getId());
					values.put("category_type", categories.get(j).getType());
					values.put("icon", categories.get(j).getIcon());
					db.insert("incomes_table", null, values);
					break;
				}
			}
		}
		Cursor expensesCursor = db.query("expanses_table", null, null, null, null, null, null);
		ArrayList<String> expensesId = new ArrayList<>();
		expensesCursor.moveToFirst();
		while (!expensesCursor.isAfterLast()) {
			expensesId.add(expensesCursor.getString(incomesCursor.getColumnIndex("category_id")));
			expensesCursor.moveToNext();
		}

		db.execSQL("DROP TABLE expanses_table");
		db.execSQL("create table expanses_table ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "category_name TEXT,"
				+ "category_id TEXT,"
				+ "category_type INTEGER,"
				+ "icon TEXT,"
				+ "empty TEXT"
				+ ");");
		for (int i=0; i<expensesId.size(); i++) {
			ContentValues values = new ContentValues();
			if (expensesId.get(i) == null) {
				values.put("category_name", context.getResources().getString(R.string.no_category));
				db.insert("expanses_table", null, values);
				continue;
			}
			for (int j=0; j<categories.size(); j++) {
				if (expensesId.get(i).matches(categories.get(j).getId())) {
					values.put("category_name", categories.get(j).getName());
					values.put("category_id", categories.get(j).getId());
					values.put("category_type", categories.get(j).getType());
					values.put("icon", categories.get(j).getIcon());
					db.insert("expanses_table", null, values);
					break;
				}
			}
		}
	}
}
