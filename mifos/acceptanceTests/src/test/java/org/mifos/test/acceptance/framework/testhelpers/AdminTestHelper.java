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

package org.mifos.test.acceptance.framework.testhelpers;

import org.mifos.test.acceptance.framework.admin.AdminPage;
import org.mifos.test.acceptance.framework.admin.ChecklistDetailsPage;
import org.mifos.test.acceptance.framework.admin.DefineChecklistParameters;
import org.mifos.test.acceptance.framework.admin.DefineLabelsPage;
import org.mifos.test.acceptance.framework.admin.DefineLabelsParameters;
import org.mifos.test.acceptance.framework.admin.DefineLookupOptionParameters;
import org.mifos.test.acceptance.framework.admin.DefineLookupOptionsPage;
import org.mifos.test.acceptance.framework.admin.SystemInfoPage;

import com.thoughtworks.selenium.Selenium;

public class AdminTestHelper {
    private final NavigationHelper navigationHelper;

    public AdminTestHelper(Selenium selenium) {
        this.navigationHelper = new NavigationHelper(selenium);
    }

    public DefineLookupOptionsPage navigateToDefineLookupOptionsPage() {
        return navigationHelper
                .navigateToAdminPage()
                .navigateDefineLookupOptionsPage();
    }

    public DefineLookupOptionsPage defineNewLookupOption(DefineLookupOptionParameters lookupOptionParams) {
        return navigationHelper
                .navigateToAdminPage()
                .navigateDefineLookupOptionsPage()
                .navigateToDefineLookupOptionPage(lookupOptionParams)
                .fillFormAndSubmit(lookupOptionParams);
    }

    public void verifyDefinedLookupOptionOnCreateNewClientPage(String officeName, DefineLookupOptionParameters lookupOptionParams) {
        navigationHelper
            .navigateToClientsAndAccountsPage()
            .navigateToCreateNewClientPage()
            .navigateToCreateClientWithoutGroupPage()
            .chooseOffice(officeName)
            .verifyLookupOption(lookupOptionParams);
    }

    public AdminPage defineLabels(DefineLabelsParameters defineLabelsParams) {
        DefineLabelsPage defineLabelsPage = navigationHelper
            .navigateToAdminPage()
            .navigateToDefineLabelsPage();
        for(String label : defineLabelsParams.getKeys()) {
            defineLabelsPage.setLabelValue(label, defineLabelsParams.getLabelText(label));
        }
        return defineLabelsPage.submit();
    }

    public AdminPage verifyLabels(DefineLabelsParameters defineLabelsParams) {
        DefineLabelsPage defineLabelsPage = navigationHelper
            .navigateToAdminPage()
            .navigateToDefineLabelsPage();
        for(String label : defineLabelsParams.getKeys()) {
            defineLabelsPage.verifyLabelValue(label, defineLabelsParams.getLabelText(label));
        }
        return defineLabelsPage.cancel();
    }

    public SystemInfoPage navigateToSystemInfoPage() {
        return navigationHelper
            .navigateToAdminPage()
            .navigateToSystemInfoPage();
    }

    public AdminPage defineNewChecklist(DefineChecklistParameters checklistParams) {
        return navigationHelper
            .navigateToAdminPage()
            .navigateToDefineNewChecklistPage()
            .fillFormAndNavigateToPreviewPage(checklistParams)
            .submit();
    }

    public ChecklistDetailsPage editChecklist(String currentChecklistName, DefineChecklistParameters checklistParams) {
        return navigateToChecklistDetailsPage(currentChecklistName)
            .navigateToEditChecklistPage()
            .fillFormAndNavigateToPreviewPage(checklistParams)
            .submit();
    }

    public ChecklistDetailsPage navigateToChecklistDetailsPage(String checklistName) {
        return navigationHelper
            .navigateToAdminPage()
            .navigateToViewChecklistsPage()
            .navigateToChecklistDetailsPage(checklistName);
    }
}
