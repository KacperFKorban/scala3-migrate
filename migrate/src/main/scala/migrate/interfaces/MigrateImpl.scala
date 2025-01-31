package migrate.interfaces

import java.nio.file.Path
import java.{util => jutil}

import scala.jdk.CollectionConverters._

import migrate.Scala3Migrate
import migrate.internal.AbsolutePath
import migrate.internal.Classpath
import migrate.utils.ScalaExtensions._
import migrate.utils.ScalafixService

final class MigrateImpl() extends Migrate {

  override def migrate(
    unmanagedSources: jutil.List[Path],
    managedSources: jutil.List[Path],
    targetRoot: Path,
    scala2Cp: jutil.List[Path],
    scala2CompilerOptions: jutil.List[String],
    scala3Cp: jutil.List[Path],
    scala3CompilerOptions: jutil.List[String],
    scala3ClassDirectory: Path
  ): Unit =
    (for {
      unmanagedSourcesAbs     <- unmanagedSources.asScala.toSeq.map(AbsolutePath.from).sequence
      managedSourcesAbs       <- managedSources.asScala.toSeq.map(AbsolutePath.from).sequence
      targetRootAbs           <- AbsolutePath.from(targetRoot)
      scala2CpAbs             <- scala2Cp.asScala.toList.map(AbsolutePath.from).sequence
      scala2Classpath          = Classpath(scala2CpAbs: _*)
      scala3CpAbs             <- scala3Cp.asScala.toList.map(AbsolutePath.from).sequence
      scala3Classpath          = Classpath(scala3CpAbs: _*)
      scala3ClassDirectoryAbs <- AbsolutePath.from(scala3ClassDirectory)
      configuredScalafixSrv <-
        ScalafixService.from(scala2CompilerOptions.asScala.toList, scala2Classpath, targetRootAbs)
      scalaMigrate = new Scala3Migrate(configuredScalafixSrv)
      _ <- scalaMigrate
             .migrate(
               unmanagedSources = unmanagedSourcesAbs,
               managedSources = managedSourcesAbs,
               scala3Classpath = scala3Classpath,
               scala3CompilerOptions = scala3CompilerOptions.asScala.toList,
               scala3ClassDirectory = scala3ClassDirectoryAbs
             )
    } yield ()).get

  override def migrateScalacOption(scala3CompilerOptions: jutil.List[String]): ScalacOptions = {
    val s = scala3CompilerOptions.asScala.toList // .mkString(" ")
    Scala3Migrate.migrateScalacOptions(s)
  }

  override def migrateLibs(libs: jutil.List[Lib]): MigratedLibs = {
    val initialLibs = libs.asScala.toList
    Scala3Migrate.migrateLibs(initialLibs)
  }

  override def migrateSyntax(
    unmanagedSources: jutil.List[Path],
    targetRoot: Path,
    scala2Cp: jutil.List[Path],
    scala2CompilerOptions: jutil.List[String]
  ): Unit =
    (for {
      unmanagedSourcesAbs <- unmanagedSources.asScala.toSeq.map(AbsolutePath.from).sequence
      targetRootAbs       <- AbsolutePath.from(targetRoot)
      scala2CpAbs         <- scala2Cp.asScala.toList.map(AbsolutePath.from).sequence
      scala2Classpath      = Classpath(scala2CpAbs: _*)
      configuredScalafixSrv <-
        ScalafixService.from(scala2CompilerOptions.asScala.toList, scala2Classpath, targetRootAbs)
      scalaMigrate = new Scala3Migrate(configuredScalafixSrv)
      _           <- scalaMigrate.migrateSyntax(unmanagedSourcesAbs)
    } yield ()).get

}
