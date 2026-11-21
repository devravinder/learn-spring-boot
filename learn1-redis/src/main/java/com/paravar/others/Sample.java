package com.paravar.others;

import lombok.Value;

// one RedisTimeSeries point: epoch millis + value
@Value
public class Sample {
    long timestamp;
    double value;
}
