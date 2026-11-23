package com.ksmconnect.user;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.ksmconnect.common.error.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {

    private static final String DEFAULT_REGION = "PL";

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public String toE164(String rawPhoneNumber) {
        try {
            Phonenumber.PhoneNumber parsed = phoneNumberUtil.parse(rawPhoneNumber, DEFAULT_REGION);
            if (!phoneNumberUtil.isValidNumber(parsed)) {
                throw invalid();
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException exception) {
            throw invalid();
        }
    }

    private static BusinessRuleException invalid() {
        return new BusinessRuleException("INVALID_PHONE_NUMBER", "Phone number is not valid");
    }
}
