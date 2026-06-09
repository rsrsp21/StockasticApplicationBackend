package com.stockasticappbackend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stockasticappbackend.dto.watchlist.WatchlistDetailResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistItemResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.model.entity.Watchlist;
import com.stockasticappbackend.model.entity.WatchlistItem;

@Mapper(componentModel = "spring", uses = { StockMapper.class })
public interface WatchlistMapper {

   /*
    * Watchlist → Response
    */

   @Mapping(source = "watchlistId", target = "id")
   @Mapping(source = "user.userId", target = "userId")
   @Mapping(source = "name", target = "name")
   @Mapping(target = "description", constant = "")
   @Mapping(source = "createdAt", target = "createdAt")
   @Mapping(expression = "java(watchlist.getItems() != null ? watchlist.getItems().size() : 0)", target = "stockCount")
   WatchlistResponse toResponse(Watchlist watchlist);

   List<WatchlistResponse> toResponseList(List<Watchlist> watchlists);

   /*
    * Watchlist → Detail Response
    */

   @Mapping(source = "watchlistId", target = "watchlistId")
   @Mapping(source = "user.userId", target = "userId")
   @Mapping(source = "name", target = "name")
   @Mapping(source = "createdAt", target = "createdAt")
   @Mapping(source = "items", target = "items")
   WatchlistDetailResponse toDetailResponse(Watchlist watchlist);

   /*
    * WatchlistItem → Response (uses StockMapper for stock)
    */

   @Mapping(source = "watchlistItemId", target = "watchlistItemId")
   @Mapping(source = "watchlist.watchlistId", target = "watchlistId")
   @Mapping(source = "addedAt", target = "addedAt")
   @Mapping(source = "stock", target = "stock") // StockMapper handles this
   WatchlistItemResponse toItemResponse(WatchlistItem item);

   List<WatchlistItemResponse> toItemResponseList(List<WatchlistItem> items);
}
