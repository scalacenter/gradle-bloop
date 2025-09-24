package bloop.integrations.gradle

import scala.util.Try

object SemVer {

  case class Version(
      major: Int,
      minor: Int,
      patch: Option[Int],
      releaseCandidate: Option[Int],
      milestone: Option[Int]
  ) {
    def >(that: Version): Boolean = {
      this.major > that.major ||
      (this.major == that.major && this.minor > that.minor) ||
      (this.major == that.major && this.minor == that.minor && (for {
        p1 <- this.patch; p2 <- that.patch
      } yield p1 > p2).getOrElse(true)) ||
      // 3.0.0-RC1 > 3.0.0-M1
      this.releaseCandidate.isDefined && that.milestone.isDefined ||
      // 3.0.0 > 3.0.0-M2 and 3.0.0 > 3.0.0-RC1
      (this.milestone.isEmpty && this.releaseCandidate.isEmpty && (that.milestone.isDefined || that.releaseCandidate.isDefined)) ||
      // 3.0.0-RC2 > 3.0.0-RC1
      comparePreRelease(that, (v: Version) => v.releaseCandidate) ||
      // 3.0.0-M2 > 3.0.0-M1
      comparePreRelease(that, (v: Version) => v.milestone)
    }

    def >=(that: Version): Boolean = this > that || this == that

    def <(that: Version): Boolean = !(this >= that)

    def <=(that: Version): Boolean = !(this > that)

    private def comparePreRelease(
        that: Version,
        preRelease: Version => Option[Int]
    ): Boolean = {
      val thisPrerelease = preRelease(this)
      val thatPrerelease = preRelease(that)
      this.major == that.major && this.minor == that.minor && (this.patch.isEmpty || that.patch.isEmpty || this.patch == that.patch) &&
      thisPrerelease.isDefined && thatPrerelease.isDefined && thisPrerelease
        .zip(thatPrerelease)
        .exists { case (a, b) => a > b }
    }

    override def toString: String = {
      val base = patch match {
        case Some(p) => s"$major.$minor.$p"
        case None => s"$major.$minor"
      }
      val suffix = releaseCandidate.map(s => s"-RC$s").orElse(milestone.map(s => s"-M$s"))
      base + suffix.getOrElse("")
    }

  }

  object Version {
    def fromString(version: String): Version = {
      // Extract numeric core (may be 1, 2 or 3 segments) and keep the rest (pre-release / metadata)
      val core = version.takeWhile(c => c.isDigit || c == '.')
      val remainder = version.drop(core.length)

      val parts = core.split("\\.").filter(_.nonEmpty).toList
      def toIntOpt(s: String) = Try(s.toInt).toOption

      val major = parts.lift(0).flatMap(toIntOpt).getOrElse(0)
      val minor = parts.lift(1).flatMap(toIntOpt).getOrElse(0)
      val patch = parts.lift(2).flatMap(toIntOpt)

      def extract(tag: String): Option[Int] = {
        val i = remainder.indexOf(tag)
        if (i >= 0) {
          val after = remainder.substring(i + tag.length)
          val digits = after.takeWhile(_.isDigit)
          // also allow forms like -RC1-suffix
          if (digits.nonEmpty) Try(digits.toInt).toOption else None
        } else None
      }

      val releaseCandidate = extract("-RC")
      val milestone = extract("-M")

      Version(major, minor, patch, releaseCandidate, milestone)
    }
  }

  def isCompatibleVersion(minimumVersion: String, version: String): Boolean = {
    Version.fromString(version) >= Version.fromString(minimumVersion)
  }

  def isLaterVersion(earlierVersion: String, laterVersion: String): Boolean = {
    Version.fromString(laterVersion) > Version.fromString(earlierVersion)
  }
}
