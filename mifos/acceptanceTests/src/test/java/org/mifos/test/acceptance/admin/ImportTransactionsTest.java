/*
 * Copyright (c) 2005-2011 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.test.acceptance.admin;

import org.joda.time.DateTime;
import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.admin.AdminPage;
import org.mifos.test.acceptance.framework.admin.ImportTransactionsConfirmationPage;
import org.mifos.test.acceptance.framework.admin.ImportTransactionsPage;
import org.mifos.test.acceptance.framework.testhelpers.NavigationHelper;
import org.mifos.test.acceptance.remote.DateTimeUpdaterRemoteTestingService;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "classpath:ui-test-context.xml" })
@Test(sequential = true, groups = {"admin", "acceptance","ui"})
public class ImportTransactionsTest extends UiTestCaseBase {

    private NavigationHelper navigationHelper;
    private static final String EXCEL_IMPORT_TYPE = "Audi Bank (Excel 2007)";

    @Autowired
    private DriverManagerDataSource dataSource;
    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    public static final String ACCOUNT_PAYMENT = "ACCOUNT_PAYMENT";
    public static final String ACCOUNT_TRXN = "ACCOUNT_TRXN";
    public static final String FINANCIAL_TRXN = "FINANCIAL_TRXN";
    public static final String LOAN_ACTIVITY_DETAILS = "LOAN_ACTIVITY_DETAILS";
    public static final String LOAN_SCHEDULE = "LOAN_SCHEDULE";
    public static final String LOAN_SUMMARY = "LOAN_SUMMARY";
    public static final String LOAN_TRXN_DETAIL = "LOAN_TRXN_DETAIL";

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        navigationHelper = new NavigationHelper(selenium);
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2009,11,7,10,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);

    }

    @AfterMethod
    public void tearDown() {
        (new MifosPage(selenium)).logout();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled=false)
    public void importExcelFormatAudiBankTransactions() throws Exception {
        String importFile = this.getClass().getResource("/AudiUSD-SevenTransactions.xls").toString();
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_009_dbunit.xml", dataSource, selenium);

        importTransaction(importFile, EXCEL_IMPORT_TYPE);

        // TODO - add proper UI verifications and enable this test after MIFOS-4651 is fixed
     }


    private void importTransaction(String importFile, String importType) {
        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ImportTransactionsPage importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        importTransactionsPage.verifyPage();
        ImportTransactionsConfirmationPage importTransactionsConfirmationPage = importTransactionsPage.importAudiTransactions(importFile, importType);
        importTransactionsConfirmationPage.verifyPage();
    }

    //  Test the import transaction page loads with no plugins available  - regression test for MIFOS-2683
    @Test(enabled=true)
    public void importTransactionPageLoad() {
        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ImportTransactionsPage importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        importTransactionsPage.verifyPage();
    }
}
