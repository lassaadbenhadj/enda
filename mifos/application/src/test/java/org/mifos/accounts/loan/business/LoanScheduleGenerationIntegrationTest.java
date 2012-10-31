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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mifos.framework.util.helpers.IntegrationTestObjectMother.sampleBranchOffice;
import static org.mifos.framework.util.helpers.IntegrationTestObjectMother.testUser;
import static org.mifos.framework.util.helpers.TestObjectFactory.EVERY_WEEK;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.fees.business.AmountFeeBO;
import org.mifos.accounts.fees.business.FeeBO;
import org.mifos.accounts.fees.business.FeeDto;
import org.mifos.accounts.fees.util.helpers.FeeCategory;
import org.mifos.accounts.fees.util.helpers.FeePayment;
import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.application.holiday.util.helpers.RepaymentRuleTypes;
import org.mifos.application.master.business.MifosCurrency;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.meeting.util.helpers.RankOfDay;
import org.mifos.application.meeting.util.helpers.RecurrenceType;
import org.mifos.application.meeting.util.helpers.WeekDay;
import org.mifos.config.FiscalCalendarRules;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.domain.builders.CenterBuilder;
import org.mifos.domain.builders.FeeBuilder;
import org.mifos.domain.builders.GroupBuilder;
import org.mifos.domain.builders.LoanProductBuilder;
import org.mifos.domain.builders.MeetingBuilder;
import org.mifos.dto.domain.HolidayDetails;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.TestUtils;
import org.mifos.framework.util.DateTimeService;
import org.mifos.framework.util.StandardTestingService;
import org.mifos.framework.util.helpers.IntegrationTestObjectMother;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.TestObjectFactory;
import org.mifos.service.test.TestMode;
import org.mifos.test.framework.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * FIXME - completely rewrite/fix these tests
 * These tests validate new schedule-generating code for loan repayments
 */
public class LoanScheduleGenerationIntegrationTest extends MifosIntegrationTestCase {

    //Things you need to set up before you can create a loan
    private MeetingBO meeting;
    private OfficeBO office;
    private CenterBO center;
    private GroupBO group;
    private LoanOfferingBO loanOffering;
    private FiscalCalendarRules fiscalCalendarRules = new FiscalCalendarRules();
    private List<WeekDay> savedWorkingDays = fiscalCalendarRules.getWorkingDays();

    private static MifosCurrency oldDefaultCurrency;

//    @Autowired
//    private CustomerDao customerDao;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeClass
    public static void initialiseHibernateUtil() {

        oldDefaultCurrency = Money.getDefaultCurrency();
        Money.setDefaultCurrency(TestUtils.RUPEE);
        new StandardTestingService().setTestMode(TestMode.INTEGRATION);
    }

    @AfterClass
    public static void resetCurrency() {
        Money.setDefaultCurrency(oldDefaultCurrency);
    }

    @Before
    public void cleanDatabaseTables() {
        databaseCleaner.clean();
        fiscalCalendarRules.setWorkingDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        office = sampleBranchOffice();
    }

    @After
    public void cleanDatabaseTablesAfterTest() {
        // NOTE: - only added to stop older integration tests failing due to brittleness
        databaseCleaner.clean();
        fiscalCalendarRules.setWorkingDays(weekDaysToPropertyString(savedWorkingDays));
        new DateTimeService().resetToCurrentSystemDateTime();
    }

    @Test
    public void testNewWeeklyGroupLoanNoFeesNoHoliday() throws Exception {
        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setCurrentDateTime(new LocalDate(2010, 2, 25).toDateTimeAtStartOfDay());
        LoanBO loan = createWeeklyGroupLoanWithDisbursementDateWithOccurrences(date(2010, 10, 15), 9); //Meets on Fridays

        /*
         * Since disbursal is on a meeting day, the first installment date is one week from disbursement date.
         * All asserted dates are on Fridays
         */
        validateDates(loan, date(2010, 10, 22), date(2010, 10, 29), date(2010, 11, 5),
                            date(2010, 11, 12), date(2010, 11, 19), date(2010, 11, 26),
                            date(2010, 12, 3),  date(2010, 12, 10), date(2010, 12, 17));
        dateTimeService.resetToCurrentSystemDateTime();
    }

    @Ignore
    @Test
    public void testNewWeeklyGroupLoanOnePeriodicFeeNoHoliday() throws Exception {

        new DateTimeService().setCurrentDateTimeFixed(date(2010, 10, 13)); //Wednesday before loan start date

        MeetingBuilder feeMeetingBuilder = new MeetingBuilder().every(1).weekly().withStartDate(date(2010, 10, 15));
        AmountFeeBO fee = new FeeBuilder().appliesToLoans()
                                          .with(feeMeetingBuilder)
                                          .withFeeAmount("14.0")
                                          .withName("Periodic Loan Fee")
                                          .with(sampleBranchOffice())
                                          .build();
        IntegrationTestObjectMother.saveFee(fee);

        FeeBO periodicFee = TestObjectFactory.createPeriodicAmountFee("Periodic Loan Fee", FeeCategory.LOAN, "14.0",
                RecurrenceType.WEEKLY, EVERY_WEEK);

        LoanBO loan = createWeeklyGroupLoanWithDisbursementDateWithOccurrences(date(2010, 10, 15), 9, periodicFee); //Meets on Fridays

        /*
         * Since disbursal is on a meeting day, the first installment date is one week from disbursement date.
         * All asserted dates are on Fridays
         */
        validateDates(loan, date(2010, 10, 22), date(2010, 10, 29), date(2010, 11, 5),
                            date(2010, 11, 12), date(2010, 11, 19), date(2010, 11, 26),
                            date(2010, 12, 3),  date(2010, 12, 10), date(2010, 12, 17));
        validateOnePeriodicFee(loan, "Periodic Loan Fee", 14.0, 14.0, 14.0, 14.0, 14.0, 14.0, 14.0, 14.0, 14.0);
    }

    @Test
    public void testNewWeeklyGroupLoanWithUpfrontFeeNoHoliday() throws Exception {

        new DateTimeService().setCurrentDateTimeFixed(date(2010, 10, 13)); //Wednesday before loan start date

        AmountFeeBO fee = new FeeBuilder().appliesToLoans()
                                          .oneTime()
                                          .withFeePayment(FeePayment.UPFRONT)
                                          .withFeeAmount("14.0")
                                          .withName("Onetime Loan Fee Due on Disbursement")
                                          .with(sampleBranchOffice())
                                          .build();
        IntegrationTestObjectMother.saveFee(fee);

        LoanBO loan = createWeeklyGroupLoanWithDisbursementDateWithOccurrences(date(2010, 10, 15), 9, fee); //Meets on Fridays
        /*
         * Since disbursal is on a meeting day, the first installment date is one week from disbursement date.
         * All asserted dates are on Fridays
         */
        validateDates(loan, date(2010, 10, 22), date(2010, 10, 29), date(2010, 11, 5),
                            date(2010, 11, 12), date(2010, 11, 19), date(2010, 11, 26),
                            date(2010, 12, 3),  date(2010, 12, 10), date(2010, 12, 17));
        validateOneOneTimeFee(loan, "Onetime Loan Fee Due on Disbursement", 1, 14.0);
    }

    @Ignore
    @Test
    public void testNewWeeklyGroupLoanOnePeriodicFeeMoratorium() throws Exception {

        MeetingBuilder feeMeetingBuilder = new MeetingBuilder().every(1).weekly().withStartDate(date(2010, 10, 15));
        AmountFeeBO fee = new FeeBuilder().appliesToLoans()
                                          .with(feeMeetingBuilder)
                                          .withFeeAmount("14.0")
                                          .withName("Periodic Loan Fee")
                                          .with(sampleBranchOffice())
                                          .build();
        IntegrationTestObjectMother.saveFee(fee);

        // Moratorium starts Friday (when 1st payment due) thru the Thursday before the 2nd payment
        buildAndPersistMoratorium(date(2010, 10, 22), date(2010, 10, 28));

        LoanBO loan = createWeeklyGroupLoanWithDisbursementDateWithOccurrences(date(2010, 10, 15), 6, fee); //Meets on Fridays
        /*
         * Since disbursal is on a meeting day, the first installment date is one week from disbursement date.
         * All asserted dates are on Fridays
         */
        validateDates(loan, date(2010, 10, 29), date(2010, 11, 5), date(2010, 11, 12),
                            date(2010, 11, 19), date(2010, 11, 26), date(2010, 12, 3));
        validateOnePeriodicFee(loan, "Periodic Loan Fee", 14.0, 14.0, 14.0, 14.0, 14.0, 14.0);
    }

    @Ignore
    @Test
    public void testNewWeeklyGroupLoanOnePeriodicFeeMoratoriumHitsThirdRepayment() throws Exception {

        MeetingBuilder feeMeetingBuilder = new MeetingBuilder().every(1).weekly().withStartDate(date(2010, 10, 15));
        AmountFeeBO fee = new FeeBuilder().appliesToLoans()
                                          .with(feeMeetingBuilder)
                                          .withFeeAmount("14.0")
                                          .withName("Periodic Loan Fee")
                                          .with(sampleBranchOffice())
                                          .build();
        IntegrationTestObjectMother.saveFee(fee);

        // Moratorium starts Friday (when 1st payment due) thru the Thursday before the 2nd payment
        buildAndPersistMoratorium(date(2010, 11, 5), date(2010, 11, 11));

        LoanBO loan = createWeeklyGroupLoanWithDisbursementDateWithOccurrences(date(2010, 10, 15), 6, fee); //Meets on Fridays
        /*
         * Since disbursal is on a meeting day, the first installment date is one week from disbursement date.
         * Third and later payments pushed out one week.
         */
        validateDates(loan, date(2010, 10, 22), date(2010, 10, 29), date(2010, 11, 12),
                            date(2010, 11, 19), date(2010, 11, 26), date(2010, 12, 3));
        validateOnePeriodicFee(loan, "Periodic Loan Fee", 14.0, 14.0, 14.0, 14.0, 14.0, 14.0);
    }

    @Ignore
    @Test
    public void testNewWeeklyGroupLoanNoFeesSpansMoratorium() throws Exception {

        // Moratorium starts Friday (when 1st payment due) thru the Thursday before the 2nd payment
        buildAndPersistMoratorium(date(2010, 10, 22), date(2010, 10, 28));

        LoanBO loan = createWeeklyGroupLoanWithDisbursementDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * the first installment date is one week later. Since the first installment date falls in the moratorium,
         * all installments should get pushed out one week.
         *
         */
        validateDates(loan, date(2010, 10, 29), date(2010, 11, 5), date(2010, 11, 12),
                date(2010, 11, 19), date(2010, 11, 26), date(2010, 12, 3));
    }

    /****************************************************
     * Generate loan schedules for monthly schedules meeting on a day of the month.
     *****************************************************/

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthNoHoliday() throws Exception {

        LoanBO loan = createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly.
         */
        validateDates(loan, date(2010, 11, 15), date(2010, 12, 15), date(2011, 1, 17),
                            date(2011, 2, 15),  date(2011, 3, 15),  date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthSecondInstallmentSpansMoratorium() throws Exception {

        //setup meeting 15th of every month starting 10/15/2010, with moratorium on the second meeting date.
        buildAndPersistMoratorium(date(2010, 12, 15), date(2010, 12, 15));
        LoanBO loan = createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. Moratorium pushes December and subsequent dates out one month.
         *
         */
        validateDates(loan, date(2010, 11, 15), date(2011, 1, 17), date(2011, 2, 15),
                            date(2011, 3, 15),  date(2011, 4, 15), date(2011, 5, 16));

    }

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthSecondAndThirdInstallmentsSpanMoratorium() throws Exception {

        //setup
        buildAndPersistMoratorium(date(2010, 12, 10), date(2011, 1, 31));
        LoanBO loan = createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. Second and subsequent dates are pushed out two months.
         *
         */
        validateDates(loan, date(2010, 11, 15), date(2011, 2, 15), date(2011, 3, 15),
                            date(2011, 4, 15),  date(2011, 5, 16), date(2011, 6, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthSecondInstallmentInNextMeetingHoliday() throws Exception {

        //Setup meeting and holiday spanning third meeting date.
        buildAndPersistHoliday(date(2010, 12, 10), date(2010, 12, 20), RepaymentRuleTypes.NEXT_MEETING_OR_REPAYMENT);
        LoanBO loan = createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the next-meeting holiday so should
         * shift to the next meeting date, Monday January 17th 2011, the first working day on or after
         * Saturday, January 15th
         *
         */
        validateDates(loan, date(2010, 11, 15), date(2011, 1, 17), date(2011, 1, 17),
                            date(2011, 2, 15),  date(2011, 3, 15), date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthSecondInstallmentInNextMeetingHolidayAndThirdInstallmentInMoratorium()
                    throws Exception {

        //Setup meeting and holiday spanning third meeting date and moratorium in January
        buildAndPersistHoliday(date(2010, 12, 15), date(2010, 12, 15), RepaymentRuleTypes.NEXT_MEETING_OR_REPAYMENT);
        buildAndPersistMoratorium(date(2011, 1, 1), date(2011, 1, 31));
        LoanBO loan = createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the next-meeting holiday so should
         * shift to the next meeting date, Monday January 17th 2011. Since that date falls in the moratorium, it and
         * all subsequent dates are pushed out one month. Note there are still double payments due after the
         * moratorium.
         */
        validateDates(loan, date(2010, 11, 15), date(2011, 2, 15), date(2011, 2, 15),
                            date(2011,  3, 15), date(2011, 4, 15), date(2011, 5, 16));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanGroupOnDayOfMonthSecondInstallmentInMoratoriumPushedIntoNextMeetingHoliday()
                    throws Exception {

        //Setup meeting and holiday spanning third meeting date and moratorium in January
        buildAndPersistMoratorium(date(2010, 12, 15), date(2010, 12, 31));
        buildAndPersistHoliday(date(2011, 1, 10), date(2011, 1, 20), RepaymentRuleTypes.NEXT_MEETING_OR_REPAYMENT);
        LoanBO loan = createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the next-meeting holiday so should
         * shift to the next meeting date, Monday January 17th 2011. Since that date falls in the moratorium, it and
         * all subsequent dates are pushed out one month. Note there are still double payments due after the
         * moratorium.
         */
        validateDates(loan,
                      date(2010, 11, 15), date(2011, 2, 15), date(2011, 2, 15),
                      date(2011,  3, 15), date(2011, 4, 15), date(2011, 5, 16));

    }

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthSecondInstallmentInNextWorkingDayHoliday() throws Exception {

        //Setup meeting and holiday spanning third meeting date.
        buildAndPersistHoliday(date(2010, 12, 15) /*wed*/, date(2010, 12, 15), RepaymentRuleTypes.NEXT_WORKING_DAY);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the one-day, next-working-day holiday so should
         * shift to Thursday, 2010/12/16, the next working day.
         *
         */
        validateDates(createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6),
                      date(2010, 11, 15), date(2010, 12, 16) /*Thursday*/, date(2011, 1, 17) /*Monday*/,
                      date(2011, 2, 15), date(2011,3,15), date(2011,4,15));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanGroupOnDayOfMonthSecondInstallmentInNexWorkingDayHolidayAndNextWorkingDayIsInMoratorium()
                    throws Exception {

        //Setup meeting and holiday spanning third meeting date and moratorium in January
        buildAndPersistHoliday(date(2010, 12, 15), date(2010, 12, 15), RepaymentRuleTypes.NEXT_WORKING_DAY);
        buildAndPersistMoratorium(date(2010, 12, 16), date(2010, 12, 31));

        /*
         * Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the next-working-day holiday so should
         * shift to the next working day -- Thursday, December 16. Since that date falls in the moratorium, it should
         * shift to the next working day after the moratorium -- Monday, January 3. But since no regularly
         * scheduled meeting date falls in the moratorium, no other dates get adjusted.
         */
        validateDates(createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6),
                      date(2010, 11, 15), date(2011, 1, 3), date(2011, 1, 17),
                      date(2011,  2, 15), date(2011, 3, 15), date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfMonthSecondInstallmentInMoratoriumPushedIntoNextWorkingDayHoliday()
                    throws Exception {

        //Setup meeting and holiday spanning third meeting date and moratorium in January
        buildAndPersistMoratorium(date(2010, 12, 15), date(2010, 12, 31));
        buildAndPersistHoliday   (date(2011, 1, 10),  date(2011, 1, 20), RepaymentRuleTypes.NEXT_WORKING_DAY);

        /*
         * Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the moratorium so it and all following dates
         * shift one month. Since that date falls in the next-working-day holiday, it shifts to
         * Friday, January 21.
         */
        validateDates(createMonthlyOnDateGroupLoanWithStartDateWithOccurrences(date(2010, 10, 15), 6),
                            date(2010, 11, 15), date(2011, 1, 21), date(2011, 2, 15),
                            date(2011,  3, 15), date(2011, 4, 15), date(2011, 5, 16));
    }

    /****************************************************
     * Generate loan schedules for monthly schedules meeting on a day in a week of the month.
     *****************************************************/
    @Ignore
    @Test
    public void testNewMonthlyGroupLoanOnDayOfWeekNoHoliday() throws Exception {

        /*
         * Note start date IS the third Friday in October.
         * Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. The dates here are successive 3rd Fridays of the month.
         */
        validateDates(createMonthlyGroupLoanOnDayOfWeek(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY, 6),
                date(2010, 11, 19),  date(2010, 12, 17),  date(2011, 1, 21),
                date(2011, 2, 18),  date(2011, 3, 18),  date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentSpansMoratorium() throws Exception {

        //setup meeting third Friday of every month starting 10/15/2010, with moratorium spanning the second meeting date.
        buildAndPersistMoratorium(date(2010, 12, 10), date(2010, 12, 20));

        /*
         * Since start date is a meeting date, disbursal is on that day and
         * installment dates on the third Friday of every month. From December on, dates are pushed
         * out one month by the moratorium.
         *
         */
        validateDates(createMonthlyGroupLoanOnDayOfWeek(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY, 6),
                            date(2010, 11, 19),  date(2011, 1, 21), date(2011, 2, 18),
                            date(2011, 3, 18), date(2011, 4, 15), date(2011, 5, 20));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondAndThirdInstallmentsSpanMoratorium() throws Exception {

        //setup
        DateTime startDate = date(2010, 10, 15);
        buildAndPersistMoratorium(date(2010, 12, 10), date(2011, 1, 31));
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);

        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly on the third Friday of the month. Second and subsequent dates are pushed out
         *  two months.
         *
         */
        validateDates(loan, date(2010, 11, 19), date(2011, 2, 18), date(2011, 3, 18),
                            date(2011, 4, 15), date(2011, 5, 20), date(2011, 6, 17));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentInNextMeetingHoliday() throws Exception {

        //Setup meeting and holiday spanning December's meeting date.
        DateTime startDate = date(2010, 10, 15);
        buildAndPersistHoliday(date(2010, 12, 10), date(2010, 12, 20), RepaymentRuleTypes.NEXT_MEETING_OR_REPAYMENT);
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);


        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the next-meeting holiday so should
         * shift to the next meeting date, January 21st. All other dates remain unchanged.
         */
        validateDates(loan, date(2010, 11, 19),  date(2011, 1, 21),  date(2011, 1, 21),
                date(2011, 2, 18),  date(2011, 3, 18),  date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentInNextMeetingHolidayAndThirdInstallmentInMoratorium()
                    throws Exception {

        //Setup meeting and holiday December's date and moratorium in January
        DateTime startDate = date(2010, 10, 15);
        buildAndPersistHoliday(date(2010, 12, 17), date(2010, 12, 17), RepaymentRuleTypes.NEXT_MEETING_OR_REPAYMENT);
        buildAndPersistMoratorium(date(2011, 1, 1), date(2011, 1, 31));
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);


        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 15th falls in the next-meeting holiday so should
         * shift to the next meeting date, January 21, 2011. Since that date falls in the moratorium, it and
         * all subsequent dates are pushed out one month. Note there are still double payments due after the
         * moratorium.
         */
        validateDates(loan, date(2010, 11, 19), date(2011, 2, 18), date(2011, 2, 18),
                            date(2011,  3, 18), date(2011, 4, 15), date(2011, 5, 20));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentInMoratoriumPushedIntoNextMeetingHoliday()
                    throws Exception {

        //Setup meeting and holiday. Moratorium encloses December's date and next-meeting holiday encloses January's
        DateTime startDate = date(2010, 10, 15);
        buildAndPersistMoratorium(date(2010, 12, 15), date(2010, 12, 31));
        buildAndPersistHoliday(date(2011, 1, 10), date(2011, 1, 25), RepaymentRuleTypes.NEXT_MEETING_OR_REPAYMENT);
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);


        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 17th falls in the moratorium so second and subsequent
         * dates shift one month. Since the second installment now is in the next-meeting holiday it
         * shifts one more month.
         */
        validateDates(loan, date(2010, 11, 19), date(2011, 2, 18), date(2011, 2, 18),
                            date(2011,  3, 18), date(2011, 4, 15), date(2011, 5, 20));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentInNextWorkingDayHoliday() throws Exception {

        //Setup meeting and holiday enclosing December's meeting date.
        DateTime startDate = date(2010, 10, 15); //Friday
        buildAndPersistHoliday(date(2010, 12, 15) /*wed*/, date(2010, 12, 17), RepaymentRuleTypes.NEXT_WORKING_DAY);
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);


        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 17th falls in the  next-working-day holiday so should
         * shift to Monday, 12/20, the next working day.
         *
         */
        validateDates(loan, date(2010, 11, 19),  date(2010, 12, 20),  date(2011, 1, 21),
                date(2011, 2, 18),  date(2011, 3, 18),  date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentInNexWorkingDayHolidayAndNextWorkingDayIsInMoratorium()
                    throws Exception {

        //Setup meeting and holiday enclosing December's date and moratorium enclosing the next working day
        DateTime startDate = date(2010, 10, 15);
        buildAndPersistHoliday(date(2010, 12, 17), date(2010, 12, 17), RepaymentRuleTypes.NEXT_WORKING_DAY);
        buildAndPersistMoratorium(date(2010, 12, 18), date(2010, 12, 31));
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);


        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 17th falls in the next-working-day holiday so should
         * shift to the next working day -- Monday, December 20. Since that date falls in the moratorium, it should
         * shift to the next working day after the moratorium -- Monday, January 3. Since no regularly
         * scheduled meeting date falls in the moratorium, no other dates get adjusted.
         */
        validateDates(loan, date(2010, 11, 19), date(2011, 1, 3), date(2011, 1, 21),
                            date(2011,  2, 18), date(2011, 3, 18), date(2011, 4, 15));

    }

    @Ignore
    @Test
    public void testNewMonthlyLoanOnDayOfWeekSecondInstallmentInMoratoriumPushedIntoNextWorkingDayHoliday()
                    throws Exception {

        //Setup meeting, moratorium enclosing December's date, holiday enclosing January's
        DateTime startDate = date(2010, 10, 15);
        buildAndPersistMoratorium(date(2010, 12, 15), date(2010, 12, 31));
        buildAndPersistHoliday(date(2011, 1, 10), date(2011, 1, 21), RepaymentRuleTypes.NEXT_WORKING_DAY);
        setupMonthlyScheduleOnDayOfWeekStartingDate(date(2010, 10, 15), RankOfDay.THIRD, WeekDay.FRIDAY);


        //Make loan and schedule
        LoanBO loan = createStandardLoan(startDate, 6, group);

        /*
         * Validate. Since start date is a meeting date, disbursal is on that day and
         * installment dates follow monthly. December 17th falls in the moratorium so it and all following dates
         * shift one month. Since January's meeting date falls in the next-working-day holiday, it shifts to
         * the next working date, Monday, January 24.
         */
        validateDates(loan, date(2010, 11, 19), date(2011, 1, 24), date(2011, 2, 18),
                            date(2011,  3, 18), date(2011, 4, 15), date(2011, 5, 20));
    }

    /****************************************
     * Helper methods
     ****************************************/

    private LoanBO createWeeklyGroupLoanWithDisbursementDateWithOccurrences (DateTime startDate, int occurrences, FeeBO...fees)
                throws Exception {
        setupWeeklyScheduleStartingOn(startDate);
        return createStandardLoan(startDate, occurrences, group, fees);
    }

    private void setupWeeklyScheduleStartingOn(DateTime startDate) {
        meeting = new MeetingBuilder().weekly().withStartDate(startDate).build();
        IntegrationTestObjectMother.saveMeeting(meeting);

        setupOfficeAndCenterAndGroupAndLoanOfferingForMeeting(meeting);
    }

    private LoanBO createMonthlyOnDateGroupLoanWithStartDateWithOccurrences (DateTime startDate, int occurrences)
                throws Exception {
        setupMonthlyScheduleOnDateStartingDate(startDate);
        return createStandardLoan(startDate, occurrences, group);
    }

    private void setupMonthlyScheduleOnDateStartingDate(DateTime startDate) {
        meeting = new MeetingBuilder().monthly().onDayOfMonth(15).withStartDate(startDate).build();
        setupOfficeAndCenterAndGroupAndLoanOfferingForMeeting(meeting);
    }

    private LoanBO createMonthlyGroupLoanOnDayOfWeek (DateTime startDate, RankOfDay weekOfMonth, WeekDay dayOfWeek, int occurrences)
                throws Exception {
        setupMonthlyScheduleOnDayOfWeekStartingDate(startDate, weekOfMonth, dayOfWeek);
        return createStandardLoan(startDate, occurrences, group);
    }
    private void setupMonthlyScheduleOnDayOfWeekStartingDate(DateTime startDate, RankOfDay weekOfMonth, WeekDay dayOfWeek)
                throws Exception {
        meeting = new MeetingBuilder().monthly()
                                      .onWeek(weekOfMonth)
                                      .occuringOnA(dayOfWeek)
                                      .startingFrom(startDate.toDate())
                                      .build();
//        meeting = new MeetingBuilder().buildMonthlyFor(weekOfMonth, dayOfWeek);
        setupOfficeAndCenterAndGroupAndLoanOfferingForMeeting(meeting);
    }

    private void setupOfficeAndCenterAndGroupAndLoanOfferingForMeeting (MeetingBO meeting) {
        center = new CenterBuilder().with(meeting).with(office).withLoanOfficer(testUser()).build();
        group = new GroupBuilder().withParentCustomer(center).withOffice(office).withMeeting(meeting).build();
        loanOffering = new LoanProductBuilder().withMeeting(meeting).buildForIntegrationTests();

    }

    private void buildAndPersistHoliday (DateTime start, DateTime through, RepaymentRuleTypes rule) throws Exception {
        HolidayDetails holidayDetails = new HolidayDetails("testHoliday", start.toDate(), through.toDate(), rule.getValue());
        List<Short> officeIds = new LinkedList<Short>();
        officeIds.add((short)1);
        IntegrationTestObjectMother.createHoliday(holidayDetails, officeIds);
//        StaticHibernateUtil.flushAndClearSession();
//        StaticHibernateUtil.commitTransaction();
    }

    private void buildAndPersistMoratorium (DateTime start, DateTime through) throws Exception {
        buildAndPersistHoliday(start, through, RepaymentRuleTypes.REPAYMENT_MORATORIUM);
    }

    private void validateDates (LoanBO loan, DateTime... dates) {
        Assert.assertEquals(dates.length, loan.getAccountActionDates().size());
        Assert.assertEquals(dates.length, loan.getNoOfInstallments().intValue());
        for (short installmentId = 1; installmentId <= loan.getNoOfInstallments(); installmentId++) {
            Assert.assertEquals("Installment " + installmentId + "'s date is wrong.",
                                dates[installmentId-1].toDate(), loan.getAccountActionDate(installmentId).getActionDate());
        }
    }

    private DateTime date (int year, int month, int day) {
        return new DateTime().withDate(year, month, day).toDateMidnight().toDateTime();
    }

    private LoanBO createStandardLoan (DateTime startDate, int numberOfInstallments, CustomerBO customer, FeeBO...fees) throws Exception {
        List<FeeDto> feeDtos = new ArrayList<FeeDto>();
        for (FeeBO fee : fees) {
            FeeDto feeView = new FeeDto(TestUtils.makeUser(), fee);
            feeDtos.add(feeView);
        }
        return LoanBO
        .createLoan(TestUtils.makeUser(), loanOffering, customer, AccountState.LOAN_APPROVED, new Money(
                Money.getDefaultCurrency(), "300.0"), (short) numberOfInstallments, startDate.toDate(), false,
                loanOffering.getDefInterestRate(), (short) 0, null, feeDtos, null, 300.0, 300.0,
                loanOffering.getEligibleInstallmentSameForAllLoan().getMaxNoOfInstall(),
                loanOffering.getEligibleInstallmentSameForAllLoan().getMinNoOfInstall(),
                false, null);

    }

    private String weekDaysToPropertyString(List<WeekDay> weekDays) {
        String propertyString = "";
        Boolean first = true;
        for (WeekDay day : weekDays) {
            if (!first) {
                propertyString = propertyString + ",";
            } else {
                first = false;
            }
            propertyString = propertyString + day.toString().toUpperCase();
        }
        return propertyString;
    }

    private void validateOnePeriodicFee (LoanBO loan, String expectedPeriodicFeeName, double...expectedFees) {

        int installmentId = 1;
        for (AccountActionDateEntity accountActionDate : getActionDatesSortedByDate(loan)) {
            LoanScheduleEntity scheduleEntity= (LoanScheduleEntity) accountActionDate;
            assertThat("wrong number of fees for installment " + installmentId, scheduleEntity.getAccountFeesActionDetails().size(), is(1));
            assertThat(scheduleEntity.getAccountFeesActionDetailsSortedByFeeId().get(0).getFee().getFeeName(),
                    is(expectedPeriodicFeeName));
            assertThat(scheduleEntity.getAccountFeesActionDetailsSortedByFeeId().get(0).getFeeAmount().getAmountDoubleValue(),
                    is(expectedFees[scheduleEntity.getInstallmentId()-1]));
            installmentId++;
        }
    }

    private void validateOneOneTimeFee (LoanBO loan, String expectedPeriodicFeeName, int expectedInstallmentFeeIsAppliedTo,
            double expectedFee) {

        int installmentId = 1;
        for (AccountActionDateEntity accountActionDate : getActionDatesSortedByDate(loan)) {
            LoanScheduleEntity scheduleEntity= (LoanScheduleEntity) accountActionDate;
            if (installmentId == expectedInstallmentFeeIsAppliedTo) {
                assertThat("wrong number of fees for one-time installment " + installmentId, scheduleEntity.getAccountFeesActionDetails().size(), is(1));
                assertThat(scheduleEntity.getAccountFeesActionDetailsSortedByFeeId().get(0).getFee().getFeeName(),
                        is(expectedPeriodicFeeName));
                assertThat(scheduleEntity.getAccountFeesActionDetailsSortedByFeeId().get(0).getFeeAmount().getAmountDoubleValue(),
                        is(expectedFee));

            } else {
            assertThat("wrong number of fees for installment " + installmentId, scheduleEntity.getAccountFeesActionDetails().size(), is(0));
            }
           installmentId++;
        }
    }

    private List<AccountActionDateEntity> getActionDatesSortedByDate(LoanBO loan) {
        List<AccountActionDateEntity> sortedList = new ArrayList<AccountActionDateEntity>();
        sortedList.addAll(loan.getAccountActionDates());
        Collections.sort(sortedList);
        return sortedList;
    }

}
