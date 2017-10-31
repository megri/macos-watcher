package com.megri.sbt.macos.watcher

import java.io.{File => JFile}
import java.nio.file.{Path => JPath, WatchEvent => JWatchEvent, WatchKey => JWatchKey, Watchable => JWatchable}
import java.util.{List => JList}

import com.barbarysoftware.watchservice.{WatchEvent => BWatchEvent, WatchKey => BWatchKey, WatchService => BWatchService, WatchableFile => BWatchableFile}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._

class MacOSWatchService extends sbt.io.WatchService with BJConverters {
  import MacOSWatchServicePlugin.logger

  private[this] val underlying = BWatchService.newWatchService()
  private[this] val registered = mutable.Buffer.empty[JWatchKey]
  private[this] val name = getClass.getSimpleName

  override def init(): Unit = {
    logger.debug( s"$name: init()")
    logger.info( "Using native WatchService" )
  }

  override def pollEvents(): Map[JWatchKey, Seq[JWatchEvent[JPath]]] = {
    logger.debug( s"$name: pollEvents()")
    registered.flatMap{ watchKey =>
      val events = watchKey.pollEvents()

      if ( events == null ) None
      else Some( watchKey -> events.asScala.asInstanceOf[Seq[JWatchEvent[JPath]]] )
    }.toMap
  }

  // This doesn't seem to get called; leaving it unimplemented for now
  override def poll( timeout: Duration ): JWatchKey = {
    logger.warn( s"$name: poll()")
    ???
  }

  override def register( jPath: JPath, jEvents: JWatchEvent.Kind[JPath]* ): JWatchKey = {
    logger.debug( s"$name: register( $jPath, [${jEvents.mkString(", ")}])")
    val bwf = new BWatchableFile( jPath.toFile )
    val bEvents = jEvents.map( e => convertJWatchEventKind( e ) )
    val bKey = bwf.register( underlying, bEvents: _* )
    val jKey = convertBWatchKey( jPath, bKey )
    registered += jKey
    jKey
  }

  override def close(): Unit = {
    logger.debug( s"$name: close()")
    underlying.close()
  }
}

trait BJConverters {
  import java.nio.file.{StandardWatchEventKinds => JKind}
  import com.barbarysoftware.watchservice.{StandardWatchEventKind => BKind}

  def convertBWatchKey( jPath: JPath, bWatchKey: BWatchKey): JWatchKey =
    new JWatchKey {
      override def cancel( ): Unit =
        bWatchKey.cancel()

      override def pollEvents(): JList[JWatchEvent[_]] =
        bWatchKey.pollEvents().asScala
          .map( event => convertBWatchEvent( event ) )
          .asJava

      override def watchable( ): JWatchable = jPath

      override def isValid: Boolean =
        bWatchKey.isValid

      override def reset( ): Boolean =
        bWatchKey.reset()
    }

  def convertBWatchEvent( bWatchEvent: BWatchEvent[_] ): JWatchEvent[_] =
    new JWatchEvent[JPath] {
      override def kind(): JWatchEvent.Kind[JPath] = bWatchEvent.kind match {
        case BKind.ENTRY_CREATE => JKind.ENTRY_CREATE
        case BKind.ENTRY_DELETE => JKind.ENTRY_DELETE
        case BKind.ENTRY_MODIFY => JKind.ENTRY_MODIFY
//        case BKind.OVERFLOW     => JKind.OVERFLOW
      }

      override def count() =
        bWatchEvent.count()

      override def context(): JPath =
        bWatchEvent.context().asInstanceOf[JFile].toPath
    }

  def convertJWatchEventKind( jKind: JWatchEvent.Kind[_] ): BWatchEvent.Kind[_] = jKind match {
    case JKind.ENTRY_CREATE => BKind.ENTRY_CREATE
    case JKind.ENTRY_DELETE => BKind.ENTRY_DELETE
    case JKind.ENTRY_MODIFY => BKind.ENTRY_MODIFY
    case JKind.OVERFLOW     => BKind.OVERFLOW
  }

  def convertJPath( jPath: JPath ): BWatchableFile =
    new BWatchableFile( jPath.toFile )

  def convertBWatchableFile( bWatchableFile: BWatchableFile ): JPath =
    bWatchableFile.getFile.toPath
}
