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
import org.joda.time.LocalDate;
import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountEntryPage;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountSearchParameters;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountSubmitParameters;
import org.mifos.test.acceptance.framework.loan.DisburseLoanParameters;
import org.mifos.test.acceptance.framework.loan.EditLoanAccountInformationParameters;
import org.mifos.test.acceptance.framework.loan.EditLoanAccountStatusParameters;
import org.mifos.test.acceptance.framework.loan.LoanAccountPage;
import org.mifos.test.acceptance.framework.testhelpers.LoanTestHelper;
import org.mifos.test.acceptance.remote.DateTimeUpdaterRemoteTestingService;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
@ContextConfiguration(locations = { "classpath:ui-test-context.xml" })
@Test(sequential = true, groups = {"loan","acceptance","ui"})
public class CreateGLIMLoanAccountTest extends UiTestCaseBase {

    private LoanTestHelper loanTestHelper;

    @Autowired
    private DriverManagerDataSource dataSource;

    @Autowired
    private DbUnitUtilities dbUnitUtilities;

    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        super.setUp();
        loanTestHelper = new LoanTestHelper(selenium);
    }

    @AfterMethod
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }

    /*
     * This test is to verify that you can edit a GLIM loan account after it has been
     * dibursed without getting an invalid disbursal date error. See MIFOS-2597.
     */
    @Test(enabled=true)
    // http://mifosforge.jira.com/browse/MIFOSTEST-132
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void checkGLIMLoanCreatedBySubmitForApproval() throws Exception {
        //Given
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2009,7,11,13,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);

        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);
        //When
        LoanAccountPage loanAccountPage = loanTestHelper.createLoanAccountForMultipleClientsInGroup(true);

        String accountId = loanAccountPage.getAccountId();

        EditLoanAccountStatusParameters statusParameters = new EditLoanAccountStatusParameters();
        statusParameters.setStatus(EditLoanAccountStatusParameters.APPROVED);
        statusParameters.setNote("Test");

        loanTestHelper.changeLoanAccountStatus(accountId, statusParameters);
        DisburseLoanParameters params = new DisburseLoanParameters();

        params.setDisbursalDateDD("11");
        params.setDisbursalDateMM("07");
        params.setDisbursalDateYYYY("2009");
        params.setPaymentType(DisburseLoanParameters.CASH);

        loanTestHelper.disburseLoan(accountId, params);
        dateTimeUpdaterRemoteTestingService.setDateTime(new LocalDate(2009,7,20).toDateTimeAtStartOfDay());

        EditLoanAccountInformationParameters editLoanAccountInformationParameters = new EditLoanAccountInformationParameters();
        editLoanAccountInformationParameters.setExternalID("ID2323ID");

        loanTestHelper.changeLoanAccountInformation(accountId, new CreateLoanAccountSubmitParameters(), editLoanAccountInformationParameters);
   }

    @Test(enabled=true)
    // http://mifosforge.jira.com/browse/MIFOSTEST-133
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void checkGLIMLoanCreatedBySaveForLater() throws Exception {
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2009,7,11,13,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);

        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);

        loanTestHelper.createLoanAccountForMultipleClientsInGroup(false);
    }

    // http://mifosforge.jira.com/browse/MIFOSTEST-134
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void verifyLoanAccountCreationPipelineWhenGlimIsEnabled() throws Exception {
        //Given
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2009,7,11,13,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_003_dbunit.xml", dataSource, selenium);

        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Stu1232993852651 Client1232993852651");
        searchParameters.setLoanProduct("WeeklyClientDeclinetLoanWithPeriodicFee");
        String[] locators = {   "name=prdOfferingId",
                                "loancreationdetails.input.sumLoanAmount",
                                "loancreationdetails.input.interestRate",
                                "loancreationdetails.input.numberOfInstallments",
                                "disbursementDateDD",
                                "disbursementDateMM",
                                "disbursementDateYY",
                                "name=loanOfferingFund",
                                "name=businessActivityId",
                                "name=collateralTypeId",
                                "name=collateralNote",
                                "name=externalId",
                                "name=selectedFee[0].feeId",
                                "name=selectedFee[0].amount",
                                "name=selectedFee[1].feeId",
                                "name=selectedFee[1].amount",
                                "name=selectedFee[2].feeId",
                                "name=selectedFee[2].amount",
                                "loancreationdetails.button.continue",
                                "loancreationdetails.button.cancel"
                            };
        //When / Then
        loanTestHelper.navigateToCreateLoanAccountEntryPage(searchParameters).verifyAllElementsArePresent(locators);
   }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void newWeeklyGLIMAccount() throws Exception {
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);

        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("MyGroup1233266297718");
        searchParameters.setLoanProduct("WeeklyGroupFlatLoanWithOnetimeFee");

        CreateLoanAccountEntryPage loanAccountEntryPage = loanTestHelper.navigateToCreateLoanAccountEntryPage(searchParameters);

        loanAccountEntryPage.selectGLIMClients(0, "Stu1233266299995 Client1233266299995 \n Client Id: 0006-000000051", "301");
        loanAccountEntryPage.selectGLIMClients(2, "Stu1233266319760 Client1233266319760 \n Client Id: 0006-000000053", "401");

        loanAccountEntryPage.submitAndNavigateToGLIMLoanAccountConfirmationPage();
   }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void checkGLIMAccountTotalCalculationWithDecimal() throws Exception {
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);

        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("MyGroup1233266297718");
        searchParameters.setLoanProduct("WeeklyGroupFlatLoanWithOnetimeFee");

        CreateLoanAccountEntryPage loanAccountEntryPage = loanTestHelper.navigateToCreateLoanAccountEntryPage(searchParameters);

        loanAccountEntryPage.selectGLIMClients(0, "Stu1233266299995 Client1233266299995 \n Client Id: 0006-000000051", "9999.9");
        loanAccountEntryPage.selectGLIMClients(1, "Stu1233266309851 Client1233266309851 \n Client Id: 0006-000000052", "99999.9");
        loanAccountEntryPage.selectGLIMClients(2, "Stu1233266319760 Client1233266319760 \n Client Id: 0006-000000053", "99999.9");

        loanAccountEntryPage.checkTotalAmount("209999.7");

   }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void checkGLIMAccountTotalCalculationWholeNumber() throws Exception {
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);

        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("MyGroup1233266297718");
        searchParameters.setLoanProduct("WeeklyGroupFlatLoanWithOnetimeFee");

        CreateLoanAccountEntryPage loanAccountEntryPage = loanTestHelper.navigateToCreateLoanAccountEntryPage(searchParameters);

        loanAccountEntryPage.selectGLIMClients(0, "Stu1233266299995 Client1233266299995 \n Client Id: 0006-000000051", "9999");
        loanAccountEntryPage.selectGLIMClients(1, "Stu1233266309851 Client1233266309851 \n Client Id: 0006-000000052", "99999");
        loanAccountEntryPage.selectGLIMClients(2, "Stu1233266319760 Client1233266319760 \n Client Id: 0006-000000053", "99999");

        loanAccountEntryPage.checkTotalAmount("209997");

   }
}
