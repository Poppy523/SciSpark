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
package org.dia.loaders

import java.util.Random

import org.dia.Constants._
import org.nd4j.linalg.factory.Nd4j
import org.slf4j.Logger
import ucar.nc2.dataset.NetcdfDataset

import scala.collection.mutable

object NetCDFLoader {
  // Class logger
  val LOG: Logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  /**
   * Gets an NDimensional Array of ND4j from a TRMM tensors
   * @param url where the netcdf file is located
   * @param variable the NetCDF variable to search for
   * @return
   */
  def loadNetCDFTRMMVars(url: String, variable: String): (Array[Double], Array[Int]) = {
    val netcdfFile = NetCDFUtils.loadNetCDFDataSet(url)
    val rowDim = NetCDFUtils.getDimensionSize(netcdfFile, X_AXIS_NAMES(0))
    val columnDim = NetCDFUtils.getDimensionSize(netcdfFile, Y_AXIS_NAMES(0))

    val coordinateArray = NetCDFUtils.convertMa2ArrayTo1DJavaArray(netcdfFile, variable)

    (coordinateArray, Array(rowDim, columnDim))
  }

  /**
   * Gets an NDimensional array of INDArray from a NetCDF url
   * @param url where the netcdf file is located
   * @param variable the NetCDF variable to search for
   * @return
   */
  def loadNetCDFNDVars(url: String, variable: String): (Array[Double], Array[Int]) = {
    val netcdfFile = NetCDFUtils.loadNetCDFDataSet(url)

    if (netcdfFile != null) {
      val coordinateArray = NetCDFUtils.convertMa2ArrayTo1DJavaArray(netcdfFile, variable)
      if (coordinateArray.length > 0) {
        val dims = NetCDFUtils.getDimensionSizes(netcdfFile, variable)
        var shape = dims.toArray.sortBy(_._1).map(_._2)
        if (shape.length < 2) shape = Array(1, 1)
        return (coordinateArray, shape)
      }
      LOG.warn("Variable '%s' in dataset in %s not found!".format(variable, url))
      return (Array(-9999), Array(1, 1))
    }
    LOG.warn("Variable '%s' in dataset in %s not found!".format(variable, url))
    return (Array(-9999), Array(1, 1))
  }

  def loadNetCDFVariables(url: String): List[String] = {
    val netcdfFile = NetCDFUtils.loadNetCDFDataSet(url)
    val variables = netcdfFile.getVariables
    var list: List[String] = List()
    for (i <- 0 to variables.size - 1) {
      val k = variables.get(i).getName
      list ++= List(k)
    }
    list
  }

  /**
   * Creates a 2D array from a list of dimensions using a variable
   * @param dimensionSizes hashmap of (dimension, size) pairs
   * @param netcdfFile the NetcdfDataset to read
   * @param variable the variable array to extract
   * @return DenseMatrix
   */
  def create2dArray(dimensionSizes: mutable.HashMap[Int, Int], netcdfFile: NetcdfDataset, variable: String): (Array[Double], Array[Int]) = {

    val x = dimensionSizes.get(1).get
    val y = dimensionSizes.get(2).get

    val coordinateArray = NetCDFUtils.convertMa2ArrayTo1DJavaArray(netcdfFile, variable)
    (coordinateArray, Array(x, y))
  }

  def loadRandomArray(url : String, varname : String) : (Array[Double], Array[Int]) = {
    val generator = new Random()
    generator.setSeed(url.hashCode)
    val randomCenter = generator.nextDouble * 20.0
    val randomCenterOther = generator.nextDouble * 20
    val otroRandomCenter = generator.nextDouble * 20
    val ndArray = Nd4j.zeros(20, 20)
    for(row <- 0 to ndArray.rows - 1){
      for(col <- 0 to ndArray.columns - 1){
        if (Math.pow((row - randomCenter), 2) + Math.pow((col - randomCenter), 2) <= 9) ndArray.put(row, col, generator.nextDouble * 340)
        if (Math.pow((row - randomCenterOther), 2) + Math.pow((col - randomCenterOther), 2) <= 9) ndArray.put(row, col, generator.nextDouble * 7000)
        if (Math.pow((row - otroRandomCenter), 2) + Math.pow((col - otroRandomCenter), 2) <= 9) ndArray.put(row, col, generator.nextDouble * 24000)
      }
    }
    (ndArray.data.asDouble, ndArray.shape)
  }
}
