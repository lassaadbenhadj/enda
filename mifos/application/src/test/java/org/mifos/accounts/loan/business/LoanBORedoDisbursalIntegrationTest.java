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

package org.mifos.accounts.loan.business;

import static org.apache.commons.lang.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang.math.NumberUtils.SHORT_ZERO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.business.AccountBO;
import org.mifos.accounts.business.AccountFeesEntity;
import org.mifos.accounts.business.AccountPaymentEntity;
import org.mifos.accounts.business.AccountTrxnEntity;
import org.mifos.accounts.exceptions.AccountException;
import org.mifos.accounts.fees.business.FeeBO;
import org.mifos.accounts.fees.util.helpers.FeeCategory;
import org.mifos.accounts.fees.util.helpers.FeeFormula;
import org.mifos.accounts.fees.util.helpers.FeePayment;
import org.mifos.accounts.financial.business.GLCodeEntity;
import org.mifos.accounts.fund.business.FundBO;
import org.mifos.accounts.productdefinition.business.GracePeriodTypeEntity;
import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
import org.mifos.accounts.productdefinition.business.LoanOfferingTestUtils;
import org.mifos.accounts.productdefinition.business.PrdApplicableMasterEntity;
import org.mifos.accounts.productdefinition.business.PrdStatusEntity;
import org.mifos.accounts.productdefinition.business.ProductCategoryBO;
import org.mifos.accounts.productdefinition.exceptions.ProductDefinitionException;
import org.mifos.accounts.productdefinition.util.helpers.ApplicableTo;
import org.mifos.accounts.productdefinition.util.helpers.GraceType;
import org.mifos.accounts.productdefinition.util.helpers.InterestType;
import org.mifos.accounts.productdefinition.util.helpers.PrdStatus;
import org.mifos.accounts.util.helpers.AccountActionTypes;
import org.mifos.accounts.util.helpers.AccountConstants;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.accounts.util.helpers.PaymentData;
import org.mifos.application.master.business.InterestTypesEntity;
import org.mifos.application.meeting.MeetingTemplateImpl;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.meeting.util.helpers.RecurrenceType;
import org.mifos.application.meeting.util.helpers.WeekDay;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.center.CenterTemplate;
import org.mifos.customers.center.CenterTemplateImpl;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.center.persistence.CenterPersistence;
import org.mifos.customers.exceptions.CustomerException;
import org.mifos.customers.group.GroupTemplate;
import org.mifos.customers.group.GroupTemplateImpl;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.group.persistence.GroupPersistence;
import org.mifos.customers.group.util.helpers.GroupConstants;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.persistence.LegacyPersonnelDao;
import org.mifos.domain.builders.CenterBuilder;
import org.mifos.domain.builders.MeetingBuilder;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.TestUtils;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.ValidationException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.persistence.TestObjectPersistence;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.IntegrationTestObjectMother;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.MoneyUtils;
import org.mifos.framework.util.helpers.TestGeneralLedgerCode;
import org.mifos.framework.util.helpers.TestObjectFactory;
import org.mifos.security.util.UserContext;

/**
 * FIXME - #000001 - keithw - Complete rewrite implementation of these integration tests moving away from template, fixture and
 * use of persistence to using Builder and ObjectMother patterns that leverage appropriate static factory methods of
 * customer classes.
 *
 */
@Deprecated
public class LoanBORedoDisbursalIntegrationTest extends MifosIntegrationTestCase {

    private CenterPersistence centerPersistence;
    private GroupPersistence groupPersistence;
    private UserContext userContext;
    private CustomerBO center = null;
    private CustomerBO group = null;
    private AccountBO loanBO = null;
    private MeetingBO meeting = null;
    private FeeBO fee;

    @Before
    public void setUp() throws Exception {
        centerPersistence = new CenterPersistence();
        groupPersistence = new GroupPersistence();
        initializeStatisticsService();
        userContext = TestUtils.makeUser();
        fee = null;
    }

    @After
    public void tearDown() throws Exception {
        // TestObjectFactory.removeObject(loanOffering);
        if (loanBO != null) {
            loanBO = (AccountBO) StaticHibernateUtil.getSessionTL().get(AccountBO.class, loanBO.getAccountId());
        }
        if (group != null) {
            group = (CustomerBO) StaticHibernateUtil.getSessionTL().get(CustomerBO.class, group.getCustomerId());
        }
        if (center != null) {
            center = (CustomerBO) StaticHibernateUtil.getSessionTL().get(CustomerBO.class, center.getCustomerId());
        }
        loanBO = null;
        group = null;
        center = null;
        if (null != fee) {
            fee = (FeeBO) StaticHibernateUtil.getSessionTL().get(FeeBO.class, fee.getFeeId());
            fee = null;
        }
        StaticHibernateUtil.flushSession();
    }

    public void testDummy() {
        // dummy test to keep test running
    }

    @Ignore
    @Test
    public void testRedoLoan() throws Exception {

        int loanStartDaysAgo = 14;
        int paymentDaysAgo = 8;

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, loanStartDaysAgo, new ArrayList<AccountFeesEntity>());

        disburseLoanAndVerify(userContext, loan, loanStartDaysAgo);

        Assert.assertFalse(loan.havePaymentsBeenMade());

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyAndVerifyPayment(userContext, loan, paymentDaysAgo, new Money(getCurrency(), "50"));

        Assert.assertTrue(loan.havePaymentsBeenMade());

        LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore
    @Test
    public void testRedoLoanApplyWholeMiscPenaltyBeforeRepayments() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        Double feeAmount = new Double("33.0");
        Assert.assertTrue(MoneyUtils.isRoundedAmount(feeAmount));
        applyCharge(loan, Short.valueOf(AccountConstants.MISC_PENALTY), feeAmount);

        Assert.assertFalse(loan.havePaymentsBeenMade());

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 33.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore
    @Test
    public void testRedoLoanApplyWholeMiscPenaltyAfterPartialPayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "50"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, Short.valueOf(AccountConstants.MISC_PENALTY), new Double("33"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 33.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore
    @Test
    public void testRedoLoanApplyWholeMiscPenaltyAfterFullPayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        // make one full repayment
        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, Short.valueOf(AccountConstants.MISC_PENALTY), new Double("33"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 33.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore
    @Test
    public void testRedoLoanApplyFractionalMiscPenaltyBeforeRepayments() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        applyCharge(loan, Short.valueOf(AccountConstants.MISC_PENALTY), new Double("33.7"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.2, 0.1, 0.0, 0.0, 33.7);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.2, 0.8, 0.0, 0.0, 0.0);
    }

    @Ignore
    @Test(expected = org.mifos.accounts.exceptions.AccountException.class)
    public void testRedoLoanApplyFractionalMiscPenaltyAfterPartialPayment() throws Exception {

        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "50"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            Assert.assertFalse(MoneyUtils.isRoundedAmount(33.7));
            Assert.assertFalse(loan.canApplyMiscCharge(new Money(getCurrency(), new BigDecimal(33.7))));
            // Should throw AccountExcption
            applyCharge(loan, Short.valueOf(AccountConstants.MISC_PENALTY), new Double("33.7"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 51.2, 0.1, 0.0, 0.0, 33.7);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.2, 0.8, 0.0, 0.0, 0.0);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore
    @Test
    public void testRedoLoanApplyFractionalMiscPenaltyAfterFullPayment() throws Exception {

        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            // make one full repayment
            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            // Should throw AccountException
            applyCharge(loan, Short.valueOf(AccountConstants.MISC_PENALTY), new Double("33.7"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 51.2, 0.1, 0.0, 0.0, 33.7);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.2, 0.8, 0.0, 0.0, 0.0);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore
    @Test
    public void testRedoLoanApplyWholeMiscFeeBeforeRepayments() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        Double feeAmount = new Double("33.0");
        Assert.assertTrue(MoneyUtils.isRoundedAmount(feeAmount));
        applyCharge(loan, Short.valueOf(AccountConstants.MISC_FEES), feeAmount);

        Assert.assertFalse(loan.havePaymentsBeenMade());

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 33.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyWholeMiscFeeAfterPartialPayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "50"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, Short.valueOf(AccountConstants.MISC_FEES), new Double("33"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 33.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyWholeMiscFeeAfterFullPayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        // make one full repayment
        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, Short.valueOf(AccountConstants.MISC_FEES), new Double("33"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 33.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyFractionalMiscFeeBeforeRepayments() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        applyCharge(loan, Short.valueOf(AccountConstants.MISC_FEES), new Double("33.7"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.2, 0.1, 0.0, 33.7, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.2, 0.8, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyFractionalMiscFeeAfterPartialPayment() throws Exception {

        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "50"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            Assert.assertFalse(MoneyUtils.isRoundedAmount(33.7));
            Assert.assertFalse(loan.canApplyMiscCharge(new Money(getCurrency(), new BigDecimal(33.7))));
            // Should throw AccountExcption
            applyCharge(loan, Short.valueOf(AccountConstants.MISC_FEES), new Double("33.7"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 1.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 51.2, 0.1, 0.0, 33.7, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.2, 0.8, 0.0, 0.0, 0.0);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore @Test
    public void testRedoLoanApplyFractionalMiscFeeAfterFullPayment() throws Exception {

        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            // make one full repayment
            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            // Should throw AccountException
            applyCharge(loan, Short.valueOf(AccountConstants.MISC_FEES), new Double("33.7"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 51.2, 0.1, 0.0, 33.7, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.2, 0.8, 0.0, 0.0, 0.0);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore
    @Test
    public void testRedoLoanWithOneTimeWholeAmountFee() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, createFeeViewsWithOneTimeAmountFee(10.0));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyOneTimeWholeAmountFeeBeforeRepayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, createOneTimeAmountFee(10.0).getFeeId(), 10.0);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyOneTimeWholeAmountFeeAfterRepayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

        // make one full repayment
        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, createOneTimeAmountFee(10.0).getFeeId(), 10.0);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanWithOneTimeFractionalAmountFee() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, createFeeViewsWithOneTimeAmountFee(10.2));
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 61.0, 50.7, 0.1, 10.2, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.7, 0.3, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyOneTimeFractionalAmountFeeBeforeRepayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, createOneTimeAmountFee(10.2).getFeeId(), 10.2);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 62.0, 51.7, 0.1, 10.2, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 44.7, 1.3, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanWithOneTimeFractionalAmountFeeAfterRepayment() throws Exception {
        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

            // make one full repayment
            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            applyCharge(loan, createOneTimeAmountFee(10.2).getFeeId(), 10.2);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore @Test
    public void testRedoLoanWithPeriodicWholeAmountFee() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, createFeeViewsWithPeriodicAmountFee(10.0));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 56.0, 45.5, 0.5, 10.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyPeriodicWholeAmountFeeBeforeRepayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, createPeriodicAmountFee(10.0).getFeeId(), 10.0);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 71.0, 50.9, 0.1, 20.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 56.0, 45.5, 0.5, 10.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanWithPeriodicWholeAmountFeeAfterRepayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

        // make one full repayment
        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, createPeriodicAmountFee(10.0).getFeeId(), 10.0);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 71.0, 50.9, 0.1, 20.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 61.0, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 56.0, 45.5, 0.5, 10.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanWithPeriodicRateFee() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, createFeeViewsWithPeriodicRateFee(5.0));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 15.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanApplyPeriodicRateFeeBeforeRepayment() throws Exception {

        LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

        applyCharge(loan, createPeriodicRateFee(5.0).getFeeId(), 5.0);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 30.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 15.0, 0.0, 0.0);

    }

    @Ignore @Test
    public void testRedoLoanApplyPeriodicRateFeeAfterRepayment() throws Exception {
        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, new ArrayList<AccountFeesEntity>());
            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 51.0, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 45.5, 0.5, 0.0, 0.0, 0.0);

            // make one full repayment
            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "51"));

            LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

            // Expect AccountException
            applyCharge(loan, createPeriodicRateFee(5.0).getFeeId(), 5.0);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore @Test
    public void testRedoLoanRemovePeriodicWholeAmountFeeBeforeRepayment() throws Exception {

        LoanBO loan = redoLoanWithMeetingTodayAndVerify(userContext, 14, createFeeViewsWithPeriodicAmountFee(10.0));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 10.0, 0.0, 0.0);

        removeAccountFee(loan);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    @Ignore @Test
    public void testRedoLoanRemovePeriodicWholeAmountFeeAfterRepayment() throws Exception {

        LoanBO loan = redoLoanWithMeetingTodayAndVerify(userContext, 14, createFeeViewsWithPeriodicAmountFee(10.0));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 10.0, 0.0, 0.0);

        // make one full repayment
        applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "61"));

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 10.0, 0.0, 0.0);

        removeAccountFee(loan);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 0.0, 0.0, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 10.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);
    }

    /**
     * Removing a periodic amount fee, whose amount is more precise than the rounding precision specified for the
     * installment, should result in an AccountException.
     * <p>
     * TODO: financial-calculation. This is a short-term solution (V1.1) until it is determined how rounding should be
     * applied when fees or charges are added or removed partway through a loan cycle.
     */

    @Ignore @Test
    public void testRedoLoanRemovePeriodicFractionalAmountFeeBeforePayment() throws Exception {

        LoanBO loan = redoLoanWithMeetingTodayAndVerify(userContext, 14, createFeeViewsWithPeriodicAmountFee(5.1));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 46.0, 0.9, 5.1, 0.0, 0.0);

        removeAccountFee(loan);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.8, 0.1, 5.1, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.7, 1.3, 0.0, 0.0, 0.0);
    }

    /**
     * Removing a periodic amount fee, whose amount (5.1) is more precise than the rounding precision specified for the
     * installment (0), should result in an AccountException.
     * <p>
     * TODO: financial-calculation. This is a short-term solution (V1.1) until it is determined how rounding should be
     * applied when fees or charges are added or removed partway through a loan cycle.
     */

    @Ignore @Test
    public void testRedoLoanRemovePeriodicFractionalAmountFeeAfterPayment() throws Exception {
        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, createFeeViewsWithPeriodicAmountFee(5.1));

            disburseLoanAndVerify(userContext, loan, 14);

            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "50"));

            // expect account exception
            removeAccountFee(loan);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    @Ignore @Test
    public void testRedoLoanRemovePeriodicRateFeeBeforeRepayment() throws Exception {

        LoanBO loan = redoLoanWithMeetingTodayAndVerify(userContext, 14, createFeeViewsWithPeriodicRateFee(5.0));

        disburseLoanAndVerify(userContext, loan, 14);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 15.0, 0.0, 0.0);

        removeAccountFee(loan);

        LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 15.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 0.0, 0.0, 0.0);
        LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 0.0, 0.0, 0.0);

    }

    @Ignore @Test
    public void testRedoLoanRemovePeriodicRateFeeAfterRepayment() throws Exception {
        try {
            LoanBO loan = redoLoanWithMondayMeetingAndVerify(userContext, 14, createFeeViewsWithPeriodicRateFee(5.0));

            disburseLoanAndVerify(userContext, loan, 14);

            LoanTestUtils.assertInstallmentDetails(loan, 1, 50.9, 0.1, 15.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 2, 50.9, 0.1, 15.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 3, 50.9, 0.1, 15.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 4, 50.9, 0.1, 15.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 5, 50.9, 0.1, 15.0, 0.0, 0.0);
            LoanTestUtils.assertInstallmentDetails(loan, 6, 45.5, 0.5, 15.0, 0.0, 0.0);

            applyAndVerifyPayment(userContext, loan, 7, new Money(getCurrency(), "50"));

            // Expect AccountException
            removeAccountFee(loan);
            Assert.fail("Expected AccountException !!");
        } catch (AccountException e) {
        }
    }

    private LoanOfferingBO createLoanOffering(UserContext userContext, MeetingBO meeting, Date loanProductStartDate)
            throws ProductDefinitionException {
        PrdApplicableMasterEntity prdApplicableMaster = new PrdApplicableMasterEntity(ApplicableTo.GROUPS);
        ProductCategoryBO productCategory = TestObjectFactory.getLoanPrdCategory();
        GracePeriodTypeEntity gracePeriodType = new GracePeriodTypeEntity(GraceType.NONE);
        InterestTypesEntity interestType = new InterestTypesEntity(InterestType.FLAT);
        GLCodeEntity glCodePrincipal = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.LOANS_TO_CLIENTS);
        GLCodeEntity glCodeInterest = (GLCodeEntity) StaticHibernateUtil.getSessionTL().get(GLCodeEntity.class,
                TestGeneralLedgerCode.INTEREST_ON_LOANS);

        boolean interestDeductedAtDisbursement = false;
        boolean principalDueInLastInstallment = false;
        Money loanAmount = new Money(getCurrency(), "300");
        Double interestRate = new Double(1.2);
        Short installments = new Short((short) 6);
        short gracePeriodDuration = (short) 0;
        LoanOfferingBO loanOffering = LoanOfferingTestUtils.createInstanceForTest(userContext, "TestLoanOffering",
                "TLO", productCategory, prdApplicableMaster, loanProductStartDate, null, null, gracePeriodType,
                gracePeriodDuration, interestType, loanAmount, loanAmount, loanAmount, interestRate, interestRate, interestRate,
                installments, installments, installments, true, interestDeductedAtDisbursement,
                principalDueInLastInstallment, new ArrayList<FundBO>(), new ArrayList<FeeBO>(), meeting,
                glCodePrincipal, glCodeInterest);

        PrdStatusEntity prdStatus = new TestObjectPersistence().retrievePrdStatus(PrdStatus.LOAN_ACTIVE);
        LoanOfferingTestUtils.setStatus(loanOffering, prdStatus);
        LoanOfferingTestUtils.setGracePeriodType(loanOffering, gracePeriodType, gracePeriodDuration);
        loanOffering.save();

        return loanOffering;
    }

    private LoanBO redoLoanAccount(GroupBO group, LoanOfferingBO loanOffering, MeetingBO meeting, List<AccountFeesEntity> feeDtos) throws AccountException {
        Short numberOfInstallments = Short.valueOf("6");
        List<Date> meetingDates = TestObjectFactory.getMeetingDates(group.getOfficeId(), meeting, numberOfInstallments);
        loanBO = LoanBO.redoLoan(TestUtils.makeUser(), loanOffering, group, AccountState.LOAN_APPROVED, TestUtils
                .createMoney("300.0"), numberOfInstallments, meetingDates.get(0), false, 1.2, (short) 0, null,
                feeDtos, DOUBLE_ZERO, DOUBLE_ZERO, SHORT_ZERO, SHORT_ZERO, false, null);
        ((LoanBO) loanBO).save();
        new TestObjectPersistence().persist(loanBO);
        return (LoanBO) loanBO;
    }

    private void disburseLoan(UserContext userContext, LoanBO loan, Date loanDisbursalDate) throws AccountException,
            PersistenceException {
        PersonnelBO personnel = legacyPersonnelDao.getPersonnel(userContext.getId());
        loan.disburseLoan(null, loanDisbursalDate, Short.valueOf("1"), personnel, null, Short.valueOf("1"));
        new TestObjectPersistence().persist(loan);
    }

    private void applyPaymentForLoan(UserContext userContext, LoanBO loan, Date paymentDate, Money money) {
        loan.setUserContext(userContext);

        PersonnelBO loggedInUser = legacyPersonnelDao.findPersonnelById(userContext.getId());

        List<AccountActionDateEntity> accntActionDates = new ArrayList<AccountActionDateEntity>();
        accntActionDates.addAll(loan.getAccountActionDates());

        PaymentData paymentData = loan.createPaymentData(money, paymentDate, null, null, Short.valueOf("1"), loggedInUser);
        IntegrationTestObjectMother.applyAccountPayment(loan, paymentData);
        new TestObjectPersistence().persist(loan);
    }

    private Date createPreviousDate(int numberOfDays) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -numberOfDays);
        Date pastDate = DateUtils.getDateWithoutTimeStamp(calendar.getTime());
        return pastDate;
    }

    private LoanBO redoLoanWithMondayMeeting(UserContext userContext, Date disbursementDate, List<AccountFeesEntity> feeDtos)
            throws Exception {

        OfficeBO office = TestObjectFactory.getOffice(TestObjectFactory.SAMPLE_BRANCH_OFFICE);

        meeting = new MeetingBuilder().weekly().occuringOnA(WeekDay.MONDAY).startingFrom(disbursementDate).build();
        // IntegrationTestObjectMother.saveMeeting(meeting);

        center = new CenterBuilder().with(office).withLoanOfficer(null).with(meeting).build();
        // IntegrationTestObjectMother.saveCustomer(center);

        GroupTemplate groupTemplate = GroupTemplateImpl.createNonUniqueGroupTemplate(center.getCustomerId());

        GroupBO group = TestObjectFactory.createInstanceForTest(userContext, groupTemplate, (CenterBO) center,
                disbursementDate);
        new GroupPersistence().saveGroup(group);

        // group = createGroup(userContext, groupTemplate, disbursementDate);

        LoanOfferingBO loanOffering = createLoanOffering(userContext, meeting, disbursementDate);
        return redoLoanAccount(group, loanOffering, meeting, feeDtos);
    }

    private LoanBO redoLoanWithMeetingToday(UserContext userContext, Date disbursementDate, List<AccountFeesEntity> feeDtos)
            throws Exception {
        OfficeBO office = TestObjectFactory.getOffice(TestObjectFactory.SAMPLE_BRANCH_OFFICE);
        meeting = new MeetingBO(MeetingTemplateImpl.createWeeklyMeetingTemplateStartingFrom(disbursementDate));

        CenterTemplate centerTemplate = new CenterTemplateImpl(meeting, office.getOfficeId());
        center = getCenterPersistence().createCenter(userContext, centerTemplate);

        GroupTemplate groupTemplate = GroupTemplateImpl.createNonUniqueGroupTemplate(center.getCustomerId());

        group = createGroup(userContext, groupTemplate, disbursementDate);

        LoanOfferingBO loanOffering = createLoanOffering(userContext, meeting, disbursementDate);
        return redoLoanAccount((GroupBO)group, loanOffering, meeting, feeDtos);
    }

    protected LoanBO redoLoanWithMondayMeetingAndVerify(UserContext userContext, int numberOfDaysInPast,
            List<AccountFeesEntity> feeDtos) throws Exception {
        LoanBO loan = redoLoanWithMondayMeeting(userContext, createPreviousDate(numberOfDaysInPast), feeDtos);
        Assert.assertEquals(new Money(getCurrency(), "300.0"), loan.getLoanAmount());
        return loan;
    }

    protected LoanBO redoLoanWithMeetingTodayAndVerify(UserContext userContext, int numberOfDaysInPast,
            List<AccountFeesEntity> feeDtos) throws Exception {
        LoanBO loan = redoLoanWithMeetingToday(userContext, createPreviousDate(numberOfDaysInPast), feeDtos);
        Assert.assertEquals(new Money(getCurrency(), "300.0"), loan.getLoanAmount());
        return loan;
    }

    // inlined the persistence method to set the activation/creation date of
    // customer on or before disbursement date
    private GroupBO createGroup(UserContext userContext, GroupTemplate groupTemplate, final Date disbursementDate)
            throws PersistenceException, ValidationException, CustomerException {
        CenterBO center = null;
        if (groupTemplate.getParentCenterId() != null) {
            center = getGroupPersistence().getCenterPersistence().getCenter(groupTemplate.getParentCenterId());
            if (center == null) {
                throw new ValidationException(GroupConstants.PARENT_OFFICE_ID);
            }
        }
        GroupBO group = TestObjectFactory.createInstanceForTest(userContext, groupTemplate, center, disbursementDate);
        new GroupPersistence().saveGroup(group);
        return group;
    }

    protected LoanBO redoLoanAndVerify(UserContext userContext, Date disbursementDate) throws Exception {
        LoanBO loan = redoLoanWithMondayMeeting(userContext, disbursementDate, new ArrayList<AccountFeesEntity>());
        Assert.assertEquals(new Money(getCurrency(), "300.0"), loan.getLoanAmount());
        return loan;
    }

    private void disburseLoanAndVerify(UserContext userContext, LoanBO loan, int numberofDaysInPast) throws Exception {
        Date disbursementDate = createPreviousDate(numberofDaysInPast);
        disburseLoan(userContext, loan, disbursementDate);
        Assert.assertEquals(disbursementDate.getTime(), loan.getDisbursementDate().getTime());

        // Validate disbursement information
        Iterator<AccountPaymentEntity> payments = loan.getAccountPayments().iterator();
        AccountPaymentEntity disbursement = payments.next();
        Iterator<AccountTrxnEntity> trxns = disbursement.getAccountTrxns().iterator();
        AccountTrxnEntity trxn;
        do {
            trxn = trxns.next();
            if (trxn.getAccountAction() == AccountActionTypes.DISBURSAL) {
                break;
            }
        } while (trxns.hasNext());
        Assert.assertEquals(AccountActionTypes.DISBURSAL, trxn.getAccountAction());
        Assert.assertEquals(loan.getLoanAmount(), trxn.getAmount());
        Assert.assertEquals(disbursementDate.getTime(), trxn.getActionDate().getTime());

    }

    private void verifyPayment(LoanBO loan, Date paymentDate, Money paymentAmount) throws Exception {

        Iterator<AccountPaymentEntity> payments = loan.getAccountPayments().iterator();
        AccountPaymentEntity payment;
        Iterator<AccountTrxnEntity> trxns = null;
        AccountTrxnEntity trxn = null;
        boolean foundThePayment = false;
        do {
            payment = payments.next();
            trxns = payment.getAccountTrxns().iterator();
            do {
                trxn = trxns.next();
                if (trxn.getAccountAction() == AccountActionTypes.LOAN_REPAYMENT
                        && trxn.getAmount().equals(paymentAmount)) {
                    foundThePayment = true;
                    Assert.assertEquals(paymentDate, trxn.getActionDate());
                    break;
                }
            } while (trxns.hasNext());
        } while (payments.hasNext());
        Assert.assertTrue("Couldnt find a LOAN_REPAYMENT", foundThePayment);
    }

    protected void applyAndVerifyPayment(UserContext userContext, LoanBO loan, int numberOfDaysInPast, Money amount)
            throws Exception {

        Date paymentDate = createPreviousDate(numberOfDaysInPast);
        applyPaymentForLoan(userContext, loan, paymentDate, amount);
        verifyPayment(loan, paymentDate, amount);

    }

    protected void removeAccountFee(LoanBO loan) throws Exception {
        for (AccountFeesEntity accountFeesEntity : loan.getAccountFees()) {
            loan.removeFeesAssociatedWithUpcomingAndAllKnownFutureInstallments(accountFeesEntity.getFees().getFeeId(), Short.valueOf("1"));
        }
    }

    private FeeBO createOneTimeAmountFee(double amount) {
        fee = TestObjectFactory.createOneTimeAmountFee("oneTimeAmountFee", FeeCategory.GROUP, String.valueOf(amount),
                FeePayment.TIME_OF_FIRSTLOANREPAYMENT);
        return fee;
    }

    private List<AccountFeesEntity> createFeeViewsWithOneTimeAmountFee(double amount) {
        List<AccountFeesEntity> feeDtos = new ArrayList<AccountFeesEntity>();

        FeeBO upFrontAmountFee = createOneTimeAmountFee(amount);
        feeDtos.add(new AccountFeesEntity(null, upFrontAmountFee, Double.valueOf(amount)));
        return feeDtos;
    }

    private FeeBO createPeriodicAmountFee(double amount) {
        fee = TestObjectFactory.createPeriodicAmountFee("PeriodicAmountFee", FeeCategory.GROUP, String.valueOf(amount),
                RecurrenceType.WEEKLY, (short) 1);
        return fee;
    }

    private List<AccountFeesEntity> createFeeViewsWithPeriodicAmountFee(double amount) {
        List<AccountFeesEntity> feeDtos = new ArrayList<AccountFeesEntity>();
        FeeBO upFrontAmountFee = createPeriodicAmountFee(amount);
        feeDtos.add(new AccountFeesEntity(null, upFrontAmountFee, Double.valueOf(amount)));
        return feeDtos;
    }

    private FeeBO createPeriodicRateFee(double rate) {
        fee = TestObjectFactory.createPeriodicRateFee("PeriodicRateFee", FeeCategory.GROUP, rate, FeeFormula.AMOUNT,
                RecurrenceType.WEEKLY, (short) 1);
        return fee;
    }

    private List<AccountFeesEntity> createFeeViewsWithPeriodicRateFee(double rate) {
        List<AccountFeesEntity> feeDtos = new ArrayList<AccountFeesEntity>();
        FeeBO periodicRateFee = createPeriodicRateFee(rate);
        feeDtos.add(new AccountFeesEntity(null, periodicRateFee, Double.valueOf(rate)));
        return feeDtos;
    }

    private void applyCharge(LoanBO loan, short chargeId, double chargeAmount) throws Exception {
        loan.applyCharge(chargeId, chargeAmount);
        new TestObjectPersistence().persist(loan);
    }

    private CenterPersistence getCenterPersistence() {
        return centerPersistence;
    }

    private GroupPersistence getGroupPersistence() {
        return groupPersistence;
    }
}