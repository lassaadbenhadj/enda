package org.mifos.accounts.loan.business.service.validators;

import org.mifos.accounts.loan.util.helpers.RepaymentScheduleInstallment;
import org.mifos.platform.util.CollectionUtils;
import org.mifos.platform.util.Transformer;
import org.mifos.platform.validations.ErrorEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mifos.accounts.util.helpers.AccountConstants.INSTALLMENT_DUEDATE_DUPLICATE;
import static org.mifos.accounts.util.helpers.AccountConstants.INSTALLMENT_DUEDATE_INVALID_ORDER;

public class ListOfInstallmentsValidatorImpl implements ListOfInstallmentsValidator {
    @Override
    public List<ErrorEntry> validateDuplicateDueDates(List<RepaymentScheduleInstallment> installments) {
        Map<Date, List<String>> installmentsLookup = getDueDateInstallmentsLookup(installments);
        return retrieveErrors(installmentsLookup);
    }

    private Map<Date, List<String>> getDueDateInstallmentsLookup(List<RepaymentScheduleInstallment> installments) {
        Map<Date, List<String>> dateInstallmentsLookup = new LinkedHashMap<Date, List<String>>();
        for (RepaymentScheduleInstallment installment : installments) {
            Date key = installment.getDueDateValue();
            String value = installment.getInstallmentNumberAsString();
            CollectionUtils.addKeyValue(dateInstallmentsLookup, key, value);
        }
        return dateInstallmentsLookup;
    }

    private List<ErrorEntry> retrieveErrors(Map<Date, List<String>> dateInstallmentsLookup) {
        List<ErrorEntry> errorEntries = new ArrayList<ErrorEntry>();
        for (List<String> installments : dateInstallmentsLookup.values()) {
            if (installments.size() > 1) {
                errorEntries.add(new ErrorEntry(INSTALLMENT_DUEDATE_DUPLICATE, installments.toString()));
            }
        }
        return errorEntries;
    }

    @Override
    public List<ErrorEntry> validateOrderingOfDueDates(List<RepaymentScheduleInstallment> installments) {
        List<ErrorEntry> errorEntries = new ArrayList<ErrorEntry>();
        if (CollectionUtils.isNotEmpty(installments)) {
            List<Date> dueDates = CollectionUtils.collect(installments, getDueDateTransformer());
            int index = CollectionUtils.itemIndexOutOfAscendingOrder(dueDates);
            if (index >= 0) {
                String fieldName = installments.get(index).getInstallmentNumberAsString();
                errorEntries.add(new ErrorEntry(INSTALLMENT_DUEDATE_INVALID_ORDER, fieldName));
            }
        }
        return errorEntries;
    }

    private Transformer<RepaymentScheduleInstallment, Date> getDueDateTransformer() {
        return new Transformer<RepaymentScheduleInstallment, Date>() {
            @Override
            public Date transform(RepaymentScheduleInstallment input) {
                return input.getDueDateValue();
            }
        };
    }

}
