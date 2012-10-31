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

import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.loan.CreateMultipleLoanAccountSelectParameters;
import org.mifos.test.acceptance.framework.loanproduct.DefineNewLoanProductPage;
import org.mifos.test.acceptance.framework.testhelpers.FormParametersHelper;
import org.mifos.test.acceptance.framework.testhelpers.LoanTestHelper;
import org.mifos.test.acceptance.loanproduct.LoanProductTestHelper;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
@ContextConfiguration(locations={"classpath:ui-test-context.xml"})
@Test(sequential=true, groups={"loan","acceptance","ui"})
public class CreateMultipleLoanAccountsWithFeesTest extends UiTestCaseBase {

    private LoanProductTestHelper loanProductTestHelper;
    private LoanTestHelper loanTestHelper;

    @Autowired
    private DriverManagerDataSource dataSource;
    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // one of the dependent methods throws Exception
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        loanProductTestHelper = new LoanProductTestHelper(selenium);
        loanTestHelper = new LoanTestHelper(selenium);
    }

    @AfterMethod
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // http://mifosforge.jira.com/browse/MIFOSTEST-54
    public void createBulkLoanAccountsInPendingApprovalStatus() throws Exception{
        //Given
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);
        DefineNewLoanProductPage.SubmitFormParameters formParameters = FormParametersHelper.getWeeklyLoanProductParametersWithInstallmentsSetsByLastLoanAmount();
        CreateMultipleLoanAccountSelectParameters multipleAccParameters = new CreateMultipleLoanAccountSelectParameters();
        multipleAccParameters.setBranch("MyOffice1233265929385");
        multipleAccParameters.setLoanOfficer("Joe1233265931256 Guy1233265931256");
        multipleAccParameters.setCenter("MyCenter1233265933427");
        multipleAccParameters.setLoanProduct(formParameters.getOfferingName());
        String[] clients = new String[3];
        clients[0] = "Stu1233265941610 Client1233265941610";
        clients[1] = "Stu1233265958456 Client1233265958456";
        clients[2] = "Stu1233265968663 Client1233265968663";
        String[] clientsDeselected = new String[9];
        clientsDeselected[0] = "Stu1233265978656 Client1233265978656";
        clientsDeselected[1] = "Stu1233265991241 Client1233265991241";
        clientsDeselected[2] = "Stu1233266001331 Client1233266001331";
        clientsDeselected[3] = "Stu1233266011478 Client1233266011478";
        clientsDeselected[4] = "Stu1233266021748 Client1233266021748";
        clientsDeselected[5] = "Stu1233266033986 Client1233266033986";
        clientsDeselected[6] = "Stu1233266043526 Client1233266043526";
        clientsDeselected[7] = "Stu1233266053368 Client1233266053368";
        clientsDeselected[8] = "Stu1233266063395 Client1233266063395";

        //When
        loanProductTestHelper.defineNewLoanProduct(formParameters, "OneTimeFirstRepay90", "OneTimeUpFront900");

        //Then
        loanTestHelper.createMultipleLoanAccountsAndVerify(multipleAccParameters, clients, "0000-Animal Husbandry", "990.0", LoanTestHelper.METHOD_SUBMIT_FOR_APPROVAL);
        loanTestHelper.verifyLoansAreNotOnClientsLoanLists(clientsDeselected, formParameters.getOfferingName());
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // http://mifosforge.jira.com/browse/MIFOSTEST-55
    public void createBulkLoanAccountsInPartialApllicationStatus() throws Exception{
        //Given
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_011_dbunit.xml", dataSource, selenium);
        DefineNewLoanProductPage.SubmitFormParameters formParameters = FormParametersHelper.getWeeklyLoanProductParametersWithInstallmentsSetsByLastLoanAmount();
        CreateMultipleLoanAccountSelectParameters multipleAccParameters = new CreateMultipleLoanAccountSelectParameters();
        multipleAccParameters.setLoanProduct(formParameters.getOfferingName());
        multipleAccParameters.setBranch("MyOffice1233265929385");
        multipleAccParameters.setCenter("MyCenter1233265933427");
        multipleAccParameters.setLoanOfficer("Joe1233265931256 Guy1233265931256");
        String[] clients = new String[3];
        clients[0] = "Stu1233265941610 Client1233265941610";
        clients[1] = "Stu1233265958456 Client1233265958456";
        clients[2] = "Stu1233265968663 Client1233265968663";

        //When
        loanProductTestHelper.defineNewLoanProduct(formParameters);

        //Then
        loanTestHelper.createMultipleLoanAccountsAndVerify(multipleAccParameters, clients, "0000-Animal Husbandry", "0.0", LoanTestHelper.METHOD_SAVE_FOR_LATER);
    }
}

