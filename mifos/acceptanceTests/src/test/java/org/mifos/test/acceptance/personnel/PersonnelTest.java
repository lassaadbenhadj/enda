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

package org.mifos.test.acceptance.personnel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mifos.framework.util.DbUnitUtilities;
import org.mifos.test.acceptance.framework.AppLauncher;
import org.mifos.test.acceptance.framework.HomePage;
import org.mifos.test.acceptance.framework.MifosPage;
import org.mifos.test.acceptance.framework.UiTestCaseBase;
import org.mifos.test.acceptance.framework.admin.AdminPage;
import org.mifos.test.acceptance.framework.loan.QuestionResponseParameters;
import org.mifos.test.acceptance.framework.login.ChangePasswordPage;
import org.mifos.test.acceptance.framework.login.LoginPage;
import org.mifos.test.acceptance.framework.office.ChooseOfficePage;
import org.mifos.test.acceptance.framework.questionnaire.CreateQuestionGroupParameters;
import org.mifos.test.acceptance.framework.questionnaire.QuestionResponsePage;
import org.mifos.test.acceptance.framework.questionnaire.QuestionnairePage;
import org.mifos.test.acceptance.framework.testhelpers.NavigationHelper;
import org.mifos.test.acceptance.framework.testhelpers.UserHelper;
import org.mifos.test.acceptance.framework.user.CreateUserConfirmationPage;
import org.mifos.test.acceptance.framework.user.CreateUserEnterDataPage;
import org.mifos.test.acceptance.framework.user.CreateUserParameters;
import org.mifos.test.acceptance.framework.user.CreateUserPreviewDataPage;
import org.mifos.test.acceptance.framework.user.EditUserDataPage;
import org.mifos.test.acceptance.framework.user.EditUserPreviewDataPage;
import org.mifos.test.acceptance.framework.user.UserViewDetailsPage;
import org.mifos.test.acceptance.framework.testhelpers.QuestionGroupTestHelper;
import org.mifos.test.acceptance.remote.InitializeApplicationRemoteTestingService;
import org.mifos.test.acceptance.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
@ContextConfiguration(locations = { "classpath:ui-test-context.xml" })
@Test(sequential = true, groups = {"personnel","acceptance","ui"})
public class PersonnelTest extends UiTestCaseBase {

    private NavigationHelper navigationHelper;
    private UserHelper userHelper;
    private QuestionGroupTestHelper questionGroupTestHelper;

    @Autowired
    private DriverManagerDataSource dataSource;

    @Autowired
    private DbUnitUtilities dbUnitUtilities;

    @Autowired
    private InitializeApplicationRemoteTestingService initRemote;

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.setUp();
        navigationHelper = new NavigationHelper(selenium);
        userHelper = new UserHelper(selenium);
        questionGroupTestHelper = new QuestionGroupTestHelper(selenium);
    }

    @AfterMethod
    public void logOut() {
        (new MifosPage(selenium)).logout();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled=true)
    public void editUserTest() throws Exception {
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_003_dbunit.xml", dataSource, selenium);

        AdminPage adminPage = navigationHelper.navigateToAdminPage();

        UserViewDetailsPage userDetailsPage = userHelper.createUser(adminPage.getAdminUserParameters(), "MyOffice1233171674227");

        EditUserDataPage editUserPage = userDetailsPage.navigateToEditUserDataPage();

        CreateUserParameters formParameters = new CreateUserParameters();
        formParameters.setFirstName("Update");
        formParameters.setLastName("User" + StringUtil.getRandomString(8));
        formParameters.setEmail("xxx.yyy@xxx.zzz");

        EditUserPreviewDataPage editPreviewDataPage = editUserPage.submitAndGotoEditUserPreviewDataPage(formParameters);
        UserViewDetailsPage userDetailsPage2 = editPreviewDataPage.submit();
        userDetailsPage2.verifyModifiedNameAndEmail(formParameters);
    }

    //http://mifosforge.jira.com/browse/MIFOSTEST-298
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Test(enabled=true)
    public void createUserWithNonAdminRoleTest() throws Exception {
        //Given
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_003_dbunit.xml", dataSource, selenium);

        AdminPage adminPage = navigationHelper.navigateToAdminPage();
        CreateUserParameters formParameters = adminPage.getNonAdminUserParameters();
        //When
        userHelper.createUser(formParameters, "MyOffice1233171674227");
        LoginPage loginPage = new AppLauncher(selenium).launchMifos();
        loginPage.verifyPage();
        //Then
        HomePage homePage = loginPage.loginSuccessfulAsWithChnagePasw(formParameters.getUserName(), formParameters.getPassword());
        homePage.verifyPage();
        adminPage=navigationHelper.navigateToAdminPageAsLogedUser(formParameters.getUserName(), "newPasw");
        adminPage.navigateToCreateUserPage();
        String error = selenium.getText("admin.error.message");
        Assert.assertEquals(error.contains("You do not have permissions to perform this activity. Contact your system administrator to grant you the required permissions and try again."), true);
    }

    //http://mifosforge.jira.com/browse/MIFOSTEST-670
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void createUserWithQuestionGroup()  throws Exception {
        //Given
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_016_dbunit.xml", dataSource, selenium);
        //When
        Map<String, List<String>> sectionQuestions = new HashMap<String, List<String>>();

        List<String> questions = new ArrayList<String>();

        questions.add("question 4");
        questions.add("question 3");
        questions.add("Date");

        sectionQuestions.put("Sec 1", questions);

        questions = new ArrayList<String>();
        questions.add("Number");
        questions.add("question 1");
        questions.add("DateQuestion");
        questions.add("Text");

        sectionQuestions.put("Sec 2", questions);

        CreateQuestionGroupParameters createQuestionGroupParameters = new CreateQuestionGroupParameters();
        createQuestionGroupParameters.setAnswerEditable(true);
        createQuestionGroupParameters.setAppliesTo("Create Personnel");
        createQuestionGroupParameters.setTitle("Create Personnel QG1");
        createQuestionGroupParameters.setExistingQuestions(sectionQuestions);

        questionGroupTestHelper.createQuestionGroup(createQuestionGroupParameters);

        sectionQuestions = new HashMap<String, List<String>>();
        questions = new ArrayList<String>();
        questions.add("question 4");
        sectionQuestions.put("Sec 3", questions);

        createQuestionGroupParameters = new CreateQuestionGroupParameters();
        createQuestionGroupParameters.setAnswerEditable(true);
        createQuestionGroupParameters.setAppliesTo("Create Personnel");
        createQuestionGroupParameters.setTitle("Create Personnel QG2");
        createQuestionGroupParameters.setExistingQuestions(sectionQuestions);

        questionGroupTestHelper.createQuestionGroup(createQuestionGroupParameters);

        AdminPage adminPage = navigationHelper.navigateToAdminPage();

        CreateUserParameters userParameters = adminPage.getAdminUserParameters();

        ChooseOfficePage createUserPage = adminPage.navigateToCreateUserPage();
        createUserPage.verifyPage();

        CreateUserEnterDataPage userEnterDataPage = createUserPage.selectOffice("MyOffice1233171674227");

        QuestionResponsePage questionResponsePage = userEnterDataPage.submitAndNavigateToQuestionResponsePage(userParameters);
        questionResponsePage.verifyPage();

        QuestionResponseParameters responseParameters = new QuestionResponseParameters();
        responseParameters.addSingleSelectAnswer("questionGroups[0].sectionDetails[0].questions[2].value", "yes");
        responseParameters.addTextAnswer("questionGroups[0].sectionDetails[1].questions[3].value", "text1");

        questionResponsePage.populateAnswers(responseParameters);

        CreateUserPreviewDataPage createUserPreviewDataPage = questionResponsePage.continueAndNavigateToCreateUserPreviewPage();

        questionResponsePage = createUserPreviewDataPage.navigateToEditAdditionalInformation();

        questionResponsePage.populateTextAnswer("questionGroups[0].sectionDetails[1].questions[3].value", "text2");

        createUserPreviewDataPage = questionResponsePage.continueAndNavigateToCreateUserPreviewPage();

        CreateUserConfirmationPage userConfirmationPage = createUserPreviewDataPage.submit();

        QuestionnairePage questionnairePage = userConfirmationPage.navigateToUserViewDetailsPage().navigateToQuestionnairePage();
        //Then
        questionnairePage.verifyField("details[0].sectionDetails[1].questions[3].value", "text2");
        questionnairePage.verifyRadioGroup("details[0].sectionDetails[0].questions[2].value", "yes", true);
        //When
        questionnairePage.typeText("details[0].sectionDetails[1].questions[3].value", "text3");

        questionnairePage.submitAndNavigateToPersonnalDetailsPage();

        List<String> questionToAdd= new ArrayList<String>();
        questionToAdd.add("question 2");
        questionToAdd.add("question 5");

        List<String> questionToDesactivate = new ArrayList<String>();
        questionToDesactivate.add("DateQuestion");
        questionToDesactivate.add("Number");
        questionToDesactivate.add("question 1");
        questionToDesactivate.add("Text");

        createQuestionGroupParameters = new CreateQuestionGroupParameters();
        for (String question : questionToAdd) {
            createQuestionGroupParameters.addExistingQuestion("Sec 1", question);
        }
        questionGroupTestHelper.addQuestionsToQuestionGroup("Create Personnel QG1", createQuestionGroupParameters.getExistingQuestions());

        for (String question : questionToDesactivate) {
            questionGroupTestHelper.markQuestionAsInactive(question);
        }
        questionGroupTestHelper.markQuestionGroupAsInactive("Create Personnel QG2");

        adminPage = navigationHelper.navigateToAdminPage();

        userParameters = adminPage.getAdminUserParameters();

        createUserPage = adminPage.navigateToCreateUserPage();
        createUserPage.verifyPage();

        userEnterDataPage = createUserPage.selectOffice("MyOffice1233171674227");

        questionResponsePage = userEnterDataPage.submitAndNavigateToQuestionResponsePage(userParameters);
        questionResponsePage.verifyPage();
        //Then
        questionResponsePage.verifyQuestionsDoesnotappear(questionToDesactivate.toArray(new String[questionToDesactivate.size()]));
        questionResponsePage.verifyQuestionsExists(questionToAdd.toArray(new String[questionToAdd.size()]));
        questionResponsePage.verifySectionDoesnotappear("Sec 2");
    }

    @Test(enabled=false) // http://mifosforge.jira.com/browse/MIFOS-4755
    //http://mifosforge.jira.com/browse/MIFOSTEST-296
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void createUserTest() throws Exception {
        //Given
        initRemote.dataLoadAndCacheRefresh(dbUnitUtilities, "acceptance_small_003_dbunit.xml", dataSource, selenium);
        //When
        HomePage homePage = loginSuccessfully();
        AdminPage adminPage = homePage.navigateToAdminPage();

        CreateUserParameters userParameters = adminPage.getAdminUserParameters();
        ChooseOfficePage createUserPage = adminPage.navigateToCreateUserPage();
        createUserPage.verifyPage();

        CreateUserEnterDataPage userEnterDataPage = createUserPage.selectOffice("MyOffice1233171674227");
        userParameters.setPasswordRepeat("pass");
        userEnterDataPage = userEnterDataPage.submitAndReturnToThisPage(userParameters);
        //Then
        userEnterDataPage.verifyPasswordChangeError();
        //When
        userParameters.setPasswordRepeat("password");
        userParameters.setDateOfBirthYYYY("mmm");
        userEnterDataPage = userEnterDataPage.submitAndReturnToThisPage(userParameters);
        //Then
        userEnterDataPage.verifyDateError();
        //When
        userParameters.setDateOfBirthYYYY("1980");
        CreateUserPreviewDataPage userPreviewDataPage = userEnterDataPage.submitAndGotoCreateUserPreviewDataPage(userParameters);
        CreateUserConfirmationPage userConfirmationPage = userPreviewDataPage.submit();
        //Then
        userConfirmationPage.verifyPage();
        UserViewDetailsPage userDetailsPage = userConfirmationPage.navigateToUserViewDetailsPage();
        userDetailsPage.verifyPage();

        Assert.assertTrue(userDetailsPage.getFullName().contains(userParameters.getFirstName() + " " + userParameters.getLastName()));
        //When
        EditUserDataPage editUserPage = userDetailsPage.navigateToEditUserDataPage();

        CreateUserParameters passwordParameters = new CreateUserParameters();
        passwordParameters.setPassword("tester1");
        passwordParameters.setPasswordRepeat("tester");
        //Then
        editUserPage = editUserPage.submitWithInvalidData(passwordParameters);
        editUserPage.verifyPasswordChangeError();
        //When
        passwordParameters.setPasswordRepeat("tester1");
        //Then
        EditUserPreviewDataPage editPreviewDataPage = editUserPage.submitAndGotoEditUserPreviewDataPage(passwordParameters);
        UserViewDetailsPage submitUserpage = editPreviewDataPage.submit();
        submitUserpage.verifyPage();
        //When
        LoginPage loginPage = (new MifosPage(selenium)).logout();

        ChangePasswordPage changePasswordPage = loginPage.loginAndGoToChangePasswordPageAs(userParameters.getUserName(), passwordParameters.getPassword()); // tester1

        ChangePasswordPage.SubmitFormParameters changePasswordParameters = new ChangePasswordPage.SubmitFormParameters();
        changePasswordParameters.setOldPassword("tester"); // wrong old password
        changePasswordParameters.setNewPassword(""); // empty new password
        changePasswordParameters.setConfirmPassword("");

        //Then
        changePasswordPage = changePasswordPage.submitWithInvalidData(changePasswordParameters);
        //When
        changePasswordParameters.setNewPassword("tester2"); //wrong old password with good new
        changePasswordParameters.setConfirmPassword("tester2");
        //Then
        changePasswordPage = changePasswordPage.submitWithInvalidData(changePasswordParameters);
        //When
        changePasswordParameters.setOldPassword("tester1"); // good old password and good new
        changePasswordParameters.setNewPassword("tester2");
        changePasswordParameters.setConfirmPassword("tester2");
        //Then
        HomePage homePage2 = changePasswordPage.submitAndGotoHomePage(changePasswordParameters);
        Assert.assertTrue(homePage2.getWelcome().contains(userParameters.getFirstName()));

        loginPage = (new MifosPage(selenium)).logout();
        homePage = loginPage.loginSuccessfulAs(userParameters.getUserName(), "tester2");

        changePasswordPage = homePage.navigateToYourSettingsPage().navigateToChangePasswordPage();

        changePasswordPage = changePasswordPage.submitWithInvalidData(changePasswordParameters);
        //When
        changePasswordParameters.setNewPassword("tester2"); //wrong old password with good new
        changePasswordParameters.setConfirmPassword("tester2");
        //Then
        changePasswordPage = changePasswordPage.submitWithInvalidData(changePasswordParameters);
        //When
        changePasswordParameters.setOldPassword("tester2"); // good old password and good new
        changePasswordParameters.setNewPassword("tester3");
        changePasswordParameters.setConfirmPassword("tester3");
        changePasswordPage.submitAndGotoHomePage(changePasswordParameters);

        loginPage = (new MifosPage(selenium)).logout();
        homePage = loginPage.loginSuccessfulAs(userParameters.getUserName(), changePasswordParameters.getNewPassword());

        Assert.assertTrue(homePage.getWelcome().contains(userParameters.getFirstName()));
    }

    private HomePage loginSuccessfully() {
        (new MifosPage(selenium)).logout();
        LoginPage loginPage = new AppLauncher(selenium).launchMifos();
        loginPage.verifyPage();
        HomePage homePage = loginPage.loginSuccessfullyUsingDefaultCredentials();
        homePage.verifyPage();

        return homePage;
    }
}
