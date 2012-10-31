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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.mifos.framework.util.ConfigurationLocator;
import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.admin.AdminPage;
import org.mifos.test.acceptance.framework.admin.ImportTransactionsConfirmationPage;
import org.mifos.test.acceptance.framework.admin.ImportTransactionsPage;
import org.mifos.test.acceptance.framework.admin.ManageRolePage;
import org.mifos.test.acceptance.framework.admin.ViewOrganizationSettingsPage;
import org.mifos.test.acceptance.framework.admin.ViewRolesPage;
import org.mifos.test.acceptance.framework.loan.LoanAccountPage;
import org.mifos.test.acceptance.framework.loan.TransactionHistoryPage;
import org.mifos.test.acceptance.framework.loan.ViewRepaymentSchedulePage;
import org.mifos.test.acceptance.framework.savings.SavingsAccountDetailPage;
import org.mifos.test.acceptance.framework.testhelpers.CustomPropertiesHelper;
import org.mifos.test.acceptance.framework.testhelpers.NavigationHelper;
import org.mifos.test.acceptance.framework.testhelpers.SavingsAccountHelper;
import org.mifos.test.acceptance.remote.DateTimeUpdaterRemoteTestingService;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This test requires installation of MPESA plugin and changing @Test(enabled=false) to @Test(enabled=true).
 *
 * The test imports the files from acceptanceTests/src/test/resources/mpesa/*.xls and for each file expects
 * that on import review page there will be messages from the corresponding file
 * acceptanceTests/src/test/resources/mpesa/*.xls.expected.txt
 */
@ContextConfiguration(locations = { "classpath:ui-test-context.xml" })
@Test(sequential = true, groups = {"import"})
public class MpesaImportTest extends UiTestCaseBase {

    private NavigationHelper navigationHelper;
    CustomPropertiesHelper propertiesHelper;
    private boolean copyPlugin = false;
    private static final String EXCEL_IMPORT_TYPE = "M-PESA Excel 97(-2007)";
    private static final String PLUGIN_NAME = "mpesa-xls-importer-0.0.2-SNAPSHOT-jar-with-dependencies.jar";
    private String configPath = new ConfigurationLocator().getConfigurationDirectory();
    static final String[] TEST_FILES = new String[] { "loan_product_code.xls", "mixed.xls", "saving_and_loan.xls",
        "savings_product_code.xls", "example_loan_disb.xls"};

    static final String FILE_WITH_NO_ERRORS = "import_no_errors.xls";
    static final String FILE_WITH_OVERPAYMENT_AMOUNT = "overpayment.xls";
    static final String DISBURSAL_IMPORT = "disbursal_import.xls";
    static final String PAYMENT_IMPORT = "payment_import.xls";

    @Autowired
    private DbUnitUtilities dbUnitUtilities;
    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void loadPlugin() throws Exception{

        if (!configPath.endsWith(File.separator)) {
            configPath += File.separator;
        }

        String plugin = MpesaImportTest.class.getResource("/mpesa/" + PLUGIN_NAME).getFile();
        File mpesa = new File(configPath+"plugins" + File.separator + PLUGIN_NAME);
        File folderPlugins = new File(configPath+"plugins");

        if(!folderPlugins.exists())
        {
            folderPlugins.mkdir();
        }

        if(!mpesa.exists())
        {
            mpesa.createNewFile();
            copyFile(new File(plugin), mpesa);
            copyPlugin = true;
        }
    }

    private void unloadPlugin()
    {
        File mpesa = new File(configPath +"plugins" + File.separator + PLUGIN_NAME);

        if(copyPlugin && mpesa.exists()){
            mpesa.delete();
        }
    }

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    // one of the dependent methods throws Exception
    @BeforeMethod
    public void setUp() throws Exception {
        loadPlugin();
        navigationHelper = new NavigationHelper(selenium);
        propertiesHelper = new CustomPropertiesHelper(selenium);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private String copyPluginToTemp() throws Exception {
        File mpesa = new File(configPath+"plugins" + File.separator + PLUGIN_NAME);
        File temp = File.createTempFile(PLUGIN_NAME, ".tmp");

        copyFile(mpesa,temp);

        mpesa.delete();

        return temp.getAbsolutePath();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void copyPluginFromTemp(String tempFileName) throws Exception {
        File temp = new File(tempFileName);
        File mpesa = new File(configPath+"plugins" + File.separator + PLUGIN_NAME);

        copyFile(temp,mpesa);

        temp.delete();
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
     }

    @AfterMethod
    public void tearDown() {
        (new MifosPage(selenium)).logout();
        unloadPlugin();
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.SystemPrintln"})
    @Test(enabled=true)
    public void importMpesaTransactions() throws Exception {
        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ViewOrganizationSettingsPage viewOrganizationSettingsPage = adminPage.navigateToViewOrganizationSettingsPage();
        viewOrganizationSettingsPage.verifyMiscellaneous(new String[]{"Max MPESA Disbursal Limit: 50000.0"});

        propertiesHelper.setImportTransactionOrder("AL3,AL5");

        String dataset = "mpesa_export.xml";
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, dataset, dataSource, selenium);

        for (String importFile : TEST_FILES) {
            String path = this.getClass().getResource("/mpesa/" + importFile).toString();
            importTransaction(path);
            checkIfOutputMatchesExpected(path);
        }
    }
    /**
     * MPESA - Import transactions from file with no errors
     * http://mifosforge.jira.com/browse/MIFOSTEST-688
     * @throws Exception
     */
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.SystemPrintln"})
    @Test(enabled = true)
    public void importTransactionsFromFileWithNoErrors() throws Exception {
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2011,01,28,12,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);

        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ViewRolesPage viewRolesPage = adminPage.navigateToViewRolesPage();
        ManageRolePage manageRolePage = viewRolesPage.navigateToManageRolePage("Admin");

        manageRolePage.disablePermission("8_3");

        viewRolesPage = manageRolePage.submitAndGotoViewRolesPage();
        adminPage = viewRolesPage.navigateToAdminPage();
        adminPage = adminPage.failNavigationToImportTransactionsPage();

        adminPage.verifyError("You do not have permissions to perform "
                + "this activity. Contact your system administrator "
                + "to grant you the required permissions and try again.");

        viewRolesPage = adminPage.navigateToViewRolesPage();
        manageRolePage = viewRolesPage.navigateToManageRolePage("Admin");

        manageRolePage.enablePermission("8_3");

        viewRolesPage = manageRolePage.submitAndGotoViewRolesPage();

        propertiesHelper.setImportTransactionOrder("AL3,AL5");

        String dataset = "mpesa_export.xml";
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, dataset, dataSource, selenium);

        String path = this.getClass().getResource("/mpesa/" + FILE_WITH_NO_ERRORS).toString();

        ImportTransactionsPage importTransactionsPage = importTransaction(path);
        checkIfOutputMatchesExpected(path);
        importTransactionsPage.cancelImportTransaction();

        LoanAccountPage loanAccountPage = navigationHelper.navigateToLoanAccountPage("000100000000013");
        loanAccountPage.verifyStatus(LoanAccountPage.ACTIVE);
        loanAccountPage.verifyExactLoanAmount("2000.0");

        SavingsAccountDetailPage savingsAccountDetailPage = navigationHelper.navigateToSavingsAccountDetailPage("000100000000015");
        savingsAccountDetailPage.verifySavingsAmount("0.0");

        adminPage = navigationHelper.navigateToAdminPage();
        importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        ImportTransactionsConfirmationPage importTransactionsConfirmationPage = importTransactionsPage.importTransactions(path, EXCEL_IMPORT_TYPE);

        importTransactionsConfirmationPage.verifyImportSuccess("You have successfully imported transactions.");

        loanAccountPage = navigationHelper.navigateToLoanAccountPage("000100000000013");
        loanAccountPage.verifyStatus(LoanAccountPage.CLOSED);

        ViewRepaymentSchedulePage viewRepaymentSchedulePage = loanAccountPage.navigateToRepaymentSchedulePage();
        viewRepaymentSchedulePage.verifyFirstInstallmentDate(5, 3, "28-Jan-2011");

        loanAccountPage = viewRepaymentSchedulePage.navigateToLoanAccountPage();

        loanAccountPage.verifyPerformanceHistory("11", "11");

        TransactionHistoryPage transactionHistoryPage = loanAccountPage.navigateToTransactionHistoryPage();

        transactionHistoryPage.verifyTransactionHistory(2013, 2, 48);
        transactionHistoryPage.verifyPostedBy("mifos", 48);

        savingsAccountDetailPage = navigationHelper.navigateToSavingsAccountDetailPage("000100000000015");
        savingsAccountDetailPage.verifySavingsAmount("3170.0");
        savingsAccountDetailPage.verifyDate("28/01/2011");
    }

    private void checkIfOutputMatchesExpected(String path) throws FileNotFoundException, IOException, URISyntaxException {
        DataInputStream in = new DataInputStream(new FileInputStream(new File(new URI(path + ".expected.txt"))));
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            if (!selenium.isTextPresent(line.trim())) {
                Assert.fail("No text <" + line.trim() + "> present on the page");
            }
        }
    }

    private ImportTransactionsPage importTransaction(String importFile) {
        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ImportTransactionsPage importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        importTransactionsPage.verifyPage();
        importTransactionsPage.reviewTransactions(importFile, EXCEL_IMPORT_TYPE);

        return importTransactionsPage;
    }

    //  Test the import transaction page loads with no plugins available  - regression test for MIFOS-2683
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled=true)
    public void importTransactionPageLoad() throws Exception {
        String tempFileName = copyPluginToTemp();
        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ImportTransactionsPage importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        importTransactionsPage.verifyPage();
        copyPluginFromTemp(tempFileName);
    }

    /**
     * MPESA - Import has expected errors due to invalid data
     * and overpayment amount and user is not able to continue
     * http://mifosforge.jira.com/browse/MIFOSTEST-692
     * @throws Exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled = true)
    public void failImportTransaction() throws Exception {
        //Given
        String path = this.getClass().getResource("/mpesa/" + FILE_WITH_OVERPAYMENT_AMOUNT).toString();
        String dataset = "mpesa_export.xml";

        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, dataset, dataSource, selenium);

        propertiesHelper.setImportTransactionOrder("AL3,AL5");

        SavingsAccountHelper savingsAccountHelper = new SavingsAccountHelper(selenium);
        savingsAccountHelper.closeSavingsAccount("000100000000019","Close account");

        //When
        ImportTransactionsPage importTransactionsPage = importTransaction(path);
        importTransactionsPage.checkErrors(new String[]{"Row <24> error - THY89933"
                + " - Last account is a loan account but the total paid in amount"
                + " is greater than the total due amount"});

        //Then
        LoanAccountPage loanAccountPage = navigationHelper.navigateToLoanAccountPage("000100000000013");
        loanAccountPage.verifyStatus(LoanAccountPage.ACTIVE);
        loanAccountPage.verifyExactLoanAmount("2000.0");

        TransactionHistoryPage transactionHistoryPage = loanAccountPage.navigateToTransactionHistoryPage();
        transactionHistoryPage.verifyTransactionHistory(183, 1, 6);
    }

    /**
     * MPESA - Import has errors and user chooses to import
     * a different file with some valid and invalid data.
     * http://mifosforge.jira.com/browse/MIFOSTEST-695
     * @throws Exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled = true)
    public void importTheSameFiles() throws Exception {
        //Given
        DateTimeUpdaterRemoteTestingService dateTimeUpdaterRemoteTestingService = new DateTimeUpdaterRemoteTestingService(selenium);
        DateTime targetTime = new DateTime(2011,01,28,12,0,0,0);
        dateTimeUpdaterRemoteTestingService.setDateTime(targetTime);
        String path = this.getClass().getResource("/mpesa/" + FILE_WITH_NO_ERRORS).toString();
        String dataset = "mpesa_export.xml";

        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, dataset, dataSource, selenium);

        propertiesHelper.setImportTransactionOrder("AL3,AL5");

        //When
        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        ImportTransactionsPage importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        ImportTransactionsConfirmationPage importTransactionsConfirmationPage = importTransactionsPage.importTransactions(path, EXCEL_IMPORT_TYPE);

        //Then
        importTransactionsConfirmationPage.verifyImportSuccess("You have successfully imported transactions.");
        LoanAccountPage loanAccountPage = navigationHelper.navigateToLoanAccountPage("000100000000013");
        loanAccountPage.verifyStatus(LoanAccountPage.CLOSED);

        ViewRepaymentSchedulePage viewRepaymentSchedulePage = loanAccountPage.navigateToRepaymentSchedulePage();
        viewRepaymentSchedulePage.verifyFirstInstallmentDate(5, 3, "28-Jan-2011");

        loanAccountPage = viewRepaymentSchedulePage.navigateToLoanAccountPage();

        loanAccountPage.verifyPerformanceHistory("11", "11");

        TransactionHistoryPage transactionHistoryPage = loanAccountPage.navigateToTransactionHistoryPage();

        transactionHistoryPage.verifyTransactionHistory(2013, 2, 48);
        transactionHistoryPage.verifyPostedBy("mifos", 48);

        SavingsAccountDetailPage savingsAccountDetailPage = navigationHelper.navigateToSavingsAccountDetailPage("000100000000015");
        savingsAccountDetailPage.verifySavingsAmount("3170.0");
        savingsAccountDetailPage.verifyDate("28/01/2011");

        //When
        adminPage = navigationHelper.navigateToAdminPage();
        importTransactionsPage = adminPage.navigateToImportTransactionsPage();
        importTransactionsPage = importTransactionsPage.failImportTransaction(path, EXCEL_IMPORT_TYPE);

        //Then
        importTransactionsPage.checkErrors(new String[]{"Same file has been already imported. Please import a different file."});
    }

    /**
     * Digits after decimal validate occurs for disbursal and payment import
     * http://mifosforge.jira.com/browse/MIFOSTEST-278
     * @throws Exception
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled = true)
    public void validateImport() throws Exception {
        //Given
        String path = this.getClass().getResource("/mpesa/" + DISBURSAL_IMPORT).toString();
        String dataset = "mpesa_export.xml";

        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, dataset, dataSource, selenium);

        propertiesHelper.setImportTransactionOrder("AL3,AL5");
        propertiesHelper.setDigitsAfterDecimal(0);
        //When
        ImportTransactionsPage importTransactionsPage = importTransaction(path);

        //Then
        importTransactionsPage.checkErrors(new String[]{"Row <24> error - "
                + "C94ZH942 - Number of fraction digits in the \"Withdrawn\""
                + " column - 3 - is greater than configured for the currency - 0"});

        //When
        path = this.getClass().getResource("/mpesa/" + PAYMENT_IMPORT).toString();
        importTransactionsPage = importTransaction(path);

        //Then
        importTransactionsPage.checkErrors(new String[]{"Row <24> error - THY89933"
                + " - Number of fraction digits in the \"Paid In\" column - 3 - "
                + "is greater than configured for the currency - 0"});

        propertiesHelper.setDigitsAfterDecimal(1);
    }
}