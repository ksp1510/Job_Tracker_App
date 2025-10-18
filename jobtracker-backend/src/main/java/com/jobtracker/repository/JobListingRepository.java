// Job Listing Repository
package com.jobtracker.repository;

import com.jobtracker.model.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.Instant;
import java.util.List;

public interface JobListingRepository extends MongoRepository<JobListing, String> {
    
    @Query("{ 'isActive': true }")
    Page<JobListing> findActiveJobs(Pageable pageable);

    @Query("{ 'isActive': true, 'title': { : ?0, : 'i' }, 'location': { : ?1, : 'i' } }")
    Page<JobListing> findByTitleAndLocationContainingIgnoreCase(String title, String location, Pageable pageable);
    
    @Query("{ 'isActive': true, 'title': { : ?0, : 'i' } }")
    Page<JobListing> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    @Query("{ 'isActive': true, 'location': { : ?0, : 'i' } }")
    Page<JobListing> findByLocationContainingIgnoreCase(String location, Pageable pageable);
    
    @Query("{ 'isActive': true, 'company': { : ?0, : 'i' } }")
    Page<JobListing> findByCompanyContainingIgnoreCase(String company, Pageable pageable);
    
    @Query("{ 'isActive': true, 'skills': { : ?0 } }")
    Page<JobListing> findBySkillsIn(List<String> skills, Pageable pageable);
    
    @Query("{ 'isActive': true, 'salary': { : ?0, : ?1 } }")
    Page<JobListing> findBySalaryBetween(Double minSalary, Double maxSalary, Pageable pageable);
    
    @Query("{ 'isActive': true, 'location': { : true }, 'coordinates': { : { : { type: 'Point', coordinates: [?0, ?1] }, : ?2 } } }")
    List<JobListing> findNearbyJobs(double lon, double lat, double maxDistanceMeters);

    List<JobListing> findByFetchedAtBefore(Instant cutoffTime);

    @Query("{ 'isActive': true, 'location': { : true }, 'coordinates': { : { : { type: 'Point', coordinates: [?0, ?1] }, : ?2 } } }")
    List<JobListing> findByLocationNear(Point geoPoint, Distance distance);

    @Query("{ 'isActive': true, 'title': { : ?0, : 'i' }, 'location': { : ?1, : 'i' } }")
    Page<JobListing> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(String trim, String trim2,
            Pageable pageable);
}