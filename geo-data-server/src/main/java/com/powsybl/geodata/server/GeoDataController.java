/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server;

import com.powsybl.geodata.server.dto.LineGeoData;
import com.powsybl.geodata.server.dto.SubstationGeoData;
import com.powsybl.geodata.server.repositories.*;
import com.powsybl.geodata.server.utils.PaginationUtils;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RestController
@RequestMapping(value = GeoDataController.API_VERSION)
@Api(value = "Geo data")
@ComponentScan(basePackageClasses = {GeoDataController.class, GeoDataService.class, NetworkStoreService.class})
public class GeoDataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataController.class);

    static final String API_VERSION = "v1";

    @Autowired
    private GeoDataService geoDataService;

    @Autowired
    private NetworkStoreService networkStoreService;

    @Autowired
    private SubstationsRepository substationsRepository;

    @Autowired
    private LinesRepository linesRepository;

    @Autowired
    private LinesCustomRepository linesCustomRepository;


    // End points to get data either by pagination mechanism or not

    @GetMapping(value = "lines/{idNetwork}")
    @ApiOperation(value = "Get Network lines graphics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGeoData>> getLinesGraphics(@PathVariable UUID idNetwork,
                                                              @RequestParam(name = "pagination", defaultValue = "false") boolean pagination,
                                                              @RequestParam(name = "page", defaultValue = "1") int page,
                                                              @RequestParam(name = "size", defaultValue = "100") int size) {
        Network network = networkStoreService.getNetwork(idNetwork);
        List<LineGeoData> lines = new ArrayList<>(geoDataService.getNetworkLinesCoordinates(network).values());
        if (pagination) {
            return ResponseEntity.ok().body(PaginationUtils.getSublist(lines, page, size));
        } else {
            return ResponseEntity.ok().body(lines);
        }
    }

    @GetMapping(value = "substations/{idNetwork}")
    @ApiOperation(value = "Get Network substations graphics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of substations graphics")})
    public ResponseEntity<List<SubstationGeoData>> getSubstationsGraphic(@PathVariable UUID idNetwork,
                                                                         @RequestParam(name = "pagination", defaultValue = "false") boolean pagination,
                                                                         @RequestParam(name = "page", defaultValue = "1") int page,
                                                                         @RequestParam(name = "size", defaultValue = "100") int size) {
        Network network = networkStoreService.getNetwork(idNetwork);
        List<SubstationGeoData> substations = new ArrayList<>(geoDataService.getSubstationsCoordinates(network).values());
        if (pagination) {
            return ResponseEntity.ok().body(PaginationUtils.getSublist(substations, page, size));
        } else {
            return ResponseEntity.ok().body(substations);
        }
    }

    // End points to get data based on the nominal voltage

    @GetMapping(value = "lines/{idNetwork}/{voltage}")
    @ApiOperation(value = "Get Network lines graphics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGeoData>> getLinesGraphicsByVoltage(@PathVariable UUID idNetwork, @PathVariable int voltage) {
        Network network = networkStoreService.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(geoDataService.getNetworkLinesCoordinates(network, voltage).values()));
    }

    @GetMapping(value = "lines-basic/{idNetwork}/{voltage}")
    @ApiOperation(value = "Get Network lines graphics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of known lines graphics")})
    public ResponseEntity<List<LineGeoData>> getLinesGraphicsByVoltageLightVersion(@PathVariable UUID idNetwork, @PathVariable int voltage) {
        Network network = networkStoreService.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(geoDataService.getKnownNetworkLinesCoordinates(network, voltage).values()));
    }

    // End points to get just the raw data

    @GetMapping(value = "lines-basic/{idNetwork}")
    @ApiOperation(value = "Get Network lines graphics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of known lines graphics")})
    public ResponseEntity<List<LineGeoData>> getLinesGraphicsLightVersion(@PathVariable UUID idNetwork) {
        Network network = networkStoreService.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(geoDataService.getKnownNetworkLinesCoordinates(network).values()));
    }

    // End points to save or update data into the database

    @PostMapping(value = "substations")
    @ApiOperation(value = "Save/Update substations")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "substations Saved")})
    public ResponseEntity<Void> saveSubstations(@RequestBody List<SubstationGeoData> substationGeoData) {
        LOGGER.info("/substations [POST] : Save Substations request received");
        List<SubstationEntity> substationEntities = new ArrayList<>();
        substationGeoData.forEach(s -> substationEntities.add(SubstationEntity.builder()
                .country(s.getCountry().toString())
                .substationID(s.getId())
                .voltages(s.getVoltages())
                .coordinate(CoordinateEntity.builder().lat(s.getPosition().getLat()).lon(s.getPosition().getLon()).build())
                .build()));
        substationsRepository.saveAll(substationEntities);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "lines")
    @ApiOperation(value = "Save/Update lines")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "lines Saved")})
    public ResponseEntity<Void> saveLines(@RequestBody List<LineGeoData> linesGraphics) {
        LOGGER.info("/lines [POST] : Save Lines request received");
        List<String> savedLines = new ArrayList<>(linesCustomRepository.getAllLines().keySet());
        // ignore lines that exist and calculated
        List<LineGeoData> filteredLinesGraphics = linesGraphics.stream().filter(lg -> !savedLines.contains(lg.getId())).collect(Collectors.toList());

        List<LineEntity> linesEntities = new ArrayList<>();
        filteredLinesGraphics.forEach(l -> linesEntities.add(LineEntity.builder()
                .country(l.getCountry().toString())
                .voltage(l.getVoltage())
                .lineID(l.getId())
                .aerial(l.isAerial())
                .ordered(false)
                .coordinates(l.getCoordinates().stream()
                        .map(p -> CoordinateEntity.builder().lat(p.getLat()).lon(p.getLon()).build())
                        .collect(Collectors.toList()))
                .build()));

        linesRepository.saveAll(linesEntities);
        return ResponseEntity.ok().build();
    }

    // End points to force calculate the unordered lines of a given network

    @GetMapping(value = "precalculate-lines/{idNetwork}")
    @ApiOperation(value = "Get Network lines graphics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGeoData>> precalculateLines(@PathVariable UUID idNetwork) {
        Network network = networkStoreService.getNetwork(idNetwork);
        geoDataService.precalculateLines(network);
        return ResponseEntity.ok().build();
    }
}
