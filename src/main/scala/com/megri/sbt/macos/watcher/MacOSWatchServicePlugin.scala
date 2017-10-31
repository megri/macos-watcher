package com.megri.sbt.macos.watcher

import java.nio.file.FileSystems

import sbt.Keys.watchService
import sbt.Watched.PollDelay
import sbt.io.PollingWatchService
import sbt.{AutoPlugin, ConsoleLogger}

import scala.util.Properties

object MacOSWatchServicePlugin extends AutoPlugin {
  private[watcher] val logger = ConsoleLogger()

  lazy val watchServiceSetting =
    watchService := { () =>
      if ( Properties.isMac )
        new MacOSWatchService
      else
        sys.props.get( "sbt.watch.mode" ) match {
          case Some( "polling" ) =>
            new PollingWatchService( PollDelay )
          case _ =>
            FileSystems.getDefault.newWatchService()
        }
    }

  override def requires = sbt.plugins.JvmPlugin
  override def trigger  = allRequirements
  override def globalSettings = Seq( watchServiceSetting )
}
