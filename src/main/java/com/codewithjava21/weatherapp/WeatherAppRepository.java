package com.codewithjava21.weatherapp;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherAppRepository extends CassandraRepository<WeatherEntity,WeatherPrimaryKey> {

}
