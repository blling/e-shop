package com.eshop.gqlgateway.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record BasketDto(
  @JsonProperty("id") UUID id,
  @JsonProperty("buyerId") String buyerId,
  @JsonProperty("items") List<BasketItemDto> items
) {
}
