package com.leo.hekima.model;

import java.time.Instant;

public record NoteSummary(long noteid, String noteuri, Instant createdat, String valeur) {
}
