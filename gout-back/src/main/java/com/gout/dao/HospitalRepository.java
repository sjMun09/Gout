package com.gout.dao;

import com.gout.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, String> {

    @Query("SELECT h FROM Hospital h WHERE h.isActive = true " +
           "AND (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(h.address) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Hospital> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(nativeQuery = true, value = """
        SELECT id, hira_code, name, address, phone, is_active, latitude, longitude, departments, operating_hours,
               ST_Distance(location, ST_MakePoint(:lng, :lat)::geography) AS distance_meters
        FROM hospitals
        WHERE is_active = true
        AND location IS NOT NULL
        AND ST_DWithin(location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
        AND (:keyword IS NULL OR name ILIKE CONCAT('%', :keyword, '%'))
        ORDER BY ST_Distance(location, ST_MakePoint(:lng, :lat)::geography)
        LIMIT :limit OFFSET :offset
        """)
    List<Object[]> searchByLocation(@Param("lat") double lat,
                                    @Param("lng") double lng,
                                    @Param("radiusMeters") int radiusMeters,
                                    @Param("keyword") String keyword,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    @Query(nativeQuery = true, value = """
        SELECT COUNT(*)
        FROM hospitals
        WHERE is_active = true
        AND location IS NOT NULL
        AND ST_DWithin(location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
        AND (:keyword IS NULL OR name ILIKE CONCAT('%', :keyword, '%'))
        """)
    long countByLocation(@Param("lat") double lat,
                         @Param("lng") double lng,
                         @Param("radiusMeters") int radiusMeters,
                         @Param("keyword") String keyword);
}
