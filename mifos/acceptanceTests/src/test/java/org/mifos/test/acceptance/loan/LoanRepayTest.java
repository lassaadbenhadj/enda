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

import org.apache.commons.lang.StringUtils;
import org.dbunit.DatabaseUnitException;
import org.joda.time.DateTime;
import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.admin.AdminPage;
import org.mifos.test.acceptance.framework.admin.DefineAcceptedPaymentTypesPage;
import org.mifos.test.acceptance.framework.loan.AccountActivityPage;
import org.mifos.test.acceptance.framework.loan.ChargeParameters;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountSearchParameters;
import org.mifos.test.acceptance.framework.loan.CreateLoanAccountSubmitParameters;
import org.mifos.test.acceptance.framework.loan.DisburseLoanParameters;
import org.mifos.test.acceptance.framework.loan.EditLoanAccountStatusParameters;
import org.mifos.test.acceptance.framework.loan.LoanAccountPage;
import org.mifos.test.acceptance.framework.loan.RepayLoanConfirmationPage;
import org.mifos.test.acceptance.framework.loan.RepayLoanPage;
import org.mifos.test.acceptance.framework.loan.RepayLoanParameters;
import org.mifos.test.acceptance.framework.testhelpers.LoanTestHelper;
import org.mifos.test.acceptance.framework.testhelpers.NavigationHelper;
import org.mifos.test.acceptance.remote.DateTimeUpdaterRemoteTestingService;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

@SuppressWarnings("PMD")
@ContextConfiguration(locations = {"classpath:ui-test-context.xml"})
@Test(sequential = true, groups = {"acceptance", "ui", "loan"})
public class LoanRepayTest extends UiTestCaseBase {
    @Autowired
    private DriverManagerDataSource dataSource;
    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired

    private InitializeApplicationRemoteTestingService initRemote;
    private LoanTestHelper loanTestHelper;
    private String loanId = "000100000000005";
    private NavigationHelper navigationHelper;


    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.setUp();
        loanTestHelper = new LoanTestHelper(selenium);
        setupTimeAndDB();
        navigationHelper = new NavigationHelper(selenium);
    }

    @AfterMethod(alwaysRun = true)
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }

    @Test(enabled=false)
    public void repay() {
        chargeFeeAndPenalty();
        loanTestHelper.disburseLoan(loanId, getDisburseLoanParameters());
        testRepayLoanPageWhenProductHasNoInterestWaiver();
        loanTestHelper.editLoanProduct("Cattle Loan", true);
        testRepayLoanPageWhenProductHasInterestWaiver();
        repayLoanWithInterestWaived();
        verifyLoanStateAndAccountSummary();
        verifyRepaymentSchedule();
        verifyAccountActivity();
    }

    //http://mifosforge.jira.com/browse/MIFOSTEST-251
    @Test(enabled=true)
    public void paymentTypeForLoanRepayments()  throws Exception{
        //Given
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2011,2,1,13,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_008_dbunit.xml", dataSource, selenium);

        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        DefineAcceptedPaymentTypesPage defineAcceptedPaymentTypesPage = adminPage.navigateToDefineAcceptedPaymentType();
        defineAcceptedPaymentTypesPage.addLoanRepaymentsPaymentType(defineAcceptedPaymentTypesPage.CHEQUE);

        adminPage = navigationHelper.navigateToAdminPage();
        defineAcceptedPaymentTypesPage = adminPage.navigateToDefineAcceptedPaymentType();
        defineAcceptedPaymentTypesPage.addLoanRepaymentsPaymentType(defineAcceptedPaymentTypesPage.VOUCHER);

        //When
        CreateLoanAccountSearchParameters searchParameters = new CreateLoanAccountSearchParameters();
        searchParameters.setSearchString("Client - Mary Monthly");
        searchParameters.setLoanProduct("MonthlyClientFlatLoan1stOfMonth");

        CreateLoanAccountSubmitParameters submitAccountParameters = new CreateLoanAccountSubmitParameters();
        submitAccountParameters.setAmount("1423.0");
        submitAccountParameters.setGracePeriodTypeNone(true);

        LoanAccountPage loanAccountPage;
        loanAccountPage=loanTestHelper.createLoanAccount(searchParameters, submitAccountParameters);
        String lid=loanAccountPage.getAccountId();
        EditLoanAccountStatusParameters params = new EditLoanAccountStatusParameters();
        params.setStatus(EditLoanAccountStatusParameters.APPROVED);
        params.setNote("Test");

        loanTestHelper.changeLoanAccountStatus(lid, params);
        loanTestHelper.disburseLoan(lid, getDisburseLoanParameters());

        loanAccountPage=navigationHelper.navigateToLoanAccountPage(lid);
        loanAccountPage.navigateToRepayLoan();
        String[] modesOfPayment=selenium.getSelectOptions("RepayLoan.input.modeOfRepayment");

        //Then
        Assert.assertEquals(RepayLoanParameters.CASH,modesOfPayment[1]);
        Assert.assertEquals(RepayLoanParameters.CHEQUE,modesOfPayment[2]);
        Assert.assertEquals(RepayLoanParameters.VOUCHER,modesOfPayment[3]);

        //When
        lid="000100000000004";
        loanTestHelper.disburseLoan(lid, getDisburseLoanParameters());

        loanAccountPage=navigationHelper.navigateToLoanAccountPage(lid);
        loanAccountPage.navigateToRepayLoan();
        String[] modesOfPayment2=selenium.getSelectOptions("RepayLoan.input.modeOfRepayment");

        //Then
        Assert.assertEquals(RepayLoanParameters.CASH,modesOfPayment2[1]);
        Assert.assertEquals(RepayLoanParameters.CHEQUE,modesOfPayment2[2]);
        Assert.assertEquals(RepayLoanParameters.VOUCHER,modesOfPayment2[3]);

    }

    private void verifyAccountActivity() {
        LoanAccountPage accountPage = navigationHelper.navigateToLoanAccountPage(loanId);
        AccountActivityPage accountActivityPage = accountPage.navigateToAccountActivityPage();
        Assert.assertEquals(accountActivityPage.getLastPrinciplePaid(),"1000.0");
        Assert.assertEquals(accountActivityPage.getLastInterestPaid(),"9.5");
        Assert.assertEquals(accountActivityPage.getLastFeePaid().trim(),"10.0");
        Assert.assertEquals(accountActivityPage.getLastPenaltyPaid(),"5.0");
        Assert.assertEquals(accountActivityPage.getLastTotalPaid(),"1024.5");
        Assert.assertEquals(accountActivityPage.getRunningPrinciple(),"0.0");
        Assert.assertEquals(accountActivityPage.getRunningInterest(),"0.0");
        Assert.assertEquals(accountActivityPage.getRunningFees(),"0.0");
        Assert.assertEquals(accountActivityPage.getRunningTotal(),"0.0");
    }

    private void verifyRepaymentSchedule() {
        LoanAccountPage accountPage = navigationHelper.navigateToLoanAccountPage(loanId);
        accountPage.navigateToRepaymentSchedulePage();
        Assert.assertEquals(selenium.getTable("repaymentScheduleTable.7.4").trim(),"1.9");
    }

    private void verifyLoanStateAndAccountSummary() {
        LoanAccountPage accountPage = navigationHelper.navigateToLoanAccountPage(loanId);
        Assert.assertTrue(selenium.isTextPresent("Closed- Obligation met"));
        Assert.assertEquals(accountPage.getOriginalLoanAmount(), "1000.0");
        Assert.assertEquals(accountPage.getPrinciplePaid(), "1000.0");
        Assert.assertEquals(accountPage.getPrincipleBalance(), "0.0");
        Assert.assertEquals(accountPage.getOriginalInterestAmount(), "9.5");
        Assert.assertEquals(accountPage.getInterestPaid(), "9.5");
        Assert.assertEquals(accountPage.getInterestBalance(), "0.0");
        Assert.assertEquals(accountPage.getOriginalFeesAmount(), "10.0");
        Assert.assertEquals(accountPage.getFeesPaid(), "10.0");
        Assert.assertEquals(accountPage.getFeesBalance(), "0.0");
        Assert.assertEquals(accountPage.getOriginalPenaltyAmount(), "5.0");
        Assert.assertEquals(accountPage.getPenaltyPaid(), "5.0");
        Assert.assertEquals(accountPage.getPenaltyBalance(), "0.0");
        Assert.assertEquals(accountPage.getOriginalTotalAmount(), "1024.5");
        Assert.assertEquals(accountPage.getTotalPaid(), "1024.5");
        Assert.assertEquals(accountPage.getTotalBalance(), "0.0");
    }

    private void testRepayLoanPageWhenProductHasInterestWaiver() {
        RepayLoanPage repayLoanPage = navigateToRepayLoanPage();
        verifySelectionOfWaiveInterest(repayLoanPage);
        verifySelectionInConfirmationPage(repayLoanPage, "Yes");
        repayLoanPage.interestWaiver(false);
        verifySelectionOfDoNotWaiveInterest(repayLoanPage, true);
        verifySelectionInConfirmationPage(repayLoanPage, "No");
    }

    private void repayLoanWithInterestWaived() {
        RepayLoanPage repayLoanPage = navigateToRepayLoanPage();
        repayLoanPage.interestWaiver(true);
        repayLoanPage.submitAndNavigateToRepayLoanConfirmationPage(getRepayLoanParameters()).submitAndNavigateToLoanAccountDetailsPage();
    }

    private void verifySelectionOfWaiveInterest(RepayLoanPage repayLoanPage) {
        Assert.assertTrue(repayLoanPage.isWaiveInterestSelected());
        Assert.assertEquals(repayLoanPage.totalRepaymentAmount(), "1026.4");
        Assert.assertFalse(repayLoanPage.isTotalRepaymentAmountVisible());
        Assert.assertEquals(repayLoanPage.waivedRepaymentAmount(), "1024.5");
        Assert.assertTrue(repayLoanPage.isWaivedRepaymentAmoutVisible());
        Assert.assertTrue(repayLoanPage.isWaiverInterestWarningVisible());
        Assert.assertTrue(repayLoanPage.isWaiverInterestSelectorVisible());

    }

    private void testRepayLoanPageWhenProductHasNoInterestWaiver() {
        RepayLoanPage repayLoanPage = navigateToRepayLoanPage();
        verifySelectionOfDoNotWaiveInterest(repayLoanPage, false);
        verifySelectionInConfirmationPage(repayLoanPage, null);
    }

    private RepayLoanPage navigateToRepayLoanPage() {
        return navigationHelper.navigateToLoanAccountPage(loanId).navigateToRepayLoan();
    }

    private void verifySelectionInConfirmationPage(RepayLoanPage repayLoanPage, String waiveInterestConfirmationText) {
        RepayLoanConfirmationPage repayLoanConfirmationPage = repayLoanPage.submitAndNavigateToRepayLoanConfirmationPage(getRepayLoanParameters());
        Assert.assertEquals(repayLoanConfirmationPage.getSelectedValueForInterestWaiver(), waiveInterestConfirmationText);
        repayLoanPage = repayLoanConfirmationPage.edit();
        boolean stateToBeRetainedDuringEdit = StringUtils.equals("Yes", waiveInterestConfirmationText);
        Assert.assertEquals(repayLoanPage.isWaiveInterestSelected(), stateToBeRetainedDuringEdit);
    }

    private void verifySelectionOfDoNotWaiveInterest(RepayLoanPage repayLoanPage, boolean isWaiverInterestSelectorVisible) {
        Assert.assertFalse(repayLoanPage.isWaiveInterestSelected());
        Assert.assertEquals(repayLoanPage.totalRepaymentAmount(), "1026.4");
        Assert.assertTrue(repayLoanPage.isTotalRepaymentAmountVisible());
        Assert.assertEquals(repayLoanPage.waivedRepaymentAmount(), "1024.5");
        Assert.assertFalse(repayLoanPage.isWaivedRepaymentAmoutVisible());
        Assert.assertFalse(repayLoanPage.isWaiverInterestWarningVisible());
        Assert.assertEquals(repayLoanPage.isWaiverInterestSelectorVisible(), isWaiverInterestSelectorVisible);
    }

    private RepayLoanParameters getRepayLoanParameters() {
        RepayLoanParameters repayLoanParameters = new RepayLoanParameters();
        repayLoanParameters.setModeOfRepayment(RepayLoanParameters.CASH);
        return repayLoanParameters;
    }

    private void chargeFeeAndPenalty() {
        ChargeParameters chargeParameters = getChargeParameters("Misc Fees", "10");
        loanTestHelper.applyCharge(loanId, chargeParameters);
        chargeParameters = getChargeParameters("Misc Penalty", "5");
        loanTestHelper.applyCharge(loanId, chargeParameters);
    }

    private ChargeParameters getChargeParameters(String type, String amount) {
        ChargeParameters chargeParameters = new ChargeParameters();
        chargeParameters.setType(type);
        chargeParameters.setAmount(amount);
        return chargeParameters;
    }

    private DisburseLoanParameters getDisburseLoanParameters() {
        DisburseLoanParameters disburseLoanParameters = new DisburseLoanParameters();
        disburseLoanParameters.setDisbursalDateDD("08");
        disburseLoanParameters.setDisbursalDateMM("09");
        disburseLoanParameters.setDisbursalDateYYYY("2010");
        disburseLoanParameters.setPaymentType(DisburseLoanParameters.CASH);
        return disburseLoanParameters;
    }

    private void setupTimeAndDB() throws DatabaseUnitException, SQLException, IOException, URISyntaxException {
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2010, 9, 9, 13, 0, 0, 0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_007_dbunit.xml", dataSource, selenium);
    }
}