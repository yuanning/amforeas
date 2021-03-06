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
package org.amforeas.sql.dialect;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import amforeas.enums.Operator;
import amforeas.jdbc.LimitParam;
import amforeas.jdbc.OrderParam;
import amforeas.sql.Select;
import amforeas.sql.SelectParam;
import amforeas.sql.dialect.HSQLDialect;

@Tag("dialect-tests")
public class HSQLDialectTest extends SQLDialectTest {

    public HSQLDialectTest() {
        d = new HSQLDialect();
    }

    @Test
    @Override
    public void testDelete () {}

    @Test
    @Override
    public void testInsert () {}

    @Test
    @Override
    public void testSelect () {
        doTest("SELECT * FROM a_table", new Select(table));

        doTest("SELECT * FROM a_table WHERE tableId = ?",
            new Select(table).setParameter(new SelectParam(table.getPrimaryKey(), Operator.EQUALS, "1")));

        doTest("SELECT * FROM a_table WHERE name = ?",
            new Select(table).setParameter(new SelectParam("name", Operator.EQUALS, "1")));

        doTest("SELECT * FROM a_table WHERE name = ? LIMIT 25 OFFSET 0", new Select(table)
            .setParameter(new SelectParam("name", Operator.EQUALS, "1")).setLimitParam(new LimitParam()));

        doTest("SELECT * FROM a_table WHERE tableId = ? ORDER BY tableId ASC LIMIT 25 OFFSET 0",
            new Select(table).setParameter(new SelectParam(table.getPrimaryKey(), Operator.EQUALS, "1"))
                .setLimitParam(l).setOrderParam(new OrderParam(table)));
    }

    @Test
    @Override
    public void testSelect_between () {
        doTest("SELECT * FROM a_table WHERE tableId BETWEEN ? AND ?",
            new Select(table).setParameter(new SelectParam(table.getPrimaryKey(), Operator.BETWEEN, "1", "2")));
    }

    @Test
    @Override
    public void testSelect_like () {
        doTest("SELECT * FROM a_table WHERE tableId LIKE ?",
            new Select(table).setParameter(new SelectParam(table.getPrimaryKey(), Operator.LIKE, "1")));
    }

    @Test
    @Override
    public void testSelect_isNull () {
        doTest("SELECT * FROM a_table WHERE tableId IS NULL",
            new Select(table).setParameter(new SelectParam(table.getPrimaryKey(), Operator.ISNULL)));
    }

    @Test
    @Override
    public void testSelect_isNotNull () {
        doTest("SELECT * FROM a_table WHERE tableId IS NOT NULL",
            new Select(table).setParameter(new SelectParam(table.getPrimaryKey(), Operator.ISNOTNULL)));
    }

    @Test
    @Override
    public void testUpdate () {}

    @Test
    @Override
    public void test_rowCountStatement () {}

    @Test
    @Override
    public void testSelect_columns () {}
}
