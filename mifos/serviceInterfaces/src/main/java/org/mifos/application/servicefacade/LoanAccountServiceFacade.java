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

package org.mifos.application.servicefacade;

import org.mifos.dto.domain.AccountPaymentParametersDto;
import org.mifos.dto.domain.AccountStatusDto;
import org.mifos.dto.domain.AccountUpdateStatus;
import org.mifos.dto.domain.CreateAccountNote;
import org.mifos.dto.domain.CreateLoanRequest;
import org.mifos.dto.domain.CustomerDto;
import org.mifos.dto.domain.LoanAccountDetailsDto;
import org.mifos.dto.domain.LoanActivityDto;
import org.mifos.dto.domain.LoanInstallmentDetailsDto;
import org.mifos.dto.domain.LoanPaymentDto;
import org.mifos.dto.screen.ChangeAccountStatusDto;
import org.mifos.dto.screen.LoanAccountDetailDto;
import org.mifos.dto.screen.LoanAccountInfoDto;
import org.mifos.dto.screen.LoanAccountMeetingDto;
import org.mifos.dto.screen.LoanCreationLoanDetailsDto;
import org.mifos.dto.screen.LoanCreationPreviewDto;
import org.mifos.dto.screen.LoanCreationProductDetailsDto;
import org.mifos.dto.screen.LoanCreationResultDto;
import org.mifos.dto.screen.LoanDisbursalDto;
import org.mifos.dto.screen.LoanInformationDto;
import org.mifos.dto.screen.LoanScheduledInstallmentDto;
import org.mifos.dto.screen.MultipleLoanAccountDetailsDto;
import org.mifos.dto.screen.RepayLoanDto;
import org.mifos.dto.screen.RepayLoanInfoDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.List;

public interface LoanAccountServiceFacade {

    @PreAuthorize("isFullyAuthenticated()")
    AccountStatusDto retrieveAccountStatuses(Long loanAccountId);

    @PreAuthorize("isFullyAuthenticated()")
    String updateLoanAccountStatus(AccountUpdateStatus updateStatus);

    @PreAuthorize("isFullyAuthenticated()")
    LoanAccountDetailDto retrieveLoanAccountNotes(Long loanAccountId);

    @PreAuthorize("isFullyAuthenticated()")
    void addNote(CreateAccountNote accountNote);

    @PreAuthorize("isFullyAuthenticated()")
    LoanCreationProductDetailsDto retrieveGetProductDetailsForLoanAccountCreation(Integer customerId);

    @PreAuthorize("isFullyAuthenticated()")
    LoanCreationLoanDetailsDto retrieveLoanDetailsForLoanAccountCreation(Integer customerId, Short productId);

    @PreAuthorize("isFullyAuthenticated()")
    LoanCreationPreviewDto previewLoanCreationDetails(Integer customerId, List<LoanAccountDetailsDto> accountDetails, List<String> selectedClientIds);

    @PreAuthorize("isFullyAuthenticated()")
    LoanCreationResultDto createLoan(LoanAccountMeetingDto loanAccountMeetingDto, LoanAccountInfoDto loanAccountInfoDto, List<LoanScheduledInstallmentDto> loanRepayments);

    @PreAuthorize("isFullyAuthenticated()")
    LoanCreationResultDto redoLoan(LoanAccountMeetingDto loanAccountMeetingDto, LoanAccountInfoDto loanAccountInfoDto, List<LoanPaymentDto> existingLoanPayments, List<LoanScheduledInstallmentDto> installmentDtos);

    @PreAuthorize("isFullyAuthenticated()")
    List<LoanActivityDto> retrieveAllLoanAccountActivities(String globalAccountNum);

    @PreAuthorize("isFullyAuthenticated()")
    LoanInstallmentDetailsDto retrieveInstallmentDetails(Integer accountId);

    @PreAuthorize("isFullyAuthenticated()")
    boolean isTrxnDateValid(Integer loanAccountId, Date trxnDate);

    @PreAuthorize("isFullyAuthenticated()")
    void makeEarlyRepayment(RepayLoanInfoDto repayLoanInfoDto);

    @PreAuthorize("isFullyAuthenticated()")
    LoanInformationDto retrieveLoanInformation(String globalAccountNum);

    @PreAuthorize("isFullyAuthenticated()")
    RepayLoanDto retrieveLoanRepaymentDetails(String globalAccountNum);

    @PreAuthorize("isFullyAuthenticated()")
    List<LoanAccountDetailsDto> retrieveLoanAccountDetails(LoanInformationDto loanInformationDto);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_DISBURSE_LOAN')")
    LoanDisbursalDto retrieveLoanDisbursalDetails(Integer loanAccountId);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_DISBURSE_LOAN')")
    void disburseLoan(AccountPaymentParametersDto loanDisbursement, Short paymentTypeId);

    @PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CAN_APPROVE_LOANS_IN_BULK', 'ROLE_CAN_CREATE_MULTIPLE_LOAN_ACCOUNTS')")
    ChangeAccountStatusDto retrieveAllActiveBranchesAndLoanOfficerDetails();

    @PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CAN_APPROVE_LOANS_IN_BULK', 'ROLE_CAN_CREATE_MULTIPLE_LOAN_ACCOUNTS')")
    ChangeAccountStatusDto retrieveLoanOfficerDetailsForBranch(Short officeId);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_APPROVE_LOANS_IN_BULK')")
    List<String> updateSeveralLoanAccountStatuses(List<AccountUpdateStatus> accountsForUpdate);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_REVERSE_LOAN_DISBURSAL')")
    List<LoanActivityDto> retrieveLoanPaymentsForReversal(String globalAccountNum);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_REVERSE_LOAN_DISBURSAL')")
    void reverseLoanDisbursal(String globalAccountNum, String note);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_CREATE_MULTIPLE_LOAN_ACCOUNTS')")
    List<CustomerDto> retrieveActiveGroupingAtTopOfCustomerHierarchyForLoanOfficer(Short loanOfficerId, Short officeId);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_CREATE_MULTIPLE_LOAN_ACCOUNTS')")
    MultipleLoanAccountDetailsDto retrieveMultipleLoanAccountDetails(String searchId, Short branchId, Integer productId);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_CREATE_MULTIPLE_LOAN_ACCOUNTS')")
    List<String> createMultipleLoans(List<CreateLoanRequest> createMultipleLoans);
}