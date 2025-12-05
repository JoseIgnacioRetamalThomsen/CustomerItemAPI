package com.billy.objects;

import java.util.Optional;

public record ItemUpdateRequest(
        Optional<String> name,
        Optional<Double> size,
        Optional<Double> weight,
        Optional<String> color
) {}
