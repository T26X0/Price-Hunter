package com.pricehunter.city;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Предоставляет публичное read-only чтение справочника городов. */
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    /** Возвращает города в детерминированном порядке для селектора frontend. */
    @Transactional(readOnly = true)
    public List<CityResponse> findAll() {
        return cityRepository.findAllByOrderByNameAscIdAsc().stream().map(CityResponse::from).toList();
    }
}
