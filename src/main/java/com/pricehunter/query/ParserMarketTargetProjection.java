package com.pricehunter.query;

import com.pricehunter.retail.SalesChannel;
import com.pricehunter.store.ParserType;

import java.util.UUID;

public interface ParserMarketTargetProjection {
    UUID getMarketId();
    UUID getChainId();
    String getChainCode();
    ParserType getParserType();
    UUID getCityId();
    String getCityName();
    SalesChannel getSalesChannel();
    String getSourceBaseUrl();
}
