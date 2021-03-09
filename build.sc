/*
 * Copyright 2021 Tomas Zeman <tomas@functionals.cz>
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
import java.util.jar.JarFile

import mill._
import mill.api.Loose
import mill.define.{Command, Sources, Target}
import mill.scalajslib._
import mill.scalalib._
import mill.scalalib.publish._

import scala.io.Source
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object V {
  val app = "0.1-SNAPSHOT"
  val scalaJs = "1.5.0"
  val scala212 = "2.12.12"
  val scala213 = "2.13.5"

  object udash {
    val core = "0.9.0-M8"
  }

  object webjars {
    val bulma = "0.9.1"
  }

}

object D {

  object udash {
    val css = ivy"io.udash::udash-css::${V.udash.core}"
  }

  object webjars {
    val bulma = ivy"org.webjars.npm:bulma:${V.webjars.bulma}"
  }

}

trait Common extends CrossScalaModule with PublishModule {

  override def artifactName: T[String] = "bulma"

  override def publishVersion: Target[String] = V.app

  override def pomSettings: T[PomSettings] = PomSettings(
    description = "Scala.js library facade for Bulma CSS framework",
    organization = "cz.functionals",
    url = "https://fossil.functionals.cz/udash-bulma",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl(developerConnection = Some(
      "ssh://tzeman@fossil.functionals.cz/repos/public/udash-bulma.fossil")),
    developers = Seq(
      Developer("tzeman", "Tomas Zeman", "")
    )
  )

  override def scalacOptions = T{Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-target:jvm-1.8"
  )}

  override def sources: Sources = T.sources{
    super.sources() :+ PathRef(millSourcePath / 'shared)
  }

  def bulmaDep = T{Agg(D.webjars.bulma)}

  def pascalCase(s: String): String = s.split("-").map(
    _.toList match {
      case first :: rest => (first.toUpper :: rest).mkString
    }).mkString

  def camelCase(s: String): String = pascalCase(s).toList match {
    case first :: rest => (first.toLower :: rest).mkString
  }

  override def generatedSources = T.sources {
    val d = T.ctx.dest

    val allClasses = resolveDeps(bulmaDep)().iterator flatMap { fn =>
      val jar = new JarFile(fn.path.toIO)
      jar.entries().asScala.toSeq find(_.getName endsWith "/bulma.css") map(e =>
        jar -> e)
    } flatMap { case (jar, e) =>
      Source.fromInputStream(jar.getInputStream(e)).getLines()
    } filter(_.startsWith(".")) flatMap { v =>
      v drop 1 split '.' map(_ takeWhile(c =>
        c.isLetterOrDigit || Set('-', '_').contains(c)))
    }

    val delim = '"'
    val definitions = allClasses.toList.distinct.sorted map { c =>
      s"  val ${camelCase(c)}: CssStyleName = CssStyleName($delim$c$delim)"
    }

    os.write(d / "Bulma.scala",
      s"""package bulma
         |
         |import io.udash.css.CssStyleName
         |
         |trait Bulma {
         |${definitions mkString "\n"}
         |}
         |
         |object Bulma extends Bulma
         |""".stripMargin)

    super.generatedSources() :+ PathRef(d)
  }

  protected def commonDeps = Agg(D.udash.css)

  override def ivyDeps: Target[Loose.Agg[Dep]] = commonDeps

}

class JvmModule(val crossScalaVersion: String) extends Common
class JsModule(val crossScalaVersion: String, crossJSVersion: String)
  extends ScalaJSModule with Common {

  override def scalaJSVersion: Target[String] = crossJSVersion

  override def millSourcePath = super.millSourcePath / os.up
}

object jvm extends Cross[JvmModule](V.scala212, V.scala213)
object js extends Cross[JsModule](
  V.scala212 -> V.scalaJs,
  V.scala213 -> V.scalaJs
)

def compileAll(): Command[Unit] = T.command{
  jvm(V.scala213).compile()
  js(V.scala213, V.scalaJs).compile()
  jvm(V.scala212).compile()
  js(V.scala212, V.scalaJs).compile()
  ()
}

def publishLocal(): Command[Unit] = T.command{
  jvm(V.scala213).publishLocal()()
  js(V.scala213, V.scalaJs).publishLocal()()
  jvm(V.scala212).publishLocal()()
  js(V.scala212, V.scalaJs).publishLocal()()
}

def publishM2Local(p: os.Path): Command[Unit] = T.command{
  jvm(V.scala213).publishM2Local(p.toString)()
  js(V.scala213, V.scalaJs).publishM2Local(p.toString)()
  jvm(V.scala212).publishM2Local(p.toString)()
  js(V.scala212, V.scalaJs).publishM2Local(p.toString)()
  ()
}

// vim: et ts=2 sw=2 syn=scala
