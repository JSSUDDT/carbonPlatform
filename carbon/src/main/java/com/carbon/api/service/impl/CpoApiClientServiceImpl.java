package com.carbon.api.service.impl;

import com.carbon.api.dto.external.cpo.*;
import com.carbon.api.service.CpoApiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Slf4j
@Service
public class CpoApiClientServiceImpl implements CpoApiClientService {

    private final RestTemplate restTemplate;

    public CpoApiClientServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(45000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public CpoDatasetResponse fetchDataset(String apiUrl, String token, String vendorId, String startTime, String endTime, Integer page, Integer pageSize) {
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("API URL must not be empty");
        }

        int targetPage = (page != null && page > 0) ? page : 1;
        int targetPageSize = (pageSize != null && pageSize > 0) ? pageSize : 1000;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl.trim())
                .queryParam("vendorId", vendorId != null ? vendorId.trim() : "")
                .queryParam("startTime", startTime != null ? startTime.trim() : "")
                .queryParam("endTime", endTime != null ? endTime.trim() : "")
                .queryParam("page", targetPage)
                .queryParam("pageSize", targetPageSize);

        String fullUrl = builder.toUriString();
        log.info("[CPO Client] Requesting CPO API: {} (page={}, size={})", fullUrl, targetPage, targetPageSize);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (token != null && !token.trim().isEmpty()) {
            String cleanToken = token.trim();
            if (cleanToken.toLowerCase().startsWith("bearer ")) {
                cleanToken = cleanToken.substring(7).trim();
            }
            headers.setBearerAuth(cleanToken);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        int maxRetries = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<CpoDatasetResponse> response = restTemplate.exchange(
                        fullUrl,
                        HttpMethod.GET,
                        requestEntity,
                        CpoDatasetResponse.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    CpoDatasetResponse body = response.getBody();
                    log.info("[CPO Client] Response 200 OK from {}, resultCode: {}, resultMsg: {}", apiUrl, body.getResultCode(), body.getResultMsg());
                    return body;
                } else {
                    log.warn("[CPO Client] Unexpected HTTP status {} from {}", response.getStatusCode(), apiUrl);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("[CPO Client] Attempt {}/{} failed for {}: {}", attempt, maxRetries, apiUrl, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.error("[CPO Client] Failed to fetch data from CPO API: {} after {} attempts", apiUrl, maxRetries, lastException);
        CpoDatasetResponse errorResponse = new CpoDatasetResponse();
        errorResponse.setResultCode("9999");
        errorResponse.setResultMsg("CPO API Connection Failed: " + (lastException != null ? lastException.getMessage() : "Unknown Error"));
        return errorResponse;
    }

    @Override
    public CpoDatasetData fetchAllPages(String apiUrl, String token, String vendorId, String startTime, String endTime, Integer pageSize) {
        int targetPageSize = (pageSize != null && pageSize > 0) ? pageSize : 1000;
        int currentPage = 1;
        int maxPages = 50; // 防呆安全上限

        Map<String, CpoStationDto> stationMap = new LinkedHashMap<>();
        Map<String, CpoChargerDto> chargerMap = new LinkedHashMap<>();
        List<CpoTransactionDto> allTransactions = new ArrayList<>();

        while (currentPage <= maxPages) {
            CpoDatasetResponse response = fetchDataset(apiUrl, token, vendorId, startTime, endTime, currentPage, targetPageSize);
            if (response == null || !"0000".equals(response.getResultCode()) || response.getData() == null) {
                log.warn("[CPO Client] Non-0000 response or empty data at page {}: {}", currentPage, response != null ? response.getResultMsg() : "null");
                break;
            }

            CpoDatasetData data = response.getData();

            // 匯總站點
            if (data.getStations() != null) {
                for (CpoStationDto st : data.getStations()) {
                    if (st != null && st.getStationId() != null) {
                        stationMap.put(st.getStationId(), st);
                    }
                }
            }

            // 匯總設備槍頭
            if (data.getChargers() != null) {
                for (CpoChargerDto ch : data.getChargers()) {
                    if (ch != null && ch.getStationId() != null && ch.getChargerId() != null && ch.getConnectId() != null) {
                        String key = ch.getStationId() + "_" + ch.getChargerId() + "_" + ch.getConnectId();
                        chargerMap.put(key, ch);
                    }
                }
            }

            // 匯總交易
            List<CpoTransactionDto> txList = data.getTransactions();
            if (txList != null && !txList.isEmpty()) {
                allTransactions.addAll(txList);
                log.info("[CPO Client] Page {} retrieved {} transactions (Total so far: {})", currentPage, txList.size(), allTransactions.size());

                // 若當頁交易比數少於 pageSize，表示已拉取完畢
                if (txList.size() < targetPageSize) {
                    break;
                }
            } else {
                break;
            }

            currentPage++;
        }

        CpoDatasetData aggregated = new CpoDatasetData();
        aggregated.setStations(new ArrayList<>(stationMap.values()));
        aggregated.setChargers(new ArrayList<>(chargerMap.values()));
        aggregated.setTransactions(allTransactions);

        log.info("[CPO Client] Aggregated totals: {} stations, {} chargers, {} transactions",
                aggregated.getStations().size(), aggregated.getChargers().size(), aggregated.getTransactions().size());
        return aggregated;
    }
}
