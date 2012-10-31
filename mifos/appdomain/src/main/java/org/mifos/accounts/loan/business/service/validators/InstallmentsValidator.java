package org.mifos.accounts.loan.business.service.validators;

import org.mifos.accounts.loan.util.helpers.RepaymentScheduleInstallment;
import org.mifos.accounts.productdefinition.business.VariableInstallmentDetailsBO;
import org.mifos.platform.validations.Errors;

import java.util.List;

public interface InstallmentsValidator {
    Errors validateInputInstallments(List<RepaymentScheduleInstallment> installments, InstallmentValidationContext installmentValidationContext);

    Errors validateInstallmentSchedule(List<RepaymentScheduleInstallment> installments, VariableInstallmentDetailsBO variableInstallmentDetailsBO);
}
