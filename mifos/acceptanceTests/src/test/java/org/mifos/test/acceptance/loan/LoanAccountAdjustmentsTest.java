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

package org.mifos.test.acceptance.loan;

import org.joda.time.DateTime;
import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.loan.LoanAccountPage;
import org.mifos.test.acceptance.framework.loan.RedoLoanDisbursalParameters;
import org.mifos.test.acceptance.framework.loan.TransactionHistoryPage;
import org.mifos.test.acceptance.framework.testhelpers.LoanTestHelper;
import org.mifos.test.acceptance.remote.DateTimeUpdaterRemoteTestingService;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(locations={"classpath:ui-test-context.xml"})
@Test(sequential=true, groups={"acceptance", "ui", "loan"})
public class LoanAccountAdjustmentsTest extends UiTestCaseBase {
    private LoanTestHelper loanTestHelper;

    @Autowired
    private DriverManagerDataSource dataSource;
    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @BeforeClass
    public void setUpClass() throws Exception {
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_003_dbunit.xml", dataSource, selenium);
    }

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();

        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2011,2,1,15,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);

        loanTestHelper = new LoanTestHelper(selenium);
    }

    @AfterMethod
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }

    /**
     * Verify loan account status changes when number of days in arrears becomes greater than
     * the lateness definition by performing multiple adjustments when Account status is "Active in Good Standing"
     * http://mifosforge.jira.com/browse/MIFOSTEST-30
     * @throws Exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void verifyAccountStatusAfterMultipleAdjustments() throws Exception {
        String client = "Stu1233266134755 Client1233266134755";
        RedoLoanDisbursalParameters redoParams = new RedoLoanDisbursalParameters();
        redoParams.setDisbursalDateDD("06");
        redoParams.setDisbursalDateMM("01");
        redoParams.setDisbursalDateYYYY("2011");

        LoanAccountPage loanAccountPage = loanTestHelper.redoLoanDisbursal(client, "WeeklyFlatLoanWithOneTimeFees", redoParams, null, 0, false);
        String loanID = loanAccountPage.getAccountId();
        loanAccountPage = loanTestHelper.applyMultipleAdjustments(loanID, 2);

        loanAccountPage.verifyPerformanceHistory("1", "2");
        loanAccountPage.verifyStatus(LoanAccountPage.ACTIVE_BAD);
    }

    /**
     * Verify multiple adjustment changes are logged in transaction history with proper GL codes,
     * account summary changes when the account status is "Active in Bad Standing".
     * http://mifosforge.jira.com/browse/MIFOSTEST-27
     * @throws Exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void verifyChangesInTransactionHistory() throws Exception {
        RedoLoanDisbursalParameters redoParams = new RedoLoanDisbursalParameters();
        redoParams.setDisbursalDateDD("07");
        redoParams.setDisbursalDateMM("01");
        redoParams.setDisbursalDateYYYY("2010");

        LoanAccountPage loanAccountPage = loanTestHelper.redoLoanDisbursal("MyGroup1233266255641", "WeeklyGroupFlatLoanWithOnetimeFee", redoParams, null, 0, false);

        String loanID = loanAccountPage.getAccountId();
        loanAccountPage = loanTestHelper.applyMultipleAdjustments(loanID, 5);

        loanAccountPage.verifyStatus(LoanAccountPage.ACTIVE_BAD);
        loanAccountPage.verifyTotalOriginalLoan("4290.0");
        loanAccountPage.verifyTotalAmountPaid("3951.0");
        loanAccountPage.verifyAccountSummary("339.0", "03/02/2011", "339.0");
        TransactionHistoryPage transactionHistoryPage = loanAccountPage.navigateToTransactionHistory();
        transactionHistoryPage.verifyTransactionHistory(3951.0, 52, 238);
    }
}
