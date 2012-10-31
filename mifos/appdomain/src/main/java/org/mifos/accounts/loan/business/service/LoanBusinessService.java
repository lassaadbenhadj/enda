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

package org.mifos.accounts.loan.business.service;

import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.business.AccountBO;
import org.mifos.accounts.business.AccountPaymentEntity;
import org.mifos.accounts.business.service.AccountBusinessService;
import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.accounts.loan.business.LoanScheduleEntity;
import org.mifos.accounts.loan.business.OriginalLoanScheduleEntity;
import org.mifos.accounts.loan.business.ScheduleCalculatorAdaptor;
import org.mifos.accounts.loan.persistance.LegacyLoanDao;
import org.mifos.accounts.loan.persistance.LoanDao;
import org.mifos.accounts.loan.util.helpers.LoanConstants;
import org.mifos.accounts.loan.util.helpers.RepaymentScheduleInstallment;
import org.mifos.accounts.util.helpers.AccountExceptionConstants;
import org.mifos.accounts.util.helpers.PaymentData;
import org.mifos.application.holiday.business.service.HolidayService;
import org.mifos.application.servicefacade.ApplicationContextProvider;
import org.mifos.config.AccountingRules;
import org.mifos.config.business.service.ConfigurationBusinessService;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.framework.business.AbstractBusinessObject;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.Money;
import org.mifos.platform.validations.Errors;
import org.mifos.security.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class LoanBusinessService implements BusinessService {

    private LegacyLoanDao legacyLoanDao = ApplicationContextProvider.getBean(LegacyLoanDao.class);
    private ConfigurationBusinessService configService = new ConfigurationBusinessService();

    @Autowired
    private AccountBusinessService accountBusinessService;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private ScheduleCalculatorAdaptor scheduleCalculatorAdaptor;


    public LegacyLoanDao getlegacyLoanDao() {
        if (legacyLoanDao == null) {
            legacyLoanDao = ApplicationContextProvider.getBean(LegacyLoanDao.class);
        }
        return legacyLoanDao;
    }

    public ConfigurationBusinessService getConfigService() {
        if (configService == null) {
            configService = new ConfigurationBusinessService();
        }
        return configService;
    }

    public AccountBusinessService getAccountBusinessService() {
        if (accountBusinessService == null) {
            accountBusinessService = new AccountBusinessService();
        }
        return accountBusinessService;
    }

    // Spring requires this
    // Autowired can be used at the constructor but
    // constructor is possing some non bean dependencies so for now
    // we use property bean injection instead of construct-arg
    protected LoanBusinessService() {
    }

    public LoanBusinessService(LegacyLoanDao legacyLoanDao, ConfigurationBusinessService configService,
                               AccountBusinessService accountBusinessService, HolidayService holidayService,
                               ScheduleCalculatorAdaptor scheduleCalculatorAdaptor) {
        this.legacyLoanDao = legacyLoanDao;
        this.configService = configService;
        this.accountBusinessService = accountBusinessService;
        this.holidayService = holidayService;
        this.scheduleCalculatorAdaptor = scheduleCalculatorAdaptor;
    }

    @Override
    public AbstractBusinessObject getBusinessObject(@SuppressWarnings("unused") final UserContext userContext) {
        return null;
    }

    /**
     * @deprecated use {@link LoanDao#findByGlobalAccountNum(String)}
     */
    @Deprecated
    public LoanBO findBySystemId(final String accountGlobalNum) throws ServiceException {
        try {
            return getlegacyLoanDao().findBySystemId(accountGlobalNum);
        } catch (PersistenceException e) {
            throw new ServiceException(AccountExceptionConstants.FINDBYGLOBALACCNTEXCEPTION, e,
                    new Object[] { accountGlobalNum });
        }
    }

    /**
     * use loanDao implementation
     */
    @Deprecated
    public List<LoanBO> findIndividualLoans(final String accountId) throws ServiceException {
        try {
            return getlegacyLoanDao().findIndividualLoans(accountId);
        } catch (PersistenceException e) {
            throw new ServiceException(AccountExceptionConstants.FINDBYGLOBALACCNTEXCEPTION, e,
                    new Object[] { accountId });
        }
    }

    /**
     * @deprecated - use {@link LoanDao#findById(Integer)}
     */
    @Deprecated
    public LoanBO getAccount(final Integer accountId) throws ServiceException {
        try {
            return getlegacyLoanDao().getAccount(accountId);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public List<LoanBO> getLoanAccountsActiveInGoodBadStanding(final Integer customerId) throws ServiceException {
        try {
            return getlegacyLoanDao().getLoanAccountsActiveInGoodBadStanding(customerId);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public Short getLastPaymentAction(final Integer accountId) throws ServiceException {
        try {
            return getlegacyLoanDao().getLastPaymentAction(accountId);
        } catch (PersistenceException e) {
            throw new ServiceException(e);
        }
    }

    public List<LoanBO> getSearchResults(final String officeId, final String personnelId, final String currentStatus)
            throws ServiceException {
        try {
            return getlegacyLoanDao().getSearchResults(officeId, personnelId, currentStatus);
        } catch (PersistenceException he) {
            throw new ServiceException(he);
        }
    }

    public List<LoanBO> getAllLoanAccounts() throws ServiceException {
        try {
            return getlegacyLoanDao().getAllLoanAccounts();
        } catch (PersistenceException pe) {
            throw new ServiceException(pe);
        }
    }

    public List<LoanBO> getAllChildrenForParentGlobalAccountNum(final String globalAccountNum) throws ServiceException {
        return findIndividualLoans(findBySystemId(globalAccountNum).getAccountId().toString());
    }

    public List<LoanBO> getActiveLoansForAllClientsAssociatedWithGroupLoan(final LoanBO loan) throws ServiceException {
        List<LoanBO> activeLoans = new ArrayList<LoanBO>();
        Collection<CustomerBO> clients = getClientsAssociatedWithGroupLoan(loan);
        if (clients != null && clients.size() > 0) {
            for (CustomerBO customerBO : clients) {
                for (AccountBO accountBO : customerBO.getAccounts()) {
                    if (accountBO.isActiveLoanAccount()) {
                        activeLoans.add((LoanBO) accountBO);
                    }
                }
            }
        }
        return activeLoans;
    }

    private Collection<CustomerBO> getClientsAssociatedWithGroupLoan(final LoanBO loan) throws ServiceException {
        Collection<CustomerBO> clients;

        if (getConfigService().isGlimEnabled()) {
            clients = getAccountBusinessService().getCoSigningClientsForGlim(loan.getAccountId());
        } else {
            clients = loan.getCustomer().getChildren();
        }
        return clients;
    }

    public List<RepaymentScheduleInstallment> applyDailyInterestRatesWhereApplicable(LoanScheduleGenerationDto loanScheduleGenerationDto, Locale locale) {
        LoanBO loanBO = loanScheduleGenerationDto.getLoanBO();
        List<RepaymentScheduleInstallment> installments = loanBO.toRepaymentScheduleDto(locale);
        return applyDailyInterestRatesWhereApplicable(loanScheduleGenerationDto, installments);
    }

    private boolean dailyInterestRatesApplicable(LoanScheduleGenerationDto loanScheduleGenerationDto, LoanBO loanBO) {
        return loanScheduleGenerationDto.isVariableInstallmentsAllowed() || loanBO.isDecliningBalanceInterestRecalculation();
    }

    public void applyDailyInterestRates(LoanScheduleGenerationDto loanScheduleGenerationDto) {
        Double dailyInterestFactor = loanScheduleGenerationDto.getInterestRate() / (AccountingRules.getNumberOfInterestDays() * 100d);
        Money principalOutstanding = loanScheduleGenerationDto.getLoanAmountValue();
        Money runningPrincipal = new Money(loanScheduleGenerationDto.getLoanAmountValue().getCurrency());
        Date initialDueDate = loanScheduleGenerationDto.getDisbursementDate();
        int installmentIndex, numInstallments;
        for (installmentIndex = 0, numInstallments = loanScheduleGenerationDto.getInstallments().size(); installmentIndex < numInstallments - 1; installmentIndex++) {
            RepaymentScheduleInstallment installment = loanScheduleGenerationDto.getInstallments().get(installmentIndex);
            Date currentDueDate = installment.getDueDateValue();
            long duration = DateUtils.getNumberOfDaysBetweenTwoDates(currentDueDate, initialDueDate);
            Money fees = installment.getFees();
            Money interest = computeInterestAmount(dailyInterestFactor, principalOutstanding, installment, duration);
            Money miscFee = installment.getMiscFees();
            Money miscPenality = installment.getMiscPenalty();
            Money total = installment.getTotalValue();
            Money principal = total.subtract(interest.add(fees).add(miscFee).add(miscPenality));
            installment.setPrincipalAndInterest(interest, principal);
            initialDueDate = currentDueDate;
            principalOutstanding = principalOutstanding.subtract(principal);
            runningPrincipal = runningPrincipal.add(principal);
        }

        RepaymentScheduleInstallment lastInstallment = loanScheduleGenerationDto.getInstallments().get(installmentIndex);
        long duration = DateUtils.getNumberOfDaysBetweenTwoDates(lastInstallment.getDueDateValue(), initialDueDate);
        Money interest = computeInterestAmount(dailyInterestFactor, principalOutstanding, lastInstallment, duration);
        Money fees = lastInstallment.getFees();
        Money principal = loanScheduleGenerationDto.getLoanAmountValue().subtract(runningPrincipal);
        Money total = principal.add(interest).add(fees);
        lastInstallment.setTotalAndTotalValue(total);
        lastInstallment.setPrincipalAndInterest(interest, principal);
    }

    private Money computeInterestAmount(Double dailyInterestFactor, Money principalOutstanding,
                                        RepaymentScheduleInstallment installment, long duration) {
        Double interestForInstallment = dailyInterestFactor * duration * principalOutstanding.getAmountDoubleValue();
        return new Money(installment.getCurrency(), interestForInstallment);
    }

    public void adjustInstallmentGapsPostDisbursal(List<RepaymentScheduleInstallment> installments,
                                                   Date oldDisbursementDate, Date newDisbursementDate, Short officeId) {
        Date oldPrevDate = oldDisbursementDate, newPrevDate = newDisbursementDate;
        for (RepaymentScheduleInstallment installment : installments) {
            Date currentDueDate = installment.getDueDateValue();
            long delta = DateUtils.getNumberOfDaysBetweenTwoDates(currentDueDate, oldPrevDate);
            Date newDueDate = DateUtils.addDays(newPrevDate, (int) delta);
            if (holidayService.isFutureRepaymentHoliday(DateUtils.getCalendar(newDueDate), officeId)) {
                installment.setDueDateValue(holidayService.getNextWorkingDay(newDueDate, officeId));
            } else {
                installment.setDueDateValue(newDueDate);
            }
            oldPrevDate = currentDueDate;
            newPrevDate = installment.getDueDateValue();
        }
    }

    public void adjustDatesForVariableInstallments(boolean variableInstallmentsAllowed, List<RepaymentScheduleInstallment> originalInstallments,
                                                   Date oldDisbursementDate, Date newDisbursementDate, Short officeId) {
        if (variableInstallmentsAllowed) {
            adjustInstallmentGapsPostDisbursal(originalInstallments, oldDisbursementDate, newDisbursementDate, officeId);
        }
    }


    public List<RepaymentScheduleInstallment> applyDailyInterestRatesWhereApplicable(
            LoanScheduleGenerationDto loanScheduleGenerationDto, List<RepaymentScheduleInstallment> installments) {
        LoanBO loanBO = loanScheduleGenerationDto.getLoanBO();
        if (dailyInterestRatesApplicable(loanScheduleGenerationDto, loanBO)) {
            loanScheduleGenerationDto.setInstallments(installments);
            applyDailyInterestRates(loanScheduleGenerationDto);
            loanBO.updateInstallmentSchedule(installments);
        }
        return installments;
    }

    public void applyPayment(PaymentData paymentData, LoanBO loanBO, AccountPaymentEntity accountPaymentEntity) {
        Money balance = paymentData.getTotalAmount();
        PersonnelBO personnel = paymentData.getPersonnel();
        Date transactionDate = paymentData.getTransactionDate();
        if (loanBO.isDecliningBalanceInterestRecalculation()) {
            scheduleCalculatorAdaptor.applyPayment(loanBO, balance, transactionDate, personnel, accountPaymentEntity);
        } else {
            for (AccountActionDateEntity accountActionDate : loanBO.getAccountActionDatesSortedByInstallmentId()) {
                balance = ((LoanScheduleEntity) accountActionDate).applyPayment(accountPaymentEntity, balance, personnel, transactionDate);
            }
        }
    }

    public void persistOriginalSchedule(LoanBO loan) throws PersistenceException {
        Collection<LoanScheduleEntity> loanScheduleEntities = loan.getLoanScheduleEntities();
        Collection<OriginalLoanScheduleEntity> originalLoanScheduleEntities = new ArrayList<OriginalLoanScheduleEntity>();
        for (LoanScheduleEntity loanScheduleEntity : loanScheduleEntities) {
                   originalLoanScheduleEntities.add(new OriginalLoanScheduleEntity(loanScheduleEntity));
        }
        legacyLoanDao.saveOriginalSchedule(originalLoanScheduleEntities);
    }

    public List<OriginalLoanScheduleEntity> retrieveOriginalLoanSchedule(Integer accountId) throws PersistenceException {
        return legacyLoanDao.getOriginalLoanScheduleEntity(accountId);
    }

    public Errors computeExtraInterest(LoanBO loan, Date asOfDate) {
        Errors errors = new Errors();
        validateForComputeExtraInterestDate(loan, asOfDate, errors);
        if (!errors.hasErrors()) scheduleCalculatorAdaptor.computeExtraInterest(loan, asOfDate);
        return errors;
    }

    private void validateForComputeExtraInterestDate(LoanBO loan, Date extraInterestDate, Errors errors) {
        if(loan.isDecliningBalanceInterestRecalculation()) {
            AccountPaymentEntity mostRecentNonzeroPayment = loan.findMostRecentNonzeroPaymentByPaymentDate();
            if (mostRecentNonzeroPayment != null) {
                Date lastPaymentDate = mostRecentNonzeroPayment.getPaymentDate();
                if(DateUtils.dateFallsBeforeDate(extraInterestDate, lastPaymentDate)) {
                    errors.addError(LoanConstants.CANNOT_VIEW_REPAYMENT_SCHEDULE, new String[]{extraInterestDate.toString()});
                }
            }
        }
    }
}
