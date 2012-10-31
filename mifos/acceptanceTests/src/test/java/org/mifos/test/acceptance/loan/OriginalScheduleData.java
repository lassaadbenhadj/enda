package org.mifos.test.acceptance.loan;

@SuppressWarnings("PMD")
public class OriginalScheduleData {

    public static final String[][] FLAT_LOAN_SCHEDULE = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "11-Oct-2011", "200.2", "3.8", "110.0", "324.0"}, //110
            {"2", "18-Oct-2011", "200.2", "3.8", "100.0", "304.0"},
            {"3", "25-Oct-2011", "200.2", "3.8", "100.0", "304.0"},
            {"4", "02-Nov-2011", "200.2", "3.8", "100.0", "304.0"},
            {"5", "08-Nov-2011", "199.2", "4.8", "100.0", "304.0"}};

    public static final String[][] VARIABLE_LOAN_EARLY_DISBURSAL_SCHEDULE = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "18-Oct-2011", "197.6", "4.4", "10.0", "222.0"},
            {"2", "25-Oct-2011", "198.9", "3.1", "0.0", "202.0"},
            {"3", "02-Nov-2011", "199.4", "2.6", "0.0", "202.0"},
            {"4", "08-Nov-2011", "200.7", "1.3", "0.0", "202.0"},
            {"5", "22-Nov-2011", "203.4", "1.6", "0.0", "205.0"}}; //1.6 -> 1.4

    public static final String[][] VARIABLE_LOAN_LATE_DISBURSAL_SCHEDULE = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "12-Oct-2011", "201.5", "0.5", "10.0", "222.0"},
            {"2", "19-Oct-2011", "198.9", "3.1", "0.0", "202.0"},
            {"3", "26-Oct-2011", "199.7", "2.3", "0.0", "202.0"},
            {"4", "03-Nov-2011", "200.2", "1.8", "0.0", "202.0"},
            {"5", "09-Nov-2011", "199.7", "0.7", "0.0", "200.3"}};
    public static final String[][] DEC_BAL_INT_RECALC_LOAN_EARLY_DISBURSAL_SCHEDULE_ON = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "11-Oct-2011", "201.5", "0.5", "110.0", "322.0"},
            {"2", "18-Oct-2011", "198.9", "3.1", "100.0", "302.0"},
            {"3", "25-Oct-2011", "199.7", "2.3", "100.0", "302.0"},
            {"4", "02-Nov-2011", "200.2", "1.8", "100.0", "302.0"},
            {"5", "08-Nov-2011", "199.7", "0.7", "100.0", "300.3"}};
    public static final String[][] DEC_BAL_INT_RECALC_LOAN_LATE_DISBURSAL_SCHEDULE_ON = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "18-Oct-2011", "198.2", "3.8", "110.0", "322.0"},
            {"2", "25-Oct-2011", "198.9", "3.1", "100.0", "302.0"},
            {"3", "02-Nov-2011", "199.4", "2.6", "100.0", "302.0"},
            {"4", "08-Nov-2011", "200.7", "1.3", "100.0", "302.0"},
            {"5", "22-Nov-2011", "202.9", "1.6", "100.0", "304.4"}};
    public static final String[][] DEC_BAL_INT_RECALC_LOAN_EARLY_DISBURSAL_SCHEDULE_OFF = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "11-Oct-2011", "201.5", "0.5", "110.0", "322.0"}};
    //    public static final String[][] DEC_BAL_INT_RECALC_LOAN_EARLY_DISBURSAL_SCHEDULE_OFF = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
//            {"1", "11-Oct-2011", "221.5", "0.5", "100.0", "322.0"},
//            {"2", "18-Oct-2011", "199.0", "3.0", "100.0", "302.0"},
//            {"3", "25-Oct-2011", "199.8", "2.2", "100.0", "302.0"},
//            {"4", "02-Nov-2011", "200.3", "1.7", "100.0", "302.0"},
//            {"5", "08-Nov-2011", "179.4", "0.6", "100.0", "280.0"}};
    public static final String[][] DEC_BAL_INT_RECALC_LOAN_LATE_DISBURSAL_SCHEDULE_OFF = {{"Installment", "DueDate", "Principal", "Interest", "Fees", "Total (with Penalty)"},
            {"1", "25-Oct-2011", "198.2", "3.8", "110.0", "322.0"},
            {"2", "02-Nov-2011", "198.5", "3.5", "100.0", "302.0"},
            {"3", "08-Nov-2011", "200.0", "2.0", "100.0", "302.0"},
            {"4", "22-Nov-2011", "198.9", "3.1", "100.0", "302.0"},
            {"5", "22-Nov-2011", "204.4", "0.0", "100.0", "304.4"}};

}
