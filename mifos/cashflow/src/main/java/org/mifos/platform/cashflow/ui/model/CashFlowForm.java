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
package org.mifos.platform.cashflow.ui.model;

import org.mifos.platform.cashflow.CashFlowConstants;
import org.mifos.platform.cashflow.service.CashFlowDetail;
import org.mifos.platform.cashflow.service.MonthlyCashFlowDetail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CashFlowForm implements Serializable {
    private static final long serialVersionUID = -3806820293757764245L;

    private CashFlowDetail cashFlowDetail;
    private boolean captureCapitalLiabilityInfo;
    private BigDecimal loanAmount;
    private Double indebtednessRatio;
    private BigDecimal totalRevenues;
    private BigDecimal totalExpenses;
    private Locale locale;

    @SuppressWarnings({"UnusedDeclaration", "PMD.UnnecessaryConstructor", "PMD.UncommentedEmptyConstructor"})
    public CashFlowForm() {
    }

    public CashFlowForm(CashFlowDetail cashFlowDetail, boolean captureCapitalLiabilityInfo, BigDecimal loanAmount, Double indebtednessRatio, Locale locale) {
        this.cashFlowDetail = cashFlowDetail;
        this.captureCapitalLiabilityInfo = captureCapitalLiabilityInfo;
        this.loanAmount = loanAmount;
        this.indebtednessRatio = indebtednessRatio;
        this.locale = locale;
    }

    public void setTotalCapital(BigDecimal totalCapital) {
        cashFlowDetail.setTotalCapital(totalCapital);
    }

    public void setTotalLiability(BigDecimal totalLiability) {
        cashFlowDetail.setTotalLiability(totalLiability);
    }

    public BigDecimal getTotalCapital() {
        return cashFlowDetail.getTotalCapital();
    }

    public BigDecimal getTotalLiability() {
        return cashFlowDetail.getTotalLiability();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public List<MonthlyCashFlowForm> getMonthlyCashFlows() {
        List<MonthlyCashFlowForm> monthlyCashFlows = new ArrayList<MonthlyCashFlowForm>();
        for (MonthlyCashFlowDetail monthlyCashFlowDetail : cashFlowDetail.getMonthlyCashFlowDetails()) {
            MonthlyCashFlowForm monthlyCashFlowForm = new MonthlyCashFlowForm(monthlyCashFlowDetail);
            monthlyCashFlowForm.setLocale(locale);
            monthlyCashFlows.add(monthlyCashFlowForm);
        }
        return monthlyCashFlows;
    }

    public boolean isCaptureCapitalLiabilityInfo() {
        return captureCapitalLiabilityInfo;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public Double getIndebtednessRatio() {
        return indebtednessRatio;
    }

    public boolean shouldForValidateIndebtednessRate() {
        return captureCapitalLiabilityInfo && indebtednessRatio != null && indebtednessRatio > 0 &&
                loanAmount != null && cashFlowDetail != null && cashFlowDetail.shouldForValidateIndebtednessRate();
    }

    public BigDecimal getTotalRevenues() {
        return totalRevenues;
    }

    public void setTotalRevenues(BigDecimal totalRevenues) {
        this.totalRevenues = totalRevenues;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getTotalBalance() {
        return totalRevenues.subtract(totalExpenses);
    }

    public BigDecimal computeRepaymentCapacity(BigDecimal totalInstallmentAmount) {
        return getTotalBalance().add(loanAmount).multiply(CashFlowConstants.HUNDRED).
                divide(totalInstallmentAmount, 2, BigDecimal.ROUND_HALF_UP);
    }
}
