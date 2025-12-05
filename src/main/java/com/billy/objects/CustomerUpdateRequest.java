package com.billy.objects;

import java.util.Optional;
import java.util.OptionalLong;

public record CustomerUpdateRequest(
        long id,
        Optional<String> name,
        Optional<String> lastName,
        Optional<String> gender,
        Optional<String> email
) {}