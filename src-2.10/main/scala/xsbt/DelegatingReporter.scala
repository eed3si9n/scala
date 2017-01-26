/* sbt -- Simple Build Tool
 * Copyright 2008, 2009, 2010  Mark Harrah
 */
package xsbt

import xsbti.{ F0, Logger, Maybe }
import java.io.File
import sbt.util.InterfaceUtil.o2jo
import java.util.Optional

private object DelegatingReporter {
  def apply(settings: scala.tools.nsc.Settings, delegate: xsbti.Reporter): DelegatingReporter =
    new DelegatingReporter(Command.getWarnFatal(settings), Command.getNoWarn(settings), delegate)
}

// The following code is based on scala.tools.nsc.reporters.{AbstractReporter, ConsoleReporter}
// Copyright 2002-2009 LAMP/EPFL
// Original author: Martin Odersky
private final class DelegatingReporter(warnFatal: Boolean, noWarn: Boolean, private[this] var delegate: xsbti.Reporter) extends scala.tools.nsc.reporters.Reporter {
  import scala.tools.nsc.util.{ FakePos, NoPosition, Position }

  def dropDelegate(): Unit = { delegate = null }
  def error(msg: String): Unit = error(FakePos("scalac"), msg)

  def printSummary(): Unit = delegate.printSummary()

  override def hasErrors = delegate.hasErrors
  override def hasWarnings = delegate.hasWarnings
  def problems = delegate.problems
  override def comment(pos: Position, msg: String): Unit = delegate.comment(convert(pos), msg)

  override def reset(): Unit = {
    super.reset
    delegate.reset()
  }
  protected def info0(pos: Position, msg: String, rawSeverity: Severity, force: Boolean): Unit = {
    val skip = rawSeverity == WARNING && noWarn
    if (!skip) {
      val severity = if (warnFatal && rawSeverity == WARNING) ERROR else rawSeverity
      delegate.log(convert(pos), msg, convert(severity))
    }
  }
  def convert(posIn: Position): xsbti.Position =
    {
      val pos =
        posIn match {
          case null | NoPosition => NoPosition
          case x: FakePos        => x
          case x =>
            posIn.inUltimateSource(posIn.source)
        }
      pos match {
        case NoPosition | FakePos(_) => position(None, None, None, "", None, None, None)
        case _                       => makePosition(pos)
      }
    }
  private[this] def makePosition(pos: Position): xsbti.Position =
    {
      val src = pos.source
      val sourcePath = src.file.path
      val sourceFile = src.file.file
      val line = pos.line
      val lineContent = pos.lineContent.stripLineEnd
      val offset = getOffset(pos)
      val pointer = offset - src.lineToOffset(src.offsetToLine(offset))
      val pointerSpace = ((lineContent: Seq[Char]).take(pointer).map { case '\t' => '\t'; case x => ' ' }).mkString
      position(Some(sourcePath), Some(sourceFile), Some(line), lineContent, Some(offset), Some(pointer), Some(pointerSpace))
    }
  private[this] def getOffset(pos: Position): Int =
    {
      // for compatibility with 2.8
      implicit def withPoint(p: Position): WithPoint = new WithPoint(pos)
      final class WithPoint(val p: Position) { def point = p.offset.get }
      pos.point
    }
  private[this] def position(sourcePath0: Option[String], sourceFile0: Option[File], line0: Option[Int], lineContent0: String, offset0: Option[Int], pointer0: Option[Int], pointerSpace0: Option[String]) =
    new xsbti.Position {
      val line = o2oi(line0)
      val lineContent = lineContent0
      val offset = o2oi(offset0)
      val sourcePath = o2jo(sourcePath0)
      val sourceFile = o2jo(sourceFile0)
      val pointer = o2oi(pointer0)
      val pointerSpace = o2jo(pointerSpace0)
      override def toString =
        (sourcePath0, line0) match {
          case (Some(s), Some(l)) => s + ":" + l
          case (Some(s), _)       => s + ":"
          case _                  => ""
        }
    }

  import xsbti.Severity.{ Info, Warn, Error }
  private[this] def convert(sev: Severity): xsbti.Severity =
    sev match {
      case INFO    => Info
      case WARNING => Warn
      case ERROR   => Error
    }

  import java.lang.{ Integer => I }
  private[this] def o2oi(opt: Option[Int]): Optional[I] = opt match { case None => Optional.empty[I]; case Some(s) => Optional.ofNullable[I](s) }
}
