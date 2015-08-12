/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dia.algorithms.mcc

import org.dia.Constants._
import org.dia.core.{SciSparkContext, sRDD, sciTensor}
import org.dia.sLib.mccOps

import scala.collection.mutable
import scala.language.implicitConversions

/**
  */
object Main {

  /**
   * NetCDF variables to use
   * TODO:: Make the netcdf variables global - however this may need broadcasting
   */
  val rowDim = 180
  val columnDim = 360
  val TextFile = "TestLinks"

  def main(args: Array[String]): Unit = {
    var master = ""
    val testFile = if (args.isEmpty) "TestLinks2" else args(0)
    if (args.isEmpty || args.length <= 1) master = "local[24]" else master = args(1)

    val sc = new SciSparkContext(master, "test")
    sc.setLocalProperty(ARRAY_LIB, BREEZE_LIB)

    val variable = if (args.isEmpty || args.length <= 2) "TotCldLiqH2O_A" else args(2)
    val RDDmetatuple = sc.NetcdfFile(testFile, List(variable), 1)

    val sRDD = RDDmetatuple._1
    val dateMap = RDDmetatuple._2

    println(dateMap)

    val filtered = sRDD.map(p => p(variable) <= 241.0)

    val filteredCartesian = filtered.cartesian(filtered).filter(p => Integer.parseInt(p._1.metaData("FRAME")) == (Integer.parseInt(p._2.metaData("FRAME")) + 1))

    val componentFrameRDD = filteredCartesian.flatMap(p => {
      val components1 = mccOps.findCloudComponents(p._1).filter(checkCriteria)
      val components2 = mccOps.findCloudComponents(p._2).filter(checkCriteria)
      val componentPairs = for (x <- components1; y <- components2) yield (x, y)
      val overlapped = componentPairs.filter(p => !(p._1.tensor * p._2.tensor).isZero)
      overlapped.map(p => ((p._1.metaData("FRAME"), p._1.metaData("COMPONENT")), (p._2.metaData("FRAME"), p._2.metaData("COMPONENT"))))
    })

    val collectedEdges = componentFrameRDD.collect()
    val vertex = collectedEdges.flatMap(p => List(p._1, p._2)).toSet
    println(vertex.toList.sortBy(p => p._1))
    println(vertex.size)
    println(filtered.count())
    println(collectedEdges.toList)
    println(collectedEdges.length)

  }

  def checkCriteria(p : sciTensor) : Boolean = {
    val hash = p.metaData
    val area = hash("AREA").toDouble
    val tempDiff = hash("DIFFERENCE").toDouble
    (area >= 40.0) || (area < 40.0) && (tempDiff > 10.0)
  }

  def getVertexArray(collection: sRDD[sciTensor]): mutable.HashMap[(String, String), Long] = {
    val id = collection.map(p => (p.metaData("FRAME") , p.metaData("COMPONENT"))).collect().toList
    val size = id.length
    val range = 0 to (size - 1)
    val hash = new mutable.HashMap[(String, String), Long]
    range.map(p => hash += ((id(p), p)))
    hash
  }
}

