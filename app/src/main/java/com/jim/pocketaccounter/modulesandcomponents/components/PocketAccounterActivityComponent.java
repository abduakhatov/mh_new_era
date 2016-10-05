package com.jim.pocketaccounter.modulesandcomponents.components;

import com.jim.pocketaccounter.PocketAccounter;
import com.jim.pocketaccounter.credit.AdapterCridet;
import com.jim.pocketaccounter.credit.AdapterCridetArchive;
import com.jim.pocketaccounter.credit.notificat.AutoMarketService;
import com.jim.pocketaccounter.credit.notificat.NotificationManagerCredit;
import com.jim.pocketaccounter.debt.AddBorrowFragment;
import com.jim.pocketaccounter.debt.BorrowFragment;
import com.jim.pocketaccounter.debt.DebtBorrowFragment;
import com.jim.pocketaccounter.debt.InfoDebtBorrowFragment;
import com.jim.pocketaccounter.finance.CurrencyAdapter;
import com.jim.pocketaccounter.finance.CurrencyExchangeAdapter;
import com.jim.pocketaccounter.finance.TransferAccountAdapter;
import com.jim.pocketaccounter.fragments.AccountEditFragment;
import com.jim.pocketaccounter.fragments.AccountFragment;
import com.jim.pocketaccounter.fragments.AccountInfoFragment;
import com.jim.pocketaccounter.fragments.AddAutoMarketFragment;
import com.jim.pocketaccounter.fragments.AddCreditFragment;
import com.jim.pocketaccounter.fragments.AddSmsParseFragment;
import com.jim.pocketaccounter.fragments.AutoMarketFragment;
import com.jim.pocketaccounter.fragments.CategoryFragment;
import com.jim.pocketaccounter.fragments.CategoryInfoFragment;
import com.jim.pocketaccounter.fragments.CreditFragment;
import com.jim.pocketaccounter.fragments.CreditTabLay;
import com.jim.pocketaccounter.fragments.CurrencyChooseFragment;
import com.jim.pocketaccounter.fragments.CurrencyEditFragment;
import com.jim.pocketaccounter.fragments.CurrencyFragment;
import com.jim.pocketaccounter.fragments.InfoCreditFragment;
import com.jim.pocketaccounter.fragments.InfoCreditFragmentForArchive;
import com.jim.pocketaccounter.fragments.MainPageFragment;
import com.jim.pocketaccounter.fragments.PurposeEditFragment;
import com.jim.pocketaccounter.fragments.PurposeFragment;
import com.jim.pocketaccounter.fragments.PurposeInfoFragment;
import com.jim.pocketaccounter.fragments.RecordDetailFragment;
import com.jim.pocketaccounter.fragments.RecordEditFragment;
import com.jim.pocketaccounter.fragments.ReportByAccountFragment;
import com.jim.pocketaccounter.fragments.ReportByCategory;
import com.jim.pocketaccounter.fragments.ReportByCategoryExpansesFragment;
import com.jim.pocketaccounter.fragments.ReportByCategoryIncomesFragment;
import com.jim.pocketaccounter.fragments.RootCategoryEditFragment;
import com.jim.pocketaccounter.fragments.SMSParseEditFragment;
import com.jim.pocketaccounter.fragments.SMSParseFragment;
import com.jim.pocketaccounter.fragments.SearchFragment;
import com.jim.pocketaccounter.fragments.SmsParseMainFragment;
import com.jim.pocketaccounter.managers.LogicManager;
import com.jim.pocketaccounter.managers.PAFragmentManager;
import com.jim.pocketaccounter.managers.SettingsManager;
import com.jim.pocketaccounter.managers.ToolbarManager;
import com.jim.pocketaccounter.modulesandcomponents.modules.PocketAccounterActivityModule;
import com.jim.pocketaccounter.report.CategoryReportView;
import com.jim.pocketaccounter.report.ReportByCategoryDialogAdapter;
import com.jim.pocketaccounter.syncbase.SyncBase;
import com.jim.pocketaccounter.utils.SubCatAddEditDialog;
import com.jim.pocketaccounter.utils.TransferDialog;
import com.jim.pocketaccounter.utils.record.RecordExpanseView;
import com.jim.pocketaccounter.utils.record.RecordIncomesView;

import dagger.Component;

/**
 * Created by DEV on 27.08.2016.
 */
@Component(
        modules = {PocketAccounterActivityModule.class},
        dependencies = {PocketAccounterApplicationComponent.class}
)
public interface PocketAccounterActivityComponent {
    void inject(PocketAccounter pocketAccounter);
    void inject(SettingsManager settingsManager);
    void inject(CurrencyFragment currencyFragment);
    void inject(CurrencyAdapter currencyAdapter);
    void inject(CurrencyChooseFragment currencyChooseFragment);
    void inject(CurrencyEditFragment currencyEditFragment);
    void inject(CurrencyExchangeAdapter currencyExchangeAdapter);
    void inject(AccountFragment accountFragment);
    void inject(AccountEditFragment accountEditFragment);
    void inject(CategoryFragment categoryFragment);
    void inject(AccountInfoFragment accountInfoFragment);
    void inject(SubCatAddEditDialog subCatAddEditDialog);
    void inject(RootCategoryEditFragment rootCategoryEditFragment);
    void inject(CategoryInfoFragment categoryInfoFragment);
    void inject(PurposeFragment purposeFragment);
    void inject(TransferDialog transferDialog);
    void inject(PurposeEditFragment purposeEditFragment);
    void inject(PurposeInfoFragment purposeInfoFragment);
    void inject(DebtBorrowFragment debtBorrowFragment);
    void inject(InfoDebtBorrowFragment infoDebtBorrowFragment);
    void inject(BorrowFragment borrowFragment);
    void inject(AddBorrowFragment addBorrowFragment);
    void inject(AdapterCridetArchive adapterCridetArchive);
    void inject(AdapterCridet adapterCridet);
    void inject(CreditTabLay creditTabLay);
    void inject(CreditFragment creditFragment);
    void inject(InfoCreditFragment infoCreditFragment);
    void inject(InfoCreditFragmentForArchive infoCreditFragmentForArchive);
    void inject(AddCreditFragment addCreditFragment);
    void inject(NotificationManagerCredit notificationManagerCredit);
    void inject(ToolbarManager toolbarManager);
    void inject(RecordEditFragment recordEditFragment);
    void inject(RecordDetailFragment recordDetailFragment);
    void inject(RecordExpanseView recordExpanseView);
    void inject(RecordIncomesView recordIncomesView);
    void inject(MainPageFragment mainPageFragment);
    void inject(AutoMarketFragment autoMarketFragment);
    void inject(AddAutoMarketFragment addAutoMarketFragment);
    void inject(SearchFragment searchFragment);
    void inject(AutoMarketService autoMarketService);
    void inject(ReportByAccountFragment reportByAccountFragment);
    void inject(ReportByCategory reportByCategory);
    void inject(ReportByCategoryExpansesFragment reportByCategoryExpansesFragment);
    void inject(ReportByCategoryIncomesFragment reportByCategoryIncomesFragment);
    void inject(CategoryReportView categoryReportView);
    void inject(SMSParseFragment smsParseFragment);
    void inject(SMSParseEditFragment smsParseEditFragment);
    void inject(ReportByCategoryDialogAdapter reportByCategoryDialogAdapter);
    void inject(SmsParseMainFragment smsParseMainFragment);
    void inject(AddSmsParseFragment addSmsParseFragment);
    //    void inject(LogicManager logicManager);
}
