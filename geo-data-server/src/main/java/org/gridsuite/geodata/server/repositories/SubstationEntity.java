/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import org.gridsuite.geodata.extensions.Coordinate;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import com.powsybl.iidm.network.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Table("substations")
@AllArgsConstructor
@Getter
@Builder
@ToString
public class SubstationEntity {

    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String country;

    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String id;

    private CoordinateEntity coordinate;

    public static SubstationEntity create(SubstationGeoData s) {
        return SubstationEntity.builder()
                .country(s.getCountry().toString())
                .id(s.getId())
                .coordinate(CoordinateEntity.builder()
                        .lat(s.getCoordinate().getLat())
                        .lon(s.getCoordinate().getLon())
                        .build())
                .build();
    }

    public SubstationGeoData toGeoData() {
        return SubstationGeoData.builder()
                .country(Country.valueOf(country))
                .id(id)
                .coordinate(new Coordinate(coordinate.getLat(), coordinate.getLon()))
                .build();
    }
}
