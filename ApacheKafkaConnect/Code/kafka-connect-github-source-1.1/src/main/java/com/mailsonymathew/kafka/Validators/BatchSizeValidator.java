package com.mailsonymathew.kafka.Validators;

import org.apache.kafka.common.config.ConfigDef;
/*
 * Validator to ensure the Batch Size is Greater than 1 and <=100 when provided in the configuration parameter
 */
import org.apache.kafka.common.config.ConfigException;

public class BatchSizeValidator implements ConfigDef.Validator {

    @Override
    public void ensureValid(String name, Object value) {
        Integer batchSize = (Integer) value;
        if (!(1 <= batchSize && batchSize <=100)){
            throw new ConfigException(name, value, "Batch Size must be a positive integer that's less or equal to 100");
        }
    }
}