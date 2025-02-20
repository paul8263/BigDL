/*
 * Copyright 2021 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.ppml.vfl.example.logisticregression

import com.intel.analytics.bigdl.dllib.feature.dataset.{DataSet, MiniBatch, Sample, SampleToMiniBatch}
import com.intel.analytics.bigdl.dllib.tensor.Tensor
import com.intel.analytics.bigdl.ppml.algorithms.PSI
import com.intel.analytics.bigdl.ppml.vfl.VflContext
import com.intel.analytics.bigdl.ppml.example.ExampleUtils
import com.intel.analytics.bigdl.ppml.algorithms.vfl.LogisticRegression
import com.intel.analytics.bigdl.ppml.utils.DataFrameUtils
import org.apache.log4j.Logger
import scopt.OptionParser

import scala.io.Source
import collection.JavaConverters._
import collection.JavaConversions._

/**
 * A two process example to simulate 2 nodes Vfl of a Neural Network
 * This example will start a FLServer first, to provide PSI algorithm
 * and store parameters as Parameter Server
 */
object VflLogisticRegression {
  var featureNum: Int = _
  val logger = Logger.getLogger(getClass)

  def getData(pSI: PSI, dataPath: String, rowKeyName: String, batchSize: Int = 4) = {
    //TODO: we use get intersection to get data and input to model
    // this do not need to be DataFrame?
    // load data from dataset and preprocess\
    val salt = pSI.getSalt()
    val spark = VflContext.getSparkSession()
    import spark.implicits._
    val df = spark.read.option("header", "true").csv(dataPath)
    val ids = df.select(rowKeyName).as[String].collect().toList
    pSI.uploadSet(ids, salt)
    val intersections = pSI.downloadIntersection()
    val intersectionSet = intersections.toSet
    val dataSet = df.filter(r => intersectionSet.contains(r.getAs[String](0)))
    // we use same dataset to train and validate in this example
    (dataSet, dataSet)

  }

  def main(args: Array[String]): Unit = {
    case class Params(dataPath: String = null,
                      rowKeyName: String = "ID",
                      learningRate: Float = 0.005f,
                      batchSize: Int = 4)
    val parser: OptionParser[Params] = new OptionParser[Params]("VFL Logistic Regression") {
      opt[String]('d', "dataPath")
        .text("data path to load")
        .action((x, params) => params.copy(dataPath = x))
        .required()
      opt[String]('r', "rowKeyName")
        .text("row key name of data")
        .action((x, params) => params.copy(rowKeyName = x))
      opt[String]('l', "learningRate")
        .text("learning rate of training")
        .action((x, params) => params.copy(learningRate = x.toFloat))
      opt[String]('b', "batchSize")
        .text("batchsize of training")
        .action((x, params) => params.copy(batchSize = x.toInt))
    }
    val argv = parser.parse(args, Params()).head
    // load args and get data
    val dataPath = argv.dataPath
    val rowKeyName = argv.rowKeyName
    val learningRate = argv.learningRate
    val batchSize = argv.batchSize


    /**
     * Usage of BigDL PPML starts from here
     */
    VflContext.initContext()
    val pSI = new PSI()
    val (trainData, valData) = getData(pSI, dataPath, rowKeyName, batchSize)

    // create LogisticRegression object to train the model
    val lr = new LogisticRegression(featureNum, learningRate)
    lr.fit(trainData, valData)
    lr.evaluate()
  }

}
