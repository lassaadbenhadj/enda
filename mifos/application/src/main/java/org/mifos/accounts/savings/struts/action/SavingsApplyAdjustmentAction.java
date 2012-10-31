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

package org.mifos.accounts.savings.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hibernate.Hibernate;
import org.mifos.accounts.business.AccountActionEntity;
import org.mifos.accounts.business.AccountPaymentEntity;
import org.mifos.accounts.business.service.AccountBusinessService;
import org.mifos.accounts.exceptions.AccountException;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.persistence.SavingsPersistence;
import org.mifos.accounts.savings.struts.actionforms.SavingsApplyAdjustmentActionForm;
import org.mifos.accounts.savings.util.helpers.SavingsConstants;
import org.mifos.accounts.savings.util.helpers.SavingsHelper;
import org.mifos.accounts.util.helpers.AccountTypes;
import org.mifos.core.MifosRuntimeException;
import org.mifos.dto.domain.SavingsAdjustmentDto;
import org.mifos.dto.screen.SavingsAdjustmentReferenceDto;
import org.mifos.framework.struts.action.BaseAction;
import org.mifos.framework.util.helpers.Constants;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TransactionDemarcate;
import org.mifos.security.util.UserContext;
import org.mifos.service.BusinessRuleException;

/**
 * @deprecated - this struts action should be replaced by ftl/spring web flow or spring mvc implementation.
 *             - note: service facade is in place to return all information needed and spring security is set up on service facade
 */
@Deprecated
public class SavingsApplyAdjustmentAction extends BaseAction {

    @TransactionDemarcate(joinToken = true)
    public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {

        clearActionForm(form);
        doCleanUp(request);
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        SessionUtils.removeAttribute(Constants.BUSINESS_KEY, request);

        Long savingsId = Long.valueOf(savings.getAccountId());
        savings = this.savingsDao.findById(savingsId);
        savings.setUserContext(uc);

        SavingsAdjustmentReferenceDto savingsAdjustmentDto = this.savingsServiceFacade.retrieveAdjustmentReferenceData(savingsId);

        if (savingsAdjustmentDto.isDepositOrWithdrawal()) {

            AccountPaymentEntity lastPayment = savings.findMostRecentPaymentByPaymentDate();
            AccountActionEntity accountAction = legacyMasterDao.getPersistentObject(
                    AccountActionEntity.class, new SavingsHelper().getPaymentActionType(lastPayment));
            accountAction.setLocaleId(uc.getLocaleId());

            Hibernate.initialize(savings.findMostRecentPaymentByPaymentDate().getAccountTrxns());

            SessionUtils.setAttribute(SavingsConstants.ACCOUNT_ACTION, accountAction, request);
            SessionUtils.setAttribute(SavingsConstants.CLIENT_NAME, savingsAdjustmentDto.getClientName(), request);
            SessionUtils.setAttribute(SavingsConstants.IS_LAST_PAYMENT_VALID, Constants.YES, request);
        } else {
            SessionUtils.setAttribute(SavingsConstants.IS_LAST_PAYMENT_VALID, Constants.NO, request);
        }

        SessionUtils.setAttribute(Constants.BUSINESS_KEY, savings, request);

        return mapping.findForward("load_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward preview(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, @SuppressWarnings("unused") HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        return mapping.findForward("preview_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward previous(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, @SuppressWarnings("unused") HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        return mapping.findForward("previous_success");
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward adjustLastUserAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        request.setAttribute("method", "adjustLastUserAction");
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        Integer accountId = savings.getAccountId();
        Integer versionNum = savings.getVersionNo();
        savings = savingsDao.findById(accountId);

        // NOTE: initialise so when error occurs when apply adjustment, savings object is correctly initialised for
        // use within Tag library in jsp.
        new SavingsPersistence().initialize(savings);
        checkVersionMismatch(versionNum, savings.getVersionNo());
        savings.setUserContext(uc);
        SessionUtils.setAttribute(Constants.BUSINESS_KEY, savings, request);

        if (savings.getPersonnel() != null) {
            getBizService().checkPermissionForAdjustment(AccountTypes.SAVINGS_ACCOUNT, null, uc,
                    savings.getOffice().getOfficeId(), savings.getPersonnel().getPersonnelId());
        } else {
            getBizService().checkPermissionForAdjustment(AccountTypes.SAVINGS_ACCOUNT, null, uc,
                    savings.getOffice().getOfficeId(), uc.getId());
        }

        SavingsApplyAdjustmentActionForm actionForm = (SavingsApplyAdjustmentActionForm) form;
        if (actionForm.getLastPaymentAmount() == null) {
            throw new MifosRuntimeException("Null payment amount is not allowed");
        }

        Long savingsId = Long.valueOf(accountId.toString());
        Double adjustedAmount = Double.valueOf(actionForm.getLastPaymentAmount());
        String note = actionForm.getNote();

        SavingsAdjustmentDto savingsAdjustment = new SavingsAdjustmentDto(savingsId, adjustedAmount, note);
        try {
            this.savingsServiceFacade.adjustTransaction(savingsAdjustment);
        } catch (BusinessRuleException e) {
            throw new AccountException(e.getMessageKey(), e);
        } finally {
            doCleanUp(request);
        }

        return mapping.findForward("account_detail_page");
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward cancel(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        doCleanUp(request);
        return mapping.findForward("account_detail_page");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward validate(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        String method = (String) request.getAttribute("methodCalled");
        String forward = null;
        if (method != null && method.equals("preview")) {
            forward = "preview_failure";
        } else if (method != null && method.equals("SavingsAdjustmentAction")) {
            forward = "adjustLastUserAction_failure";
        }
        return mapping.findForward(forward);
    }

    private void clearActionForm(ActionForm form) {
        SavingsApplyAdjustmentActionForm actionForm = (SavingsApplyAdjustmentActionForm) form;
        actionForm.setLastPaymentAmountOption("1");
        actionForm.setLastPaymentAmount(null);
        actionForm.setNote(null);
    }

    private void doCleanUp(HttpServletRequest request) throws Exception {
        SessionUtils.removeAttribute(SavingsConstants.ACCOUNT_ACTION, request);
        SessionUtils.removeAttribute(SavingsConstants.CLIENT_NAME, request);
    }

    private AccountBusinessService getBizService() {
        return new AccountBusinessService();
    }
}