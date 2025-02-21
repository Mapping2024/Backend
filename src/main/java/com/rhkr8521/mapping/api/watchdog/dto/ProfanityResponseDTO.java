package com.rhkr8521.mapping.api.watchdog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProfanityResponseDTO {
    @JsonProperty("censored_text")
    private String censoredText;

    @JsonProperty("contains_profanity")
    private boolean containsProfanity;

    @JsonProperty("detected_words")
    private List<String> detectedWords;

    private String text;
}
