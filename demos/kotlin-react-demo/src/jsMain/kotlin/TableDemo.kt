/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import kotlin.uuid.Uuid
import it.neckar.react.common.table.*
import it.neckar.uuid.randomForEnvironment
import react.*
import react.dom.*

val TableDemo: FC<TableDemoProps> = fc("TableDemo") { props ->
  div("container") {
    div("row") {
      div("col") {
        h3 {
          +"Table"
        }

        val idHeaderColumn: TableHeaderColumn<MyTableRow> = TableHeaderColumn("idHeaderColumn", "da ID") {
          {
            p {
              +it.id.toString()
            }
          }
        }

        val nameDataColumn: TableDataColumn<MyTableRow> = TableDataColumn("daAgeColumnId", "da age") {
          {
            p {
              +it.name
            }
          }
        }

        val ageDataColumn: TableDataColumn<MyTableRow> = TableDataColumn("daAgeColumnId", "da age") {
          {
            p {
              +it.age.toString()
            }
          }
        }

        val columns: List<TableColumn<MyTableRow>> = listOf(idHeaderColumn, nameDataColumn, ageDataColumn)

        val tableEntries = useMemo {
          listOf(
            MyTableRow(Uuid.randomForEnvironment(), "daName1", 42),
            MyTableRow(Uuid.randomForEnvironment(), "daName2", 43),
            MyTableRow(Uuid.randomForEnvironment(), "daName3", 44),
            MyTableRow(Uuid.randomForEnvironment(), "daName4", 45),
            MyTableRow(Uuid.randomForEnvironment(), "daName5", 46),
            MyTableRow(Uuid.randomForEnvironment(), "daName6", 47),
            MyTableRow(Uuid.randomForEnvironment(), "daName7", 48),
            MyTableRow(Uuid.randomForEnvironment(), "daName8", 49),
            MyTableRow(Uuid.randomForEnvironment(), "daName9", 50),
            MyTableRow(Uuid.randomForEnvironment(), "daName10", 51),
            MyTableRow(Uuid.randomForEnvironment(), "daName11", 52),
            MyTableRow(Uuid.randomForEnvironment(), "daName12", 53),
            MyTableRow(Uuid.randomForEnvironment(), "daName13", 54),
            MyTableRow(Uuid.randomForEnvironment(), "daName14", 55),
            MyTableRow(Uuid.randomForEnvironment(), "daName15", 56),
            MyTableRow(Uuid.randomForEnvironment(), "daName16", 57),
            MyTableRow(Uuid.randomForEnvironment(), "daName17", 58),
            MyTableRow(Uuid.randomForEnvironment(), "daName18", 59),
            MyTableRow(Uuid.randomForEnvironment(), "daName19", 60),
            MyTableRow(Uuid.randomForEnvironment(), "daName20", 61),
            MyTableRow(Uuid.randomForEnvironment(), "daName21", 62),
            MyTableRow(Uuid.randomForEnvironment(), "daName22", 63),
            MyTableRow(Uuid.randomForEnvironment(), "daName23", 64),
            MyTableRow(Uuid.randomForEnvironment(), "daName24", 65),
            MyTableRow(Uuid.randomForEnvironment(), "daName25", 66),
            MyTableRow(Uuid.randomForEnvironment(), "daName26", 67),
            MyTableRow(Uuid.randomForEnvironment(), "daName27", 68),
            MyTableRow(Uuid.randomForEnvironment(), "daName28", 69),
            MyTableRow(Uuid.randomForEnvironment(), "daName29", 70),
            MyTableRow(Uuid.randomForEnvironment(), "daName30", 71),
            MyTableRow(Uuid.randomForEnvironment(), "daName31", 72),
            MyTableRow(Uuid.randomForEnvironment(), "daName32", 73),
            MyTableRow(Uuid.randomForEnvironment(), "daName33", 74),
            MyTableRow(Uuid.randomForEnvironment(), "daName34", 75),
            MyTableRow(Uuid.randomForEnvironment(), "daName35", 76),
            MyTableRow(Uuid.randomForEnvironment(), "daName36", 77),
            MyTableRow(Uuid.randomForEnvironment(), "daName37", 78),
            MyTableRow(Uuid.randomForEnvironment(), "daName38", 79),
            MyTableRow(Uuid.randomForEnvironment(), "daName39", 80),
            MyTableRow(Uuid.randomForEnvironment(), "daName40", 81),
            MyTableRow(Uuid.randomForEnvironment(), "daName41", 82),
            MyTableRow(Uuid.randomForEnvironment(), "daName42", 83),
            MyTableRow(Uuid.randomForEnvironment(), "daName43", 84),
            MyTableRow(Uuid.randomForEnvironment(), "daName44", 85),
            MyTableRow(Uuid.randomForEnvironment(), "daName45", 86),
          )
        }

        val initiallySortedBy: SortedByFunction<MyTableRow> = SortedByFunction(
          sortedBy = SortedBy.SortedAscending,
          sortedColumn = nameDataColumn,
        )

        tableWithPagination(
          entries = tableEntries,
          columns = columns,
          initialPageSize = 10,
          initiallySortedBy = initiallySortedBy,
        )
      }
    }
  }
}

external interface TableDemoProps : Props {
  var daName: String
}

data class MyTableRow(
  val id: Uuid,
  val name: String,
  val age: Int,
)
