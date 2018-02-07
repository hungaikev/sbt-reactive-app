/*
 * Copyright 2017 Lightbend, Inc.
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

package com.lightbend.rp.sbtreactiveapp.magic

import sbt.{ Attributed, File, IO }
import scala.collection.JavaConverters._

object Build {
  def annotate(config: String): String =
    s"""|# Generated by sbt-reactive-app. To disable this, set the `prependRpConf` SBT key to `None`.
        |
        |$config""".stripMargin

  def withHeader(comment: String, config: String): String =
    s"""|# $comment
        |
        |$config""".stripMargin

  def extractApplicationConf(
    managedConfigNames: Seq[String],
    unmanagedConfigNames: Seq[String],
    unmanagedResources: Seq[File],
    dependencyClasspath: Seq[Attributed[File]]): Option[String] = {
    val dependencyClassLoader = new java.net.URLClassLoader(dependencyClasspath.files.map(_.toURI.toURL).toArray)

    val managedConfigs =
      managedConfigNames
        .flatMap(dependencyClassLoader.findResources(_).asScala)

    val unmanagedConfigs =
      unmanagedConfigNames
        .flatMap(c => unmanagedResources.filter(_.getName == c))
        .map(_.toURI.toURL)

    val allConfigs =
      managedConfigs ++ unmanagedConfigs

    if (allConfigs.nonEmpty) {
      Some(
        annotate(
          allConfigs
            .foldLeft(Seq.empty[String]) {
              case (accum, conf) =>
                accum :+ withHeader(conf.toString, IO.readLinesURL(conf).mkString(IO.Newline))
            }
            .mkString(IO.Newline)))

    } else {
      None
    }
  }
}