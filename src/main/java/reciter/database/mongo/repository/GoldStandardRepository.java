package reciter.database.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import reciter.database.mongo.model.GoldStandard;

public interface GoldStandardRepository extends MongoRepository<GoldStandard, String> {

}
