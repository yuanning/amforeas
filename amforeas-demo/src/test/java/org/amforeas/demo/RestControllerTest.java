/**
 * Copyright (C) Alejandro Ayuso
 *
 * This file is part of Amforeas. Amforeas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Amforeas is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Amforeas. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amforeas.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import amforeas.AmforeasUtils;
import amforeas.RestController;
import amforeas.SingletonFactory;
import amforeas.demo.Demo;
import amforeas.demo.DemoSingletonFactory;
import amforeas.exceptions.AmforeasBadRequestException;
import amforeas.exceptions.StartupException;
import amforeas.jdbc.LimitParam;
import amforeas.jdbc.OrderParam;
import amforeas.rest.xstream.ErrorResponse;
import amforeas.rest.xstream.HeadResponse;
import amforeas.rest.xstream.Pagination;
import amforeas.rest.xstream.Row;
import amforeas.rest.xstream.SuccessResponse;

/**
 *
 */
@Tag("sql-tests")
public class RestControllerTest {

    private static final Logger l = LoggerFactory.getLogger(RestControllerTest.class);

    RestController controller = new RestController("demo1");
    LimitParam limit = new LimitParam();
    OrderParam order = new OrderParam();

    @BeforeAll
    public static void setUp () throws StartupException {
        SingletonFactory factory = new DemoSingletonFactory();
        factory.getConfiguration();
    }

    @AfterAll
    public static void tearDownClass () throws Exception {
        SingletonFactory factory = new DemoSingletonFactory();
        Demo.destroyDemoDatabases(factory.getConfiguration().getDatabases());
        factory.resetConfiguration();
    }

    @Test
    public void testRestController () {
        try {
            new RestController(null);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            new RestController("");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            new RestController(" ");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            new RestController("noway");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetDatabaseMetadata () {
        SuccessResponse r = (SuccessResponse) controller.getDatabaseMetadata();
        testSuccessResponse(r, Response.Status.OK, 10);
    }

    @Test
    public void testGetResourceMetadata () {
        HeadResponse r = (HeadResponse) controller.getResourceMetadata("users");
        assertEquals(Response.Status.OK, r.getStatus());
        assertTrue(r.isSuccess());
        assertEquals(7, r.getRows().size());

        r = (HeadResponse) controller.getResourceMetadata("MAKER_STATS_2010");
        assertEquals(Response.Status.OK, r.getStatus());
        assertTrue(r.isSuccess());
        assertEquals(3, r.getRows().size());

        ErrorResponse err = (ErrorResponse) controller.getResourceMetadata(null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getResourceMetadata("");
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getResourceMetadata("this table doesnt exists");
        testErrorResponse(err, Response.Status.BAD_REQUEST, "42501", Integer.valueOf(-5501));
    }

    @Test
    public void testReadResource () {
        doTestRead(null);

        // test a table with a custom column
        order.setColumn("cid");
        var r = (SuccessResponse) controller.getResource("car", "cid", "1", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);

        r = (SuccessResponse) controller.getResource("car", "transmission", "Automatic", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);
    }

    @Test
    public void testReadResource_withCols () {
        doTestRead("name");
        doTestRead("name, age");
        doTestRead("name,age");
    }

    private void doTestRead (String cols) {
        var r = (SuccessResponse) controller.getResource("users", "id", "0", limit, order, cols);
        testSuccessResponse(r, Response.Status.OK, 1);

        r = (SuccessResponse) controller.getResource("users", "name", "foo", limit, order, cols);
        testSuccessResponse(r, Response.Status.OK, 1);

        r = (SuccessResponse) controller.getResource("users", "birthday", "1992-01-15", limit, order, cols);
        testSuccessResponse(r, Response.Status.OK, 1);

        r = (SuccessResponse) controller.getResource("users", "age", "33", limit, order, cols);
        testSuccessResponse(r, Response.Status.OK, 1);

        r = (SuccessResponse) controller.getResource("users", "credit", "32.5", limit, order, cols);
        testSuccessResponse(r, Response.Status.OK, 1);

        var err = (ErrorResponse) controller.getResource("", "id", "0", limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getResource(null, null, null, limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        // fails if we try for a non-existing resource.
        err = (ErrorResponse) controller.getResource("users", "id", "1999", limit, order, cols);
        testErrorResponse(err, Response.Status.NOT_FOUND, null, null);

        err = (ErrorResponse) controller.getResource("users", "id", "not an integer", limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, "22018", -3438);

        err = (ErrorResponse) controller.getResource("users", "name", "1999", limit, order, cols);
        testErrorResponse(err, Response.Status.NOT_FOUND, null, null);

        err = (ErrorResponse) controller.getResource("users", "id", "", limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getResource("users", "id", null, limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getResource("users", "", null, limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getResource("users", null, null, limit, order, cols);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);


    }

    @Test
    public void testReadAllResources () {
        limit = new LimitParam();
        order = new OrderParam();
        var r = (SuccessResponse) controller.getAllResources("maker", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 25);
        testPagination(r, Pagination.of(limit, 25, 62));

        order.setColumn("cid");
        r = (SuccessResponse) controller.getAllResources("car", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 3);
        testPagination(r, Pagination.of(limit, 3, 3));

        order.setColumn("id");
        var err = (ErrorResponse) controller.getAllResources("no_exists", limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, "42501", Integer.valueOf(-5501));

        err = (ErrorResponse) controller.getAllResources("", limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.getAllResources(null, limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        r = (SuccessResponse) controller.getAllResources("empty", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 0);
        testPagination(r, Pagination.of(limit, 0, 0));

        // test a view
        r = (SuccessResponse) controller.getAllResources("maker_stats_2010", new LimitParam(1000), new OrderParam("month"), null);
        testSuccessResponse(r, Response.Status.OK, 744);
        testPagination(r, Pagination.of(new LimitParam(1000), 744, 744));
    }

    @Test
    public void testFindByDynamicFinder () {
        testDynamicFinder("users", "findAllByAgeBetween", "18", "99");
        testDynamicFinder("users", "findAllByBirthdayBetween", "1992-01-01", "1992-12-31");

        order.setColumn("cid");
        testDynamicFinder("car", "findAllByFuelIsNull", 1);
        testDynamicFinder("car", "findAllByFuelIsNotNull", 2);

        order.setColumn("id");
        testDynamicFinder("users", "findAllByCreditGreaterThan", "0");
        testDynamicFinder("users", "findAllByCreditGreaterThanEquals", "0");
        testDynamicFinder("users", "findAllByCreditLessThanEquals", "0");
        testDynamicFinder("sales_stats", "findAllByLast_updateBetween", 6, "2000-01-01T00:00:00.000Z", "2000-06-01T23:55:00.000Z");

        var err = (ErrorResponse) controller.findByDynamicFinder("users", "findAllByCreditLessThan", Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.NOT_FOUND, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("users", "findAllByCreditLessThhhhan", Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("users", "findAllByCreditLessThhhhan", Arrays.asList(new String[] {""}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("users", "", Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("users", null, Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("", "", Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder(null, "", Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("", null, Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder(null, null, Arrays.asList(new String[] {"0"}), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findByDynamicFinder("users", "findAllByCreditLessThan", new ArrayList<String>(), limit, order);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, 0);

        try {
            controller.findByDynamicFinder("users", "findAllByCreditLessThan", null, limit, order);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        limit = new LimitParam(1000);
        order = new OrderParam("month");
        testDynamicFinder("maker_stats_2010", "findAllByMakerLikeAndMonthLessThanEquals", 24, "A%", "4");
    }

    @Test
    public void testCreateResource () {
        var newMock = UserMock.getRandomInstance();
        var r = (SuccessResponse) controller.insertResource("users", "id", newMock.toJSON());
        testSuccessResponse(r, Response.Status.CREATED, 1);

        r = (SuccessResponse) controller.getResource("users", "name", newMock.name, limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);

        newMock = UserMock.getRandomInstance();
        r = (SuccessResponse) controller.insertResource("users", "id", newMock.toMap());
        testSuccessResponse(r, Response.Status.CREATED, 1);

        r = (SuccessResponse) controller.getResource("users", "name", newMock.name, limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);

        var err = (ErrorResponse) controller.insertResource("users", "id", "");
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.insertResource("users", "id", new HashMap<String, String>());
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.insertResource("", "id", new HashMap<String, String>());
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.insertResource(null, "id", new HashMap<String, String>());
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.insertResource("users", "", new HashMap<String, String>());
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.insertResource("users", null, new HashMap<String, String>());
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        newMock = UserMock.getRandomInstance();
        Map<String, String> wrongUserParams = newMock.toMap();
        wrongUserParams.put("birthday", "0000"); // in wrong format
        err = (ErrorResponse) controller.insertResource("users", "id", wrongUserParams);
        testErrorResponse(err, Response.Status.BAD_REQUEST, "42561", Integer.valueOf(-5561));

        wrongUserParams = newMock.toMap();
        wrongUserParams.put("age", null); // age can be null
        r = (SuccessResponse) controller.insertResource("users", "id", wrongUserParams);
        testSuccessResponse(r, Response.Status.CREATED, 1);

        wrongUserParams = newMock.toMap();
        wrongUserParams.put("age", ""); // age can't be empty
        err = (ErrorResponse) controller.insertResource("users", "id", wrongUserParams);
        testErrorResponse(err, Response.Status.BAD_REQUEST, "22018", Integer.valueOf(-3438));

        wrongUserParams = newMock.toMap();
        wrongUserParams.put("name", null); // name can't be null
        err = (ErrorResponse) controller.insertResource("users", "id", wrongUserParams);
        testErrorResponse(err, Response.Status.BAD_REQUEST, "23502", Integer.valueOf(-10));

        wrongUserParams = newMock.toMap();
        wrongUserParams.put("name", ""); // name can be empty
        r = (SuccessResponse) controller.insertResource("users", "id", wrongUserParams);
        testSuccessResponse(r, Response.Status.CREATED, 1);
        r = (SuccessResponse) controller.getResource("users", "name", newMock.name, limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);
    }

    @Test
    public void testUpdateResource () {
        var r = (SuccessResponse) controller.updateResource("users", "id", "0", "{\"age\":\"90\"}");
        testSuccessResponse(r, Response.Status.OK, 1);

        var err = (ErrorResponse) controller.updateResource("users", "id", "0", "{\"age\":\"\"}"); // age can't be empty
        testErrorResponse(err, Response.Status.BAD_REQUEST, "22018", Integer.valueOf(-3438));

        err = (ErrorResponse) controller.updateResource("users", "id", "0", "{\"age\":}"); // invalid json
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.updateResource("users", "id", "0", "{\"age\":\"90\", \"birthday\":00X0}"); // invalid date
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.updateResource("users", "id", "0", "{\"age\":\"90\", \"birthday\":\"00X0\"}"); // invalid date
        testErrorResponse(err, Response.Status.BAD_REQUEST, "22007", Integer.valueOf(-3407));

        err = (ErrorResponse) controller.updateResource(null, "id", "0", "{\"age\":\"90\"}");
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.updateResource("users", "id", "9999", "{\"age\":\"90\"}");
        testErrorResponse(err, Response.Status.NO_CONTENT, null, null);

        r = (SuccessResponse) controller.updateResource("car", "cid", "0", "{\"model\":\"Test$%&·$&%·$/()=?¿Model\"}"); // custom id
        testSuccessResponse(r, Response.Status.OK, 1);

        r = (SuccessResponse) controller.updateResource("maker_stats_2010", "month", "3", "{\"sales\":0}");
        testSuccessResponse(r, Response.Status.OK, 1);
    }

    @Test
    public void testRemoveResource () {
        var newMock = UserMock.getRandomInstance();
        var r = (SuccessResponse) controller.insertResource("users", "id", newMock.toJSON());
        testSuccessResponse(r, Response.Status.CREATED, 1);

        r = (SuccessResponse) controller.getResource("users", "name", newMock.name, limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);

        var id = getId(r, "id");
        assertNotNull(id);

        r = (SuccessResponse) controller.deleteResource("users", "id", id);
        testSuccessResponse(r, Response.Status.OK, 1);

        var err = (ErrorResponse) controller.deleteResource("users", "id", "");
        testErrorResponse(err, Response.Status.BAD_REQUEST, "22018", Integer.valueOf(-3438));

        err = (ErrorResponse) controller.deleteResource("users", "id", null);
        testErrorResponse(err, Response.Status.NO_CONTENT, null, null);

        err = (ErrorResponse) controller.deleteResource("users", "", "32");
        testErrorResponse(err, Response.Status.NO_CONTENT, null, null);

        err = (ErrorResponse) controller.deleteResource(null, "", "32");
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);
    }

    @Test
    public void testFindResources () {
        var r = (SuccessResponse) controller.findResources("comments", "car_id", "0", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 2);
        testPagination(r, Pagination.of(limit, 2, 4));

        r = (SuccessResponse) controller.findResources("comments", "car_id", "2", limit, order, null);
        testSuccessResponse(r, Response.Status.OK, 1);
        testPagination(r, Pagination.of(limit, 1, 4));

        r = (SuccessResponse) controller.findResources("maker_stats_2010", "maker", "FIAT", limit, new OrderParam("month"), null);
        testSuccessResponse(r, Response.Status.OK, 12);
        testPagination(r, Pagination.of(limit, 12, 744));

        var err = (ErrorResponse) controller.findResources("comments", "car_id", "1", limit, order, null);
        testErrorResponse(err, Response.Status.NOT_FOUND, null, null);

        err = (ErrorResponse) controller.findResources("comments", "car_id_grrr", "2", limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, "42501", Integer.valueOf(-5501));

        err = (ErrorResponse) controller.findResources("comments", "car_id", "", limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findResources("comments", "", "0", limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);

        err = (ErrorResponse) controller.findResources("", "id", "0", limit, order, null);
        testErrorResponse(err, Response.Status.BAD_REQUEST, null, null);
    }

    @Test
    public void testSimpleFunction () throws AmforeasBadRequestException {
        var r = (SuccessResponse) controller.executeStoredProcedure("simpleStoredProcedure", "[]");
        testSuccessResponse(r, Response.Status.OK, 1);
    }

    @Test
    public void testSimpleStoredProcedure () throws AmforeasBadRequestException {
        var json =
            "[{\"value\":4,\"name\":\"car_id\",\"outParameter\":false,\"type\":\"INTEGER\",\"index\":1},{\"value\":\"This is a comment\",\"name\":\"comment\",\"outParameter\":false,\"type\":\"VARCHAR\",\"index\":2}]";
        AmforeasUtils.getStoredProcedureParamsFromJSON(json);
        var r = (SuccessResponse) controller.executeStoredProcedure("insert_comment", json);
        testSuccessResponse(r, Response.Status.OK, 0);
    }

    @Test
    public void testComplexStoredProcedure () throws AmforeasBadRequestException {
        var json =
            "[{\"value\":2010,\"name\":\"in_year\",\"outParameter\":false,\"type\":\"INTEGER\",\"index\":1},{\"name\":\"out_total\",\"outParameter\":true,\"type\":\"INTEGER\",\"index\":2}]";
        AmforeasUtils.getStoredProcedureParamsFromJSON(json);
        var r = (SuccessResponse) controller.executeStoredProcedure("get_year_sales", json);
        testSuccessResponse(r, Response.Status.OK, 1);
        assertEquals("12", r.getRows().get(0).getCells().get("out_total"));
    }

    private void testErrorResponse (ErrorResponse err, Response.Status expectedStatus, String expectedSqlState,
        Integer expectedSqlCode) {
        assertEquals(expectedStatus, err.getStatus());
        assertNotNull(err.getMessage());
        assertFalse(err.isSuccess());
        assertEquals(expectedSqlState, err.getSqlState());
        assertEquals(expectedSqlCode, err.getSqlCode());
        l.debug(err.getMessage());
    }

    private void testSuccessResponse (SuccessResponse r, Response.Status expectedStatus) {
        assertEquals(expectedStatus, r.getStatus());
        assertTrue(r.isSuccess());
    }

    private void testSuccessResponse (SuccessResponse r, Response.Status expectedStatus, int expectedResults) {
        List<Row> rows = r.getRows();
        assertEquals(expectedStatus, r.getStatus());
        assertTrue(r.isSuccess());
        assertEquals(expectedResults, rows.size());
    }

    private void testPagination (SuccessResponse r, Pagination p) {
        if (p == null) {
            assertNull(r.getPagination());
        } else {
            assertEquals(r.getPagination(), p);
        }
    }

    private void testDynamicFinder (String resource, String finder, String... arr) {
        var r = (SuccessResponse) controller.findByDynamicFinder(resource, finder, Arrays.asList(arr), limit, order);
        testSuccessResponse(r, Response.Status.OK);
    }

    private void testDynamicFinder (String resource, String finder, int expectedResults, String... arr) {
        var r = (SuccessResponse) controller.findByDynamicFinder(resource, finder, Arrays.asList(arr), limit, order);
        testSuccessResponse(r, Response.Status.OK, expectedResults);
    }

    private String getId (final SuccessResponse response, final String id) {
        for (Row row : response.getRows()) {
            for (String k : row.getCells().keySet()) {
                if (k.equalsIgnoreCase(id)) {
                    return row.getCells().get(k).toString();
                }
            }
        }
        return null;
    }
}
