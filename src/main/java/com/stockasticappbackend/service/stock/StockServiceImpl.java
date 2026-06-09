package com.stockasticappbackend.service.stock;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.stock.BulkUploadResponse;
import com.stockasticappbackend.dto.stock.StockRequest;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.StockMapper;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.stockprice.IndicatorService;
import com.stockasticappbackend.util.Constants;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final StockMapper stockMapper;
    private final EntityManager entityManager;
    private final IndicatorService indicatorService;

    @Value("${file.upload-stocks:uploads/stocks}")
    private String stocksDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/webp");

    private static final Set<String> CSV_CONTENT_TYPES = Set.of(
            "text/csv",
            "application/vnd.ms-excel",
            "application/csv",
            "text/plain");

    private static final Set<String> EXCEL_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel");
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
    private PageResponse<StockResponse> toPageResponse(Page<Stock> stockPage) {
        List<StockResponse> content = stockPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<StockResponse>builder()
                .content(content)
                .page(stockPage.getNumber())
                .size(stockPage.getSize())
                .totalElements(stockPage.getTotalElements())
                .totalPages(stockPage.getTotalPages())
                .first(stockPage.isFirst())
                .last(stockPage.isLast())
                .hasNext(stockPage.hasNext())
                .hasPrevious(stockPage.hasPrevious())
                .build();
    }
    private StockResponse mapToResponse(Stock stock) {
        StockResponse response = stockMapper.toResponse(stock);
        enrichWithLatestPrice(response, stock);
        return response;
    }
    private void enrichWithLatestPrice(StockResponse response, Stock stock) {
        List<StockPrice> latestPrices = stockPriceRepository.findByStockOrderByPriceTimeDesc(
                stock, PageRequest.of(0, 1)).getContent();

        if (latestPrices.isEmpty()) {
            return;
        }

        StockPrice latestPrice = latestPrices.get(0);
        response.setCurrentPrice(latestPrice.getIntervalClose());
        Long totalVolume = stockPriceRepository.sumVolumeByStockIdAndDate(stock.getStockId(), latestPrice.getPriceTime().toLocalDate());
        response.setVolume(totalVolume);
        Double avg = stockPriceRepository.getAvgVolumeByStockIdAndDate(stock.getStockId(), latestPrice.getPriceTime().toLocalDate());
        response.setAvgVolume(avg != null ? Math.round(avg) : 0L);

        BigDecimal baseline = latestPrice.getPreviousClose();
        if (baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            baseline = latestPrice.getIntervalOpen();
        }

        if (baseline != null && baseline.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal change = latestPrice.getIntervalClose().subtract(baseline);
            BigDecimal changePercent = change
                    .divide(baseline, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            response.setChangePercent(changePercent);
        }
    }
    @Override
    @Caching(evict = {
            @CacheEvict(value = "allStocks", allEntries = true),
            @CacheEvict(value = "stockById", allEntries = true),
            @CacheEvict(value = "stockBySymbol", allEntries = true)
    })
    public StockResponse createStock(StockRequest stockRequest, MultipartFile image) {

        if (stockRepository.existsBySymbol(stockRequest.getSymbol())) {
            throw new IllegalArgumentException(
                    String.format(Constants.STOCK_DUPLICATE_SYMBOL, stockRequest.getSymbol()));
        }

        Stock stock = stockMapper.toEntity(stockRequest);
        Stock savedStock = stockRepository.save(stock);

        if (image != null && !image.isEmpty()) {
            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new IllegalArgumentException(Constants.STOCK_IMAGE_TYPE_ERROR);
            }

            try {
                Path uploadPath = Paths.get(stocksDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);

                String extension = resolveExtension(contentType);
                String fileName = "stock_" + savedStock.getStockId() + extension;
                Path filePath = uploadPath.resolve(fileName);

                image.transferTo(filePath.toFile());

                savedStock.setImage(fileName);
                savedStock = stockRepository.save(savedStock);

            } catch (Exception e) {
                throw new RuntimeException(Constants.STOCK_IMAGE_UPLOAD_ERROR, e);
            }
        }

        // Bootstrap indicator window and compute if enough history is available.
        // This ensures newly added stocks can show indicators without requiring app restart.
        try {
            indicatorService.initializeIndicators(savedStock);
        } catch (Exception e) {
            log.warn("Failed to initialize indicators for new stock {}: {}", savedStock.getSymbol(), e.getMessage());
        }

        return mapToResponse(savedStock);
    }

    private static final int BATCH_SIZE = 50;

    /**
     * Bulk uploads stocks from a CSV or Excel (.xlsx/.xls) file.
     * Expected columns: symbol (required), name (required), exchange (required),
     * sector (optional), description (optional), isActive (optional, defaults to true).
     *
     * Stocks are processed in batches of 50. After each batch, the persistence context
     * is flushed and cleared to prevent excessive memory consumption on large uploads.
     *
     * An optional ZIP file can be provided containing stock logo images named by symbol
     * (e.g., AAPL.webp, GOOGL.webp). Only WEBP images are supported. After each stock is
     * saved, if a matching image is found in the ZIP, it is extracted and saved.
     *
     * Rows are processed independently — a failure in one row does not prevent others
     * from being saved. Duplicate symbols (both within the file and against the DB) are skipped.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allStocks", allEntries = true),
            @CacheEvict(value = "stockById", allEntries = true),
            @CacheEvict(value = "stockBySymbol", allEntries = true)
    })
    public BulkUploadResponse bulkUploadStocks(MultipartFile file, MultipartFile imagesZip) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(Constants.BULK_UPLOAD_EMPTY_FILE);
        }

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        List<Map<String, String>> rows;
        if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
            rows = parseCsvFile(file);
        } else if (fileName != null && (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls"))) {
            rows = parseExcelFile(file);
        } else if (contentType != null && CSV_CONTENT_TYPES.contains(contentType)) {
            rows = parseCsvFile(file);
        } else if (contentType != null && EXCEL_CONTENT_TYPES.contains(contentType)) {
            rows = parseExcelFile(file);
        } else {
            throw new IllegalArgumentException(Constants.BULK_UPLOAD_INVALID_FORMAT);
        }
        Map<String, byte[]> imageMap = new HashMap<>();
        if (imagesZip != null && !imagesZip.isEmpty()) {
            imageMap = extractImagesFromZip(imagesZip);
        }
        Path uploadPath = Paths.get(stocksDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (Exception e) {
            log.warn("Could not create upload directory: {}", e.getMessage());
        }
        int totalRows = rows.size();
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;
        List<BulkUploadResponse.RowError> errors = new ArrayList<>();
        Set<String> seenSymbols = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNumber = i + 1;
            String symbol = getCleanValue(row, "symbol");

            try {
                if (symbol == null || symbol.isBlank()) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol("").message(Constants.BULK_UPLOAD_ROW_SYMBOL_REQUIRED).build());
                    failureCount++;
                    continue;
                }

                symbol = symbol.toUpperCase().trim();

                String name = getCleanValue(row, "name");
                String exchange = getCleanValue(row, "exchange");
                String sector = getCleanValue(row, "sector");
                String description = getCleanValue(row, "description");
                String isActiveStr = getCleanValue(row, "isactive");

                if (name == null || name.isBlank()) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_ROW_NAME_REQUIRED).build());
                    failureCount++;
                    continue;
                }

                if (exchange == null || exchange.isBlank()) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_ROW_EXCHANGE_REQUIRED).build());
                    failureCount++;
                    continue;
                }
                if (symbol.length() > 10) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_ROW_SYMBOL_TOO_LONG).build());
                    failureCount++;
                    continue;
                }
                if (exchange.length() > 50) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_ROW_EXCHANGE_TOO_LONG).build());
                    failureCount++;
                    continue;
                }
                if (sector != null && sector.length() > 100) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_ROW_SECTOR_TOO_LONG).build());
                    failureCount++;
                    continue;
                }
                if (description != null && description.length() > 500) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_ROW_DESC_TOO_LONG).build());
                    failureCount++;
                    continue;
                }
                if (seenSymbols.contains(symbol)) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol).message(Constants.BULK_UPLOAD_DUPLICATE_IN_FILE).build());
                    skippedCount++;
                    continue;
                }
                seenSymbols.add(symbol);
                if (stockRepository.existsBySymbol(symbol)) {
                    errors.add(BulkUploadResponse.RowError.builder()
                            .row(rowNumber).symbol(symbol)
                            .message(String.format(Constants.STOCK_DUPLICATE_SYMBOL, symbol)).build());
                    skippedCount++;
                    continue;
                }
                boolean isActive = true;
                if (isActiveStr != null && !isActiveStr.isBlank()) {
                    isActive = isActiveStr.equalsIgnoreCase("true")
                            || isActiveStr.equalsIgnoreCase("yes")
                            || isActiveStr.equals("1");
                }
                Stock stock = new Stock();
                stock.setSymbol(symbol);
                stock.setName(name.trim());
                stock.setExchange(exchange.trim());
                stock.setSector(sector != null ? sector.trim() : null);
                stock.setDescription(description != null ? description.trim() : null);
                stock.setIsActive(isActive);

                Stock savedStock = stockRepository.save(stock);
                if (!imageMap.isEmpty()) {
                    byte[] imageBytes = imageMap.get(symbol);
                    if (imageBytes != null) {
                        try {
                            String imgFileName = "stock_" + savedStock.getStockId() + ".webp";
                            Path filePath = uploadPath.resolve(imgFileName);
                            Files.write(filePath, imageBytes);
                            savedStock.setImage(imgFileName);
                            stockRepository.save(savedStock);
                        } catch (Exception imgEx) {
                            log.warn("Failed to save image for symbol {}: {}", symbol, imgEx.getMessage());
                        }
                    }
                }

                successCount++;
                if (successCount % BATCH_SIZE == 0) {
                    stockRepository.flush();
                    entityManager.clear();
                    log.info("Bulk upload: flushed and cleared after {} successful saves", successCount);
                }

            } catch (Exception e) {
                log.error("Bulk upload error at row {}: {}", rowNumber, e.getMessage());
                errors.add(BulkUploadResponse.RowError.builder()
                        .row(rowNumber).symbol(symbol != null ? symbol : "")
                        .message(e.getMessage()).build());
                failureCount++;
            }
        }
        if (successCount % BATCH_SIZE != 0) {
            stockRepository.flush();
            entityManager.clear();
        }

        log.info("Bulk upload completed: total={}, success={}, skipped={}, failed={}",
                totalRows, successCount, skippedCount, failureCount);

        return BulkUploadResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failureCount(failureCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .build();
    }

    /**
     * Extracts WEBP images from a ZIP file into a map of SYMBOL -> byte[].
     * Only .webp files are extracted. File names are uppercased (without extension) as the key.
     */
    private Map<String, byte[]> extractImagesFromZip(MultipartFile zipFile) {
        Map<String, byte[]> imageMap = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String entryName = entry.getName();
                if (entryName.contains("/")) {
                    entryName = entryName.substring(entryName.lastIndexOf('/') + 1);
                }

                if (entryName.toLowerCase().endsWith(".webp")) {
                    String symbolKey = entryName.substring(0, entryName.lastIndexOf('.')).toUpperCase().trim();
                    byte[] imageBytes = zis.readAllBytes();
                    imageMap.put(symbolKey, imageBytes);
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            log.warn("Failed to extract images from ZIP: {}", e.getMessage());
        }
        return imageMap;
    }

    /**
     * Parses a CSV file into a list of row maps (column-name → value).
     */
    private List<Map<String, String>> parseCsvFile(MultipartFile file) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException(Constants.BULK_UPLOAD_EMPTY_FILE);
            }
            String[] normalizedHeaders = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                normalizedHeaders[i] = headers[i].trim().toLowerCase().replace(" ", "").replace("_", "");
            }

            validateHeaders(normalizedHeaders);

            List<Map<String, String>> rows = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> rowMap = new HashMap<>();
                for (int i = 0; i < normalizedHeaders.length && i < line.length; i++) {
                    rowMap.put(normalizedHeaders[i], line[i]);
                }
                rows.add(rowMap);
            }
            return rows;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(Constants.BULK_UPLOAD_PARSE_ERROR, e);
        }
    }

    /**
     * Parses an Excel (.xlsx/.xls) file into a list of row maps (column-name → value).
     */
    private List<Map<String, String>> parseExcelFile(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException(Constants.BULK_UPLOAD_EMPTY_FILE);
            }
            Row headerRow = rowIterator.next();
            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers[i] = (cell != null) ? getCellStringValue(cell).trim().toLowerCase().replace(" ", "").replace("_", "") : "";
            }

            validateHeaders(headers);

            List<Map<String, String>> rows = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                boolean isEmpty = true;
                Map<String, String> rowMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.getCell(i);
                    String value = (cell != null) ? getCellStringValue(cell) : "";
                    if (!value.isBlank()) {
                        isEmpty = false;
                    }
                    rowMap.put(headers[i], value);
                }
                if (!isEmpty) {
                    rows.add(rowMap);
                }
            }
            return rows;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(Constants.BULK_UPLOAD_PARSE_ERROR, e);
        }
    }

    /**
     * Validates that the required columns exist in the headers.
     */
    private void validateHeaders(String[] headers) {
        Set<String> headerSet = new HashSet<>();
        for (String h : headers) {
            headerSet.add(h);
        }
        if (!headerSet.contains("symbol") || !headerSet.contains("name") || !headerSet.contains("exchange")) {
            throw new IllegalArgumentException(Constants.BULK_UPLOAD_MISSING_HEADERS);
        }
    }

    /**
     * Gets a cell's value as a trimmed string regardless of the cell type.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue();
            default -> "";
        };
    }

    /**
     * Gets a clean value from row map by key, returns null if blank.
     */
    private String getCleanValue(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
    @Override
    @Cacheable(value = "stockById", key = "#stockId")
    public StockResponse getStockById(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.STOCK_NOT_FOUND_ID + stockId));
        return mapToResponse(stock);
    }
    @Override
    @Cacheable(value = "stockBySymbol", key = "#symbol")
    public StockResponse getStockBySymbol(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.STOCK_NOT_FOUND_SYMBOL + symbol));
        return mapToResponse(stock);
    }
    @Override
    @Cacheable(value = "allStocks", key = "'all'")
    public List<StockResponse> getAllStocks() {
        List<Stock> stocks = stockRepository.findAll();
        return stocks.stream().map(this::mapToResponse).toList();
    }
    @Override
    public PageResponse<StockResponse> getAllStocksPaged(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<Stock> stockPage = stockRepository.findAll(pageable);
        return toPageResponse(stockPage);
    }
    @Override
    @Cacheable(value = "allStocks", key = "'active'")
    public List<StockResponse> getActiveStocks() {
        List<Stock> stocks = stockRepository.findByIsActiveTrue();
        return stocks.stream().map(this::mapToResponse).toList();
    }
    @Override
    public PageResponse<StockResponse> getActiveStocksPaged(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<Stock> stockPage = stockRepository.findByIsActiveTruePaged(pageable);
        return toPageResponse(stockPage);
    }
    @Override
    public PageResponse<StockResponse> searchStocksWithFilters(String query, String sector, String exchange, String marketType,
            BigDecimal minPrice, BigDecimal maxPrice, Long minVolume, Long maxVolume,
            int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<Stock> stockPage = stockRepository.searchStocksWithFilters(query, sector, exchange, marketType, minPrice, maxPrice,
                minVolume, maxVolume, pageable);
        return toPageResponse(stockPage);
    }
    @Override
    public PageResponse<StockResponse> searchAllStocksWithFilters(String query, String sector, String exchange, String marketType,
            BigDecimal minPrice, BigDecimal maxPrice, Long minVolume, Long maxVolume,
            int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<Stock> stockPage = stockRepository.searchAllStocksWithFilters(query, sector, exchange, marketType, minPrice, maxPrice,
                minVolume, maxVolume, pageable);
        return toPageResponse(stockPage);
    }
    @Override
    @Caching(evict = {
            @CacheEvict(value = "allStocks", allEntries = true),
            @CacheEvict(value = "stockById", allEntries = true),
            @CacheEvict(value = "stockBySymbol", allEntries = true)
    })
    public StockResponse updateStock(Long stockId, StockRequest stockRequest, MultipartFile image) {

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.STOCK_NOT_FOUND_ID + stockId));

        if (!stock.getSymbol().equals(stockRequest.getSymbol()) &&
                stockRepository.existsBySymbol(stockRequest.getSymbol())) {
            throw new IllegalArgumentException(
                    String.format(Constants.STOCK_DUPLICATE_SYMBOL, stockRequest.getSymbol()));
        }

        stockMapper.updateEntityFromRequest(stockRequest, stock);

        if (image != null && !image.isEmpty()) {
            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new IllegalArgumentException(Constants.STOCK_IMAGE_TYPE_ERROR);
            }

            try {
                Path uploadPath = Paths.get(stocksDir).toAbsolutePath().normalize();

                if (stock.getImage() != null) {
                    Path oldFilePath = uploadPath.resolve(stock.getImage()).normalize();
                    Files.deleteIfExists(oldFilePath);
                }

                String extension = resolveExtension(contentType);
                String fileName = "stock_" + stock.getStockId() + extension;
                Path filePath = uploadPath.resolve(fileName);

                image.transferTo(filePath.toFile());

                stock.setImage(fileName);

            } catch (Exception e) {
                throw new RuntimeException(Constants.STOCK_IMAGE_UPLOAD_ERROR, e);
            }
        }

        Stock updatedStock = stockRepository.save(stock);

        return mapToResponse(updatedStock);
    }
    @Override
    @Caching(evict = {
            @CacheEvict(value = "allStocks", allEntries = true),
            @CacheEvict(value = "stockById", allEntries = true),
            @CacheEvict(value = "stockBySymbol", allEntries = true)
    })
    public StockResponse toggleStockStatus(Long stockId) {

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.STOCK_NOT_FOUND_ID + stockId));

        stock.setIsActive(!stock.getIsActive());
        Stock updatedStock = stockRepository.save(stock);

        return mapToResponse(updatedStock);
    }
    @Override
    @Caching(evict = {
            @CacheEvict(value = "allStocks", allEntries = true),
            @CacheEvict(value = "stockById", allEntries = true),
            @CacheEvict(value = "stockBySymbol", allEntries = true)
    })
    public void deleteStock(Long stockId) {

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.STOCK_NOT_FOUND_ID + stockId));

        if (stock.getImage() != null) {
            try {
                Path uploadPath = Paths.get(stocksDir).toAbsolutePath().normalize();
                Path filePath = uploadPath.resolve(stock.getImage()).normalize();
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.error("Failed to delete stock image: {}", stock.getImage());
            }
        }
        stockRepository.delete(stock);
    }
    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}

